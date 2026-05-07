# Project Status: BirthdayBuddy 2

> **TL;DR für neue Sessions:** Moderne M3 Geburtstags-App. Android-Kontakte sind die Single-Source-of-Truth. Migration auf `LOOKUP_KEY` & `localId` abgeschlossen (zukunftssicher). High-Performance Sync (Batch-Labels), hybride Filter-Logik (Filterleiste nur bei Bedarf), präzises Widget-Update (Mitternacht via WorkManager). Architektur ist sauber, modular und performant (Room v6, Compose BOM 2026.04).

## 🚀 Aktueller Stand
...
Die App ist eine moderne Geburtstags-Verwaltung, die Kontakte aus dem Android-System synchronisiert, filtert und in einem Widget anzeigt. Sie folgt Material 3 Richtlinien und Clean Architecture Prinzipien.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `ui.screens.settings.labels`, `ui.screens.settings.notifications`, `viewmodel`, `database`, `widget`). Alles, was zu einem Feature gehört (UI, Worker, Preferences), liegt im entsprechenden Package.
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth für den UI-State; nutzt `SharedFlow` für UI-Events.
- **Data:** 
    - `ContactUiModel` (@Immutable) für performante UI-Updates; robuste Datums-Berechnungen (Schaltjahr-sicher).
    - **Single Source of Truth:** Das Android-System (Kontakte-Provider) bleibt die einzige Quelle für Personendaten. Die App pflegt keine eigenen Kontaktdaten, sondern reichert diese nur für Filterzwecke (Labels) an.
    - **Stabile Identifikatoren:** Migration von der numerischen System-ID auf den `LOOKUP_KEY` als Primärschlüssel für UI-Stabilität und Robustheit bei Geräte-Wechseln oder System-Syncs. Interner Room-Key (`localId`) sorgt für performante Relationen.
- **Navigation:** Jetpack Navigation mit Material 3 Shared-Axis-Animationen.

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

## 🎯 Kommende Aufgaben (Backlog)
- **Quick-Actions:** Implementierung von Swipe-Gesten in der Liste (z.B. Swipe-to-Ignore).
- **Label-Visualisierung:** Optionale Farben für Labels zur besseren Sichtbarkeit.
- **Benachrichtigungen:** Einstellbare Erinnerungen (1 Tag vorher, am Tag selbst) via WorkManager.
