# 🎂 BirthdayBuddy

> **"Because Google can track your location down to the exact toilet stall you're sitting in, but somehow 'forgets' to notify you when your spouse's wedding anniversary is *today*."**

BirthdayBuddy is a modern, privacy-first, ridiculously over-engineered birthday, wedding anniversary, and name day management app for Android.

---

### 😠 The Backstory: Google vs. Your Relationships

Let’s be honest. Google is exceptional at cataloging humanity's collective knowledge, but when it comes to notifying you of your mother's birthday, it behaves like a distracted teenager who forgot to do their chores. 

One year your birthday reminders live in Google Calendar. The next, they’ve been silently migrated into Google Assistant settings. A few months later, they vanish entirely because a product manager needed a promotion milestone and decided to "streamline" the reminder experience into Google Tasks—which then requires three separate sync toggles and an obscure background battery exemption just to notify you at 4:00 PM when the day is already over.

**BirthdayBuddy was born out of pure, unadulterated engineering spite.** 

It treats your native **Android System Contacts Provider** as the absolute Single Source of Truth (SSOT). We don't host your loved ones' birthdays on some venture-backed cloud that will shut down in 18 months. We don't try to upsell you Google One storage tiers. And we don't drop notifications because an algorithm decided an advertisement was more relevant. If Google's UI breaks, your contact data remains safe inside the Android OS database, and BirthdayBuddy guarantees you will actually see it.

---

## 🚀 Why BirthdayBuddy?

1. **Anti-Ghosting Notification Engine:** Powered by self-rescheduling WorkManager tasks and a hardened `BootReceiver` equipped with `goAsync()`. We don't care about Android's aggressive Doze mode or proprietary OEM battery task-killers—our reminders fire when they are supposed to.
2. **Couch-Avoidance Technology ("Other Events"):** Comprehensive tracking and synchronization for wedding anniversaries and name days. Forgetting your spouse’s anniversary is a one-way ticket to sleeping on the living room sofa; we consider preventing that a core architectural requirement.
3. **Zero Proprietary Database Lock-In:** Your contacts live in the Android Contacts Provider (`ContactsContract`). If you uninstall BirthdayBuddy in a moment of existential crisis, your data remains 100% intact in your phone's address book.
4. **Radical Privacy:** No cloud backends, no telemetry trackers, no analytics SDKs, and no generative AI inspecting your social circle. Just your device, a fast local cache, and a list of people you should probably text.
5. **Obsessively Over-Engineered Build Pipeline:** Built with **AGP 9.x+**, **Java 17 toolchain**, **Navigation 3**, and **Android 16+ AppFunctions**. We purged the third-party Lottie library and replaced it with 100% native Compose vector and shader canvas drawings just to shave 400 KB off the APK. Efficiency isn't just a goal; it's our identity.

---

## ✨ Key Features & Capabilities

* **Ehepaar-Verknüpfung (Couple Coupling):** When two contacts share the same wedding anniversary, BirthdayBuddy intelligently couples them into a unified anniversary card. They receive a single combined push notification, a merged calendar event, and unified gift ideas—eliminating duplicate notification spam and double-gift panic.
* **Triple System Calendar Sync:** Decouples event sync into three dedicated local system calendars: **Birthdays** (🌸 Pink), **Anniversaries** (🍇 Purple), and **Name Days** (🍊 Orange). Created under a custom `"BirthdayBuddy"` local account via `CalendarContract` so it never gets corrupted or wiped by external cloud sync adapters.
* **Android 16+ AppFunctions (AI Agent Interface):** Exposes atomic on-device capabilities to Google Assistant, Gemini, and system AI via `@AppFunction` primitives (e.g. querying upcoming birthdays, looking up dates, or deep-linking to pre-filled contact editors) without opening the app UI.
* **100% Native Compose Illustrations (The Lottie Purge):** Fired external animation libraries in favor of GPU-accelerated, pure Jetpack Compose vector animations and animated shaders. Zero external runtime bloat, zero deprecation risks.
* **Google Photos-Style FastScrollbar:** High-precision sub-pixel scrollbar with a dedicated `FastScrollState` engine. Features an animated `Popup` label bubble, haptic section feedback, an automatic 1.5-second inactivity fade-out, and transparent touch pass-through when idle.
* **Adaptive Multi-Pane & Foldable Architecture:** Fully optimized for phones, foldables, tablets, and Chromebooks using Navigation 3's `ListDetailSceneStrategy` and `WindowAdaptiveInfo`. Features an adaptive navigation drawer/rail, posture-aware hinge handling, and responsive Master-Detail split panes.
* **Dynamic HSV Material 3 Theming:** Don't want the default purple? Pick any color under the sun with our HSV Color Picker. Powered by `material-color-utilities`, it generates a full, dynamic Material 3 color palette from a single HEX code, complete with true AMOLED pure black support and WCAG-AA compliant contrast tokens.
* **Token-Agnostic Smart Search:** Search for `"Doe John"` or `"John Doe"`—the search engine tokenizes and matches names regardless of query order or diacritics.
* **Modern Jetpack Glance Homescreen Widget:** Translucent 80% opacity Material 3 surfaces inside a transparent container, responsive layout sizing, granular label filters, and instant reactive updates via `BirthdayWidgetWorker`.
* **Leap-Year Resilience (Feb 29):** True leap-year safety with `NO_YEAR_MARKER = 4`. Contacts born on February 29 without a recorded year are seamlessly preserved and projected to February 28 in non-leap years across the UI, calendar sync, and notification schedules.

---

## 🏛 Deep-Dive Technical Architecture

BirthdayBuddy follows **Clean Architecture** and **Unidirectional Data Flow (MVI/UDF)** principles, strictly enforcing the separation of concerns across presentation, domain, and data layers.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer (UI)                         │
│   Jetpack Compose BOM • Material 3 Adaptive • Navigation 3 NavDisplay   │
│   Feature-co-located ViewModels • Immutable UI States • Sealed Intents  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Observes Flows / Emits Intents)
┌───────────────────────────────────▼────────────────────────────────────┐
│                             Domain Layer                               │
│     Pure Kotlin Business Logic • Use Cases • AppFunction Primitives    │
│            Domain Models (Contact, GiftIdea, EventType)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Repository Interfaces)
┌───────────────────────────────────▼────────────────────────────────────┐
│                              Data Layer                                │
│  ┌───────────────────────────────┐   ┌──────────────────────────────┐  │
│  │     Android System APIs       │   │       Local Room Caches      │  │
│  │  • ContactsContract (SSOT)    │   │  • AppDatabase (Contacts,    │  │
│  │  • CalendarContract (Sync)    │   │    PendingNotifications)     │  │
│  │  • WorkManager & Broadcasts   │   │  • SettingsDatabase (Rules,  │  │
│  │                               │   │    Labels, UserData, Prefs)  │  │
│  └───────────────────────────────┘   └──────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### 1. Presentation & Navigation (Navigation 3)
* **Navigation 3 Runtime & UI:** Navigation is orchestrated via `NavDisplay` and the type-safe Builder DSL (`entryProvider { entry<T> { ... } }`).
* **Type-Safe Routes:** All navigation keys (`NavRoutes.kt`, `HomeNavKey.kt`) are `@Serializable` Kotlin objects or data classes.
* **Scoped Lifecycle & Memory Safety:** ViewModels are scoped directly to their `NavEntry` using `rememberViewModelStoreNavEntryDecorator()`, guaranteeing automatic memory cleanup when routes are popped from the backstack.
* **Feature-Co-Located MVI:** ViewModels live directly inside their feature directory (`ui/screens/<feature>/`) alongside their screen, state, and intent declarations (`HomeIntent.kt`, `HomeUiState.kt`). ViewModels are completely decoupled from Android UI contexts.

### 2. Dual-Database Persistence & Storage Safety
BirthdayBuddy divides its persistent data across two isolated Room databases:
* **`AppDatabase`:** Acts as a high-performance local cache for contacts fetched from the system `ContentResolver` and tracks active system notifications (`PendingNotification`).
* **`SettingsDatabase`:** Stores persistent user configurations, custom reminder rules (`NotificationRule`), label filter settings (`LabelConfig`), and user-authored metadata such as gift ideas and couple links (`ContactUserData`).
* **Strict Migration Policy:** `fallbackToDestructiveMigration` is strictly forbidden. Every schema modification requires a version bump, an explicit auto-migration (or manual SQL migration), and verified JSON schema exports in `app/schemas` backed by instrumented migration tests.
* **Two-Phase Transactions:** Mutations to cached contact metadata execute via `executeWithSettingsRollback`, ensuring that if a cache update fails, the underlying settings state rolls back safely while preserving Kotlin Coroutines `CancellationException` structured concurrency.

### 3. Resilient Background & Notification Pipeline
* **Doze-Proof Evaluation:** In `GetPendingNotificationsUseCase`, notification rules are evaluated against past and current time intervals (`ruleTime <= currentLocalTime`) combined with database-backed deduplication (`hasNotificationBeenScheduled`), eliminating missed notifications caused by Android Doze mode or OEM execution jitter.
* **System Event Recovery:** `BootReceiver` registers for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET`, and `DATE_CHANGED`. It utilizes `goAsync()` with `@ApplicationScope` coroutine dispatchers to ensure rescheduling completes even if the OS attempts to terminate the broadcast early.
* **Leap-Year Math:** Built-in date arithmetic (`DateUtils.toYear()`) projects leap-day events (Feb 29) to Feb 28 during non-leap years so notifications and calendar events never drop silently.

### 4. Image Loading & Fast-Scroll Optimization (Coil 3)
* **Coil 3 Engine:** Built on `io.coil-kt.coil3` with OkHttp networking.
* **Deterministic Cache Keys:** Avatars use explicit, deterministic cache keys (`avatar_${lookupKey}_${imageUri}`) across both memory and disk caches.
* **Pre-emptive Prefetching:** The contact list automatically prefetches and decodes upcoming avatar bitmaps during list emission via `context.imageLoader.enqueue()`, ensuring zero dropped frames and a consistent 120 FPS during rapid flings with the `FastScrollbar`.

### 5. Android 16+ AppFunctions Architecture
* Built on `androidx.appfunctions:1.0.0-alpha11` with compile-time KSP code generation (`BirthdayBuddyGeneratedAppFunctionService`).
* Exposes four system-accessible operations:
  1. `getUpcomingBirthdays(withinDays)`: Queries upcoming birthdays within an arbitrary day window.
  2. `getContactBirthday(contactName)`: Performs a fuzzy case-insensitive contact lookup.
  3. `sendBirthdayMessage(contactId, app)`: Generates a messaging `PendingIntent` for WhatsApp, Signal, Telegram, or SMS.
  4. `addBirthdayToContact(contactId, year, month, day)`: Prepares a secure in-app deep link for user confirmation.

---

## 🛠 Tech Stack (Evergreen via Version Catalog)

> [!IMPORTANT]
> All dependencies, plugins, and compiler tools are centralized in the Gradle Version Catalog ([`gradle/libs.versions.toml`](gradle/libs.versions.toml)) as the Single Source of Truth (SSOT).

| Category | Technologies / Libraries |
|---|---|
| **Build & Toolchain** | Android Gradle Plugin 9.4+, Kotlin 2.4+, KSP 2.3+, Java 17 (`jvmToolchain(17)`), Kotlin DSL |
| **UI Framework** | Jetpack Compose BOM (2026.08.00), Material 3, Material 3 Adaptive Layouts |
| **Navigation** | Jetpack Navigation 3 (`navigation3-runtime`, `navigation3-ui`, `adaptive-navigation3`) |
| **Architecture** | Clean Architecture, MVI / UDF, Kotlin Coroutines, StateFlow / SharedFlow |
| **Dependency Injection** | Dagger Hilt 2.60+ with KSP, Hilt Navigation Compose, Hilt WorkManager |
| **Persistence** | Room 2.8+ with KSP & Schema Exports, KotlinX Serialization JSON |
| **Background Processing**| AndroidX WorkManager 2.11+ (`@AssistedInject` Workers), Broadcast Receivers with `goAsync()` |
| **Homescreen Widgets** | Jetpack Glance 1.2+ (`glance-appwidget`, `glance-material3`) |
| **Image Loading** | Coil 3.6+ (`coil-compose`, `coil-network-okhttp`) with Okio DiskCache |
| **On-Device AI** | Android AppFunctions 1.0.0-alpha11 (`androidx.appfunctions` API 36+) |
| **Design System** | `material-color-utilities` (Dynamic M3 Color Generation), Native Canvas Animations |
| **Testing** | JUnit 4, Roborazzi 1.73+ (Screenshot Tests), Robolectric 4.16+, MockK, Mockito-Kotlin, Truth |
| **Optimization** | Baseline Profiles (`androidx.baselineprofile 1.5.0-rc02`), ProfileInstaller, R8 Shrinking |

---

## 🧪 Testing & Quality Assurance

BirthdayBuddy maintains a comprehensive test suite across three distinct testing tiers:

```
app/src/
├── test/                          # JVM Unit Tests & Roborazzi Screenshot Tests (~325+ tests)
│   ├── java/.../data/             # Repository, Mapper, and DataSource unit tests
│   ├── java/.../domain/           # UseCase, FilterLogic, and AppFunction service tests
│   ├── java/.../ui/screens/       # Feature ViewModel and MVI Intent unit tests
│   ├── java/.../screenshot/       # Roborazzi Golden-Image visual regression tests
│   └── snapshots/images/          # Version-controlled screenshot golden images
└── androidTest/                   # Instrumented Android Tests (Run on real device or AVD)
    ├── java/.../data/local/       # Room Database migration tests (V5 -> V10)
    ├── java/.../data/repository/  # Live ContentResolver and Room repository tests
    └── java/.../ui/               # Compose UI interaction and integration tests
```

### Key Gradle Commands

```bash
# Run all local JVM unit tests
./gradlew testDebugUnitTest

# Validate UI layouts against Roborazzi golden images
./gradlew verifyRoborazziDebug

# Record/update golden screenshot images after intentional UI updates
./gradlew recordRoborazziDebug

# Generate a visual HTML diff report for failed screenshot assertions
./gradlew compareRoborazziDebug

# Run instrumented integration and database migration tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Assemble and verify a fully minified, R8-shrunk release build
./gradlew :app:assembleRelease
```

---

## 📦 Architecture Rules & Code Guidelines

To maintain architectural integrity and predictability, all contributions adhere to these core rules:

1. **Android Contacts as Authority:** The Android Contacts Provider is the absolute Single Source of Truth (SSOT). Local databases serve purely as caches and configuration stores.
2. **Database Migration Safety:** `fallbackToDestructiveMigration` is prohibited. Every database schema modification requires a version bump, a migration path, and an updated schema export in `app/schemas`.
3. **No Layout Jumps (Padding Rule):** Additive vertical paddings are forbidden. Spacing between dynamic items must be declared using `bottom-padding` to prevent visual jumping during `AnimatedVisibility` expansions.
4. **Dimension Tokens Only:** All dimensions, icon sizes, and corner radiuses must reference centralized tokens in `Dimensions.kt` and `Shapes.kt`—no hardcoded magic numbers in UI composables.
5. **Synchronized Internationalization (I18n):** Any string added or altered must be updated simultaneously in both [`res/values/strings.xml`](app/src/main/res/values/strings.xml) (English) and [`res/values-de/strings.xml`](app/src/main/res/values-de/strings.xml) (German).
6. **Build Hardening & R8 Optimization:** The build enforces strict R-class hardening (`android.nonTransitiveRClass=true`, `android.nonFinalResIds=true`) and surgical ProGuard rules, shrinking the production release APK to **< 6.0 MB**.

---

**Developed with ❤️ and pure spite – Because Google forgetting birthdays is so 2023.**
