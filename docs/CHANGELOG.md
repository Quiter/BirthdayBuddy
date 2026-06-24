# Changelog: BirthdayBuddy (Reihenfolge: Chronologisch aufsteigend / Älteste oben, Neueste unten - neue Einträge unten anfügen)
> **Note:** Historische Einträge (Meilensteine 1-190) wurden nach [CHANGELOG_ARCHIVE.md](CHANGELOG_ARCHIVE.md) verschoben, um das Kontext-Fenster für LLM-Sessions zu optimieren.

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
    - **HCT Farb-Engine in [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt):** Ersetzung der alten fehleranfällinen manuellen HSV-Verschiebungen durch den offiziellen Google HCT-Farbraum (`Hct.fromInt`) und `SchemeContent` in Verbindung mit `MaterialDynamicColors`.
    - **Farbkonsistenz und Kontrastsicherheit:** Dadurch werden alle Farbschemata (Voreinstellungen und nutzerdefinierte Hex-Farben aus dem Color Picker) mathematisch exakt nach Material Design 3 Spezifikationen generiert, wodurch alle Container-Farben automatisch passen und Barrierefreiheit/Kontraste für alle Farben garantiert sind.

196. **Material 3 Design- und Token-Optimierung in BirthdayItem (Design System Compliance):**
    - **Vermeidung magischer Zahlen:** Auslagerung der Rahmenbreiten für ausgewählte Kontakte (`SelectedBorderWidth = 1.5.dp`) und Geburtstage (`BirthdayBorderWidth = 2.dp`) in [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt).
    - **M3 Konformität in [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt):** Ersetzung von `RoundedCornerShape(SpacingSmall)` durch das korrekte M3-Shape-Token `MaterialTheme.shapes.small` sowie Ablösung der manuellen Deckkraft-Farbe `primary.copy(alpha = AlphaContainerSubtle)` durch die semantischen Farbrollen `primaryContainer` / `onPrimaryContainer` im Toggle-Bereich für Geschenkideen zur Gewährleistung barrierefreier Kontraste.

197. **Best-Effort-Rollback für Cross-Datenbank-Operationen (Data Integrity & Code Quality):**
    - **Problem:** `linkAsCouple()`, `unlinkCouple()` und `updateGiftIdeas()` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt) schrieben in zwei separate Room-Datenbanken (`settingsDatabase` und `appDatabase`) in unverbundenen Transaktionen. Ein Fehler bei der zweiten Transaktion hinterließ einen inkonsistenten Zustand.
    - **Lösung – Rollback-Muster:** Da SQLite keine atomaren Cross-DB-Transaktionen unterstützt, wurde ein explizites Best-Effort-Rollback-Muster eingeführt: Die `settingsDatabase` (Quelle der Wahrheit) wird immer zuerst geschrieben. Das anschließende `appDatabase`-Cache-Update ist in einem `try/catch` gesichert – schlägt es fehl, wird die `settingsDatabase` in einem Rollback-Block auf den zuvor gesicherten Zustand zurückgesetzt und der Fehler wird nach oben propagiert.
    - **Atomare Einzel-DB-Operationen:** Alle Lesen-Modifizieren-Schreiben-Vorgänge innerhalb einer einzelnen Datenbank werden nun jeweils in `withTransaction { }` gewrapped, um partielle Schreibvorgänge zu verhindern.
    - **Log-Hygiene:** Deutschen Log-Text in `syncContacts()` auf Englisch umgestellt.

198. **Instrumentierte Tests für Gift-Idea-Operationen (Test Coverage & Data Integrity):**
    - **Analyse:** Bestätigt, dass `addGiftIdea()`, `toggleGiftIdea()`, `deleteGiftIdea()` und `updateGiftIdeaText()` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt) bereits vollständig das Best-Effort-Rollback-Muster via privater Hilfsmethode `updateGiftIdeas()` implementieren – identisch zu `linkAsCouple()` / `unlinkCouple()`.
    - **Neue Testsuite:** Erstellung von [ContactRepositoryGiftIdeaTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepositoryGiftIdeaTest.kt) mit **17 instrumentierten Tests** (In-Memory-Datenbanken, analog zu `ContactRepositoryCoupleLinkTest`):
        - `addGiftIdea`: Hinzufügen in beiden DBs, Bestand erhalten, Sortierung vor erledigte Ideen, Kontakt nicht vorhanden.
        - `toggleGiftIdea`: `isChecked` setzen/löschen in beiden DBs, Sortierung ans Ende.
        - `deleteGiftIdea`: Löschen aus beiden DBs, andere Ideen erhalten, unbekannte ID.
        - `updateGiftIdeaText`: Text in beiden DBs aktualisieren, ID/Status erhalten, unbekannte ID.
        - **Konsistenz-Tests:** Verifiziert nach jeder Operation, dass `SettingsDatabase` und `AppDatabase` denselben Zustand zeigen.
        - **Lifecycle-Test:** Vollständiger Add → Toggle → Delete-Zyklus.
    - **Kompilierung:** `BUILD SUCCESSFUL` – alle 17 Tests kompilieren fehlerfrei.

199. **Business-Logik Extraktion: BirthdayTier-Enum (Architecture / Code Quality):**
    - **Problem:** In [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt) wurde dieselbe Alter-basierte Tier-Klassifizierung (`nextAge % 10`, `nextAge % 5`, `nextAge in 0..9`) zweimal unabhängig für `borderStroke` und `confettiColors` inline berechnet – ein klarer Clean-Architecture-Verstoß (Business-Logik im UI-Layer).
    - **Neues Enum [BirthdayTier.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/BirthdayTier.kt):** Einführung eines typsicheren `BirthdayTier`-Enums in `ui/model/` mit vier Einträgen (`MILESTONE_GOLD`, `MILESTONE_SILVER`, `CHILD`, `REGULAR`) und einer `companion object fun from(nextAge: Int?): BirthdayTier`-Factory als Single Source of Truth für die Tier-Logik. Neu: der `MILESTONE_SILVER`-Ast (`% 5 == 0, % 10 != 0`) vervollständigt die fehlende Abstufung.
    - **Mapper-Integration [ContactMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/data/mapper/ContactMapper.kt):** `BirthdayTier.from(nextAgeValue)` wird einmalig in `toUiModelForEvent` berechnet und als `birthdayTier`-Feld auf das [ContactUiModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/ContactUiModel.kt) gesetzt.
    - **UI-Vereinfachung [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt):** Die beiden doppelten `when`-Ketten wurden durch saubere, exhaustive `when(contact.birthdayTier)`-Ausdrücke ersetzt. Die `remember`-Keys verwenden jetzt `contact.birthdayTier` statt `contact.nextAge` (semantisch präziser).
    - **Test-Abdeckung:** Neue Klasse [ContactMapperTierTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapperTierTest.kt) mit 18 Unit-Tests für `BirthdayTier.from()` (alle Tier-Grenzen und Edge-Cases) und Integrations-Tests für die Mapper-Anbindung. Bestehende Tests in `ContactMapperTest.kt` bleiben grün und wurden um eine Tier-Assertion ergänzt.

200. **Entfernung des Sentinel-Value-Anti-Patterns in `daysUntilNext` (Code Quality / Type Safety):**
    - **Problem:** `ContactUiModel.daysUntilNext` war vom Typ `Long` (nicht nullable). Fehlte ein Datum, wurde `Long.MAX_VALUE` als Sentinel verwendet – ein klassisches Anti-Pattern, das fragile Vergleiche wie `contact.daysUntilNext != Long.MAX_VALUE` erzwang.
    - **Lösung:** `daysUntilNext` in [ContactUiModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/ContactUiModel.kt) auf `Long?` umgestellt. `null` drückt semantisch korrekt aus, dass kein Datum hinterlegt ist.
    - **Mapper:** [ContactMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapper.kt) – `?: Long.MAX_VALUE` entfernt; `daysLeft` wird direkt als `Long?` weitergegeben.
    - **UI-Callsites:** Alle drei Sentinel-Vergleiche in [BirthdayStatus.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayStatus.kt), [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt) und [BirthdayDetailPane.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayDetailPane.kt) auf `== null` / `!= null` umgestellt.
    - **Sortierung:** [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/HomeViewModel.kt) – `compareBy<ContactUiModel> { it.daysUntilNext }` durch `compareBy(nullsLast(naturalOrder())) { it.daysUntilNext }` ersetzt. Sortierverhalten unverändert: Kontakte ohne Datum landen weiterhin am Listenende.
    - **Tests:** [ContactMapperTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapperTest.kt) – drei `isEqualTo(Long.MAX_VALUE)`-Assertions durch `isNull()` ersetzt. Alle Unit-Tests grün.

201. **Zentralisierung duplizierter Intent-Extra-Schlüssel in `IntentExtras` (Code Quality / Typsicherheit):**
    - **Problem:** Die String-Literale `"SCROLL_TO_TOP"`, `"NAVIGATE_TO_NOTIFICATIONS"`, `"OPEN_SEARCH"` und `"OPEN_ADD_CONTACT"` waren als Intent-Extra-Schlüssel an mehreren Stellen dupliziert (Producer: Widget, NotificationHelper; Consumer: MainActivity). Ein Tippfehler führte zu stummem Fehlverhalten zur Laufzeit ohne Compiler-Warnung.
    - **Lösung:** Neues [IntentExtras.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/util/IntentExtras.kt) mit `object IntentExtras` and 4 `const val`-Konstanten. Tippfehler werden nun vom Kotlin-Compiler abgefangen.
    - **Consumer:** [MainActivity.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/MainActivity.kt) – alle 8 String-Literale in `HandleIntents()` ersetzt (`getBooleanExtra` + `removeExtra` je Schlüssel).
    - **Producer Widget:** [BirthdayWidget.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidget.kt) – `putExtra("SCROLL_TO_TOP", true)` ersetzt.
    - **Producer Notification:** [NotificationHelper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/notifications/components/NotificationHelper.kt) – `putExtra("NAVIGATE_TO_NOTIFICATIONS", true)` ersetzt.

202. **Einführung einer Kontrasteinstellung für benutzerdefinierte Themes (Accessibility & Theming):**
    - **Problem:** Nach der Umstellung der Farbgenerierung auf den Google HCT-Algorithmus fehlte eine Möglichkeit, die Kontrastwerte (z. B. für verbesserte Barrierefreiheit) im Theming-System flexibel anzupassen.
    - **Lösung:** Hinzufügen einer `themeContrast`-Option (Standard: `0.0`, Mittel: `0.3`, Hoch: `1.0`) im Theming-System und den Einstellungen.
    - **Datenbank & Migration:** `AppSettings` in [AppSettings.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/AppSettings.kt) um `themeContrast: Double` erweitert. Die `SettingsDatabase` in [SettingsDatabase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/SettingsDatabase.kt) auf Version 7 angehoben und eine Room-Migration (`MIGRATION_6_7`) implementiert.
    - **Theme-Verarbeitung:** In [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt) wird die Kontrasteinstellung an `createHctColorScheme` und `SchemeContent(hct, isDark, contrast)` übergeben.
    - **UI & Lokalisierung:** `ThemeSettingsScreen` in [ThemeSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/theme/ThemeSettingsScreen.kt) um eine Radio-Button-Auswahl für Kontraststufen ergänzt. Lokalisierungsschlüssel in Deutsch und Englisch hinzugefügt.
    - **Tests:** Erweiterung von [SettingsMigrationTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/data/local/SettingsMigrationTest.kt) um den Migrations-Test `migrate6To7`. Aktualisierung von `ThemeViewModelTest`, `CalendarViewModelTest`, `NotificationViewModelTest` und `OnboardingViewModelTest` für die neue `updateSettings`-Signatur. All 91 JVM-Tests erfolgreich bestanden.

203. **Extraktion des duplizierten `getLabel`-Lambdas in `HomeContent` (Code Quality / Compose-Optimierung):**
    - **Problem:** Das `getLabel`-Lambda für `FastScrollbar` war identisch in zwei Branches von `HomeContent` ([HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt)) definiert – einmal im Compact-Branch (Smartphone-Layout) und einmal im Tablet-Branch (`ListDetailPaneScaffold`).
    - **Lösung:** Das Lambda wurde als lokale Variable `getScrollLabel: (ContactUiModel) -> String` mittels `remember(uiState.selectedLabel)` vor dem `if/else`-Branch extrahiert. Beide `FastScrollbar`-Aufrufe verweisen nun auf dieselbe Instanz. `remember(uiState.selectedLabel)` stellt sicher, dass das Lambda bei einem Label-Wechsel korrekt neu erstellt wird, ohne bei jedem Recompose eine neue Instanz zu erzeugen.
    - **Änderungen:** [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) – Import von `ContactUiModel` ergänzt; `getScrollLabel`-Variable eingefügt; beide `getLabel`-Lambdas ersetzt.
    - **Build:** `compileDebugKotlin` erfolgreich, BUILD SUCCESSFUL in 30s.

204. **Extraktion von `NotificationRepository` aus `MainActivity` in `AppViewModel` (Clean Architecture):**
    - **Problem:** `MainActivity` verletzte Clean Architecture durch direkte Hilt-Field-Injection (`@Inject lateinit var notificationRepository: NotificationRepository`) und direkten Datenzugriff: `lifecycleScope.launch { notificationRepository.syncScheduling() }` sowie `notificationRepository.settings.collectAsStateWithLifecycle()` für das globale App-Theme.
    - **Lösung:** Neues `@HiltViewModel` [AppViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/AppViewModel.kt) eingeführt. Es hält `appSettings: StateFlow<AppSettings>` (via `SharingStarted.Eagerly` für sofortige Theme-Verfügbarkeit beim Kaltstart) und ruft `syncScheduling()` einmalig im `init {}`-Block auf. Das ViewModel überlebt Konfigurationsänderungen (Rotation), sodass `syncScheduling()` nicht mehr bei jeder Activity-Recreation erneut aufgerufen wird.
    - **`MainActivity.kt`:** `@Inject notificationRepository`-Feld, `lifecycleScope.launch { ... }`-Block sowie die direkte Flow-Subscription entfernt. Stattdessen wird `appViewModel: AppViewModel = hiltViewModel()` genutzt und `appSettings by appViewModel.appSettings.collectAsStateWithLifecycle()` collected. Nicht mehr benötigte Imports (`lifecycleScope`, `kotlinx.coroutines.launch`, `javax.inject.Inject`, `NotificationRepository`) entfernt.
    - **Tests:** Neues [AppViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/AppViewModelTest.kt) mit 3 Unit-Tests: Verifikation des Default-`AppSettings`-Initial-Values, reaktiver Settings-Emission, und dass `syncScheduling()` einmalig im `init` aufgerufen wird.

205. **Entkopplung von `HomeScreen` vom `HomeViewModel` (Testbarkeit / Clean Architecture):**
    - **Problem:** `HomeScreen` nahm `viewModel: HomeViewModel` direkt als Parameter entgegen. Composable-UI-Tests waren dadurch nur mit vollständiger Hilt-DI-Infrastruktur möglich – ein unnötiger Test-Overhead.
    - **Lösung:** Signaturen auf State + Intent-Handler umgestellt. `HomeScreen` akzeptiert jetzt ausschließlich `uiState: HomeUiState`, `onIntent: (HomeIntent) -> Unit` und `scrollToTopEvent: SharedFlow<Unit>`. Der ViewModel wird ausschließlich an der Aufrufstelle in `MainActivity.kt` (im `composable<Home>`-Block) instantiiert und sein State/Intent-Handling per Lambda weitergegeben. Damit ist `HomeScreen` vollständig ohne Hilt testbar.
    - **`HomeScreen.kt`:** Import von `HomeViewModel` durch `HomeIntent` + `SharedFlow` ersetzt (ViewModel-Referenz nur noch für `HomeViewModel.LABEL_NO_BIRTHDAY`-Konstante in `HomeContent`). Alle `viewModel.*`-Calls auf `onIntent(HomeIntent.*)` umgestellt: `syncContacts()`, `consumeSearchFocus()`, `consumeNewlyAddedIdeaId()`, `setIsResettingFilter()`, `scrollToTopEvent`-Handling sowie alle `HomeActions`-Lambdas. `remember`-Schlüssel von `viewModel` auf `onIntent` geändert. Import `collectAsStateWithLifecycle` entfernt.
    - **`MainActivity.kt`:** Im `composable<Home>`-Block wird nun `val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()` collected und an `HomeScreen` übergeben. `onIntent = homeViewModel::onIntent`, `scrollToTopEvent = homeViewModel.scrollToTopEvent` werden als Lambdas weitergereicht. `HandleIntents`, ContentObserver und Lifecycle-Effekte behalten direkten ViewModel-Zugriff (liegen außerhalb von `HomeScreen`).
    - **Neuer Test:** [HomeScreenTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreenTest.kt) mit 3 Compose-Smoke-Tests (leer, Beispieldaten, `null`-State) – kein Hilt, nur `createComposeRule()`.
    - **Build:** `compileDebugKotlin` erfolgreich, BUILD SUCCESSFUL in 10s.

206. **Bündelung der Einstellungen in abgerundeten Karten-Containern (UI/UX Polish / Material 3 Compliance):**
    - **Problem:** Die Einstellungen auf den verschiedenen Einstellungsseiten (Design, Benachrichtigungen, Kalender, Backup, Sync, Andere Ereignisse) waren flach auf dem Hintergrund gerendert, was insbesondere im AMOLED-Schwarz-Modus unstrukturiert wirkte.
    - **Lösung:** Gruppierung aller Einstellungsoptionen in visuell getrennte Material 3 `Card`-Container mit `MaterialTheme.shapes.extraLarge` Eckenradius und einer `surfaceContainer`-Hintergrundfarbe auf allen Einstellungsseiten zur Erzielung eines Designs "wie aus einem Guss".
    - **Details der Änderungen:**
      - **`ThemeSettingsScreen.kt`**: Optionen für Theme-Modus, Kontrast und Akzentfarbe in Karten gruppiert mit abgerundeten Rändern (`clip`) und dezenten `HorizontalDivider`n.
      - **`NotificationSettingsScreen.kt` / `NotificationRuleItem.kt`**: Aktivierungs- und persistente Schalter in eine gemeinsame Karte verschoben. Geplante Erinnerungsregeln aus dem flachen Grid in eine vertikal gestapelte Liste innerhalb einer Karte mit Trennstrichen überführt.
      - **`CalendarSettingsScreen.kt`**: Synchronisierungs-Aktivierung und die Sektion der Kalenderfarben in getrennte Karten gegliedert; Trennelemente zwischen den Kalendertypen eingefügt.
      - **`BackupContent.kt`**: Export- und Importaktionen in eine gemeinsame Karte zusammengefasst.
      - **`SyncSettingsScreen.kt` & `OtherEventsSettingsScreen.kt`**: Die jeweiligen einzelnen Aktionselemente bzw. Schalter in Karten-Container eingepackt.
      - Verwendung von `ListItemDefaults.colors(containerColor = Color.Transparent)` auf allen betroffenen `ListItem`s zur nahtlosen Hintergrund-Integration.

207. **Entfernung ungenutzter ViewModel-Hilfsfunktionen in `HomeViewModel` (Code-Bereinigung / Compiler-Warnungen):**
    - **Problem:** Nach der Entkopplung des `HomeScreen`s vom `HomeViewModel` (Meilenstein 205), bei der alle Interaktionen auf das einheitliche MVI/UDF-Muster (`onIntent`) umgestellt wurden, blieben elf veraltete Hilfsfunktionen im ViewModel zurück. Diese führten zu Compiler-Warnungen über ungenutzten Code.
    - **Lösung:** Entfernung aller ungenutzten Hilfsfunktionen, um den ViewModel sauber zu halten und die Build-Warnungen zu beseitigen.
    - **Details der Änderungen:**
      - **`HomeViewModel.kt`**: Die ungenutzten Funktionen `setIsResettingFilter`, `addGiftIdea`, `toggleGiftIdea`, `deleteGiftIdea`, `updateGiftIdeaText`, `updateBirthday`, `consumeSearchFocus`, `consumeNewlyAddedIdeaId`, `linkAsCouple`, `unlinkCouple` und `ignoreCoupleSuggestion` wurden entfernt.
      - Die tatsächlich verwendeten Hilfsfunktionen (wie `onSearchQueryChange` in Tests, `resetFilters` und `syncContacts` in `MainActivity`) wurden beibehalten.

208. **Erstellung von Compose Previews für alle Einstellungsseiten (Developer Experience & UI-Verifikation):**
    - **Problem:** Für die Einstellungsseiten `CalendarSettingsScreen`, `OtherEventsSettingsScreen`, `SyncSettingsScreen` und `ThemeSettingsScreen` fehlten Jetpack Compose `@Preview`-Funktionen. Dadurch war es nicht möglich, das Design dieser Screens direkt im Android Studio-Editor zu betrachten und zu verifizieren. Zudem war `ThemeSettingsScreen` eng an den `ThemeViewModel` gekoppelt.
    - **Lösung:**
      - Hinzufügen von `@Preview`-Funktionen für `CalendarSettingsScreen`, `OtherEventsSettingsScreen` und `SyncSettingsScreen` unter Verwendung ihrer Content-Komponenten.
      - Refactoring von `ThemeSettingsScreen.kt`: Extraktion der UI-Hierarchie in eine neue `ThemeSettingsContent`-Composable-Funktion, die unabhängig vom `ThemeViewModel` über einfache Parameter und Callbacks konfiguriert wird. Hinzufügen einer `@Preview`-Funktion für `ThemeSettingsContent`.
    - **Details der Änderungen:**
      - **`CalendarSettingsScreen.kt`**: Preview für `CalendarSettingsContent` hinzugefügt und `Preview`-Import ergänzt.
      - **`OtherEventsSettingsScreen.kt`**: Preview für `OtherEventsSettingsContent` hinzugefügt und `Preview`-Import ergänzt.
      - **`SyncSettingsScreen.kt`**: Preview für `SyncSettingsContent` hinzugefügt und `Preview`-Import ergänzt.
      - **`ThemeSettingsScreen.kt`**: UI in `ThemeSettingsContent` ausgelagert, die ViewModel-Interaktionen per Lambdas entkoppelt und Preview für `ThemeSettingsContent` hinzugefügt sowie `Preview`-Import ergänzt.

209. **Vereinheitlichung der Eckenradien (Shapes) in den Einstellungen auf Material 3 extraLarge (UI/UX Polish):**
    - **Problem:** Einige Container auf den Einstellungsseiten (z. B. `InfoCard`, `SetupStepsCard` in `CalendarSettingsScreen` und `LabelConfigCard` in `LabelSettingsScreen`) verwendeten abweichende Eckenradien (`shapes.medium` oder `shapes.large`), was das ansonsten durchgängige Design von Karten mit `shapes.extraLarge` unterbrach.
    - **Lösung:** Vereinheitlichung aller Einstellungskarten auf den Eckenradius `shapes.extraLarge` (28.dp).
    - **Details der Änderungen:**
      - **`InfoCard.kt`**: Umstellung des Eckenradius der `Card` von `shapes.medium` auf `shapes.extraLarge`.
      - **`CalendarSettingsScreen.kt`**: Eckenradius der `SetupStepsCard` von `shapes.large` auf `shapes.extraLarge` geändert.
      - **`LabelSettingsScreen.kt`**: Eckenradius der `LabelConfigCard` von `shapes.medium` auf `shapes.extraLarge` geändert.

210. **Vereinheitlichung der Elementabstände in den Einstellungen auf SpacingNormal (UI/UX Polish):**
    - **Problem:** Die vertikalen Abstände zwischen Karten und Sektionen auf verschiedenen Einstellungsseiten waren uneinheitlich. Einige Seiten verwendeten feste paddings (`SpacingLarge`, `SpacingSmall`) oder Spacer in Kombination mit dem standardmäßigen `Arrangement.spacedBy(SpacingNormal)`, was zu optisch unbalancierten Lücken führte.
    - **Lösung:** Alle Einstellungslisten auf `Arrangement.spacedBy(SpacingNormal)` (16.dp) umgestellt und ad-hoc Innen-/Außenabstände an den Elementen entfernt.
    - **Details der Änderungen:**
      - **`NotificationSettingsScreen.kt`**: Zusätzliches Top/Bottom-Padding von `InfoCard` und Bottom-Padding vom geplanten Benachrichtigungs-Container entfernt. Die Anordnung erfolgt nun ausschließlich über den standardmäßigen Abstand des Grids.
      - **`BackupContent.kt`**: Unnötigen `Spacer` vor der `InfoCard` entfernt.
      - **`CalendarSettingsScreen.kt`**: `LazyColumn` auf `verticalArrangement = Arrangement.spacedBy(SpacingNormal)` umgestellt; manuelles Padding bei `SetupStepsCard`, der Sync-Aktivierungskarte und dem Kalenderfarben-Container entfernt.
      - **`OtherEventsSettingsScreen.kt`**: `LazyColumn` auf `spacedBy(SpacingNormal)` umgestellt und das Bottom-Padding der Sync-Karte entfernt.
      - **`SyncSettingsScreen.kt`**: `LazyColumn` auf `spacedBy(SpacingNormal)` umgestellt und das Bottom-Padding der Sync-Karte entfernt.

211. **Einführung einer wiederverwendbaren `AppSwitch`-Komponente zur Design-Konsistenz (UI/UX Polish):**
    - **Problem:** Die `Switch`-Komponente wurde an 9 verschiedenen Stellen in den Einstellungen, Onboarding-Seiten und Dialogen verwendet. An fast allen Stellen wurde der Häkchen-Indikator auf dem Daumen (`thumbContent = if (checked) { Icon(...) }`) redundant kopiert. Zukünftige Designänderungen an Schaltern hätten manuelle Updates an allen 9 Orten erfordert.
    - **Lösung:** Einführung der zentralen Komponente `AppSwitch` in `ui/components/AppSwitch.kt`. Diese kapselt das standardmäßige Design und die Häkchen-Logik. Alle direkten `Switch`-Vorkommen im Projekt wurden durch `AppSwitch` ersetzt.
    - **Details der Änderungen:**
      - **`AppSwitch.kt` [NEU]**: Erstellung der Komponente mit vorinstalliertem `Icons.Default.Check` auf dem Schalter-Thumb.
      - **`PROJECT_STRUCTURE.md`**: Dokumentation der neuen Datei.
      - **Ersetzungen**: `Switch` by `AppSwitch` und Bereinigung der duplizierten `thumbContent`-Lambdas in `CalendarSettingsScreen.kt`, `NotificationSettingsScreen.kt`, `OtherEventsSettingsScreen.kt`, `ThemeSettingsScreen.kt`, `BirthdayDatePickerDialog.kt`, `CalendarPage.kt`, `ContactsPage.kt` und `NotificationsPage.kt`.

212. **Migration der App-Navigation auf Jetpack Navigation 3 & Fix der Navigationsanimationen:**
    - **Problem:** Die Navigation nutzte zuvor das standardmäßige Jetpack Navigation Compose mit `NavHost`. Die Übergangsanimationen und das Z-Index-Verhalten bei Zurück-Navigation (Pop) waren nach der ersten Migration in `MainActivity.kt` fehlerhaft (die eintreffende Seite schob sich über die austreffende Seite, und die Seite verschwand in der Mitte, anstatt seitlich wegzusliden).
    - **Lösung:** Vollständige Umstellung der globalen und lokalen Navigation auf Jetpack Navigation 3 (`NavDisplay`). Zudem Anpassung der Übergangsanimationen (`transitionSpec` und `popTransitionSpec`) in `MainActivity.kt` auf ein flüssiges horizontales Slide-Verhalten mit Parallaxe:
      - **Vorwärts (Push)**: Der eintreffende Screen slidet von rechts (`100%` Breite) hinein, während der austreffende Screen mit Parallaxe nach links slidet (`-25%` Breite) und ausblendet.
      - **Rückwärts (Pop)**: Der austreffende Screen slidet nach rechts weg (`100%` Breite), während der eintreffende Screen mit Parallaxe von links (`-25%` Breite) hineinslidet. Über `targetContentZIndex = -1f` wird sichergestellt, dass der austreffende Screen beim Zurückgehen über dem eintreffenden liegt und sauber zur Seite weggeleitet wird (analog zum Android-Systemverhalten beim Verlassen einer App zum Homescreen).
    - **Details der Änderungen:**
      - **`MainActivity.kt`**: Umstellung von `NavHost` auf `NavDisplay` mit `togetherWith` und Steuerung des `targetContentZIndex` bei Pop. Implementierung des horizontalen Slide-Parallaxe-Verfahrens.
      - **`HomeScreen.kt` & `SettingsScreen.kt`**: Umstellung auf `currentWindowAdaptiveInfoV2()` zur Behebung von Deprecations.
      - **`NotificationHelperTest.kt`**: Behebung eines fehlenden Imports für `kotlin.time.Duration.Companion.milliseconds` in Android-Tests.
      - **`project_guidelines.md`**: Aktualisierung der Navigations- und Layout-Richtlinien.

213. **Master-Schalter für das Label-Management (Feature):**
    - **Problem:** Wenn ein Nutzer zwar eigene Labels in seinen System-Kontakten definiert hatte, die Label-Filterung und -Verwaltung in BirthdayBuddy jedoch nicht nutzen wollte, gab es keine Möglichkeit, diese Funktion abzuschalten. Sie belegte Platz auf dem Startbildschirm und filterte ungewollt Kontakte aus. Zudem sollten Hochzeitstage und Namenstage bei deaktiviertem Label-Management mit in die Gesamtliste aufgenommen werden.
    - **Lösung:** Einführung eines Master-Schalters ("Label-Management aktivieren") oben im Label-Verwaltungs-Bildschirm. Wenn dieser deaktiviert ist, wird die Filterleiste auf dem Startbildschirm ausgeblendet, Kontakte werden nicht mehr anhand ihrer Labels gefiltert oder im Widget ignoriert, und die Konfigurationskarten im Einstellungsbildschirm werden ausgegraut und gesperrt. Außerdem werden Hochzeitstage und Namenstage (sofern "Weitere Ereignisse" aktiv ist) in der Gesamtliste einsortiert, da die separaten Label-Filtertabs ausgeblendet sind.
    - **Details der Änderungen:**
      - **`AppSettings.kt`**: Neues Attribut `labelsEnabled` (default `true`) zur Speicherung der Einstellung.
      - **`SettingsDatabase.kt`**: Datenbankversion auf `8` erhöht und Migration `MIGRATION_7_8` implementiert.
      - **`ContactRepository.kt`**: Flow `labelsEnabled` und Methode `updateLabelsEnabled` bereitgestellt.
      - **`LabelViewModel.kt`**: StateFlow `labelsEnabled` und Methode `setLabelsEnabled` bereitgestellt.
      - **`HomeViewModel.kt`**: Logik von `ignoredLabels` und `availableLabels` angepasst; löscht Label-Tags der Kontakte im `uiState` wenn das Label-Management deaktiviert ist. Hochzeitstage und Namenstage werden bei deaktiviertem Label-Management und aktiviertem `otherEventsEnabled` in die Gesamtliste integriert.
      - **`BirthdayWidget.kt`**: Deaktiviert das Filtern ignorierter Kontakte im Widget, wenn das Label-Management aus ist.
      - **`LabelSettingsScreen.kt`**: Integration von `AppSwitch` als Master-Schalter und visuelle Sperrung (`AlphaEmphasisDisabled`) der Label-Konfigurationskarten.
      - **`strings.xml` & `strings.xml (de)`**: Texte für den Schalter hinzugefügt.
      - **`SettingsMigrationTest.kt`**: Migrationstest `migrate7To8` zur Absicherung des DB-Updates.
      - **`LabelViewModelTest.kt` & `HomeViewModelTest.kt` & `HomeViewModelSearchTest.kt`**: Anpassung und Erweiterung der Unit-Tests zur Verifizierung (einschließlich Kombination der Ereignisse bei deaktiviertem Label-Management).

214. **Zentralisierung der Einstellungs-UI-Elemente in `SettingsComponents.kt` (UI/UX Polish / Code-Bereinigung):**
    - **Problem:** Auf den verschiedenen Einstellungsseiten wurden UI-Elemente wie Sektions-Header, abgerundete Karten und Einstellungszeilen (mit Schaltern oder Klickeffekten) manuell und redundant mit viel Boilerplate-Code implementiert. Dies erschwerte zukünftige Designänderungen und führte zu potenziellen Fehlern (z.B. bei der Verschachtelung von Eckenradien).
    - **Lösung:** Einführung einer zentralen Komponente `SettingsComponents.kt` in `ui/components/`. Diese stellt wiederverwendbare, konsistente Material 3 UI-Elemente bereit (`SettingsSectionHeader`, `SettingsCard`, `SettingsSwitchRow` und `SettingsClickableRow`). Alle Einstellungsbildschirme wurden so refaktoriert, dass sie diese neuen Komponenten nutzen, wodurch die Code-Duplizierung eliminiert und ein absolut einheitliches Erscheinungsbild der Einstellungen gewährleistet wird.
    - **Details der Änderungen:**
      - **`SettingsComponents.kt` [NEU]**: Erstellung der wiederverwendbaren UI-Elemente für die Einstellungen.
      - **`PROJECT_STRUCTURE.md`**: Dokumentation der neuen Datei.
      - **`ThemeSettingsScreen.kt`**: Refaktoriert auf die neuen Komponenten; manuelle Listen-Items für Theme-Auswahl und Kontrast sowie AMOLED-Schalter durch `SettingsClickableRow` und `SettingsSwitchRow` ersetzt.
      - **`NotificationSettingsScreen.kt`**: Refaktoriert auf die neuen Komponenten.
      - **`CalendarSettingsScreen.kt`**: Refaktoriert auf die neuen Komponenten.
      - **`OtherEventsSettingsScreen.kt`**: Refaktoriert auf die neuen Komponenten.
      - **`SyncSettingsScreen.kt`**: Refaktoriert auf die neuen Komponenten.
      - **`BackupContent.kt`**: Refaktoriert auf die neuen Komponenten.

215. **Fix des Predictive Back Gesture Verhaltens (UI/UX Polish):**
    - **Problem:** Bei der Benutzung der Zurück-Geste des Systems (Predictive Back Gesture) unter Android wurde der aktuelle Bildschirm verkleinert und transparent dargestellt. Dadurch schimmerte der darunterliegende Startbildschirm auf unschöne Weise durch, was optisch unprofessionell wirkte.
    - **Lösung:** Definition von `predictivePopTransitionSpec` im `NavDisplay` in `MainActivity.kt`. Diese nutzt nun dieselbe horizontale Slide-Animation wie die standardmäßige `popTransitionSpec` (Ausblenden und Heraussliden nach rechts), sodass die Zurück-Geste visuell identisch zur Pfeil-Zurück-Aktion oben links ausgeführt wird und die unschöne Skalierung/Transparenz behoben ist.
    - **Details der Änderungen:**
      - **`MainActivity.kt`**: `predictivePopTransitionSpec` zu `NavDisplay` hinzugefügt und auf dieselbe Animation wie `popTransitionSpec` konfiguriert.

216. **Refactoring und Bündelung von Onboarding-Illustrationen in neuem Paket `ui.illustrations` (Clean Architecture / Code-Bereinigung):**
    - **Problem:** Die Illustrationen auf den Onboarding-Seiten wurden lokal und privat innerhalb der jeweiligen Seitendateien (`WelcomePage.kt`, `CalendarPage.kt`, etc.) definiert. Das führte zu langen Quelldateien (~300 Zeilen) und verhinderte die Wiederverwendung dieser grafischen Elemente in anderen Bereichen wie z.B. den Einstellungsbildschirmen. Zudem lag die bestehende `ContactsIllustration.kt` unter `ui/components/`, was nicht optimal strukturiert war.
    - **Lösung:** Einführung des neuen Pakets `com.heckmannch.birthdaybuddy.ui.illustrations` und Auslagerung aller Onboarding-Illustrationen in dieses Paket. Dadurch sind alle grafischen Custom-Illustrationen an einem zentralen Ort gebündelt, können in Previews isoliert gerendert werden, und stehen für eine zukünftige Wiederverwendung in den Einstellungsbildschirmen zur Verfügung. Die Onboarding-Seitendateien wurden dadurch erheblich vereinfacht und gekürzt.
    - **Details der Änderungen:**
      - **`ui/illustrations/` [NEU]**: Paketverzeichnis erstellt und folgende Dateien angelegt:
        - `WelcomeIllustration.kt`: Ausgelagerte Illustration für die Begrüßungsseite.
        - `ContactsIllustration.kt`: Aus `ui/components/` hierher verschoben und Paket angepasst.
        - `NotificationsIllustration.kt`: Ausgelagerte Illustration für die Benachrichtigungsseite.
        - `CalendarIllustration.kt`: Ausgelagerte Illustration für die Kalenderseite.
        - `CalendarGuideIllustration.kt`: Ausgelagerte Illustration für die Kalender-Anleitung.
        - `ReadyIllustration.kt`: Ausgelagerte Illustration für die Abschlussseite.
      - **`ui/components/ContactsIllustration.kt` [DELETE]**: Alte Datei gelöscht.
      - **`WelcomePage.kt`**, **`ContactsPage.kt`**, **`NotificationsPage.kt`**, **`CalendarPage.kt`**, **`CalendarGuidePage.kt`**, **`ReadyPage.kt`**: Importiert die Illustrationen nun aus dem neuen Paket `ui.illustrations`. Lokale Illustrations-Definitionen vollständig entfernt.
      - **`BirthdayList.kt`**: Import-Pfad von `ContactsIllustration` auf das neue Paket aktualisiert.
      - **`PROJECT_STRUCTURE.md`**: Dokumentation der neuen Paketstruktur und der verschobenen Dateien.

217. **Präzisierung der Hintergrundfarb-Animation im Onboarding (UI/UX Polish):**
    - **Problem:** Die Hintergrundfarbe `ambientColor` in `OnboardingScreen.kt` wurde basierend auf `pagerState.currentPage` animiert. Wenn der Kalender-Guide übersprungen wurde, verschob sich der Index der Abschlussseite auf `4`, wodurch fälschlicherweise die Farbe der Kalender-Anleitung (Secondary Container) statt der Abschlussseite (Tertiary Container) angezeigt wurde.
    - **Lösung:** Berechnung von `actualPage` zur Berücksichtigung der Sichtbarkeit des Kalender-Guides und Kopplung von `ambientColor` an diese berechnete Seite.
    - **Details der Änderungen:**
      - **`OnboardingScreen.kt`**: Logik zur Berechnung von `actualPage` hinzugefügt und das Farbschema-Mapping darauf umgestellt.

218. **Einführung der wiederverwendbaren `StepItem`-Komponente und Angleichung der Karten-Ecken (UI/UX Polish / Code Quality):**
    - **Problem:** Die zweispaltige Darstellung von Setup-Schritten mit einem Kreis-Indikator (Icon/Nummer) links und Text rechts war doppelt und redundant implementiert – einmal privat als `GuideStepItem` in `CalendarGuidePage.kt` und einmal privat als `StepItem` in `CalendarSettingsScreen.kt`. Zudem wiesen die Karten im Onboarding (Radius `medium`) und den Einstellungen (Radius `extraLarge`) einen unterschiedlichen Eckenradius auf, was visuell inkonsistent wirkte.
    - **Lösung:** Einführung einer zentralen, hochflexiblen `StepItem`-Komponente unter `ui/components/StepItem.kt` zur Nutzung an beiden Stellen. Einheitliche Definition des Eckenradius aller Onboarding-Karten auf `extraLarge`, passend zum Design-System der Einstellungen.
    - **Details der Änderungen:**
      - **`StepItem.kt` [NEU]**: Erstellung der flexiblen Komponente mit Unterstützung für Nummern, Icons, erledigte/gesperrte Zustände und Aktions-Buttons.
      - **`CalendarSettingsScreen.kt`**: Löschung des privaten `StepItem` und Migration auf die zentrale Komponente.
      - **`CalendarGuidePage.kt`**: Löschung des privaten `GuideStepItem`, Migration auf die neue Komponente und Anpassung des Eckenradius der Karte auf `extraLarge`.
      - **`ContactsPage.kt`**, **`NotificationsPage.kt`**, **`CalendarPage.kt`**, **`ReadyPage.kt`**: Konfiguration des Card-Parameters `shape = MaterialTheme.shapes.extraLarge` zur Vereinheitlichung der UI-Eckenradien.
      - **`PROJECT_STRUCTURE.md`**: Dokumentation der neuen Datei `StepItem.kt`.

219. **Migration der Kalender-Farbzeilen auf die zentrale `SettingsClickableRow`-Komponente (Code Quality / Konsistenz):**
    - **Problem:** Die Zeilen zur Auswahl der Kalenderfarben (für Geburtstage, Hochzeitstage und Namenstage) in `CalendarSettingsScreen.kt` nutzten rohe `ListItem`-Aufrufe mit manuellem `clickable`-Modifier und redundantem Design-Boilerplate. Das verletzte das Prinzip der zentralisierten Design-Tokens in Einstellungs-Screens.
    - **Lösung:** Ersetzung der rohen `ListItem`-Aufrufe durch die in `SettingsComponents.kt` bereitgestellte Komponente `SettingsClickableRow`.
    - **Details der Änderungen:**
      - **`CalendarSettingsScreen.kt`**: Import für `SettingsClickableRow` hinzugefügt und die drei Farb-Zeilen auf die zentrale Komponente umgestellt.
220. **Verwendung von StepItem in der Zusammenfassung auf ReadyPage.kt (UI/UX Polish / Konsistenz):**
    - **Problem:** Die Zusammenfassung der Einstellungen auf der onboarding-Abschlussseite (`ReadyPage.kt`) wurde als flache Textzeilen mit fest codierten Häkchen/Kreuzen (`✓`/`✗`) gerendert. Das wirkte visuell inkonsistent im Vergleich zum restlichen Onboarding und den Einstellungen, die die neue `StepItem`-Komponente nutzen.
    - **Lösung:** Umstellung der Zusammenfassungs-Liste auf die wiederverwendbare `StepItem`-Komponente.
    - **Details der Änderungen:**
      - **`strings.xml` / `values-de/strings.xml`**: Hinzufügen neuer, bereinigter Strings für Titel und Aktivierungsstatus (ohne das vorangestellte `✓`/`✗`) und Löschung der veralteten XML-Ressourcen.
      - **`ReadyPage.kt`**: Import von `StepItem` sowie `Icons.Default.Close` hinzugefügt und die drei Statuselemente (Kontakte, Benachrichtigungen, Kalender-Sync) auf das einheitliche `StepItem`-Design umgestellt. Erfolgreiche Status zeigen ein grünes Häkchen, inaktive Status zeigen ein graues Schließen-Kreuz.

221. **Behebung der Filterfunktion im Tablet-Layout (Bug Fix & Testability):**
    - **Problem:** Im Tablet-Layout (`WindowWidthSizeClass.Expanded` / `WindowWidthSizeClass.Medium`) war das Filtern von Kontakten über die Label-Filterleiste funktionslos. Zwar reagierten die Chips visuell auf Klicks, die Kontaktliste wurde jedoch nicht gefiltert. Dies lag daran, dass Jetpack Navigation 3's `NavDisplay` die `NavEntry`s cached, wodurch die `@Composable`-Inhaltslambdas die initialen, veralteten Werte der lokalen Statusvariablen (`contacts`, `selectedLabel`, etc.) aus dem Eltern-Scope einfingen.
    - **Lösung:** Einführung von `currentUiState` und `currentActions` via `rememberUpdatedState` am Anfang von `HomeContent` in [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt). Die Referenzen innerhalb des Tablet-Zweigs von `NavDisplay` wurden auf diese Zustandskapsel umgestellt, wodurch Compose Zustandsänderungen nun korrekt beobachtet und die Listen bei Label-Auswahl filtert.
    - **UI-Test:** Erweiterung der Testklasse [HomeScreenTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreenTest.kt) um den instrumentierten UI-Test `homeScreen_tabletLayout_labelSelectionFiltersList`. Dieser simuliert das Tablet-Layout und verifiziert, dass das Klicken auf einen Label-Filter-Chip die angezeigten Kontakte erfolgreich einschränkt.

222. **Entfernung der Kontrasteinstellung und card-spezifischen Kontrastrahmen (Rollback / Feature Removal):**
    - **Problem:** Die zuvor eingeführte Kontrasteinstellung (`themeContrast`) und die darauf basierenden visuellen Rahmen um Karten/Hintergründe stellten sich als nicht vorteilhaft heraus und sollten wieder vollständig entfernt werden. Ein einfacher Rollback auf Version 7 führt jedoch zu Abstürzen auf Test- oder Entwicklergeräten, die bereits Version 7 (mit Kontrast) oder Version 8 (mit Kontrast + Labels) installiert hatten.
    - **Lösung:** Rückbau aller Kontrast-Funktionen auf den ursprünglichen Zustand vor der Einführung der Kontrasteinstellung unter Verwendung eines Migrationspfads zu Version 9:
      - **Datenbank & Migration:** Die Datenbank-Version der `SettingsDatabase` wurde auf Version 9 angehoben. Um Kompatibilität zu gewährleisten, wurden die originalen Migrationsschritte erhalten (`MIGRATION_6_7` fügt `themeContrast` hinzu, `MIGRATION_7_8` fügt `labelsEnabled` hinzu). Mit `MIGRATION_8_9` wurde eine Migration per Tabellen-Neuerstellung eingeführt, die die Spalte `themeContrast` sicher entfernt und die Daten (inkl. `labelsEnabled`) erhält. Das neue Schema wurde in `9.json` exportiert.
      - **Modelle & ViewModels:** `themeContrast` wurde aus `AppSettings`, `ThemeUiState` sowie den ViewModels (`ThemeViewModel`, `AppViewModel`) und dem `NotificationRepository` entfernt.
      - **UI-Komponenten:** Der Dynamic Theme Wrapper `BirthdayBuddyTheme` und die `MainActivity` verwenden keinen Kontrast-Parameter mehr. Die Card-Rahmen und dynamic transparent background Logik in `InfoCard` sowie alle Einstellungsoptionen für den Kontrast im `ThemeSettingsScreen` wurden zurückgebaut. Localization Keys wurden gelöscht.
      - **Tests & Bereinigung:** Die Testsuite `SettingsMigrationTest` wurde erweitert und führt nun die Verifikationen für `migrate6To7`, `migrate7To8` und `migrate8To9` aus, um den kompletten Upgrade-Pfad zu validieren. Alle restlichen View-Modell-Tests (`ThemeViewModelTest`, `CalendarViewModelTest`, `NotificationViewModelTest`, `OnboardingViewModelTest`, `AppViewModelTest`) spiegeln die bereinigte `updateSettings`-Signatur und den gelöschten Kontrast-State wider.

223. **Verschiebung der Label-Filterleiste in die Geburtstagsliste & Paket-Bereinigung (UI/UX Polish / Architecture):**
    - **WhatsApp-Style Scrollverhalten:** Die [LabelFilterBar](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/LabelFilterBar.kt) wurde aus dem statischen Header entfernt und als erstes Element (`item(key = "label_filter_bar")`) direkt in die scrollbare [BirthdayList](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) integriert. 
    - **Paket-Relokation:** Passend zur neuen Rolle wurde `LabelFilterBar.kt` aus dem Paket `ui.screens.home.components.topbar` in das Paket `ui.screens.home.components.list` verschoben.
    - **Code-Vereinfachung:** Die komplexe Scroll-Sperren-Logik (`filterVisibilityLock`, `isFilterBarVisible`) in [HomeState.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeState.kt) wurde vollständig entfernt.
    - **Auflösung von HomeTopBar.kt:** Da nur noch die SearchBar als statischer Header verbleibt, wurde die Hilfsdatei `HomeTopBar.kt` gelöscht. Die [SearchBar](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/topbar/SearchBar.kt) wird nun direkt in [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) in einer status-bar-padded Surface gerendert.
    - **Anpassung der FastScrollbar:** Die [FastScrollbar](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt) wurde um einen `headerCount`-Offset erweitert, damit Scrollbar-Gesten trotz der Listen-Kopfzeilen präzise arbeiten.


