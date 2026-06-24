# Changelog: BirthdayBuddy
> **Note:** Historische Einträge (Meilensteine 1-181) wurden nach [CHANGELOG_ARCHIVE.md](CHANGELOG_ARCHIVE.md) verschoben, um das Kontext-Fenster für LLM-Sessions zu optimieren.

211. **Einführung einer wiederverwendbaren `AppSwitch`-Komponente zur Design-Konsistenz (UI/UX Polish):**
    - **Problem:** Die `Switch`-Komponente wurde an 9 verschiedenen Stellen in den Einstellungen, Onboarding-Seiten und Dialogen verwendet. An fast allen Stellen wurde der Häkchen-Indikator auf dem Daumen (`thumbContent = if (checked) { Icon(...) }`) redundant kopiert. Zukünftige Designänderungen an Schaltern hätten manuelle Updates an allen 9 Orten erfordert.
    - **Lösung:** Einführung der zentralen Komponente `AppSwitch` in `ui/components/AppSwitch.kt`. Diese kapselt das standardmäßige Design und die Häkchen-Logik. Alle direkten `Switch`-Vorkommen im Projekt wurden durch `AppSwitch` ersetzt.
    - **Details der Änderungen:**
      - **`AppSwitch.kt` [NEU]**: Erstellung der Komponente mit vorinstalliertem `Icons.Default.Check` auf dem Schalter-Thumb.
      - **`PROJECT_STRUCTURE.md`**: Dokumentation der neuen Datei.
      - **Ersetzungen**: `Switch` durch `AppSwitch` und Bereinigung der duplizierten `thumbContent`-Lambdas in `CalendarSettingsScreen.kt`, `NotificationSettingsScreen.kt`, `OtherEventsSettingsScreen.kt`, `ThemeSettingsScreen.kt`, `BirthdayDatePickerDialog.kt`, `CalendarPage.kt`, `ContactsPage.kt` und `NotificationsPage.kt`.

210. **Vereinheitlichung der Elementabstände in den Einstellungen auf SpacingNormal (UI/UX Polish):**
    - **Problem:** Die vertikalen Abstände zwischen Karten und Sektionen auf verschiedenen Einstellungsseiten waren uneinheitlich. Einige Seiten verwendeten feste paddings (`SpacingLarge`, `SpacingSmall`) oder Spacer in Kombination mit dem standardmäßigen `Arrangement.spacedBy(SpacingNormal)`, was zu optisch unbalancierten Lücken führte.
    - **Lösung:** Alle Einstellungslisten auf `Arrangement.spacedBy(SpacingNormal)` (16.dp) umgestellt und ad-hoc Innen-/Außenabstände an den Elementen entfernt.
    - **Details der Änderungen:**
      - **`NotificationSettingsScreen.kt`**: Zusätzliches Top/Bottom-Padding von `InfoCard` und Bottom-Padding vom geplanten Benachrichtigungs-Container entfernt. Die Anordnung erfolgt nun ausschließlich über den standardmäßigen Abstand des Grids.
      - **`BackupContent.kt`**: Unnötigen `Spacer` vor der `InfoCard` entfernt.
      - **`CalendarSettingsScreen.kt`**: `LazyColumn` auf `verticalArrangement = Arrangement.spacedBy(SpacingNormal)` umgestellt; manuelles Padding bei `SetupStepsCard`, der Sync-Aktivierungskarte und dem Kalenderfarben-Container entfernt.
      - **`OtherEventsSettingsScreen.kt`**: `LazyColumn` auf `spacedBy(SpacingNormal)` umgestellt und das Bottom-Padding der Sync-Karte entfernt.
      - **`SyncSettingsScreen.kt`**: `LazyColumn` auf `spacedBy(SpacingNormal)` umgestellt und das Bottom-Padding der Sync-Karte entfernt.

209. **Vereinheitlichung der Eckenradien (Shapes) in den Einstellungen auf Material 3 extraLarge (UI/UX Polish):**
    - **Problem:** Einige Container auf den Einstellungsseiten (z. B. `InfoCard`, `SetupStepsCard` in `CalendarSettingsScreen` und `LabelConfigCard` in `LabelSettingsScreen`) verwendeten abweichende Eckenradien (`shapes.medium` oder `shapes.large`), was das ansonsten durchgängige Design von Karten mit `shapes.extraLarge` unterbrach.
    - **Lösung:** Vereinheitlichung aller Einstellungskarten auf den Eckenradius `shapes.extraLarge` (28.dp).
    - **Details der Änderungen:**
      - **`InfoCard.kt`**: Umstellung des Eckenradius der `Card` von `shapes.medium` auf `shapes.extraLarge`.
      - **`CalendarSettingsScreen.kt`**: Eckenradius der `SetupStepsCard` von `shapes.large` auf `shapes.extraLarge` geändert.
      - **`LabelSettingsScreen.kt`**: Eckenradius der `LabelConfigCard` von `shapes.medium` auf `shapes.extraLarge` geändert.

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

207. **Entfernung ungenutzter ViewModel-Hilfsfunktionen in `HomeViewModel` (Code-Bereinigung / Compiler-Warnungen):**
    - **Problem:** Nach der Entkopplung des `HomeScreen`s vom `HomeViewModel` (Meilenstein 205), bei der alle Interaktionen auf das einheitliche MVI/UDF-Muster (`onIntent`) umgestellt wurden, blieben elf veraltete Hilfsfunktionen im ViewModel zurück. Diese führten zu Compiler-Warnungen über ungenutzten Code.
    - **Lösung:** Entfernung aller ungenutzten Hilfsfunktionen, um den ViewModel sauber zu halten und die Build-Warnungen zu beseitigen.
    - **Details der Änderungen:**
      - **`HomeViewModel.kt`**: Die ungenutzten Funktionen `setIsResettingFilter`, `addGiftIdea`, `toggleGiftIdea`, `deleteGiftIdea`, `updateGiftIdeaText`, `updateBirthday`, `consumeSearchFocus`, `consumeNewlyAddedIdeaId`, `linkAsCouple`, `unlinkCouple` und `ignoreCoupleSuggestion` wurden entfernt.
      - Die tatsächlich verwendeten Hilfsfunktionen (wie `onSearchQueryChange` in Tests, `resetFilters` und `syncContacts` in `MainActivity`) wurden beibehalten.

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


205. **Entkopplung von `HomeScreen` vom `HomeViewModel` (Testbarkeit / Clean Architecture):**
    - **Problem:** `HomeScreen` nahm `viewModel: HomeViewModel` direkt als Parameter entgegen. Composable-UI-Tests waren dadurch nur mit vollständiger Hilt-DI-Infrastruktur möglich – ein unnötiger Test-Overhead.
    - **Lösung:** Signaturen auf State + Intent-Handler umgestellt. `HomeScreen` akzeptiert jetzt ausschließlich `uiState: HomeUiState`, `onIntent: (HomeIntent) -> Unit` und `scrollToTopEvent: SharedFlow<Unit>`. Der ViewModel wird ausschließlich an der Aufrufstelle in `MainActivity.kt` (im `composable<Home>`-Block) instantiiert und sein State/Intent-Handling per Lambda weitergegeben. Damit ist `HomeScreen` vollständig ohne Hilt testbar.
    - **`HomeScreen.kt`:** Import von `HomeViewModel` durch `HomeIntent` + `SharedFlow` ersetzt (ViewModel-Referenz nur noch für `HomeViewModel.LABEL_NO_BIRTHDAY`-Konstante in `HomeContent`). Alle `viewModel.*`-Calls auf `onIntent(HomeIntent.*)` umgestellt: `syncContacts()`, `consumeSearchFocus()`, `consumeNewlyAddedIdeaId()`, `setIsResettingFilter()`, `scrollToTopEvent`-Handling sowie alle `HomeActions`-Lambdas. `remember`-Schlüssel von `viewModel` auf `onIntent` geändert. Import `collectAsStateWithLifecycle` entfernt.
    - **`MainActivity.kt`:** Im `composable<Home>`-Block wird nun `val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()` collected und an `HomeScreen` übergeben. `onIntent = homeViewModel::onIntent`, `scrollToTopEvent = homeViewModel.scrollToTopEvent` werden als Lambdas weitergereicht. `HandleIntents`, ContentObserver und Lifecycle-Effekte behalten direkten ViewModel-Zugriff (liegen außerhalb von `HomeScreen`).
    - **Neuer Test:** [HomeScreenTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreenTest.kt) mit 3 Compose-Smoke-Tests (leer, Beispieldaten, `null`-State) – kein Hilt, nur `createComposeRule()`.
    - **Build:** `compileDebugKotlin` erfolgreich, BUILD SUCCESSFUL in 10s.

202. **Einführung einer Kontrasteinstellung für benutzerdefinierte Themes (Accessibility & Theming):**
    - **Problem:** Nach der Umstellung der Farbgenerierung auf den Google HCT-Algorithmus fehlte eine Möglichkeit, die Kontrastwerte (z. B. für verbesserte Barrierefreiheit) im Theming-System flexibel anzupassen.
    - **Lösung:** Hinzufügen einer `themeContrast`-Option (Standard: `0.0`, Mittel: `0.3`, Hoch: `1.0`) im Theming-System und den Einstellungen.
    - **Datenbank & Migration:** `AppSettings` in [AppSettings.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/AppSettings.kt) um `themeContrast: Double` erweitert. Die `SettingsDatabase` in [SettingsDatabase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/SettingsDatabase.kt) auf Version 7 angehoben und eine Room-Migration (`MIGRATION_6_7`) implementiert.
    - **Theme-Verarbeitung:** In [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt) wird die Kontrasteinstellung an `createHctColorScheme` und `SchemeContent(hct, isDark, contrast)` übergeben.
    - **UI & Lokalisierung:** `ThemeSettingsScreen` in [ThemeSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/theme/ThemeSettingsScreen.kt) um eine Radio-Button-Auswahl für Kontraststufen ergänzt. Lokalisierungsschlüssel in Deutsch und Englisch hinzugefügt.
    - **Tests:** Erweiterung von [SettingsMigrationTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/data/local/SettingsMigrationTest.kt) um den Migrations-Test `migrate6To7`. Aktualisierung von `ThemeViewModelTest`, `CalendarViewModelTest`, `NotificationViewModelTest` und `OnboardingViewModelTest` für die neue `updateSettings`-Signatur. All 91 JVM-Tests erfolgreich bestanden.

204. **Extraktion von `NotificationRepository` aus `MainActivity` in `AppViewModel` (Clean Architecture):**
    - **Problem:** `MainActivity` verletzte Clean Architecture durch direkte Hilt-Field-Injection (`@Inject lateinit var notificationRepository: NotificationRepository`) und direkten Datenzugriff: `lifecycleScope.launch { notificationRepository.syncScheduling() }` sowie `notificationRepository.settings.collectAsStateWithLifecycle()` für das globale App-Theme.
    - **Lösung:** Neues `@HiltViewModel` [AppViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/AppViewModel.kt) eingeführt. Es hält `appSettings: StateFlow<AppSettings>` (via `SharingStarted.Eagerly` für sofortige Theme-Verfügbarkeit beim Kaltstart) und ruft `syncScheduling()` einmalig im `init {}`-Block auf. Das ViewModel überlebt Konfigurationsänderungen (Rotation), sodass `syncScheduling()` nicht mehr bei jeder Activity-Recreation erneut aufgerufen wird.
    - **`MainActivity.kt`:** `@Inject notificationRepository`-Feld, `lifecycleScope.launch { ... }`-Block sowie die direkte Flow-Subscription entfernt. Stattdessen wird `appViewModel: AppViewModel = hiltViewModel()` genutzt und `appSettings by appViewModel.appSettings.collectAsStateWithLifecycle()` collected. Nicht mehr benötigte Imports (`lifecycleScope`, `kotlinx.coroutines.launch`, `javax.inject.Inject`, `NotificationRepository`) entfernt.
    - **Tests:** Neues [AppViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/AppViewModelTest.kt) mit 3 Unit-Tests: Verifikation des Default-`AppSettings`-Initial-Values, reaktiver Settings-Emission, und dass `syncScheduling()` einmalig im `init` aufgerufen wird.

203. **Extraktion des duplizierten `getLabel`-Lambdas in `HomeContent` (Code Quality / Compose-Optimierung):**
    - **Problem:** Das `getLabel`-Lambda für `FastScrollbar` war identisch in zwei Branches von `HomeContent` ([HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt)) definiert – einmal im Compact-Branch (Smartphone-Layout) und einmal im Tablet-Branch (`ListDetailPaneScaffold`).
    - **Lösung:** Das Lambda wurde als lokale Variable `getScrollLabel: (ContactUiModel) -> String` mittels `remember(uiState.selectedLabel)` vor dem `if/else`-Branch extrahiert. Beide `FastScrollbar`-Aufrufe verweisen nun auf dieselbe Instanz. `remember(uiState.selectedLabel)` stellt sicher, dass das Lambda bei einem Label-Wechsel korrekt neu erstellt wird, ohne bei jedem Recompose eine neue Instanz zu erzeugen.
    - **Änderungen:** [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) – Import von `ContactUiModel` ergänzt; `getScrollLabel`-Variable eingefügt; beide `getLabel`-Lambdas ersetzt.
    - **Build:** `compileDebugKotlin` erfolgreich, BUILD SUCCESSFUL in 30s.

200. **Entfernung des Sentinel-Value-Anti-Patterns in `daysUntilNext` (Code Quality / Type Safety):**
    - **Problem:** `ContactUiModel.daysUntilNext` war vom Typ `Long` (nicht nullable). Fehlte ein Datum, wurde `Long.MAX_VALUE` als Sentinel verwendet – ein klassisches Anti-Pattern, das fragile Vergleiche wie `contact.daysUntilNext != Long.MAX_VALUE` erzwang.
    - **Lösung:** `daysUntilNext` in [ContactUiModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/ContactUiModel.kt) auf `Long?` umgestellt. `null` drückt semantisch korrekt aus, dass kein Datum hinterlegt ist.
    - **Mapper:** [ContactMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapper.kt) – `?: Long.MAX_VALUE` entfernt; `daysLeft` wird direkt als `Long?` weitergegeben.
    - **UI-Callsites:** Alle drei Sentinel-Vergleiche in [BirthdayStatus.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayStatus.kt), [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt) und [BirthdayDetailPane.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayDetailPane.kt) auf `== null` / `!= null` umgestellt.
    - **Sortierung:** [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/HomeViewModel.kt) – `compareBy<ContactUiModel> { it.daysUntilNext }` durch `compareBy(nullsLast(naturalOrder())) { it.daysUntilNext }` ersetzt. Sortierverhalten unverändert: Kontakte ohne Datum landen weiterhin am Listenende.
    - **Tests:** [ContactMapperTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapperTest.kt) – drei `isEqualTo(Long.MAX_VALUE)`-Assertions durch `isNull()` ersetzt. Alle Unit-Tests grün.

201. **Zentralisierung duplizierter Intent-Extra-Schlüssel in `IntentExtras` (Code Quality / Typsicherheit):**
    - **Problem:** Die String-Literale `"SCROLL_TO_TOP"`, `"NAVIGATE_TO_NOTIFICATIONS"`, `"OPEN_SEARCH"` und `"OPEN_ADD_CONTACT"` waren als Intent-Extra-Schlüssel an mehreren Stellen dupliziert (Producer: Widget, NotificationHelper; Consumer: MainActivity). Ein Tippfehler führte zu stummem Fehlverhalten zur Laufzeit ohne Compiler-Warnung.
    - **Lösung:** Neues [IntentExtras.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/util/IntentExtras.kt) mit `object IntentExtras` und 4 `const val`-Konstanten. Tippfehler werden nun vom Kotlin-Compiler abgefangen.
    - **Consumer:** [MainActivity.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/MainActivity.kt) – alle 8 String-Literale in `HandleIntents()` ersetzt (`getBooleanExtra` + `removeExtra` je Schlüssel).
    - **Producer Widget:** [BirthdayWidget.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidget.kt) – `putExtra("SCROLL_TO_TOP", true)` ersetzt.
    - **Producer Notification:** [NotificationHelper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/notifications/components/NotificationHelper.kt) – `putExtra("NAVIGATE_TO_NOTIFICATIONS", true)` ersetzt.


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
    - **Mapper-Integration [ContactMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapper.kt):** `BirthdayTier.from(nextAgeValue)` wird einmalig in `toUiModelForEvent` berechnet und als `birthdayTier`-Feld auf das [ContactUiModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/ContactUiModel.kt) gesetzt.
    - **UI-Vereinfachung [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt):** Die beiden doppelten `when`-Ketten wurden durch saubere, exhaustive `when(contact.birthdayTier)`-Ausdrücke ersetzt. Die `remember`-Keys verwenden jetzt `contact.birthdayTier` statt `contact.nextAge` (semantisch präziser).
    - **Test-Abdeckung:** Neue Klasse [ContactMapperTierTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapperTierTest.kt) mit 18 Unit-Tests für `BirthdayTier.from()` (alle Tier-Grenzen und Edge-Cases) und Integrations-Tests für die Mapper-Anbindung. Bestehende Tests in `ContactMapperTest.kt` bleiben grün und wurden um eine Tier-Assertion ergänzt.
