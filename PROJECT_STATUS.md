# Project Status: BirthdayBuddy 2

> **TL;DR für neue Sessions:** Moderne M3 Geburtstags-App. Android-Kontakte sind die Single-Source-of-Truth. Migration auf `LOOKUP_KEY` & `localId` abgeschlossen (zukunftssicher). High-Performance Sync (Parallel/Batch), hybride Filter-Logik, präzises Widget-Update (Mitternacht). **Neu: Robustes Backup/Restore für Geschenkideen (JSON) & modernisiertes M3 Label-Management.** Architektur sauber & performant (Room v6, Compose BOM 2026.04).

## 🚀 Aktueller Stand
Die App befindet sich gerade in einer umfassenden Refactoring-Phase (V3), um die Skalierbarkeit und Testbarkeit zu erhöhen. Fokus liegt auf der Trennung von Business-Logik und UI-State durch das Repository-Pattern und moderner Lifecycle-Integration.

**WICHTIG FÜR NEUE SESSIONS:**
1. **Dokumentations-Pflicht:** Bei jeder strukturellen Änderung (neue Dateien, Refactoring) muss die `PROJECT_STRUCTURE.md` zwingend aktualisiert werden.
2. **Changelog-Pflicht:** Jede signifikante Änderung muss in der `CHANGELOG.md` dokumentiert werden.
3. **Status-Relevanz:** Nur Meilensteine und aktuell relevante Fokus-Themen verbleiben in dieser Datei. Der detaillierte Verlauf findet sich in der `CHANGELOG.md`.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `ui.screens.settings.labels`, `ui.screens.settings.notifications`, `viewmodel`, `database`, `widget`). Alles, was zu einem Feature gehört (UI, Worker, Preferences), liegt im entsprechenden Package.
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth; nutzt `SharedFlow` für UI-Events.
- **Data:** `ContactUiModel` (@Immutable) für performante Updates; stabile Identifikatoren via `LOOKUP_KEY`.
- **Dependency Injection:** Vollständige Integration von Hilt für alle Layer.

## ✨ Key Features
1. **HomeScreen:** High-End Fast-Scrollbar, Gmail-Style Suche, intelligenter FAB.
2. **Geschenkideen & Backup:** Google Keep Style Checklisten mit lokalem JSON-Backup/Restore (Auto-Matching).
3. **Label-Management:** Modernes M3-Interface zur Steuerung der Sichtbarkeit und hybride Filter-Logik.
4. **Widget (Jetpack Glance):** Dynamisches Layout mit präzisen Mitternachts-Updates.
5. **Performance:** Zweistufige ViewModel-Pipeline für verzögerungsfreies Filtern; paralleler Sync; GPU-beschleunigte UI-Animationen.

## 📦 Abhängigkeiten & Polish
- **Core:** Compose BOM (2026.05.00), Room (2.8.4), Glance (1.1.1).
- **Architecture:** Hilt (2.59), DataStore (1.2), WorkManager (2.11).
- **Branding:** SVG-Icon (50% Safe-Scale), DayNight-Theming für nahtlosen Start.

## 📜 Aktuelle Meilensteine (Auszug)
*Detaillierte Historie siehe `CHANGELOG.md`*

- **V3 Refactoring (Abgeschlossen):** Einführung des Repository-Patterns, Hilt-Integration und vollständiges State-Hoisting in der UI.
- **Data Layer Optimization:** Extraktion von System-Abfragen in spezialisierte DataSources zur Entlastung der Repositories.
- **Backup System:** Implementierung von robustem Import/Export für Geschenkideen inkl. Fallback-Matching.
- **UI Modernization:** Neugestaltung des Label-Managements mit Material 3 Cards und FilterChips; intelligente Filterung verwaister Labels für mehr Übersichtlichkeit.
- **Advanced Notifications:** Implementierung eines dynamischen Regelsystems für Benachrichtigungen (beliebige Tage vorher & Uhrzeiten) via WorkManager und Room.
- **UX & Technical Polish:** Einführung einer Global Search Logik mit Keyword-Matching (Reihenfolge-unabhängig), verfeinerte visuelle Status-Indikatoren für Geburtstage, Integration der Extended Material Icons für präzisere Symbolik und projektweite Bereinigung technischer Schulden (Code Analysis).

## 🎯 Kommende Aufgaben (Backlog)
- **Persistent Notifications:** Implementierung eines "Erledigt"-Status für Benachrichtigungen, sodass diese bis zur aktiven Quittierung erhalten bleiben.
- **Leistungsoptimierung:** Weitere Reduzierung der Re-Compositions in der Hauptliste bei großen Datenmengen.
