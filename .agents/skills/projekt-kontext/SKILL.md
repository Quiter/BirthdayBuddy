---
name: projekt-kontext
description: Lädt die unveränderlichen Projekt-Richtlinien, den Tech-Stack und die Ordnerstruktur von BirthdayBuddy für die Entwicklung neuer Features.
---

# BirthdayBuddy Projekt-Kontext & Richtlinien

Dieses Dokument dient als systemischer Kontext für die Entwicklung von Features und Code-Anpassungen im **BirthdayBuddy**-Projekt. Es enthält alle unveränderlichen Architekturregeln, den Technologiestack und Richtlinien zur Einhaltung der Code-Qualität.

---

## 🛠️ Technologiestack & Versions-Management (Evergreen)

> [!IMPORTANT]
> **Single Source of Truth (SSOT) für Abhängigkeiten**:
> Alle Bibliotheks-, Plugin- und SDK-Versionen werden ausschließlich im Gradle Version Catalog ([`gradle/libs.versions.toml`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml)) gepflegt.
> In Richtlinien und Prompts werden **keine statischen Versionsnummern festgeschrieben**. Das Projekt wird kontinuierlich auf aktuellen, zueinander kompatiblen stabilen Versionen gehalten (Evergreen-Prinzip).

- **Plattform & Build**:
  - **Gradle Version Catalog**: Zentraler Einstiegspunkt für alle Abhängigkeiten und Plugins (`gradle/libs.versions.toml`).
  - **Java / JVM Toolchain**: Java 17 (`jvmToolchain(17)`, `sourceCompatibility` / `targetCompatibility`).
  - **Gradle & AGP**: Modernes Gradle & Android Gradle Plugin (AGP 9.x+ deklaratives Plugin-Management).
  - **Gradle DSL**: Ausschließlich Kotlin DSL mit typsicheren Accessoren (`configure<ApplicationExtension>`). Legacy `apply plugin` ist verboten; alle Plugins im `plugins { ... }`-Block deklarieren.
  - **Compiler-Flags** (in `gradle.properties` erzwungen):
    - `android.nonTransitiveRClass=true` (R-Klassen nicht transitiv für maximale Build-Performance)
    - `android.nonFinalResIds=true` (Ressourcen-IDs sind nicht final)
- **UI & UX**:
  - **Framework**: Jetpack Compose mit aktuellem Compose BOM, Material 3 & Material 3 Adaptive Layouts.
  - **Homescreen-Widgets**: Jetpack Glance (mit transparentem Container & 80% transluzenten M3-Karten).
  - **Animationen**: 100% native Compose-Illustrationen und animierte Shader (vollständiger Verzicht auf Lottie für minimierte APK-Größe).
- **Data & Architecture**:
  - **Datenbank & Caching**: Room mit KSP-Compiler für robustes Caching & persistente Einstellungen.
  - **Hintergrund-Tasks**: WorkManager mit Hilt-Worker-Integration (`@AssistedInject`).
  - **Dependency Injection**: Hilt mit KSP-Unterstützung.
  - **On-Device AI / System-Shortcuts**: Android AppFunctions (`domain/appfunctions/`, API 36+).
  - **Architektur-Pattern**: Clean Architecture (Feature-based Layering) & MVI/UDF (Uni-Directional Data Flow) mit `@Immutable` UI-Modellen und Screen-spezifischen ViewModels.

---

## 🏛️ Architektur- & Entwicklungsregeln

### 1. Architektur & Navigation (Navigation 3)
- **Navigation**: Verwaltet über Jetpack Navigation 3 (`NavDisplay`).
  - Alle Routen/Keys müssen `@Serializable` Kotlin-Objekte oder Data Classes sein ([`NavRoutes.kt`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt)).
  - Die Navigationsstruktur und das Screen-Mapping werden zentral in [`AppNavHost.kt`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt) verwaltet.
  - Der Backstack wird als observable State-Liste von Keys verwaltet und per Mutation (Add/Pop) gesteuert.
- **ViewModels**:
  - Müssen vollständig von Android APIs entkoppelt sein.
  - Liegen **feature-co-located** direkt im jeweiligen Feature-Ordner unter `ui/screens/<feature>/` (Ausnahme: `AppViewModel.kt` im Root-Package für Activity-weite Aufgaben).
  - ViewModels werden via `hiltViewModel()` bezogen und müssen über `rememberViewModelStoreNavEntryDecorator()` an den jeweiligen `NavEntry` gekoppelt werden.
  - **Parameter-Übergabe**: Keine String-Bundles/`SavedStateHandle` verwenden. Stattdessen **Hilt Assisted Injection** (Factory & Creation-Callback im `NavEntry`-Block) nutzen.

### 2. Datenbank-Sicherheit (Room)
- **Migrations-Sicherheit**:
  - Die Verwendung von `fallbackToDestructiveMigration` ist **verboten**.
  - Jede Schema-Änderung erfordert:
    1. Erhöhung der Version in der Room-Datenbank.
    2. Definition einer Auto-Migration (oder manuellen Migration).
    3. Export des Datenbankschemas nach `app/schemas` für automatisierte Tests.
- **Daten-Modellierung**: Room-Entitäten und DAOs müssen in `proguard-rules.pro` vor Code-Shrinking geschützt werden.

### 3. UI- & Layout-Prinzipien
- **Padding & Spacing**:
  - Keine additiven vertikalen Paddings verwenden. Abstände zwischen Elementen müssen über `bottom-padding` definiert werden, um Layout-Sprünge innerhalb von `AnimatedVisibility` zu verhindern.
  - Alle UI-Maße (Spacings, Icons, Opazitäten) müssen aus [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt) stammen (keine Hardcoded/Magic-Values).
- **Responsive Layouts**:
  - Multi-Column-Layouts auf Tablets/Chromebooks mittels `WindowSizeClass` (aus `androidx.window.core.layout`) über `ResponsiveLayout.kt` und `calculatePaneScaffoldDirective(windowAdaptiveInfo)` realisieren (Master-Detail).
  - Volle Edge-to-Edge Unterstützung (Android 15+) unter Verwendung von `AppResponsiveScaffold` für automatische System-Insets.
- **Bilder & Scrolling**:
  - Coil-Bilder vorab über `enqueue` mit expliziten Memory-Cache-Keys laden, um Scroll-Lag in Fast-Scroll-Listen zu verhindern.

### 4. Internationalisierung (I18n) & Dokumentation
- **I18n**: Jedes neue oder geänderte String-Ressource-Element muss zeitgleich in [strings.xml (DE)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) und [strings.xml (EN)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) eingepflegt werden.
- **Dokumentations-Pflicht**:
  - Signifikante Code-Änderungen *zwingend* an [CHANGELOG.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/CHANGELOG.md) anhängen.
  - Neue Dateien, Module oder DB-Entitäten in [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md) registrieren.
  - KDoc (Englisch) und inline Kommentare für alle neuen/modifizierten Klassen, public Funktionen und komplexe Logikblöcke zur Erklärung von Entwurfsentscheidungen nutzen.

### 5. Testing & Qualitätssicherung
- **Unit Tests**: ViewModels, Use Cases und Repositories müssen in `src/test` abgedeckt sein (unter Nutzung der `MainDispatcherRule`).
- **Screenshot Tests**: JVM-basierte Roborazzi Screenshot-Tests in `src/test/java/.../screenshot/` zur Verifikation von UI-Layouts und Dark/Light-Theming.
- **Pre-Release Check**: Vor jedem Release müssen die Tests via `./gradlew test` (sowie instrumentierte Tests bei DB-/ContentProvider-Änderungen) erfolgreich durchgelaufen sein.

---

## 📁 Projekt-Struktur & Namenskonventionen

### Namenskonventionen
- **ViewModels**: Suffix `ViewModel` (z.B. `HomeViewModel.kt`), platziert im jeweiligen Feature-Ordner.
- **UI-Screens & Layouts**: Hauptkomponenten enden auf `Screen` (z.B. `HomeScreen.kt`). Reine Layout-Komponenten auf `Content` (z.B. `HomeContent.kt`).
- **UI-Modelle**: Suffix `UiModel` (z.B. `ContactUiModel.kt`) oder `UiState` (z.B. `HomeUiState.kt`).
- **Ressourcen**: Standardmäßig Kleinschreibung mit Snake-Case (`ic_launcher.xml`, `strings.xml`).

### Verzeichnisstruktur
- **`di/`**: Hilt-Konfigurationsmodule (z.B. `AppModule.kt`, `HelperBindingsModule.kt`).
- **`domain/`**:
  - **`model/`**: Reine Kotlin-Geschäftsmodelle (z.B. `Contact`, `GiftIdea`, `EventType`).
  - **`usecase/`**: Fachliche Anwendungsfälle / Use Cases (z.B. `GetContactsUseCase`, `GetPendingNotificationsUseCase`).
  - **`appfunctions/`**: Android AppFunctions für KI- und System-Integrationen (`BirthdayAppFunctionService.kt`).
  - **`permission/`**: Plattformunabhängige Schnittstellen für Berechtigungsprüfungen (`PermissionChecker.kt`).
- **`data/`**:
  - **`local/`**: Room-Datenbanken (`AppDatabase`, `SettingsDatabase`), Entitäten (`Contact`, `AppSettings`, `NotificationRule`, `PendingNotification`) und DAOs.
  - **`repository/`**: Orchestriert den Datenfluss (`ContactRepository`, `CalendarSyncRepository`, `NotificationRepository`).
  - **`mapper/`**: Mappings von DB-Entitäten zu Domain-Modellen (`ContactDbMapper`).
  - **`permission/`**: Plattformspezifische Implementierung (`AndroidPermissionChecker.kt`).
- **`ui/`**:
  - **`screens/`**: Feature-basierte Verzeichnisse (`home/`, `onboarding/`, `settings/`). Jedes Feature beherbergt Screen, Content, ViewModel, State, Actions und Sub-Komponenten (feature-co-located).
  - **`navigation/`**: Navigation 3 Routen (`NavRoutes.kt`) und Nav-Host (`AppNavHost.kt`).
  - **`model/`**: Immutable UI-Modelle, Enums (z.B. `BirthdayTier`) und UI-States.
  - **`components/`**: Globale, wiederverwendbare Custom-Komponenten (z.B. `ColorPickerDialog.kt`, `ResponsiveLayout.kt`).
  - **`illustrations/`**: Native Compose-Animationen (`ContactsIllustration.kt`, `WelcomeIllustration.kt`).
  - **`theme/`**: Jetpack Compose Design-System (Farben, Typografie, Formen, `Dimensions.kt`).
- **`util/` & `ui/util/`**: Allgemeingültige Hilfsklassen (`DateUtils.kt`, `IntentExtras.kt`, `ContactActions.kt`).
- **`widget/`**: Glance App-Widget Implementierung (`BirthdayWidget.kt`, `BirthdayWidgetWorker.kt`).

## Synchronisations-Regel
Sobald neue Architektur-Muster, Bibliotheken oder globale UI-Komponenten finalisiert und getestet wurden, MUSS der Agent die entsprechende Dokumentation und Richtlinien eigenständig aktualisieren, bevor der Task als abgeschlossen gilt.
