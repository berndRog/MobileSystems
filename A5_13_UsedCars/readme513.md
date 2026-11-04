# A5_13_UsedCars

English | [Deutsch](readme513_ger.md)

## Purpose

**A5_13 revisits [A5_02_UsedCarsRoom3](../A5_02_UsedCarsRoom3/readme502.md): the UsedCars app now obtains people, cars, and test drives through Retrofit and a `UsedCarsApi` instead of Room.** The same domain concepts make it possible to compare local persistence with a remote data source. The app needs a running API for its data; the repository's in-memory state is not an offline database.

## Relationships and data source

| Concept | A5_02 (Room) | A5_13 (Retrofit) |
| --- | --- | --- |
| People, cars, test drives | Three entities, DAOs, and one `AppDatabase` | Three serializable DTOs, Retrofit webservices, and repositories |
| Seller → cars (1:N) | `CarDto.personId` foreign key; Room queries and JOINs | `CarDto.personId` in JSON; `GET usedcarsapi/v1/people/{id}/cars` retrieves a seller's cars |
| Person ↔ car via test drive (m:n) | `TDriveDto` association table and JOINs | `TDriveDto` carries `personId` and `carId` in API requests and responses |
| Observation | Room emits database changes | Repositories refresh from HTTP and publish the latest snapshot through `MutableStateFlow` |

The **seller** of a car and the **participant** in a test drive are different roles. A person can sell several cars; a person can also test-drive cars sold by someone else. `TDriveDto` has its own `id`, `start: Instant`, and `isCompleted` fields. Its `start` value is serialized as an ISO-8601 string. Unlike A5_02's nullable Room seller column, A5_13's `CarDto.personId` is a required `String`. Room's foreign keys, unique indexes, and JOINs are no longer implemented in this Android client; API-side data integrity depends on the server.

## Request and repository flow

`IPersonWebservice`, `ICarWebservice`, and `ITDriveWebservice` define GET, POST, PUT, and DELETE requests under `usedcarsapi/v1/`. Retrofit maps JSON to `PersonDto`, `CarDto`, and `TDriveDto`; mapping functions convert these into the domain entities. Koin provides the three webservices and repositories. The shared module supplies Retrofit, OkHttp, JSON serialization, and network-error mapping.

The UI still works through `IPersonRepository`, `ICarRepository`, and `ITDriveRepository`. An HTTP GET returns a single snapshot, so each repository uses a `MutableStateFlow` to implement `observeAll()` and updates it after successful writes. A fresh server response is needed to see changes made by another client. `CarRepository.findByPersonId()` calls the seller-specific API route, rather than running a local JOIN.

The separate C# `UsedCarsApi` exposes this OpenAPI v1 contract. All routes below start with `/usedcarsapi/v1`:

| Resource path | Methods and results |
| --- | --- |
| `/people` | `GET` all (`200`), `POST` JSON person (`201`) |
| `/people/count` | `GET` count (`200`) |
| `/people/{id}` | `GET` one (`200`), `PUT` JSON update (`200`), `DELETE` (`204`) |
| `/people/{id}/cars` | `GET` cars offered by this seller (`200`) |
| `/people/{id}/test-drive-cars` | `GET` cars linked through test drives (`200`) |
| `/people/{id}/image` | `POST` multipart `file` upload or replacement (`200`), `DELETE` image (`200`) |
| `/cars` | `GET` all (`200`), `POST` JSON car (`201`) |
| `/cars/count` | `GET` count (`200`) |
| `/cars/{id}` | `GET` one (`200`), `PUT` JSON update (`200`), `DELETE` (`204`) |
| `/cars/{id}/images` | `POST` one multipart `file` upload (`200`) |
| `/cars/{id}/images/{fileName}` | `DELETE` one car image (`200`) |
| `/tdrives` | `GET` all (`200`), `POST` JSON test drive (`201`) |
| `/tdrives/count` | `GET` count (`200`) |
| `/tdrives/{id}` | `GET` one (`200`), `PUT` JSON update (`200`), `DELETE` (`204`) |
| `/images/{fileName}` | `GET` stored image bytes (`200`; `404` when absent) |

Route IDs are UUIDs. The API DTOs contain `PersonDto` with one nullable `imageUrl`, `CarDto` with a nullable list of `imageUrls` and a seller `personId`, and `TDriveDto` with participant `personId`, `carId`, `start` as a date-time string, and `isCompleted`. The Android `CarDto` requires `personId` and represents absent image lists as empty. Person JSON create ignores a submitted `imageUrl`; JSON update preserves the existing URL. Images are changed through the separate endpoints. Invalid person-image uploads may return `400`, missing people or images `404`, and conflicting person creation `409`, with `ProblemDetails` where specified by the API.

The server also exposes `/people/{id}/test-drive-cars`; the current Android Retrofit interfaces declare the seller-car route but not this test-drive-car route. The image download route is likewise consumed through returned image URLs rather than an explicit Retrofit method.

## Images and sample data

People have one server image (`PersonDto.imageUrl`); cars can have several (`CarDto.imageUrls`). The domain's `imagePath` or `imagePaths` can temporarily contain local picker/seed paths. After upload, they contain server URLs. JSON create/update and image transfer are separate requests:

- `POST /people/{id}/image` uploads a person's image; `DELETE /people/{id}/image` removes it.
- `POST /cars/{id}/images` uploads a car image; `DELETE /cars/{id}/images/{fileName}` removes one.
- `PersonRepository` and `CarRepository` upload local images after creating the JSON resource. If an upload fails, they attempt to delete the new resource. During car updates, existing remote URLs are retained, removed images are deleted, and new local files are uploaded.

These multi-request operations are not atomic transactions. `SeedApi` runs only when all three API collections are empty. It creates people, then cars, then test drives; uploads their images separately; and removes temporary local files after success. On failure it attempts to delete created test drives, cars, and people in reverse order.

The returned person and car image URLs must be reachable from the Android device. A reachable JSON API alone does not guarantee that images will load if their URLs contain an inaccessible host address.

## Run and test

Start the separate C# `UsedCarsApi` on the computer, listening on **`0.0.0.0:5082`**. `0.0.0.0` is the server's bind address, not an Android client URL. On the computer, check the API with `curl http://127.0.0.1:5082/usedcarsapi/v1/people/count`.

### Android Virtual Device (AVD)

Forward the emulator's port to the computer with ADB:

```bash
adb reverse tcp:5082 tcp:5082
```

Keep `Globals.baseUrl` at `http://127.0.0.1:5082/`. Through `adb reverse`, this address reaches the API on the computer. Reapply the rule after reconnecting or restarting the emulator if necessary.

### Physical test phone on the same Wi-Fi network

Find the computer's **LAN IP address** on that Wi-Fi network, for example `192.168.1.25`. Change `Globals.baseUrl` to `http://192.168.1.25:5082/`, then rebuild and reinstall the app. The phone's `127.0.0.1` refers to the phone itself. No `adb reverse` rule is needed for this Wi-Fi route. Allow incoming TCP port 5082 through the computer's firewall and ensure the Wi-Fi network does not isolate devices. Replace the example IP with the actual address. Returned `imageUrl` and `imageUrls` must also use a host address reachable from the phone.

The app manifest permits HTTP for this example. From this project directory:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Local unit tests cover DTO mapping, `Instant` serialization, image handling and deletion state in repositories, test-drive loading and sorting, and selected ViewModel behavior. They use fake webservices where needed and do not require a running API. To use the app, check that the API is running and that AVD forwarding or the computer's LAN IP configured in the app matches the selected setup.

## Suggested learning path

1. Compare the three Room DAOs in A5_02 with A5_13's three Retrofit interfaces.
2. Trace a car from its seller ID to `GET /people/{id}/cars`; then trace a test drive through `personId` and `carId`.
3. Follow create and update with images across JSON, multipart upload, returned URLs, and repository state.
4. Compare Room's live queries and database constraints with HTTP snapshots and server-owned integrity.
