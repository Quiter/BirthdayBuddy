# Project Status: BirthdayBuddy

> **TL;DR für neue Sessions:** Moderne M3 Geburtstags-App. Android-Kontakte sind die Single-Source-of-Truth. App-Rename von BirthdayBuddy2 auf BirthdayBuddy abgeschlossen. **Datenbank auf Version 1 zurückgesetzt (Clean Start nach App-ID Wechsel).** Architektur sauber & performant. Alle Einstellungen & Benachrichtigungsregeln sind in Room konsolidiert.

## 🚀 Aktueller Stand
Die App wurde erfolgreich von BirthdayBuddy2 in BirthdayBuddy umbenannt. In diesem Zuge wurde die `applicationId` angepasst und die Datenbank auf Version 1 zurückgesetzt, um einen sauberen Neustart zu ermöglichen. Der Fokus liegt weiterhin auf maximaler Stabilität, exzellenter Code-Qualität nach Clean-Code-Prinzipien und der kontinuierlichen Verfeinerung der Nutzererfahrung (UX-Polish).

**WICHTIG FÜR NEUE SESSIONS:**
1. **Dokumentations-Pflicht:** Bei jeder strukturellen Änderung (neue Dateien, Refactoring) muss die `PROJECT_STRUCTURE.md` zwingend aktualisiert werden.
2. **Changelog-Pflicht:** Jede signifikante Änderung muss in der `CHANGELOG.md` dokumentiert werden.
3. **Status-Relevanz:** Nur Meilensteine und aktuell relevante Fokus-Themen verbleiben in dieser Datei. Der detaillierte Verlauf findet sich in der `CHANGELOG.md`.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `ui.screens.settings.labels`, `ui.screens.settings.notifications`, `viewmodel`, `database`, `widget`).
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth; nutzt `SharedFlow` für UI-Events.
- **Data:** `ContactUiModel` (@Immutable) für performante Updates; stabile Identifikatoren via `LOOKUP_KEY`.
- **Dependency Injection:** Vollständige Integration von Hilt für alle Layer.
- **Persistence:** **Room-only Strategie.** Alle App-Einstellungen, Benachrichtigungsregeln und Kontaktdaten liegen in der Room-Datenbank.

## 🔒 Architektur-Constraints (Nicht ändern!)
Diese Punkte sind Kernentscheidungen und sollen nicht zur Optimierung oder Änderung vorgeschlagen werden:
1. **Kontakt-Sortierung:** Die Kontakte in der Hauptliste sind immer nach "Tagen bis zum nächsten Geburtstag" sortiert.
2. **Single Source of Truth:** Android-Kontakte sind die führende Quelle; die lokale DB ist ein Cache mit Anreicherungen (Geschenkideen).
3. **Persistence-Wahl:** Room ist der einzige Speicherort für strukturierte und unstrukturierte Einstellungen (AppSettings).
4. **Migration-First Policy:** Bei zukünftigen Schema-Änderungen müssen Migrationen definiert werden.

## ✨ Key Features
1. **HomeScreen:** High-End Fast-Scrollbar, Gmail-Style Suche, intelligenter FAB.
2. **Geschenkideen & Backup:** Google Keep Style Checklisten mit lokalem JSON-Backup/Restore.
3. **Label-Management:** Modernes M3-Interface zur Steuerung der Sichtbarkeit.
4. **Widget (Jetpack Glance):** Dynamisches Layout mit präzisen Mitternachts-Updates.
5. **Database Safety:** Schema-Export aktiviert (`app/schemas`) zur Unterstützung von Auto-Migrations.

## 📦 Abhängigkeiten & Polish
- **Core:** Compose BOM (2026.05.00), Room (2.8.4), Glance (1.1.1).
- **Architecture:** Hilt (2.59), WorkManager (2.11).
- **Branding:** Zentrales SVG-Logo (`res/drawable/ic_app_logo.xml`).

## 📜 Aktuelle Meilensteine (Auszug)
*Detaillierte Historie siehe `CHANGELOG.md`*

- **App Renaming & ID Swap:** Vollständige Umstellung von `com.heckmannch.birthdaybuddy2` auf `com.heckmannch.birthdaybuddy`.
- **Database Reset (v1):** Zurücksetzen der Datenbank auf Version 1 und Bereinigung alter Migrations-Logik nach dem App-ID Wechsel.
- **Settings Consolidation (Room-only):** Alle Einstellungen liegen in der `AppSettings` Room-Tabelle.
- **Advanced Notifications:** Dynamisches Regelsystem für Benachrichtigungen via WorkManager und Room.
- **Persistent Notifications:** Robustes Quittierungssystem für Erinnerungen.
- **Code Quality & Refactoring:** Einführung von `HomeUiState` und `ContactMapper` zur Verbesserung der Wartbarkeit und Reduzierung von Boilerplate (Release-Ready).

## 🎯 Kommende Aufgaben (Backlog)
- **Edge-Cases:** Verfeinerung des Verhaltens bei extrem vielen Kontakten (> 1000).
