---
name: projekt-kontext
description: Lädt die unveränderlichen Projekt-Richtlinien, den Tech-Stack und die Ordnerstruktur von BirthdayBuddy für die Entwicklung neuer Features.
---

# BirthdayBuddy Projekt-Kontext & Richtlinien

Dieses Dokument dient als systemischer Kontext für die Entwicklung von Features und Code-Anpassungen im **BirthdayBuddy**-Projekt. Es enthält alle unveränderlichen Architekturregeln, den Technologiestack und Richtlinien zur Einhaltung der Code-Qualität.

---

## 🛠️ Technologiestack & Build-Konfiguration

- **Plattform & Build**:
  - **SDK / Java**: Java 17 (`jvmToolchain(17)`, `sourceCompatibility` / `targetCompatibility`).
  - **Gradle / AGP**: Gradle 9.5, Android Gradle Plugin (AGP) 9.2.1.
  - **Gradle DSL**: Ausschließlich Kotlin DSL mit expliziter Konfiguration (`configure<ApplicationExtension>`). Legacy `apply plugin` ist verboten; alle Plugins im `plugins { ... }`-Block deklarieren.
  - **Compiler-Flags** (in `gradle.properties` erzwungen):
    - `android.nonTransitiveRClass=true` (R-Klassen nicht transitiv)
    - `android.nonFinalResIds=true` (Ressourcen-IDs sind nicht final)
- **UI & UX**:
  - **Framework**: Jetpack Compose (BOM 2026.05.01), Material 3 Adaptive Layouts.
  - **Homescreen-Widgets**: Jetpack Glance (mit transparentem Container & 80% transluzenten M3-Karten).
  - **Animationen**: 100% native Compose-Illustrationen und animierte Shader (vollständiger Verzicht auf Lottie für minimierte APK-Größe).
- **Data & Architecture**:
  - **Datenbank**: Room (2.8.4) für Caching & Einstellungen.
  - **Hintergrund-Tasks**: WorkManager (2.11).
  - **Dependency Injection**: Hilt (2.59).
  - **Architektur-Pattern**: Clean Architecture (Feature-based Layering) & MVI/UDF (Uni-Directional Data Flow) mit `@Immutable` UI-Modellen und Screen-spezifischen ViewModels.

---

## 🏛️ Architektur- & Entwicklungsregeln

### 1. Architektur & Navigation (Navigation 3)
- **Navigation**: Verwaltet über Jetpack Navigation 3 (`NavDisplay`).
  - Alle Routen/Keys müssen `@Serializable` Kotlin-Objekte oder Data Classes sein (globale Keys in [MainActivity.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/MainActivity.kt), lokale Keys in den jeweiligen Screen-Dateien).
  - Der Backstack wird als observable State-Liste von Keys verwaltet und per Mutation (Add/Pop) gesteuert.
- **ViewModels**:
  - Müssen vollständig von Android APIs entkoppelt sein.
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
  - Alle UI-Maße (Spacings, Icons, Opazitäten) müssen aus [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/ui/theme/Dimensions.kt) stammen (keine Hardcoded/Magic-Values).
- **Responsive Layouts**:
  - Multi-Column-Layouts auf Tablets/Chromebooks mittels `currentWindowAdaptiveInfoV2()` und `calculatePaneScaffoldDirective(windowAdaptiveInfo)` realisieren (Master-Detail).
  - Volle Edge-to-Edge Unterstützung (Android 15+) unter Verwendung von `AppResponsiveScaffold` für automatische System-Insets.
- **Bilder & Scrolling**:
  - Coil-Bilder vorab über `enqueue` mit expliziten Memory-Cache-Keys laden, um Scroll-Lag in Fast-Scroll-Listen zu verhindern.

### 4. Internationalisierung (I18n) & Dokumentation
- **I18n**: Jedes neue oder geänderte String-Ressource-Element muss zeitgleich in [strings.xml (DE)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/res/values-de/strings.xml) und [strings.xml (EN)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/res/values/strings.xml) eingepflegt werden.
- **Dokumentations-Pflicht**:
  - Signifikante Code-Änderungen *zwingend* an [CHANGELOG.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/CHANGELOG.md) anhängen.
  - Neue Dateien, Module oder DB-Entitäten in [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md) registrieren.
  - KDoc (Englisch) und inline Kommentare für alle neuen/modifizierten Klassen, public Funktionen und komplexe Logikblöcke zur Erklärung von Entwurfsentscheidungen nutzen.

### 5. Testing & Qualitätssicherung
- **Unit Tests**: ViewModels und Repositories müssen in `src/test` abgedeckt sein (unter Nutzung der `MainDispatcherRule`).
- **Pre-Release Check**: Vor jedem Release müssen die Tests via `./gradlew test connectedDebugAndroidTest` lokal erfolgreich durchgelaufen sein.

---

## 📁 Projekt-Struktur & Namenskonventionen

### Namenskonventionen
- **ViewModels**: Suffix `ViewModel` (z.B. `HomeViewModel.kt`).
- **UI-Screens & Layouts**: Hauptkomponenten enden auf `Screen` (z.B. `HomeScreen.kt`). Reine Layout-Komponenten auf `Content` (z.B. `HomeContent.kt`).
- **UI-Modelle**: Suffix `UiModel` (z.B. `ContactUiModel.kt`) oder `UiState` (z.B. `HomeUiState.kt`).
- **Ressourcen**: Standardmäßig Kleinschreibung mit Snake-Case (`ic_launcher.xml`, `strings.xml`).

### Verzeichnisstruktur
- **`di/`**: Hilt-Konfigurationsmodule (z.B. [AppModule.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/di/AppModule.kt)).
- **`domain/`**:
  - **`model/`**: Reine Kotlin-Geschäftsmodelle (z.B. `Contact`, `GiftIdea`).
  - **`usecase/`**: Fachliche Anwendungsfälle / Use Cases (z.B. `GetContactsUseCase`).
- **`data/`**:
  - **`local/`**: Room-Datenbank, Entitäten (`ContactEntity`, `AppSettings`, `NotificationRule`, `PendingNotification`) und DAOs.
  - **`repository/`**: Orchestriert den Datenfluss (z.B. `ContactRepository`, `CalendarSyncRepository`).
  - **`mapper/`**: Mappings von DB-Entitäten zu Domain-Modellen (`ContactDbMapper`) sowie UI-Modellen (`ContactMapper`).
- **`ui/`**:
  - **`screens/`**: Feature-basierte Verzeichnisse (z.B. `home/`, `onboarding/`, `settings/`). Jedes Feature beherbergt Screen, Content, State, Actions und Sub-Komponenten.
  - **`model/`**: Immutable UI-Modelle, Enums (z.B. `BirthdayTier`, `EventType`) und UI-States.
  - **`components/`**: Globale, wiederverwendbare Custom-Komponenten (z.B. `ColorPickerDialog.kt`).
  - **`illustrations/`**: Native Compose-Animationen (`ContactsIllustration.kt`).
  - **`theme/`**: Jetpack Compose Design-System (Farben, Typografie, Formen, [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/ui/theme/Dimensions.kt)).
- **`viewmodel/`**: Zentraler Ort für alle Screen-ViewModels (veraltet, da ViewModels gemäß Clean Architecture idealerweise nahe bei den Screens liegen; existierende ViewModels verbleiben hier vorerst).
- **`util/` & `ui/util/`**: Allgemeingültige Hilfsklassen (z.B. `DateUtils.kt`, `IntentExtras.kt`).
- **`widget/`**: Glance App-Widget Implementierung.

## Synchronisations-Regel
Sobald neue Architektur-Muster, Bibliotheken oder globale UI-Komponenten (Jetpack Compose) finalisiert und getestet wurden, MUSS der Agent die entsprechende `SKILL.md` im Ordner `.agents/skills/projekt-kontext/` eigenständig aktualisieren, bevor der Task als abgeschlossen gilt.
