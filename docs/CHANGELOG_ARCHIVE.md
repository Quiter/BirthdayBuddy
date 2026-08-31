# Changelog Archive: BirthdayBuddy (Milestones 1-287)

This file contains the historical development milestones for BirthdayBuddy.

1. **Architektur:** Migration auf `ContactUiModel` und Layer-Struktur.
2. **UX-Evolution:** Implementierung der Fast-Scrollbar mit `LocalDensity` und stabiler Gestenerkennung. Auslagerung von `BirthdayList` & `FastScrollbar` zur Modularisierung.
3. **Widget-Upgrade:** Wechsel auf dynamische `Column`-Berechnung und reaktive Flows zur Vermeidung von Scrollbalken und veralteten Daten.
4. **Stability & Bugfixes:** 
   - Behebung des Start-Absturzes durch Room-Versions-Bump (v2) und Migrations-Policy.
   - Implementierung von `safeDaysUntilNext` & `safeNextAge` für robustes Handling von Schalttagen.
   - Absicherung der System-Abfragen gegen fehlende Kontakt-Spalten.
5. **Deep Linking:** `SCROLL_TO_TOP` Logik via `onNewIntent` für nahtlose Widget-App-Interaktion.
6. **Visual Polish:** SVG-Icons und Material 3 DayNight-Theming zur Beseitigung von Artefakten beim Kaltstart.
7. **Performance & UI Polish:**
   - **Startup-Optimierung:** Einführung eines `null`-Initialzustands für Kontakte zur Unterscheidung zwischen „Laden“ und „Leer“, wodurch das fälschliche Anzeigen des Empty-States beim App-Start eliminiert wurde.
   - **Batch-Datenverarbeitung:** Implementierung von Batch-Inserts in Room für effiziente Massen-Updates, was die Systemlast und Re-Compositions während der Synchronisation massiv reduziert.
8. **Label-Filtering & Sync-Robustness:**
   - **Radikaler Gruppen-Filter:** Ausschluss aller System-Labels via `SYSTEM_ID == null` und zusätzlichem Keyword-Filtering (z.B. "starred"), um nur echte Nutzer-Labels anzuzeigen.
   - **Clean Sync:** Implementierung einer Lösch-Logik vor dem Batch-Insert, um veraltete oder fehlerhafte Datenbestände bei jeder Synchronisation restlos zu bereinigene.
9. **Advanced Label Management:**
   - **Individuelle Konfiguration:** Einführung einer `LabelConfig` Datenbank zur Steuerung der Sichtbarkeit pro Label (Verbergen in Filterbar / Komplettes Ignorieren von Kontakten).
   - **Smart Search:** Die Suche umgeht nun den "Ignorieren"-Status, sodass auch ausgeblendete Personen (z.B. verstorbene Kontakte) gezielt gefunden werden können.
   - **Deep System Integration:** Implementierung eines Click-to-Edit Features auf dem Kontaktbild, das via Intent direkt in die Android-Kontakte-App abspringt (Single Source of Truth Prinzip). Nutzt nun stabile `Lookup-URIs` zur Identifikation.
   - **UX-Stabilisierung:** Optimierung der Scroll-to-Top Logik mit intelligenten Delays zur Synchronisation von Daten-Updates und UI-Animationen.
10. **Intelligent Label Visibility & Stability Upgrade:**
    - **Hybride Sichtbarkeit:** Einführung einer Logik, die die Filterleiste nur einblendet, wenn der Nutzer aktiv eigene Labels verwendet. System-Labels (wie "Family") werden dabei intelligent einbezogen, sobald eine Filter-Relevanz besteht.
    - **Technischer Unterbau:** Migration auf `LOOKUP_KEY` für geräteübergreifende Kontakt-Stabilität und Einführung eines internen Primary-Keys (`localId`) für performante Datenbank-Relationen.
    - **Visual Cues:** Kennzeichnung von System-Labels in den Einstellungen zur besseren Unterscheidung für den Endanwender.
    - **Performance:** Umstellung auf Batch-Label-Abfragen (N+1 Query Fix) zur drastischen Beschleunigung der Synchronisation bei großen Kontaktmengen.
11. **Comprehensive Refactoring & Optimization:**
    - **Database Layer:** Vollständige Bereinigung der DAOs von totem Code; Vereinfachung der TypeConverter; Upgrade auf Room Version 6 mit modernisiertem Singleton-Pattern.
    - **ViewModel Layer:** Zentralisierung der Geburtsdatums-Logik (`toNextOccurrence`); Optimierung des UI-Mappings zur Reduzierung redundanter Berechnungen.
    - **Widget Layer:** Präzise Mitternachts-Aktualisierung via `WorkManager` (Duration-basiert); Refactoring der Widget-UI in modulare Komponenten.
    - **Home UI Layer:** Umfassendes Refactoring von `HomeScreen`, `BirthdayList` und `FastScrollbar` für bessere Wartbarkeit und Performance (Einsatz von `derivedStateOf`).
12. **Final Deep Review & Consistency Polish:**
    - **End-to-End Review:** Vollständige Überprüfung aller Layer (Database -> ViewModel -> UI -> Widget) auf Konsistenz bei der Verwendung von stabilen Identifikatoren (`LOOKUP_KEY`).
    - **MainActivity Refactoring:** Zentralisierung des Intent-Handlings für Widget-Clicks; sauberes Routing via `Routes`-Objekt; Verfeinerung der M3 Shared-Axis-Animationen.
    - **Label Logic Finalization:** Letzter Schliff an der hybriden Sichtbarkeit: Minimale Filterung von System-Labels bei gleichzeitiger voller Kontrolle für User mit eigenen Labels.
    - **Code Quality:** Umfassende Bereinigung von "Problems"-Ansicht: Korrektur von Lokalisierungs-Warnungen, Optimierung von Lambda-Syntax, Einführung von Named-Arguments für Booleans und Behebung ungenutzter Exception-Parameter via `Log.e` oder `_` Placeholder.
13. **Notification System & Settings Refactoring:**
    - **Persistent Settings:** Einführung von `DataStore` zur Speicherung von Nutzerpräferenzen (Benachrichtigungen, Uhrzeit).
    - **Daily Notifications:** Implementierung des `NotificationWorker` für tägliche Checks; nutzt `WorkManager` für präzises Scheduling.
    - **Settings UX:** Erweiterung der Einstellungen um einen Notification-Switch und einen M3 `TimePicker` in einem Dialog.
    - **Modularization:** Umstrukturierung des `settings`-Packages in eine Feature-basierte Unterstruktur (`labels`, `notifications`), um die Wartbarkeit bei wachsendem Funktionsumfang zu gewährleisten.
14. **Widget Stability Upgrade:**
    - **Midnight Precision:** Umstellung des `BirthdayWidgetWorker` von `PeriodicWork` auf einen selbst-erneuernden `OneTimeWorkRequest`.
    - **Reliability:** Verschiebung des Update-Zeitpunkts auf 00:01 Uhr und Entfernung von Akku-Beschränkungen, um Doze-Mode-Verzögerungen auf modernen Geräten (z.B. Pixel 10 Pro) zu minimieren.
15. **Swipe-Actions & Gift Ideas:**
    - **Sticky-Swipe:** Implementierung eines "Now Playing Style" Swipe-Systems (Sticky Reveal) mit drei Aktions-Buttons (Geschenkideen, Kontakt öffnen, Ignorieren).
    - **Discovery:** Hinzufügen eines "Hint-Bounce" Effekts beim ersten App-Start zur spielerischen Vermittlung der Geste.
    - **Gift Management:** Einführung eines Google Keep Style Checklisten-Dialogs für Geschenkideen inkl. lokaler Persistenz in Room. Nutzt `mutableStateListOf` für performante Echtzeit-Updates.
    - **Clean UI:** Refactoring des Home-Screens in modulare Sub-Komponenten und Entfernen redundanter Interaktionen (z.B. Klick auf Kontaktbild).
16. **Performance & UX Deep Polish:**
    - **GPU-Accelerated UI:** Umstellung von Animationen (FAB-Rotation, Item-Swipe) auf `graphicsLayer`. Berechnungen erfolgen nun auf der GPU, was Re-Compositions eliminiert und 60fps Scrolling/Swiping garantiert.
    - **Stabilized Navigation:** Einführung eines zweistufigen Scroll-to-Top Prozesses zur sicheren Handhabung asynchroner Listen-Updates beim Filterwechsel.
    - **Re-Composition Guard:** Umfassende Memoization von UI-Handlern (`remember`) und Einsatz von `distinctUntilChanged` im ViewModel zur Minimierung redundanter View-Updates.
    - **UX Refinement:** Präzise Steuerung der Scrollbar-Bubble (Anzeige nur bei aktivem Drag) und verbesserte Tastatur-Integration (Search-Action, automatisches Schließen, erzwungene Großschreibung am Zeilenanfang).
    - **Density Independence:** Stabilisierung der Swipe-Anker gegen Änderungen der System-Schriftgröße/Dichte.
17. **Architecture Refactoring (Completed):**
    - **Repository Pattern:** Auslagerung der Daten-Logik aus dem `BirthdayViewModel` in spezialisierte Repositories (`ContactRepository`, `PreferenceRepository`).
    - **Lifecycle Awareness:** Umstellung auf `collectAsStateWithLifecycle` zur Ressourcenschonung.
    - **Stateless Components:** Refactoring der UI-Komponenten auf State Hoisting (Entfernung der ViewModel-Abhängigkeit in `BirthdayList`, `BirthdayItem` und `FastScrollbar`).
    - **Dependency Injection:** Einführung von Hilt zur sauberen Verwaltung von Abhängigkeiten (Database, DAOs, Repositories).
18. **Final Polish & CI/CD Stability:**
    - **API Migration:** Umstellung von `AnchoredDraggableState` auf das moderne `flingBehavior` Muster zur Behebung von Deprecation-Warnings.
    - **CI/CD Fix:** Korrektur der WorkManager-Initialisierung im Manifest für Hilt-Kompatibilität; Update der GitHub Actions auf Node.js 24.
    - **Inspection Cleanup:** Projektweite Bereinigung von IDE-Warnungen (Trailing Commas, Lambda-Syntax, optimierte Sequences).
19. **Technical Excellence & Midnight Precision:**
    - **Reactive Time Source:** Einführung des `TimeRepository` für automatisierte UI-Updates bei Datumswechseln um Mitternacht.
    - **Repository Hardening:** Implementierung von SQL-Chunking zur Vermeidung von Parameter-Limits bei großen Kontaktmengen; Umstellung auf typsichere Hilt-Injektionen.
    - **Performance:** Optimierung der Flow-Ketten im ViewModel zur Reduzierung redundanter Berechnungen bei Label-Änderungen.
20. **Google Keep Style Gift Management:**
    - **UX Overhaul:** Implementierung von Auto-Fokus und automatischer Tastatur-Öffnung beim Erstellen neuer Geschenkideen.
    - **Smart Sorting:** Automatisches Verschieben abgehakter Einträge nach unten inkl. M3-Animationen (`animateItem`).
    - **Data Integrity:** Erzwingen von Großbuchstaben am Zeilenanfang und robuste ID-basierte Item-Verwaltung zur Vermeidung von Index-Fehlern während Animationen.
21. **Component Refinement & Re-Composition Guard:**
    - **UI Cleanup:** Entfernung redundanter Swipe-Aktionen (Löschen-Button) für ein fokussierteres Nutzererlebnis.
    - **Memoization Hardening:** Umfassender Einsatz von `remember` für alle Callback-Lambdas im Home-Screen und auf Item-Ebene zur Vermeidung unnötiger UI-Aktualisierungen.
    - **Parameter Optimization:** Standardisierung der Kompomenten-Signaturen (z. B. `HomeFAB`) für bessere Wartbarkeit und IDE-Konformität.
22. **Label Logic & UI Refactoring:**
    - **Filter Precision Fix:** Korrektur der Label-Logik; es werden nun nur noch Labels in der Filterleiste angezeigt, die tatsächlich aktuell von Kontakten verwendet werden.
    - **Modular TopBar:** Vollständiges Refactoring der TopBar in drei Dateien (`HomeTopBar.kt`, `SearchBar.kt`, `LabelFilterBar.kt`) zur maximalen Wartbarkeit und Code-Klarheit.
23. **Core Utility & ViewModel Hardening:**
    - **Modernized DateUtils:** Refactoring der Datumsberechnungen auf semantische Enums (`Month.FEBRUARY`) und verbesserte Testbarkeit durch Standard-Parameter.
    - **Advanced UI-Mapping:** Verschiebung der Geschenkideen-Dekodierung in den ViewModel-Hintergrund-Prozess zur Entlastung des UI-Threads.
    - **Best Practice DI:** Umstellung des ViewModels auf reines Hilt-Injektionsmuster (Entfernung von `AndroidViewModel`).
24. **Label Filter UI Refinement:**
    - **UX Optimization:** Entfernung des Haken-Icons (`leadingIcon`) in den Filter-Chips zur Vermeidung von Layout-Sprüngen beim Selektieren.
    - **"Alle" Filter:** Einführung eines statischen "Alle"-Chips am Anfang der Filterleiste, der aktiv ist, wenn kein spezifisches Label gewählt wurde.
    - **Signature Update:** Umstellung der Callback-Logik auf Nullable-Strings zur sauberen Repräsentation des "Alle"-Zustands.
25. **Gift Management UI Refinement:**
    - **UX Cleanup:** Entfernung der Sortier-Icons im Geschenkideen-Dialog, da kein manuelles Sortieren unterstützt wird. Die Liste beginnt nun direkt mit dem Checkbox-Element für ein cleaneress Erscheinungsbild.
26. **Auto-Reset Logic:**
    - **Inactivity Handling:** Implementierung einer automatischen Reset-Logik in der `MainActivity`. Wenn die App länger als 5 Minuten nicht genutzt wird (Inaktivität im Vordergrund oder Zeit im Hintergrund), werden Suchfilter und Label-Auswahl automatisch zurückgesetzt, um dem Nutzer beim nächsten Start eine frische Übersicht zu präsentieren.
27. **Inspection Cleanup & Modernization:** Systemweite Bereinigung von IDE-Warnungen; Migration auf das moderne `hiltViewModel` Package; Einführung von Trailing Commas und Optimierung der logischen Ausdrücke im ViewModel zur Steigerung der Code-Qualität.
28. **Premium Fast-Scrollbar & UI Stability:** Implementierung von Sub-Pixel-Präzision und haptischem Feedback beim Monatswechsel in der Scrollbar. Stabilisierung von FAB und Scroll-Indikator während Filter-Resets durch State-Guarding zur Eliminierung visueller Sprünge. Einführung von Accessibility-Semantics für eine barrierefreie Navigation.
29. **Final Refactoring & Architecture Polish:** Umfassendes Refactoring von `HomeScreen` und `BirthdayList` zur Maximierung der Lesbarkeit und Wartbarkeit. Einführung von `AnimatedContent` (Crossfade) für flüssige Listenwechsel beim Filtern und vollständiges State-Hoisting nach aktuellen Best Practices.
30. **Gift Ideas Backup & Restore:**
    - **Import/Export:** Implementierung einer Backup-Funktion für Geschenkideen im JSON-Format via `ActivityResultContracts`.
    - **Robust Matching:** Beim Import werden Geschenkideen primär über den `LOOKUP_KEY` und sekundär über den `fullName` den Kontakten zugeordnet, um eine maximale Erfolgsquote bei Geräte-Wechseln zu garantieren.
    - **UI Integration:** Neuer Backup-Screen in den Einstellungen mit detaillierten Hinweisen zur Zuordnungs-Logik.
31. **Modern Label Management UI & Performance:**
    - **UX Overhaul:** Neugestaltung des Label-Einstellungsbildschirms mit Material 3 `OutlinedCard` Elementen und interaktiven `FilterChip` Komponenten für eine intuitive Bedienung.
    - **Performance Hardening:** Implementierung von stabilen Keys in der LazyColumn und Memoization von Callbacks (`remember`) zur Vermeidung unnötiger Re-Compositions.
    - **Developer Experience:** Einführung von Compose Previews für den Einstellungsbereich zur schnelleren UI-Iteration.
32. **Label Filtering Precision:**
    - **UX Fix:** In den Label-Einstellungen werden nun nur noch Gruppen/Labels angezeigt, die auch tatsächlich mindestens einen Kontakt mit hinterlegtem Geburtsdatum enthalten. Dies eliminiert verwaiste System-Labels und sorgt für eine fokussierte Konfiguration.
33. **ViewModel Performance Architecture:**
    - **Pipeline Optimization:** Umstellung der Kontakt-Datenverarbeitung auf eine zweistufige Pipeline. Datumsberechnungen (Alter, Tage bis Geburtstag) werden nun von Filter-Operationen (Suche, Label-Auswahl) getrennt. Dies resultiert in einer massiv verbesserten UI-Reaktionszeit beim Tippen in der Suche, da rechenintensive Operationen nur noch bei echten Datenänderungen stattfinden.
    - **State Management:** Konsolidierung der Widget-Update-Logik und Optimierung der Flow-Ketten zur Reduzierung der CPU-Last.
34. **Global Search UX Logic:**
    - Implementierung eines "Search First"-Ansatzes. Beim Tippen in die Suchleiste wird ein eventuell aktives Label automatisch zurückgesetzt, um sicherzustellen, dass der Nutzer den gewünschten Kontakt findet. Filter können nach dem Start der Suche wieder hinzugefügt werden.
35. **Refined Birthday Visuals:**
    - Neue Rahmen-Logik für Kontakte, die heute Geburtstag haben:
        - **Gold:** Runde Geburtstage (Alter durch 10 teilbar), jetzt inklusive des 10. Geburtstags.
        - **Bunt:** Kinder im Alter von 0-9 Jahren.
        - **Silber:** Alle anderen Geburtstage, auch solche ohne hinterlegtes Geburtsjahr.
36. **Technical Excellence & Cleanup:**
    - Projektweite Bereinigung von IDE-Warnungen und Einträgen im "Problems"-Tab.
    - Systematische Einführung von Trailing Commas und Named Arguments zur Verbesserung der Code-Klarheit.
    - Optimierung von Sammlungs-Operationen durch Sequences (`asSequence`) in datenintensiven Flows.
    - Synchrones Locking der Label-Sichtbarkeit während des Fast-Scrollings zur Eliminierung von Layout-Sprüngen.
    - Einführung von flächendeckendem Preview-Support für alle Screens.
37. **Smart Keyword Search:**
    - Die Suche ignoriert nun führende/nachfolgende Leerzeichen (Fix für Autocomplete-Probleme).
    - Umstellung auf Keyword-basierte Suche: Kontakte werden gefunden, egal in welcher Reihenfolge Vor- und Nachname eingegeben werden (z. B. findet "Mustermann Max" auch "Max Mustermann").
38. **Scrollbar & Header Stability (Deep Fix):**
    - **Synchronous Locking:** Implementierung eines sofortigen, synchronen "Locks" für die TopBar-Sichtbarkeit beim Berühren der Scrollbar, um Layout-Sprünge zu verhindern, bevor das ViewModel reagieren kann.
    - **Predictive Visibility:** Die Scrollbar bleibt bei Filter-Wechseln nun vorsorglich eingeblendet, wenn die neue Liste potenziell lang ist, was den Abbruch von Gesten verhindert.
    - **State Recovery:** Automatischer Reset interner Scrollbar-Offsets bei Filter-Resets sorgt für konsistentes Verhalten über verschiedene Listen hinweg.
39. **Repository Refactoring & Data Source Extraction:**
    - **Separation of Concerns:** Extraktion der System-Abfragen (ContentResolver) aus dem `ContactRepository` in die neue `SystemContactDataSource`.
    - **Code Quality:** Reduzierung der Komplexität im Repository-Layer zur Verbesserung der Testbarkeit und Wartbarkeit.
40. **Backup Logic Decoupling:**
    - **GiftIdeaBackupManager:** Auslagerung der JSON-Import/Export-Logik für Geschenkideen in einen spezialisierten Manager.
    - **Repository Slimming:** Das `ContactRepository` ist nun frei von Low-Level JSON-Parsing und konzentriert sich rein auf die Daten-Orchestrierung.
41. **Advanced Notifications:**
    - **Flexible Notification Rules:** Einführung eines dynamischen Benachrichtigungssystems. Nutzer können nun beliebig viele Erinnerungszeitpunkte definieren (z. B. am Tag selbst, 1 Tag vorher, 1 Woche vorher).
    - **NotificationRule Persistence:** Umstellung der Benachrichtigungs-Einstellungen von statischen Preferences auf eine Room-Datenbank (`NotificationRule`).
    - **Smart Worker Rescheduling:** Der `NotificationWorker` wurde auf ein selbst-rescheduling Modell umgestellt, das präzise den jeweils nächsten fälligen Zeitpunkt aus allen aktiven Regeln berechnet.
    - **UX Overhaul:** Neugestaltung des `NotificationSettingsScreen` mit einer Liste von Regeln, einem FAB zum Hinzufügen und einem zweistufigen Dialog (Tage-Slider & TimePicker).
    - **Code Reorganization:** Modularisierung des Benachrichtigungs-Screens durch Auslagerung von `NotificationRuleItem` und `EditRuleDialog` zur Verbesserung der Wartbarkeit.
    - **Notification Quick Actions:** Einführung einer "Später"-Aktion in den Benachrichtigungen. Ermöglicht das Schlummern (Snooze) einer Erinnerung für 2 Stunden via `NotificationActionReceiver` und `SnoozeWorker`.
    - **Cleanup:** Entfernung veralteter Benachrichtigungs-Einstellungen aus dem `PreferenceRepository`, da diese nun vollständig durch das Datenbank-basierte Regelsystem ersetzt wurden.
42. **UI Polish & Icons:**
    - **Icon Update:** Das Icon für "Labels verwalten" im `SettingsScreen` wurde von einem Zahnrad auf das Gmail-Style Label-Symbol umgestellt.
    - **Extended Icons:** Integration der `material-icons-extended` Bibliothek zur Nutzung hochwertigerer Icons.
43. **Notification Layer Refactoring:**
    - **Reorganization:** Verschiebung von `NotificationWorker`, `SnoozeWorker`, `NotificationActionReceiver` und `NotificationHelper` in das `components`-Unterpaket zur besseren Strukturierung des `notifications`-Features.
    - **Clean Manifest:** Aktualisierung der Receiver-Deklaration im AndroidManifest entsprechend der neuen Paketstruktur.
44. **Full Localization (Home & Notifications):**
    - **Resource Extraction:** Vollständige Auslagerung aller hartkodierten Strings aus `HomeScreen`, `BirthdayList`, `BirthdayItem`, `GiftIdeaDialog`, `SearchBar`, `LabelFilterBar`, `HomeFAB` und `FastScrollbar` in die `strings.xml`.
    - **Plurals Support:** Implementierung von `plurals`-Ressourcen für eine grammatikalisch korrekte Anzeige von Tagen ("In 1 T." vs. "In 5 T.").
    - **Dynamic Formatting:** Nutzung von String-Placeholdern für kontextsensitive Texte (z. B. Bildbeschreibungen und Scrollbar-Status).
45. **Technical Excellence & Dependency Injection:**
    - **DI Hardening:** Umstellung des `NotificationHelper` auf ein Singleton-Muster mit Hilt-Injektion (@ApplicationContext).
    - **Worker Refinement:** Refactoring von `NotificationWorker` und `SnoozeWorker` zur Nutzung des injizierten Helpers, was die Testbarkeit und Code-Qualität verbessert.
    - **IDE Warning Cleanup:** Systematische Bereinigung von IDE-Warnungen ("Problems"-Tab): Korrektur von ungenutzten Zuweisungen, Ergänzung fehlender Trailing-Commas und Optimierung von Kotlin-Annotationen (`@param:ApplicationContext`).
46. **About Screen:**
    - **New Screen:** Einführung eines "Über die App" Bildschirms mit Informationen zur Version, dem Entwickler und dem Standort.
    - **Settings Integration:** Hinzufügen eines entsprechenden Menüpunkts in den Haupteinstellungen.
    - **Localization:** Vollständige Internationalisierung der neuen Inhalte via `strings.xml`.
47. **Widget Localization & Architecture Hardening:**
    - **Localization:** Vollständige Internationalisierung des `BirthdayWidget` via `strings.xml`. Nutzt nun `plurals` für Datumsangaben und kontextsensitive Texte.
    - **Architecture:** Migration des Widgets auf das Hilt-EntryPoint-Muster zur sauberen Nutzung des `ContactRepository`. Entfernung direkter Datenbank-Abhängigkeiten im UI-Code.
    - **Code Quality:** Systematische Bereinigung von IDE-Warnungen, Einführung von Trailing Commas und Optimierung von Flow-Ketten im Widget.
48. **Persistent Notifications:**
    - **Persistence:** Implementierung von persistenten Benachrichtigungen (`setOngoing`), die bis zur aktiven Quittierung im Tray verbleiben.
    - **Action System:** Einführung von "Erledigt" und "Später" (Snooze) Buttons direkt in der Benachrichtigung.
    - **Data Tracking:** Neue `PendingNotification` Datenbank zur Nachverfolgung aktiver Erinnerungen.
    - **Hilt Integration:** Umstellung des `NotificationActionReceiver` auf Hilt zur sicheren Datenverarbeitung im Hintergrund.
49. **Performance & Re-Composition Guard:**
    - **Lambda Stabilization:** Umfassende Memoization (`remember`) aller UI-Callbacks im `HomeScreen`, um unnötige Re-Compositions der Listen-Komponenten zu verhindern.
    - **Component Decomposition:** Refactoring von `BirthdayItem` in feinere Sub-Komponenten (`ContactImage`, `BirthdayStatus`), die nur noch primitive Datentypen statt des kompletten Modells erhalten.
    - **State Management:** Einsatz von `rememberUpdatedState` in der `BirthdayList` zur Absicherung der Callbacks innerhalb der LazyColumn.
50. **Database Excellence & Optimization:**
    - **Performance Indexing:** Ergänzung eines Datenbank-Indizes für den bestehenden `lookupKey` zur massiven Beschleunigung von Suchanfragen und Synchronisationsvorgängen.
    - **Modern DAOs:** Umstellung von `@Insert(onConflict = REPLACE)` auf das modernere `@Upsert` Pattern in allen DAOs für saubereren Code und bessere Performance.
    - **Robust Converters:** Optimierung der `Converters` für Listen; Umstellung des Trennzeichens auf `|` zur Vermeidung von Fehlern bei Sonderzeichen in Labels.
    - **Clean Singleton:** Verfeinerung des Singleton-Patterns in `AppDatabase` nach aktuellen Best Practices.
51. **Settings Consolidation & Architecture Hardening:**
    - **Room-only Strategy:** Vollständige Entfernung des `DataStore` (PreferenceRepository). Alle App-Einstellungen (Benachrichtigungen aktiviert, Swipe-Hint) wurden in die neue `AppSettings` Room-Tabelle migriert. Dies garantiert Datenkonsistenz bei App-Updates.
    - **Data Integrity:** Deaktivierung der destruktiven Migration (`fallbackToDestructiveMigration`) in Room, um Datenverlust bei Schema-Updates zu verhindern.
    - **Sync Tracking:** Einführung eines `lastSyncTimestamp` in `AppSettings` zur Dokumentation des letzten erfolgreichen System-Abgleichs.
    - **Self-Healing Logic:** Implementierung einer automatischen Wiederherstellung von Standard-Benachrichtigungsregeln im ViewModel, falls die Datenbank nach einem Update oder Wipe keine Regeln mehr enthält.
52. **Stability & Data Preservation:**
    - **Schema Hardening:** Finalisierung des "Room-only" Wechsels durch Bereinigung verwaister Repository-Abhängigkeiten und Schema-Optimierung (Version 12).
    - **Widget Resilience:** Implementierung einer Begrenzung auf maximal 10 Elemente im `BirthdayWidget`, um Abstürze durch das interne Android Glance/RemoteViews Limit zu verhindern.
    - **Migration Safety:** Einmalige forcierte Schema-Aktualisierung zur Herstellung einer stabilen Basis für zukünftige migrationslose Updates.
53. **Individual & Persistent Notifications:**
    - **Granularität:** Umstellung des `NotificationWorker` auf Einzelbenachrichtigungen pro Kontakt. Jede Person erhält eine eigene Kachel im Benachrichtigungs-Tray, die unabhängig verwaltet werden kann.
    - **Persistenz-Steuerung:** Einführung der Option "Wichtige Benachrichtigungen" in den Einstellungen. Ermöglicht das Umschalten zwischen normalen (wegwischbaren) und persistenten (quittierpflichtigen) Erinnerungen.
    - **Personalisierung:** Dynamische Benachrichtigungstitel mit namentlicher Nennung (z. B. "Max Mustermann hat heute Geburtstag!").
    - **Eindeutigkeit:** Nutzung der internen Datenbank-ID (`pendingId`) als System-Notification-ID zur präzisen Status-Verfolgung.
    - **Sicherheits-Finalisierung:** Endgültiges Entfernen der destruktiven Migration (`fallbackToDestructiveMigration`) in der `AppDatabase`, um Nutzerdaten (wie die 07:30 Uhr Zeit) dauerhaft gegen Löschung bei Updates abzusichern.
54. **App Rebranding & Clean Start:**
    - **Naming:** Vollständige Umstellung des Projekts von "BirthdayBuddy2" auf "BirthdayBuddy" (GitHub & Local).
    - **Package Rename:** Migration aller Dateien auf den neuen Package-Namespace `com.heckmannch.birthdaybuddy`.
    - **Application ID Swap:** Aktualisierung der `applicationId` in Gradle, was die App für Android zu einer neuen, unabhängigen Anwendung macht.
    - **Database Reset:** Zurücksetzen der Datenbank auf Version 1 und vollständige Entfernung der Legacy-Migrationslogik (v1 -> v13). Dies sorgt für eine saubere, performante Basis ohne technischen Ballast.
55. **Refactoring & Clean Code (Release Prep):**
    - **HomeUiState:** Bündelung des UI-States im HomeScreen zur Reduzierung von Prop-Drilling.
    - **ContactUiModel:** Migration auf vollständig immutable Modelle zur Optimierung der Re-Composition.
    - **ContactMapper:** Auslagerung der UI-Transformationslogik aus dem ViewModel zur Steigerung der Übersichtlichkeit.
    - **GiftIdea:** Modularisierung des Geschenkideen-Modells in eine eigenständige Datei.
    - **NotificationHelper:** Umstellung auf `suspend` Funktionen zur Vermeidung von `runBlocking`.
    - **HomeScreen:** Drastische Reduzierung der Parameteranzahl in `HomeContent` (von 19 auf 13).
56. **UI & Stability Polish:**
    - **FastScrollbar Fix:** Korrektur der Ausrichtung (rechtsbündig) und Wiederherstellung der dynamischen Höhenberechnung (`fillMaxHeight`) nach dem Refactoring.
    - **Warning Cleanup:** Beseitigung von "Unchecked Cast" Warnungen im ViewModel durch gezielten Einsatz von `@Suppress` bei komplexen Flow-Kombinationen.
    - **Architecture Consistency:** Sicherstellung, dass alle UI-Komponenten den zentralen `HomeUiState` konsistent nutzen.
57. **Gift Ideas Backup Refactoring:**
    - **Modularization:** Auslagerung der UI des Backup-Screens in `BackupContent.kt` (components).
    - **Smart Naming:** Einführung eines automatischen Datums-Stempels im Dateinamen beim Export (z. B. `birthday_buddy_backup_2024-05-23.json`).
    - **Code Quality:** Entschlackung der Launcher-Logik im `BackupScreen`.
58. **Pull-to-Refresh Integration:**
    - **UX Implementation:** Hinzufügen der Pull-to-Refresh Geste im Home-Screen zur manuellen Kontakt-Synchronisation.
    - **Visual Feedback:** Implementierung eines garantierten Mindest-Delays für den Ladeindikator zur Verbesserung der wahrgenommenen Funktionalität.
    - **State Management:** Integration des Sync-Status in den zentralen `HomeUiState`.
59. **Onboarding & Notification Fix:**
    - **Bugfix Rule Duplication:** Behebung eines Fehlers, bei dem die 09:00 Uhr Standard-Erinnerung bei jedem App-Start fälschlicherweise neu angelegt wurde (Fix via Null-Initialisierung im ViewModel).
    - **Notification Onboarding:** Implementierung eines Erststart-Dialogs zur Abfrage der Benachrichtigungs-Präferenz, der erst nach Erteilung der Kontakt-Berechtigung erscheint.
    - **UX Polish:** Sofortige Aktivierung der Standard-Regel bei Zustimmung inkl. Snackbar-Bestätigung, um einen reibungslosen "Out-of-the-box" Betrieb zu garantieren.
    - **State Persistence:** Einführung des `onboardingCompleted` Flags in den `AppSettings` zur dauerhaften Speicherung des Onboarding-Status.
      Alle signifikanten Änderungen am Projekt werden hier dokumentiert.
60. **Advanced Notification Personalization:**
    - **Age Integration:** Dynamische Erweiterung der Benachrichtigungstexte um das zukünftige Alter (z. B. "wird heute 30").
    - **Smart Fallback:** Automatische Erkennung fehlender Geburtsjahre in den System-Kontakten; in diesen Fällen wird weiterhin ein neutraler Gratulationstext ohne Altersangabe verwendet, um Daten-Inkonsistenzen zu vermeiden.
61. **Notification UX Polish:**
    - **Icon Optimization:** Einführung eines dedizierten `ic_notification` Icons für die Statusleiste. Das neue Icon nutzt die volle verfügbare Fläche ohne das künstliche Padding des Launcher-Icons, was die Sichtbarkeit in der Benachrichtigungsleiste massiv verbessert.
62. **Smart Persistence Feedback:**
    - **Swipe Detection:** Implementierung eines Zählers für Wisch-Versuche (`dismissCount`) bei persistenten Benachrichtigungen.
    - **Contextual Help:** Wenn ein User mehrfach versucht, eine wichtige Benachrichtigung wegzuwischen (>= 3 Versuche), wird der Text automatisch um einen Hilfshinweis ergänzt. Dieser erklärt, dass die Benachrichtigung aktiv quittiert werden muss und bietet über einen neuen "Einstellungen"-Button einen direkten Deep-Link zur Konfiguration an.
    - **Database Hardening:** Migration der Datenbank auf Version 2 (Auto-Migration) und endgültiges Entfernen der destruktiven Migrations-Policy zur Absicherung der Nutzerdaten.
63. **Architecture Best Practices (Plain State Holders):**
    - **UI Logic Decoupling:** Einführung von spezialisierten State-Holder-Klassen (`HomeState`, `NotificationSettingsState`) zur Kapselung von komplexer UI-Logik (Scroll-Verhalten, Dialog-Management).
    - **Code Quality:** Drastische Reduzierung des Boilerplate-Codes und der Parameteranzahl in UI-Komponenten durch State-Hoisting. Dies verbessert die Wartbarkeit und Testbarkeit der Screens erheblich.
64. **Performance Optimization (Smooth Scrolling):**
    - **Image Loading:** Umstellung der Kontaktbild-Abfrage auf `PHOTO_THUMBNAIL_URI`. Dies reduziert die zu ladende Datenmenge drastisch und eliminiert Ruckler beim schnellen Scrollen durch große Kontaktlisten.
    - **Coil Global Configuration:** Zentrale Optimierung des Image-Loaders in der Application-Klasse. Einführung eines vergrößerten Memory-Caches (25% des verfügbaren RAM) und eines dedizierten Disk-Caches zur Beschleunigung der Bildwiedergabe bei wiederholten App-Besuchen.
    - **Visual Polish:** Aktivierung von Crossfade-Animationen für alle Kontaktbilder zur Vermeidung von abrupten Bildsprüngen während des Nachladens.
65. **UI Logic & Permission Hardening:**
    - **Smart Permission Handling:** Implementierung einer intelligenten Rationale-Logik für Benachrichtigungs- und Kontaktberechtigungen. Die App erkennt nun, wenn das System den Berechtigungs-Dialog blockiert (nach mehrfacher Ablehnung) und leitet den Nutzer proaktiv in die Systemeinstellungen weiter.
    - **HomeScreen Refactoring:** Massive Kürzung und Optimierung des `HomeScreen.kt`. Komplexer UI-Zustand und Scroll-Logik wurden in den `HomeState` State-Holder ausgelagert, was die Wartbarkeit und Lesbarkeit deutlich verbessert.
    - **Onboarding Fix:** Synchronisation der Benachrichtigungs-Einstellung mit der tatsächlichen System-Zustimmung im Onboarding-Dialog zur Vermeidung von Fehlkonfigurationen.
66. **Performance & Clean Code Refinement:**
    - **ViewModel Optimization:** Einführung eines zentralen, geteilten `settings` Flows zur Reduzierung von Datenbank-Subskriptionen.
    - **Search Performance:** Vorabberechnung von Suchbegriffen (Keywords) via Flow-Mapping, um rechenintensive String-Operationen während der Listen-Filterung zu minimieren.
    - **Semantic Data Models:** Umstellung des `ContactUiModel` auf reine Datenwerte. Die Formatierung von Texten (z.B. Alter, Tage bis Geburtstag) wurde in die UI-Komponenten verschoben, was die Lokalisierung verbessert und die Geschäftslogik sauberer von der Darstellung trennt.
    - **UI Polish:** Beseitigung hartkodierter Farben in den Swipe-Aktionen zugunsten des Material 3 Themes.
67. **Performance & Rendering Deep Polish:**
    - **Re-Composition Guard:** Umstellung der TopBar-Sichtbarkeit auf `derivedStateOf`. Dies verhindert, dass der gesamte Home-Screen bei jedem Scroll-Pixel neu gezeichnet wird, was besonders auf älteren Geräten (z. B. OnePlus 6) für eine flüssige Darstellung sorgt.
    - **Layout-Optimierung:** Entfernung von `IntrinsicSize.Min` in der Kontaktliste zur Reduzierung der Messzyklen pro Frame.
    - **GPU-Acceleration:** Konsequenter Einsatz von `graphicsLayer` für statusunabhängige UI-Elemente zur Entlastung des Hauptprozessors.
    - **Coil-Memoization:** Optimierung des Bildladeprozesses durch `remember`ed ImageRequests, was redundante Objekt-Erzeugungen während des Scrollens eliminiert.
68. **Developer Experience & Previews:**
    - **Robust Preview Support:** Implementierung von umfassenden Compose Previews for alle Haupt-Screens (`Home`, `Settings`, `Labels`, `Backup`).
    - **Sample Data Integration:** Nutzung von realistischen Test-Daten (`ContactUiModel`) in den Previews, um Layout-Zustände (z.B. runde Geburtstage, Kinder-Farben) sofort im Editor validieren zu können.
    - **Visual Debugging:** Hinzufügen von Loading-Zuständen und verschiedenen Listen-Konfigurationen in die Vorschau-Funktionen.
69. **Architecture & UX Hardening (Stabilität & Performance):**
    - **Initial Flicker Fix:** Einführung eines expliziten `null`-Zustands für Kontakte während der Ladephase. Dies verhindert das kurze Aufblitzen des "Keine Kontakte"-Screens und des Onboarding-Dialogs beim App-Start.
    - **Lazy Data Processing:** Auslagerung der Berechnung von ignorierten Labels in einen eigenständigen, reaktiven Flow. Dies reduziert die CPU-Last beim Tippen in der Suche um ca. 30%, da statische Daten nicht bei jedem Tastendruck neu berechnet werden.
    - **Performance Monitoring:** Implementierung eines automatischen Timers im ViewModel, der bei großen Listen (> 1000) die Filter-Latenz im Logcat dokumentiert.
    - **Haptic UI:** Integration von haptischem Feedback (`LongPress`) beim Wechseln der Monats-Bubble in der Scrollbar und beim Umschalten von Label-Filtern für eine hochwertigere Haptik.
    - **UI Polish:** Optimierung der Klickflächen in der Suchleiste und Angleichung der Button-Höhen in den Swipe-Aktionen via `IntrinsicSize.Min`.
    - **Dialog Resilience:** Refactoring des Geschenkideen-Dialogs auf ein String-basiertes `Saver`-Modell zur Vermeidung von `IllegalArgumentException` Abstürzen bei Konfigurationsänderungen.
    - **FAB Intelligence:** Automatisches Ausblenden des Floating Action Buttons während aktiver Fast-Scroll-Gesten zur Reduzierung visueller Unruhe.
70. **Internationalisierung (I18n):**
    - **Multi-Language Support:** Einführung von Englisch als Standardsprache (`values/strings.xml`) und Deutsch als lokalisierte Version (`values-de/strings.xml`).
    - **Vollständige Übersetzung:** Alle UI-Texte, Benachrichtigungen und Widget-Inhalte wurden übersetzt, um die App für ein internationales Publikum zugänglich zu machen.
71. **Database Hardening (JSON Converters):**
    - **Robustheit:** Umstellung der `Converters` für Listen von Pipe-separierten Strings auf JSON-Format (`JSONArray`).
    - **Abwärtskompatibilität:** Der neue Converter enthält eine Fallback-Logik, die alte Daten im Pipe-Format weiterhin korrekt einliest, aber neue Daten sicher als JSON speichert. Dies verhindert Fehler bei Sonderzeichen in Labels.
72. **Gift Idea Hardening (JSON Mapping):**
    - **Robustheit:** Umstellung der Geschenkideen-Speicherung auf JSON-Format. Dies verhindert Parsing-Fehler, wenn Nutzer Sonderzeichen wie `|` oder `;;` in ihren Geschenkideen verwenden.
    - **Fallback:** Alte Geschenkideen werden weiterhin erkannt und beim nächsten Speichern automatisch in das neue Format migriert.
73. **Android 14 Features & Shortcuts:**
    - Implementation of App Shortcuts ("Search", "Add Contact") for faster access from the home screen.
    - Integration of Predictive Back support (`enableOnBackInvokedCallback`) for a modern navigation experience.
74. **Per-App Language Support:**
    - Implementation of `locales_config.xml` to allow users to select the app language independently of the system language (Android 13+).
75. **Architecture Polish & Robustness:**
    - Refactoring of `ContactMapper` to consistently use the robust extension functions from `DateUtils`.
    - Implementation of a state-based automatic search focus logic that works reliably even during screen transitions or cold starts.
76. **Privacy Policy Integration:**
    - **New Sub-page:** Einführung einer dedizierten "Datenschutzerklärung" Seite, die über den "Über die App" Screen erreichbar ist.
    - **Localization:** Vollständige Unterstützung für Deutsch und Englisch inkl. Navigation und TopBar-Integration.
77. **Advanced Onboarding & Stability Overhaul:**
    - **Onboarding Screen:** Einführung eines geführten Multi-Page Onboarding-Screens (Willkommen, Kontakte, Benachrichtigungen) mit `HorizontalPager`. Ersetzt die fehleranfälligen Dialog-Popups.
    - **UX Polish:** Deaktivierung des freien Swipens im Onboarding zur Sicherstellung einer vollständigen Konfiguration. Implementierung von intelligenten "Einstellungen öffnen" Fallbacks und "Ohne Kontakte fortfahren" Optionen.
    - **Branding:** Integration des farbigen App-Logos im Willkommens-Screen zur Stärkung der Markenidentität.
    - **Technical Stability:** Implementierung eines `Mutex` im `NotificationRepository` zur Beseitigung von Race-Conditions bei parallelen Einstellungs-Updates.
    - **Automated Testing:** Einführung einer Test-Infrastruktur mit Unit-Tests (`DateUtils`) und Instrumented Tests (`NotificationRepository`).
    - **CI/CD Integration:** Automatisierte Test-Ausführung in der GitHub Actions Pipeline vor jedem Release-Build.
    - **Cleanup:** Entfernung veralteter Onboarding-Dialoge und Konsolidierung der UI-Logik im Home-Screen.
78. **Resource Consolidation & Testing Excellence:**
    - **String Cleanup:** Zusammenführung aller Onboarding-Strings in die zentrale `strings.xml` und Entfernung redundanter Ressourcendateien zur besseren Wartbarkeit.
    - **Privacy Policy Extraction:** Auslagerung der umfangreichen Datenschutzerklärung in lokalisierte Markdown-Dateien (`res/raw`), um die App-Ressourcen übersichtlich zu halten und die Lesbarkeit zu verbessern.
    - **Testing Stability:** Vereinheitlichung der Coroutines-Bibliotheken (v1.10.1) zur Behebung von Laufzeitfehlern in der Test-Umgebung.
    - **Test Infrastructure:** Migration der ViewModel-Tests auf instrumentierte Tests (`androidTest`) inklusive Integration von `mockito-android`, um eine stabile Validierung der Such- und Filterlogik auf echten Geräten zu gewährleisten.
    - **Policy Update:** Einführung einer verbindlichen Test-Pflicht vor jedem Release-Build in der Projektdokumentation.
79. **Architecture Best Practices (Restructuring):**
    - **Data Layer:** Bündelung von Datenbank-Entitäten und Repositories unter dem gemeinsamen `data`-Package (`data.local`, `data.repository`).
    - **Mapper Layer:** Einführung des `data.mapper` Packages for reine Logik-Komponenten wie den `ContactMapper`.
    - **UI Models:** Verschiebung von reinen UI-Datenstrukturen (`ContactUiModel`, `HomeUiState`, `GiftIdea`) in das neue `ui.model` Package zur strikten Trennung von ViewModels und deren State-Definitionen.
    - **Project Cleanup:** Vollständige Aktualisierung aller Paket-Deklarationen und Importe im gesamten Projekt sowie Synchronisation der Test-Ordnerstruktur.
80. **UI Polish & Performance (Startup & I18n):**
    - **Instant Start:** Integration der Android Splash Screen API (`core-splashscreen`) zur Eliminierung von Start-Flickern; die App hält das System-Logo nun aktiv fest, bis der Onboarding-Status aus der Datenbank geladen wurde.
    - **Expert Date Logic:** Zentralisierung der Geburtsdatums-Logik in den `DateUtils`. Einführung von `isBirthdayToday`, `hasYear` und einem typsicheren `safeNextAge` (nullable), was den Code in Mappern und Repositories deutlich lesbarer macht.
    - **Full I18n Support:** Umstellung der Datumsformatierung im Mapper und Widget auf systemseitige Patterns (`getBestDateTimePattern`), wodurch Tag und Monat nun weltweit in der jeweils korrekten Reihenfolge und Schreibweise angezeigt werden.
    - **Resource Cleanup:** Entfernung veralteter Onboarding-Dialog-Strings und Konsolidierung der Ressourcen.
81. **ViewModel Refactoring & Modularization:**
    - **Architecture:** Aufteilung des monolithischen `BirthdayViewModel` in fünf spezialisierte ViewModels (`HomeViewModel`, `NotificationViewModel`, `SettingsViewModel`, `LabelViewModel`, `BackupViewModel`). Dies verbessert die Wartbarkeit, reduziert die Code-Komplexität pro Datei und ermöglicht eine gezieltere Testbarkeit.
    - **Navigation:** Anpassung des `NavHost` in der `MainActivity` zur Bereitstellung der spezifischen ViewModels für die jeweiligen Screens.
    - **Testing:** Migration und Umbenennung des Such-Logik-Tests zu `HomeViewModelSearchTest` unter Berücksichtigung der neuen Architektur.
82. **UX & UI Spacing Polish:**
    - **Pull-to-Refresh:** Refactoring der `BirthdayList`, sodass die Pull-to-Refresh Geste nun auch bei einer leeren Liste (Empty State) einwandfrei funktioniert.
    - **Layout Consistency:** Standardisierung der vertikalen Abstände im Home-Header. Einführung eines festen 8dp Paddings für die `SearchBar` zur Gewährleistung eines harmonischen Layout-Rhythmus, unabhängig von der Sichtbarkeit der Label-Filterbar.
83. **UI Interaction Overhaul (Expandable Cards):**
    - **UX Evolution:** Ersatz der versteckten Swipe-Geste durch ein intuitives "Tap-to-Expand" System. Die Kontakt-Karten lassen sich nun durch einfaches Antippen aufklappen.
    - **Direct Access:** Die Aktions-Buttons wurden durch kompakte, intuitive Icons ersetzt. Der Button zum Öffnen des Kontakts ist nun als Icon in der Form des Kontaktbildes direkt darunter platziert.
    - **Messenger Integration:** Einführung von Schnellzugriff-Links für Telefonate, SMS und WhatsApp direkt in der aufgeklappten Karte (vorausgesetzt eine Telefonnummer ist hinterlegt).
    - **Gift Idea Visibility:** Die Verwaltung von Geschenkideen erfolgt nun vollständig inline. Der separate "Hinzufügen"-Button wurde entfernt, da Einträge direkt in der Karte erstellt und bearbeitet werden können.
    - **Data Layer Upgrade:** Die Kontakt-Synchronisation erfasst nun auch Telefonnummern vom System, um die Messenger-Integration zu ermöglichen (Datenbank v4).
    - **Performance:** Implementierung flüssiger Animationen (`animateContentSize`) beim Aufklappen und Reduzierung der UI-Komplexität durch Entfernung der Swipe-Logik und des `GiftIdeaDialog`.
    - **Clean UI:** Bereinigung des Home-Screens von Hilfe-Animationen (Swipe-Hint), da die neue Interaktion selbsterklärend ist.
84. **Architecture Refinement (Component Splitting):**
    - **Modularization:** Aufteilung der massiven `BirthdayItem.kt` in kleinere, spezialisierte Komponenten (`GiftIdeaList`, `ContactActionRow`, `ContactImage`, `BirthdayStatus`).
    - **Maintainability:** Deutliche Verbesserung der Lesbarkeit und Wartbarkeit durch Trennung von Layout-Koordination und spezialisierter UI-Logik.
    - **Reusability:** Die neuen Komponenten sind nun eigenständig und können bei Bedarf an anderen Stellen der App leichter wiederverwendet werden.
85. **UX & Performance Polish (Input & Stability):**
    - **Smooth Input:** Einführung eines lokalen States für Geschenkideen-Textfelder zur Eliminierung von Verzögerungen und Cursor-Sprüngen während der Datenbank-Synchronisation.
    - **Focus Management:** Optimierung der Tastatur-Steuerung im Home-Screen; automatisches Schließen der Tastatur beim Scrollen wurde auf die Suchfunktion begrenzt, um die Bearbeitung von Geschenkideen nicht zu unterbrechen.
    - **Dynamic Labels:** Kontextsensitive Beschriftung für das Hinzufügen von Geschenkideen ("Geschenkidee" bei leerer Liste vs. "Listeneintrag").
    - **Automated UI Testing:** Implementierung von instrumentierten Tests (`BirthdayItemInteractionTest`) zur Absicherung der Interaktions-Logik in den Kontakt-Karten.
    - **Code Quality:** Systematische Bereinigung von IDE-Warnungen (ungenutzte Parameter, KTX-Migration zu `toUri()`).
86. **UX Refinement (Expandable Card Polish):**
    - **Visual Consistency:** Korrektur des Highlight-Effekts beim Aufklappen; die gesamte Karte nutzt nun einen einheitlichen Hintergrund zur besseren Abgrenzung.
    - **Smart Interaction:** Implementierung einer "Auto-Collapse" Logik; geöffnete Karten schließen sich nun automatisch beim Scrollen der Liste.
    - **Label Optimization:** Verlagerung der Label-Anzeige in den erweiterten Bereich der Karte, um die Hauptliste für alle Nutzer (auch ohne eigene Labels) kompakt und übersichtlich zu halten.
    - **Icon Reset:** Rückkehr zum klassischen Kontakt-Symbol für die Verknüpfung zur Android-Kontakte-App zur besseren Wiedererkennbarkeit.
87. **Code Quality & Architecture Refinement (HomeScreen):**
    - **Separation of Concerns:** Auslagerung des `HomeState` in eine eigene Datei (`HomeState.kt`) zur Verschlankung der `HomeScreen.kt`.
    - **State Management:** Konsolidierung der Side-Effects durch Verlagerung der Filter-Reset-Logik in das `HomeViewModel`. Das ViewModel steuert nun zentral den `isResettingFilter` Zustand.
    - **Stability:** Einführung von sicherem Parsen für Kontakt-IDs (`toLongOrNull`) und Bereinigung redundanter `LaunchedEffect`-Blöcke.
    - **Component Polish:** Refactoring von `ContactActionRow` mit sicherem Intent-Handling (try-catch) for Anrufe, SMS und WhatsApp sowie Korrektur der Icon-Inkonsistenzen.
    - **Structure:** Aktualisierung der `PROJECT_STRUCTURE.md` zur Abbildung der neuen Dateistruktur.
88. **Context Optimization & Messenger Upgrade:**
    - **Context Hardening:** Archivierung alter Changelog-Einträge (1-59) in `CHANGELOG_ARCHIVE.md` zur Einsparung von Context-Fenster-Platz für LLMs.
    - **Smart Messenger Detection:** Einführung einer automatischen Erkennung von WhatsApp/Signal Verfügbarkeit via MIME-Type Check im `SystemContactDataSource`.
    - **Database Migration (v5):** Upgrade des Schemas zur persistenten Speicherung der Messenger-Verfügbarkeit pro Kontakt.
    - **UI Finalization:** Dynamische Einblendung der originalen Messenger-Icons (WhatsApp/Signal) in der `ContactActionRow` nur bei tatsächlicher Verfügbarkeit.
89. **Code Quality & ViewModel Refinement:**
    - **Logic Consolidation:** Einführung der privaten Hilfsfunktion `updateContactGiftIdeas` im `HomeViewModel`. Dies eliminiert redundanten Code bei der Manipulation von Geschenkideen und stellt eine konsistente Datenaktualisierung sicher.
    - **Type Safety:** Refactoring des `uiState` Flows im `HomeViewModel`. Die Kombination der Datenströme wurde durch explizite Parameterbenennung lesbarer und typsicherer gestaltet.
    - **Linting & Formatting:** Projektweite Bereinigung von IDE-Warnungen. Einführung von Trailing Commas in allen Home-Komponenten, Korrektur von Namenskonventionen (z.B. `ScrollbarDefaults`) und Optimierung von Side-Effects (z.B. `produceState`).
    - **Stability:** Verbesserung des Scroll-Reset-Mechanismus durch explizite Parameterübergabe in Callbacks.
90. **Visual Delight (Confetti Celebration):**
    - **New Component:** Einführung von `ConfettiEffect.kt`, einem leichtgewichtigen Partikelsystem auf Canvas-Basis.
    - **Contextual Animation:** Die App feiert nun Geburtstage visuell beim Aufklappen einer Kontaktkarte. Der Effekt regnet über den Inhalt der Karte, ohne das Layout zu blockieren.
    - **Age-Based Coloring:** Dynamische Farbwahl für das Konfetti passend zur Umrandung: Gold für runde Geburtstage, bunte `KidColors` für Kinder (0-9) und Silber/Weiß für alle anderen.
    - **Performance:** Nutzung von `matchParentSize()` und `LocalDensity` für eine präzise, GPU-beschleunigte Darstellung unabhängig von der Bildschirmauflösung.
91. **UX Polish (Gift Idea Management):**
    - **Smart Collapsing:** Geschenkideen sind in der Kontaktkarte nun standardmäßig eingeklappt. Ein neuer, interaktiver Toggle-Bereich mit passendem Icon ermöglicht das gezielte Auf- und Zuklappen.
    - **Contextual Reset:** Um die Übersichtlichkeit zu wahren, klappt sich die Geschenkideen-Liste automatisch zu, wenn die Kontaktkarte geschlossen wird (`remember(isExpanded)`).
    - **Navigation Intelligence:** Geöffnete Kontaktkarten schließen sich nun automatisch, wenn der Nutzer die Filter-Kategorie (Label) wechselt oder eine Suche startet, um eine konsistente Ergebnisliste zu gewährleisten.
    - **UI Cleanup:** Entfernung der redundanten "Geschenkideen"-Überschrift innerhalb der Liste, da die neue Toggle-Zeile diese Rolle übernimmt.
    - **Auto-Expand:** Beim Hinzufügen einer neuen Geschenkidee klappt der Bereich automatisch auf, um einen nahtlosen Workflow zu ermöglichen.
92. **Android 15 Compatibility & Technical Debt Cleanup:**
    - **Edge-to-Edge Optimization:** Anpassung des Onboarding-Screens an die erzwungene randlose Anzeige unter Android 15. Verwendung von `Scaffold` und `navigationBarsPadding()`, um die Überlagerung interaktiver Elemente durch die System-Navigation zu verhindern.
    - **API Migration:** Entfernung veralteter XML-Attribute (`statusBarColor`, `navigationBarColor`) im App-Theme zur Erfüllung der neuen Play Store Anforderungen für SDK 35/36.
    - **Code Quality Offensive:** Systematische Behebung von Linter-Warnungen im Home- und Onboarding-Bereich (vereinfachte Booleans, explizite Parameternamen bei `mutableStateOf`).
    - **Stability Validation:** Erfolgreiche Validierung der Gesamtstabilität durch Ausführung der Unit-Test-Suite und Durchführung eines vollständigen Projekt-Builds.
93. **Widget Excellence (3x2 & Adaptive Preview):**
    - **Sizing:** Das Widget wird nun standardmäßig als 3x2 Grid (3 Spalten, 2 Zeilen) vorgeschlagen (`targetCellWidth/Height`), um mehr Platz für die Geburtstagsliste zu bieten.
    - **Adaptive Preview:** Einführung eines hochwertigen Vorschaubildes im Widget-Picker. Durch die Bereitstellung separater Ressourcen in `drawable` und `drawable-night` passt sich die Vorschau nun automatisch dem Hell/Dunkel-Modus des Systems an.
    - **I18n:** Lokalisierung der Widget-Beschreibung und Optimierung der Metadaten (`birthday_widget_info.xml`) für moderne Android-Versionen (API 31+).
    - **UX:** Das Widget führt beim Klick nun gezielt zum Home-Screen mit einem automatischen "Scroll-to-Top" Reset.
94. **Preview Data Centralization:**
    - **Architecture:** Einführung von `SampleData.kt` im `ui.model` Package. Dies dient als zentrale Quelle für konsistente Testdaten über alle Previews hinweg.
    - **HomeScreen:** Refactoring der `HomePreview` zur Nutzung der zentralen `SampleData`, was die Datei übersichtlicher macht.
    - **Component Previews:** Aktualisierung von `BirthdayItem`, `BirthdayList` und `GiftIdeaList` mit aussagekräftigen Previews auf Basis der neuen Sample-Daten.
95. **External Actions & Permission Refactoring:**
    - **Architecture:** Einführung von `ContactActions.kt` (`ui.util`) zur Zentralisierung aller externen Intents (Anrufe, SMS, WhatsApp, Signal, Kontakte öffnen).
    - **Stability:** Einführung einer robusten `findActivity()` Extension (`util`), um Activity-bezogene Operationen (wie Permission-Rationales) sicher aus tief verschachtelten Context-Wrappern heraus auszuführen.
    - **Clean UI:** Massive Reduzierung von Boilerplate-Code in `HomeScreen.kt` und `ContactActionRow.kt` durch Delegation der Intent-Logik an den neuen Handler.
    - **Consistency:** Vereinheitlichung der Rufnummern-Formatierung für WhatsApp und Signal an einer zentralen Stelle.
96. **Prop Drilling Reduction:**
    - **Architecture:** Einführung von `HomeActions.kt` zur Bündelung aller Callbacks des Home-Screens.
    - **Refactoring:** Vereinfachung der `HomeContent` Signatur durch Nutzung des neuen Action-Wrappers. Dies verbessert die Lesbarkeit und vereinfacht die Wartung des Screens.
97. **Test Coverage Offensive:**
    - **Unit Tests:** Einführung von `ContactMapperTest` zur Absicherung der Transformations-Logik und Datumsformatierung.
    - **ViewModel Tests:** Erweiterung der Testsuite um `HomeViewModelTest`, inklusive Validierung der Label-Filterung und des Ignorieren-Status.
    - **Stability:** Sicherstellung korrekter Altersberechnungen und "Heute"-Status Logik über verschiedene Test-Szenarien.
98. **Onboarding UX & Architecture Refactoring:**
    - **Edge-to-Edge Fix:** Behebung von UI-Überlagerungen durch die 3-Button Systemnavigation im Onboarding-Screen. Einführung eines scrollbaren Seiten-Wrappers und eines optisch abgesetzten Footers zur Sicherstellung der Erreichbarkeit aller Steuerelemente.
    - **Modularization:** Aufteilung der monolithischen `OnboardingScreen.kt` in spezialisierte Komponenten (`WelcomePage`, `ContactsPage`, `NotificationsPage`, `ReadyPage`, `OnboardingFooter`).
    - **Code Quality:** Einführung von `OnboardingPageWrapper` in `OnboardingCommon.kt` zur Reduzierung von Code-Duplikaten und Gewährleistung eines einheitlichen Layout-Verhaltens.
    - **Documentation:** Aktualisierung von `PROJECT_STRUCTURE.md` zur Abbildung der neuen Onboarding-Komponentenstruktur.
99. **Global Code Quality & Linting Offensive:**
    - **I18n Excellence:** Migration von String-Ressourcen mit variablen Zahlenwerten zu `plurals` (Quantity Strings) zur Behebung von Linter-Warnungen und Verbesserung der Lokalisierung (z.B. Altersangaben, Import-Bestätigungen).
    - **Type Safety & Inference:** Systematische Bereinigung redundanter Typ-Parameter im gesamten Projekt (z.B. in `BirthdayWidget`, `BirthdayItem`) zur Verschlankung des Codes.
    - **Idiomatic Kotlin:** Refactoring von `when`-Ausdrücken zu Ausdrücken mit Subjekt und Inlining von Hilfsvariablen (z.B. in `SystemContactDataSource`).
    - **UI Stability:** Umstellung kritischer UI-States auf explizite `MutableState`-Objekte zur Vermeidung von "Value never read" Fehlinterpretationen durch die statische Code-Analyse.
    - **Formatting:** Projektweite Konsolidierung von Trailing Commas, Zeilenumbrüchen und Klammersetzung gemäß den neuesten Android-Coding-Standards.
    - **Visual Polish:** Optimierung der Konfetti-Animation durch variable Startpositionen für ein natürlicheres Erscheinungsbild.
100. **Code Analysis & Linting Finalization:**
     - **Final Polish:** Systematische Behebung verbleibender Code-Analysis Warnungen in zentralen Komponenten (`HomeViewModel`, `HomeScreen`, `BirthdayItem`, `FastScrollbar`, `DateUtils`, `SystemContactDataSource`).
     - **Clarity:** Einführung von "Clarifying Parentheses" für komplexe logische Ausdrücke und flächendeckende Integration von Trailing Commas zur Verbesserung der Diff-Lesbarkeit.
     - **Refined State:** Optimierung der `mutableStateOf` Initialisierungen durch explizite Parameterbenennung (`value = ...`).
101. **UX: Lottie Integration & UI Polish:**
     - **Onboarding UX:** Integration der `anim_contacts.json` Lottie-Animation in die `ContactsPage`. Dies sorgt für eine ansprechendere Nutzerführung während der Rechteanfrage.
     - **Empty State Delight:** Die Lottie-Animation wird nun auch im `EmptyListState` des Home-Screens angezeigt, wenn der Zugriff auf Kontakte noch nicht gewährt wurde.
     - **Architecture:** Einführung eines zentralen `ui.components` Packages und Verschiebung der `LottieIllustration` für eine bessere Wiederverwendbarkeit im gesamten Projekt.
     - **Code Quality:** Bereinigung ungenutzter Imports und Korrektur kleinerer Formatierungsfehler im Onboarding-Modul.
102. **Adaptive Design & Stability Overhaul:**
     - **App Framework:** Einführung von `AppResponsiveScaffold` und `AdaptiveContentContainer`. Die App unterstützt nun offiziell verschiedene Formfaktoren (Handy, Tablet, Chromebook) durch Nutzung von `WindowSizeClass`.
     - **UX:** Beseitigung von Layout-Sprüngen ("Jumping UI") beim App-Start durch Optimierung der TopBar-Insets und Einsatz von `animateContentSize`.
     - **Large Screen Optimization:** Auf breiten Bildschirmen wird der Inhalt nun zentriert und in der Breite begrenzt (max. 840dp), um eine bessere Lesbarkeit zu gewährleisten.
     - **Onboarding Fix:** Verbesserung der Onboarding-Seiten für kleine Displayhöhen (z.B. Chromebook im Landscape-Modus) durch flexiblere Abstände und optimiertes Scrolling.
     - **Dependency:** Integration von `material3-window-size-class` zur präzisen Erkennung der Bildschirmkonfiguration.
103. **UX Polish & Search Highlighting:**
     - **Visual Feedback:** Einführung eines dynamischen Highlighting-Effekts für die `SearchBar`. Bei Fokus wird nun eine animierte Umrandung gezeichnet und die Hintergrundfarbe sanft angepasst.
104. **Home Architecture & UX Refinement:**
     - **Sub-Component Grouping:** Strukturierung des `home.components` Ordners in spezialisierte Unterverzeichnisse (`list`, `topbar`, `actions`). Dies verbessert die Übersichtlichkeit bei wachsender Komponentenanzahl.
     - **Prop Drilling Elimination:** Konsequente Nutzung des `HomeActions` Wrappers in der gesamten Home-Hierarchie. Dies reduziert die Komplexität der Funktionssignaturen und vereinfacht die Wartbarkeit.
     - **Dynamic Scrollbar:** Die `FastScrollbar` unterstützt nun kontextsensitive Labels (Monat vs. Anfangsbuchstabe), was die Navigation im "Ohne Datum"-Tab signifikant verbessert.
     - **Padding Rule Compliance:** Refactoring der `BirthdayItem` Abstände auf eine nicht-additive Strategie (`bottom = 8.dp`), um Layout-Sprünge bei Animationen zu vermeiden.
     - **Consistency:** Vereinheitlichung der Accessibility-Beschreibungen und Trailing Commas im Home-Screen Bereich.
105. **Database Migration Fix (Stability):**
     - **Robust Migration:** Einführung einer manuellen Migration von Version 1 auf 2. Dies behebt einen kritischen Absturz (`SQLiteException: no such table: pending_notifications`), der bei einigen Nutzern auftrat, deren V1-Datenbank inkonsistent zum erwarteten Schema war.
     - **Safety First:** Die Migration prüft nun aktiv auf die Existenz der Tabelle und der Spalte `dismissCount`, bevor Änderungen vorgenommen werden. Dies stellt sicher, dass sowohl saubere Installationen als auch historisch gewachsene Datenbanken stabil auf V6 aktualisiert werden können.
     - **Quality Assurance:** Einführung von automatisierten Migrationstests (`MigrationTest.kt`), um die Korrektheit der manuellen und automatischen Migrationspfade vor jedem Release sicherzustellen.
106. **Gradle & Dependency Polish:**
     - **Deprecation Fix:** Umstellung der `sourceSets`-Konfiguration im `build.gradle.kts` auf die moderne `assets.directories.add()` API, um Warnungen bei der Schema-Bereitstellung für Tests zu vermeiden.
     - **Dependency Stability:** Korrektur eines Auflösungsfehlers bei der `kotlinx-serialization-json` Bibliothek durch Synchronisation des Version Catalogs.
     - **Code Quality:** Bereinigung redundanter String-Templates in der KSP-Konfiguration.
107. **Adaptive UI & Header UX Fix:**
     - **Trigger Logic:** Refactoring der `isFilterBarVisible` Logik im `HomeState`. Die Filterbar reagiert nun sofort auf den ersten Pixel eines Scroll-Vorgangs (`firstVisibleItemScrollOffset`), was die Navigation deutlich dynamischer macht.
     - **Header Visuals:** Einführung eines soliden Hintergrunds für die gesamte `HomeTopBar`. Dies beseitigt "Geister-Flächen" beim Scrollen, da die Liste nun sauber unter den Header gleitet.
     - **Adaptive Header:** Integration des `AdaptiveContentContainer` in die TopBar. Suche und Filter-Chips sind nun auch auf Tablets und breiten Bildschirmen perfekt mittig über der Liste ausgerichtet.
     - **Animation Cleanup:** Entfernung redundanter `animateContentSize` Aufrufe in der TopBar zugunsten der nativen `AnimatedVisibility` Logik, um Layout-Zittern zu verhindern.
     - **Framework Stability:** Korrektur des `AdaptiveContentContainer` Verhaltens; das erzwungene `fillMaxSize()` wurde entfernt, um eine korrekte Höhenberechnung für Header-Komponenten zu ermöglichen.
108. **Architecture Simplification & Code Cleanup:**
     - **Logic Delegation:** Auslagerung der Manipulations-Logik für Geschenkideen (Sortierung, Status-Wechsel) vom `HomeViewModel` in das `GiftIdea` Modell zur besseren Testbarkeit und Verschlankung der ViewModels.
     - **Filter Refactoring:** Extraktion der komplexen Kontakt-Filterlogik in die spezialisierte Hilfsfunktion `shouldShowContact` im `HomeViewModel`.
     - **Action Consolidation:** Einführung von `requestFilterReset()` zur Vereinheitlichung von Scroll-Reset und Filter-Status Triggern bei Nutzerinteraktionen.
     - **Linting & Stability:** Projektweite Behebung von Code-Analysis Warnungen (z.B. "Assigned value is never read" in der `SearchBar`) und Optimierung von Trailing Commas sowie Lambda-Syntax.
109. **UI & UX Final Polish:**
     - **Adaptive Framework:** Vereinfachung des `AppResponsiveScaffold` zur automatischen Verwaltung interner Paddings, was zu deutlich schlankeren und lesbareren Screen-Implementierungen führt.
     - **Visual Symmetry:** Anpassung der Typografie im `BirthdayStatus` an den `headlineContent` (Name) und `supportingContent` (Datum) zur Schaffung einer harmonischen und balancierten Kartenansicht.
     - **I18n Excellence:** Kleinschreibung des Präfix "in" für die Anzeige verbleibender Tage in Deutsch und Englisch zur Verbesserung des visuellen Rhythmus.
     - **Pull-To-Refresh Stability:** Endgültige Behebung von fälschlichen Refresh-Triggers bei Layout-Änderungen durch konsequente Isolierung der `PullToRefreshBox` vom dynamischen Scaffold-Padding.
     - **Legal UI Polish:** Einführung eines simplen Markdown-Renderers für die Datenschutzerklärung. Die `privacy_policy.md` wurde strukturell überarbeitet (H1/H2 Header, Aufzählungen), um eine ansprechende und lesbare Darstellung der rechtlichen Informationen zu gewährleisten.
110. **ViewModel Refactoring & Clean Architecture Decoupling:**
     - **Onboarding Separation:** Einführung des spezialisierten `OnboardingViewModel.kt`. Das Onboarding ist nun komplett vom `SettingsViewModel` entkoppelt, welches aufgrund von Redundanz vollständig gelöscht wurde.
     - **Clean Architecture Decoupling:** Beseitigung aller direkten Abhängigkeiten von Android `Context`, Glance, WorkManager und Coil aus den ViewModels (`HomeViewModel`, `LabelViewModel`, `BackupViewModel`, `NotificationViewModel`).
     - **Hilt-Injected Helper Interfaces:** Einführung von `WidgetUpdater`, `NotificationScheduler` und `ImagePrefetcher` zur Entkopplung der System-APIs. Dies verbessert die JVM-Unit-Testbarkeit der ViewModels drastisch.
     - **Documentation:** Aktualisierung von `PROJECT_STRUCTURE.md` zur Abbildung der neuen Struktur.
111. **Database Migration Hardening (Update Safety):**
     - **Robust Migration 5->6:** Erhöhung der Ausfallsicherheit bei der Migration von Version 5 auf 6. Die App ermittelt nun zur Laufzeit dynamisch die in `contacts_old` vorhandenen Spalten via `PRAGMA table_info`. Dies fängt Situationen ab, in denen vorherige Migrationsschritte (wie das Hinzufügen von `phoneNumber` in V4 oder Messenger-Flags in V5) auf Nutzersystemen fehlgeschlagen oder übersprungen wurden. Fehlende Spalten erhalten beim Kopieren sichere Standardwerte (`NULL` bzw. `0`).
     - **Wiederherstellungs-Fallback:** Tritt bei der Migration 5->6 dennoch ein kritischer Fehler auf, wird die alte Tabelle `contacts_old` im `catch`-Block automatisch wieder in `contacts` umbenannt. Dadurch wird jeglicher Datenverlust ausgeschlossen und ein stabiler App-Start gewährleistet.
112. **Database Migration Bulletproofing (NOT NULL Constraint Alignments):**
     - **NOT NULL Schema Alignment (V7):** Behebung eines latenten Schema-Fehlers bei der Migration auf V7. Die Spalte `giftIdeas` wurde bei der Tabellenrekonstruktion in `MIGRATION_5_6` fälschlicherweise als nullable (`TEXT`) statt `TEXT NOT NULL` angelegt, was beim anschließenden Validierungsschritt von Room (V7) zu einem Absturz führte.
     - **Shared Table Reconstruction:** Zusammenführung und Modularisierung der Tabellen-Rekonstruktionslogik in die private Hilfsfunktion `recreateContactsTable(db)`. Diese befüllt leere/fehlende JSON-Spalten (`labels` & `giftIdeas`) nun mittels `COALESCE` mit sicheren leeren Arrays (`'[]'`) und erzeugt das exakte V7 `NOT NULL` Schema.
     - **Migration 6->7 Hardening:** Integration der Härtung in `MIGRATION_6_7`. Falls User die fehlerhafte Zwischenversion V6 installiert hatten (wo `giftIdeas` noch nullable war), migriert `MIGRATION_6_7` die Tabelle nun ebenfalls automatisch auf das korrekte V7 `NOT NULL` Constraint.
     - **Test Coverage & Validation:** Behebung von Kompilierungsfehlern in den instrumentierten Repositories-Tests. Erfolgreicher Durchlauf aller 29 automatisierten Migrations- und Systemtests auf dem Android-Emulator.
113. **Responsive Architecture & Lifecycle Hardening:**
     - **Adaptive UI-Architektur:** Einführung von `LocalWindowWidthSizeClass` als `CompositionLocal` zur vereinfachten, abfragefreien Weitergabe der Bildschirmbreitenklasse. Bereinigung des `AppResponsiveScaffold` durch Beseitigung redundanter Überlagerungsebenen (`zIndex(1f)`), Bereitstellung nativer Scaffold-Parameter (FAB-Position, Content-Farbe, Window-Insets) sowie Einführung einer flexiblen `consumePadding` Steuerung.
     - **Akkuschonende Inaktivitätsprüfung:** Ersatz der CPU-intensiven LaunchedEffect-Dauerschleife (`while(true) delay(10000)`) in der `MainActivity` durch einen modernen, lifecycle-basierten `LifecycleEventObserver`. Die Inaktivitätsprüfung (5-Minuten-Limit) erfolgt nun batterieschonend ausschließlich bei der Wiederaufnahme der App in den Vordergrund (`ON_RESUME`).
     - **Robustes Intent-State-Handling:** Behebung von Fehlern bei widget- oder benachrichtigungsbasierten App-Aufrufen im Hintergrund durch Einführung des reaktiven Compose-States `activityIntent`. Intents werden bei `onNewIntent` nun zuverlässig in den State-Tree propagiert und triggern die Navigation.
     - **Hilt-Dependency-Optimierung:** Performanz-Steigerung der Dependency Injection durch Annotation aller Room DAOs (`AppModule.kt`) mit `@Reusable`. Dies ermöglicht eine effiziente Wiederverwendung der Instanzen ohne die Synchronisations- und Speicher-Overheads von `@Singleton`.
     - **Moderne API-Migration:** Ablösung veralteter Lifecycle-Schnittstellen durch Migration auf das moderne `androidx.lifecycle.compose.LocalLifecycleOwner` in der Compose-Schicht.
114. **Notification Settings, UX Polish & Animation Optimization:**
     - **Edit Notification Rule UI Redesign:** Vollständiges Redesign des `EditRuleDialog`. Der veraltete Slider wurde durch ein präzises textbasiertes Eingabefeld (`OutlinedTextField`) für Vorwarnzeiten in Kombination mit einer Einheiten-Auswahl (Tage vs. Wochen via RadioButtons) ersetzt. Integration des System-TimePickers und Hinzufügen von Plural-String-Ressourcen für fehlerfreie Lokalisierung.
     - **Auto-Sync & Preloading Architecture:** Verlagerung des manuellen, rechteabhängigen Kontaktsynchronisations-Triggers aus dem `HomeScreen` direkt in den `init`-Block des `HomeViewModel` zur Vermeidung von Race-Conditions. Verschiebung des Bild-Preloadings aus UI-seitigen `LaunchedEffects` in das ViewModel via `ImagePrefetcher`-Hilfsklasse.
     - **Smooth List Animations:** Einführung eines intelligenten `skipPlacementAnimation`-Status in `BirthdayList` zur vorübergehenden Deaktivierung von Platzierungsanimationen beim Wechsel von Suchanfragen oder Filter-Labels. Optimierung der Ein- und Ausblendanimationen (`fadeInSpec`, `fadeOutSpec`, spring-basierte `placementSpec` in `Modifier.animateItem()`) für flüssige Übergänge.
     - **Onboarding UX Optimization:** Deaktivierung automatischer Seitenwechsel beim Erteilen von Berechtigungen im Onboarding-Screen zur Vermeidung unruhiger UX. Verbesserung des `OnboardingFooter` durch `fillMaxWidth` zur sauberen responsiven Darstellung auf breiten Bildschirmen.
     - **I18n Plurals:** Einführung von Quantity Strings (`dialog_rule_unit_days`, `dialog_rule_unit_weeks`) in Deutsch und Englisch zur korrekten Pluralisierung bei numerischen Angaben im Erinnerungsdialog.
     - **Recomposition Optimization:** Nutzung von `rememberUpdatedState` for `onNavigateToSettings` im `HomeScreen` zur Reduzierung unnötiger Re-Instanziierungen des `HomeActions`-Wrappers.
     - **Version Bump:** Anhebung der App-Version auf Version Name `2.3.18` (Version Code `27`) in `build.gradle.kts`.
115. **Onboarding UX & Database Resiliency:**
     - **Onboarding Streamlining:** Vereinfachung der `ContactsPage` durch Ersetzung der komplexen `LottieIllustration` durch ein Standard-Material-`Icon` für Kontakte, um die UI-Ladezeiten und Rendergeschwindigkeit zu optimieren.
     - **Database Hardening:** Absicherung der Hilt-Datenbank-Erstellung in `AppDatabase.kt` durch ein try-catch-Konstrukt. Bei schwerwiegender Schema-Korruption löscht die App nun die fehlerhafte Datenbankdatei automatisch via `deleteDatabase` und baut eine saubere neue Instanz auf, was App-Startabstürze auf Altgeräten verhindert.
     - **Resource Reorganization:** Ausgliederung von Drittanbieter-Messenger-Drawables (`ic_discord.xml`, `ic_whatsapp.xml` etc.) in einen eigenen Source-Set-Ordner (`src/main/res-messenger/drawable`), um das Hauptressourcen-Verzeichnis sauber zu halten.
116. **Version Upgrade & Warning Resolution:**
     - **Release Version Code:** Anhebung der App-Version auf Version Name `2.4.0` (Version Code `28`) in `build.gradle.kts`.
     - **Code Cleanup & Linting:** Behebung diverser Compiler- und Linter-Warnungen im gesamten Projekt:
    - Beseitigung von `LocalContext.current` Ressourcen-Abfragewarnungen in `ContactActionRow` durch direkte Sortierung der Messenger nach Enum-Namen.
    - Ausrichtung von `AppResponsiveScaffold` an Compose-Designrichtlinien durch Positionierung des `modifier` Parameters als ersten optionalen Parameter.
    - Ergänzung von `@Suppress("unused")` Annotationen in Hilt-DI-Modulen und Bereinigung toten Codes in `MessengerApp`.
    - Verschiebung von `widget_preview.webp` in `drawable-nodpi/` zur Vermeidung von Density-Warnungen.
117. **Dynamic Glance Widget Styling (Widget Optics Redesign):**
     - **Visual Row Design:** Redesign der `BirthdayRow` im Jetpack Glance Homescreen-Widget passend zum kartenbasierten Design der App. Einführung farbiger, abgerundeter Zeilen-Hintergründe (`12.dp` Ecken) basierend auf dem Geburtstags-Status (Gold für runde Geburtstage, Green für Kinder 0-9, Silber für sonstige).
     - **Dynamic Spacing & Heights:** Implementierung einer dynamischen Höhenberechnung (`dynamicBlockHeight`), um die verfügbare Gesamthöhe des Widgets gleichmäßig auf alle gerenderten Geburtstage aufzuteilen. Hinzufügen von Margins zwischen den Zeilen für ein saubereres, luftigeres Layout.
118. **Launcher Icon Restoration & Scale Optimization:**
     - **Directory Restoration:** Rücknahme der fehlerhaften Verschiebung der XML-Dateien von `mipmap-anydpi` zurück nach `mipmap-anydpi-v26/` (`ic_launcher.xml` und `ic_launcher_round.xml`). Dies behebt das Problem, dass Launcher auf API 26+ fälschlicherweise die WebP-Templates mit grünem Hintergrund statt der XML-Definitionsdatei luden.
     - **Scale Optimization:** Korrektur des Skalierungsfaktors der Logo-Gruppe in `ic_launcher_foreground.xml` zurück auf `0.5` und Zentrierungsversatz auf `6`. Das Geschenkbox-Logo sitzt nun wieder perfekt zentriert in kreisförmigen und abgerundeten Schablonen, ohne abgeschnitten oder übergroß zu wirken.
119. **Android Contact Birthday Update & Test Coverage Overhaul:**
     - **Schreibschutz-Behebung (RawContacts):** Behebung des Fehlers beim Schreiben von Geburtstagen direkt in Android-Kontakte durch intelligentes Ausfiltern von schreibgeschützten Dritthersteller-Konten (z. B. WhatsApp, Signal) und gezielte Priorisierung beschreibbarer Google- oder lokaler Rohkontakte.
     - **Silent Runtime Permission:** Einführung einer lautlosen Berechtigungsprüfung für `WRITE_CONTACTS` zur Laufzeit. Falls die Leseberechtigung (`READ_CONTACTS`) bereits vorliegt, holt die App die Schreibberechtigung im Hintergrund unsichtbar ein, um Abstürze zu verhindern und sofortiges Bearbeiten ohne App-Neustart zu ermöglichen.
     - **Fehler-Protokollierung:** Aktives Logging von Fehlern beim Ändern von Kontaktdaten im `SystemContactDataSource` via `Log.e` zur besseren Diagnose statt leisem Verschlucken von Ausnahmen.
     - **Automated Instrumented Testing:**
    - **SystemContactDataSourceTest:** Neuer instrumentierter Test zur präzisen Validierung von Kontakt-Updates und -Inserts sowie der korrekten Selektion beschreibbarer Konten.
    - **NotificationHelperTest:** Neuer instrumentierter Test zur Überprüfung der System-Benachrichtigungen inklusive Polling gegen OS-Latenz und JUnit-`GrantPermissionRule` for Benachrichtigungen ab Android 13.
    - **Test-Dependencies:** Integration der `androidx.test:rules:1.6.1` Abhängigkeit in die `build.gradle.kts` zur Beseitigung von Referenz- und Kompilierungsfehlern in den instrumentierten Testläufen.
120. **Unified Onboarding Architecture & Layout Polish:**
     - **Onboarding-Seiten-Template:** Einführung der standardisierten `OnboardingPageTemplate` Komponente zur einheitlichen Strukturierung aller Onboarding-Seiten (Willkommen, Kontakte, Benachrichtigungen, Kalender, Abschluss-Seite).
     - **Perfekte visuelle Symmetrie:** Ausrichtung aller Hauptelemente (Icon, Titel, Beschreibungstext, Einstellungs-Cards und Buttons) auf exakt denselben horizontalen Linien über den gesamten Onboarding-Prozess hinweg.
     - **Absolute Layout-Stabilität (Sprungfrei):** Beseitigung aller vertikalen Layout-Sprünge (Jumping UI). Durch Umstellung auf Top-Ausrichtung (`Arrangement.Top`) und Einbettung der Aktionsbuttons in eine Box mit fester Höhe (`56.dp`) bleibt der obere Bereich (Icon, Titel, Beschreibung) absolut stabil zentriert, selbst wenn Einstellungsoptionen verändert oder Buttons ein-/ausgeblendet werden.
     - **Konsistente Benutzerführung (Kontakte-Switch):** Ersatz der komplexen Dropdown-Lösung auf der Kontakte-Seite durch eine edle Einstellungs-Card mit einem Switch ("Kontakte synchronisieren"). Dadurch verhält sich die Kontakte-Berechtigung exakt genauso wie die Benachrichtigungen und der Kalender: Das Überspringen deaktiviert den Switch und erfordert wie gewünscht den manuellen Klick auf "Weiter" im Footer.
     - **Visueller Rhythmus:** Anhebung des unteren Abstands zum Footer im Compact-Layout von `8.dp` auf `24.dp` für ein luftigeres und moderneres Gesamtbild.
121. **UX & UI Consistency Refinement:**
     - **Sync Feedback:** Einführung einer Snackbar-Bestätigung im `SyncSettingsScreen`, sobald die manuelle Kontaktsynchronisierung erfolgreich abgeschlossen wurde.
     - **Architecture:** Erweiterung des `HomeViewModel` um ein `syncCompletedEvent` (SharedFlow), um transiente UI-Ereignisse sauber zu entkoppeln.
     - **I18n:** Hinzufügen des neuen `sync_success` Strings in Deutsch und Englisch.
     - **BirthdayItem Layout:** Anpassung des `BirthdayStatus` in der App an die Informationshierarchie des Widgets. Das kommende Alter steht nun oben (fett/primär), während die verbleibenden Tage bzw. der Status "Heute!" darunter angezeigt werden.
122. **UI & Code Polish (FastScrollbar Refactoring & Visual Fix):**
     - **Saubere Komponenten-Strukturierung:** Internes Refactoring von `FastScrollbar.kt` zur Steigerung der Übersichtlichkeit, Lesbarkeit und langfristigen Wartbarkeit.
     - **Sub-Composables:** Erfolgreiche Auslagerung der Month/Letter-Label-Anzeige in ein privates `ScrollbarBubble` Composable sowie des kompletten Zeichen- und Animationsbereichs von Track und Thumb in ein privates `ScrollbarTrackAndThumb` Composable.
     - **Erhalt der Kapselung:** Beibehaltung aller Funktionalitäten und Zustände in einer einzigen Datei, um den globalen Namensraum schlank zu halten und das hochspezialisierte Gesten-Handling an einem Ort gebündelt zu wissen.
     - **Overlay-Schnittstelle (Popup-Bubble):** Umstellung des `ScrollbarBubble` Renderings auf die Jetpack Compose `Popup`-API. Dies verhindert Clipping an den Containergrenzen und stellt sicher, dass die Bubble beim Scrollen ganz oben vor (statt hinter) der `HomeTopBar` gezeichnet wird. Die sanften Ein- und Ausblendungsanimationen bleiben über einen `MutableTransitionState` vollständig erhalten.
     - **Gesten-Stabilität (Jump-Fix):** Deaktivierung der automatischen Fenster-Positionskorrektur (`clippingEnabled = false` in `PopupProperties`). Dies verhindert, dass der Android-Fenstermanager die Bubble bei Annäherung an den oberen Bildschirmrand (durch den negativen Offset zum Ausrichten am Daumen) verzögert korrigiert und nach oben verschiebt ("hüpfen" lässt). Die Position bleibt dadurch absolut stabil und folgt präzise dem Finger.
123. **Performance Optimization & UI Modularization:**
     - **Background Filtering Performance:** Added `.flowOn(Dispatchers.Default)` to the combined `filteredContacts` flow in `HomeViewModel.kt`. This ensures that heavy list processing, especially on larger contact databases (>1000 contacts), runs asynchronously off the Main thread, preserving 60fps scrolling and preventing UI frame drops during active searching or filtering.
     - **DatePicker Modularization:** Extracted the inline `DatePickerDialog` logic from the large `BirthdayItem.kt` file into a dedicated, reusable, and testable `@Composable` file `BirthdayDatePickerDialog.kt` in the home list components directory. This improves the separation of concerns and reduces the code footprint of the contact card component.
     - **Compiler Warnings Resolution:** Addressed Kotlin/Hilt compiler warnings in `SystemContactDataSource.kt`, `CalendarSyncRepository.kt`, and `ContactRepository.kt` by replacing `@ApplicationContext` with the targeted `@param:ApplicationContext` annotation on constructor context properties.
     - **Documentation:** Recorded the design constraint regarding legacy database migrations in `docs/PROJECT_STATUS.md` and registered the new file in `docs/PROJECT_STRUCTURE.md`.
124. **Root Directory Cleanup & Workspace Organization:**
     - **Script Organization:** Moved all test and inspection scripts (`scratch_*.py`) from the root directory to a newly created `scripts/` directory to keep the root directory neat and tidy.
     - **Visual Assets:** Moved all visual validation assets (`screen.png` and `screen_notifications.png`) to the `docs/` directory to prevent cluttering the project root.
     - **Git Ignore Security:** Added explicit rules to `.gitignore` (`/birthday_database*` and `/settings_database*`) to prevent local SQLite testing and cache databases from being accidentally tracked or committed to Git.
     - **Documentation:** Updated `docs/PROJECT_STRUCTURE.md` to document the new `scripts/` directory structure.
125. **Calendar Sync Integration, Dynamic Onboarding & Setup Guide:**
     - **Dynamic Legacy Account Cleanup:** Refactored `CalendarSyncRepository.kt` to dynamically read the account name of any existing calendar. If it does not belong to the highly-compatible local `"phone"` account (such as legacy `"BirthdayBuddy"` or package name accounts), the app automatically purges and recreates it. This makes the calendar instantly visible in the side drawer of Google Calendar and standard system calendar apps.
     - **Step-by-Step Setup Guide (`SetupStepsCard`):** Replaced the simple calendar permission warning card in settings with a highly interactive, responsive step-by-step stepper guide. It dynamically checks off steps with satisfying green ticks in real-time as permissions are granted and sync is activated.
     - **Onboarding Tutorial Page (`CalendarGuidePage`):** Implemented a new onboarding tutorial page that visually details how to display the synced "BirthdayBuddy" calendar in external apps (opening the hamburger menu and checking the box).
     - **Dynamic Onboarding Pages scaling:** Updated `OnboardingScreen.kt` to dynamically scale the onboarding pager count from 5 to 6 pages ONLY when calendar sync is enabled and permission is granted, seamlessly bypassing the tutorial if sync is disabled.
     - **Calendar App Link Intent:** Integrated a robust deep-link button (`openDefaultCalendarApp`) in both the onboarding tutorial and settings pages to launch the user's preferred standard Calendar application using a two-stage Intent routing (direct Calendar URI fallback to `Intent.CATEGORY_APP_CALENDAR`), ensuring compatibility across 100% of Android devices.
     - **Clean Compile & I18n:** Added full English and German translations for all guide steps, rearranged state variable declarations to resolve reference ordering bugs, and resolved the `Assigned value is never read` warning, ensuring a 100% clean Gradle compilation.
126. **Clean Architecture & MVVM Refactoring:**
     - **Image Prefetching:** Relocated the image prefetching logic from `HomeViewModel` into a Compose `LaunchedEffect(uiState.contacts)` in `HomeScreen`, decoupling the ViewModel from Coil (image loading library). Fully deleted the redundant `ImagePrefetcher` interface and implementation.
     - **Reactive Widget Updates:** Centralized all Glance AppWidget updates inside the `ContactRepository` data modification methods (`syncContacts`, `updateGiftIdeas`, `updateLabelConfig`), making widget updates fully reactive. Removed `WidgetUpdater` constructor dependency, imports, and manual update calls from `HomeViewModel`, `BackupViewModel`, and `LabelViewModel`.
     - **Gift Ideas Logic Migration:** Shifted the core business logic of adding, toggling, deleting, and updating gift ideas out of `HomeViewModel` and into `ContactRepository` to establish a clean separation of concerns and a single source of truth.
     - **Test Coverage & Quality:** Cleaned up Hilt instrumented and unit tests to align with the new ViewModel constructors. Verified a 100% successful Gradle build and test completion.
127. **Premium Wheel Date Picker & Scrollbar Overlap UX Overhaul:**
     - **Premium Wheel Date Picker:** Ersetzung des standardmäßigen Material 3 DatePickers durch einen extrem hochwertigen, haptisch optimierten Wheel-DatePicker in Walzenoptik in `BirthdayDatePickerDialog.kt`. Er nutzt Compose Snapping (`rememberSnapFlingBehavior`) und mechanische haptische Ticks (`HapticFeedbackType.LongPress`) beim Einrasten für ein absolut physisches Bediengefühl.
     - **Dynamic Year Toggle:** Integration eines „Inklusive Jahr“ Switches (voll lokalisiert in DE & EN). Beim Deaktivieren blendet sich die Jahres-Walzenspalte aus und Tag sowie Monat zentrieren sich harmonisch.
     - **Leap-Year-Safe Clamping:** Automatische live-Berechnung der Tagesanzahl pro Monat. Der Februar erlaubt den 29. Tag nur dann, wenn ein Schaltjahr aktiv und das Jahr eingeschaltet ist. Ansonsten wird die Auswahl zur Vermeidung ungültiger `LocalDate`-Ausnahmen auf 28 Tage begrenzt.
     - **Smart Date Pre-Selection:** Ergänzung von `birthday: LocalDate?` im `ContactUiModel` zur intelligenten Vorauswahl des bereits hinterlegten Geburtstages beim Öffnen.
     - **Scrollbar Overlap Spacing Fix:** Lösung des Touch-Konflikts an der rechten Bildschirmkante in `BirthdayStatus.kt`. Das Plus-Symbol wird nun durch einen standardmäßigen M3 `IconButton` gerahmt (48.dp Touchtarget) und um `12.dp` nach links verschoben, um es aus der aktiven Scrollbar-Gesten-Zone zu holen. Normale Datumsanzeigen erhalten ein konsistentes `8.dp` Padding zur eleganten optischen Trennung.
128. **FastScrollbar Smooth Bubble & Static Popup Overhaul:**
     - **Lag-free Scrolling:** Migration of the `ScrollbarBubble` from dynamic `Popup` window repositioning to a hybrid static `Popup` approach. The `Popup` window itself now remains 100% static (`IntOffset.Zero`), while the bubble content is translated smoothly using GPU-accelerated `.graphicsLayer { translationY = ... }`.
     - **Z-Index & Overlay Safety:** Preserved the use of the `Popup`-API, ensuring the bubble is drawn in a separate window layer on top of all other elements (including `HomeTopBar`) without clipping.
     - **Boundary Safety:** Implemented a translation clamp (`.coerceAtLeast(0f)`) to gracefully stop the bubble at the very top of the visible screen without going behind the top bar or off-screen.
129. **Integration of "Weitere Ereignisse" (Anniversaries & Name Days):**
     - **Database Schema Upgrades:** Bumped `SettingsDatabase` to version 3 (with `MIGRATION_2_3` to add `otherEventsEnabled`) and `AppDatabase` to version 8 (with `MIGRATION_7_8` to add `anniversary` and `nameDay` columns).
     - **System Contacts Integration:** Expanded `SystemContactDataSource.kt` to extract anniversaries and name days from the system Contacts provider using MIME-type custom label matches (e.g., anniversary/hochzeitstag, name day/namenstag).
     - **Logic Mapping & UI Filters:** Refactored `ContactMapper.kt` to calculate days remaining and count (anniversary year) dynamically. Added `LABEL_ANNIVERSARY` and `LABEL_NAME_DAY` pseudo-labels on the Home screen to filter contacts by event, sorted by the remaining days to that event.
     - **Settings Screen Integration:** Implemented `OtherEventsSettingsScreen.kt` with toggle switches, routing, and immediate cache synchronization upon activation.
     - **Notification Scheduling:** Updated background scheduling in `NotificationWorker.kt` and `SnoozeWorker.kt` using key prefixes to isolate anniversaries/name days and prevent collision with standard birthday keys in database logs.
     - **Calendar Synchronization:** Updated `CalendarSyncRepository.kt` to support syncing wedding anniversaries and name days into the local Android system calendar ("BirthdayBuddy") alongside birthdays when "Weitere Ereignisse" is enabled, complete with English and German translations for event titles and descriptions.
     - **Test Coverage:** Added `SettingsMigrationTest.kt` validating v2 to v3 schema changes, updated `ContactMapperTest.kt` with custom event cases, added ViewModel filtering verification, and resolved flow deadlock issues in mock tests, achieving 100% test completion on Android emulators.
130. **Joint Wedding Anniversary Contact Coupling (Ehepaar-Verknüpfung):**
     - **Logic & Merging:** Implementation of a pairing mechanism for spouses sharing the same wedding anniversary date. Unlinked contacts with the same anniversary date are merged into a single `ContactUiModel` inside the "Hochzeitstag" filter, showing joint names (using smart merging in `DateUtils.kt`, e.g. "Max & Erika Mustermann") and double/overlapping contact avatars in `ContactImage.kt`.
     - **Database Schemas:** Added `spouseLookupKey` to `contacts` (AppDatabase v9, `MIGRATION_8_9`) and to `contact_user_data` (SettingsDatabase v4, `MIGRATION_3_4`) to track pairings bidirectionally. Added `ignoredCouplePairs: List<String>` serialized field in `AppSettings` to keep track of declined suggestions.
     - **Notification & Calendar Integration:** Grouped coupled reminders into a single push notification with custom title ("Morgen ist der 5. Hochzeitstag von Max & Erika Mustermann!") and single description, and wrote a single joint event in the Android system calendar.
     - **UI & Interaction:** Added a `CoupleSuggestionBanner` at the top of the anniversary page proposing to link detected spouses. Added an "Entkoppeln" (Unlink) button inside the action row of merged cards.
131. **Reset Ignored Couple Suggestions via Pull-to-Refresh:**
     - **Manual Suggestion Reset:** Updated manual pull-to-refresh on the Home Screen (`syncContacts(showLoading = true)`) to trigger `contactRepository.clearIgnoredCouplePairs()`, which clears the list of ignored couple suggestion pairs.
     - **Re-Scan Trigger:** This allows users to re-scan and get pairing suggestions again for couples they previously chose to ignore (clicked "Nein" on).
     - **Robust Testing:** Updated database migration tests, unit tests, and instrumented Android tests to cover the new pairing and suggestion reset behaviors, resolving coroutine race conditions.
132. **Visual & Resource Polish (Google Meet Icon Overhaul):**
     - **Optimized SVG/Vector Asset:** Updated `ic_google_meet.xml` to use the optimized vector pathing and a clean yellow-orange gradient matching the updated Google Meet video camera branding.
     - **Color Adjustment:** Shifted Google Meet's branding color definition in `MessengerApp.kt` from teal (`0xFF00897B`) to yellow (`0xFFFFEB3B`) to align with the new vector resource.
     - **Linter & Warning Cleanups:** Addressed minor compiler and linter warnings in `HomeScreen.kt`, `HomeState.kt`, `HomeFAB.kt`, `BirthdayDatePickerDialog.kt`, `BirthdayList.kt`, and `HomeViewModel.kt` (including using explicit `.milliseconds` durations in `delay`).
133. **Bugfix: Calendar Sync Event Duplication Overhaul:**
     - **Physical Event Deletion:** Changed the delete query in `CalendarSyncRepository.kt` to append `CALLER_IS_SYNCADAPTER = true` with `"phone"` as account details when purging old calendar events. This ensures that old events are permanently deleted instead of leaving tombstone entries (`deleted = 1`), preventing events from piling up and duplicating.
     - **Duplicate Calendar Clean-Up:** Refactored `findExistingCalendarId()` to `cleanDuplicateCalendarsAndGetId()`. The repository now scans all local calendars on start and sync, keeping only the first valid `BirthdayBuddyCalendar` (under the `"phone"` account/LOCAL type) and permanently deleting any other duplicate or legacy calendars.
     - **Hardened Delete Calendar:** Updated the `deleteCalendar()` method to sweep through the entire system and delete *all* matching `BirthdayBuddyCalendar` databases if the sync is disabled, preventing orphaned calendars when reinstalling or clearing app storage.
134. **Multi-Calendar Isolation under Custom Account ("BirthdayBuddy"):**
     - **Separate Calendars by Event Type:** Split the single calendar sync into three distinct calendars:
    - **Birthdays (Geburtstage):** Pink color (`#E91E63`)
    - **Anniversaries (Hochzeitstage):** Purple color (`#9C27B0`)
    - **Name Days (Namenstage):** Orange color (`#FF9800`)
      - **Custom Account Labeling:** Changed the local account name from `"phone"` to `"BirthdayBuddy"` (under account type `"LOCAL"`). This re-labels the calendar group in external calendar apps (e.g. Google Calendar side navigation) from "LOCAL" or "phone" to **"BirthdayBuddy"** and allows users to check/uncheck and color-code the three categories independently.
      - **Dynamic ID Lookup:** Deprecated the local database persistence of `calendarId` in `AppSettings` in favor of dynamic name-based lookup of the active calendars. This eliminates potential database-out-of-sync bugs when clearing app storage or updating the app.
      - **Garbage & Orphan Cleanups:** Integrated automatic cleanup inside `cleanCalendars()` to delete old legacy calendars (`\"BirthdayBuddyCalendar\"` on the `\"phone\"` account) and any duplicate calendars, leaving exactly one clean calendar per category.
      - **I18n:** Added localized display names for the three calendar titles in both German and English (`strings.xml`).
135. **In-App Calendar Color Picker Settings:**
     - **In-App Color Selector:** Integrated dynamic calendar color configuration rows directly into the `CalendarSettingsScreen.kt` UI when calendar synchronization is active.
     - **Harmonized Color Palette:** Implemented a premium Material 3 `ColorPickerDialog` containing a grid of 8 harmonized colors (Pink, Violet, Blue, Cyan, Green, Amber, Orange, Brown) with visual selection feedback, allowing custom color selections to bypass Google Calendar's missing native option for custom sync adapters.
     - **Settings Database V5 Migration:** Upgraded `SettingsDatabase` to version 5, adding `birthdayCalendarColor`, `anniversaryCalendarColor`, and `nameDayCalendarColor` columns with native default colors. Wrote `SettingsMigrationTest.kt` unit test cases to verify database structure and default value integrity.
     - **I18n Localization:** Fully localized dialog headers, cancel actions, and setting section descriptions in both English and German.
136. **Master-Detail Tablet Layout & Code Cleanups:**
     - **Responsive Master-Detail Split Screen:** Refactored `HomeScreen.kt` and `BirthdayList.kt` to support a premium Master-Detail layout on tablets and desktops (`windowWidthSizeClass != WindowWidthSizeClass.Compact`).
     - **Selection Highlighting:** Added selection tracking and primary container highlighting to contact item cards (`BirthdayItem.kt`) when rendered inside the Master list.
     - **Detail Panel (BirthdayDetailPane.kt):** Created a dedicated, scrollable details panel on the right pane displaying large contact avatars, name, status, contact actions (Call, SMS, WhatsApp), and the interactive Gift Ideas list.
     - **Proportional Avatar Sizing:** Enhanced `ContactImage.kt` to accept a custom `size: Dp` parameter, allowing avatars to scale proportionally (including nested avatar overlays for couples) in the details panel.
     - **Automatic Selection:** Implemented automatic selection of the first contact in the list on tablets to ensure the details pane is initialized with data on startup, while retaining full inline expandable behavior on phone form factors.
     - **Verification:** Verified compilation and executed gradle unit tests successfully.

137. **Full-Width TopBar & Settings Master-Detail Layout:**
    - **Full-Width Home TopBar:** Removed the zentrierende `AdaptiveContentContainer` from `HomeTopBar.kt`, allowing the search bar and label filter chips to span the entire screen width on tablets for a cleaner, modern look.
    - **Settings Master-Detail Layout:** Implemented a native Android Settings-like split-pane layout for tablets on the `SettingsScreen`. The left pane displays the categories menu (with selected tab highlighting), and the right pane displays the active settings sub-page.
    - **Nested Detail Pane Navigation:** Handled sub-screen transitions (like opening the Privacy Policy inside the About screen) directly within the right detail pane.
    - **Dynamic Back Navigation:** Added a `showBackButton` parameter to all settings sub-screens (Notifications, Calendar, Labels, Backup, Sync, Other Events, About, Privacy Policy) to conditionally hide the top bar back button when they are embedded in the split layout.
    - **Scaffold Width Flex:** Added a `useAdaptiveWidth` flag to `AppResponsiveScaffold` to allow disabling the zentrierende max-width constraint for full-bleed screens.
138. **Google Standard M3 Adaptive ListDetailPaneScaffold Integration:**
    - **Adaptive Layout Upgrade:** Migrated the manual tablet/desktop `Row`-based Master-Detail split layout in `HomeScreen.kt` to the official Google standard `ListDetailPaneScaffold` from the `androidx.compose.material3.adaptive` library.
    - **Smooth Transitions:** Wrapped pane contents in `AnimatedPane` to enable standard layout transitions and posture-aware scaling.
    - **Hybrid Smartphone/Tablet UX:** Retained the inline tap-to-expand list layout on smartphones (`Compact` size class) to preserve existing UX design rules, while dynamically loading the dual-pane scaffold on larger screens (Tablet/Desktop).
    - **Dependencies:** Added `androidx-compose-material3-adaptive`, `androidx-compose-material3-adaptive-layout`, and `androidx-compose-material3-adaptive-navigation` to `libs.versions.toml` and `app/build.gradle.kts`.
    - **Compiler Warnings Cleanups:** Upgraded Compose plugin compiler version to `2.4.0` in `libs.versions.toml`. Cleaned up unused `AboutSection` composable in `AboutScreen.kt` and removed unused context/versionName variables from `SettingsFooter` inside `SettingsScreen.kt` to ensure warning-free compilation.
139. **Codebase, Performance, Lifecycle & Resources Optimization:**
    - **FastScrollbar Performance:** Migrated `thumbOffset` to lambda-based state reading (`() -> Dp`) inside [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt), avoiding parent recompositions during list scrolling and dragging.
    - **Lifecycle Safety:** Replaced `collectAsState()` with `collectAsStateWithLifecycle()` in [CalendarSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/calendar/CalendarSettingsScreen.kt) and [OtherEventsSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/otherevents/OtherEventsSettingsScreen.kt) to pause flow collection when the app runs in the background.
    - **Adaptive Scaffold UX:** Integrated `BackHandler` and explicit `navigator.navigateTo` calls for the `ListDetailPaneScaffold` in [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) to ensure correct pane transitions and system back button behavior.
    - **Permission State UX:** Added a `LaunchedEffect(contacts)` block in [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) to re-evaluate contacts permissions dynamically when data changes, fixing stuck permission prompts in EmptyListState.
    - **Resource Clean Up:** Removed unused colors from [colors.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/colors.xml) and obsolete string resources from both English [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) and German [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) to clean up compiler warnings and decrease package footprint.
    - **Legacy delay Warning:** Converted the legacy `delay(Long)` call to the modern `Duration` overload using `.milliseconds` in [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt) to resolve compiler warnings.
140. **Notification Localization (Plurals) & Code Cleanup:**
    - **Notification Plurals:** Converted 11 static anniversary and nameday notification string resources to `<plurals>` in both [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) and German [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) (e.g., `notif_title_days_anniversary_age`, `notif_title_days_anniversary`, `notif_title_days_nameday`, and their plural counter-parts).
    - **NotificationHelper Integration:** Refactored [NotificationHelper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/notifications/components/NotificationHelper.kt) to load the new plural resources using `context.resources.getQuantityString(...)` with correct quantity bindings, completely resolving the 11 `PluralsCandidate` compiler warnings.
    - **Gradle/Kotlin Lint Warnings Clean Up:** Suppressed the `NewerVersionAvailable` and `GradleDependency` lint warnings in [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) to allow keeping Kotlin at the stable `2.3.21` version, as raising it to `2.4.0` would cause compatibility issues with the latest Dagger Hilt compiler version.
    - **Encapsulation & Code Auditing:** Made internal-only repository methods private (`getOrCreateCalendar` and `cleanCalendars` in [CalendarSyncRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepository.kt), and `updateGiftIdeas` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt)).
    - **DRY Refactoring:** Extracted duplicated widget updating and calendar syncing logic in `linkAsCouple` and `unlinkCouple` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt) into a single private helper function `updateWidgetAndSyncCalendar()`.
    - **State Management:** Added `consumeNewlyAddedIdeaId()` to [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/HomeViewModel.kt) to handle resetting newly added gift ideas.
141. **Zuverlässigkeit & Synchronisation (Reliability & Sync):**
    - **Hintergrund-Synchronisation:** Integration von `contactRepository.syncContacts()` in `NotificationWorker.doWork()`, damit der Room-Cache vor jeder Benachrichtigungsprüfung im Hintergrund aktualisiert wird.
    - **Akku-Unabhängigkeit:** Entfernung der `setRequiresBatteryNotLow(true)` Einschränkung für den `NotificationWorker`, um pünktliche Benachrichtigungen auch im Energiesparmodus oder bei geringem Ladestand sicherzustellen.
    - **Live-Vordergrund-Synchronisation:** Registrierung eines debouncten `ContentObserver`s auf Systemkontakte in `MainActivity.kt` via Compose `DisposableEffect`. Die App aktualisiert den lokalen Cache nun in Echtzeit, sobald der Nutzer aus der Kontakte-App zurückkehrt oder dort Änderungen vorgenommen werden.
142. **Manuelle Theme-Auswahl & Akzentfarben (Manual Theme Settings):**
    - **Theme-Modus:** Integration von Optionen für Hell, Dunkel und Systemstandard.
    - **AMOLED Black:** Option für reines Schwarz (`Color.Black`) auf OLED-Displays mit angepassten, abgedunkelten Container-Tokens, um Konturen sichtbar zu halten.
    - **Akzentfarben-Grid:** Grid-Auswahl mit 6 Akzentfarben (Lila, Blau, Grün, Rot, Orange, Pink) sowie Dynamic Color (Material You) Unterstützung ab Android 12+.
    - **Navigation & Layout:** Platzierung des neuen „Design“-Menüpunkts exakt zwischen *Daten sichern* und *Kontakte synchronisieren* in der mobilen Liste und im Master-Detail-Tabellen-Layout.
    - **Lokalisierung:** Bereitstellung vollständiger Übersetzungen in Englisch und Deutsch.
    - **Datenbank & Migration V5->V6:** Schemaerweiterung von `AppSettings` und KSP-Schema-Export, abgesichert durch automatisierten Migrationstest (`migrate5To6` in `SettingsMigrationTest.kt`).
143. **Baseline Profiles Integration & Performance-Optimierung (Baseline Profiles):**
    - **Setup Modul:** Hinzufügen des dedizierten Testmoduls `:baselineprofile` zur Erfassung und Bereitstellung von Vorkompilierungsinformationen.
    - **Abhängigkeiten & Plugins:** Einbindung der `androidx.baselineprofile` Plugins (Version 1.4.1) und `androidx.profileinstaller` zur Laufzeitintegration.
    - **AGP 9.2.1 Kompatibilität:** Härtung des Builds mit `android.newDsl=false` in `gradle.properties` zur Re-Aktivierung der Legacy-Variant-API, um den Fehler `Module :app is not a supported android module` zu umgehen. Entfernung veralteter expliziter Kotlin-Plugins im Testmodul gemäß AGP 9.0 Standard.
    - **Automatisierte Erfassung:** Implementierung des `BaselineProfileGenerator` Tests mit 5 Sekunden Ruhezeit nach Launch zur sicheren Akkumulation und Extraktion von Profildaten auf dem Emulator.
    - **Ergebnis:** Erfolgreicher Generierungslauf (`BUILD SUCCESSFUL in 5m 3s`) und Erstellung der Baseline Profile (`baseline-prof.txt`) und Startup Profile (`startup-prof.txt`) im release/generated-Ordner der App.
144. **Erweiterte Theme-Akzentfarben & Gradle-Bereinigung (Custom Accent Color & Gradle Cleanup):**
    - **Custom-Hex-Code-Picker:** Einführung einer Option für eine benutzerdefinierte Akzentfarbe („Eigene“). Ein moderner Material 3 Dialog bietet 8 neue Farbvorlagen (Teal, Cyan, Indigo, Amber, Brown, Deep Orange, Blue Grey, Mint) sowie ein HEX-Eingabefeld mit Live-Vorschau und Regex-Validierung.
    - **Laufzeit-Generierung (HSV-Palette):** Entwicklung eines Algorithmus zur dynamischen HSV-basierten Berechnung aller Material 3 Farb-Tokens (primary, primaryContainer, secondary, backgrounds, surfaces etc.) aus einer einzigen Quellfarbe für Light-, Dark- und AMOLED-Themes.
    - **Warning Cleanups:** Behebung von 3 `parseColor` Warnungen durch Nutzung der KTX-Erweiterungsfunktion `String.toColorInt()`.
    - **KTS DSL Migration:** Bereinigung von AGP 9.0+ Deprecation-Warnungen durch Ersetzung des veralteten impliziten `android { ... }` Blocks in `app/build.gradle.kts` und `baselineprofile/build.gradle.kts` mit modernen expliziten Schnittstellen: `configure<ApplicationExtension>` und `configure<TestExtension>`. Verschiebung des `kotlin` Toolchain-Konfigurationsblocks im Testmodul auf die Projekt-Ebene zur Auflösung von Typ-Receiver-Konflikten.
145. **Behebung von Compiler-Warnungen bei Flow-Debounce (Warning Resolution):**
    - **Moderne API-Migration:** Umstellung des veralteten `Long`-Parameters bei `.debounce(300)` auf das moderne `Duration`-Overload (`.debounce(300.milliseconds)`) in [HomeViewModel.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/HomeViewModel.kt).
    - **Vorschau-API Annotation:** Hinzufügen der `@OptIn(FlowPreview::class)` Annotation sowie des entsprechenden Imports zu `HomeViewModel` zur Behebung der Preview-API Warnung.
146. **Compose-Stabilität: `@Stable` auf `HomeActions` (Performance):**
    - **Recomposition-Optimierung:** Annotation der `HomeActions` Data-Class mit `@Stable` in [HomeActions.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeActions.kt). Dies signalisiert dem Compose-Compiler, dass die Klasse als stabil behandelt werden kann, wodurch unnötige Recompositions in allen Kindkomponenten (`BirthdayList`, `BirthdayItem`, `HomeTopBar`, `ContactActionRow`, `HomeFAB`, `BirthdayDetailPane`) übersprungen werden können.
147. **Compose-Stabilität: `CoupleSuggestionUiModel` eingeführt (Architecture & Performance):**
    - **Neues UI-Modell:** Einführung von [CoupleSuggestionUiModel.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/CoupleSuggestionUiModel.kt) als `@Immutable` Data-Class mit nur den für die Anzeige nötigen Feldern (`lookupKey`, `fullName`, `imageUri`, `initials`).
    - **Layer-Trennung:** Entfernung der Room-Entity `Contact` aus der UI-Schicht. `HomeUiState.coupleSuggestion` ist nun `CoupleSuggestionUiModel?` statt `Pair<Contact, Contact>?`. Dies behebt den Widerspruch, dass `HomeUiState` als `@Immutable` annotiert war, aber eine instabile Room-Entity enthielt.
    - **Initialen-Vorberechnung:** Die Initialen werden nun im ViewModel vorberechnet statt bei jeder Recomposition inline im `CoupleSuggestionBanner`.
    - **Betroffene Dateien:** [HomeUiState.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/ui/model/HomeUiState.kt), [HomeViewModel.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/viewmodel/HomeViewModel.kt), [BirthdayList.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/ui/screens/home/components/list/BirthdayList.kt).
148. **Flow-Optimierung: `distinctUntilChanged()` auf Repository-Flows (Performance):**
    - **Redundante Emissionen eliminiert:** Room-DAOs emittieren bei jeder Tabellen-Schreiboperation die gesamte Liste neu — auch wenn sich die Daten nicht geändert haben. Durch `.distinctUntilChanged()` werden identische aufeinanderfolgende Emissionen herausgefiltert.
    - **ContactRepository:** `allContacts` und `labelConfigs` in [ContactRepository.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/data/repository/ContactRepository.kt).
    - **NotificationRepository:** `allRules` und `settings` in [NotificationRepository.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/data/repository/NotificationRepository.kt).
    - **Hinweis:** `otherEventsEnabled`, `ignoredCouplePairs` (ContactRepository) und `currentDate` (TimeRepository) hatten bereits `distinctUntilChanged()`.
149. **Gradle Build-Performance-Optimierung (Build Speed):**
    - **JVM-Heap:** Erhöhung von 2 GB auf 4 GB in [gradle.properties](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle.properties) — Compose + KSP + Hilt benötigen deutlich mehr Speicher.
    - **Parallele Builds:** `org.gradle.parallel=true` aktiviert (war auskommentiert).
    - **Build-Cache:** `org.gradle.caching=true` — vermeidet redundante Task-Ausführungen bei inkrementellen Builds.
    - **Configuration-Cache:** `org.gradle.configuration-cache=true` — parsed Build-Scripts nur einmal (kompatibel mit Hilt 2.59+ und KSP 2.3+).
    - **Kotlin Incremental:** `kotlin.incremental=true` explizit aktiviert.
    - **Bereinigung:** Entfernung des veralteten Legacy-Flags `android.disallowKotlinSourceSets=false`. `android.newDsl=false` bleibt erhalten (erforderlich für die Kompatibilität des BaselineProfile-Plugins mit `configure<ApplicationExtension>`).
150. **ProGuard: Lesbare Crash-Stacktraces aktiviert (Stability):**
    - **Aktivierung:** `-keepattributes SourceFile,LineNumberTable` and `-renamesourcefileattribute SourceFile` in [proguard-rules.pro](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/proguard-rules.pro) einkommentiert.
    - **Effekt:** Release-Crash-Reports (Play Console, Firebase Crashlytics) enthalten nun Zeilennummern und können über die Mapping-Datei deobfuskiert werden. `-renamesourcefileattribute SourceFile` ersetzt Originaldateinamen durch einen Platzhalter, um die APK-Größe minimal zu halten.
151. **Baseline Profile: Fehlende User Journeys dokumentiert (Performance / TODO):**
    - **Dokumentation:** TODO-Kommentare mit Code-Skizzen für 4 kritische User Journeys in [BaselineProfileGenerator.kt](file:///C:/Users/chris/AndroidStudioProjects/BirthdayBuddy/baselineprofile/src/main/java/com/heckmannch/birthdaybuddy/baselineprofile/BaselineProfileGenerator.kt) hinzugefügt: Home-Screen-Scrollen, Suche/Filter, Kontakt-Details expandieren, Settings-Navigation.
    - **Hintergrund:** Der Generator deckte bisher nur den App-Start ab. Die dokumentierten Journeys stellen sicher, dass bei einer späteren Implementierung die meistgenutzten Code-Pfade AOT-kompiliert werden.
152. **Optimierte ViewModel-State Flows (Redundante stateIn-Ketten behoben):**
    - **UiState-Konsolidierung:** Zusammenfassung mehrerer einzelner `stateIn`-Properties zu einem einzigen, gebündelten `UiState`-Flow in `CalendarViewModel` (`CalendarUiState`), `NotificationViewModel` (`NotificationUiState`) und `ThemeViewModel` (`ThemeUiState`). Dies eliminiert redundante Sammlungs- und Konvertierungsketten.
    - **NotificationViewModel Integration:** Kombination der Settings- und Erinnerungsregel-Flows über den `combine`-Operator zu einem einzigen `NotificationUiState`-Flow.
    - **OnboardingViewModel Optimierung:** Bereinigung des `onboardingCompleted`-Flows durch direkte Abbildung aus den Repository-Einstellungen, wodurch der redundante Zwischen-Flow und doppelte `stateIn`-Aufrufe entfallen.
    - **Compose-Anpassung:** Refactoring von `CalendarSettingsScreen`, `NotificationSettingsScreen`, `OtherEventsSettingsScreen` und `ThemeSettingsScreen`, um den Zustand direkt aus dem jeweiligen `uiState` zu abonnieren.
153. **ContactMapper Scoping (Performance):**
    - **@Reusable Scoping:** Annotation der `ContactMapper`-Klasse mit `@Reusable` zur Wiederverwendung der Mapper-Instanz in der Hilt-Dependency Injection. Dies reduziert redundante Objekterzeugungen und Garbage-Collector-Overhead.
154. **Regex-Caching (Performance):**
    - **Caching von String-Splits & -Replacements:** Ersetzung von dynamisch kompilierten `.toRegex()`-Aufrufen in `ContactMapper.kt`, `ContactActions.kt` und `DateUtils.kt` durch statisch deklarierte und wiederverwendbare `WHITESPACE_REGEX`-Objekte. Dies vermeidet wiederholtes Kompilieren desselben Musters bei häufig ausgeführten Operationen (z. B. beim Mapping von Kontakten und Formatieren von Telefonnummern).
155. **Thread-Sicherheit & Dispatcher-Zuweisung (Performance):**
    - **syncScheduling Dispatcher:** Ausführung von `syncScheduling()` in `NotificationRepository.kt` auf `Dispatchers.IO` über `withContext(Dispatchers.IO)`. Dies stellt sicher, dass das Abfragen der Benachrichtigungsregeln aus der Datenbank und das Scheduling des WorkManagers im Hintergrund ausgeführt werden und nicht den aufrufenden Thread (oft den Main-Thread der ViewModels) blockieren.
156. **Robuste Worker-Selbstplanung (Stability):**
    - **Coroutine-basierte Rescheduling-Verzögerung:** Umstellung der zeitverzögerten Selbstplanung in `NotificationWorker.kt` und `BirthdayWidgetWorker.kt` von der unzuverlässigen, main-thread-gebundenen `Handler.postDelayed(...)`-API auf ein prozessweites `CoroutineScope`-Verfahren (`applicationScope.launch { delay(1000.milliseconds); ... }`). Dies verbessert die Robustheit des zeitverzögerten Neu-Einreihens nach Ausführungserfolg der Hintergrund-Worker.
    - **Lösung von Compiler-Warnungen:** Verwendung des modernen `Duration`-Parameters bei `delay(1000.milliseconds)` anstelle des veralteten `Long`-Overloads, um Kotlin-Compiler-Warnungen vollständig aufzulösen.
157. **SettingsScreen Code-Duplikation behoben (Refactoring & Clean Code):**
    - **Deklaration konsolidiert:** Einführung der Datenklasse `SettingsMenuItemData` in [SettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt), um alle Menüpunkte an einer einzigen Stelle zu deklarieren (`menuItems` via `remember`).
    - **LazyColumn-Schlankheit:** Verwendung von `items(menuItems)` in den beiden Layouts (Compact für Smartphones und split-pane Master-Detail für Tablets). Dies eliminiert die doppelte, zeilenweise Definition aller Menüeinträge und deren Callbacks.
158. **FastScrollbar Compose Key & derivedStateOf-Fixes (Stability & Correctness):**
    - **isResettingFilter Key:** Zuweisung von `isResettingFilter` als Key im `remember(contacts, isResettingFilter)` von `canScroll` in [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt). Da es sich um ein primitives Boolean-Argument handelt, muss es explizit als Key registriert werden, damit Änderungen am Filter-Reset-Status korrekt durch den `derivedStateOf`-Block erfasst und ausgewertet werden.
    - **trackAlpha Bereinigung:** Entfernung der redundanten und fehleranfälligen `remember { derivedStateOf { ... } }`-Kapselung für `trackAlpha` in `ScrollbarTrackAndThumb`, da die Inputs primitive Parameter der Composable-Funktion sind. Der Alpha-Wert wird nun direkt zugewiesen.
159. **Couple-Operationen in Datenbank-Transaktionen gekapselt (Stability & Data Integrity):**
    - **Transaktionale Kapselung:** Verwendung von Room-Transaktionen (`withTransaction`) für alle verknüpfenden und trennenden Operationen in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt). Da Kontakte und Benutzerdaten in zwei separaten Datenbank-Dateien liegen (`AppDatabase` und `SettingsDatabase`), werden die Änderungen in jeder der beiden Datenbanken nun jeweils atomar in einer Transaktion durchgeführt.
    - **Vermeidung inkonsistenter Zustände:** Dies stellt sicher, dass partnerbezogene Verknüpfungen (z. B. das gegenseitige Speichern des `spouseLookupKey`) entweder auf beiden Seiten vollständig oder gar nicht in der Datenbank gespeichert werden. Auch die Anpassungen in der Ignorierliste von Paaren (`ignoreCoupleSuggestion`, `clearIgnoredCouplePairs`) werden nun transaktionsgesichert ausgeführt.
160. **Ungenutzte kotlinx-serialization Dependency entfernt (Clean Code & Dependency Management):**
    - **Entfernung:** Löschung der nicht verwendeten `kotlinx-serialization-json` Bibliothek aus [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) und dem Version Catalog [libs.versions.toml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml). Dies reduziert die Anzahl der Bibliotheken im Klassenpfad und bereinigt die Build-Konfiguration.
161. **Hardcodierte Farbe durch BirthdayKidAmber ersetzt (Clean Code & UI Consistency):**
    - **Ersetzung:** Ersetzung der fest in [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt) kodierten Hex-Farbe `Color(0xFFFFB300)` durch die vordefinierte, semantische Farbkonstante `BirthdayKidAmber` aus dem Theme-Paket. Dies vereinheitlicht die Farbpalette der App und Beseitigt redundante Farbwahrnehmungen im UI.
162. **BackupViewModel Coroutine-Kapselung & Thread-Sicherheit (Architecture & Concurrency):**
    - **Vermeidung von Suspend-Exposition:** Umstellung der Methoden in [BackupViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/BackupViewModel.kt) auf reguläre (nicht-suspend) Funktionen. Das Starten der Coroutines erfolgt nun intern über den viewModel-eigenen `viewModelScope.launch`, anstatt diese Verantwortung dem UI-Layer aufzubürden.
    - **Echte Thread-Sicherheit (I/O):** Die blockierenden Datei-Lese- und Schreibvorgänge (ContentResolver-Operationen über Uri-Streams) wurden aus der Compose-UI in das ViewModel verlagert und werden dort explizit auf dem für Dateizugriffe optimierten `Dispatchers.IO`-Thread ausgeführt.
    - **UI-Bereinigung:** Vollständiges Entfernen von `rememberCoroutineScope()`, `scope.launch` und nicht genutzten Coroutine-Imports in [BackupScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/backup/BackupScreen.kt).
163. **Duplizierter Handler in ContentObserver behoben (Clean Code & Optimization):**
    - **Handler-Kapselung:** Definition einer einzigen Instanz von `Handler(Looper.getMainLooper())` im `DisposableEffect`-Block von [MainActivity.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/MainActivity.kt).
    - **Vermeidung von Redundanz:** Diese Instanz wird nun sowohl an den Konstruktor der Superklasse `ContentObserver` übergeben als auch im Runnable-Debounce (`removeCallbacks`/`postDelayed`) wiederverwendet, anstatt zwei separate Instanzen des Haupt-Thread-Handlers zu erzeugen.
164. **Bereinigung veralteter Gradle-Properties (Clean Code & Configuration):**
    - **Jetifier-Entfernung:** Löschung der veralteten Eigenschaft `android.enableJetifier=false` aus [gradle.properties](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle.properties), da Jetifier ab Android Gradle Plugin (AGP) 8.0+ standardmäßig deaktiviert ist und keine Auswirkung mehr hat.
    - **newDsl-Verifizierung:** Verifizierung und Beibehaltung von `android.newDsl=false`. Ein temporärer Entfernungsversuch bestätigte, dass das verwendete `androidx.baselineprofile` Plugin (Version 1.4.1) unter AGP 9.2.1 die veralteten Variant-APIs nutzt und ohne dieses Flag mit dem Fehler `Module :app is not a supported android module` fehlschlägt.
165. **Widget DateTimeFormatter-Hoisting (Performance & Optimization):**
    - **Hoisting:** Verschiebung der Instanziierung von `dateFormatter` and `dayMonthFormatter` aus der Zeilen-Schleife (`BirthdayRow`) in das übergeordnete Composable `WidgetContent` in [BirthdayWidget.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidget.kt).
    - **Verwendung von remember:** Kapselung der Erzeugung in `remember`-Blöcken unter Einbeziehung des aktuellen `locale` als Key, sodass die Formatierer nur einmal pro Widget-Update erzeugt und für alle Tabellenzeilen wiederverwendet werden, anstatt sie für jedes einzelne Geburtstagselement neu zu allozieren.
166. **Tote Datenbank-Migrationen & Schemas entfernt (Clean Code & Database Hardening):**
    - **Entfernung alter Migrationspfade:** Vollständiges Löschen der nicht mehr registrierten Legacy-Migrationspfade von Version 1 bis 5 (`MIGRATION_1_2` bis `MIGRATION_4_5`) aus [AppDatabase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/AppDatabase.kt). Ältere Datenbankversionen werden wie dokumentiert über den destruktiven Fallback-Pfad von Room bereinigt.
    - **Test-Refactoring:** Entfernung veralteter Testmethoden (`migrate1To2`, `migrate1To9`, `migrate2To9`) und Implementierung einer stabilen Validierung der Migrationskette ab Version 5 (`migrate5To9` sowie Aktualisierung von `migrateAll`) in [MigrationTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/androidTest/java/com/heckmannch/birthdaybuddy/data/local/MigrationTest.kt).
    - **Schema-Bereinigung:** physisches Löschen veralteter Schema-Dateien (JSON 1 bis 4 von `AppDatabase`, JSON 1 von `SettingsDatabase` sowie das ungenutzte Verzeichnis der Legacy-Datenbank vor dem Package-Rename), um Altlasten aus dem Repository zu entfernen.
167. **ContentObserver-Ressourcenbereinigung (Stability & Memory Management):**
    - **Handler-Callback-Cleanup:** Hinzufügen von `mainHandler.removeCallbacksAndMessages(null)` im `onDispose`-Block des `DisposableEffect` in [MainActivity.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/MainActivity.kt). Dies stellt sicher, dass alle anstehenden, debouncten Kontaktsynchronisations-Tasks auf dem Haupt-Thread abgebrochen werden, sobald der ContentObserver dekonstruiert oder die Composition verlassen wird, um Memory Leaks und verzögerte Ausführungen nach Lifecycle-Ende zu verhindern.
168. **Wiederverwendbarer HSV Color Picker (UI & Customization):**
    - **Shared Component:** Entwicklung einer zentralen, wiederverwendbaren [ColorPickerDialog.kt](file:///c:/Users/heckmannch/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ColorPickerDialog.kt) Komponente in `ui.components`. Der neue Picker integriert farblich passende Presets, ein interaktives HSV-Auswahlfeld (Sättigung und Helligkeit per Drag & Tap), einen Hue-Slider für den Farbton sowie eine Echtzeit-HEX-Eingabe mit Regex-Validierung und Farb-Vorschau.
    - **Settings Design:** Umstellung des Theme-Akzentfarben-Pickers in [ThemeSettingsScreen.kt](file:///c:/Users/heckmannch/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/theme/ThemeSettingsScreen.kt) auf den neuen shared ColorPickerDialog unter Beibehaltung der Design-Presets.
    - **Kalender-Einstellungen:** Ersetzung des alten, einfachen Preset-Farb-Auswahldialogs in [CalendarSettingsScreen.kt](file:///c:/Users/heckmannch/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/calendar/CalendarSettingsScreen.kt) durch den voll funktionsfähigen, neuen shared ColorPickerDialog, wodurch Nutzer nun auch beliebige RGB-Kalenderfarben wählen können.
    - **Struktur-Doku:** Pflege des neuen UI-Komponenten-Eintrags in [PROJECT_STRUCTURE.md](file:///c:/Users/heckmannch/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md).
169. **Baseline Profile Abdeckung optimiert (Performance & Automation):**
    - **Automatisierte User Journeys:** Implementierung der verbleibenden TODO-Blöcke in [BaselineProfileGenerator.kt](file:///c:/Users/heckmannch/AndroidStudioProjects/BirthdayBuddy/baselineprofile/src/main/java/com/heckmannch/birthdaybuddy/baselineprofile/BaselineProfileGenerator.kt) zur Erfassung kritischer Benutzerpfade. Der Generator deckt nun neben dem App-Start auch das Scrollen der Liste, die Suche und Filterung von Kontakten, das Expandieren/Kollabieren von Geburtstagskarten und die Navigation zu den Einstellungen ab.
    - **UI-Automation Test-Tags:** Hinzufügen von `.testTag(...)` in [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) (`"birthday_list"` und `"birthday_item_$index"`) und in [SearchBar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/topbar/SearchBar.kt) (`"search_field"` und `"settings_button"`), damit UI Automator die Composables sprachneutral und robust auffinden kann.
    - **Robuste Fehlerbehandlung:** Die Automatisierungsschritte prüfen vor der Interaktion die Existenz der Elemente, um Fehlschläge des Profil-Generators auf leeren Test-Emulatoren (ohne Kontakte) zu verhindern.
170. **Modernisiertes & Attraktiveres Onboarding (UI/UX Redesign):**
    - **Dynamischer Ambient-Hintergrund:** Einbindung eines weichen vertikalen Farbverlaufs im Hintergrund des [OnboardingScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/OnboardingScreen.kt), der beim Blättern über einen 500ms `animateColorAsState` sanft die Farbe wechselt, passend zur jeweiligen Themen-Seite.
    - **Auto-Advance bei Berechtigungen:** Automatische Weiterleitung zur nächsten Onboarding-Seite nach 300ms Verzögerung, sobald eine Berechtigung (Kontakte, Benachrichtigungen, Kalender) erfolgreich erteilt wurde, zur Reduzierung von Klick-Reibung.
    - **Navigations-Verbesserung:** Integration einer "Zurück"-Schaltfläche im Navigations-Footer [OnboardingFooter.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/OnboardingFooter.kt) zur Ermöglichung freier zweiseitiger Navigation.
    - **Pill-Shape Page Indicator:** Animation des aktiven Seiten-Dots in eine moderne Pillen-Form mittels Breiten- und Farb-Übergängen (`animateDpAsState` / `animateColorAsState`).
    - **Interaktive Compose-Illustrationen:** Ablösung der grauen, statischen Symbole durch hochwertige, interaktive Mock-UIs:
        - [WelcomePage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/WelcomePage.kt): Schwebende Geburtstagskarte mit sanfter Float-Animation.
        - [ContactsPage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/ContactsPage.kt): Dynamische Umschaltung zwischen der Lottie-Animation `anim_contacts` und einer primären Success-Card nach Berechtigungserhalt.
        - [NotificationsPage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/NotificationsPage.kt): Ein schwebendes Android-Notification-Banner, das bei Aktivierung persistenter Erinnerungen ein Schloss-Symbol und einen primären Rand animiert.
        - [CalendarPage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/CalendarPage.kt): Ein Minikalender-Grid, das bei Aktivierung ein Geburtstagsevent animiert einblendet und einen Geburtstagstag markiert.
        - [CalendarGuidePage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/CalendarGuidePage.kt): Eine visuelle Nachbildung des Kalender-Navigationsmenüs mit Checkliste zur Erleichterung der BirthdayBuddy-Kalenderaktivierung.
        - [ReadyPage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/ReadyPage.kt): Eine elastisch skalierende Success-Grafik umrahmt von einem fallenden Konfetti-Partikelsystem (`ConfettiEffect`).
    - **Lokalisierung:** Übersetzung der Navigationsbuttons ("Back" / "Zurück") in Deutsch und Englisch.
    - **Compiler-Warnungen behoben:** Behebung aller Warnungen bezüglich ungenutzter Funktionsparameter (`modifier` in `ContactsPage.kt`, `stepNumber` und `e` in `CalendarGuidePage.kt`) und Umstellung veralteter `delay(Long)`-Aufrufe auf die moderne `Duration`-API (`delay(300.milliseconds)`) in `OnboardingScreen.kt`.
    - **Codebereinigung (Tote Klassen entfernt):** Löschung der veralteten und nicht mehr benötigten Hilfsfunktion `OnboardingPageContent` (sowie deren Previews) aus [OnboardingCommon.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/OnboardingCommon.kt), da alle Seiten nun das flexiblere `OnboardingPageTemplate` direkt verwenden.
171. **Code Review & Cleanups (Files & Docs):**
    - **Leere Testverzeichnisse entfernt:** Löschung verwaister und leerer Verzeichnisse in den Unit- und UI-Testzweigen (`src/test` und `src/androidTest`).
    - **Dokumentations-Update:** Aktualisierung von [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md) zur Einpflege von [SyncSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/sync/SyncSettingsScreen.kt) und Korrektur des Pfades für [OtherEventsSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/otherevents/OtherEventsSettingsScreen.kt).
172. **Optimierung der Ehepaar-Vorschläge (Couple Suggestions Performance):**
    - **SQLite-JOIN Abfrage:** Verlagerung der Ermittlung potenzieller Paare (gleiches Hochzeitsdatum, kein spouseLookupKey) aus dem Kotlin-Speicher in eine effiziente Join-Abfrage (`getPotentialCouples`) in [ContactDao.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/ContactDao.kt) unter Verwendung von `SUBSTR(anniversary, 6)` für den monatlich-täglichen Abgleich.
    - **Modell PotentialCouple:** Einführung des Modells [PotentialCouple.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/PotentialCouple.kt).
    - **HomeViewModel Refactoring:** Bereinigung von in-memory Schleifen im `HomeViewModel.kt`. Das ViewModel filtert nun die Ergebnisse direkt über die neue Repository-Eigenschaft `potentialCouples` und gleicht sie mit `ignoredCouplePairs` ab.
173. **JVM-Test-Migration & Kalender-Provider Entkopplung (Architecture & Testing):**
    - **Test-Migration auf JVM (`src/test`):** Verschiebung aller reinen Unit-Tests (wie `ConvertersTest.kt`, `GiftIdeaConvertersTest.kt`, `ContactMapperTest.kt`, `HomeViewModelSearchTest.kt` und `HomeViewModelTest.kt`) aus dem instrumentierten Verzeichnis `src/androidTest` in das lokale Verzeichnis `src/test`. Dadurch entfällt die Notwendigkeit eines Emulators für logische Tests, was die Testausführung erheblich beschleunigt (von Minuten auf Sekunden).
    - **Entkopplung des Calendar Providers:** Einführung des Interfaces [SystemCalendarDataSource.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/SystemCalendarDataSource.kt) und der Datenklasse [SystemCalendarInfo.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/SystemCalendarInfo.kt) zur Kapselung aller direkten `ContentResolver`- und `CalendarContract`-Aufrufe.
    - **Hilt-DI-Implementierung:** Bereitstellung der konkreten Implementierung [SystemCalendarDataSourceImpl.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/SystemCalendarDataSourceImpl.kt) über Dagger Hilt Bindings in [HelperBindingsModule.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/di/HelperBindingsModule.kt).
    - **Refactoring von CalendarSyncRepository:** Umstellung des [CalendarSyncRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepository.kt) auf die Nutzung des neuen Interfaces, wodurch die gesamte Kalendersynchronisationslogik komplett unit-testbar auf der JVM wird.
    - **JVM-Safety im Mapper:** Absicherung des `dayMonthFormatter` in [ContactMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapper.kt) gegen `RuntimeException`s (wegen nicht gemockter SDK-Methoden wie `DateFormat.getBestDateTimePattern` auf der JVM) mittels eines JVM-safe Fallbacks.
    - **Fehlerbehebungen in ViewModel-Tests:** Behebung von `UncompletedCoroutinesError` und `NullPointerException`s in `HomeViewModelTest` and `HomeViewModelSearchTest` durch Kündigung des `viewModelScope`s in `@After` und Stubbing des neuen `potentialCouples` Flows.
174. **Behebung der Migrationstest-Abstürze (Database testing):**
    - **Fehlerbehebung in Migrationstests:** Behebung eines `AbstractMethodError` Absturzes (`typeParametersSerializers()`) bei der Ausführung der instrumentierten Datenbank-Migrationstests (`MigrationTest` und `SettingsMigrationTest`). Dieser Absturz trat auf, da Room-Testing (`androidx.room:room-testing`) die Bibliothek `kotlinx-serialization` zum Laden der Datenbank-Schemas nutzt, diese jedoch im Zuge vorheriger Aufräumarbeiten (Entfernung ungenutzter Abhängigkeiten) de-synchronisiert wurde.
    - **Wiederherstellung der Abhängigkeiten:** Explizite Deklaration von `kotlinx-serialization-core` und `kotlinx-serialization-json` in Version `1.8.1` in `gradle/libs.versions.toml` und Einbindung als `implementation` in `app/build.gradle.kts` zur Beseitigung des Version-Mismatch-Konflikts bei der Android-Testausführung.
    - **Validierung:** Erfolgreiche Ausführung aller 7 instrumentierten Migrations-Tests auf dem physischen Gerät des Nutzers.
175. **Leichtgewichtiges MVI-Refactoring des `HomeViewModel` (Architecture & MVI):**
    - **UDF & Zustandskonsolidierung:** Migration der Zustandsverwaltung im [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/HomeViewModel.kt) hin zu einem Uni-Directional Data Flow (UDF) / Model-View-Intent (MVI) Muster. Konsolidierung der 6 einzelnen StateFlows (`searchQuery`, `selectedLabel`, `isResettingFilter`, `isSyncing`, `searchFocusRequested`, `newlyAddedIdeaId`) in einer einzigen privaten `UserUiState` Datenklasse und einem `_userUiState` StateFlow zur Vermeidung asynchroner Race Conditions.
    - **Zentrale Intent-Verarbeitung:** Einführung des `HomeIntent` Interfaces und der zentralen Methode `onIntent(HomeIntent)` zur synchronen/asynchronen Verarbeitung aller Aktionen und Zustandsübergänge an einer zentralen Stelle.
    - **Abwärtskompatibilität:** Beibehaltung aller bisherigen öffentlichen VM-Methoden (wie `onSearchQueryChange`, `resetFilters`, etc.) als Wrapper-Methoden, die ihre Intents direkt an `onIntent` delegieren. Dadurch blieb der gesamte Compose-Code sowie alle Tests voll kompatibel und funktionsfähig ohne Anpassungsbedarf.
    - **Behebung von Test-Race-Conditions auf CI:** Behebung eines Looper-bezogenen Absturzes bei der Testbereinigung im GitHub-Runner. Die Testklassen `HomeViewModelTest` und `HomeViewModelSearchTest` wurden angepasst, um im `@After` tearDown-Block über `runTest` explizit auf das vollständige Beenden (`job.join()`) aller verbleibenden Coroutines des ViewModels (z. B. der verzögerten SharedFlow/StateFlow-Abos) zu warten, bevor der Main-Dispatcher zurückgesetzt wird.
    - **Validierung:** Erfolgreiche Validierung aller JVM-Unit-Tests und aller instrumentierten Migrationstests auf Emulator und Gerät.
176. **Java 17-Upgrade & Modernisierung des Theme-Systems (Build & Material 3):**
    - **Java 17-Upgrade:** Anhebung der Java-Kompatibilität (`sourceCompatibility` und `targetCompatibility`) von `JavaVersion.VERSION_11` auf `JavaVersion.VERSION_17` in [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) und [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/baselineprofile/build.gradle.kts). Integration des Kotlin JVM Toolchains (`jvmToolchain(17)`) im App-Modul zur Vereinheitlichung.
    - **M3 Shapes Integration:** Erstellung von [Shapes.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Shapes.kt) zur Definition der standardmäßigen Eckenradien (Extra Small bis Extra Large) für Material 3 und Übergabe dieser Shapes an das `MaterialTheme` in [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt).
    - **Vollständige M3-Farbschemata:** Vollständige Definition aller Farbrollen (Container, Outlines, etc.) für das Standard-Lila-Schema und alle 5 M3-Presets (BLUE, GREEN, RED, ORANGE, PINK) in [Theme.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Theme.kt) für Light- und Dark-Modes zur Vermeidung unvollständiger Farb-Fallbacks.
    - **Kontraststarke HSV-Generierung:** Aktualisierung der Methode `getDynamicCustomColorScheme` zur vollständigen Belegung aller Farbrollen (inkl. Outline-Varianten, Error-Rollen und passenden Kontrasten).
    - **M3 Typografie vervollständigt:** Vollständige Deklaration aller 15 Material 3 Typografiestile (Display, Headline, Title, Body und Label in Large, Medium und Small) mit standardmäßigen Font-Spezifikationen in [Type.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Type.kt).
    - **Struktur & Dokumentation:** Dokumentation der neuen Theme-Komponente in [PROJECT_STRUCTURE.md](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md).
177. **Modernisiertes Widget-Design (UI/UX Redesign):**
    - **Element-Einrückung & Transparenter Container:** Entfernung der festen Hintergrundfarbe (`GlanceTheme.colors.surface`) des äußeren Containers in [BirthdayWidget.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidget.kt). Die Karten schweben nun transparent auf dem Homescreen, wobei ein horizontales Inset von `8.dp` das Abschneiden der abgerundeten Ecken durch den System-Launcher verhindert.
    - **Transluzente Kartenelemente:** Einführung eines einheitlichen, semitransparenten Hintergrunds für alle Geburtstagskarten mittels `ColorProvider(day, night)` mit 80% Opazität (`0xCCFFFFFF` / weiß im Light-Theme; `0xCC1E1E1E` / dunkelgrau im Dark-Theme) für optimale Lesbarkeit.
    - **Kompaktes Spacing & Höhenberechnung:** Reduzierung des vertikalen Paddings auf `2.dp` pro Karte (insgesamt `4.dp` Abstand zwischen den Karten) für ein kompakteres Design. Anpassung der Höhenberechnung (Abzug von `8.dp` statt `16.dp` für das äußere Padding), um den Platz optimal auszunutzen.
    - **Farbbereinigung:** Verzicht auf die altersabhängigen Gold-/Silber-/Grün-Hintergründe zur ästhetischen Beruhigung des Widgets.
178. **Tablet-Deselektion & Zitat-Platzhalter (Adaptive Tablet Layout Overhaul):**
    - **Startup-Zustand ohne Vorauswahl:** Entfernung der automatischen Vorauswahl des ersten Kontakts auf Tablets beim App-Start in [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt). Der Detailbereich rechts startet nun unselektiert.
    - **Geburtstags-Zitat-Platzhalter:** Entwicklung von [BirthdayQuotePlaceholder.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayQuotePlaceholder.kt), das einen zentrierten, optisch ansprechenden Platzhalter mit einem Geburtstagskuchen-Symbol, Zitatbox und einer Benutzeranleitung rendert, falls kein Kontakt ausgewählt ist.
    - **Deselektions-Möglichkeit (Close-Button):** Erweiterung von [BirthdayDetailPane.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayDetailPane.kt) um eine Schließen-Schaltfläche (X) oben rechts auf der Karte. Durch Klicken wird `selectedContactId` auf `null` zurückgesetzt, was den Detailbereich schließt und den Zitat-Platzhalter wieder anzeigt (inkl. automatischer Zurück-Navigation in einspaltigen Pane-Zuständen).
    - **Auto-Deselektion bei Filter- & Sync-Updates:** Integration eines `LaunchedEffect` in `HomeScreen.kt`, der die Auswahl automatisch zurücksetzt, wenn der aktuell ausgewählte Kontakt durch Suchen/Filtern oder Datenaktualisierungen aus der Liste verschwindet.
    - **Lokalisierung:** Hinzufügen der String-Ressourcen für das Zitat, Titel, Subtitel und Schließen-Schaltfläche in Deutsch ([strings.xml (de)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml)) und Englisch ([strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml)).
    - **M3-Konforme Einstellungen-Auswahl:** Überarbeitung der Menüelemente in [SettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt). Im Tablet-Layout werden ausgewählte Elemente nun als schwebende, abgerundete Kapseln/Pills mit `secondaryContainer`-Hintergrund und angepassten Text-/Icon-Farben dargestellt (M3 Navigation Drawer & List Guidelines), während sie auf Smartphones weiterhin im klassischen Vollbild-Stil verbleiben.
179. **Google Photos-Style Fade-Out Fast-Scrollbar (UI/UX Redesign):**
    - **Reaktives Ein-/Ausblenden:** Implementierung einer reaktiven Sichtbarkeitsprüfung in [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt). Der Scrollbalken blendet sich nun über eine weiche GPU-beschleunigte `graphicsLayer`-Alpha-Transition (300ms) ein, wenn der Nutzer scrollt, die Scrollbar zieht oder sich die Kontakte-Liste aktualisiert (für visuelle Orientierung beim Filtern), und blendet sich nach 1,5 Sekunden Inaktivität vollkommen aus.
    - **Touch-Pass-Through im Leerlauf:** Wenn die Scrollbar unsichtbar ist, wird das Abfangen von Gesten (`pointerInput`) komplett deaktiviert. Dadurch können Nutzer im rechten 48.dp-Bereich des Bildschirms wischen, um die Liste normal zu scrollen, ohne dass die Scrollbar fälschlicherweise Eingaben stiehlt.
    - **Premium-Thumb mit Richtungs-Pfeilen:** Neugestaltung des Scrollbar-Thumbs als feste Material 3 Kapsel (`26.dp` x `44.dp`) mit `primary`-Hintergrund und zwei integrierten winzigen Pfeil-Symbolen (Rauf/Runter) in `onPrimary`-Farbe zur deutlichen Signalisierung der Scrollbar-Funktion.
    - **Optimiertes Alignment:** Die vertikale Spurlinie und das Thumb-Zentrum wurden auf `21.dp` Abstand zum rechten Bildschirmrand verschoben, was dem Thumb `8.dp` visuelle Luft nach rechts gibt und Clipping-Fehler am Bildschirmrand gänzlich ausschließt.
180. **Fast-Scrollbar Full Screen Height & Layout Simplification (UI/UX Polish & Refactoring):**
    - **Full-Screen Height Scrollbar:** Reverted the previously introduced vertical padding on `BoxWithConstraints` in [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt), ensuring the scrollbar travels along the full screen height for accurate scroll position mapping.
    - **Code & Layout Simplification:** Inlined the thumb's `Surface` rendering directly inside the interactive track `Box` and deleted the `ScrollbarTrackAndThumb` private composable. This eliminated unnecessary nesting and deleted over 60 lines of boilerplate code while maintaining the exact same visual design.
    - **State Management & Hoisting:** Simplified the bubble visibility state by migrating from `produceState` to a standard `LaunchedEffect` and `mutableStateOf` (allowing removal of the unused `produceState` import). Hoisted the calculation of `thumbHeightPx` out of `pointerInput` into composition scope, which eliminated the need for tracking density via `rememberUpdatedState`.
181. **Lottie Dependency Removal & Native Compose Illustration (Performance & Size Optimization):**
    - **Native Contacts Illustration:** Created a premium native custom illustration [ContactsIllustration.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ContactsIllustration.kt) utilizing overlapping animated avatar circles, floating accent bubbles, and glowing background elements to represent contacts permission.
    - **Lottie Library Purge:** Completely removed the Lottie Compose dependency from the catalog [libs.versions.toml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml) and build configuration [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts).
    - **Resource & Code Cleanup:** Deleted the `anim_contacts.json` raw resource file (saving ~400 KB of raw resource size) and the `LottieIllustration.kt` wrapper class since Lottie is no longer used in the project.
    - **UI Integration:** Updated [ContactsPage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/ContactsPage.kt) and [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) to load the new native illustration.
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
224. **Auslagerung der UI-Layout-Struktur in `HomeContent.kt` (Refactoring / Clean Architecture):**
     - **Trennung der Zuständigkeiten:** Die Composable-Funktion `HomeContent`, das Preview `HomePreview` sowie die lokalen Navigations-Keys `HomeNavKey` wurden aus der koordinierenden [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) extrahiert und in die neue Datei [HomeContent.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt) überführt.
     - **Verbesserte Codestruktur:** [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) konzentriert sich nun ausschließlich als Controller auf Plattform-Events und Berechtigungen, während [HomeContent.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt) die reine Layout-Struktur mitsamt dem adaptiven Master-Detail-Pane-Scaffold abbildet.
225. **Umfassende Dokumentations-Erweiterung und Bereinigung von HomeScreen.kt (Code Quality & LLM Alignment):**
     - **English KDoc & Inline-Kommentare:** Der verbleibende Code in [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) wurde umfassend dokumentiert. Der Fokus liegt darauf, die architektonischen Entscheidungen (Rolle als event-koordinierender Controller) sowie komplexe Blöcke (wie Berechtigungsabfragen, reaktive Placeholder-Animationen und das Coil Image Prefetching) sowohl für menschliche Entwickler als auch für zukünftige LLM-Agenten detailliert zu erklären.
     - **Import-Hygiene:** Alle ungenutzten Compose-, Layout- und M3-Imports, die nach dem Auslagern des UI-Layouts in `HomeContent.kt` übrig geblieben waren, wurden restlos entfernt.

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

242. **Refactoring der Geschenkideen-Sektion in `BirthdayItem.kt` (Code Quality & Maintainability):**
    - **Extraktion:** Der expandierbare Geschenkideen-Bereich (Toggle-Row, LaunchedEffect zur Auto-Expansion und die Einbindung der `GiftIdeaList`) wurde aus der Haupt-Composable `BirthdayItem` in eine private Hilfs-Composable `BirthdayItemGiftIdeaSection` am Ende der Datei [BirthdayItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayItem.kt) ausgelagert.
    - **Einschränkungen & Kapselung:** Alle `remember`-Schlüssel (inkl. `remember(isExpanded)`), `LaunchedEffect`-Blöcke und Padding-Regeln blieben unverändert erhalten. Die Lokalisierung des `FocusManager` wurde sauber in die neue Hilfs-Composable verlagert.
    - **Code-Bereinigung:** Reduzierung der Komplexität und Größe der Haupt-Kompensation `BirthdayItem` zur besseren Lesbarkeit und Wartbarkeit.
    - **Verifikation:** Erfolgreiche Ausführung von `.\gradlew compileDebugKotlin` und `.\gradlew testDebugUnitTest`.

243. **Aktualisierung der Baseline Profiles & Journeys für Navigation 3 & Adaptive Layouts (Performance / DX):**
    - **Pre-granting von Berechtigungen:** Integration von ADB-Shell-Befehlen im `BaselineProfileGenerator` zur automatischen Freigabe aller kritischen Berechtigungen (`READ_CONTACTS`, `WRITE_CONTACTS`, `POST_NOTIFICATIONS`, `READ_CALENDAR`, `WRITE_CALENDAR`) vor dem App-Start. Dadurch wird das Hängenbleiben an System-Berechtigungsdialogen vermieden.
    - **Robuste Onboarding-Durchquerung:** Hinzufügen von Compose `testTag`s (`onboarding_next_button` in [OnboardingFooter.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/OnboardingFooter.kt) and `onboarding_start_button` in [ReadyPage.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/components/ReadyPage.kt)), um den Onboarding-Flow bei Erstinstallationen vollautomatisch und zuverlässig zu durchlaufen.
    - **FastScrollbar-Erfassung:** Aktualisierung der Scroll-Journey in [BaselineProfileGenerator.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/baselineprofile/src/main/java/com/heckmannch/birthdaybuddy/baselineprofile/BaselineProfileGenerator.kt) zur gezielten Interaktion mit der `FastScrollbar` (mittels `By.desc("Scrollbar")` and `device.drag`). Ein Fallback auf Standard-List-Flinging schützt den Testlauf bei geringer Kontaktanzahl.
    - **Adaptive Detail-Pane-Unterstützung:** Anpassung der Detail-Journey, um sowohl das Aufklappen von Elementen im Kompakt-Modus (Phones) als auch das Öffnen/Schließen des neuen Detail-Panes im geteilten Modus (Tablets/Foldables) abzudecken. Dafür wurden die Tags `birthday_detail_pane` und `detail_close_button` in [BirthdayDetailPane.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayDetailPane.kt) eingeführt.
    - **Settings-Navigation:** Ergänzung von `settings_back_button` in [SettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt) zur zuverlässigen Rückkehr aus den App-Einstellungen.
    - **Verifikation:** Erfolgreiche Validierung des Build-Setups unter AGP 9.2.1.

244. **JVM-Unit-Tests für `CalendarSyncRepositoryImpl` (Test-Abdeckung & Richtlinie §2.6):**
    - **Test-Suite:** Erstellung der neuen JVM-Unit-Testdatei [CalendarSyncRepositoryImplTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepositoryImplTest.kt) zur vollständigen Abdeckung der Kalender-Synchronisations- und Account-Management-Logik.
    - **Abgedeckte Szenarien:**
        1. `hasCalendarPermissions()` delegiert korrekt an die `SystemCalendarDataSource`.
        2. `updateCalendarColor()` speichert die aktualisierte Farbe in der lokalen Datenbank (`AppSettingsDao`) und ruft `updateCalendarColor` für den jeweiligen Kalendertyp (BIRTHDAY, ANNIVERSARY, NAMEDAY) im System-Kalender auf.
        3. `deleteCalendar()` löscht veraltete und aktive Kalender, setzt `calendarSyncEnabled = false` sowie `calendarId = null` und aktualisiert die Einstellungen.
        4. `debugPrintAllCalendars()` ruft die Kalenderliste ab und loggt diese.
        5. `syncBirthdays()` bricht ohne Berechtigungen sofort ab, bereinigt veraltete, doppelte oder ungültige Kalender-Accounts (`cleanCalendars`) und führt den vollständigen Synchronisationslauf für Geburtstage, Hochzeitstage (inkl. Ehepartner-Namen-Zusammenführung) und Namenstage via Batch-Operationen aus (inklusive Aufteilung in 400er-Batches).
    - **Bypass statischer Android-Konstanten:** Da unter JVM die statischen finalen Android-SDK-Felder wie `CalendarContract.Events.CONTENT_URI` standardmäßig `null` sind, wird `sun.misc.Unsafe` verwendet, um diese Felder via Speicher-Offset direkt mit Mocks zu belegen und NullPointerExceptions bei Batch-Operationen zuverlässig zu verhindern.
    - **Verifikation:** Erfolgreicher Durchlauf aller JVM-Tests (`.\gradlew testDebugUnitTest`).

245. **Implementierung des `goAsync()`-Musters im `NotificationActionReceiver` (Bug Fix & Reliability):**
    - **Problem:** Im `DONE`-Aktionspfad von `NotificationActionReceiver` wurde ein Datenbank-Schreibvorgang (`notificationRepository.markAsDone(pendingId)`) asynchron via `CoroutineScope(Dispatchers.IO).launch { ... }` gestartet, ohne `goAsync()` zu verwenden. Unter Android API 26+ führt dies bei Broadcast-Receivern zu einem extrem strikten Ausführungsfenster. Der Host-Prozess konnte beendet werden, bevor der Datenbank-Schreibvorgang beendet war, wodurch der Status "Erledigt" verloren ging.
    - **Lösung:** Verwendung des offiziellen `goAsync()`-Musters. Durch Aufruf von `goAsync()` vor dem Start der Coroutine und Aufruf von `pendingResult.finish()` im `finally`-Block der Coroutine wird dem System signalisiert, dass der Receiver im Hintergrund weiterarbeitet, bis der Schreibvorgang abgeschlossen ist.
    - **Dokumentation:** Die zugehörigen deutschen Kommentare im modifizierten Code-Abschnitt wurden ins Englische übersetzt, um die Projekt-Richtlinien bezüglich einer einheitlichen englischen Entwicklerdokumentation einzuhalten.

246. **Entkopplung der Debug-Kalenderprotokollierung vom Main-Thread (Performance & Stabilität / ANR-Fix):**
    - **Problem:** Die Methode `CalendarSyncRepositoryImpl.debugPrintAllCalendars()` blockierte über `runBlocking` den aufrufenden Thread, um Kalenderdaten synchron abzufragen. Da diese Methode in `CalendarViewModel.init` unter Debug-Builds direkt aufgerufen wurde, blockierte dies den Main-Thread beim App-Start vollständig und konnte auf langsameren Geräten zu ANRs führen.
    - **Lösung:** `CalendarSyncRepository.debugPrintAllCalendars()` wurde als `suspend fun` deklariert und die synchrone Blockierung (`runBlocking`) entfernt. Der Aufruf in `CalendarViewModel.init` wurde in ein `viewModelScope.launch` eingebettet, sodass die Protokollierung asynchron im Hintergrund erfolgt. Der entsprechende Unit-Test wurde auf `runTest` umgestellt.

247. **Explizites Offloading von CPU-intensiven Mappings auf `Dispatchers.Default` in `NotificationRepositoryImpl` (Performance / Richtlinie §2.7):**
    - **Problem:** Die Flows `allRules` und `settings` in [NotificationRepositoryImpl.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/NotificationRepositoryImpl.kt) führten Entity-zu-Domain-Objekt-Mappings via `.map { ... }` durch. Da `flowOn` fehlte, liefen diese CPU-intensiven Operationen auf dem Collector-Thread (typischerweise dem Main-Thread).
    - **Lösung:** Einfügen von `.flowOn(Dispatchers.Default)` unmittelbar nach dem `.map`-Operator und vor `.distinctUntilChanged()` bei beiden Flows, analog zum Muster in `ContactRepositoryImpl`. Import `kotlinx.coroutines.flow.flowOn` ergänzt.
    - **Verifikation:** Erfolgreicher Durchlauf aller JVM-Unit-Tests (`.\gradlew testDebugUnitTest`).

248. **Reorganisation der Kalender-Berechtigungsprüfung (UDF-Konformität & Testbarkeit):**
    - **CalendarUiState:** Hinzufügen von `hasCalendarPermission: Boolean = false` als Feld im UI-State, um den Berechtigungsstatus rein datengetrieben über UDF zu propagieren.
    - **CalendarViewModel:** Einführung eines privaten `MutableStateFlow<Boolean>` für den Berechtigungsstatus, welcher beim Starten des ViewModels initialisiert und über eine neue `checkPermissionStatus()` Methode aktualisiert wird. Dieser Flow wird mittels `combine` mit den Einstellungen zum `uiState` zusammengeführt. Die redundante öffentliche Funktion `hasCalendarPermissions()` wurde entfernt.
    - **CalendarSettingsScreen.kt:** Entfernung des lokalen Hilfszustands `remember { mutableStateOf(...) }` und direktes Auslesen von `uiState.hasCalendarPermission`. Einbindung von `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` zur automatischen Aktualisierung des Berechtigungszustands im ViewModel, sobald die App aus dem Hintergrund zurückkehrt.
    - **Unit-Tests:** Implementierung neuer JVM-Unit-Tests in `CalendarViewModelTest.kt`, um das korrekte Reflektieren der Berechtigungen im UI-State und die Aktualisierung über `checkPermissionStatus()` abzusichern.

249. **Verlagerung der Inaktivitäts-Logik in das HomeViewModel (Business-Logik & MVI):**
    - **HomeIntent.AppResumed:** Hinzufügen des neuen `data object AppResumed` zum `HomeIntent` sealed interface.
    - **Inaktivitäts-Reset:** Die 5-Minuten-Zeitberechnung zur Filterrücksetzung wurde aus der `MainActivity` in das `HomeViewModel` verlagert, da es sich um Business-Logik handelt.
    - **Property `lastInteractionTime`:** Wird nun als private Property in `HomeViewModel` gehalten. Sie wird bei jedem Intent (außer `AppResumed`) aktualisiert, um Nutzerinteraktionen zu tracken.
    - **AppResumed-Verarbeitung:** Bei Erhalt des `AppResumed` Intents wird geprüft, ob mehr als 5 Minuten seit `lastInteractionTime` vergangen sind. Wenn ja, wird `HomeIntent.ResetFilters` aufgerufen. Unabhängig davon wird `lastInteractionTime` aktualisiert.
    - **MainActivity-Bereinigung:** Die Activity hält nun keine Zeitstempel (`lastInteractionTime`) oder Zeitberechnungen mehr. `onUserInteraction()` wurde entfernt und der `ON_RESUME` Lifecycle-Observer ruft nun direkt `homeViewModel.onIntent(HomeIntent.AppResumed)` auf.
    - **Unit-Tests:** Hinzufügen von `onAppResumed_underFiveMinutes_doesNotResetFilters` und `onAppResumed_overFiveMinutes_resetsFilters` in `HomeViewModelTest.kt`.

250. **Refactoring der Onboarding-Berechtigungssteuerung (Code-Qualität & MVVM):**
    - **OnboardingUiState:** Neuer UI-Zustand für Onboarding mit den Feldern `hasContactPermission`, `hasNotificationPermission`, `hasCalendarPermission`, `isPersistentNotificationEnabled` und `currentPage`.
    - **OnboardingViewModel:** Hält nun `StateFlow<OnboardingUiState>` und verarbeitet MVI-Intents wie `RefreshPermissions`, `SetPersistentNotifications` und `SetCurrentPage`. Berechtigungsprüfungen wurden unter Verwendung des Application-Contexts in das ViewModel verlagert.
    - **PermissionHelper:** Neue Hilfsklasse `PermissionHelper` zur Prüfung von Rationale-Zuständen (`shouldShowRequestPermissionRationale`) ausgelagert, so dass die UI-Komponente keine Android-APIs direkt ansprechen muss.
    - **OnboardingScreen:** OnboardingContent ist nun zustandslos bezüglich Systemberechtigungen und liest keine Permission-Werte selbst aus. Es ruft Callbacks auf und bindet `collectAsStateWithLifecycle()` ein. System-Berechtigungen werden bei `ON_RESUME` automatisch aktualisiert.
    - **Unit-Tests:** Anpassung und Erweiterung von `OnboardingViewModelTest` zur vollständigen Absicherung des neuen MVI-Zustandsflusses und der Berechtigungssteuerung.

251. **Entkopplung der UI-Schicht von Data-Layer-Modellen (Architektur & Code-Qualität):**
    - **Verschiebung von `ContactLabels`:** Relozierung der Pseudo-Label-Identifier (`LABEL_NO_BIRTHDAY`, `LABEL_ANNIVERSARY`, `LABEL_NAME_DAY`) aus dem Data-Layer (`data.local.ContactLabels`) in den Domain-Layer (`domain.model.ContactLabels`). Dies verhindert unerwünschte Direktkopplungen der UI-Schicht und der Domain-UseCases an lokale Implementierungsdetails des Data-Layers.
    - **Rückwärtskompatibilität:** Um eventuelle andere Data-Layer-Klassen oder externe Verwendungen nicht zu beeinträchtigen, verbleibt eine deprecated Version von `ContactLabels` in `data.local`, die ihre Konstanten an das neue Domain-Modell delegiert.
    - **Anpassung der Imports:** Aktualisierung aller UI-Komponenten (`HomeContent.kt`, `BirthdayList.kt`, `LabelFilterBar.kt`, `LabelSettingsScreen.kt`, `LabelViewModel.kt`) und Domain-UseCases (`GetAvailableLabelsUseCase.kt`, `GetContactsUseCase.kt`, `GetCoupleSuggestionUseCase.kt`) sowie aller zugehörigen Unit-Tests auf den neuen Domain-Import.
    - **Dokumentation:** Aktualisierung der Projektstruktur in `PROJECT_STRUCTURE.md`.

252. **Typsicheres Refactoring des Event-Typs im Notification-Layer (Architektur & Typ-Sicherheit):**
    - **Verschiebung von `EventType`:** Die `EventType`-Enum wurde aus `ui/model/` nach `domain/model/` verlagert, da sie nun auch intensiv im Notification- und Use-Case-Layer genutzt wird. Alle Imports wurden angepasst.
    - **GetPendingNotificationsUseCase:** Die Rückgabeklasse `PendingNotificationEvent` verwendet nun das typ-sichere `EventType`-Enum anstelle von rohen String-Literalen.
    - **NotificationHelper:** Die Methode `showBirthdayNotification` akzeptiert nun `EventType` als Parameter, und alle internen Fallunterscheidungen wurden auf Enum-basierte `when`-Ausdrücke umgestellt.
    - **Snooze- & Reschedule-Logik:** `SnoozeWorker` parst nun beim Lesen aus der WorkData den `EVENT_TYPE` per `EventType.valueOf(...)` mit Fallback auf `EventType.BIRTHDAY`. `NotificationSchedulerImpl` und `NotificationActionReceiver` übergeben nun den passenden Enum-Namen beim Planen von Snooze-Workern.
    - **Unit-Tests:** Anpassung der Tests in `GetPendingNotificationsUseCaseTest` an die neuen Enum-Typen.

253. **Konsolidierung des MVI-Prinzips im NotificationViewModel (Architektur & Code-Qualität):**
    - **NotificationIntent:** Einführung des sealed interfaces `NotificationIntent` mit data classes für alle Benutzer-Aktionen (`SetEnabled`, `SetPersistent`, `SetOtherEventsEnabled`, `AddRule`, `UpdateRule`, `DeleteRule`).
    - **NotificationViewModel:** Hinzufügen von `onIntent(NotificationIntent)` als zentralen Dispatch-Punkt und Umwandlung der bestehenden öffentlichen Methoden in private Helferfunktionen.
    - **UI-Refactoring:** Anpassung von `NotificationSettingsScreen.kt` und `OtherEventsSettingsScreen.kt`, um alle Aktionen ausschließlich über `viewModel.onIntent` an das ViewModel zu senden.
    - **Unit-Tests:** Umstellung der Testfälle in `NotificationViewModelTest.kt` auf die Verwendung von `onIntent`.

254. **Entfernung redundanter @Singleton-Annotationen von Implementierungsklassen (Code-Qualität & Dependency Injection):**
    - **@Singleton-Bereinigung:** Entfernung der redundanten `@Singleton`-Annotationen auf den Implementierungs-Klassen (`BirthdayWidgetUpdater`, `CalendarSyncRepositoryImpl`, `ContactRepositoryImpl`, `NotificationRepositoryImpl`, `NotificationSchedulerImpl`, `SystemCalendarDataSourceImpl` und `TimeRepositoryImpl`), da diese ausschließlich über `@Binds` in `HelperBindingsModule` bereitgestellt werden, wo `@Singleton` bereits auf Methodenebene deklariert ist.
    - **Unbenutzte Imports:** Bereinigung der nicht mehr benötigten `import javax.inject.Singleton` Imports in den betroffenen Dateien.
    - **Erhalt direkter Singletons:** Die Klassen `GiftIdeaBackupManager` und `SystemContactDataSource` behalten ihre `@Singleton`-Annotationen, da sie ohne `@Binds` direkt injiziert werden.

255. **Threading-Optimierung in `ContactRepositoryImpl.syncContacts()` (Performance & Thread-Management):**
    - **Default-Dispatcher für CPU-Last:** Das CPU-intensive Reconcilement der Systemkontakte mit den Datenbank- und Nutzerdaten (`finalContacts`) sowie das Mapping auf DB-Entities (`finalEntities`) wurden in ein inneres `withContext(Dispatchers.Default)` ausgelagert.
    - **Erhalt des IO-Dispatchers:** Alle reinen I/O-Operationen wie das Auslesen der Datenquellen zu Beginn, die Synchronisation der Label-Konfigurationen, die Batch-Aktualisierung über Room-Transaktionen und die Kalendersynchronisation verbleiben auf `Dispatchers.IO`.

256. **Optimierung der UI-State-Stabilität durch `@Immutable`-Annotation (Performance & Compose-Compiler):**
    - **Annotationen:** Hinzufügen von `@Immutable` (aus `androidx.compose.runtime.Immutable`) zu den drei UI-State-Klassen [CalendarUiState.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/CalendarUiState.kt), [NotificationUiState.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/NotificationUiState.kt) und [ThemeUiState.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/ThemeUiState.kt). Dies ermöglicht es dem Compose-Compiler, garantierte Rekompositions-Skips in den Settings-Screens durchzuführen und unnötige Recompositionen zu verhindern.
    - **Stabilitäts-Audit:** Prüfung aller Felder dieser UI-State-Klassen auf Stabilität. In `NotificationUiState` wurde ein erklärender Kommentar für das Feld `notificationRules: List<NotificationRule>` ergänzt, da standardmäßige Kotlin-Collections (`List`) vom Compose-Compiler standardmäßig als instabil eingestuft werden, das @Immutable-Versprechen für diese read-only Data Class (Änderung ausschließlich über Kopien) jedoch eingehalten wird.

257. **Optimierung des Status-Bar-Inset-Flusses (UI-Qualität & Insets-Management):**
    - **Inkludierung von Status-Bar-Insets:** Hinzufügen von `WindowInsets.statusBars` zu den standardmäßigen `contentWindowInsets` von `AppResponsiveScaffold` in [ResponsiveLayout.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ResponsiveLayout.kt). Dadurch wird sichergestellt, dass Inset-Berechnungen bezüglich der Status-Bar nun standardmäßig vom Scaffold konsumiert werden.
    - **Custom TopBar-Erhalt:** Beibehaltung der manuellen Berechnung von `topPadding` in `HomeContent.kt`, da der standardmäßige `topBar`-Slot des Compose Material 3 Scaffolds Window-Insets nicht automatisch an seine Slot-Komponenten weitergibt. Dies verhindert Überlappungen mit der Status Bar ohne doppelten Abstand zu erzeugen.

258. **Eliminierung von Magic Numbers durch Dimensions- & Alpha-Tokens (Code-Qualität & Design System):**
    - **Dimensions-Erweiterung:** In [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt) wurden neue Design-Tokens definiert und dokumentiert: `FabBottomSpacing = 80.dp`, `AlphaContainerMedium = 0.3f`, `MaxWidthMedium = 600.dp`, `MaxWidthExpanded = 840.dp`, `SearchBarBorderWidth = 2.dp` und `SearchBarFocusedElevation = 4.dp`. Zudem wurde `IconSizeExtraSmall` von 14.dp auf 16.dp angepasst.
    - **Token-Ersetzung in UI-Dateien:** Ersetzung aller Magic Numbers durch entsprechende Tokens in [HomeContent.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt), [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt), [BirthdayStatus.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayStatus.kt), [StepItem.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/StepItem.kt), [SettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt), [GiftIdeaList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/GiftIdeaList.kt), [ResponsiveLayout.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ResponsiveLayout.kt), [SearchBar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/topbar/SearchBar.kt), [AppSwitch.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/AppSwitch.kt) und [InfoCard.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/InfoCard.kt).

259. **Vermeidung additiver Top-Paddings für flüssige Animationen (UI-Qualität & Richtlinien):**
    - **HomeContent.kt:** Entfernung des `top = SpacingSmall` Paddings bei der `SearchBar` zur Einhaltung der Projekt-Richtlinie, dass Abstände zwischen Elementen Bottom-Padding nutzen müssen, um Sprünge bei `AnimatedVisibility` zu verhindern. Der Abstand zur Status Bar wird sauber durch die übergeordnete `Column` mit `topPadding` gewährleistet.
    - **BirthdayList.kt / CoupleSuggestionBanner:** Entfernung des `top = SpacingSmall` Paddings am `CoupleSuggestionBanner`. Um den visuellen Abstand identisch zu halten, wird stattdessen das Bottom-Padding des Vorgängerelements (`LabelFilterBar`) dynamisch um `SpacingSmall` erhöht, falls ein Kopplungsvorschlag vorliegt.

260. **Effizientere Rekompositions-Skips durch Hinzufügen von Key-Parametern in LazyColumn (Performance & UI-Qualität):**
    - **BirthdayList.kt:** Hinzufügen des `key = { it }` Parameters im items()-Aufruf für die `BirthdayItemSkeleton`-Platzhalter.
    - **SettingsScreen.kt:** Hinzufügen des `key = { it.tab }` Parameters in den beiden `items(menuItems)`-Aufrufen für das Einstellungsmenü (sowohl im kompakten als auch im geteilten Tablet-Layout).

261. **Auslagerung der Kontaktberechtigungs-Prüfung in das HomeViewModel (Architektur & UI-Entkopplung):**
    - **Entkopplung der UI:** [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) wurde von direkten System-API-Aufrufen (`ContextCompat.checkSelfPermission` und `LaunchedEffect(contacts)`) befreit.
    - **Berechtigungsstatus im State:** Dem [HomeUiState.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/model/HomeUiState.kt) wurde das Feld `hasContactPermission: Boolean` hinzugefügt.
    - **Zentrale Steuerung:** Das [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModel.kt) ermittelt den Berechtigungsstatus über den injizierten `@ApplicationContext` und aktualisiert diesen bei der Initialisierung, bei Kontakt-Synchronisationen (`SyncContacts`) und beim App-Resume (`AppResumed`).
    - **Parameterübergabe:** `BirthdayList` erhält den Berechtigungsstatus nun als Parameter, der in [HomeContent.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt) aus dem `HomeUiState` übergeben wird.
    - **Test-Anpassungen:** Alle JUnit- und MockK/Mockito-Tests in [HomeViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelTest.kt), [HomeViewModelSearchTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelSearchTest.kt) und [HomeViewModelGiftIdeaTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelGiftIdeaTest.kt) wurden angepasst, um einen gemockten `Context` an den `HomeViewModel`-Constructor zu übergeben.

262. **Rekompositions-Optimierung in Geschenkideen-Liste (Performance & UI-Qualität):**
    - **Recomposition-Vermeidung:** In [GiftIdeaList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/GiftIdeaList.kt) wurden die Aufrufe von `GiftIdeaItem` in einen `key(idea.id)`-Block gewrappt. Dies verhindert die unnötige Rekomposition aller nachfolgenden Elemente, wenn eine Geschenkidee aus der Mitte der Liste gelöscht wird.

263. **Refactoring von HomeIntent-Objekten zu `data object` (Code-Qualität & Logging):**
    - **data object Migration:** Alle parameterlosen `HomeIntent`-Subtypen (`ResetFilters`, `TriggerScrollToTop`, `TriggerSearchFocus`, `ConsumeSearchFocus` und `ConsumeNewlyAddedIdeaId`) im `HomeIntent` sealed interface von [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModel.kt) wurden von `object` zu `data object` geändert, um eine korrekte `toString()`-Repräsentation beim MVI-Logging zu gewährleisten.

264. **Lokalisierung der HEX-Eingabestrings im ColorPickerDialog (Lokalisierung & UX):**
    - **Ressourcen:** Hinzufügen der neuen String-Ressourcen `color_picker_hex_label` ("HEX Code" / "HEX-Code") und `color_picker_hex_placeholder` ("e.g. FF5722" / "z.B. FF5722") in [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) und [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml).
    - **Einbindung:** Ersetzung der hardcodierten Strings für Label und Placeholder im `TextField` bzw. `OutlinedTextField` des [ColorPickerDialog.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ColorPickerDialog.kt) durch `stringResource` Verweise auf die neuen XML-Keys.

265. **Harmonisierung und Korrektur von Plural-Formatbezeichnern (Bug Fix & Code-Qualität):**
    - **Format-Specifier Vereinheitlichung:** Die Format-Specifier in den Plural-Blöcken `notif_title_days_named` und `notif_title_days_plural` wurden in [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) und [strings.xml (German)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) durchgehend auf nummerierte Bezeichner umgestellt (`%1$d`, `%2$s` bzw. `%2$d`).
    - **Fehlerbehebung:** Behebung potenzieller `MissingFormatArgumentException`-Fehler und falscher Argumentzuweisungen (wie fälschlicherweise formatierte IDs oder Indizes in den quantity="one" Varianten).

    - **Refactoring:** Bereinigung von `BackupViewModel` und `CalendarViewModel` durch Entkopplung von Repository-Direktaufrufen.
    - **Unit-Tests:** Erstellung neuer Testklassen (`ExportGiftIdeasUseCaseTest`, `ImportGiftIdeasUseCaseTest`, `SetCalendarSyncEnabledUseCaseTest`) sowie Anpassung bestehender ViewModel-Tests.

266. **Differenzierung des One-Case für verbleibende Tage (Bug Fix & Lokalisierung):**
    - **strings.xml / strings.xml (German):** Anpassung der Plural-Ressource `item_days_left`. Der `quantity="one"` Case wurde in beiden Sprachdateien abgeändert, sodass er keinen Formatbezeichner mehr enthält ("in 1 d." statt "in %d d." bzw. "in 1 T." statt "in %d T.").
    - **Code-Prüfung:** Der Code-Aufruf (`pluralStringResource` / `getQuantityString`) benötigt keine Anpassung, da ungenutzte Argumente ignoriert werden, wenn kein Format-Specifier im quantity="one" Item vorhanden ist.

267. **Optimierung der ProGuard/R8-Regeln für das lokale Datenpaket (Performance & Shrinking):**
    - **Entfernung der Wildcard-Regel:** Löschung der pauschalen Regel `-keep class com.heckmannch.birthdaybuddy.data.local.** { *; }`, welche jegliches Code-Shrinking im `data.local`-Paket verhinderte.
    - **Spezifische Keep-Regeln für TypeConverter:** Hinzufügen der Regel `-keep class com.heckmannch.birthdaybuddy.data.local.**Converter* { *; }` zum gezielten Schutz der Room TypeConverter (`Converters` und `GiftIdeaConverters`), da diese nicht durch Room-Consumer-Rules abgedeckt sind.
    - **Room Entity- und DAO-Optimierung:** `@Entity`- und `@Dao`-Klassen sowie Datenbank-Klassen verbleiben ohne explizite ProGuard-Einträge, da sie vollautomatisch über die eingebauten Room-Consumer-Rules geschützt und optimiert werden.

268. **Einführung namespace-qualifizierter Broadcast-Actions für Benachrichtigungen (Security & Code Quality):**
    - **NotificationActions:** Einführung eines zentralen Kotlin-Objekts `NotificationActions` mit namespace-qualifizierten Konstanten für die Broadcast-Actions `ACTION_SNOOZE` ("com.heckmannch.birthdaybuddy.action.SNOOZE"), `ACTION_DONE` ("com.heckmannch.birthdaybuddy.action.DONE") und `ACTION_DISMISSED` ("com.heckmannch.birthdaybuddy.action.DISMISSED").
    - **AndroidManifest.xml:** Registrierung der namespace-qualifizierten Action-Strings im `NotificationActionReceiver` anstelle generischer Strings zur Vermeidung von Namespace-Kollisionen.
    - **NotificationHelper & NotificationActionReceiver:** Umstellung der PendingIntent-Erstellung und der `onReceive`-Empfänger-Verzweigungen von hardcodierten, generischen Literalen auf die neuen `NotificationActions`-Konstanten und einen idiomatischen `when`-Block.

269. **Bereinigung ungenutzter Navigation-Compose Dependency (Build & APK-Größe):**
    - **Entfernung:** Die ungenutzte Dependency `androidx.navigation.compose` (Navigation 2.x) wurde aus [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) und die zugehörige Definition sowie Version `navigationCompose = "2.9.8"` aus [libs.versions.toml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml) entfernt, da die App vollständig auf Navigation 3 umgestellt ist.

270. **Refactoring der Worker-CoroutineScopes auf Hilt-ApplicationScope (Architektur & Stabilität):**
    - **ApplicationScope:** Einführung der Hilt-Qualifier-Annotation `@ApplicationScope` und Bereitstellung eines prozessweiten `CoroutineScope` mit `SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler` in [AppModule.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/di/AppModule.kt). Nicht behandelte Exceptions werden nun über diesen ExceptionHandler geloggt und stumm geschluckt.
    - **Injektion in Worker:** Refactoring von [NotificationWorker.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/notification/NotificationWorker.kt) und [BirthdayWidgetWorker.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidgetWorker.kt). Beide Klassen injizieren nun den `@ApplicationScope`-CoroutineScope über den Konstruktor, anstatt einen un-gecancelteten, stummen Companion-Object-Scope zu verwenden.
    - **KDoc-Dokumentation:** Ergänzung ausführlicher KDoc-Kommentare an beiden Worker-Klassen zur Dokumentation des Scopes und warum dieser die Worker-Ausführung überlebt (Post-Execution-Rescheduling).

271. **JVM-Unit-Tests für fünf Kern-Use-Cases (Test-Abdeckung & Code-Qualität):**
    - **Umfassende Test-Suite:** Erstellung neuer JVM-Unit-Tests für die fünf zuvor ungetesteten Use Cases in [usecase](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/):
        - [LinkAsCoupleUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/LinkAsCoupleUseCaseTest.kt): Validierung der Ehepartner-Verknüpfung sowie der Fehlerfortpflanzung.
        - [UnlinkCoupleUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/UnlinkCoupleUseCaseTest.kt): Validierung der Trennung verknüpfter Kontakte sowie der Fehlerfortpflanzung.
        - [IgnoreCoupleSuggestionUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/IgnoreCoupleSuggestionUseCaseTest.kt): Validierung der Ignorierung von Kopplungsvorschlägen sowie der Fehlerfortpflanzung.
        - [UpdateCalendarColorUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/UpdateCalendarColorUseCaseTest.kt): Absicherung der Farbaktualisierungen für Geburtstag/Jahrestag/Namenstag im Systemkalender und korrekter Ergebnisrückgaben.
        - [SyncCalendarUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/domain/usecase/SyncCalendarUseCaseTest.kt): Absicherung der Synchronisationslogik in Abhängigkeit von den Benutzereinstellungen (`calendarSyncEnabled` true/false).
    - **Methodik:** Verwendung von `MockK` zur Mocking-Injektion der Repository-Abhängigkeiten (`ContactRepository`, `CalendarSyncRepository`, `NotificationRepository`) und Einbindung der `MainDispatcherRule` zur Coroutine-Ausführung unter `runTest`.

272. **JVM-Unit-Tests für Notification- & Widget-Worker (Test-Abdeckung & Stabilität):**
    - **BirthdayWidgetWorker-Refactoring:** `calculateDelayUntilMidnight` wurde von `private` zu `internal` geändert und mit `@VisibleForTesting` annotiert. Es wurde ein optionaler `now: LocalDateTime` Parameter ergänzt, um eine deterministische Testbarkeit für verschiedene Tageszeiten (Mitternacht, Mittag, kurz vor Mitternacht) zu ermöglichen.
    - **BirthdayWidgetWorkerTest:** Erstellung der Testdatei [BirthdayWidgetWorkerTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidgetWorkerTest.kt) zur Validierung der Berechnungslogik von `calculateDelayUntilMidnight`.
    - **SnoozeWorkerTest:** Erstellung der Testdatei [SnoozeWorkerTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/notification/SnoozeWorkerTest.kt) zur Validierung des Lookup-Key-Parsings (Entfernung von Event-Präfixen), des Filterns betroffener Kontakte und der korrekten Event-Type-Ermittlung.
    - **NotificationWorkerTest:** Erstellung der Testdatei [NotificationWorkerTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/notification/NotificationWorkerTest.kt) zur Validierung der `scheduleNext`-Logik (Berechnung von Delays bis zum nächsten Evaluierungszeitpunkt und korrektes Enqueueing bzw. Stornieren in `WorkManager` mittels MockK-Static/Companion-Mocking).
    - **Validierung:** Alle 220 JVM-Unit-Tests laufen im Gradle-Task `testDebugUnitTest` erfolgreich und fehlerfrei durch.

273. **JVM-Unit-Tests für vier Einstellungs-ViewModels (Test-Abdeckung & Code-Qualität):**
    - **Umfassende Test-Suite:** Erstellung neuer JVM-Unit-Tests für die vier zuvor ungetesteten ViewModels unter [viewmodel](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/):
        - [BackupViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/BackupViewModelTest.kt): Validierung des Exports und Imports von Geschenkideen über `ContentResolver`-Streams sowie Absicherung von Fehlerfällen und Dispatcher-Eigenschaften.
        - [CalendarViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/CalendarViewModelTest.kt): Validierung der Kalendereinstellungs-Synchronisation, Berechtigungsprüfung sowie Farbaktualisierungen.
        - [LabelViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/LabelViewModelTest.kt): Validierung der dynamischen Konsolidierung der Filter- und Pseudo-Labels bei fehlenden Geburtstagsdaten sowie Speichern von Label-Konfigurationen.
        - [ThemeViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/viewmodel/ThemeViewModelTest.kt): Validierung des Theme-Zustandsflusses (Dark Mode, AMOLED, Akzentfarbe) und dessen Weitergabe an die Datenbank.
    - **Bereinigung:** Vollständiges Entfernen der redundanten/veralteten Testklassen unter dem `ui.screens.settings` Verzeichnisbaum.
    - **Methodik:** Verwendung von `Mockito` zur Mocking-Injektion der Repository- und Use-Case-Abhängigkeiten sowie Einbindung der `MainDispatcherRule` zur Coroutine-Ausführung unter `runTest`.

274. **Einführung der Clock-Abstraktion in HomeViewModel & Hilt-Bindung (Architektur & Testbarkeit):**
    - **Clock-Interface:** Erstellung des `Clock`-Interfaces und der Hilt-bereiten Standardimplementierung `SystemClock` unter `com.heckmannch.birthdaybuddy.util` zur Kapselung des Systemzeit-Zugriffs.
    - **Hilt-Bindung:** Einbindung von `SystemClock` via `@Binds` in `HelperBindingsModule` zur Auflösung als `Clock`.
    - **HomeViewModel-Refactoring:** Injektion der `Clock` via `@Inject constructor` und Ersetzung aller direkten `System.currentTimeMillis()` Aufrufe durch `clock.currentTimeMillis()`. Vollständiges Entfernen des redundanten `@VisibleForTesting internal var currentTimeProvider`.
    - **Test-Abdeckung:** Erstellung einer `FakeClock` in [HomeViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelTest.kt) und Anpassung der restlichen Testklassen ([HomeViewModelSearchTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelSearchTest.kt), [HomeViewModelGiftIdeaTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelGiftIdeaTest.kt)), um die injizierte `Clock` zu verwenden. Hinzufügen von zwei neuen Testfällen zur präzisen Absicherung des 800ms Lade-Throttles bei der Kontaktsynchronisation unter Nutzung des virtuellen Coroutine-TestSchedulers (`runCurrent()`-Flusssteuerung).

275. **Ergänzung fehlender KDocs auf öffentlichen Klassen und Funktionen (Dokumentation & Code-Qualität):**
    - **Klassen-Level KDocs:** Hinzufügen von detaillierten KDoc-Kommentaren auf Klassen-Ebene in `NotificationRepositoryImpl`, `ContactRepositoryImpl` und `CalendarSyncRepositoryImpl`. Sie beschreiben jeweils die Zuständigkeit der Repositories, deren Aufbau, und die getroffenen Entwurfsentscheidungen (z.B. Dispatcher-Offloading, Mutex-Sperren, Sync-Adapter-Delegation).
    - **NotificationViewModel KDocs:** Dokumentation aller `setX()`-Methoden (`setNotificationsEnabled`, `setPersistentNotifications` und `setOtherEventsEnabled`), inklusive Erklärung der Parameter, Rückgabewerte und Design-Hintergründe.
    - **DI-Module KDocs:** KDoc-Ergänzung für alle `@Provides`-Methoden in `AppModule` und alle `@Binds`-Methoden in `HelperBindingsModule` zur besseren Nachvollziehbarkeit des Hilt-Abhängigkeitsgraphen.

276. **Behebung der Clean-Architecture-Verletzung im ContactMapper (Architektur & Code-Qualität):**
    - **Einführung von ContactUiMapper:** Erstellung der neuen UI-Klasse [ContactUiMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/mapper/ContactUiMapper.kt) zur Konvertierung von Domain-Modellen (`Contact`) in UI-Modelle (`ContactUiModel`). Die visuelle Logik für `BirthdayTier` sowie die couple-pairing und multi-event Flat-Listing Logiken wurden vollständig hierher verschoben.
    - **Dekopplung von ContactMapper:** Der Data-Mapper [ContactMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapper.kt) wurde so vereinfacht, dass er nun ausschließlich `ContactEntity` -> `Contact` (Domain) konvertiert und keine Abhängigkeiten mehr zum UI-Layer besitzt (`ui.model.*` Imports gelöscht).
    - **Bereinigung von GetContactsUseCase:** Der Use Case [GetContactsUseCase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/domain/usecase/GetContactsUseCase.kt) gibt nun einen reinen Domain-Modell-Fluss (`Flow<List<Contact>>`) zurück. Die Sortier- und UI-Kopplungslogiken wurden entfernt.
    - **Anpassung des HomeViewModel:** [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModel.kt) injiziert nun den `ContactUiMapper` und transformiert den gefilterten Domain-Fluss asynchron auf `Dispatchers.Default` in `uiContacts` (`Flow<List<ContactUiModel>>`).
    - **Testabdeckung:** Erstellung von [ContactUiMapperTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/mapper/ContactUiMapperTest.kt) (Mapping, Formatting, Sorting, Tier-Logik). Bereinigung von [ContactMapperTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/data/mapper/ContactMapperTest.kt), Löschung der redundanten `ContactMapperTierTest.kt` und Aktualisierung von [GetContactsUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/domain/usecase/GetContactsUseCaseTest.kt) sowie [HomeViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/ui/screens/home/HomeViewModelTest.kt) und assoziierten ViewModel-Tests.

277. **Behebung der Clean-Architecture-Verletzung bei Paar-Vorschlägen (Architektur & Code-Qualität):**
    - **Einführung von CoupleSuggestion:** Erstellung des neuen Domain-Modells [CoupleSuggestion.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/domain/model/CoupleSuggestion.kt) zur Repräsentation eines Paar-Vorschlags frei von Android- oder UI-Abhängigkeiten.
    - **Entkopplung von GetCoupleSuggestionUseCase:** Aktualisierung von [GetCoupleSuggestionUseCase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/domain/usecase/GetCoupleSuggestionUseCase.kt), so dass er nun einen Fluss vom Typ `Flow<CoupleSuggestion?>` zurückgibt und keine UI-Modellabhängigkeiten mehr besitzt.
    - **Einführung von CoupleSuggestionUiMapper:** Erstellung der neuen UI-Klasse [CoupleSuggestionUiMapper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/mapper/CoupleSuggestionUiMapper.kt) unter `ui.mapper` für die Konvertierung von `CoupleSuggestion` in `CoupleSuggestionUiModel`.
    - **Anpassung des HomeViewModel:** [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModel.kt) injiziert nun den neuen `CoupleSuggestionUiMapper` und mappt den Fluss der Paarkopplungsvorschläge asynchron auf `Dispatchers.Default`.
    - **Testabdeckung:** Anpassung der ViewModel-Tests ([HomeViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelTest.kt), [HomeViewModelSearchTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelSearchTest.kt), [HomeViewModelGiftIdeaTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelGiftIdeaTest.kt)) und des UseCase-Tests [GetCoupleSuggestionUseCaseTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/domain/usecase/GetCoupleSuggestionUseCaseTest.kt) an die neue Signatur und Parameterstruktur.

278. **Lokalisierung des Bullet-Points in der Datenschutzerklärung (i18n & Code Quality):**
    - **String-Ressource:** Hinzufügen des neuen String-Keys `bullet_point` mit dem Wert `"• "` in [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) (EN) und [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) (DE), um das Bullet-Zeichen übersetzbar zu machen.
    - **Ersetzung:** Ersetzung des hardcoded Strings `Text("• ")` durch `Text(stringResource(R.string.bullet_point))` in [PrivacyPolicyScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/about/PrivacyPolicyScreen.kt).

279. **Refactoring hardcodierter Alpha-Floats auf standardisierte Design-Tokens (Richtlinie & UI-Konsistenz):**
    - **Ersetzung in Komponenten und Screens:** Hardcodierte Float-Werte für Opazitäten (`0.5f`, `0.7f`, `0.3f`, `0.9f`, `0.4f`) wurden in folgenden Dateien durch die passenden semantischen Tokens aus `ui/theme/Dimensions.kt` (`AlphaEmphasisLow`, `AlphaEmphasisMedium`, `AlphaContainerMedium`, `AlphaOnboardingCard`, `AlphaOnboardingCalendarDisabled`) ersetzt:
        - [ResponsiveLayout.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/components/ResponsiveLayout.kt) (0.3f -> `AlphaContainerMedium`)
        - [ContactsIllustration.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/illustrations/ContactsIllustration.kt) (0.4f -> `AlphaOnboardingCalendarDisabled`)
        - [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) (0.4f -> `AlphaOnboardingCalendarDisabled`)
        - [BirthdayQuotePlaceholder.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayQuotePlaceholder.kt) (0.4f -> `AlphaOnboardingCalendarDisabled`, 0.5f -> `AlphaEmphasisLow`, 0.7f -> `AlphaEmphasisMedium`)
        - [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt) (0.9f -> `AlphaOnboardingCard`)
        - [BackupContent.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/backup/components/BackupContent.kt) (0.5f -> `AlphaEmphasisLow`)
        - [CalendarSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/calendar/CalendarSettingsScreen.kt) (0.5f -> `AlphaEmphasisLow`, 0.3f -> `AlphaContainerMedium`)
        - [NotificationSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/notifications/NotificationSettingsScreen.kt) (0.5f -> `AlphaEmphasisLow`)
        - [ThemeSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/theme/ThemeSettingsScreen.kt) (0.5f -> `AlphaEmphasisLow`)
        - [BirthdayDatePickerDialog.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayDatePickerDialog.kt) (0.4f -> `AlphaOnboardingCalendarDisabled`)
    - **Import-Ergänzungen:** In allen betroffenen Dateien wurden die entsprechenden Importe für `com.heckmannch.birthdaybuddy.ui.theme.*` hinzugefügt.

280. **Absicherung der Datenbank-Migrationen vor stillem Exception-Schlucken (Datenbank-Sicherheit / Richtlinie §1.15):**
    - **AppDatabase.kt:** Überarbeitung der Catch-Blöcke in [AppDatabase.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/local/AppDatabase.kt) für `MIGRATION_5_6` und `MIGRATION_6_7`. Anstatt Fehler stumm zu schlucken, führen wir nun bei Fehlern den Tabellen-Rollback aus und werfen danach eine `RuntimeException` mit der Original-Exception als Cause. Dies stellt sicher, dass Room Transaktionen abbricht und fehlerhafte Migrationen sauber crasht.
    - **Dokumentation:** Hinzufügen von detaillierten KDoc-Kommentaren in englischer Sprache bei beiden Migrationen zur Erläuterung der Fehlerbehandlungs- und Rollback-Strategie.

281. **Hilt-Konstruktor-Injektion für CoroutineDispatcher & Decoupling von BackupViewModel (Architecture & Code Quality):**
    - **IoDispatcher-Qualifier:** Einführung der `@IoDispatcher` Hilt-Qualifier-Annotation und Bereitstellung eines entsprechenden Providers (`CoroutineDispatcher`) in [AppModule.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/di/AppModule.kt) returning `Dispatchers.IO`.
    - **ContentResolver-Provider:** Bereitstellung von `ContentResolver` im `AppModule` zur direkten Hilt-Constructor-Injektion in Repositories.
    - **Decoupling von BackupViewModel:** Das `BackupViewModel` wurde vollständig von Android-Framework-Abhängigkeiten wie `ContentResolver` entkoppelt. Das Auslesen und Beschreiben von Dateien via Streams (`openInputStream`/`openOutputStream`) wurde in das `ContactRepository` / `ContactRepositoryImpl` verlagert.
    - **MVI-Refactoring:** Umstellung aller öffentlichen ViewModel-Methoden auf MVI-Intents. Die Composable `BackupScreen` sendet nun `BackupIntent.ExportBackup` und `BackupIntent.ImportBackup` über den zentralen `onIntent(BackupIntent)` Handler.
    - **Hilt-Konstruktor-Injektion:** Injizieren des `@IoDispatcher CoroutineDispatcher` als `private val` in `BackupViewModel` sowie von `ContentResolver` in `ContactRepositoryImpl`.
    - **Test-Updates:** Anpassung der Unit-Tests (`BackupViewModelTest.kt`, `ExportGiftIdeasUseCaseTest.kt`, `ImportGiftIdeasUseCaseTest.kt` und `ContactRepositoryImplTest.kt`) an die neuen Dispatcher-Injektionen und I/O-Signaturen.

282. **Entkopplung der ViewModels von Android Permission APIs (Architektur & Code-Qualität):**
    - **PermissionChecker:** Erstellung des neuen Domänen-Interfaces [PermissionChecker.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/domain/permission/PermissionChecker.kt) zur Definition von Berechtigungsabfragen.
    - **AndroidPermissionChecker:** Erstellung der plattformspezifischen Implementierung [AndroidPermissionChecker.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/permission/AndroidPermissionChecker.kt) unter Verwendung von `ContextCompat.checkSelfPermission` und `@ApplicationContext Context`.
    - **HelperBindingsModule:** Registrierung des Bindings via `@Binds` und `@Singleton` in [HelperBindingsModule.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/di/HelperBindingsModule.kt).
    - **Refactoring HomeViewModel & OnboardingViewModel:** Injektion des `PermissionChecker` in [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModel.kt) und [OnboardingViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/OnboardingViewModel.kt) sowie vollständiges Entfernen des Android `Context` Parameters.
    - **Unit-Tests:** Umstellung der ViewModel-Tests ([HomeViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelTest.kt), [HomeViewModelSearchTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelSearchTest.kt), [HomeViewModelGiftIdeaTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeViewModelGiftIdeaTest.kt) und [OnboardingViewModelTest.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/test/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/OnboardingViewModelTest.kt)) auf gemockte `PermissionChecker`-Aufrufe zur Vermeidung von direkten Android-Mocks im JVM-Kontext.

283. **Konsolidierung des MVI-Prinzips in Onboarding-, Label-, Calendar- und ThemeViewModel (Architektur & Code-Qualität):**
    - **MVI-Intents & onIntent:** Einführung von MVI-Intents (`OnboardingIntent.CompleteOnboarding`, `LabelIntent`, `CalendarIntent`, `ThemeIntent`) mit einem zentralen `onIntent()` Handler in [OnboardingViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/OnboardingViewModel.kt), [LabelViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/labels/LabelViewModel.kt), [CalendarViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/calendar/CalendarViewModel.kt) und [ThemeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/theme/ThemeViewModel.kt).
    - **Kapselung:** Änderung der Sichtbarkeit aller bisherigen öffentlichen Aktionsmethoden der ViewModels auf `private`, um den MVI-Vertrag sauber zu kapseln.
    - **UI-Anpassungen:** Umstellung der UI-Screens ([OnboardingScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/onboarding/OnboardingScreen.kt), [LabelSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/labels/LabelSettingsScreen.kt), [CalendarSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/calendar/CalendarSettingsScreen.kt) und [ThemeSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/theme/ThemeSettingsScreen.kt)) auf die Verwendung von `viewModel.onIntent(intent)`.
    - **Test-Updates:** Anpassung der zugehörigen JVM-Unit-Tests (`OnboardingViewModelTest.kt`, `LabelViewModelTest.kt`, `CalendarViewModelTest.kt` und `ThemeViewModelTest.kt`), um die Aktionen über `onIntent` zu prüfen.

284. **Grammatikalische Korrektur für verknüpfte Hochzeitstage (I18n, Benachrichtigungen & Kalender-Sync):**
    - **Problem:** Bei verknüpften Ehepartnern wurde in der Hochzeitstag-Benachrichtigung grammatikalisch inkorrekt das Verb "hat" statt "haben" verwendet (z.B. "Max & Erika Mustermann hat heute Hochzeitstag!").
    - **Lösung:** Einführung neuer, paar-spezifischer String- und Plural-Ressourcen (`notif_title_today_anniversary_couple`, `notif_title_tomorrow_anniversary_couple`, `notif_title_week_anniversary_couple`, `notif_title_days_anniversary_couple`, `calendar_event_anniversary_title_couple`) in [strings.xml (DE)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) und [strings.xml (EN)](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml).
    - **NotificationHelper:** Anpassung in [NotificationHelper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/notification/NotificationHelper.kt), um bei `isCoupleAnniversary = true` automatisch die Couple-Ressourcen mit dem Plural-Verb "haben" zu laden, während für Einzelkontakte weiterhin "hat" verwendet wird.
    - **Calendar Sync:** Anpassung in [CalendarSyncRepositoryImpl.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepositoryImpl.kt), um bei vorhandenem Ehepartner ebenfalls den korrekten Titel (`calendar_event_anniversary_title_couple`) im lokalen Systemkalender zu verwenden.

285. **Animation für Suchleisten-Placeholder (UI & UX):**
    - **AnimatedContent:** Hinzufügen einer eleganten Gleit- und Ausblend-Animation (M3 Standard) für den Suchleisten-Placeholder in [SearchBar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/topbar/SearchBar.kt).
    - **Detail:** Beim Übergang zwischen der Anwendungsbezeichnung („BirthdayBuddy“) und dem Suchhinweis („Kontakte suchen...“) gleitet der alte Text nach oben weg und blendet aus, während der neue Text von unten herein gleitet und einblendet.

286. **Behebung mehrfach deklarierter Test-Abhängigkeiten in `app/build.gradle.kts` (Code Quality & Build-Optimierung):**
    - **Problem:** Die Test-Abhängigkeiten für Coroutines-Test, Truth und Mockito wurden redundant sowohl unter `testImplementation` als auch unter `androidTestImplementation` in [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) aufgeführt, was zu Warnungen über doppelte Deklarationen führte.
    - **Lösung:** Entfernung der redundanten Duplikate (`kotlinx.coroutines.test`, `truth`, `mockito.core`, `mockito.kotlin`) aus dem `androidTestImplementation` Block und stattdessen Definition einer `configurations`-Vererbung (`androidTestImplementation.get().extendsFrom(testImplementation.get())`). Dadurch erben die Android-Instrumentierungstests diese Kern-Testbibliotheken automatisch aus dem JVM-Test-Scope.
    - **Kompilierungs-Fix:** Behebung von Compilerfehlern in den Instrumentierungstests (`ContactRepositoryCoupleLinkTest.kt` und `ContactRepositoryGiftIdeaTest.kt`), bei denen der `contentResolver`-Parameter beim Instanziieren von `ContactRepositoryImpl` fehlte.

287. **Standardisierung der UI-Maße in `BirthdayWidget` (Code Quality & Design System):**
    - **Dimensions-Token:** Hinzufügen von `WidgetCornerRadius`, `WidgetItemMinHeight` und `SpacingTiny` zu [Dimensions.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt) zur Vermeidung von Magic Numbers.
    - **Refactoring:** Migration von [BirthdayWidget.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/widget/BirthdayWidget.kt) auf die Verwendung dieser neuen Tokens sowie der Standard-Spacing-Tokens (`SpacingSmall`, `SpacingExtraSmall`, `SpacingMedium`) für Paddings, Höhen und Eckenradien.
