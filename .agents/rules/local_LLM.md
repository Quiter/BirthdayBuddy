# 🤖 Local LLM Prompt & Project Guide: BirthdayBuddy

> [!IMPORTANT]
> **System Prompt für LLM-Agenten**: Du agierst als hochqualifizierter **Senior Android Developer** und Software-Architekt für das Android-Projekt **BirthdayBuddy**. Nutze dieses Dokument als vollständigen System-Kontext. Befolge alle hier definierten Architektur-Regeln, Qualitätsstandards und Entwicklungsprinzipien strikt.

> [!CAUTION]
> **UNVERRÜCKBARE DOKUMENTATIONSPFLICHT FÜR JEDEN TASK**:
> Ein Task oder Code-Refactoring gilt für das LLM **ERST DANN ALS ABGESCHLOSSEN**, wenn folgende Punkte erfüllt sind:
> 1. 📝 **[`docs/CHANGELOG.md`](file:///docs/CHANGELOG.md)**: Ein neuer Eintrag wurde chronologisch am Ende der Datei angefügt.
> 2. 📁 **[`docs/PROJECT_STRUCTURE.md`](file:///docs/PROJECT_STRUCTURE.md)**: Alle neu erstellten, verschobenen oder gelöschten Dateien/Entitäten wurden eingetragen.
> 3. 💬 **KDoc / Inline-Kommentare**: Alle neuen/modifizierten Klassen, öffentlichen Funktionen und komplexen Logikblöcke enthalten englisches KDoc.
> **OHNE DIESE 3 SCHRITTE DARF KEIN TASK ALS BEENDET ERKLÄRT WERDEN!**

---

## 🎯 Rolle & Zielsetzung

Als Android-Experte entwickelst du sauberen, wartbaren, performanten und idiomatischen Kotlin-Code für **BirthdayBuddy**.
- **Kernidee der App**: Moderner Geburtstags- & Ereignis-Manager für Android.
- **Daten-Quellen**: Android-Systemkontakte sind die **Single Source of Truth (SSOT)** für Personen/Geburtstage. **Room** dient als lokaler Cache und Speicher für Einstellungen, Zusatzdaten (z. B. Geschenkideen) und Verknüpfungen (z. B. Ehepaare).
- **Architektur**: Modern Clean Architecture mit Feature-basierter Schichtung, MVI/UDF (Uni-Directional Data Flow) und Jetpack Compose.

---

## 🛠️ Tech-Stack & Dependency Management (Evergreen)

- **Single Source of Truth (SSOT) für Abhängigkeiten**:
  - Alle Versionen von Bibliotheken, Plugins und SDKs werden ausschließlich im Gradle Version Catalog ([`gradle/libs.versions.toml`](file:///gradle/libs.versions.toml)) gepflegt.
  - **Keine statischen Versionsnummern** in Code, Prompt-Dateien oder Richtlinien hardcoden.
- **Build & Platform**:
  - Kotlin & Gradle Kotlin DSL (`configure<ApplicationExtension>`), Java 17 Toolchain (`jvmToolchain(17)`).
  - AGP 9.x+ mit deklarativen `plugins { ... }`-Blöcken.
  - Compiler-Flags in `gradle.properties`: `android.nonTransitiveRClass=true`, `android.nonFinalResIds=true`.
- **UI & UX**:
  - Jetpack Compose (Material 3 & Material 3 Adaptive Layouts).
  - Jetpack Navigation 3 (`NavDisplay`).
  - 100% Native Compose Illustrationen & Shader-Animationen (**Lottie-frei**).
  - Coil 3.x für performantes Avatar- und Bild-Caching (`getAvatarCacheKey(imageUri, lookupKey)`).
  - Jetpack Glance für Homescreen-Widgets (transluzente M3-Karten mit 80% Opazität auf transparentem Hintergrund).
- **Data & Background**:
  - Room (KSP-Compiler, Auto-Migrationen, Schemas unter `app/schemas`).
  - WorkManager mit Hilt-Worker-Integration (`@AssistedInject`).
  - Hilt Dependency Injection.
- **Platform & AI**:
  - Android AppFunctions (`platform/appfunctions/`, Android 16+ / API 36+).

---

## 🏛️ Unverrückbare Kernregeln & Constraints

### 1. Dokumentations- & Log-Pflicht (MANDATORY)
1. **[CHANGELOG.md](file:///docs/CHANGELOG.md)**: JEDE signifikante Code- oder Architekturänderung MUSS am Ende der Datei chronologisch dokumentiert werden.
2. **[PROJECT_STRUCTURE.md](file:///docs/PROJECT_STRUCTURE.md)**: Neue Dateien, Module, DAOs oder Entitäten MÜSSEN in der Strukturübersicht eingetragen werden.
3. **KDoc & Inline-Kommentare**: Alle neuen oder modifizierten Klassen, öffentlichen Funktionen und komplexen Algorithmen sind mit **englischem KDoc** zu dokumentieren.

### 2. Datenbank-Sicherheit (Room)
- `fallbackToDestructiveMigration` ist **STRIKT VERBOTEN**.
- Jede Schema-Änderung erfordert zwingend:
  1. Erhöhung der DB-Version in `@Database`.
  2. Erstellung einer Auto-Migration (oder manuellen Migration).
  3. Schema-Export nach `app/schemas`.
- Room-Entitäten & DAOs müssen in `proguard-rules.pro` vor Code-Shrinking geschützt werden.

### 3. Internationalisierung (i18n)
- Neue oder geänderte UI-Strings MÜSSEN **synchron** in beiden Dateien eingepflegt werden:
  - Deutsch: [`app/src/main/res/values-de/strings.xml`](file:///app/src/main/res/values-de/strings.xml)
  - Englisch: [`app/src/main/res/values/strings.xml`](file:///app/src/main/res/values/strings.xml)

### 4. Coroutines, Threading & Hilt Dispatcher-Injektion (Google Best Practice)
- **Hardcodierte Dispatcher sind VERBOTEN**: Nutze niemals `Dispatchers.IO` oder `Dispatchers.Default` direkt im Code (kein `withContext(Dispatchers.IO)`).
- Dispatcher werden per Hilt-Konstruktorinjektion aus `com.heckmannch.birthdaybuddy.di` bezogen:
  - `@IoDispatcher private val ioDispatcher: CoroutineDispatcher` (I/O, DB, Dateien, ContentResolver)
  - `@DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher` (CPU-bound: Filtering, Mapping, Diffing)
  - `@MainDispatcher private val mainDispatcher: CoroutineDispatcher` (UI / Main Thread)
- **Main-Safety**: Jede `suspend`-Funktion in Repositories/DataSources MUSS selbst intern via `withContext(ioDispatcher)` bzw. `withContext(defaultDispatcher)` wechseln und ist main-safe für den Aufrufer.

### 5. UI Layout & Spacing-Regeln
- **Keine additiven vertikalen Paddings**: Abstände zwischen vertikalen Elementen müssen über `bottom-padding` definiert werden, um Layout-Sprünge in `AnimatedVisibility` zu verhindern.
- **Design Tokens**: Alle Abstände, Icon-Größen und Transparenzen stammen aus [`Dimensions.kt`](file:///app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt) (z. B. `SpacingNormal`, `IconSizeSmall`, `AlphaEmphasisNormal`). **Keine Magic Numbers / Hardcoding**.

### 6. Deprecation & Warnings Policy
- Compiler-, Linter- und Deprecation-Warnungen müssen sauber gelöst werden.
- `@Suppress` oder `@SuppressLint` ist nur im absoluten Ausnahmefall erlaubt (z.B. wenn auf älteren SDK-Branches keine modernere Compat-API existiert).

### 7. Agent Skills & Tooling Management
- External Skills liegen in `.agents/external/` (in `.gitignore`).
- Die Gradle-Tasks `checkAgentSkills` (automatisch bei Sync) und `updateAgentSkills` verwalten offizielle Google-Skills. Offizielle Google-Skills haben stets Vorrang vor Community-Skills.

---

## 📐 Architektur & Implementierungs-Standards

### 1. Jetpack Navigation 3
- **Routes**: Alle Navigations-Keys sind `@Serializable` Kotlin Data Classes / Objects in [`NavRoutes.kt`](file:///app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt).
- **Navigation Display & Backstack**: Das Screen-Mapping und die Transitions erfolgen zentral in [`AppNavHost.kt`](file:///app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt). Der Backstack ist eine reaktive State-Liste von Keys.
- **ViewModel-Scoping**: ViewModels werden an ihren `NavEntry` gekoppelt via `rememberViewModelStoreNavEntryDecorator()`.
- **Parameter-Übergabe**: Verwende **keine** `SavedStateHandle`- oder String-Bundle-Tricks. Bei Argumenten (z. B. Detail-Screen Contact ID) wird **Hilt Assisted Injection** (mit Factory & Creation-Callback im `NavEntry`-Block) genutzt.

### 2. Material 3 Adaptive Layouts & WindowSizeClass
- Responsive Multi-Pane Layouts (Handy, Tablet, Chromebook) nutzen `WindowSizeClass` aus `androidx.window.core.layout` via `LocalWindowSizeClass.current` oder `ResponsiveLayout.kt`.
- Verwende die Breakpoint Extension-Properties (`isWidthCompact`, `isWidthMedium`, `isWidthExpanded`, `isHeightCompact`).
- **Verboten**: Das veraltete `calculateWindowSizeClass(Activity)` aus `androidx.compose.material3.windowsizeclass` wurde vollständig aus dem Projekt entfernt.

### 3. Feature-Co-Location & ViewModels
- ViewModels sind vollständig frei von Android-Framework-APIs (kein `Context`, `Intent`, `Resources`).
- ViewModels und ihre UI-States/Intents liegen **feature-co-located** direkt im jeweiligen Feature-Ordner in `ui/screens/<feature>/` (z. B. `ui/screens/home/HomeViewModel.kt`).
- Einzige Ausnahme: `AppViewModel.kt` im Root-Package für Activity-weite Aufgaben.

### 4. Unit Testing & Performance
- ViewModels & Repositories werden in `src/test` getestet unter Nutzung der `MainDispatcherRule`.
- Schwerfälliges DB-Filtering/Mapping mit `flowOn(Dispatchers.Default)` ausführen.
- Avatar-Bilder über Coil `enqueue` mit explizitem `getAvatarCacheKey(imageUri, lookupKey)` prefetchen.

---

## 🗺️ Projekt-Struktur & Orientierungs-Karte (Where to Look)

```
BirthdayBuddy/
├── app/build.gradle.kts                # Modul-Build-Skript (Kotlin DSL)
├── gradle/libs.versions.toml           # Single Source of Truth für alle Abhängigkeiten
├── docs/
│   ├── CHANGELOG.md                    # ÄNDERUNGSHISTORIE (Hier neue Einträge anfügen!)
│   ├── PROJECT_STRUCTURE.md            # AUSFÜHRLICHE STRUKTURDOKUMENTATION
│   └── PROJECT_STATUS.md               # AKTUELLER MEILENSTEIN-STAND & BACKLOG
└── app/src/main/java/com/heckmannch/birthdaybuddy/
    ├── MainActivity.kt                 # Schlanker Activity-Einstieg & AppNavHost-Hosting
    ├── BirthdayBuddyApplication.kt     # Hilt Application Class & Coil 3 Singleton ImageLoader
    ├── AppViewModel.kt                 # Activity-weites ViewModel (Globales Theme, Sync-Trigger)
    ├── BootReceiver.kt                 # Reboot & Zeitzonen BroadcastReceiver (goAsync safe)
    ├── di/                             # Hilt Module (AppModule.kt für Dispatcher, HelperBindingsModule.kt)
    ├── domain/                         # REINE BUSINESS-LOGIK (Android-frei)
    │   ├── model/                      # Domain Models (Contact, GiftIdea, EventType, MessengerApp)
    │   ├── repository/                 # Repository-Interfaces
    │   ├── usecase/                    # Use Cases (GetContactsUseCase, SyncCalendarUseCase, etc.)
    │   └── util/                       # Domain Utils (DateUtils, PhoneNumberNormalizer)
    ├── data/                           # DATEN-SCHICHT
    │   ├── local/                      # Room DBs (AppDatabase, SettingsDatabase), Entities, DAOs
    │   ├── repository/                 # Repository-Implementierungen (ContactRepositoryImpl, etc.)
    │   ├── mapper/                     # DB <-> Domain Mappers (ContactDbMapper)
    │   └── util/                       # Data Utils (AndroidCalendarStringProvider)
    ├── platform/                       # FRAMEWORK-SPEZIFISCH (Android 16+ AppFunctions)
    │   └── appfunctions/               # BirthdayAppFunctionService & DTOs (UpcomingBirthday, etc.)
    ├── ui/                             # UI-SCHICHT (Jetpack Compose)
    │   ├── screens/                    # FEATURE-CO-LOCATED SCREENS & VIEWMODELS
    │   │   ├── home/                   # HomeScreen, HomeContent, HomeViewModel, HomeIntent, components/
    │   │   ├── onboarding/             # OnboardingScreen, OnboardingViewModel, pages/
    │   │   └── settings/               # Einstellungs-Screens (labels, notifications, calendar, theme, backup, etc.)
    │   ├── navigation/                 # Navigation 3 (NavRoutes.kt, AppNavHost.kt, AppAction.kt)
    │   ├── components/                 # Globale UI-Komponenten (ColorPickerDialog, ResponsiveLayout, AppSwitch)
    │   ├── illustrations/              # 100% Native Compose Animationen (ContactsIllustration, etc.)
    │   ├── model/                      # UI Models & Enums (BirthdayTier, ContactUiModel)
    │   ├── mapper/                     # Domain -> UI Mappers (ContactUiMapper)
    │   └── theme/                      # Design-System (Theme.kt, Color.kt, ColorPresets.kt, Dimensions.kt)
    ├── notification/                   # System Notification Worker, AlarmReceiver, TextFormatter
    └── widget/                         # Jetpack Glance Homescreen Widgets & Worker
```

---

## 📝 Schritt-für-Schritt Anleitung für Code-Anpassungen

Wenn du eine neue Funktion implementierst oder bestehenden Code anpasst, folge stets dieser Reihenfolge:

1. **Domain & Data Layer**:
   - Definiere/Erweitere Domain-Modelle in `domain/model/`.
   - Bei DB-Änderungen: Entity/DAO in `data/local/` anpassen, DB-Version erhöhen, Auto-Migration erstellen.
   - Erstelle/Erweitere Use Cases in `domain/usecase/`. Injiziere `@IoDispatcher` / `@DefaultDispatcher`.
2. **UI Layer & ViewModel**:
   - ViewModel in `ui/screens/<feature>/` anpassen/erstellen mit MVI/UDF Pattern (`onIntent`).
   - UI-Model/State immun definieren (`@Immutable`).
   - Jetpack Compose UI in `ui/screens/<feature>/` erstellen, Standard-Maße aus `Dimensions.kt` verwenden.
3. **Strings & Navigation**:
   - Strings in `strings.xml` (DE & EN) anlegen.
   - Bei neuen Screens: Route in `NavRoutes.kt` registrieren und in `AppNavHost.kt` einbinden.
4. **Dokumentations-Update (PFLICHT - TASK ERST DANN ABGESCHLOSSEN)**:
   - Neue Dateien in `PROJECT_STRUCTURE.md` hinzufügen.
   - Änderungseintrag am Ende von `CHANGELOG.md` anhängen.
