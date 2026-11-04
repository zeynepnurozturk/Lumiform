# Lumiform Android Code Challenge

## Run & Test
- **Run:** open in Android Studio and run `app` on an API 24+ emulator, or `./gradlew installDebug`.
- **Unit tests:** `./gradlew testDebugUnitTest`
- **Instrumented tests** (Room, WorkManager, Compose UI): start an emulator, then `./gradlew connectedDebugAndroidTest`
- **Offline:** launch once online so the form is cached, then turn on airplane mode and relaunch.

**Mock data:** the app calls the gist. To use the bundled JSON in `assets/mock/` instead, switch the `@Binds` in `NetworkModule` to `MockLumiformTestService`.

**Tools:** Android Studio 2026.2, JBR 25.0.3 (Java 11 target), Gradle 9.6.0, AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk/targetSdk 37, minSdk 24.

## Architecture
Single-activity Compose app with Hilt, split into three layers by package:
- **data:** Retrofit + kotlinx.serialization fetch the gist, Room caches it, WorkManager syncs answers, `NetworkMonitor` watches the connection.
- **domain:** repository interfaces (`LumiformTestRepository`, `SyncStatusRepository`), `LumiformTestUseCase` and plain models.
- **ui:** one ViewModel per screen (home, full image, summary) exposing `StateFlow`s. Screens are stateless composables. Navigation goes through `LumiformNavigator` (type-safe routes) and toasts through `ToastManager`, so ViewModels hold no context.

In a bigger project each package would become its own Gradle module (`:feature:form`, `:feature:image`, `:feature:summary`, `:core:domain`, `:core:data`, `:core:network`, `:core:database`, `:core:sync`, `:core:designsystem`, `:core:navigation`, plus `build-logic` convention plugins). Features would depend only on domain, design system and navigation, so Gradle enforces the layer boundaries and builds unchanged modules in parallel or not at all.

## How the Requirements Are Met
| Requirement | How |
|---|---|
| Hierarchy | One recursive composable draws sections at any depth, indenting nested ones. |
| Font sizes | Page `headlineMedium` > section `titleLarge` > nested `titleMedium` > deeper `titleSmall` > question `bodySmall`. |
| Text questions | Content shown directly. |
| Image questions | Thumbnail with title; tap opens a full-size screen. |
| Choice questions | Radio buttons, or checkboxes when `multiple_selection` is true. Saved to Room on every tap. |
| Offline | Relational Room tables; the last fetched form is shown when the network fails. |

**Why relational tables:** `items` holds every page, section and question linked to its parent; `responses` holds each choice option and whether it was picked. Saving an answer touches one row, and re-fetching the form keeps the user's answers.

## Offline, Sync & Errors
- **Cache fallback:** a successful fetch replaces the cache; on failure the cached form is shown, or an error with Retry if nothing is cached.
- **Retries:** up to 3 attempts with backoff, only for lost connections, timeouts and 5xx.
- **Errors:** separate messages for network and server failures.
- **Images:** Coil caches seen images; never-loaded ones show a broken-image icon.
- **Answer sync:** Next, Finish and Submit queue pages with WorkManager, which sends them in order when online and retries on failure. There is no submit endpoint, so sending is simulated and the request body is logged.
- **Sync status strip:** combines the worker state, unsynced page count and `NetworkMonitor` into *Saved on device → Waiting for network → Sending → Retrying → Synced* ("All answers sent" shows for 3 s, then hides). Only validated networks count as online, so a captive portal shows as offline.

## Tests
- **Unit:** DTO parsing, mappers, retry policy, repository (cache fallback, keeping selections, sync), sync status rules, ViewModels and navigator.
- **Instrumented:** Room DAO, `AnswerSyncWorker` (retry, network constraint, ordered queue), image route arguments.
- **Compose UI:** font hierarchy, image tap, radio vs checkbox, paging, empty/error states, full-image screen.

## Logging
Timber with tags `AnswerSync` (queueing, attempts, request body) and `PageDb` (cache writes, answers, synced pages). A tree is planted only in debuggable builds.

## With More Time
- [ ] **Real submission:** wire Submit to a real endpoint; once the backend confirms receipt, clear Room and reset the back stack (`submitAll()` already does the send-and-clear but needs the network, so it isn't wired up yet).
- [ ] **Stale-data banner:** tell the user when they're looking at a cached form, with its last-updated time.
- [ ] **Tolerant parsing:** skip unknown item `type`s instead of failing the whole parse and showing a connection error.
- [ ] **Room migrations:** turn on `exportSchema` and add migration tests before the schema changes (currently version 1, no exported schema).
- [ ] **Multi-module split:** move to the `:feature` / `:core` layout above with `build-logic` convention plugins.
- [ ] **More tests:** Compose UI tests for the summary screen and sync status strip, a test for `NetworkMonitor`, and an end-to-end offline → online flow.
- [ ] **Accessibility:** TalkBack labels for images and choices, larger font scales, contrast checks.
- [ ] **Polish:** loading skeletons, pull-to-refresh, dark-mode review, tablet/landscape layouts, localization.