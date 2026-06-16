# Project Structure: BirthdayBuddy

## 📁 Root
- `MainActivity.kt`: Haupteinstiegspunkt der App. Regelt das Navigations-Hosting (NavHost), das globale Intent-Handling (z.B. Widget-Klicks) über Compose-seitiges `activityIntent` State-Handling, automatische Filter-Resets bei Inaktivität über einen batterieschonenden `LifecycleEventObserver`, und die Echtzeit-Synchronisation bei Änderungen im System-Adressbuch via `ContentObserver`.
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse zur Initialisierung der Dependency Injection und Konfiguration des WorkManagers.
- `PROJECT_STATUS.md`: Dokumentation des aktuellen Entwicklungsstands, der Architektur-Constraints und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation des Projekts).
- `CHANGELOG.md`: Vollständige Historie aller signifikanten Änderungen und Feature-Releases.
- `scripts/`: Ordner für nützliche Hilfs- und Inspektionsskripte (z. B. Python-Skripte zur Validierung von emulatorbasierten Benachrichtigungsregeln und Datenbankabfragen).

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Modul zur Bereitstellung von Singleton-Instanzen und performanten, leichtgewichtigen `@Reusable` DAO-Bindings zur Steigerung der DI-Performanz.
- `HelperBindingsModule.kt`: Hilt-Modul zur Bereitstellung der Singleton-Bindings für Hilfsklassen (WidgetUpdater, NotificationScheduler).

## 📁 Data Layer (`data`)
- ### 📁 Local (`data.local`)
    - `AppDatabase.kt`: Room-Datenbank für flüchtige/große Daten (Kontakte-Cache, Benachrichtigungs-Nachverfolgung).
    - `SettingsDatabase.kt`: Room-Datenbank für persistente Benutzer-Einstellungen (Regeln, Labels, GiftIdeas-Backup).
    - `AppSettings.kt`: Entity für globale App-Konfigurationen (Benachrichtigungs-Status, letzter Sync-Zeitpunkt, ignorierte Ehepaar-Kopplungsvorschläge, Theme-Modus, AMOLED-Option, Akzentfarbe).
    - `AppSettingsDao.kt`: Datenzugriffsobjekt für die App-Einstellungen.
    - `Contact.kt`: Haupt-Entity für den System-Kontakt-Cache (inkl. `spouseLookupKey` zur Verknüpfung verheirateter Ehepartner).
    - `ContactDao.kt`: DAO für Kontakte mit Unterstützung für Batch-Operationen und atomare Refreshes.
    - `ContactUserData.kt`: Entity für benutzerdefinierte Zusatzdaten (z. B. Geschenkideen und Ehepaar-Verknüpfungs-Schlüssel), die unabhängig vom Cache persistiert werden.
    - `ContactUserDataDao.kt`: DAO für benutzerdefinierte Kontaktdaten.
    - `Converters.kt`: TypeConverter für Basis-Typen (LocalDate, Listen).
    - `GiftIdeaConverters.kt`: Spezialisierte TypeConverter für Geschenkideen-Listen.
    - `LabelConfig.kt`: Entity zur Verwaltung der Sichtbarkeit und Filter-Regeln für Kontakt-Labels.
    - `LabelConfigDao.kt`: DAO für Label-Konfigurationen.
    - `NotificationRule.kt`: Entity für dynamische Benachrichtigungszeitpunkte (Tage vorher, Uhrzeit).
    - `NotificationRuleDao.kt`: DAO für die Verwaltung der Erinnerungsregeln.
    - `PendingNotification.kt`: Entity zur Nachverfolgung aktiver System-Benachrichtigungen.
    - `PendingNotificationDao.kt`: DAO für die Verwaltung noch nicht quittierter Erinnerungen.
    - `PotentialCouple.kt`: Datenklasse zur Repräsentation eines potenziellen Ehepaars, das denselben Hochzeitstag teilt.
- ### 📁 Repository (`data.repository`)
    - `CalendarSyncRepository.kt`: Orchestriert die Synchronisation von Geburtstagen, Namenstagen und Hochzeitstagen mit dem System-Kalender unter Verwendung von `SystemCalendarDataSource`.
    - `ContactRepository.kt`: Orchestriert den Datenfluss zwischen Room-DB und der System-Kontaktquelle; implementiert die Sync-Logik, reaktive Widget-Updates und Geschäftslogik für Geschenkideen.
    - `GiftIdeaBackupManager.kt`: Spezialisierte Klasse für den JSON-basierten Im- und Export von Geschenkideen.
    - `NotificationRepository.kt`: Zentraler Zugriff auf Benachrichtigungsregeln und persistente App-Einstellungen.
    - `SystemCalendarInfo.kt`: Datenklasse zur Kapselung von Kalender-Metadaten (z. B. ID, Name, Account-Typ, Farbe) zur Entkopplung der Datenquellen von Android-Datenbankcursorn.
    - `SystemCalendarDataSource.kt`: Interface für den Zugriff auf den Kalender-Provider (CRUD-Operationen auf Kalender- und Event-Tabellen).
    - `SystemCalendarDataSourceImpl.kt`: Konkrete Android-Implementierung von `SystemCalendarDataSource`, die den `ContentResolver` nutzt, um Kalender und Events im System zu verwalten.
    - `SystemContactDataSource.kt`: Kapselt den Low-Level Zugriff auf den Android ContentResolver (Kontakte, Gruppen, Events).
    - `TimeRepository.kt`: Reaktive Zeitquelle, die bei Datumswechseln (Mitternacht) automatische UI-Updates triggert.
- ### 📁 Mapper (`data.mapper`)
    - `ContactMapper.kt`: Reine Logik-Komponente zur Transformation von Datenbank-Entitäten in Anzeige-Modelle (mittels `@Reusable` für effiziente DI-Instanziierung optimiert).

## 📁 UI Layer (`ui`)
- ### 📁 Screens (`ui.screens`)
    - #### 📁 Home (`home`)
        - `HomeScreen.kt`: Container-Composable des Hauptbildschirms; verwaltet den UI-State. Verwendet `rememberUpdatedState` für sichere Navigationstargets, führt Bild-Preloading via Coil aus, und nutzt `ListDetailPaneScaffold` für ein adaptives Master-Detail-Layout.
        - `HomeState.kt`: Plain State Holder für die UI-Logik (Scroll-Zustand, Fokus).
        - `HomeActions.kt`: Wrapper für Benutzeraktionen zur Reduzierung von Prop-Drilling.
        - ##### 📁 Components (`home.components`)
            - ###### 📁 List (`home.components.list`)
                - `BirthdayDatePickerDialog.kt`: Wiederverwendbarer, modularer Date-Picker Dialog für die bequeme Eingabe von Geburtstagen.
                - `BirthdayDetailPane.kt`: Detail-Paneel zur Anzeige aller Informationen (Details, Aktionen, Geschenkideen) eines Kontakts auf Tablets.
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
            - `CalendarPage.kt`: Individuelle Onboarding-Seite zur Aktivierung des Kalender-Syncs und Einleitung der Berechtigungsanfrage.
            - `CalendarGuidePage.kt`: Individuelle Onboarding-Seite mit einer bebilderten Schritt-für-Schritt Anleitung zur Aktivierung des Kalenders in der externen Kalender-App und einem Direktlink per Intent.
            - `ReadyPage.kt`: Individuelle Onboarding-Seite für den Abschluss.
            - `OnboardingFooter.kt`: Navigations-Footer mit Dots, vollflächig gerendert (`fillMaxWidth`) für konsistente responsive Darstellung auf breiten Bildschirmen.
    - #### 📁 Settings (`settings`)
        - `SettingsScreen.kt`: Haupteinstellungsmenü.
        - `labels/LabelSettingsScreen.kt`: Verwaltung der Label-Sichtbarkeit.
        - `notifications/NotificationSettingsScreen.kt`: Konfiguration des Erinnerungssystems.
        - `calendar/CalendarSettingsScreen.kt`: Screen zur detaillierten Kalender-Sync-Konfiguration mit einer interaktiven Schritt-für-Schritt Anleitung (`SetupStepsCard`) und einem Direktlink zur Standard-Kalender-App.
        - ##### 📁 Components (`notifications.components`)
            - `NotificationRuleItem.kt`: UI-Element für eine einzelne Benachrichtigungsregel.
            - `EditRuleDialog.kt`: Dialog zum Bearbeiten/Erstellen von Erinnerungsregeln. Ermöglicht eine präzise, textbasierte Vorwarnzeit-Eingabe in Tagen oder Wochen (Einheiten-Auswahl per RadioButtons) mit integriertem System-TimePicker.
            - `NotificationWorker.kt`: Hintergrund-Prozess für die Benachrichtigungs-Logik.
            - `SnoozeWorker.kt`: Hintergrund-Prozess für die "Später"-Funktion.
            - `NotificationActionReceiver.kt`: Verarbeitet Klicks auf Benachrichtigungs-Buttons.
            - `NotificationHelper.kt`: Hilfsklasse für den System-Notification-Manager.
        - `backup/BackupScreen.kt`: Screen für den Import/Export von Geschenkideen.
        - `theme/ThemeSettingsScreen.kt`: Einstellungsbildschirm zur manuellen Auswahl des Designs (Hell/Dunkel/System), AMOLED Black-Option und Grid-Auswahl für Akzentfarben.
        - `otherevents/OtherEventsSettingsScreen.kt`: Screen zur Aktivierung und Konfiguration des Features für weitere Ereignisse (Hochzeitstage und Namenstage).
        - `sync/SyncSettingsScreen.kt`: Screen zur manuellen Synchronisierung der Kontakte mit Snackbar-Bestätigung.
        - `about/AboutScreen.kt`: Anzeige von App-Informationen und Entwickler-Details.
        - `about/PrivacyPolicyScreen.kt`: Anzeige der Datenschutzerklärung.
- ### 📁 Models (`ui.model`)
    - `ContactUiModel.kt`: Immutable UI-Modell für Kontakte.
    - `HomeUiState.kt`: Gebündelter State für den Home-Screen.
    - `CalendarUiState.kt`: Gebündelter State für den Kalender-Einstellungs-Screen.
    - `NotificationUiState.kt`: Gebündelter State für den Benachrichtigungs-Einstellungs-Screen.
    - `ThemeUiState.kt`: Gebündelter State für den Design-Einstellungs-Screen.
    - `LabelManagementModel.kt`: Modell für die Label-Verwaltung.
    - `GiftIdea.kt`: Modell für Geschenkideen mit JSON- und Manipulations-Logik.
    - `CoupleSuggestionUiModel.kt`: Immutable UI-Modell für Paar-Vorschläge, entkoppelt die UI-Schicht von der Room-Entity.
    - `SampleData.kt`: Zentraler Ort für Testdaten für Previews und Tests.
- ### 📁 UI Components (`ui.components`)
    - `ColorPickerDialog.kt`: Wiederverwendbare, premium Farbauswahl-Komponente mit HSV-Farbraum-Koordinaten (Sättigung/Helligkeit), Hue-Slider, HEX-Texteingabe und Live-Vorschau.
    - `LottieIllustration.kt`: Wiederverwendbare Komponente für Lottie-Animationen.
    - `ResponsiveLayout.kt`: Beinhaltet `AdaptiveContentContainer`, `AppResponsiveScaffold` und den globalen `LocalWindowWidthSizeClass` CompositionLocal-Provider zur flexiblen, abfragefreien Größenklassen-Weitergabe (Handy, Tablet, Chromebook).
- ### 📁 Theme (`ui.theme`)
    - `Theme.kt`, `Color.kt`, `Type.kt`, `Shapes.kt`: Design-System Definitionen (Farbschemata, Typografie, Eckenradien), custom Farbschemata für Akzentfarben und AMOLED-Erweiterung.

## 📁 ViewModel (`viewmodel`)
- `HomeViewModel.kt`: Zuständig für die Kontaktliste, Suche, Filterung und den Home-Screen State. Nutzt ein leichtgewichtiges MVI/UDF-Muster mit dem `HomeIntent` Interface und einem konsolidierten `UserUiState` Flow zur Vermeidung asynchroner Konflikte.
- `NotificationViewModel.kt`: Verwaltung der Benachrichtigungsregeln und deren Synchronisation mit dem WorkManager.
- `OnboardingViewModel.kt`: Zuständig für den Onboarding-Status und Erststart-Prozess.
- `CalendarViewModel.kt`: ViewModel für die Kalender-Einstellungen; steuert die Synchronisation und die Entfernung des Kalenders aus der App.
- `LabelViewModel.kt`: Spezialer Logik für die Label-Verwaltung und Konfiguration.
- `BackupViewModel.kt`: Logik für den Import und Export von Geschenkideen.
- `ThemeViewModel.kt`: Hält und aktualisiert den UI-Zustand für das App-Design.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Erweiterungsfunktionen für LocalDate.
- `ContextExtensions.kt`: Hilfsfunktionen für die sichere Navigation im Android-Context.
- `WidgetUpdater.kt` & `BirthdayWidgetUpdater.kt`: Hilfsklassen zur Glance-unabhängigen Aktualisierung des App-Widgets.
- `NotificationScheduler.kt` & `NotificationSchedulerImpl.kt`: Hilfsklassen zur WorkManager-unabhängigen Steuerung von Hintergrund-Workern.

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

### 📁 Lokale JVM Unit-Tests (`app/src/test`)
Diese Tests laufen ohne Emulator/Gerät direkt auf dem Entwicklungsrechner und sind sehr schnell (Kompilierung/Ausführung in Sekunden).
- `MainDispatcherRule.kt`: Coroutine-Rule zur Steuerung des Haupt-Dispatchers in Tests.
- `data/local/ConvertersTest.kt`: Tests für die TypeConverter (LocalDate, Listen).
- `data/local/GiftIdeaConvertersTest.kt`: Tests für Geschenkideen-TypeConverter.
- `data/mapper/ContactMapperTest.kt`: Tests für die Transformation von Datenbank-Entitäten in UI-Modelle (mit JVM-Safe Fallback für die Formatierung).
- `util/DateUtilsTest.kt`: Logiktests für Datumsberechnungen (Alter, Tage bis Geburtstag, etc.).
- `viewmodel/HomeViewModelSearchTest.kt`: Tests der Such- und Filterlogik im `HomeViewModel`.
- `viewmodel/HomeViewModelTest.kt`: Tests für das reaktive State-Management und die UI-Filterung im `HomeViewModel`.

### 📁 Instrumentierte Integrationstests (`app/src/androidTest`)
Diese Tests erfordern ein Android-Gerät oder einen Emulator (z. B. für Room-Datenbanken, Systemdienste oder Compose-UI-Interaktionen).
- `data/local/MigrationTest.kt`: Automatisierte Datenbank-Migrationstests für die Hauptdatenbank (V5 bis aktuell).
- `data/local/SettingsMigrationTest.kt`: Automatisierte Datenbank-Migrationstests für die Einstellungsdatenbank (V2 bis V6).
- `data/repository/NotificationRepositoryTest.kt`: Integrationstests für das Room-basierte Erinnerungs-Repository.
- `data/repository/SystemContactDataSourceTest.kt`: Tests für den realen Android-Content-Provider des Adressbuchs.
- `ui/components/BirthdayItemInteractionTest.kt`: Compose UI-Tests für die Interaktionen mit Kontakt-Karten.
- `ui/screens/settings/notifications/components/NotificationHelperTest.kt`: Tests zum Erzeugen und Validieren von System-Notifications.
