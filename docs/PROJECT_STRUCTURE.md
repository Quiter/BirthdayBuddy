# Project Structure: BirthdayBuddy

## 📁 Root
- `MainActivity.kt`: Haupteinstiegspunkt der App. Regelt das Navigations-Hosting (NavHost), das globale Intent-Handling (z.B. Widget-Klicks) über Compose-seitiges `activityIntent` State-Handling, und automatische Filter-Resets bei Inaktivität über einen batterieschonenden `LifecycleEventObserver`.
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse zur Initialisierung der Dependency Injection und Konfiguration des WorkManagers.
- `PROJECT_STATUS.md`: Dokumentation des aktuellen Entwicklungsstands, der Architektur-Constraints und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation des Projekts).
- `CHANGELOG.md`: Vollständige Historie aller signifikanten Änderungen und Feature-Releases.

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Modul zur Bereitstellung von Singleton-Instanzen und performanten, leichtgewichtigen `@Reusable` DAO-Bindings zur Steigerung der DI-Performanz.
- `HelperBindingsModule.kt`: Hilt-Modul zur Bereitstellung der Singleton-Bindings für Hilfsklassen (WidgetUpdater, NotificationScheduler, ImagePrefetcher).

## 📁 Data Layer (`data`)
- ### 📁 Local (`data.local`)
    - `AppDatabase.kt`: Room-Datenbank für flüchtige/große Daten (Kontakte-Cache, Benachrichtigungs-Nachverfolgung).
    - `SettingsDatabase.kt`: Room-Datenbank für persistente Benutzer-Einstellungen (Regeln, Labels, GiftIdeas-Backup).
    - `AppSettings.kt`: Entity für globale App-Konfigurationen (Benachrichtigungs-Status, letzter Sync-Zeitpunkt).
    - `AppSettingsDao.kt`: Datenzugriffsobjekt für die App-Einstellungen.
    - `Contact.kt`: Haupt-Entity für den System-Kontakt-Cache.
    - `ContactDao.kt`: DAO für Kontakte mit Unterstützung für Batch-Operationen und atomare Refreshes.
    - `ContactUserData.kt`: Entity für benutzerdefinierte Zusatzdaten (z.B. Geschenkideen), die unabhängig vom Cache persistiert werden.
    - `ContactUserDataDao.kt`: DAO für benutzerdefinierte Kontaktdaten.
    - `Converters.kt`: TypeConverter für Basis-Typen (LocalDate, Listen).
    - `GiftIdeaConverters.kt`: Spezialisierte TypeConverter für Geschenkideen-Listen.
    - `LabelConfig.kt`: Entity zur Verwaltung der Sichtbarkeit und Filter-Regeln für Kontakt-Labels.
    - `LabelConfigDao.kt`: DAO für Label-Konfigurationen.
    - `NotificationRule.kt`: Entity für dynamische Benachrichtigungszeitpunkte (Tage vorher, Uhrzeit).
    - `NotificationRuleDao.kt`: DAO für die Verwaltung der Erinnerungsregeln.
    - `PendingNotification.kt`: Entity zur Nachverfolgung aktiver System-Benachrichtigungen.
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
        - `HomeScreen.kt`: Container-Composable des Hauptbildschirms; verwaltet den UI-State. Verwendet `rememberUpdatedState` für sichere Navigationstargets und delegiert Daten-Synchronisation sowie Bild-Preloading an das `HomeViewModel`.
        - `HomeState.kt`: Plain State Holder für die UI-Logik (Scroll-Zustand, Fokus).
        - `HomeActions.kt`: Wrapper für Benutzeraktionen zur Reduzierung von Prop-Drilling.
        - ##### 📁 Components (`home.components`)
            - ###### 📁 List (`home.components.list`)
                - `BirthdayDatePickerDialog.kt`: Wiederverwendbarer, modularer Date-Picker Dialog für die bequeme Eingabe von Geburtstagen.
                - `BirthdayItem.kt`: Zentrales Koordinationselement der Kontaktkarte; verwaltet den Expand-State und delegiert an spezialisierte Sub-Komponenten (wie z.B. den BirthdayDatePickerDialog).
                - `BirthdayList.kt`: LazyColumn-Implementierung mit Optimierungen (Auto-Collapse beim Scrollen und Filterwechsel) sowie intelligent unterdrückten Platzierungsanimationen (`skipPlacementAnimation`) bei Filter- und Suchwechseln.
                - `BirthdayStatus.kt`: Anzeige des Alters und der verbleibenden Tage.
                - `ConfettiEffect.kt`: Hochperformantes Partikelsystem für Geburtstags-Animationen.
                - `ContactImage.kt`: Komponente für das Kontaktbild oder die Initialen.
                - `FastScrollbar.kt`: Hochperformante Scrollbar mit Sub-Pixel-Präzision. Intern sauber strukturiert durch private Sub-Composables (`ScrollbarBubble` und `ScrollbarTrackAndThumb`) zur Trennung von Layout, Gesten- und Zeichenlogik. Nutzt die `Popup`-API für die Bubble, um Clipping und Z-Order-Überlagerungen durch die `HomeTopBar` zu vermeiden.
                - `GiftIdeaList.kt`: Spezialisierte Komponente für die Inline-Verwaltung von Geschenkideen.
            - ###### 📁 TopBar (`home.components.topbar`)
                - `HomeTopBar.kt`: Kombiniert SearchBar und LabelFilterBar; unterstützt adaptive Layouts durch Zentrierung auf breiten Bildschirmen.
                - `LabelFilterBar.kt`: Chip-Leiste zur Filterung nach Labels.
                - `SearchBar.kt`: M3 SearchBar-Integration.
            - ###### 📁 Actions (`home.components.actions`)
                - `ContactActionRow.kt`: Reihe mit Messenger- und Kontakt-Aktionen.
                - `HomeFAB.kt`: Multifunktionaler FAB mit Morphing-Animation.
                - `MessengerApp.kt`: Enum zur Definition unterstützter Messenger und deren Branding.
    - #### 📁 Onboarding (`onboarding`)
        - `OnboardingScreen.kt`: Multi-Page Flow für die initiale Konfiguration.
        - ##### 📁 Components (`onboarding.components`)
            - `OnboardingCommon.kt`: Gemeinsame UI-Komponenten und Wrapper.
            - `WelcomePage.kt`: Individuelle Onboarding-Seite für die Begrüßung.
            - `ContactsPage.kt`: Individuelle Onboarding-Seite für die Kontaktberechtigung.
            - `NotificationsPage.kt`: Individuelle Onboarding-Seite für die Benachrichtigungseinstellungen.
            - `ReadyPage.kt`: Individuelle Onboarding-Seite für den Abschluss.
            - `OnboardingFooter.kt`: Navigations-Footer mit Dots, vollflächig gerendert (`fillMaxWidth`) für konsistente responsive Darstellung auf breiten Bildschirmen.
    - #### 📁 Settings (`settings`)
        - `SettingsScreen.kt`: Haupteinstellungsmenü.
        - `labels/LabelSettingsScreen.kt`: Verwaltung der Label-Sichtbarkeit.
        - `notifications/NotificationSettingsScreen.kt`: Konfiguration des Erinnerungssystems.
        - ##### 📁 Components (`notifications.components`)
            - `NotificationRuleItem.kt`: UI-Element für eine einzelne Benachrichtigungsregel.
            - `EditRuleDialog.kt`: Dialog zum Bearbeiten/Erstellen von Erinnerungsregeln. Ermöglicht eine präzise, textbasierte Vorwarnzeit-Eingabe in Tagen oder Wochen (Einheiten-Auswahl per RadioButtons) mit integriertem System-TimePicker.
            - `NotificationWorker.kt`: Hintergrund-Prozess für die Benachrichtigungs-Logik.
            - `SnoozeWorker.kt`: Hintergrund-Prozess für die "Später"-Funktion.
            - `NotificationActionReceiver.kt`: Verarbeitet Klicks auf Benachrichtigungs-Buttons.
            - `NotificationHelper.kt`: Hilfsklasse für den System-Notification-Manager.
        - `backup/BackupScreen.kt`: Screen für den Import/Export von Geschenkideen.
        - `about/AboutScreen.kt`: Anzeige von App-Informationen und Entwickler-Details.
        - `about/PrivacyPolicyScreen.kt`: Anzeige der Datenschutzerklärung.
- ### 📁 Models (`ui.model`)
    - `ContactUiModel.kt`: Immutable UI-Modell für Kontakte.
    - `HomeUiState.kt`: Gebündelter State für den Home-Screen.
    - `LabelManagementModel.kt`: Modell für die Label-Verwaltung.
    - `GiftIdea.kt`: Modell für Geschenkideen mit JSON- und Manipulations-Logik.
    - `SampleData.kt`: Zentraler Ort für Testdaten für Previews und Tests.
- ### 📁 UI Components (`ui.components`)
    - `LottieIllustration.kt`: Wiederverwendbare Komponente für Lottie-Animationen.
    - `ResponsiveLayout.kt`: Beinhaltet `AdaptiveContentContainer`, `AppResponsiveScaffold` und den globalen `LocalWindowWidthSizeClass` CompositionLocal-Provider zur flexiblen, abfragefreien Größenklassen-Weitergabe (Handy, Tablet, Chromebook).
- ### 📁 Theme (`ui.theme`)
    - `Theme.kt`, `Color.kt`, `Type.kt`: Design-System Definitionen.

## 📁 ViewModel (`viewmodel`)
- `HomeViewModel.kt`: Zuständig für die Kontaktliste, Suche, Filterung und den Home-Screen State.
- `NotificationViewModel.kt`: Verwaltung der Benachrichtigungsregeln und deren Synchronisation mit dem WorkManager.
- `OnboardingViewModel.kt`: Zuständig für den Onboarding-Status und Erststart-Prozess.
- `LabelViewModel.kt`: Spezielle Logik für die Label-Verwaltung und Konfiguration.
- `BackupViewModel.kt`: Logik für den Import und Export von Geschenkideen.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Erweiterungsfunktionen für LocalDate.
- `ContextExtensions.kt`: Hilfsfunktionen für die sichere Navigation im Android-Context.
- `WidgetUpdater.kt` & `BirthdayWidgetUpdater.kt`: Hilfsklassen zur Glance-unabhängigen Aktualisierung des App-Widgets.
- `NotificationScheduler.kt` & `NotificationSchedulerImpl.kt`: Hilfsklassen zur WorkManager-unabhängigen Steuerung von Hintergrund-Workern.
- `ImagePrefetcher.kt` & `ImagePrefetcherImpl.kt`: Hilfsklassen zur Coil-unabhängigen Vorladung von Bildern.

## 📁 UI Utilities (`ui.util`)
- `ContactActions.kt`: Zentraler Handler für externe System-Aktionen (Intents, Permissions).

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`, `BirthdayWidgetReceiver.kt`, `BirthdayWidgetWorker.kt`: Glance-basierte Widget-Komponenten.

## 🌍 Internationalisierung (I18n)
- `res/values/strings.xml`: Standard-Sprachressourcen (**Englisch**).
- `res/values-de/strings.xml`: Lokalisierte Sprachressourcen (**Deutsch**).

## 📁 Ressourcen (`res`)
- `res/raw/privacy_policy.md`: Datenschutzerklärung (EN).
- `res/raw-de/privacy_policy.md`: Datenschutzerklärung (DE).
- `res/raw/anim_contacts.json`: Lottie-Animation für das Onboarding.
- `res/mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`: Adaptive Launcher-Icon-Definitionen für Geräte ab API 26 (mit weißem Hintergrund und perfekter Skalierung).

## 📁 Custom Resource Source Set (`res-messenger`)
- `res-messenger/drawable/`: Enthält die plattformspezifischen Icons für Messenger wie WhatsApp, Signal, Discord, etc. zur dynamischen Einblendung in der Kontaktkarte, getrennt vom Standard-Ressourcenpfad zur Wahrung der Übersichtlichkeit.


## 🧪 Testing (`src/test` & `src/androidTest`)
- `MainDispatcherRule.kt`: JUnit-Rule zur Steuerung von Coroutine-Dispatchern in Tests.
- `data/local/ConvertersTest.kt`: Tests für TypeConverter.
- `data/local/MigrationTest.kt`: Automatisierte Datenbank-Migrationstests (V1 bis aktuell).
- `data/mapper/ContactMapperTest.kt`: Tests für die Transformation in UI-Modelle.
- `data/repository/NotificationRepositoryTest.kt`: Integrationstests für das Repository.
- `viewmodel/HomeViewModelSearchTest.kt`: Tests der Suchlogik im HomeViewModel.
- `viewmodel/HomeViewModelTest.kt`: Tests für Label-Filterung und State-Management.
- `ui/components/BirthdayItemInteractionTest.kt`: UI-Tests für die Interaktion mit Kontakt-Karten.
