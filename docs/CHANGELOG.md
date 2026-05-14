# Changelog: BirthdayBuddy

Alle signifikanten Änderungen am Projekt werden hier dokumentiert.

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
    - **Code Quality:** Umfassende Bereinigung der "Problems"-Ansicht: Korrektur von Lokalisierungs-Warnungen, Optimierung von Lambda-Syntax, Einführung von Named-Arguments für Booleans und Behebung ungenutzter Exception-Parameter via `Log.e` oder `_` Placeholder.
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
    - **Code Quality:** Systematische Bereinigung von IDE-Warnungen, Einführung von Trailing Commas und Optimierung der Flow-Ketten im Widget.
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


