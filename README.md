# Lumiform Android Code Challenge

## Run & Test
- Run: open the project in Android Studio and run `app` on an API 24+ emulator, or `./gradlew installDebug`.
- Unit tests: `./gradlew testDebugUnitTest`.
- Instrumented tests (Room, WorkManager, Compose UI): start an emulator, then `./gradlew connectedDebugAndroidTest`.
- Offline: launch once online so the form is cached, then turn on airplane mode and relaunch.

## Mock Data
The API response wasn't final when I started, so the early versions read a bundled copy of the JSON (`assets/mock/`). The app now calls the gist. To use the mock again, switch the `@Binds` in `NetworkModule` to `MockLumiformTestService`.

## Tool Versions
Android Studio 2026.2, JetBrains Runtime 25.0.3 (Java 11 target), Gradle 9.6.0, AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk/targetSdk 37, minSdk 24.

## Architecture
Single-activity Compose app with Hilt, in three layers:
- **data:** Retrofit + kotlinx.serialization fetch the gist, Room caches it and WorkManager syncs answers.
- **domain:** The repository interface, `LumiformTestUseCase` and plain models.
- **ui:** One ViewModel per screen (home, full image, summary) exposing `StateFlow`s. Screens are stateless composables, so they can be tested without a ViewModel. ViewModels navigate through `LumiformNavigator` with type-safe Navigation Compose routes.

## How the Requirements Are Met
| Requirement | How |
|---|---|
| Hierarchy | One recursive composable draws sections at any depth; nested sections are indented. |
| Font sizes | Page `headlineMedium` > section `titleLarge` > nested `titleMedium` > deeper `titleSmall` > question `bodySmall`. |
| Text questions | The content is shown directly. |
| Image questions | A thumbnail with its title; tapping it opens a full-size screen with the title. |
| Choice questions | Radio buttons for single selection, checkboxes when `multiple_selection` is true. Saved to Room on every tap. |
| Offline | Relational Room tables; the last fetched form is shown when the network fails. |

## Why Relational Tables
The form is saved in two tables instead of as one big JSON text:
- `items`: every page, section and question, each one linked to its parent.
- `responses`: the options of each choice question, and whether the user picked them.

This way, saving an answer changes only one row, and loading the form again from the server keeps the user's answers.

## Offline & Poor Connections
- **Cache fallback:** A successful fetch replaces the cache. If it fails, the cached form is shown; with nothing cached, an error with a Retry button appears.
- **Retries:** Up to 3 attempts with increasing delays, only for lost connections, timeouts and 5xx responses.
- **Friendly errors:** Separate messages for network and server failures.
- **Images:** Coil caches images that have been seen; one that was never loaded shows a broken-image icon.

## Answers & Sync
Each tap is saved to Room. Next, Finish and Submit queue pages with WorkManager, which sends them in order once there is a connection and retries on failure, so Submit also works offline. There is no submit endpoint, so sending is simulated and the request body is logged.

**TODO:** Once the backend confirms the answers were received, clear Room and reset the back stack. `submitAll()` in the repository already sends everything and then clears Room, but it needs the network, so it isn't wired to Submit yet.

## Tests
- **Unit:** DTO parsing, mappers, the retry policy, the repository (cache fallback, keeping selections, sync), the ViewModels and the navigator.
- **Instrumented:** The Room DAO, `AnswerSyncWorker` (retry, waiting for a network connection, ordered queue) and the image screen's route arguments.
- **Compose UI:** Font hierarchy, image tap, radio vs checkbox, paging, empty and error states, the full-image screen.

## Known Limitations
- Cached pages are shown without a banner saying they may be out of date.
- An unknown item `type` fails the whole parse and shows the connection error.
- Answer submission is simulated, and Room isn't cleared after Submit yet.
- Logging uses `println` instead of a logging library.

## AI Usage
TODO
