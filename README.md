# Lumiform Android Code Challenge

## Run & Test
- Run: open the project in Android Studio and run `app` on an API 24+ emulator, or `./gradlew installDebug`.
- Unit tests: `./gradlew testDebugUnitTest`.
- Instrumented tests (Room, WorkManager, Compose UI): start an emulator, then `./gradlew connectedDebugAndroidTest`.
- Offline mode: launch once online so the form is cached, then turn on airplane mode and relaunch.
- Bundled JSON: to run without the network, comment out the `LumiformTestServiceImpl` `@Binds` in `NetworkModule` and uncomment the `MockLumiformTestService` one below it.

## Tool Versions
Android Studio 2026.2, JetBrains Runtime 25.0.3 (Java 11 target), Gradle 9.6.0, AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk/targetSdk 37, minSdk 24.

## Architecture
Single-activity Compose app with Hilt, split into `data`, `domain` and `ui`.
- **data:** Retrofit + kotlinx.serialization fetch the gist. The JSON `type` field picks the DTO subtype (`section`, `text`, `image`, `choice`). Room caches the tree, and WorkManager syncs answers.
- **domain:** The repository interface, `LumiformTestUseCase`, and plain models. `UseCaseResult` keeps network and server errors apart.
- **ui:** One ViewModel per screen (home, full image, summary), each exposing `StateFlow`s. Screens are stateless composables that take state and a callbacks object, so they can be tested without a ViewModel.
- **Navigation:** ViewModels call `LumiformNavigator`, and `LumiformNavHost` turns its commands into type-safe Navigation Compose routes.

## How the Requirements Are Met
- **Hierarchy:** One recursive composable draws a section's items the same way as a page's, so any nesting depth works. Nested sections are indented.
- **Font sizes:** Page `headlineMedium` > section `titleLarge` > nested section `titleMedium` > deeper `titleSmall` > question `bodySmall`. The nesting level is passed down with a `CompositionLocal`.
- **Text questions:** Their content is shown directly.
- **Image questions:** A 120dp thumbnail with the title below it. Tapping it opens a full-size screen with the title. Coil shows a placeholder while loading and a broken-image icon if the image can't be loaded.
- **Choice questions:** Radio buttons when `multiple_selection` is false, checkboxes when it is true. Selections are saved to Room on every tap and restored on launch.
- **Offline:** Relational Room tables (see below). The last fetched form is shown when the network fails.

## Technical Choice: Relational Tables Instead of a JSON Blob
- `items` stores pages, sections and questions as one tree: each row has `parentId` (a foreign key with cascade delete), `position` and `type`.
- `responses` stores each choice option keyed by `(questionId, id)`, because response ids are not guaranteed to be unique across questions.
- Storing the tree as rows lets an answer change a single `selected` column instead of rewriting the whole document. It also lets a new fetch replace the form in one transaction while keeping existing selections.

## Offline & Poor Connections
- **Network first:** A successful response replaces the cache. If the request fails, the cached form is shown, and the error only appears when nothing has been cached yet. In that case the screen shows the message with a Retry button instead of an endless spinner.
- **Retries:** Up to 3 attempts with increasing delays, only for lost connections, timeouts and 5xx responses. Each attempt is limited by OkHttp connect/read/call timeouts.
- **Friendly errors:** Different messages for network and server failures, never the raw exception text.
- **Images:** Coil's disk cache keeps images that have been seen. An image that was never loaded shows a broken-image icon instead of an empty box.

## Answers & Sync
There is no submit endpoint, so syncing is simulated. Each tap is saved to Room and marks its page as unsynced. Next/Finish on a page queues an `AnswerSyncWorker` under one unique work name, so pages are sent in order and retried with backoff. The worker currently logs the request body. A page is marked synced only if it hasn't changed since the version that was sent. Submit on the summary sends every unsynced page right away. Once all of them are sent it clears the Room cache and restarts on a fresh home screen, which loads the test case from the API again with no answers. If a send fails, the error is shown and the answers stay for a retry.

## Tests
- **Unit (JVM):** DTO parsing, entity mappers, the retry policy in the service, the repository (cache fallback, keeping selections, sync), the home and summary ViewModels (selection rules, paging, saving, restoring), the UI mappers and the navigator.
- **Instrumented:** The Room DAO, `AnswerSyncWorker` (success, retry, failure, ordered queue), and the image ViewModel's route arguments.
- **Compose UI:** Font sizes decrease down the hierarchy; text, image title and image tap; radio vs checkbox roles and the saved selection; Previous/Next/Finish; the empty state; the error state and its Retry button; the full-image screen.

## Known Limitations
- When a load fails but pages are cached in Room, the cached pages are shown with no banner saying they may be out of date.
- An unknown item `type` fails the whole parse. It is reported as a network error, so the user sees the connection message.
- Answer submission is simulated, since there is no endpoint.
- Logging uses `println` instead of a logging library.

## AI Usage
TODO
