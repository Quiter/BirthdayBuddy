# Changelog: BirthdayBuddy
> **Note:** Historische Einträge (Meilensteine 1-181) wurden nach [CHANGELOG_ARCHIVE.md](CHANGELOG_ARCHIVE.md) verschoben, um das Kontext-Fenster für LLM-Sessions zu optimieren.

182. **AGP 9.0 Readiness & Gradle Hardening (Build Performance):**
    - **Modern Plugin DSL:** Migrated legacy `apply(plugin = ...)` calls in [app/build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) and [baselineprofile/build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/baselineprofile/build.gradle.kts) to the declarative `plugins { ... }` block for better configuration performance and AGP 9.0 compatibility.
    - **Strict Build Flags:** Enabled `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true` in [gradle.properties](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle.properties). This prevents R class bloat and enables deeper R8 optimizations.
    - **Guidelines Update:** Documented the new AGP 9 hardening standards (Java 17, Plugins DSL, Strict Flags) in [.agents/rules/project_guidelines.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/.agents/rules/project_guidelines.md).

183. **Sicherheits-Härtung der Room-Datenbanken (Database Safety & Integrity):**
    - **Deaktivierung destruktiver Migrationen:** AppDatabase & SettingsDatabase verwenden nun ausschließlich explizite Migrationspfade. `fallbackToDestructiveMigration(true)` wurde komplett entfernt, um unbeabsichtigten Datenverlust beim User zu verhindern.
    - **Datenintegrität beim Start:** Die try-catch-Löschlogik in `AppDatabase.kt` bei Initialisierungsfehlern wurde eliminiert, sodass Schema-Konflikte direkt während der Entwicklung abgefangen werden.

184. **Design-System & Dimension-Token Bereinigung (Design System Alignment):**
    - **Token-Integration:** Vollständige Migration von SyncSettingsScreen, ThemeSettingsScreen, ColorPickerDialog, OnboardingFooter und allen Onboarding-Seiten zu standardisierten Tokens aus `Dimensions.kt` zur Eliminierung magischer Zahlen.
    - **ColorPickerDialog Details:** Preset-Farbflächen nutzen nun `ContactImageSizeSmall` anstelle von festen `40.dp`. Die Preset-Häkchen und Palette-Symbole wurden auf standardisierte Icon-Größen (`IconSizeSmall`) umgestellt. Zudem wurden für die Eckenradien der Canvas-Boxen die Material-Theme Shapes (`MaterialTheme.shapes.small`) anstelle von festen Werten verwendet, und im Canvas-Radius wird `SpacingSmall.toPx()` genutzt.
    - **Onboarding Tokens:** Hinzufügung layout- und illustrationsspezifischer Tokens (Illustration-Kreisgröße, Footer-Buttonbreite, Guide-Maße, etc.) in `Dimensions.kt`.

185. **Adaptive Layout-Upgrades & Edge-to-Edge Padding (Adaptive UI & UX):**
    - **NotificationSettingsScreen:** Umstellung der Erinnerungsregeln auf ein adaptives, mehrspaltiges `LazyVerticalGrid` mit `GridCells.Adaptive(minSize = 340.dp)` und Inset-aware Padding.
    - **Tablet-Padding & Topbar Alignment:** Hinzufügen von Padding zu `ListDetailPaneScaffold` in HomeScreen, um Überlagerungen durch die `HomeTopBar` zu vermeiden. FastScrollbar-Höhe auf Tablets korrigiert.
    - **Notch-Padding Fix im Footer:** Einführung des Parameters `includeDisplayCutout` in `AdaptiveContentContainer`, um unschöne Verschiebungen durch die Notch im OnboardingFooter zu eliminieren.

186. **Compose Performance-Optimierung & Scroll-Stabilität (Performance & Memory):**
    - **Coil Cache-Sync:** Angabe von `.memoryCacheKey` bei `ImageRequest` in `ContactImage` zur Synchronisation mit dem Prefetching des HomeScreens, was Decodierungs-Overhead einspart.
    - **FastScrollbar Optimierung:** Entfernung von kontinuierlichen Drag-States aus den `remember`-Keys für `thumbOffset` in FastScrollbar, um State-Recreation zu verhindern. derivedStateOf-Optimierungen durchgeführt und `isNeeded` vereinfacht.
    - **Scroll-Freeze Behebung:** Entfernung der automatischen Listeneinklapp-Logik beim Scrollen (`isListDragged`) in `BirthdayList.kt` zur Vermeidung von Aufhängern in `LazyListState`.

187. **Onboarding UI-Restaurierung & Layout-Feinschliff (UI/UX Polish):**
    - **Permissions Layout:** Hinzufügen von `padding(paddingValues)` zu `HorizontalPager` im OnboardingScreen zur Behebung abgeschnittener Schaltflächen. Rückkehr zur rationalen Permission-Logik aus v2.9.0.
    - **Leerraum-Eliminierung:** Entfernung von ungenutztem Platzhalter-Leerraum in `OnboardingPageTemplate`, falls keine Aktionsschaltfläche definiert ist.

188. **Compose Previews Härtung & Developer Experience (DX & Theme Safety):**
    - **Umfassende Previews:** Implementierung von `@Preview`-Funktionen für alle Onboarding-Seiten (Welcome, Contacts, Notifications, CalendarGuide, Ready) und den gesamten OnboardingScreen.
    - **Preview-Crash-Fix:** Deaktivierung von dynamischen Farben im Layout-Editor über eine `LocalInspectionMode.current` Prüfung in `Theme.kt` zur Vermeidung von `NotFoundException`-Abstürzen. Bereitstellung von Standard-SizeClasses in `ResponsiveLayout.kt`.

189. **Release-Safety & ProGuard-Regeln (Proguard & R8 Optimization):**
    - **Generische Keep-Regel:** Ersetzung aller 12 expliziten Keep-Regeln für Navigationsrouten in `proguard-rules.pro` durch eine einzige, wartungsfreie generische Keep-Regel (`-keep @kotlinx.serialization.Serializable class * { *; }`) zur Absicherung des R8-Builds.

190. **Fehlerbehebung für Messenger-Links (Bug Fix):**
    - **Signal Universal Link:** Umstellung von `signal://` auf das offizielle HTTPS-Schema `https://signal.me/#p/` in `ContactActions.kt` zum fehlerfreien Öffnen von Direktchats.

191. **Automatisiertes Skill-Management über Gradle & Git (.gitignore Alignment) (DX & Tooling):**
    - **Projekt-Sauberkeit:** Aufnahme von `.agents/external/` in die `.gitignore` und Entfernung manuell kopierter Skills zur Vermeidung von Git-Submodules.
    - **Gradle Automation:** Implementierung der Gradle-Tasks `checkAgentSkills` (Automatisches Klonen von Google's `android/skills` und `material-3-skill` beim Sync) und `updateAgentSkills` (manuelles Update via `git pull`).
    - **Registrierung & Richtlinien:** Dynamische Skill-Registrierung in `.agents/skills.json`.
    - **LLM-Optimierung:** Überarbeitung von `project_guidelines.md` als englisches Instruction-Set für LLMs (inkl. kompakter Directory Map zur Reduzierung des Token-Verbrauchs).

192. **Behebung der Pull-to-Refresh Sichtbarkeit (Bug Fix):**
    - **Positionierung des Lade-Indikators:** Durch das Hinzufügen einer Top-Padding-Verschiebung basierend auf den Scaffold-`paddingValues` beim `PullToRefreshDefaults.Indicator` wird der Refresh-Spinner nun korrekt unterhalb der soliden `HomeTopBar` positioniert. Dies behebt die vollständige Überlagerung des Indikators im Edge-to-Edge Layout, während die Listen-Inhalte weiterhin unter der TopBar hindurchscrollen können.
    - **Code-Bereinigung (Code Quality):** Vereinfachung des Boolean-Ausdrucks in `HomeScreen.kt` auf `.none { ... }`, Verschiebung der Variablen-Deklaration `name` in den `when`-Ausdruck in `CalendarSyncRepository.kt` zur Eingrenzung des Scopes sowie Konvertierung des Legacy-`delay(Long)` zu `delay(Duration)` in `TimeRepository.kt`.

193. **Zentralisierung der Info-Karten & Benachrichtigungs-Erklärung (UI Refactoring):**
    - **Wiederverwendbares InfoCard-Widget:** Einführung einer globalen `InfoCard`-Komponente unter `ui/components/InfoCard.kt` zur Vermeidung von Code-Duplizierung und Gewährleistung eines einheitlichen M3-Designs (sekundärer Container, Info-Icon, fettgedruckter Titel und Erklärtext) in allen Einstellungsbereichen.
    - **Erklärung wichtiger Benachrichtigungen:** Hinzufügen einer erklärenden Infobox unter der Option "Wichtige Benachrichtigungen" in den Benachrichtigungseinstellungen zur besseren Erläuterung persistenter Hinweise.
    - **Konsistente Einstellungsseiten:** Migration aller Einstellungsseiten (Labels, Kalender, Benachrichtigungen, andere Ereignisse, Sync, Backup) auf das zentrale `InfoCard`-Design.

194. **Dynamische Generierung von Surface-Container-Farben für benutzerdefinierte Themes (Theme & Color Safety):**
    - **Container-Farbkorrektur:** Implementierung einer `withSurfaceContainers` Erweiterungsfunktion auf `ColorScheme` in [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt), welche die Container-Farbrollen (`surfaceContainerLow`, `surfaceContainerHigh`, etc.) dynamisch per HSV-Transformation aus der gewählten Oberflächenfarbe (`surface`) ableitet.
    - **Behebung statischer & benutzerdefinierter Akzente:** Dadurch passen sich Komponenten wie [BirthdayItem](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt) (die `surfaceContainerLow` / `surfaceContainerHigh` nutzen) nun bei allen Akzentfarben (Blau, Grün, Rot, Orange, Pink und benutzerdefinierten Hex-Farben) korrekt an das Farbschema an, anstatt auf den standardmäßigen lila-grauen Basistönen zu verbleiben.

195. **Umstellung der Farbgenerierung auf den offiziellen Google HCT-Algorithmus (Theme Engine Upgrade):**
    - **Library-Integration:** Hinzufügen der Kotlin Multiplatform Dynamic-Color-Portierung `com.materialkolor:material-color-utilities:5.0.0-alpha07` zur Versionstabelle [libs.versions.toml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml) und als Abhängigkeit in [app/build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts).
    - **HCT Farb-Engine in [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt):** Ersetzung der alten fehleranfälligen manuellen HSV-Verschiebungen durch den offiziellen Google HCT-Farbraum (`Hct.fromInt`) und `SchemeContent` in Verbindung mit `MaterialDynamicColors`.
    - **Farbkonsistenz und Kontrastsicherheit:** Dadurch werden alle Farbschemata (Voreinstellungen und nutzerdefinierte Hex-Farben aus dem Color Picker) mathematisch exakt nach Material Design 3 Spezifikationen generiert, wodurch alle Container-Farben automatisch passen und Barrierefreiheit/Kontraste für alle Farben garantiert sind.

196. **Material 3 Design- und Token-Optimierung in BirthdayItem (Design System Compliance):**
    - **Vermeidung magischer Zahlen:** Auslagerung der Rahmenbreiten für ausgewählte Kontakte (`SelectedBorderWidth = 1.5.dp`) und Geburtstage (`BirthdayBorderWidth = 2.dp`) in [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt).
    - **M3 Konformität in [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt):** Ersetzung von `RoundedCornerShape(SpacingSmall)` durch das korrekte M3-Shape-Token `MaterialTheme.shapes.small` sowie Ablösung der manuellen Deckkraft-Farbe `primary.copy(alpha = AlphaContainerSubtle)` durch die semantischen Farbrollen `primaryContainer` / `onPrimaryContainer` im Toggle-Bereich für Geschenkideen zur Gewährleistung barrierefreier Kontraste.

197. **Best-Effort-Rollback für Cross-Datenbank-Operationen (Data Integrity & Code Quality):**
    - **Problem:** `linkAsCouple()`, `unlinkCouple()` und `updateGiftIdeas()` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt) schrieben in zwei separate Room-Datenbanken (`settingsDatabase` und `appDatabase`) in unverbundenen Transaktionen. Ein Fehler bei der zweiten Transaktion hinterließ einen inkonsistenten Zustand.
    - **Lösung – Rollback-Muster:** Da SQLite keine atomaren Cross-DB-Transaktionen unterstützt, wurde ein explizites Best-Effort-Rollback-Muster eingeführt: Die `settingsDatabase` (Quelle der Wahrheit) wird immer zuerst geschrieben. Das anschließende `appDatabase`-Cache-Update ist in einem `try/catch` gesichert – schlägt es fehl, wird die `settingsDatabase` in einem Rollback-Block auf den zuvor gesicherten Zustand zurückgesetzt und der Fehler wird nach oben propagiert.
    - **Atomare Einzel-DB-Operationen:** Alle Lesen-Modifizieren-Schreiben-Vorgänge innerhalb einer einzelnen Datenbank werden nun jeweils in `withTransaction { }` gewrapped, um partielle Schreibvorgänge zu verhindern.
    - **Log-Hygiene:** Deutschen Log-Text in `syncContacts()` auf Englisch umgestellt.
