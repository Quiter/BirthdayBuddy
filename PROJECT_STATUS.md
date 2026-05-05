# Project Status: BirthdayBuddy 2

## 🚀 Aktueller Stand
Die App ist eine moderne Geburtstags-Verwaltung, die Kontakte aus dem Android-System synchronisiert, filtert und in einem Widget anzeigt. Sie folgt Material 3 Richtlinien und Clean Architecture Prinzipien.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `ui.screens.settings`, `viewmodel`, `database`, `widget`).
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth; nutzt `SharedFlow` für UI-Events (z.B. Scroll-to-Top).
- **Data:** `ContactUiModel` (@Immutable) für performante UI-Updates; alle Berechnungen im ViewModel.
- **Navigation:** Jetpack Navigation mit Shared-Axis-Z-Animationen (Scale + Fade).

## ✨ Key Features
1. **HomeScreen:** 
   - Gmail-Style Suche mit animiertem Placeholder.
   - High-End Fast-Scrollbar (Google Photos Style): 48dp Touch-Area, dynamische Breite beim Ziehen (6dp -> 12dp), vorausschauende Monats-Bubble.
   - Präzise Scroll-Logik: Slider erreicht immer 100% (Anschlag), auch bei gefilterten Listen.
   - FAB mit Doppelfunktion: Scroll-to-Top (wenn gescrollt) / System-Kontakt hinzufügen (wenn oben).
2. **Geburtstags-Logik:**
   - Highlights: Regenbogen (Kids <= 10), Gold (Runde ab 20), Silber (Standard heute).
3. **Widget (Jetpack Glance):**
   - Dynamisches Layout (`SizeMode.Exact`): Berechnet Item-Anzahl basierend auf verfügbarer Höhe; gleichmäßige Verteilung via `defaultWeight`.
   - Click-to-App: Klick auf das Widget öffnet die App und triggert via Intent-Extra (`SCROLL_TO_TOP`) einen automatischen Scroll nach oben.

## 📦 Abhängigkeiten & Polish
- **Core:** Compose BOM (2026.04.01), Room (2.8.4), Glance (1.1.1), WorkManager.
- **Icon:** SVG-Asset (50% Scale) auf weißem Hintergrund; adaptive Icons korrekt konfiguriert.
- **UX:** `Theme.Material3.DayNight` in XML verhindert weißes Flackern beim App-Start (besonders via Widget).

## 📜 Historie der Änderungen (Changelog)
1. **Architektur & Performance:** Initiales Setup mit `ContactUiModel` und ViewModel-Logik-Migration.
2. **Navigation:** Einführung von Shared-Axis Übergängen.
3. **Scrollbar-Evolution:** Von einer einfachen Leiste zu einer hochpräzisen Fast-Scrollbar mit `LocalDensity`-Unterstützung und index-basierter Monats-Vorschau beim Ziehen. Fix für 0-100% Reach in gefilterten Zuständen.
4. **Widget-Rebirth:** Kompletter Umbau von `LazyColumn` auf ein dynamisch berechnetes Spalten-Layout zur Vermeidung von Scrollbalken im Widget.
5. **Deep Linking:** Implementierung der `SCROLL_TO_TOP` Logik via `onNewIntent` in der `MainActivity` zur nahtlosen Widget-App-Interaktion.
6. **Branding & Launch:** SVG-Icon Integration und XML-Theme-Optimierung für flüssige Übergänge im Dark Mode.
7. **Architectural Refactoring & UX Polish:**
   - **Modularization:** Extracted `FastScrollbar` and `BirthdayList` into separate files for better maintainability and cleaner `HomeScreen` structure.
   - **UX Enhancements:** Implemented auto-keyboard dismissal on scroll and tap-to-dismiss focus logic for the search bar.
   - **Performance:** Optimized state management using `mutableFloatStateOf` and stabilized gesture detection via `rememberUpdatedState` in the scrollbar.
