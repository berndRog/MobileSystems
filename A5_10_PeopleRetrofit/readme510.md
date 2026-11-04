# A5_10_PeopleRetrofit

English | [Deutsch](readme510_ger.md)

## Purpose and prerequisites

**A5_10 continues [A5_01_PeopleRoom3](../A5_01_PeopleRoom3/readme501.md) with a different data source.** The people list, form, navigation, image picker, use cases, and `IPersonRepository` contract remain familiar. A5_01 implements the contract with a local Room database and DAO; A5_10 implements it with Retrofit calls to a remote `PeopleApi`. The focus is the boundary between domain logic and HTTP, not a second Room database.

Students should know the repository and dependency-injection structure from A5_01, Kotlin coroutines/`Flow`, and basic JSON and HTTP methods.

## Learning objectives

After studying the module, students should be able to:

- Translate GET, POST, PUT, and DELETE operations into a Retrofit interface using `@Path` and `@Body`.
- Separate the JSON transport model `PersonDto` from the domain entity `Person` through mapping functions.
- Explain why a one-shot HTTP response cannot replace Room's observable `Flow` directly.
- Trace how Koin connects the shared Retrofit/OkHttp infrastructure, `IPersonWebservice`, and `PersonRepository`.
- Distinguish a local image file from the path string stored by `PeopleApi`.
- Preserve coroutine cancellation and map network failures before they reach the ViewModel.

## From Room to the Web API

| A5_01 | A5_10 |
| --- | --- |
| `IPersonDao` queries the local Room table. | `IPersonWebservice` sends requests to `PeopleApi`. |
| Room re-emits `Flow<List<PersonDto>>` after table changes. | GET returns one server snapshot. `PersonRepository` keeps the latest known list in an in-memory `MutableStateFlow`. |
| Room annotations define the persistence model. | `@Serializable` defines the JSON transport model; Retrofit annotations define the HTTP contract. |

The UI and use cases still call `IPersonRepository.observeAll()`, `findById()`, `create()`, `update()`, and `remove()`. On observation, the repository fetches a fresh list from the server. After a successful create, update, or delete, it adjusts its in-memory state so active UI collectors receive the change. This state is neither an offline database nor a durable cache. The repository maps technical failures through `NetworkExceptionMapper` and lets `CancellationException` propagate.

`IPersonWebservice` uses `peopleapi/v1/people` for list, count, and create requests; `peopleapi/v1/people/{id}` addresses one person. `PersonDto.imageUrl` is mapped to and from `Person.imagePath`.

The separate C# `PeopleApi` exposes this JSON-only OpenAPI v1 contract:

| Method and path | Purpose | Success status |
| --- | --- | --- |
| `GET /peopleapi/v1/people` | Return all people | `200` |
| `GET /peopleapi/v1/people/count` | Return the number of people | `200` |
| `GET /peopleapi/v1/people/{id}` | Return one person by UUID | `200` |
| `POST /peopleapi/v1/people` | Create a person from JSON | `201` |
| `PUT /peopleapi/v1/people/{id}` | Replace editable fields using JSON | `200` |
| `DELETE /peopleapi/v1/people/{id}` | Delete one person | `204` |

The route ID is a UUID. The JSON `PersonDto` has `id`, `firstName`, `lastName`, `email`, `phone`, and nullable `imageUrl`. The API may return `ProblemDetails` on errors such as invalid input (`400`), missing people (`404`), conflicts on create (`409`), or server failures (`500`). `imageUrl` is stored as a string; the API has no endpoint that receives or serves image files.

## Image boundary in A5_10

Images selected from the gallery or camera, and images created for seed data, remain files in the app's private Android storage (`people510`). The JSON request contains **only their local path as a nullable `imageUrl` string**. `PeopleApi` stores that string; it receives no image bytes and has no image-upload or image-delete endpoint in this module. A path from one device is therefore not a portable image URL for another client.

The create/update use cases commit the local image edit only after the person request succeeds. On delete, the server record is removed first; local file cleanup follows as a best-effort operation. `SeedApi` calls `countAll()` and creates example people only when the API is empty. Their locally generated image paths are sent through the same JSON person endpoint.

## Run and inspect

Start the separate C# `PeopleApi` on the computer, listening on **`0.0.0.0:5080`**. `0.0.0.0` is the server's bind address, not an address to enter in the Android client. On the computer, check the API with `curl http://127.0.0.1:5080/peopleapi/v1/people/count`.

### Android Virtual Device (AVD)

Forward the emulator's port to the computer with ADB:

```bash
adb reverse tcp:5080 tcp:5080
```

Keep `Globals.baseUrl` at `http://127.0.0.1:5080/`. Through `adb reverse`, this address reaches the API on the computer. Reapply the forwarding rule after reconnecting or restarting the emulator if necessary.

### Physical test phone on the same Wi-Fi network

Find the computer's **LAN IP address** on that Wi-Fi network, for example `192.168.1.25`. Change `Globals.baseUrl` to `http://192.168.1.25:5080/`, then rebuild and reinstall the app. The phone's own `127.0.0.1` does not refer to the computer. No `adb reverse` rule is needed for this Wi-Fi route. Allow incoming TCP port 5080 through the computer's firewall and ensure the Wi-Fi network does not isolate devices; test the URL from the phone's browser if the app cannot connect. Replace the example IP with the actual address.

The app's manifest permits the HTTP traffic used by this teaching setup. Open this directory as an independent Android Studio project, or run its wrapper here:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest # connected device or emulator required
```

The local tests cover DTO mapping, use cases, navigation, ViewModel behavior, image editing, and successful and failed repository deletion with fakes; they do not need a running API. The instrumented test exercises the swipe UI. To investigate a network failure, check that the service is running and that the AVD forwarding or the computer's LAN IP configured in the app matches the selected setup.

## Suggested learning path

1. Compare `IPersonDao` in A5_01 with `IPersonWebservice` here. Match each repository operation to an HTTP method and endpoint.
2. Follow one `Person` through `toPersonDto()`, the JSON request, the response, and `toPerson()`.
3. Change a person's name and trace the server response into the repository's `MutableStateFlow` and the visible list.
4. Select an image and identify which component owns the file and what the server actually stores. Explain why the resulting path works only in the client's storage context.
5. Disconnect the API and inspect how `NetworkExceptionMapper` and the UI handle the failure without replacing coroutine cancellation.
