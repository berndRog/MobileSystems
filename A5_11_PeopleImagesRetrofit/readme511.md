# A5_11_PeopleImagesRetrofit

English | [Deutsch](readme511_ger.md)

## Purpose and prerequisites

**A5_11 continues [A5_10_PeopleRetrofit](../A5_10_PeopleRetrofit/readme510.md), which replaced the Room data source from [A5_01_PeopleRoom3](../A5_01_PeopleRoom3/readme501.md) with a Web API.** The UI, use cases, and `IPersonRepository` contract remain familiar. The new focus is ownership and transfer of image files: A5_10 sends only an Android-local path string in JSON, while A5_11 uses dedicated `PeopleImagesApi` endpoints to upload or delete an image. The server stores the image and returns its URL.

Students should understand the repository/Retrofit structure from A5_10, multipart HTTP requests, and the image-editing workflow from A5_01.

## Learning objectives

After studying the module, students should be able to:

- Explain why a device-local path cannot serve as a durable image resource for other clients.
- Keep person JSON requests separate from multipart image uploads and image deletion.
- Distinguish a temporary local image path from a server-owned HTTP(S) image URL.
- Trace the create, update, and delete flows across client files and server resources.
- Explain rollback after a failed second request and why local files are cleaned up only after success.
- Test image behavior through the repository and use cases without a live server.

## Person data and observable state

As in A5_10, `IPersonWebservice` uses Retrofit for GET, POST, PUT, and DELETE on `peopleapi/v1/people`. `PersonDto` is the JSON contract and maps to the domain `Person`; the UI still depends on `IPersonRepository`. Unlike Room in A5_01, an HTTP GET returns a single snapshot. `PersonRepository` refreshes an in-memory `MutableStateFlow` and updates it after successful mutations so `observeAll()` remains usable. The flow is not offline persistence.

Koin supplies the project-specific `IPersonWebservice` and repository while `Shared` supplies Retrofit, OkHttp, JSON conversion, and `NetworkExceptionMapper`. Network errors are mapped to domain-facing results; coroutine cancellation propagates.

The separate C# project `PeopleImages` exposes the `PeopleImagesApi` OpenAPI v1 contract:

| Method and path | Purpose | Success status |
| --- | --- | --- |
| `GET /peopleapi/v1/people` | Return all people | `200` |
| `GET /peopleapi/v1/people/count` | Return the number of people | `200` |
| `GET /peopleapi/v1/people/{id}` | Return one person by UUID | `200` |
| `POST /peopleapi/v1/people` | Create a person from JSON | `201` |
| `PUT /peopleapi/v1/people/{id}` | Update editable fields using JSON | `200` |
| `DELETE /peopleapi/v1/people/{id}` | Delete one person | `204` |
| `POST /peopleapi/v1/people/{id}/image` | Upload or replace one image as multipart `file` | `200` |
| `DELETE /peopleapi/v1/people/{id}/image` | Remove the person's image | `200` |
| `GET /peopleapi/v1/images/{fileName}` | Download stored image bytes | `200` |

The route ID is a UUID. JSON `PersonDto` requires nonempty `firstName` and `lastName`; `email`, `phone`, and `imageUrl` can be null. JSON create ignores a supplied `imageUrl`, while JSON update preserves the current server image URL. Upload can fail with `400` for an empty, oversized, or unsupported file; requests for missing people or images return `404`. Other validation or create conflicts can return `400` or `409`, with `ProblemDetails` in the error response.

## Image lifecycle in A5_11

The person and image are separate server resources. `IPersonWebservice` adds `POST peopleapi/v1/people/{id}/image` with a multipart `file` part and `DELETE peopleapi/v1/people/{id}/image`. Both image operations return an updated `PersonDto`. The server controls `imageUrl`; create and update JSON requests do not set it directly. Mapping places the returned URL in `Person.imagePath`, so the same domain property can temporarily hold a local path before upload and a server URL afterward.

The image download route is not declared in `IPersonWebservice`: the app displays the returned URL through its image loader. That URL must be reachable from the Android device, not only from the computer running the API.

| Operation | Sequence |
| --- | --- |
| Create with an image | Create the person with JSON, then upload the selected local file. If upload fails, the repository tries to delete the newly created person. The local file remains available for retry. |
| Update with a new local image | Update person fields with JSON, then upload the replacement file. Delete the local temporary file only after the complete operation succeeds. |
| Update with an existing server URL | Update person fields; keep the current server image without uploading the URL as a file. |
| Update without an image | Update person fields, then call the image-delete endpoint. |
| Delete a person | Delete the person on the server; the server owns its stored image. Only an exceptional local temporary file is cleaned up on Android. |

`ImageRequestPart` verifies that a local file exists, chooses a media type, and builds the multipart part. It rejects HTTP(S) URLs. `PersonUcCreate` and `PersonUcUpdate` remove a temporary picker file after success; an error preserves it for retry. These steps span independent HTTP operations, so a failed update may require another attempt rather than an atomic transaction.

`SeedApi` first checks `countAll()`. If the API is empty, it creates example people as JSON and uploads their generated images separately. After all requests succeed, it removes the local seed files. On failure, it tries to remove people created during that seed run.

## Run and inspect

Start the separate C# project `PeopleImages` on the computer, listening on **`0.0.0.0:5081`**. `0.0.0.0` is the server's bind address, not an Android client URL. On the computer, check the API with `curl http://127.0.0.1:5081/peopleapi/v1/people/count`.

### Android Virtual Device (AVD)

Forward the emulator's port to the computer with ADB:

```bash
adb reverse tcp:5081 tcp:5081
```

Keep `Globals.baseUrl` at `http://127.0.0.1:5081/`. Through `adb reverse`, this address reaches the API on the computer. Reapply the forwarding rule after reconnecting or restarting the emulator if necessary.

### Physical test phone on the same Wi-Fi network

Find the computer's **LAN IP address** on that Wi-Fi network, for example `192.168.1.25`. Change `Globals.baseUrl` to `http://192.168.1.25:5081/`, then rebuild and reinstall the app. The phone's `127.0.0.1` refers to the phone itself. No `adb reverse` rule is needed for this Wi-Fi route. Allow incoming TCP port 5081 through the computer's firewall and ensure the Wi-Fi network does not isolate devices. Replace the example IP with the actual address.

Check the returned `imageUrl` as well: if it contains a host name or address that the phone cannot reach, the person data may load while the image does not. The image URL must point to the API using an address reachable from the phone, such as the computer's LAN IP. The app's manifest permits the HTTP traffic used by this teaching setup. Open this directory as an independent Android Studio project, or run its wrapper here:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest # connected device or emulator required
```

`PersonRepositoryImageTest` uses a fake web service to cover create/upload, rollback, reuse of server URLs, replacement upload, and image deletion. Other local tests cover mapping, use cases, ViewModel behavior, and navigation. They do not need a running API. The instrumented test exercises the swipe UI. To investigate a network failure, check that the service is running and that AVD forwarding or the computer's LAN IP configured in the app matches the selected setup.

## Suggested learning path

1. Compare the two `IPersonWebservice` interfaces. Identify the image endpoints added in A5_11 and the JSON endpoints retained from A5_10.
2. Follow a selected local file through `ImageRequestPart`, multipart upload, the returned `PersonDto.imageUrl`, and the UI image.
3. Trace create with a failed upload and explain the repository's rollback and retention of the local file.
4. Compare update with a local path, a server URL, and no image. Predict the HTTP requests for each case.
5. Explain which side owns each resource after a successful upload and why deleting a server URL as a local file would be wrong.
