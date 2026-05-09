# Project Status: BirthdayBuddy 2

> **TL;DR für neue Sessions:** Moderne M3 Geburtstags-App. Android-Kontakte sind die Single-Source-of-Truth. Migration auf `LOOKUP_KEY` & `localId` abgeschlossen (zukunftssicher). High-Performance Sync (Batch-Labels), hybride Filter-Logik (Filterleiste nur bei Bedarf), präzises Widget-Update (Mitternacht via WorkManager). Architektur ist sauber, modular und performant (Room v6, Compose BOM 2026.04).

## 🚀 Aktueller Stand
Die App befindet sich gerade in einer umfassenden Refactoring-Phase (V3), um die Skalierbarkeit und Testbarkeit zu erhöhen. Fokus liegt auf der Trennung von Business-Logik und UI-State durch das Repository-Pattern und moderner Lifecycle-Integration.

**WICHTIG:** Bei jeder strukturellen Änderung (neue Dateien, Refactoring) muss die `PROJECT_STRUCTURE.md` zwingend aktualisiert werden, um die Dokumentation konsistent zu halten.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `ui.screens.settings.labels`, `ui.screens.settings.notifications`, `viewmodel`, `database`, `widget`). Alles, was zu einem Feature gehört (UI, Worker, Preferences), liegt im entsprechenden Package. Komplexe Screens nutzen ein `components` Sub-Package zur Modularisierung.
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth für den UI-State; nutzt `SharedFlow` für UI-Events.
- **Data:** 
    - `ContactUiModel` (@Immutable) für performante UI-Updates; robuste Datums-Berechnungen (Schaltjahr-sicher).
    - **Single Source of Truth:** Das Android-System (Kontakte-Provider) bleibt die einzige Quelle für Personendaten. Die App pflegt keine eigenen Kontaktdaten, sondern reichert diese nur für Filterzwecke (Labels) an.
    - **Stabile Identifikatoren:** Migration von der numerischen System-ID auf den `LOOKUP_KEY` als Primärschlüssel für UI-Stabilität und Robustheit bei Geräte-Wechseln oder System-Syncs. Interner Room-Key (`localId`) sorgt für performante Relationen.
- **Dependency Injection:** Vollständige Integration von Hilt für ViewModels, Repositories und WorkManager.
- **Navigation:** Jetpack Navigation mit Material 3 Shared-Axis-Animationen und Hilt-Integration.

## ✨ Key Features
1. **HomeScreen:** 
   - High-End Fast-Scrollbar (Google Photos Style): 48dp Touch-Area, vorausschauende Monats-Bubble, präzise 0-100% Logik.
   - Intelligente UX: Automatisches Schließen der Tastatur beim Scrollen; Gmail-Style Suche.
   - FAB mit Doppelfunktion (Scroll-to-Top / Kontakt hinzufügen).
2. **Widget (Jetpack Glance):**
   - Dynamisches Layout: Berechnet Item-Anzahl basierend auf Höhe; reaktive Updates bei Datenbank-Änderungen.
   - Click-to-App: Automatischer Scroll-to-Top beim Öffnen via Widget.
3. **Robustheit:**
   - Schaltjahr-Support (29. Februar) ohne Abstürze.
   - Fehlertolerante System-Synchronisation und Room-Migration (`destructiveMigration`).

## 📦 Abhängigkeiten & Polish
- **Core:** Compose BOM (2026.04.01), Room (2.8.4), Glance (1.1.1).
- **Branding:** SVG-Icon (50% Safe-Scale), weißer Hintergrund, `DayNight` XML-Theme gegen Start-Flackern.

## 📜 Historie der Änderungen (Changelog)
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

## 🎯 Kommende Aufgaben (Backlog)
- **Erweiterte Benachrichtigungen:** Einstellbare Erinnerungen (1 Tag vorher, am Tag selbst) via WorkManager.
