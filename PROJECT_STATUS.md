# Project Status: BirthdayBuddy 2

## 🚀 Aktueller Stand
Die App ist eine moderne Geburtstags-Verwaltung, die Kontakte aus dem Android-System synchronisiert, filtert und in einem Widget anzeigt. Sie folgt strengen Material 3 Richtlinien und Clean Architecture Prinzipien.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Organisiert nach Layer/Feature (`ui.screens.home`, `ui.screens.settings`, `viewmodel`, `database`, `widget`).
- **ViewModel-Pattern:** `BirthdayViewModel` fungiert als Single Source of Truth.
- **UI-Modelle:** Nutzung von `@Immutable ContactUiModel` zur Performance-Optimierung (alle Berechnungen im ViewModel statt in der UI).
- **Navigation:** Jetpack Navigation mit Shared-Axis-Z-Animationen (Scale + Fade) für moderne Übergänge.

## ✨ Features
1. **HomeScreen:** 
   - Gmail-Style Suchleiste mit animiertem Placeholder.
   - Filter-Chips basierend auf Android-Kontakt-Labels.
   - High-End Fast-Scrollbar (Google Photos Style): 48dp Touch-Area, dynamische Breite beim Ziehen, Monats-Bubble.
   - FAB mit Doppelfunktion (Scroll-to-Top / Kontakt hinzufügen).
2. **Geburtstags-Logik:**
   - Spezial-Highlights: Regenbogen (Kinder <= 10), Gold (Runde ab 20), Silber (Standard heute).
3. **SettingsScreen:**
   - Zentrale Synchronisation der Kontakte & Berechtigungs-Management.
4. **Widget (Jetpack Glance):**
   - Dynamisches Layout: Berechnet Item-Anzahl basierend auf Widget-Größe (kein Scrollen nötig).
   - Gleichmäßige Verteilung der Elemente via `defaultWeight`.
   - Click-to-App: Klick auf das Widget öffnet die App und scrollt automatisch nach oben.
   - Hintergrund-Updates via `WorkManager` (Mitternacht) & On-Sync.

## 📦 Abhängigkeiten
- Compose BOM (2026.04.01)
- Room Database (v2.8.4)
- Jetpack Navigation
- Jetpack Glance (Widgets)
- WorkManager (Hintergrund-Updates)
- Coil (Bilder-Laden)

## 📝 Konventionen für die KI
- **Erklärungen:** Jede technische Änderung muss erklärt werden (Was & Warum).
- **Performance:** Teure Berechnungen gehören ins ViewModel oder in `remember`-Blöcke.
- **UI:** Material 3 Standards einhalten.

## 📜 Historie der Änderungen (Changelog)
1. **Performance & Daten-Modell:**
   - Einführung von `ContactUiModel` zur Entlastung der UI. Alle Berechnungen (Tage bis zum Geburtstag, Alter) ins ViewModel verschoben. Optimierung via `contentType`.
2. **Navigation & Struktur:**
   - Umstellung auf Layer-Prinzip (`ui.screens.home`, etc.). Integration von Jetpack Navigation mit Shared-Axis Animationen.
3. **HomeScreen & UX:**
   - Implementierung der Gmail-Searchbar und FAB-Logik.
   - **Scrollbar-Upgrade:** Ausbau zur Fast-Scrollbar mit 48dp Touch-Area und dynamischer Animation (6dp -> 12dp) für optimale Bedienbarkeit.
4. **Geburtstags-Speziallogik:**
   - Korrektur der „Heute“-Logik (0 Tage). Visuelle Rahmen für Kinder, runde und normale heutige Geburtstage.
5. **Widget & App-Polish:**
   - **Glance Widget:** Umstellung auf `SizeMode.Exact`. Dynamische Berechnung der Items pro Höhe und vertikale Gleichverteilung.
   - **Interaktion:** Widget klickbar gemacht; Übergabe von `SCROLL_TO_TOP` Intent an `MainActivity` zur automatischen Listen-Positionierung.
   - **App-Icon:** Integration eines SVG-Icons (50% Scale für Safe-Area) auf weißem Hintergrund. Fix von XML-Syntaxfehlern.
   - **Theming:** Umstellung auf `Material3.DayNight` in XML zur Vermeidung des "weißen Flackerns" beim App-Start über das Widget.
