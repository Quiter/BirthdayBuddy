# Project Status: BirthdayBuddy

> **TL;DR:** Moderne Material 3 App. Android-Kontakte = SSOT. Room = Cache & Settings. Clean Architecture (Hilt, Coroutines, Flow, Compose, Navigation 3).

## 🛠 Leitplanken (Regeln & Constraints)
1. **Dokumentation:** Änderungen *zwingend* in `PROJECT_STRUCTURE.md` & `CHANGELOG.md` (nur Anhängen!) loggen.
2. **I18n:** `strings.xml` immer in **DE & EN** gleichzeitig pflegen.
3. **Database Safety:** Schema-Änderung = Version-Bump + Auto-Migration. `fallbackToDestructiveMigration` verboten. Schema-Exports in `app/schemas` einchecken.
4. **Architektur:** Feature-basierte Layer. Feature-co-located ViewModels per `NavEntry` gesealed. DI via Hilt.
5. **UI-Prinzipien:** Kontakte nach "Tagen bis Geburtstag" sortiert. Tap-to-Expand Cards (kein Swipe).
6. **Padding-Regel:** Keine additiven vertikalen Paddings. Abstand via `bottom-padding` zur Vermeidung von Layout-Sprüngen bei `AnimatedVisibility`.
7. **Threading & Dispatcher:** Keine hardcodierten Dispatcher (`withContext(Dispatchers.IO)` verboten). Ausschließlich injizierte Qualifier (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`) verwenden.
8. **Tests:** Vor Releases: `./gradlew test connectedDebugAndroidTest` & Screenshot-Verifikation via Roborazzi.

## 🏗 Technischer Stack (Evergreen via `libs.versions.toml`)
- **Version Catalog:** Alle Abhängigkeiten werden über [`gradle/libs.versions.toml`](file:///gradle/libs.versions.toml) (SSOT) zentral und aktuell gehalten.
- **Build:** AGP 9.x+, Java 17 (Toolchain), modernes Gradle, Strict R-Class Flags (`nonTransitiveRClass`, `nonFinalResIds`).
- **UI & Image Loading:** 100% Jetpack Compose mit Material 3, Material 3 Adaptive Layouts (`androidx.compose.material3.adaptive`), Navigation 3 (`NavDisplay`), Coil 3.5.0 (`io.coil-kt.coil3`), Glance 1.2.0-rc01 (Homescreen-Widget).
- **Data & Background:** Room mit KSP & Auto-Migrationen (Schema-Version 11/10), WorkManager, Hilt DI.
- **AI & Integrations:** Android AppFunctions (`platform/appfunctions/` für Android 16+ / Gemini & KI-Agenten).
- **Testing:** JVM Unit-Tests, Roborazzi 1.68.0 Screenshot-Tests, instrumentierte Room-Migrationstests.

## ✨ Kern-Features (Status Quo)
- **Home:** Adaptive List-Detail & Master-Detail Layout, Gmail-Style Suche, adaptive M3 Fade-Out Fast-Scrollbar, Konfetti-Effekt, Fluid M3 Spring Expand-Animation in `BirthdayItem`, Avatar-Preloading mit Coil 3.
- **Events & Paar-Verknüpfung:** Ehepaar-Verknüpfung für Hochzeitstage (2-Phasen-Commit/Rollback), Namenstage-Integration, Multi-Calendar Sync mit Farbauswahl.
- **Granulare Label-Steuerung:** Einzelne Schalter für Benachrichtigungen & Widget-Sichtbarkeit pro Label (DB Migration 9→10), Tablet Label-Sidebar (Rail vs. Drawer).
- **Onboarding:** Adaptiver Flow mit 100% nativen Compose-Illustrationen (Lottie-free).
- **Widget:** Jetpack Glance mit transparentem Hintergrund & transluzenten M3 Karten (80% Opazität), exakter Mitternacht-Alarm (00:01 Uhr).
- **Settings:** Uniformes `SettingsDetailScaffold` für alle 9 Einstellungs-Unter-Screens, Custom Accent Color Picker (HSV-basiert), `ColorPresets.kt` Zentralisierung, Backup/Restore von Geschenkideen.

## 📜 Aktuelle Struktur-Meilensteine
*Details siehe `CHANGELOG.md`*

- **NavEntry ViewModel Scoping & Decoupling:** `HomeViewModel` und `OnboardingViewModel` sind direkt an Navigation 3 `NavEntry` gebunden (`rememberViewModelStoreNavEntryDecorator()`). `MainActivity` ist vollständig entkoppelt und ViewModel-frei.
- **Coil 3.x Migration & Avatar Caching:** Upgrade auf Coil 3.5.0 mit `io.coil-kt.coil3`, deterministische Cache-Keys (`memoryCacheKey`/`diskCacheKey`) und Vorab-Preloading im `HomeScreen` für ruckelfreies Fast-Scrolling.
- **Moderne Adaptive WindowSizeClass API:** Ablösung veralteter `windowsizeclass`-APIs durch `androidx.compose.material3.adaptive` mit `LocalWindowSizeClass` & `LocalWindowAdaptiveInfo` (kein Parameter-Drilling mehr).
- **Granulare Label-Steuerung (DB Migration 9→10):** Aufklappbare Label-Karten mit individueller Steuerung von Benachrichtigungen (`notificationsEnabled`) und Widget-Sichtbarkeit (`showInWidget`).
- **`goAsync()` & `@ApplicationScope` Härtung:** Absicherung asynchroner BroadcastReceiver-Operationen (`BootReceiver`, `NotificationActionReceiver`) durch `goAsync()` und injizierten Hilt `@ApplicationScope` gegen vorzeitigen Prozessabbruch.
- **Fluid M3 Motion & SettingsDetailScaffold:** Stufenlose federbasierte Bildskalierung in `BirthdayItem` und vereinheitlichter Scaffolding-Container für alle 9 Einstellungs-Unter-Screens.
- **Tablet Label-Sidebar:** Dynamisch zwischen Navigation-Rail (80dp) und Drawer (240dp) umschaltbare Sidebar für Filter-Labels auf breiten Bildschirmen.

### 🏛 Frühere Meilensteine (Konsolidiert)
- **Architektur & Robustheit:** MVI-Muster (`HomeIntent`), `IntentExtras.kt` Sicherheits-Härtung, Baseline Profiles (1.5.0-alpha07), Entkopplung der DataSources, Hilt Assist Injection.
- **UI & UX:** Native Compose-Illustrationen (Lottie-Purge für ~400 KB APK-Ersparnis), Google Photos-Style FastScrollbar mit Bubble, Tablet Detail-Pane Placeholder mit Zitat.
- **Features & Services:** Android AppFunctions Integration (API 36+), Glance AppWidget Redesign (1.2.0-rc01), Multi-Calendar Sync & Color Picker, WorkManager-Härtung.

## 🎯 Kommende Aufgaben (Backlog)
- **Polish & Maintenance:** Kontinuierliche Auditierung von Dependency-Updates in `libs.versions.toml`.
- **Performance:** Weiterer Ausbau der Roborazzi-Screenshot-Test-Abdeckung für neue UI-Zustände und Animationen.
- **Plattform:** Evaluation einer WearOS-Komplikation / Tile (Birthday-Countdown).
