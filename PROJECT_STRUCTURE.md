# Project Structure: BirthdayBuddy 2

## 📁 Root
- `MainActivity.kt`: Einstiegspunkt der App. Regelt Navigation (NavHost) und Intent-Handling (z.B. vom Widget).
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse und WorkManager-Konfiguration.
- `PROJECT_STATUS.md`: Aktueller Fokus und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation).
- `CHANGELOG.md`: Vollständige Historie aller Änderungen.

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Module zur Bereitstellung von Singleton-Instanzen (DB, Repos).

## 📁 Database (`database`)
- `AppDatabase.kt`: Room-Datenbank Definition mit Singleton-Pattern.
- `Contact.kt`: Entity-Klasse für Kontakte inkl. Geschenkideen und Label-Listen.
- `ContactDao.kt`: Data Access Object für Kontakte (CRUD-Operationen & Sync-Logik).
- `LabelConfig.kt`: Entity für Label-Konfigurationen (Sichtbarkeit/Ignorieren).
- `LabelConfigDao.kt`: DAO für Label-Einstellungen.
- `NotificationRule.kt`: Entity für flexible Benachrichtigungsregeln (Abstand & Uhrzeit).
- `NotificationRuleDao.kt`: DAO für Benachrichtigungsregeln.
- `Converters.kt`: TypeConverters für `LocalDate` und Listen-Encoding.

## 📁 Repository (`repository`)
- `ContactRepository.kt`: Zentrale Instanz für Kontakt-Daten (Room + System-Provider).
- `SystemContactDataSource.kt`: Kapselt den Zugriff auf den Android ContentResolver (Kontakte, Gruppen).
- `GiftIdeaBackupManager.kt`: Handhabt den Im- und Export von Geschenkideen (JSON-Logik).
- `NotificationRepository.kt`: Verwaltung der flexiblen Benachrichtigungsregeln.
- `PreferenceRepository.kt`: Verwaltung von App-Einstellungen via DataStore.
- `TimeRepository.kt`: Reaktive Zeitquelle für automatische UI-Updates um Mitternacht.

## 📁 Home Screen (`ui.screens.home`)
- `HomeScreen.kt`: Haupt-Container des Home-Screens. Orchestriert TopBar, List und FAB.
- ### 📁 Components (`home.components`)
    - `BirthdayItem.kt`: Einzelnes Listenelement mit Sticky-Swipe (Now Playing Style) und Aktions-Buttons.
    - `BirthdayList.kt`: Verwaltet die LazyColumn der Geburtstage und den exklusiven Swipe-Status.
    - `FastScrollbar.kt`: Implementierung der Google-Photos-Style Scrollbar mit Monats-Bubble.
    - `GiftIdeaDialog.kt`: Checklisten-Dialog (Google Keep Style) für Geschenkideen.
    - `HomeFAB.kt`: Animierter Floating Action Button für Scroll-to-Top und Kontakt-Hinzufügen.
    - `HomeTopBar.kt`: Orchestriert SearchBar und LabelFilterBar.
    - `SearchBar.kt`: Die Suchleiste mit Settings-Integration.
    - `LabelFilterBar.kt`: Horizontale Liste der Filter-Chips.

## 📁 Settings (`ui.screens.settings`)
- `SettingsScreen.kt`: Haupt-Einstellungsmenü.
- `labels/LabelSettingsScreen.kt`: Verwaltung der Label-Sichtbarkeit und Filter-Regeln.
- `notifications/NotificationSettingsScreen.kt`: Einstellungen für tägliche Erinnerungen und Uhrzeit.
- `notifications/NotificationWorker.kt`: WorkManager-Logik für tägliche Benachrichtigungen.
- `notifications/NotificationHelper.kt`: Utility zum Erstellen und Anzeigen der System-Notifications.
- `backup/BackupScreen.kt`: Import/Export von Geschenkideen (JSON-Format).

## 📁 Theme & Design (`ui.theme`)
- `Theme.kt`: Material 3 Theme-Definition mit Dynamic Color Support.
- `Color.kt`: Farb-Konstanten (z.B. BirthdayGold, KidColors).
- `Type.kt`: Typografie-Einstellungen.

## 📁 ViewModel (`viewmodel`)
- `BirthdayViewModel.kt`: Zentrales ViewModel (Single Source of Truth). Regelt Sync, Filterung und Business-Logik.
- `ContactUiModel.kt`: UI-Modelle für Kontakte, Labels und Geschenkideen.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Extensions für Datumsberechnungen (Schaltjahr-Support).

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`: Glance-basierte Widget-UI.
- `BirthdayWidgetReceiver.kt`: Empfänger für Widget-Updates.
- `BirthdayWidgetWorker.kt`: WorkManager für präzise Mitternachts-Updates des Widgets.
