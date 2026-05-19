# Project Structure: BirthdayBuddy

## 📁 Root
- `MainActivity.kt`: Haupteinstiegspunkt der App. Regelt das Navigations-Hosting (NavHost), das globale Intent-Handling (z.B. Widget-Klicks) und automatische Filter-Resets bei Inaktivität.
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse zur Initialisierung der Dependency Injection und Konfiguration des WorkManagers.
- `PROJECT_STATUS.md`: Dokumentation des aktuellen Entwicklungsstands, der Architektur-Constraints und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation des Projekts).
- `CHANGELOG.md`: Vollständige Historie aller signifikanten Änderungen und Feature-Releases.

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Modul zur Bereitstellung von Singleton-Instanzen (Datenbank, DAOs, Repositories).

## 📁 Data Layer (`data`)
- ### 📁 Local (`data.local`)
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
- ### 📁 Repository (`data.repository`)
    - `ContactRepository.kt`: Orchestriert den Datenfluss zwischen Room-DB und der System-Kontaktquelle; implementiert die Sync-Logik.
    - `GiftIdeaBackupManager.kt`: Spezialisierte Klasse für den JSON-basierten Im- und Export von Geschenkideen.
    - `NotificationRepository.kt`: Zentraler Zugriff auf Benachrichtigungsregeln und persistente App-Einstellungen.
    - `SystemContactDataSource.kt`: Kapselt den Low-Level Zugriff auf den Android ContentResolver (Kontakte, Gruppen, Events).
    - `TimeRepository.kt`: Reaktive Zeitquelle, die bei Datumswechseln (Mitternacht) automatische UI-Updates triggert.
- ### 📁 Mapper (`data.mapper`)
    - `ContactMapper.kt`: Reine Logik-Komponente zur Transformation von Datenbank-Entitäten in Anzeige-Modelle.

## 📁 UI Layer (`ui`)
- ### 📁 Screens (`ui.screens`)
    - #### 📁 Home (`home`)
        - `HomeScreen.kt`: Container-Composable des Hauptbildschirms; verwaltet den UI-State.
        - `HomeState.kt`: Plain State Holder für die UI-Logik (Scroll-Zustand, Fokus).
        - ##### 📁 Components (`home.components`)
            - `BirthdayItem.kt`: Zentrales Koordinationselement der Kontaktkarte; verwaltet den Expand-State und delegiert an spezialisierte Sub-Komponenten.
            - `BirthdayList.kt`: LazyColumn-Implementierung mit Optimierungen (Auto-Collapse beim Scrollen und Filterwechsel).
            - `BirthdayStatus.kt`: Anzeige des Alters und der verbleibenden Tage.
            - `ConfettiEffect.kt`: Hochperformantes Partikelsystem für Geburtstags-Animationen.
            - `ContactActionRow.kt`: Reihe mit Messenger- und Kontakt-Aktionen.
            - `ContactImage.kt`: Komponente für das Kontaktbild oder die Initialen.
            - `FastScrollbar.kt`: Hochperformante Scrollbar mit Sub-Pixel-Präzision.
            - `GiftIdeaList.kt`: Spezialisierte Komponente für die Inline-Verwaltung von Geschenkideen.
            - `HomeFAB.kt`: Multifunktionaler FAB mit Morphing-Animation.
            - `HomeTopBar.kt`: Kombiniert SearchBar und LabelFilterBar.
            - `LabelFilterBar.kt`: Chip-Leiste zur Filterung nach Labels.
            - `SearchBar.kt`: M3 SearchBar-Integration.
    - #### 📁 Onboarding (`onboarding`)
        - `OnboardingScreen.kt`: Multi-Page Flow für die initiale Konfiguration.
    - #### 📁 Settings (`settings`)
        - `SettingsScreen.kt`: Haupteinstellungsmenü.
        - `labels/LabelSettingsScreen.kt`: Verwaltung der Label-Sichtbarkeit.
        - `notifications/NotificationSettingsScreen.kt`: Konfiguration des Erinnerungssystems.
        - ##### 📁 Components (`notifications.components`)
            - `NotificationRuleItem.kt`, `EditRuleDialog.kt`, `NotificationWorker.kt`, `SnoozeWorker.kt`, `NotificationActionReceiver.kt`, `NotificationHelper.kt`.
        - `backup/BackupScreen.kt`: Import/Export von Geschenkideen.
        - `about/AboutScreen.kt` & `PrivacyPolicyScreen.kt`: App-Infos und Rechtliches.
- ### 📁 Models (`ui.model`)
    - `ContactUiModel.kt`: Immutable UI-Modell für Kontakte.
    - `HomeUiState.kt`: Gebündelter State für den Home-Screen.
    - `LabelManagementModel.kt`: Modell für die Label-Verwaltung.
    - `GiftIdea.kt`: Modell für Geschenkideen mit JSON-Logik.
- ### 📁 Theme (`ui.theme`)
    - `Theme.kt`, `Color.kt`, `Type.kt`: Design-System Definitionen.

## 📁 ViewModel (`viewmodel`)
- `HomeViewModel.kt`: Zuständig für die Kontaktliste, Suche, Filterung und den Home-Screen State.
- `NotificationViewModel.kt`: Verwaltung der Benachrichtigungsregeln und deren Synchronisation mit dem WorkManager.
- `SettingsViewModel.kt`: Onboarding-Status und globale App-Einstellungen.
- `LabelViewModel.kt`: Spezielle Logik für die Label-Verwaltung und Konfiguration.
- `BackupViewModel.kt`: Logik für den Import und Export von Geschenkideen.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Erweiterungsfunktionen für LocalDate.

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`, `BirthdayWidgetReceiver.kt`, `BirthdayWidgetWorker.kt`: Glance-basierte Widget-Komponenten.

## 🌍 Internationalisierung (I18n)
- `res/values/strings.xml`: Standard-Sprachressourcen (**Englisch**).
- `res/values-de/strings.xml`: Lokalisierte Sprachressourcen (**Deutsch**).

## 🧪 Testing (`src/test` & `src/androidTest`)
- `data/local/ConvertersTest.kt`: Tests für TypeConverter.
- `data/repository/NotificationRepositoryTest.kt`: Integrationstests für das Repository.
- `viewmodel/HomeViewModelSearchTest.kt`: Tests der Suchlogik im HomeViewModel.
- `ui/components/BirthdayItemInteractionTest.kt`: UI-Tests für die Interaktion mit Kontakt-Karten.
