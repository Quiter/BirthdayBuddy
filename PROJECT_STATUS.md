# Project Status: BirthdayBuddy 2

## 🚀 Aktueller Stand
Die App ist eine moderne Geburtstags-Verwaltung, die Kontakte aus dem Android-System synchronisiert, filtert und in einem Widget anzeigt. Sie folgt strengen Material 3 Richtlinien und Clean Architecture Prinzipien.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Organisiert nach Layer/Feature (`ui.screens.home`, `ui.screens.settings`, `viewmodel`, `database`, `widget`).
- **ViewModel-Pattern:** `BirthdayViewModel` fungiert als Single Source of Truth.
- **UI-Modelle:** Nutzung von `@Immutable ContactUiModel` zur Performance-Optimierung (alle Berechnungen finden im ViewModel statt).
- **Navigation:** Jetpack Navigation mit Shared-Axis-Z-Animationen (Scale + Fade) für moderne Übergänge.

## ✨ Features
1. **HomeScreen:** 
   - Gmail-Style Suchleiste mit animiertem Placeholder.
   - Filter-Chips basierend auf Android-Kontakt-Labels (Systemgruppen wie "My Contacts" werden via `SYSTEM_ID` gefiltert).
   - Fast-Scrollbar (Google Photos Style) mit Monats-Bubble und Timeline-Markierungen.
   - Floating Action Button (FAB) mit Doppelfunktion (Scroll-to-Top / Kontakt hinzufügen).
2. **Geburtstags-Logik:**
   - Spezial-Rahmen für heutige Geburtstage: Regenbogen-Gradient (Kinder <= 10), Gold (Runde ab 20), Silber (Rest).
   - Korrekte Berechnung für "Heute" (0 Tage verbleibend).
3. **SettingsScreen:**
   - Zentrale Synchronisation der Kontakte.
   - Berechtigungs-Management (automatischer Request beim Start + manueller Button im Empty-State).
4. **Widget (Jetpack Glance):**
   - Zeigt die nächsten 5 Geburtstage an.
   - Tägliche Aktualisierung via `WorkManager` (Mitternacht) + Trigger bei App-Sync.
   - Responsive Design für verschiedene Größen.

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
