---
name: projekt-kontext
description: Lädt die grundlegenden Projekt-Richtlinien, den Tech-Stack, die Architektur-Ebenen und die Ordnerstruktur von BirthdayBuddy. Dient als zentraler Einstiegspunkt für Feature-Planung und allgemeine Konventionen.
---

# BirthdayBuddy: Projekt-Kontext & Architektur-Grundlagen

Dieses Dokument dient als schlanker Einstiegspunkt für die Entwicklung von Features und Code-Anpassungen im **BirthdayBuddy**-Projekt. Es definiert den Technologiestack, die Clean-Architecture-Ebenen und Namenskonventionen.

Für tiefgehende Detailthemen existieren spezialisierte Skills, die gezielt geladen werden können (siehe unten).

---

## 🛠️ Technologiestack & Versions-Management (Evergreen)

> [!IMPORTANT]
> **Single Source of Truth (SSOT) für Abhängigkeiten**:
> Alle Bibliotheks-, Plugin- und SDK-Versionen werden ausschließlich im Gradle Version Catalog ([`gradle/libs.versions.toml`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/gradle/libs.versions.toml)) gepflegt.
> In Richtlinien und Prompts werden **keine statischen Versionsnummern festgeschrieben**. Das Projekt wird kontinuierlich auf aktuellen, zueinander kompatiblen stabilen Versionen gehalten (Evergreen-Prinzip).

- **Plattform & Build**:
  - **Gradle Version Catalog**: Zentraler Einstiegspunkt (`gradle/libs.versions.toml`).
  - **Java / JVM Toolchain**: Java 17 (`jvmToolchain(17)`).
  - **Gradle & AGP**: Modernes Gradle & Android Gradle Plugin (AGP 9.x+ deklaratives Plugin-Management).
  - **Gradle DSL**: Ausschließlich Kotlin DSL mit typsicheren Accessoren. Legacy `apply plugin` ist verboten; alle Plugins im `plugins { ... }`-Block deklarieren.
  - **Compiler-Flags** (in `gradle.properties` erzwungen):
    - `android.nonTransitiveRClass=true` (R-Klassen nicht transitiv für maximale Build-Performance)
    - `android.nonFinalResIds=true` (Ressourcen-IDs sind nicht final)
- **Architektur-Pattern**: Clean Architecture (Feature-based Layering) & MVI/UDF (Uni-Directional Data Flow) mit `@Immutable` UI-Modellen und Screen-spezifischen ViewModels.

---

## 📁 Projekt-Struktur & Namenskonventionen

### Namenskonventionen
- **ViewModels**: Suffix `ViewModel` (z.B. `HomeViewModel.kt`), platziert im jeweiligen Feature-Ordner.
- **UI-Screens & Layouts**: Hauptkomponenten enden auf `Screen` (z.B. `HomeScreen.kt`). Reine Layout-Komponenten auf `Content` (z.B. `HomeContent.kt`).
- **UI-Modelle**: Suffix `UiModel` (z.B. `ContactUiModel.kt`) oder `UiState` (z.B. `HomeUiState.kt`).
- **Ressourcen**: Standardmäßig Kleinschreibung mit Snake-Case (`ic_launcher.xml`, `strings.xml`).

### Verzeichnisstruktur (`com.heckmannch.birthdaybuddy`)
- **`di/`**: Hilt-Konfigurationsmodule und Dispatcher-Qualifiers (`AppModule.kt`, `Qualifiers.kt`).
- **`domain/`**:
  - **`model/`**: Reine Kotlin-Geschäftsmodelle frei von Android-Framework-Abhängigkeiten (`Contact.kt`, `GiftIdea.kt`).
  - **`usecase/`**: Fachliche Anwendungsfälle (`GetContactsUseCase.kt`, `SyncCalendarUseCase.kt`).
  - **`util/`**: Fachliche Hilfsfunktionen (`DateUtils.kt`, `PhoneNumberNormalizer.kt`).
  - **`permission/`**: Plattformunabhängige Schnittstellen für Berechtigungsprüfungen (`PermissionChecker.kt`).
- **`platform/`**:
  - **`appfunctions/`**: Android-Framework-spezifischer `BirthdayAppFunctionService` (API 36+ Shortcuts / On-Device AI).
- **`data/`**:
  - **`local/`**: Room-Datenbanken (`AppDatabase`, `SettingsDatabase`), Entitäten und DAOs.
  - **`repository/`**: Repository-Implementierungen (`ContactRepositoryImpl`, `CalendarSyncRepositoryImpl`).
  - **`mapper/`**: Mappings von DB-Entitäten zu Domain-Modellen (`ContactDbMapper`).
- **`ui/`**:
  - **`screens/`**: Feature-basierte Verzeichnisse (`home/`, `settings/`, `onboarding/`). Screen, Content, ViewModel, State und Events sind feature-co-located.
  - **`navigation/`**: Navigation 3 Routen (`NavRoutes.kt`) und Nav-Host (`AppNavHost.kt`).
  - **`components/`**: Globale UI-Komponenten (`ResponsiveLayout.kt`, `ColorPickerDialog.kt`).
  - **`illustrations/`**: Native Compose-Animationen (`ContactsIllustration.kt`).
  - **`theme/`**: Design-System (Farben, Typografie, Formen, `Dimensions.kt`).
- **`widget/`**: Glance App-Widget (`BirthdayWidget.kt`, `BirthdayWidgetWorker.kt`).
- **`notification/`**: Benachrichtigungslogik, Worker und Alarm-Receiver.

---

## 🎯 Spezialisierte BirthdayBuddy Skills

Für spezifische Aufgaben existieren modulare Skills, die gezielt vom Modell geladen werden können:

| Skill | Zweck & Aktivierung |
|---|---|
| **`bb-compose-ui`** | UI-Entwicklung, Composables, Spacings/Bottom-Padding, `Dimensions.kt`, Responsive Layouts (Tablets), Insets, Coil-Bildvorabladung |
| **`bb-navigation`** | Jetpack Navigation 3, `NavRoutes.kt`, `AppNavHost.kt`, ViewModels an NavEntry koppeln, Hilt Assisted Injection |
| **`bb-room-database`** | Room Entities, DAOs, Migrationen (kein Destructive Migration!), Schema Export nach `app/schemas` |
| **`bb-coroutines-threading`** | Coroutines, Hilt Dispatcher-Injektion (`@IoDispatcher`, `@DefaultDispatcher`), Main-Safety, Threading-Regeln |
| **`bb-testing-qa`** | Unit Tests (`MainDispatcherRule`), Roborazzi Screenshot-Tests, Quality Gates |
| **`bb-i18n-docs`** | Lokalisierung (synchrone Pflege von `values-de` und `values`), `CHANGELOG.md`, `PROJECT_STRUCTURE.md`, KDoc |
| **`bb-glance-widget`** | Homescreen-Widget mit Jetpack Glance, M3-Styling (transluzent), Widget-Updates via Worker/Alarm |

---

## 🔄 Synchronisations-Regel
Sobald neue Architektur-Muster, Bibliotheken oder globale UI-Komponenten finalisiert und getestet wurden, MUSS die entsprechende Dokumentation und Richtlinie eigenständig aktualisiert werden, bevor der Task als abgeschlossen gilt.
