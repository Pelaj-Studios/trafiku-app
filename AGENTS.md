# Trafiku Development Instructions

## Scope

These instructions apply to the complete repository.

## Language and documentation

- Use ADS-STE100 Simplified Technical English for documentation, KDoc, comments, commit text, and user-facing technical explanations.
- Use short, direct sentences.
- Use the active voice.
- Use one instruction or one main idea in each sentence.
- Use the same word for the same object or action.
- Use approved technical terms when a simpler word changes the technical meaning.
- Do not use slang, idioms, contractions, or unnecessary abbreviations.
- Define an abbreviation at its first use.
- Put conditions before actions. Example: "If the request fails, show the retry action."
- Use imperative verbs for procedures. Example: "Select the profile."
- Keep comments useful. Explain intent, constraints, or a non-obvious decision. Do not restate the code.
- Add KDoc to public APIs when the purpose, contract, side effects, or failure behavior is not clear from the declaration.
- Keep KDoc block tags in this order: `@constructor`, `@receiver`, `@param`, `@property`, `@return`, `@throws`, and `@see`.

## Android and Kotlin code style

- Follow the official Android Kotlin style guide.
- Encode source files as UTF-8.
- Use four spaces for indentation. Do not use tab characters.
- Use a trailing comma in multiline declarations and calls.
- Put one top-level class in a file when practical. Give the file the same PascalCase name as the class.
- Use PascalCase for types and for `@Composable` functions that return `Unit`.
- Give a `@Composable` function a noun or noun phrase that describes the UI that it emits.
- Use camelCase for functions, properties, local variables, and parameters.
- Use UPPER_SNAKE_CASE only for deeply immutable constants.
- Organize imports. Do not use wildcard imports.
- Keep functions small and focused. Extract code when a function has more than one responsibility.
- Prefer immutable data and read-only collections.
- Do not use `!!`. Handle a nullable value with an explicit safe path.
- Do not catch an exception without handling it or preserving its cause.
- Remove dead code and obsolete comments. Do not keep commented-out code.
- Use named arguments when they make a call easier to read.
- Write tests for business rules, state changes, navigation decisions, and defect fixes.

## Architecture and maintainability

- Use the `grill-me` and `android-data-layer` skills together for work on repositories, Room, Retrofit, local or remote data sources, caching, synchronization, and offline-first behavior.
- Use `grill-me` first. Inspect the project, collect the required user input, test assumptions, and get agreement on the data-layer design before implementation.
- Use `android-data-layer` after the design is clear. Apply its patterns during implementation and verification.
- Also use the `android-retrofit` skill when a task adds or changes a Retrofit service, an HTTP endpoint, an OkHttp client, serialization, request data, response handling, network error handling, or network dependency injection.
- Use `android-retrofit` with `grill-me` and `android-data-layer` when a new remote data source affects repository or synchronization behavior.
- Do not use `android-retrofit` for a local-only Room or DataStore change that does not affect HTTP networking.
- Read the complete instructions for all applicable skills before you design or change the data layer.
- Use the skills as workflow and implementation guidance. These repository instructions and current official Android guidance take priority if guidance conflicts.
- If an applicable skill is not available, report this condition. Continue with the available skills and current official Android architecture guidance.
- Keep a clear UI layer and data layer.
- Add a domain layer only when shared or complex business logic requires it.
- Put data access behind a repository. Do not access a data source from a composable.
- Use coroutines and `Flow` for asynchronous work and data streams.
- Use unidirectional data flow. Pass immutable state down and send events up.
- Keep one source of truth for each state value.
- Hoist state to the lowest common owner that reads and changes it.
- Keep simple UI element state near the composable that uses it.
- Put screen state and business logic in a screen-level state holder such as a `ViewModel`.
- Do not pass a `ViewModel` to reusable child composables. Pass state and event functions.
- Collect observable state with lifecycle-aware APIs.
- Keep platform and framework types at system boundaries when practical.
- Prefer clear code to premature abstraction. Extract an abstraction after it has a stable responsibility.
- Use the version catalog for dependency versions and aliases.
- Do not add a dependency when the platform or an existing dependency meets the requirement.
- Organize UI code by stable feature and responsibility. Do not keep unrelated UI declarations in one flat package.

## Product and deployment constraints

- Show all static passenger-facing text in Albanian, Serbian in Latin script, and English.
- Show the languages in that order. Put each translation on a separate line.
- Store each translation in its locale resource directory.
- Use `values-sq`, `values-b+sr+Latn`, and `values-en` for passenger-facing text.
- Resolve all three locale resources together with the shared trilingual string resolver.
- Add all three translations when you add or change passenger-facing text.
- Do not translate route names, stop names, or other data that the server supplies.
- Let the operator select one bus line and one direction.
- Treat the route stops as an ordered sequence.
- Keep Room as the source of truth for the bus network snapshot.
- Use a 24-hour cache time to live. Let the operator force a refresh.
- Keep stored network data when a refresh fails.
- Prefer device Global Positioning System (GPS) data.
- Use a generic Universal Serial Bus (USB) serial National Marine Electronics Association (NMEA) receiver when device GPS is unavailable or stale.
- Use a 20-metre stop arrival radius.
- Keep the simulated location source in debug builds only.
- Read advertisements from `DCIM/TrafikuAds/` through MediaStore.
- Show route information for 30 seconds before an advertisement.
- Show a still image for 20 seconds. Show a video until playback ends.
- Mute advertisement video playback.
- Do not show GPS status in the advertisement header.
- Treat a paired Bluetooth remote as the authorized application input device.
- Do not add application-level remote authorization without a new product requirement.
- Keep Bluetooth pairing, unwanted input-device control, kiosk policy, and device maintenance outside the application scope.

## Jetpack Compose requirements

- Build all application UI with Jetpack Compose.
- Do not add XML layouts, fragments, or the legacy View UI toolkit.
- Use XML only for Android resources that Compose or the platform requires, such as the manifest, strings, drawables, or configuration.
- Use Compose for TV components from `androidx.tv.*` when a TV-specific component exists.
- Do not mix mobile Material components and TV Material components in one theme or component tree.
- Put `modifier: Modifier = Modifier` as the first optional parameter of a reusable composable.
- Apply the caller-provided modifier to the outermost suitable element.
- Make reusable composables stateless when practical.
- Pass only the data that a composable needs.
- Provide stable keys for items in lazy lists and lazy grids.
- Move sorting, filtering, parsing, and other expensive work out of composable bodies.
- Use `remember` only for work that must stay in the composition.
- Use `rememberSaveable` only for UI state that must survive recreation and can be saved safely.
- Use `derivedStateOf` when a derived value prevents frequent unnecessary recomposition.
- Defer reads of rapidly changing state. Use lambda-based modifiers when this skips composition or layout work.
- Do not write to state after the same composition has read it.
- Keep side effects in the correct effect API. Do not start work directly in a composable body.
- Add previews for reusable visual components when a preview gives useful review coverage.

## Android TV design and performance

- Treat the Android TV device as a constrained device. Protect memory, CPU time, GPU time, and startup time.
- Design for a D-pad and remote control first. Do not require touch gestures.
- Make every interactive element focusable and reachable in a logical direction.
- Show one clear focus target at all times when the screen has an interactive element.
- Give focused, pressed, selected, disabled, and loading states a clear visual difference.
- Use TV Material focus behavior before custom focus code.
- Restore focus when the user returns to a screen or list.
- Make Back navigation predictable. Do not trap focus or override Back without a product requirement.
- Keep important content inside TV-safe screen bounds.
- Use the project theme for color, typography, shape, spacing, and focus styling.
- Create reusable design tokens. Do not scatter raw color values, text sizes, or repeated dimensions through screens.
- Keep text readable at TV viewing distance. Maintain sufficient contrast.
- Keep animation short and purposeful. Avoid continuous animation and large blur, shadow, or alpha effects.
- Use lazy containers for long or unknown collections.
- Avoid unnecessary nested lazy containers. Verify focus and scroll behavior when nesting is required.
- Load images at their display size. Cache them. Use placeholders. Release data that the screen no longer needs.
- Avoid allocation, logging, I/O, decoding, and data transformation during composition or scrolling.
- Use immutable and stable UI models when this lets Compose skip work. Do not add stability annotations without proving the contract.
- Measure before and after a performance change. Use a release-like build on a representative low-end TV device.
- Check frame time, recomposition, memory use, startup, image loading, list scrolling, and focus movement.
- Add a benchmark or baseline profile for a critical user journey when the app has enough functionality to make the measurement useful.

## Verification

- Use the `Television_720p` Android TV virtual device for emulator tests.
- Do not use the `Small_Phone` virtual device for this project.
- Run the smallest relevant test first.
- Run `./gradlew test` for logic changes.
- Run `./gradlew lint` for Android and Compose changes.
- Run `./gradlew assembleDebug` for changes that affect the application build.
- Test focus movement, selection, Back behavior, and state restoration for TV UI changes.
- Test on an Android TV emulator or device when behavior depends on focus, remote input, rendering, or performance.
- Report each check that you did not run. Give the reason.

## Official references

Use these sources as the primary guidance. Check them again before a large architecture, style, or performance change because Android guidance can change.

- Android Kotlin style guide: https://developer.android.com/kotlin/style-guide
- Compose API guidelines: https://developer.android.com/develop/ui/compose/api-guidelines
- Android architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- Compose state hoisting: https://developer.android.com/develop/ui/compose/state-hoisting
- Compose performance best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Compose for TV: https://developer.android.com/training/tv/playback/compose
- TV focus system: https://developer.android.com/design/ui/tv/guides/styles/focus-system
