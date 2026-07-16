# LLM Project Guidelines: BirthdayBuddy

> [!IMPORTANT]
> **LLM Optimization**: This file is compiled specifically for LLM Agents. Any modification or addition to this file MUST maintain its high-density, structured English system-instruction format. DO NOT write conversational, narrative-style paragraphs.

This file provides high-priority, machine-readable instructions, constraints, and architecture guidelines. You MUST adhere to these rules at all times.

---

## 1. Core Principles & Constraints

- **Documentation Safety**:
  - Every significant code modification MUST be appended to [CHANGELOG.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/CHANGELOG.md).
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
  - Implement specialized ViewModels per screen using Uni-Directional Data Flow (UDF) / MVI patterns (e.g., central `onIntent` handler).
- **Layout & Spacing Restrictions**:
  - DO NOT use additive vertical paddings. Spacings between elements MUST use bottom padding to avoid layout jumps inside `AnimatedVisibility` components.
  - All spacing, icon sizes, and transparency values MUST use standard tokens from `ui/theme/Dimensions.kt` (e.g., `SpacingNormal`, `IconSizeSmall`, `AlphaEmphasisNormal`). No magic/hardcoded values.
- **Warnings & Suppressions**:
  - Compiler, linter, and deprecation warnings MUST be resolved cleanly rather than suppressed.
  - Using `@Suppress` (such as `@Suppress("DEPRECATION")`) or `@SuppressLint` is prohibited unless absolutely necessary (e.g., when calling platform-deprecated APIs on older Android SDK branches where no back-ported V2/compat alternative exists).


### 1.1 Directory Map (LLM Navigation Aid)
For detailed human-readable descriptions, consult [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md). Use this quick map for navigation:
- **Dependency Injection**: `di/` (`AppModule.kt`, `HelperBindingsModule.kt`)
- **Data Layer (Cache/DB)**: `data/local/` (Entities: `Contact`, `AppSettings`, `ContactUserData`, `LabelConfig`, `NotificationRule`, `PendingNotification`)
- **Repositories**: `data/repository/` (Sync, Contacts, Backup, Notifications)
- **UI Screens**: `ui/screens/`
  - `home/`: Main list, top chips, top bar, detail pane, fast-scroll, confetti
  - `onboarding/`: Onboarding pages
  - `settings/`: App configuration (Backup, Labels, Notifications, Calendar)
- **UI Commons**: `ui/model/` (UI State objects), `ui/components/` (shared dialogs, custom illustrations), `ui/theme/` (colors, typography, dimensions)
- **ViewModels**: `viewmodel/` (HomeViewModel, ThemeViewModel, NotificationViewModel, etc.)
- **Widgets**: `widget/` (Glance-based homescreen widgets)

---

## 2. Specialized Skills & Technical Standards

### 2.1 Material 3 Adaptive Layouts
- Use Jetpack Navigation 3's `ListDetailSceneStrategy` / `NavDisplay` for Master-Detail screens (e.g., `HomeScreen`, `SettingsScreen`).
- **WindowSizeClass API**: Use the modern `WindowSizeClass` (from `androidx.window.core.layout`) via `LocalWindowSizeClass.current` or passed parameter. Check breakpoints using the extension properties (e.g., `isWidthCompact`, `isWidthMedium`, `isWidthExpanded`, `isHeightCompact`, etc.) defined in [ResponsiveLayout.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ResponsiveLayout.kt). Do not use the deprecated `WindowWidthSizeClass` or `WindowHeightSizeClass` classes/enums.
- **DO NOT use** the legacy `calculateWindowSizeClass(Activity)` from `androidx.compose.material3.windowsizeclass` – this library has been removed from the project dependencies.
- Compute layout directives using `calculatePaneScaffoldDirective(windowAdaptiveInfo)` from `androidx.compose.material3.adaptive.layout`.
- On wider screens, adapt multi-column layouts and side-by-side panes using adaptive pane configurations.

### 2.2 AGP 9 & Kotlin Gradle DSL
- Use explicit KTS Gradle DSL configuration (`configure<ApplicationExtension>`) instead of implicit `android { ... }` blocks.
- Declare plugins inside the `plugins { ... }` block; do not use legacy `apply plugin`.
- Enforce Java 17 as standard (`jvmToolchain(17)`, `sourceCompatibility` / `targetCompatibility`).
- Enforce strict compiler flags in `gradle.properties`: `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true`.

### 2.3 Type-Safe Navigation (Navigation 3)
- Both global and local navigation are managed via Jetpack Navigation 3 (`NavDisplay`).
- **Destinations / Keys**: All routes/keys MUST be `@Serializable` Kotlin objects or data classes. Global routes are defined in `MainActivity.kt`, and local routes/keys (e.g. `HomeNavKey`, `SettingsNavKey`) are defined in their respective screen files.
- **Back Stack**: The backstack is managed as an observable state list of keys (e.g. `rememberNavBackStack` or a custom backstack state). Programmatic navigation is achieved by mutating this backstack list (e.g., adding keys or popping keys).
- **ViewModel Scoping**: ViewModels MUST be scoped to their respective `NavEntry` by passing `rememberViewModelStoreNavEntryDecorator()` inside the `entryDecorators` of `NavDisplay`. Use `hiltViewModel()` within the `entryProvider` to obtain these scoped ViewModels.
- **ViewModel Arguments**: Since Navigation 3 does not use traditional String bundles, if a ViewModel requires navigation arguments (such as a detail screen contact ID), use **Hilt Assisted Injection** (with a factory and creation callback inside the `NavEntry` block) instead of `SavedStateHandle`.

### 2.4 Edge-to-Edge Support
- Support full Edge-to-Edge rendering on Android 15+.
- Utilize `AppResponsiveScaffold` to automatically pass `PaddingValues` with system window insets to screen contents. Use inset-aware padding on scrollable lists and top/bottom bars.

### 2.5 Proguard & R8 Release Safety
- Protect Room entities, DAOs, Hilt workers, and `@Serializable` navigation routes from code shrinking or stripping by adding explicit keep rules to `proguard-rules.pro`.

### 2.6 Unit Testing Setup
- ViewModels and Repositories MUST be unit tested in `src/test`.
- Use `MainDispatcherRule` to mock the Main dispatcher in coroutines.
- Enable `unitTests.isReturnDefaultValues = true` for JVM tests needing mockable Android properties.

### 2.7 Performance & Compose Optimization
- Inspect recompositions, frame drops, and UI thread blocking using `perfetto-trace-analysis`.
- Offload computationally heavy tasks (e.g., raw database filtering/mapping) to `Dispatchers.Default` using `flowOn`.
- Prefetch images via Coil `enqueue` using explicit memory cache keys to avoid decoding lag during fast scroll.

---

## 3. Agent Skills & Tooling Management

- **Location & Exclusions**:
  - External skill repositories are automatically cloned/downloaded into `.agents/external/` which is ignored by Git via `.gitignore`. Do not commit external code.
- **Automatic Registration**:
  - Skills are automatically copied/synced to `.agents/skills/` under their respective names so they are natively detected by Antigravity.
- **Gradle Tasks**:
  - **Download / Check**: The Gradle task `checkAgentSkills` runs automatically during Android Studio sync (prep stage) and clones the official Google skills ([android/skills](https://github.com/android/skills)) and community Material 3 skill ([hamen/material-3-skill](https://github.com/hamen/material-3-skill)) repositories if missing, and syncs individual skills to `.agents/skills/`.
  - **Manual Update**: Run `.\gradlew updateAgentSkills` to execute `git pull` in both external repositories and sync the updated skills to `.agents/skills/`.
- **Precedence**:
  - The community `material-3` skill is subordinate to all official Google skills. In case of rules contradiction, official Google skills take precedence.
