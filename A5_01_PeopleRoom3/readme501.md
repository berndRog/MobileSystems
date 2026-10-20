# A5_01_PeopleRoom3

## Purpose and prerequisites

**A5_01 builds on A4_01_ImagePicker and makes Room with one table the teaching focus.** The complete persistence layer now belongs to the example module: database, DAO, persistence model, mapping, and repository. Students can follow local storage without depending on a database module hidden in `Shared`.

The list (`PeopleAdapter`/`PeopleScreen`), form (`PersonAdapter`/`PersonScreen`), gallery/camera workflow, Domain use cases, navigation, and confirmed swipe-to-delete provide the familiar application context. Prerequisites are Kotlin data classes, basic SQL, coroutines/`Flow`, and the architecture introduced in A4_01.

## Learning objectives

After studying the module, students should be able to:

- Define one Room table using `@Entity` and `@PrimaryKey`, including nullable columns.
- Implement a DAO contract with `@Query`, `@Insert`, `@Update`, and `@Delete`.
- Distinguish reactive observation through `Flow` from individual `suspend` operations.
- Explain how `AppDatabase` declares entities and exposes the DAO, and how Room generates implementations through KSP.
- Map between the persistence model `PersonDto` and Domain entity `Person`.
- Connect database, DAO, and repository through Koin while keeping UI and Domain use cases independent of Room-specific types.
- Explain `single`, `get<T>()`, constructor injection, and container startup; trace how the same registered database supplies DAO, repository, and seeding.

## One table: Person

`data/local/dtos/PersonDto.kt` declares `@Entity(tableName = "Person")`:

| Column | Kotlin type | Meaning |
| --- | --- | --- |
| `id` | `String` | Primary key |
| `firstName`, `lastName` | `String` | Required name values |
| `email`, `phone` | `String?` | Optional contact values |
| `imagePath` | `String?` | Optional path to the separately stored image |

The table stores an image reference; the image file remains in app storage. This keeps the image lifecycle from A4_01 relevant while focusing on a single-table database.

## Concrete Room data flow

**Write:** form → Intent → ViewModel → create/update use case → `IPersonRepository` → `PersonRepository` → `Person.toPersonDto()` → DAO → Room.

**Observe:** Room → `IPersonDao.observeAll()` → `Flow<List<PersonDto>>` → repository mapping → `Flow<Result<List<Person>>>` → ViewModel state → adapter → list.

`observeAll()` selects all people ordered by `lastName, firstName`. Writes to the table feed subsequent query results into the existing observation path; the adapter collects UI state lifecycle-aware.

The DAO also provides `findById()`, `count()`, inserts for one person or a list, update, and delete. Inserts use `OnConflictStrategy.ABORT`. The repository converts database failures into `Result.failure`, while coroutine cancellation continues to propagate.

## Didactic approach

Keep the application workflow familiar and expose the persistence boundary. Compare A4_01's shared DAO/database infrastructure with the module-owned classes in A5_01. Room annotations remain on `PersonDto`; the Domain entity `Person` remains free of them. Mapping functions make this boundary explicit.

`AppDatabase` registers only `PersonDto` and exposes `personDao()`. In `di/appModules.kt`, Koin builds the database using `Room.databaseBuilder`, `BundledSQLiteDriver`, and a query coroutine context defaulting to `Dispatchers.IO`. The configured database is `people51.db`, version 1. It then supplies the DAO to `PersonRepository`.

The Domain use cases introduced in A4_01 retain their role: coordinate person persistence and image-session commit or cleanup. They call `IPersonRepository`; the concrete Room access belongs to the Data layer.

`SeedDatabase` checks `count()` and populates example people only when the table is empty. This provides a starting dataset without overwriting an existing non-empty table.

## DI container for database access

Koin assembles the database dependencies in `di/appModules.kt`. `MainApplication` starts the container with `startKoin`, supplies `androidContext`, and loads the module through `modules(appModule())`. Database configuration therefore belongs to A5_01's composition root.

| Koin registration | Construction and dependency |
| --- | --- |
| `single<AppDatabase>` | `Room.databaseBuilder` with `androidContext()`, `Globals.databaseName`, `BundledSQLiteDriver`, and the query coroutine context |
| `single<IPersonDao>` | `get<AppDatabase>().personDao()` obtains the DAO from that database |
| `single<IPersonRepository>` | `PersonRepository(_personDao = get<IPersonDao>())` binds the Domain interface to the concrete implementation |

`single` retains one instance per registration in the container; `get<T>()` resolves a registered dependency. This creates the chain **AppDatabase → IPersonDao → PersonRepository**, shared by the consuming objects. Koin invokes the configured builders and supplies dependencies; Room generates the concrete database/DAO implementations.

The `viewModel` definitions resolve `IPersonRepository` and pass it through constructors to the ViewModels and person use cases. Consumers receive their dependencies instead of constructing databases themselves. `SeedDatabase` receives the same registered database and DAO; `MainApplication` resolves it with `get()` before starting the seed operation. `Shared` continues to supply general services such as image storage, while the database registrations remain local to A5_01.

## Suggested learning path

1. **Read the schema:** Assign each `PersonDto` property to a column; explain primary-key identity, nullability, and the image-path reference.
2. **Read the DAO:** Relate each annotation and SQL query to the corresponding user operation.
3. **Trace a write and observation:** Change a name and follow mapping, update, and the reordered list result.
4. **Check persistence:** Save a changed person, restart the app, and compare the loaded values. Read the seed condition and explain why an existing non-empty table is retained.
5. **Trace DI:** Follow `startKoin` → `appModule()` → database → DAO → repository → ViewModel/use case. Explain which objects are registered as `single` and why consumers do not create a database themselves.
6. **Inspect boundaries and failures:** Read `PersonRepositoryTest`, which uses a fake DAO to check failure/success results for update and delete. Distinguish these repository tests from testing real Room queries.

As a learning outcome, annotate one complete write/read cycle and explain why DTO, Domain entity, DAO, repository, and use case have separate responsibilities. The chapter focuses on one table; relationships and joins are outside this learning step.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A5_01_PeopleRoom3).
