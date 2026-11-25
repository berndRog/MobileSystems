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

**Observe:** Room → `IPersonDao.observeAll()` → `Flow<List<PersonDto>>` → `PersonReadModel` projection mapping → `Flow<Result<List<PersonListItem>>>` → `PeopleViewModel` state → adapter → list.

The repository loads individual Domain entities through `findById()` and performs writes. The read model observes display projections through `IPersonReadModel.observeListItems()`.

`observeAll()` selects all people ordered by `lastName, firstName`. Writes to the table feed subsequent query results into the existing observation path; the adapter collects UI state lifecycle-aware.

The DAO also provides `findById()`, `count()`, inserts for one person or a list, update, and delete. Inserts use `OnConflictStrategy.ABORT`. The repository converts database failures into `Result.failure`, while coroutine cancellation continues to propagate.

## Didactic approach

Keep the application workflow familiar and expose the persistence boundary. Compare A4_01's shared DAO/database infrastructure with the module-owned classes in A5_01. Room annotations remain on `PersonDto`; the Domain entity `Person` remains free of them. Mapping functions make this boundary explicit.

`AppDatabase` registers only `PersonDto` and exposes `personDao()`. In `di/appModules.kt`, Koin builds the database using `Room.databaseBuilder`, `BundledSQLiteDriver`, and a query coroutine context defaulting to `Dispatchers.IO`. The configured database is `people51.db`, version 1. It then supplies the DAO to `PersonRepository` and `PersonReadModel`; only the read model needs the dispatcher for observation.

The Domain use cases introduced in A4_01 retain their role: coordinate person persistence and image-session commit or cleanup. They call `IPersonRepository`; the concrete Room access belongs to the Data layer.

`SeedDatabase` is a suspending Room 3 `onCreate(connection)` callback. It uses the supplied connection, existing converters, and bound SQL parameters within the database creation transaction. It runs only when the database is first created. Existing databases, including deliberately emptied ones, are not seeded again on reopening. `MainApplication` opens the database asynchronously in a controlled operation; `resultOf()` logs failures. Initial values match A4 without overwriting existing databases.

## DI container for database access

Koin assembles the database dependencies in `di/appModules.kt`. `MainApplication` starts the container with `startKoin`, supplies `androidContext`, and loads the module through `modules(appModule())`. Database configuration therefore belongs to A5_01's composition root.

| Koin registration | Construction and dependency |
| --- | --- |
| `single<AppDatabase>` | `Room.databaseBuilder` with `androidContext()`, `Globals.databaseName`, `BundledSQLiteDriver`, and the query coroutine context |
| `single<IPersonDao>` | `get<AppDatabase>().personDao()` obtains the DAO from that database |
| `single<IPersonReadModel>` | `PersonReadModel` receives the DAO and the qualified IO dispatcher to observe display projections |
| `single<IPersonRepository>` | `PersonRepository(_personDao = get<IPersonDao>())` binds the Domain interface to the concrete implementation |

`single` retains one instance per registration in the container; `get<T>()` resolves a registered dependency. This creates the chain **AppDatabase → IPersonDao → PersonRepository**, shared by the consuming objects. Koin invokes the configured builders and supplies dependencies; Room generates the concrete database/DAO implementations.

The `viewModel` definitions supply `IPersonReadModel` to `PeopleViewModel`. `PersonViewModel` and the person use cases receive `IPersonRepository` from `domain.repositories`. `SeedDatabase` receives the seed data provider and is registered as the Room creation callback. `MainApplication` opens a reader connection to trigger creation. Shared continues to provide image storage and ImageEdit.

## Extensions beyond A4_01

Four models have separate responsibilities: `PersonForm` retains raw user input, `Person` represents the domain entity, `PersonListItem` is the display projection, and `PersonDto` holds persistence data. `PersonInputMapping.kt` maps form and domain values; `data/mapping/mappings.kt` contains DTO/projection mappings. An immutable snapshot is normalized and validated before saving. The repository loads domain entities and performs writes; only the read model observes the list. Ordering by last name and first name remains a deliberate A5 difference.

Chapter 5 introduces `try/catch/finally`, `getOrThrow()`, and `resultOf()`. One-shot loading and saving use `getOrThrow()` when Result failures and thrown exceptions share an error path. Flows still use `.onSuccess`/`.onFailure`. Cancellation is rethrown. `finally` replaces A4's completion handlers for Save/image state; `ensureActive()`, `cancelAndJoin()`, and `_isClosing` remain necessary for coordination and late results. This deliberately extends A4's handling of unexpected exceptions.

Image processing and UI remain familiar: `ImageEditDelegate` with `REPLACE` and one image, a session shared by the delegate and create/update use cases, persistence before commit, and cleanup only after successful deletion. Create/update are parameterized Koin factories resolved with `parametersOf(imageEdit)`; delete remains a standalone factory and loads the current person by ID. Picker handlers stay composed; the Save overlay, image progress, Back guards, and Snackbar clearance match A4.

## Tests

Common behavior tests cover cancellation, Retry, Save/Cancel, Discard, image actions, and session sharing. Navigation and gallery integration tests carry forward A4 coverage with form/read-model adaptations. Delete confirmation uses the real StringProvider in English and German. Additional A5 tests cover form mapping, read models, repository, DAO return values, unexpected exceptions, database creation, reopening emptied databases, and rollback. Shared's technical image tests are not duplicated.

## Suggested learning path

1. **Read the schema:** Assign each `PersonDto` property to a column; explain primary-key identity, nullability, and the image-path reference.
2. **Read the DAO:** Relate each annotation and SQL query to the corresponding user operation.
3. **Trace a write and observation:** Change a name and follow mapping, update, and the reordered list result.
4. **Check persistence:** Save a changed person, restart the app, and compare the loaded values. Read the creation callback and explain why a deliberately emptied database remains empty.
5. **Trace DI:** Follow `startKoin` → `appModule()` → database → DAO → repository → ViewModel/use case. Explain which objects are registered as `single` and why consumers do not create a database themselves.
6. **Inspect boundaries and failures:** Read `PersonRepositoryTest`, which uses a fake DAO to check failure/success results for update and delete. Distinguish these repository tests from testing real Room queries.

As a learning outcome, annotate one complete write/read cycle and explain why DTO, Domain entity, DAO, repository, and use case have separate responsibilities. The chapter focuses on one table; relationships and joins are outside this learning step.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A5_01_PeopleRoom3).
