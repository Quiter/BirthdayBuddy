# Changelog: BirthdayBuddy (Reihenfolge: Chronologisch aufsteigend / Älteste oben, Neueste unten - neue Einträge unten anfügen)
> **Note:** Historische Einträge (Meilensteine 1-225) wurden nach [CHANGELOG_ARCHIVE.md](CHANGELOG_ARCHIVE.md) verschoben, um das Kontext-Fenster für LLM-Sessions zu optimieren.

226. **Relozierung der Agenten-Skills & Gradle-Bereinigung (DX & Tooling):**
    - **Direkte Skill-Ablage:** Relozierung der externen Agent-Skills direkt in das Standardverzeichnis `.agents/skills/`, damit sie nativ und automatisch von Antigravity geladen werden, ohne eine `.agents/skills.json` pflegen zu müssen.
    - **Entfernung von `skills.json`:** Die nicht mehr benötigte Konfigurationsdatei `.agents/skills.json` wurde vollständig gelöscht.
    - **Gradle-Automatisierung:** Die Gradle-Tasks `checkAgentSkills` und `updateAgentSkills` wurden in [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/build.gradle.kts) überarbeitet. Es wurde die Klasse `SkillSyncer` eingeführt, die voll kompatibel mit dem Gradle Configuration Cache ist. Sie sucht rekursiv nach `SKILL.md` Dateien in `.agents/external/` und spiegelt diese nach `.agents/skills/` wider, während verwaiste Ordner bereinigt werden.
    - **Git-Integration:** Anpassung von [.gitignore](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/.gitignore) um `.agents/skills/*` zu ignorieren, während das lokale `projekt-kontext` via `!.agents/skills/projekt-kontext/` explizit versioniert bleibt.
    - **Richtlinien-Update:** Dokumentation der neuen Skill-Struktur und Task-Aufrufe in [.agents/rules/project_guidelines.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/.agents/rules/project_guidelines.md).

227. **Behebung doppelter Kalendereinträge bei App-Daten-Clear (Bug Fix & Test Coverage):**
    - **Problem:** Wenn die Daten der App gelöscht wurden, blieben die lokalen System-Kalender (unter dem Sync-Account "BirthdayBuddy") erhalten. Beim erneuten Einrichten wurden neue Kalendereinträge (Termine) erzeugt, aber die bestehenden Termine konnten nicht gelöscht werden, weil sie ohne `CALLER_IS_SYNCADAPTER=true` in den URIs angelegt wurden. Dadurch schlug `clearCalendarEvents()` stumm fehl (0 gelöschte Zeilen) und es entstanden doppelte Einträge.
    - **Lösung – Sync-Adapter-URIs:** Die Methode `addEvent()` in [CalendarSyncRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepository.kt) wurde so angepasst, dass sie nun den korrekten Sync-Adapter-URI (mit den Abfrageparametern `CALLER_IS_SYNCADAPTER=true`, `account_name=BirthdayBuddy` und `account_type=LOCAL`) verwendet. Dadurch sind alle Termine sauber dem Sync-Account zugeordnet und können beim Leeren rückstandslos gelöscht werden.
    - **Neue Testsuite:** Erstellung von [CalendarSyncRepositoryTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepositoryTest.kt) zur Verifikation, dass alle Termin-Inserts über den korrekten Sync-Adapter-URI durchgeführt werden.
    - **Kompilierungs-Fix:** Fehlender Import für `NotificationHelper` in [NotificationHelperTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/androidTest/java/com/heckmannch/birthdaybuddy/ui/screens/settings/notifications/components/NotificationHelperTest.kt) korrigiert.

228. **Einführung des Notification-Domain-Layers (Architecture & Code Quality):**
    - **GetPendingNotificationsUseCase:** Kapselung der Ermittlung fälliger Benachrichtigungen (Geburtstage, Hochzeitstage mit Ehepartner-Kopplung, Namenstage) basierend auf aktiven Regeln und der Vermeidung bereits geplanter Erinnerungen.
    - **SnoozeNotificationUseCase:** Kapselung der Schlummer-Erinnerungslogik (Delegation an den plattformspezifischen Scheduler).
    - **Refactoring:** Bereinigung des `NotificationWorker` und `NotificationActionReceiver` zur Entkopplung von Android-Framework-Abhängigkeiten und reiner Geschäftslogik.
    - **Unit-Tests:** Implementierung umfassender JVM-Unit-Tests in `GetPendingNotificationsUseCaseTest` (Regel-Aktivierung, Geburtstag, Name Day, Joint-Anniversary, Duplikat-Filterung) und `SnoozeNotificationUseCaseTest`.

229. **Einführung des Couple-Linking-Domain-Layers (Architecture & Code Quality):**
    - **GetCoupleSuggestionUseCase:** Kapselung der Ermittlung von Ehepartner-Kopplungsvorschlägen für den Hochzeitstag-Filter (unterstützt vollautomatisch unterschiedliche Nachnamen).
    - **Link- / Unlink- / Ignore-UseCases:** Auslagerung der entsprechenden DB-Schreibaktionen aus dem `HomeViewModel` in eigenständige Use Cases (`LinkAsCoupleUseCase`, `UnlinkCoupleUseCase`, `IgnoreCoupleSuggestionUseCase`).
    - **Refactoring:** Bereinigung des `HomeViewModel` und Anpassung der Testumgebungen (`HomeViewModelTest`, `HomeViewModelSearchTest`).
    - **Unit-Tests:** Erstellung von `GetCoupleSuggestionUseCaseTest` mit expliziter Absicherung für ungleiche Nachnamen bei gleichem Hochzeitsdatum.

230. **Einführung des Backup- & Kalendersynchronisations-Domain-Layers (Architecture & Code Quality):**
    - **ExportGiftIdeasUseCase & ImportGiftIdeasUseCase:** Kapselung des JSON-Exports/Imports von Geschenkideen.
    - **SetCalendarSyncEnabledUseCase & UpdateCalendarColorUseCase:** Kapselung der Aktivierung/Deaktivierung der Kalendersynchronisation sowie Farbänderungen.
    - **SyncCalendarUseCase:** Kapselung der Geschäftslogik zur Kalendersynchronisation.
    - **Refactoring:** Bereinigung von `BackupViewModel` und `CalendarViewModel` durch Entkopplung von Repository-Direktaufrufen.
    - **Unit-Tests:** Erstellung neuer Testklassen (`ExportGiftIdeasUseCaseTest`, `ImportGiftIdeasUseCaseTest`, `SetCalendarSyncEnabledUseCaseTest`) sowie Anpassung bestehender ViewModel-Tests.

231. **Auslagerung der Label-Filter-Logik in GetAvailableLabelsUseCase (Architecture & Code Quality):**
    - **GetAvailableLabelsUseCase:** Kapselung der Geschäftslogik zur Ermittlung der verfügbaren Filter-Labels (berechnet basierend auf Kontakten, Label-Konfigurationen und App-Einstellungen) unter `com.heckmannch.birthdaybuddy.domain.usecase`.
    - **Refactoring:** Entfernung der direkten Berechnung im `HomeViewModel` und stattdessen Verwendung des neuen Use Cases via Hilt-Injection.
    - **Unit-Tests:** Erstellung von `GetAvailableLabelsUseCaseTest` (Validierung von Label-Verhalten bei deaktivierten/aktivierten Filtern, Sortierung und Sonder-Labels) sowie Anpassung der Testumgebungen `HomeViewModelTest` und `HomeViewModelSearchTest`.

232. **Konsolidierung des MVI-Prinzips im HomeViewModel (Architecture & Code Quality):**
    - **Entfernung von Legacy-Wrapper-Methoden:** Die sechs verbleibenden Legacy-Eingabemethoden (`onSearchQueryChange()`, `onLabelSelected()`, `resetFilters()`, `syncContacts()`, `triggerScrollToTop()` und `triggerSearchFocus()`) wurden vollständig aus dem `HomeViewModel` entfernt, um eine parallele API zu vermeiden und das UDF/MVI-Prinzip zu stärken.
    - **Direkte Intent-Delegation:** Alle Aufrufstellen (in `MainActivity.kt`, `SyncSettingsScreen.kt`, `HomeViewModel.kt` selbst und den Unit-Tests) wurden auf den zentralen `onIntent(...)`-Handler umgestellt.
    - **Test-Stabilität:** Erfolgreiche Anpassung und Validierung der Unit-Tests (`HomeViewModelTest`, `HomeViewModelSearchTest`), um die korrekte Funktionalität nach dem Refactoring sicherzustellen.

233. **Explizites Offloading von CPU-intensiven Mappings auf `Dispatchers.Default` in `ContactRepositoryImpl` (Performance / Code Quality):**
    - **Problem:** Die Flows `allContacts` und `labelConfigs` in [ContactRepositoryImpl.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepositoryImpl.kt) führten pro Emission eine O(n)-Iteration über alle DB-Entitäten durch (`entities.map { mapper.toDomain(it) }`). Ohne explizites `flowOn` liefen diese CPU-intensiven Transformationen implizit auf dem Room-IO-Thread-Pool – entgegen der Projekt-Richtlinie §2.7, die rechenintensive Mappings auf `Dispatchers.Default` vorschreibt.
    - **Lösung:** Einfügen von `.flowOn(Dispatchers.Default)` unmittelbar nach dem `.map`-Operator und vor `.distinctUntilChanged()` bei den Flows `allContacts` und `labelConfigs`.
        - Die Reihenfolge `map → flowOn → distinctUntilChanged` ist korrekt: `flowOn` wirkt auf alle upstream-Operatoren (das Mapping), während `distinctUntilChanged` downstream auf dem Collector-Dispatcher läuft.
        - Import `kotlinx.coroutines.flow.flowOn` ergänzt.
    - **Analyse weiterer Flows:** Die vier verbleibenden `appSettingsDao`-Flows (`otherEventsEnabled`, `ignoredCouplePairs`, `labelsEnabled`, `potentialCouples`) führen ausschließlich triviale O(1)-Feldextraktionen durch und benötigen kein `flowOn`.
    - **Tests:** Alle bestehenden JVM-Unit-Tests grün (die Änderung ist rein additiv hinter dem Repository-Interface und berührt keine gemockten Implementierungen).

234. **Optimierung der Coroutine-Threading-Strategie in GetAvailableLabelsUseCase & HomeViewModel (Performance / Richtlinie §2.7):**
    - **GetAvailableLabelsUseCase:** Hinzufügen von `.flowOn(Dispatchers.Default)` am Ende der kombinierenden Flow-Kette in [GetAvailableLabelsUseCase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/domain/usecase/GetAvailableLabelsUseCase.kt). Dadurch werden die CPU-intensiven Berechnungen der verfügbaren Filter-Labels (Filterung, Sortierung, Einbindung von Pseudo-Labels) sicher vom Main-Thread entkoppelt.
    - **HomeViewModel - Audit & uiState:**
        - Audit von `coupleSuggestion`: Verifiziert, dass dieser Flow durch die Verwendung von `.flowOn(Dispatchers.Default)` im [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModel.kt) bereits korrekt im Default-Dispatcher ausgeführt wird.
        - Ergänzung von `.flowOn(Dispatchers.Default)` beim `uiState`-Flow vor dem `.stateIn(...)`-Terminaloperator. Dies sorgt dafür, dass die finale Konsolidierung des UI-States (einschließlich des Mappings zur Bereinigung der Labels bei deaktivierter Label-Filterung) im Default-Dispatcher ausgeführt wird.
    - **Validierung:** Alle bestehenden JUnit-Tests laufen weiterhin fehlerfrei durch.

235. **Erweiterte Unit-Tests für GetAvailableLabelsUseCase (Test-Abdeckung & Code Quality):**
    - **GetAvailableLabelsUseCaseTest:** Hinzufügen von 5 neuen, zielgerichteten Unit-Tests in [GetAvailableLabelsUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/GetAvailableLabelsUseCaseTest.kt) zur vollständigen Abdeckung der verfügbaren Label-Filter-Logik.
    - **Abgedeckte Szenarien:**
        1. Globale Deaktivierung der Label-Filterung (`labelsEnabled = false`) gibt eine leere Liste zurück.
        2. Alle zugewiesenen Labels sind als ignoriert (`isIgnored = true`) markiert, was zu einer leeren Liste führt.
        3. Jahrestage (`LABEL_ANNIVERSARY`) werden korrekt einbezogen, wenn `otherEventsEnabled = true` und Kontakte mit Jahrestagen vorhanden sind.
        4. Kontakte ohne Geburtstage blenden das Pseudo-Label `LABEL_NO_BIRTHDAY` ein, sofern es nicht in den Einstellungen ignoriert/versteckt wird.
        5. Eine Mischung aus Benutzer-Labels, Pseudo-Labels und System-Labels wird in der exakten Reihenfolge ausgegeben: Benutzer-Labels (alphabetisch) → `LABEL_NO_BIRTHDAY` → `LABEL_ANNIVERSARY` → `LABEL_NAME_DAY`.
    - **Validierung:** Alle neuen Tests wurden erfolgreich ausgeführt und verifiziert. Keine Regressionen in anderen Home-Screen-Tests.

236. **Unit-Tests für `ContactRepositoryImpl` (Test-Abdeckung & Richtlinie §2.6):**
    - **Unit-Tests:** Erstellung der neuen JVM-Unit-Testdatei [ContactRepositoryImplTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepositoryImplTest.kt) zur vollständigen Abdeckung kritischer Logikpfade des `ContactRepositoryImpl`.
    - **Abgedeckte Szenarien:**
        1. `allContacts`-Flow emittiert korrekt gemappte Domain-Objekte (`Contact`) bei DAO-Updates.
        2. `labelsEnabled`-Flow liefert den Standardwert `true`, wenn keine `AppSettingsEntity` existiert.
        3. `syncContacts()` bricht sofort ab und ruft keine System-Dienste auf, wenn die Kontakt-Berechtigung (`Manifest.permission.READ_CONTACTS`) verweigert ist.
        4. `addGiftIdea()` delegiert korrekt das Speichern an das `ContactUserDataDao` und aktualisiert den App-Cache im `ContactDao`.
        5. `labelConfigs`-Flow mappt korrekt die von der DAO gelieferten `LabelConfigEntity` in `LabelConfig` Domain-Modelle.
    - **Technische Besonderheiten:**
        - Umgehung des Hängens/Deadlocks bei Room-Transaktionen (`withTransaction`) auf gemockten Datenbanken im JVM-Kontext durch reflexionsbasierte Instanziierung von `TransactionElement(ContinuationInterceptor)`.
        - Standard-Dispatcher-Umleitung für Datenbank-Transaktionen über Executor-Mocking (`transactionExecutor`, `queryExecutor`) auf `it.run()`.
    - **Validierung:** Erfolgreicher Durchlauf aller Tests im Gradle-Task `testDebugUnitTest`.

237. **Übersetzung verbleibender deutscher Kommentare ins Englische (Code Quality / Documentation Safety):**
    - **Projekt-Richtlinie (§1):** Vollständige Übersetzung aller verbleibenden deutschen Inline-Kommentare und KDoc-Texte ins Englische zur Durchsetzung einheitlicher englischer Entwicklerdokumentation.
    - **ContactRepositoryImpl.kt:** Übersetzung von 18 internen Implementierungskommentaren zur Transaktionssteuerung, Synchronisation, Diffs, Rollbacks und Caching.
    - **ContactMapper.kt:** Übersetzung des KDoc-Klassenkommentars sowie von Hilfskommentaren zur Datumsformatierung und Test-Fallbacks.
    - **Audits:** Verifikation von `HomeViewModel.kt` und `GetContactsUseCase.kt` auf Freiheit von deutschen Kommentaren.

238. **Kapselung von `flowOn(Dispatchers.Default)` in `GetContactsUseCase` (Performance / §2.7):**
    - **Problem:** `GetContactsUseCase.invoke()` führte schwere CPU-Arbeit (Keyword-Suche, Couple-Merging `buildAnniversaryList`, Sortierung) innerhalb seines `combine`-Lambdas aus. Das `.flowOn(Dispatchers.Default)` wurde jedoch nicht vom Use Case selbst, sondern nachträglich vom Aufrufer (`HomeViewModel.filteredContacts`) gesetzt. Dies verletzte das Kapselungsprinzip: Ein zukünftiger Aufrufer könnte das `flowOn` vergessen und damit unbemerkt den Main-Thread blockieren.
    - **Lösung:** Gemäß Richtlinie §2.7 wurde `.flowOn(Dispatchers.Default)` als letzter Operator direkt in `invoke()` von [GetContactsUseCase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/domain/usecase/GetContactsUseCase.kt) eingebaut. Der Use Case kapselt nun vollständig seine eigene Threading-Entscheidung.
    - **`HomeViewModel.kt`:** Das jetzt redundante `.flowOn(Dispatchers.Default)` auf `filteredContacts` wurde entfernt. Das `.flowOn(Dispatchers.Default)` auf `uiState` (State-Konsolidierung) bleibt erhalten.
    - **KDoc:** Die KDoc von `invoke()` wurde um einen expliziten Hinweis auf den gepinnten Dispatcher und den Hinweis `Callers MUST NOT add an additional flowOn on top of this flow` ergänzt.
    - **Validierung:** Alle Unit-Tests (`testDebugUnitTest`) grün. Kein Regressionen.

239. **JVM-Unit-Tests für `NotificationRepositoryImpl` (Test-Abdeckung & Richtlinie §2.6):**
    - **Unit-Tests:** Erstellung der neuen JVM-Unit-Testdatei [NotificationRepositoryImplTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/repository/NotificationRepositoryImplTest.kt) zur vollständigen Absicherung der Einstellungs- und Benachrichtigungslogik.
    - **Abgedeckte Szenarien:**
        1. `allRules` und `settings` Flows emittieren korrekt gemappte Werte von DAOs an Domain-Modelle.
        2. `syncScheduling()` delegiert an den `NotificationScheduler` (Kombinationen von enabled/disabled App-Settings und leeren/befüllten Benachrichtigungsregeln).
        3. `updateSettings()` aktualisiert die App-Einstellungen unter `settingsMutex` Thread-Sicherheit und stößt anschließend `syncScheduling()` an.
        4. CRUD-Methoden für Benachrichtigungsregeln (`getAllRulesImmediate`, `insertRule`, `updateRule`, `deleteRule`) delegieren an DAO und lösen die Synchronisation aus.
        5. Alle Hilfs- und Statusmethoden für Pending-Notifications (z. B. Abfragen, IDs, dismissed-Zähler, Erledigt-Status und Löschvorgänge) delegieren korrekt an `PendingNotificationDao` und mappen Entities.
    - **Technische Rahmenbedingungen:**
        - Einsatz von `MockK` für relaxed Mocks aller DAOs und des Schedulers.
        - Umleitung der Main-Coroutine auf den Test-Dispatcher über `MainDispatcherRule` und Ausführung in `runTest`.

240. **Strukturelle Aufteilung von `FastScrollbar.kt` (Code Quality & Maintainability):**
    - **Motivation:** `FastScrollbar.kt` war mit ~25 KB / 525 Zeilen die größte UI-Datei im Projekt und enthielt sowohl die Public-API als auch eine vollständig isolierbare private Composable.
    - **Extraktion:** `ScrollbarBubble` (das animierte Label-Bubble neben dem Scrollbar-Thumb) wurde 1:1 in die neue Datei [ScrollbarBubble.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/ScrollbarBubble.kt) ausgelagert. Sichtbarkeit von `private` auf `internal` angehoben (minimale Erweiterung, da beide Dateien im selben Package `ui.screens.home.components.list` liegen – kein Public-API-Leak).
    - **`FastScrollbar.kt`** (verkleinert auf ~461 Zeilen): Enthält weiterhin `ScrollbarDefaults`, `ScrollSection` (Public-API), den vollständigen `FastScrollbar()`-Composable mit State-Management, Geometrie-Berechnungen, Drag-Gestures und Thumb-UI. KDoc um einen „File Structure"-Abschnitt ergänzt.
    - **`ScrollbarBubble.kt`** (neu, ~100 Zeilen): Vollständig isoliertes `internal fun ScrollbarBubble()`-Composable mit eigenem KDoc (inkl. Designentscheidungen zu `MutableTransitionState` und Lambda-Offset).
    - **Keine funktionalen Änderungen.** Alle Aufrufer (`HomeContent.kt`) kompilieren unverändert.
    - **Verifikation:** `.\gradlew compileDebugKotlin` erfolgreich.

241. **Migration von `ScrollbarBubble.kt` auf standardisierte `Dimensions.kt` Token (Code Quality & UI-Konsistenz):**
    - **Problem:** In [ScrollbarBubble.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/ScrollbarBubble.kt) wurden die Abstände (Paddings) hartkodiert mit `16.dp` und `8.dp` definiert. Dies verstößt gegen die UI-Richtlinien des Projekts, die eine einheitliche Nutzung von Dimensions-Token vorschreiben.
    - **Lösung:** Ersetzung aller hardcodierten `.dp`-Abstände durch standardisierte Token aus [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt):
        - `16.dp` wurde durch `SpacingNormal` ersetzt.
        - `8.dp` wurde durch `SpacingSmall` ersetzt.
        - Bereinigung der nicht mehr benötigten Imports (Entfernung von `import androidx.compose.ui.unit.dp`).
    - **Verifikation:** Erfolgreiche Kompilierung und Durchlauf aller Unit-Tests.

