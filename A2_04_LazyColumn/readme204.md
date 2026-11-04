# A2_04_LazyColumn

English | [Deutsch](readme204_ger.md)

## Purpose

This project displays people in a Compose `LazyColumn` and connects list events to an MVI-style ViewModel and a Room-backed repository. It extends the earlier UI examples with a real observable data source and dependency injection.

## Data and UI flow

`MainApplikation` starts Koin and the sample-data setup. `SeedDatabase` inserts example people when the database is empty. `PersonRepository` maps between the domain `Person` and the DAO DTO; `observeAll()` exposes Room changes as a `Flow<Result<List<Person>>>`.

`PeopleViewModel` collects that flow into `PeopleUiState`. `PeopleAdapter` observes the state with `collectAsStateWithLifecycle()` and passes the list and callbacks to `PeopleScreen`. The screen renders `LazyColumn` items with each person's `id` as a stable key. A remove event goes through `PeopleIntent.Remove` to the repository; Room then emits the updated list. The detail intent is currently only logged, with no navigation attached.

`PersonViewModel` and `PersonAdapter` provide a separate create/detail form example. `MainActivity` currently displays `PeopleAdapter`; the form call is commented out. `TestLazyColumn` in the activity demonstrates `item`, indexed `items(count = ...)`, and keyed `items(items = ..., key = ...)` with an in-memory sample list.

## Try it

Open this directory as an Android Studio project, or run:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Run the app and remove a sample person. Trace the event from the row through the ViewModel and DAO back to the observed list. Compare `TestLazyColumn`'s item variants and explain why stable keys matter when items change. The included tests are starter examples rather than focused list or repository tests.
