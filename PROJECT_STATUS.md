# Project Status: BirthdayBuddy 2

> **TL;DR für neue Sessions:** Moderne M3 Geburtstags-App. Android-Kontakte sind die Single-Source-of-Truth. Migration auf `LOOKUP_KEY` & `localId` abgeschlossen. High-Performance Sync (Parallel/Batch), hybride Filter-Logik, präzises Widget-Update (Mitternacht). Architektur sauber & performant (Room v13, Compose BOM 2026.05). **Alle Einstellungen & Benachrichtigungsregeln sind in Room konsolidiert (kein DataStore mehr).**

## 🚀 Aktueller Stand
Die App stellt eine solide und hochperformante Grundlage für die weitere Entwicklung dar. Das umfassende Refactoring (V3) auf das Repository-Pattern, Hilt-DI und Room v13 ist erfolgreich abgeschlossen. Der aktuelle Fokus liegt auf maximaler Stabilität, exzellenter Code-Qualität nach Clean-Code-Prinzipien und der kontinuierlichen Verfeinerung der Nutzererfahrung (UX-Polish).

**WICHTIG FÜR NEUE SESSIONS:**
1. **Dokumentations-Pflicht:** Bei jeder strukturellen Änderung (neue Dateien, Refactoring) muss die `PROJECT_STRUCTURE.md` zwingend aktualisiert werden.
2. **Changelog-Pflicht:** Jede signifikante Änderung muss in der `CHANGELOG.md` dokumentiert werden.
3. **Status-Relevanz:** Nur Meilensteine und aktuell relevante Fokus-Themen verbleiben in dieser Datei. Der detaillierte Verlauf findet sich in der `CHANGELOG.md`.

## 🛠 Architektur & Struktur
- **Package-Struktur:** Feature-basierte Layer (`ui.screens.home`, `ui.screens.settings.labels`, `ui.screens.settings.notifications`, `viewmodel`, `database`, `widget`).
- **ViewModel:** `BirthdayViewModel` als Single Source of Truth; nutzt `SharedFlow` für UI-Events.
- **Data:** `ContactUiModel` (@Immutable) für performante Updates; stabile Identifikatoren via `LOOKUP_KEY`.
- **Dependency Injection:** Vollständige Integration von Hilt für alle Layer.
- **Persistence:** **Room-only Strategie.** Alle App-Einstellungen, Benachrichtigungsregeln und Kontaktdaten liegen in der Room-Datenbank zur Vermeidung von Synchronisationsfehlern und Datenverlust bei Updates.

## 🔒 Architektur-Constraints (Nicht ändern!)
Diese Punkte sind Kernentscheidungen und sollen nicht zur Optimierung oder Änderung vorgeschlagen werden:
1. **Kontakt-Sortierung:** Die Kontakte in der Hauptliste sind immer nach "Tagen bis zum nächsten Geburtstag" sortiert. Dies ist ein festes UX-Prinzip der App.
2. **Single Source of Truth:** Android-Kontakte sind die führende Quelle; die lokale DB ist ein Cache mit Anreicherungen (Geschenkideen).
3. **Persistence-Wahl:** Room ist der einzige Speicherort für strukturierte und unstrukturierte Einstellungen (AppSettings).
4. **Migration-First Policy:** Bei Versionssprüngen der Datenbank *muss* zwingend eine Migration (Auto oder Manuell) definiert werden. Destruktive Migrationen sind nur die letzte Instanz für Entwicklungs-Builds.

## ✨ Key Features
1. **HomeScreen:** High-End Fast-Scrollbar, Gmail-Style Suche, intelligenter FAB.
2. **Geschenkideen & Backup:** Google Keep Style Checklisten mit lokalem JSON-Backup/Restore (Auto-Matching).
3. **Label-Management:** Modernes M3-Interface zur Steuerung der Sichtbarkeit und hybride Filter-Logik.
4. **Widget (Jetpack Glance):** Dynamisches Layout mit präzisen Mitternachts-Updates.
5. **Database Safety:** Schema-Export aktiviert (`app/schemas`) zur Unterstützung von Auto-Migrations und zur Vermeidung von "IllegalStateException".

## 📦 Abhängigkeiten & Polish
- **Core:** Compose BOM (2026.05.00), Room (2.8.4 - v13 Schema), Glance (1.1.1).
- **Architecture:** Hilt (2.59), WorkManager (2.11).
- **Branding:** Zentrales SVG-Logo (`res/drawable/ic_app_logo.xml`) zur konsistenten Wiederverwendung; DayNight-Theming für nahtlosen Start.

## 📜 Aktuelle Meilensteine (Auszug)
*Detaillierte Historie siehe `CHANGELOG.md`*

- **Migration Hardening (V13):** Behebung eines kritischen Start-Absturzes auf Alt-Geräten durch Implementierung einer manuellen `MIGRATION_1_13`. Dies sichert den Erhalt der `contacts`-Tabelle beim Wechsel vom Legacy-Schema (v1) auf das moderne `localId`-Schema (v13).
- **Schema Tracking:** Aktivierung des KSP-Schema-Exports zur Unterstützung von Room Auto-Migrations für zukünftige Updates.
- **Settings Consolidation (Room-only):** Vollständige Entfernung des `DataStore` (PreferenceRepository). Alle Einstellungen (Benachrichtigungen an/aus, Swipe-Hint) wurden in die neue `AppSettings` Room-Tabelle migriert. Dies garantiert Datenkonsistenz bei App-Updates.
- **Sync-Hardening:** Einführung eines `lastSyncTimestamp` in der Datenbank zur Nachverfolgung der Synchronisation.
- **Data Loss Prevention:** Re-Aktivierung von `fallbackToDestructiveMigration` lediglich als Fallback-Sicherung; Einführung einer Self-Healing Logik im ViewModel zur Wiederherstellung von Standard-Benachrichtigungsregeln.
- **Advanced Notifications:** Implementierung eines dynamischen Regelsystems für Benachrichtigungen (beliebige Tage vorher & Uhrzeiten) via WorkManager und Room.
- **Persistent Notifications:** Implementierung eines robusten Quittierungssystems für Erinnerungen. Benachrichtigungen bleiben nun so lange aktiv (persistent), bis sie explizit als "Erledigt" markiert oder "Gesnoozed" werden.
- **Database Layer Optimization:** Vollständige Bereinigung der DAOs; Einführung von Indizes für performante Lookups; Upgrade auf das moderne `@Upsert` Pattern.

## 🎯 Kommende Aufgaben (Backlog)
- **Edge-Cases:** Verfeinerung des Verhaltens bei extrem vielen Kontakten (> 1000) und weitere Reduzierung der Initial-Ladezeit.
