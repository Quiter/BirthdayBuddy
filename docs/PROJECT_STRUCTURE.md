# Project Structure: BirthdayBuddy

## 📁 Root
- `MainActivity.kt`: Haupteinstiegspunkt der App. Regelt das Navigations-Hosting (NavHost), das globale Intent-Handling (z.B. Widget-Klicks) und automatische Filter-Resets bei Inaktivität.
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse zur Initialisierung der Dependency Injection und Konfiguration des WorkManagers.
- `PROJECT_STATUS.md`: Dokumentation des aktuellen Entwicklungsstands, der Architektur-Constraints und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation des Projekts).
- `CHANGELOG.md`: Vollständige Historie aller signifikanten Änderungen und Feature-Releases.

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Modul zur Bereitstellung von Singleton-Instanzen (Datenbank, DAOs, Repositories).

## 📁 Database (`database`)
- `AppDatabase.kt`: Zentrale Room-Datenbank Definition mit Singleton-Pattern und Thread-sicherer Instanziierung.
- `AppSettings.kt`: Entity für globale App-Konfigurationen (Benachrichtigungs-Status, letzter Sync-Zeitpunkt).
- `AppSettingsDao.kt`: Datenzugriffsobjekt für die App-Einstellungen.
- `Contact.kt`: Haupt-Entity für Kontakte; speichert Basisdaten, Labels und enkodierte Geschenkideen.
- `ContactDao.kt`: DAO für Kontakte mit Unterstützung für Batch-Operationen und atomare Refreshes.
- `Converters.kt`: TypeConverter für die Konvertierung komplexer Typen (LocalDate, Listen) in DB-kompatible Formate.
- `LabelConfig.kt`: Entity zur Verwaltung der Sichtbarkeit und Filter-Regeln für Kontakt-Labels.
- `LabelConfigDao.kt`: DAO für Label-Konfigurationen.
- `NotificationRule.kt`: Entity für dynamische Benachrichtigungszeitpunkte (Tage vorher, Uhrzeit).
- `NotificationRuleDao.kt`: DAO für die Verwaltung der Erinnerungsregeln.
- `PendingNotification.kt`: Entity zur Nachverfolgung aktiver System-Benachrichtigungen für das Persistenz-System.
- `PendingNotificationDao.kt`: DAO für die Verwaltung noch nicht quittierter Erinnerungen.

## 📁 Repository (`repository`)
- `ContactRepository.kt`: Orchestriert den Datenfluss zwischen Room-DB und der System-Kontaktquelle; implementiert die Sync-Logik.
- `GiftIdeaBackupManager.kt`: Spezialisierte Klasse für den JSON-basierten Im- und Export von Geschenkideen.
- `NotificationRepository.kt`: Zentraler Zugriff auf Benachrichtigungsregeln und persistente App-Einstellungen.
- `SystemContactDataSource.kt`: Kapselt den Low-Level Zugriff auf den Android ContentResolver (Kontakte, Gruppen, Events).
- `TimeRepository.kt`: Reaktive Zeitquelle, die bei Datumswechseln (Mitternacht) automatische UI-Updates triggert.

## 📁 Home Screen (`ui.screens.home`)
- `HomeScreen.kt`: Container-Composable des Hauptbildschirms; verwaltet den Lebenszyklus des UI-States und Callbacks.
- ### 📁 Components (`home.components`)
    - `BirthdayItem.kt`: Listen-Element mit Sticky-Swipe-Logik, Aktions-Buttons und dynamischen Rahmen-Farben.
    - `BirthdayList.kt`: LazyColumn-Implementierung mit Optimierungen für Re-Compositions und exklusive Item-Expansion.
    - `FastScrollbar.kt`: Hochperformante Scrollbar mit Sub-Pixel-Präzision, haptischem Feedback und Monats-Bubble.
    - `GiftIdeaDialog.kt`: Checklisten-Dialog (Google Keep Style) für die schnelle Verwaltung von Geschenkideen.
    - `HomeFAB.kt`: Multifunktionaler FAB mit Morphing-Animation (Add-Contact vs. Scroll-to-Top).
    - `HomeTopBar.kt`: Kombiniert die SearchBar und die LabelFilterBar zu einer responsiven Kopfleiste.
    - `LabelFilterBar.kt`: Horizontale Chip-Leiste zur Filterung der Kontakte nach Labels.
    - `OnboardingDialog.kt`: M3 Dialog zur Abfrage der Benachrichtigungs-Präferenz beim Erststart.
    - `SearchBar.kt`: M3 SearchBar-Integration mit Fokus-Management und Settings-Link.

## 📁 Settings (`ui.screens.settings`)
- `SettingsScreen.kt`: Haupteinstellungsmenü mit kategorialer Unterteilung.
- `labels/LabelSettingsScreen.kt`: UI zur Verwaltung der Label-Sichtbarkeit (Verbergen vs. Ignorieren).
- `notifications/NotificationSettingsScreen.kt`: Konfiguration des dynamischen Erinnerungssystems.
- ### 📁 Components (`notifications.components`)
    - `NotificationRuleItem.kt`: Visuelle Darstellung einer einzelnen Erinnerungsregel.
    - `EditRuleDialog.kt`: Kombinierter Slider- und TimePicker-Dialog zum Bearbeiten von Regeln.
    - `NotificationWorker.kt`: Hintergrund-Job zur täglichen Prüfung und Auslösung fälliger Geburtstage.
    - `SnoozeWorker.kt`: Ermöglicht das zeitversetzte Wiederholen von Benachrichtigungen.
    - `NotificationActionReceiver.kt`: BroadcastReceiver für Quick Actions (Erledigt, Später) direkt im Tray.
    - `NotificationHelper.kt`: Utility zum Aufbau und Anzeigen von System-Benachrichtigungen (inkl. Persistenz-Support).
- `backup/BackupScreen.kt`: Orchestriert die Logik für Import/Export von Geschenkideen.
- ### 📁 Components (`backup.components`)
    - `BackupContent.kt`: Die UI-Darstellung der Backup-Seite.
- `about/AboutScreen.kt`: Informationsseite mit App-Details und rechtlichen Hinweisen.

## 📁 Theme & Design (`ui.theme`)
- `Theme.kt`: Material 3 Theme-Konfiguration mit Support für Dynamic Color und Dark Mode.
- `Color.kt`: Projektweite Farbpalette (inkl. Spezialfarben für Gold-/Silber-Geburtstage).
- `Type.kt`: Definition der Typografie-Styles.

## 📁 ViewModel (`viewmodel`)
- `BirthdayViewModel.kt`: Zentrales ViewModel (Single Source of Truth); orchestriert State-Flows für Suche, Filter und Daten-Sync.
- `ContactUiModel.kt`: Immutable UI-Modell für Kontakte; optimiert für Jetpack Compose.
- `LabelManagementModel.kt`: Immutable UI-Modell für die Verwaltung von Labels in den Einstellungen.
- `HomeUiState.kt`: Gebündelter State für den Home-Screen zur Reduzierung von Prop-Drilling und Steigerung der Performance.
- `GiftIdea.kt`: Eigenständiges Modell für Geschenkideen mit integrierter JSON-Mapping-Logik für die Persistenz.
- `ContactMapper.kt`: Reine Logik-Komponente zur Transformation von Datenbank-Entitäten in Anzeige-Modelle.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Erweiterungsfunktionen für LocalDate (Schaltjahr-Support, Datums-Projektionen).

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`: Glance-basierte Widget-UI mit Unterstützung für dynamische Layouts und Glance-State.
- `BirthdayWidgetReceiver.kt`: Einstiegspunkt für das Widget-System und Trigger für Daten-Updates.
- `BirthdayWidgetWorker.kt`: WorkManager-Job für die präzise Aktualisierung des Widgets um Mitternacht.

## 🌍 Internationalisierung (I18n)
- `res/values/strings.xml`: Standard-Sprachressourcen (**Englisch**).
- `res/values-de/strings.xml`: Lokalisierte Sprachressourcen (**Deutsch**).
- **WICHTIGER CONSTRAINT:** Bei der Erstellung neuer Strings oder der Bearbeitung bestehender Texte müssen **zwingend immer beide Dateien** (`values` und `values-de`) synchron gehalten werden, um eine konsistente Nutzererfahrung in beiden Sprachen zu gewährleisten.
