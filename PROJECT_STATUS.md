# Project Status: BirthdayBuddy 2

## 🚀 Aktueller Stand
Die App ist eine moderne Geburtstags-Verwaltung, die Kontakte aus dem Android-System synchronisiert, filtert und in einem Widget anzeigt. Sie folgt Material 3 Richtlinien und Clean Architecture Prinzipien.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `viewmodel`, `database`, `widget`).
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth; nutzt `SharedFlow` für UI-Events.
- **Data:** `ContactUiModel` (@Immutable) für performante UI-Updates; robuste Datums-Berechnungen (Schaltjahr-sicher).
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
