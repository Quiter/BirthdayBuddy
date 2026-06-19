# Project Guidelines & Context

This rule ensures that the agent always respects the current status, structure, and guidelines of the BirthdayBuddy project.
The project is hosted at: https://github.com/Quiter/BirthdayBuddy

## Project Documents to Consult
Always refer to and follow the specifications, rules, and architecture laid out in:
- [docs/PROJECT_STATUS.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STATUS.md)
- [docs/PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md)

## Core Guidelines
1. **Rules & Constraints:** Always adhere to the "Leitplanken" defined in `docs/PROJECT_STATUS.md` (e.g. database safety, i18n in both German and English, Clean Architecture, Hilt DI, no additive vertical paddings).
2. **Project Structure:** Follow the directory layout and file placements documented in `docs/PROJECT_STRUCTURE.md`.
3. **Documentation:** Log changes strictly in `docs/PROJECT_STRUCTURE.md` and `CHANGELOG.md` (append only).

## Spezialisierte Skills & Tooling
Bei der Weiterentwicklung des Projekts wurden spezialisierte Skills eingesetzt, deren Prinzipien bei zukünftigen Änderungen zwingend beachtet werden müssen:

1. **Adaptive Skill (Material 3 Adaptive):**
   - **Prinzip:** Nutze für Master-Detail-Ansichten das offizielle `ListDetailPaneScaffold` (siehe `HomeScreen` & `SettingsScreen`).
   - **Responsivität:** Layouts müssen sowohl `WindowWidthSizeClass` als auch `WindowHeightSizeClass` berücksichtigen (bereitgestellt via `LocalWindowWidthSizeClass` und `LocalWindowHeightSizeClass`).
   - **Grids:** Listen in Einstellungen oder Übersichten sollten auf breiten Bildschirmen via `LazyVerticalGrid` adaptiv mehrspaltig gerendert werden.

2. **AGP 9 Upgrade Skill:**
   - **DSL-Standard:** Verwende die explizite KTS-Konfiguration (`configure<ApplicationExtension>`) anstelle der impliziten `android { ... }` Blöcke. Plugins müssen im `plugins { ... }` Block deklariert werden (kein `apply plugin`).
   - **Performance & Safety:** Aktiviere strikte Flags in `gradle.properties` (`android.nonTransitiveRClass=true`, `android.nonFinalResIds=true`).
   - **Java-Version:** Standard ist Java 17 (`jvmToolchain(17)`, `sourceCompatibility` & `targetCompatibility`).
   - **Kompatibilität:** Beachte das Flag `android.newDsl=false` in `gradle.properties`, solange Plugins (wie das BaselineProfile-Plugin) dies für die Variant-API benötigen.

3. **Navigation-3 Skill (Type-Safe Navigation):**
   - **Standard:** Nutze ausschließlich das typsichere Jetpack Navigation System (Navigation 2.8+).
   - **Routen:** Definiere Routen als `@Serializable` Objekte oder Datenklassen in der `MainActivity.kt`.
   - **Aufrufe:** Verwende `navController.navigate(RouteObject)` und `composable<RouteClass>`. Vermeide String-basierte Routen.

4. **Edge-to-Edge Skill (Android 15 Ready):**
   - **Prinzip:** Alle Screens müssen das Edge-to-Edge-Konzept unterstützen.
   - **Insets:** Nutze `AppResponsiveScaffold`, das Insets via `PaddingValues` an den Inhalt weiterreicht. Vermeide hartcodierte Paddings; nutze stattdessen Inset-aware Paddings für Listen und TopBars.

5. **R8-Analyzer Skill (Release Safety):**
   - **Regeln:** Sichert die Stabilität des Release-Builds.
   - **Schutz:** Schütze Room-Entities, DAOs, Hilt-Worker und `@Serializable` Navigations-Routen in der `proguard-rules.pro` vor Over-Stripping durch R8.

6. **Testing-Setup Skill (Stability):**
   - **Standard:** Jedes ViewModel und kritische Repositories müssen durch Unit-Tests in `src/test` abgedeckt sein.
   - **Tooling:** Nutze `MainDispatcherRule` für Coroutines und `unitTests.isReturnDefaultValues = true` für JVM-Tests mit Android-Abhängigkeiten.

7. **Design-System & Token Skill (Consistency):**
   - **Tokens:** Verwende ausschließlich die in `ui/theme/Dimensions.kt` definierten Tokens für Abstände (`Spacing...`), Icon-Größen (`IconSize...`) und Transparenz (`AlphaEmphasis...`).
   - **Hardcoding-Verbot:** Vermeide hartcodierte DP-Werte und Magic-Number-Alpha-Werte im UI-Code. Nutze das Material 3 Theme (`colorScheme`, `typography`) konsequent für volle Dark-Mode-Kompatibilität.

8. **Performance & Rendering Skill (Perfetto & Compose Optimization):**
   - **Analyse:** Nutze `perfetto-trace-analysis` zur Identifikation von Jank, UI-Thread-Blocking und unnötigen Re-Compositions.
   - **Daten-Effizienz:** Filtere Rohdaten (z. B. via `asSequence().filter`) in ViewModels oder Repositories *vor* dem Mapping in UI-Modelle, um CPU-Overhead bei großen Listen zu minimieren.
   - **UI-Stabilität:** Nutze stabiles Image-Prefetching (z. B. via Coil `enqueue`) mit dedizierten Cache-Keys, um redundante Ladeprozesse während schneller UI-Interaktionen (wie Suche oder Scrolling) zu verhindern.
   - **Threading:** Verlagere rechenintensive Operationen (Mapping, Sortierung, Filterung) konsequent via `flowOn(Dispatchers.Default)` aus dem UI-Thread.

**Vorgehen bei Änderungen:** Vor größeren Refactorings oder Updates sollten die entsprechenden Skills konsultiert oder erneut zur Analyse eingebunden werden, um die Einhaltung der aktuellen Best Practices sicherzustellen.
