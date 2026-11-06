# LLM Project Guidelines: BirthdayBuddy

> [!IMPORTANT]
> **LLM Optimization**: This file is compiled specifically for LLM Agents. Any modification or addition to this file MUST maintain its high-density, structured English system-instruction format. DO NOT write conversational, narrative-style paragraphs.

This file provides high-priority, machine-readable instructions, constraints, and architecture guidelines. You MUST adhere to these rules at all times.

---

## 1. Core Principles & Constraints

- **Documentation Safety (MANDATORY)**:
  - Every significant code modification MUST be appended chronologically to [CHANGELOG.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/CHANGELOG.md).
  - Any new file, module, or database entity MUST be documented in [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md).
  - All new or modified classes, public functions, and complex code blocks MUST be documented using English KDoc or inline comments. Explain design decisions and architectural choices to support future LLM interactions.
- **Database Safety (Room)**:
  - DO NOT use `fallbackToDestructiveMigration` under any circumstances.
  - Schema changes MUST increment the version, define an auto-migration, and export schemas to `app/schemas` for verification.
- **Internationalization (i18n)**:
  - Strings MUST be updated simultaneously in German (`res/values-de/strings.xml`) and English (`res/values/strings.xml`).
- **Clean Architecture & Dependency Injection**:
  - Follow Feature-based Layering and Clean Architecture principles.
  - ViewModels MUST be decoupled from Android APIs. Use Hilt DI to supply dependencies.
  - Implement specialized ViewModels per screen using Uni-Directional Data Flow (UDF) / MVI patterns (e.g., central `onIntent` handler). ViewModels are feature-co-located directly alongside their screens in `ui/screens/<feature>/`.
- **Evergreen Dependencies & Version Catalog (SSOT)**:
  - [`gradle/libs.versions.toml`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml) is the authoritative Single Source of Truth for all library, plugin, and SDK versions.
  - DO NOT hardcode static library version numbers into rule files or architectural prompts. Always consult and maintain `libs.versions.toml` with current, compatible stable releases.
- **Coroutines & Hilt Dispatcher Injection (Google Best Practice)**:
  - DO NOT hardcode dispatchers (e.g., `withContext(Dispatchers.IO)` is prohibited).
  - Inject `@IoDispatcher`, `@DefaultDispatcher`, or `@MainDispatcher` from `com.heckmannch.birthdaybuddy.di` into Repositories, UseCases, and ViewModels.
  - All suspend functions in Repositories/DataSources MUST be main-safe.
- **Layout & Spacing Restrictions**:
  - DO NOT use additive vertical paddings. Spacings between elements MUST use bottom padding to avoid layout jumps inside `AnimatedVisibility` components.
  - All spacing, icon sizes, and transparency values MUST use standard tokens from `ui/theme/Dimensions.kt` (e.g., `SpacingNormal`, `IconSizeSmall`, `AlphaEmphasisNormal`). No magic/hardcoded values.
- **Warnings & Suppressions**:
  - Compiler, linter, and deprecation warnings MUST be resolved cleanly rather than suppressed.
  - Using `@Suppress` (such as `@Suppress("DEPRECATION")`) or `@SuppressLint` is prohibited unless absolutely necessary.

### 1.1 Directory Map (LLM Navigation Aid)
For detailed human-readable descriptions, consult [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md). Use this quick map for navigation:
- **Dependency Injection**: `di/` (`AppModule.kt`, `HelperBindingsModule.kt`)
- **Domain Layer**: `domain/` (`model/`, `usecase/`, `appfunctions/`, `permission/`)
- **Data Layer (Cache/DB)**: `data/local/` (Entities: `Contact`, `AppSettings`, `ContactUserData`, `LabelConfig`, `NotificationRule`, `PendingNotification`), `data/repository/` (Sync, Contacts, Backup, Notifications), `data/mapper/`
- **Platform Layer**: `platform/appfunctions/` (Android 16+ AppFunctions service & `@AppFunctionSerializable` models)
- **UI Screens & ViewModels**: `ui/screens/` (feature-co-located Screens, Contents, ViewModels, States, Actions)
  - `home/`: Main list, top chips, top bar, detail pane, fast-scroll, confetti (`HomeViewModel.kt`)
  - `onboarding/`: Onboarding flow and pages (`OnboardingViewModel.kt`)
  - `settings/`: App configuration (Backup, Labels, Notifications, Calendar, Theme, Sync, About)
- **UI Navigation**: `ui/navigation/` (`NavRoutes.kt` for serializable keys, `AppNavHost.kt` for NavDisplay and transitions)
- **UI Commons**: `ui/model/` (UI State/Models), `ui/components/` (shared dialogs, responsive layout), `ui/illustrations/` (custom native animations), `ui/theme/` (colors, typography, dimensions)
- **App-Level ViewModel**: `AppViewModel.kt` in root package (Activity-wide state)
- **Widgets**: `widget/` (Glance-based homescreen widgets)

---

## 2. Specialized Skills & Technical Standards

### 2.1 Material 3 Adaptive Layouts
- Use Jetpack Navigation 3's `ListDetailSceneStrategy` / `NavDisplay` for Master-Detail screens (e.g., `HomeScreen`, `SettingsScreen`).
- **WindowSizeClass API**: Use modern `WindowSizeClass` (from `androidx.window.core.layout`) via `LocalWindowSizeClass.current` or passed parameter. Check breakpoints using extension properties (`isWidthCompact`, `isWidthMedium`, `isWidthExpanded`, `isHeightCompact`) defined in [ResponsiveLayout.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ResponsiveLayout.kt). Do not use deprecated `WindowWidthSizeClass` / `WindowHeightSizeClass`.
- **DO NOT use** legacy `calculateWindowSizeClass(Activity)` from `androidx.compose.material3.windowsizeclass`.
- Compute layout directives using `calculatePaneScaffoldDirective(windowAdaptiveInfo)` from `androidx.compose.material3.adaptive.layout`.

### 2.2 AGP 9 & Kotlin Gradle DSL
- Use explicit KTS Gradle DSL configuration (`configure<ApplicationExtension>`).
- Declare plugins inside `plugins { ... }` block; do not use legacy `apply plugin`.
- Enforce Java 17 as standard (`jvmToolchain(17)`, `sourceCompatibility` / `targetCompatibility`).
- Enforce strict compiler flags in `gradle.properties`: `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true`.

### 2.3 Type-Safe Navigation (Navigation 3)
- Both global and local navigation are managed via Jetpack Navigation 3 (`NavDisplay`).
- **Destinations / Keys**: All routes/keys MUST be `@Serializable` Kotlin objects or data classes defined in [`NavRoutes.kt`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt). Screen mapping and transitions are orchestrated in [`AppNavHost.kt`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt).
- **Back Stack**: The backstack is managed as an observable state list of keys. Programmatic navigation is achieved by mutating this backstack list.
- **ViewModel Scoping**: ViewModels MUST be scoped to their respective `NavEntry` by passing `rememberViewModelStoreNavEntryDecorator()` inside `entryDecorators` of `NavDisplay`. Use `hiltViewModel()` within `entryProvider`.
- **ViewModel Arguments**: Use **Hilt Assisted Injection** (with a factory and creation callback inside `NavEntry` block) instead of `SavedStateHandle`.

### 2.4 Edge-to-Edge Support
- Support full Edge-to-Edge rendering on Android 15+.
- Utilize `AppResponsiveScaffold` to automatically pass `PaddingValues` with system window insets to screen contents.

### 2.5 Proguard & R8 Release Safety
- Protect Room entities, DAOs, Hilt workers, and `@Serializable` navigation routes from code shrinking or stripping by adding explicit keep rules to `proguard-rules.pro`.

### 2.6 Unit Testing Setup
- ViewModels and Repositories MUST be unit tested in `src/test`.
- Use `MainDispatcherRule` to mock the Main dispatcher in coroutines.
- Enable `unitTests.isReturnDefaultValues = true` for JVM tests needing mockable Android properties.

### 2.7 Performance & Compose Optimization
- Offload computationally heavy tasks to `Dispatchers.Default` using `flowOn`.
- Prefetch images via Coil `enqueue` using explicit memory cache keys (`getAvatarCacheKey(imageUri, lookupKey)`).

---

## 3. Agent Skills & Tooling Management

- **Location & Exclusions**: External skill repositories are automatically cloned into `.agents/external/` (git-ignored).
- **Gradle Tasks**: `checkAgentSkills` runs automatically during Android Studio sync; run `.\gradlew updateAgentSkills` to update skills.
- **Precedence**: Official Google skills take precedence over community skills.
