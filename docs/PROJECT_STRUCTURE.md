# Project Structure: BirthdayBuddy

## 📁 Root
- `MainActivity.kt`: Schlanker Einstiegspunkt der App (~130 Zeilen). Verantwortlich für: Splash-Screen, Edge-to-Edge, Theme-Bereitstellung, globales Intent-Handling (z.B. Widget-Klicks) via `activityIntent`-State und Inaktivitäts-Reset über `LifecycleEventObserver`. Die Navigationslogik liegt in `AppNavHost.kt`, der ContentObserver in `ContactSyncEffect.kt`, die Routen in `NavRoutes.kt`.
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse zur Initialisierung der Dependency Injection und Konfiguration des WorkManagers.
- `AppViewModel.kt`: App-weites `@HiltViewModel`, das auf Activity-Ebene gehalten wird (Root-Package, da Activity-weit gültig).
- `AppViewModelTest.kt`: Tests für `AppViewModel`.
- `PROJECT_STATUS.md`: Dokumentation des aktuellen Entwicklungsstands, der Architektur-Constraints und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation des Projekts).
- `CHANGELOG.md`: Vollständige Historie aller signifikanten Änderungen und Feature-Releases.
- `scripts/`: Ordner für nützliche Hilfs- und Inspektionsskripte (z. B. Python-Skripte zur Validierung von emulatorbasierten Benachrichtigungsregeln und Datenbankabfragen).
- `.agents/`: Enthält agentenspezifische Konfigurationen, darunter Projekt-Richtlinien (`rules/project_guidelines.md`), die Skill-Registrierung (`skills.json`), lokale Workspace-Skills (`skills/`) und per Git-Submodule verlinkte Community-Skills (`external/`).

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
    - `ContactLabels.kt`: Zentrales `object` mit system-definierten Pseudo-Label-Identifiern (`LABEL_NO_BIRTHDAY`, `LABEL_ANNIVERSARY`, `LABEL_NAME_DAY`). Liegt im Data-Layer, da diese Werte als `LabelConfig`-Einträge in der Datenbank gespeichert werden und von mehreren Schichten genutzt werden (ViewModels, UI-Screens). Löst die frühere Abhängigkeit auf `HomeViewModel.companion`.
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

## 📁 Domain Layer (`domain`)
- ### 📁 Models (`domain.model`)
    - `GiftIdea.kt`: Reines Domänenmodell für Geschenkideen (id, text, isChecked) mit statischen Hilfsmethoden zum Hinzufügen, Sortieren und Umschalten von Ideen.
- ### 📁 Use Cases (`domain.usecase`)
    - `GetContactsUseCase.kt`: Kapselt die gesamte Filterlogik für die Home-Kontaktliste. Empfängt reaktive Inputs (Kontakte, Datum, Suchbegriff, Labels, Einstellungen) und gibt einen sortierten `Flow<List<ContactUiModel>>` zurück. Enthält die Pairing-Logik für Ehepaar-Jubiläen und die `LabelSettingsState`-Datenklasse. Annotiert mit `@Reusable` (kein Singleton nötig).
    - `GetAvailableLabelsUseCase.kt`: Kapselt die Logik zur Ermittlung der verfügbaren Filter-Labels für den Home-Screen (User-Labels, "Ohne Datum"-Pseudo-Label und weitere Ereignistyp-Labels wie Hochzeitstag und Namenstag). Annotiert mit `@Reusable`.
    - `GetPendingNotificationsUseCase.kt`: Evaluert die aktiven Benachrichtigungsregeln für den aktuellen Zeitpunkt und liefert die fälligen Termine (Geburtstage, Hochzeitstage mit Paar-Verknüpfung, Namenstage) zurück, die noch nicht geplant wurden.
    - `SnoozeNotificationUseCase.kt`: Kapselt die Logik für das Schlummern von Benachrichtigungen und delegiert dies an den plattformspezifischen Scheduler.
    - `GetCoupleSuggestionUseCase.kt`: Ermittelt reaktiv den ersten unignorierten Paar-Kopplungsvorschlag auf dem Home-Screen.
    - `LinkAsCoupleUseCase.kt`: Verknüpft zwei Kontakte als Ehepaar in den Datenbanken.
    - `UnlinkCoupleUseCase.kt`: Hebt die Paarverknüpfung zwischen zwei Kontakten auf.
    - `IgnoreCoupleSuggestionUseCase.kt`: Ignoriert einen Paarvorschlag dauerhaft.
    - `ExportGiftIdeasUseCase.kt`: JSON-exportiert Geschenkideen aus der Datenbank.
    - `ImportGiftIdeasUseCase.kt`: JSON-importiert Geschenkideen in die Datenbank.
    - `SyncCalendarUseCase.kt`: Synchronisiert Geburtstage, Hochzeitstage und Namenstage in den Systemkalender, sofern aktiviert.
    - `SetCalendarSyncEnabledUseCase.kt`: Konfiguriert die Kalendersynchronisation und führt Initialisierungs- oder Löschaktionen durch.
    - `UpdateCalendarColorUseCase.kt`: Aktualisiert die Systemkalenderfarbe für einen bestimmten Ereignistyp.

## 📁 UI Layer (`ui`)

- ### 📁 Screens (`ui.screens`)
    - #### 📁 Home (`home`)
        - `HomeScreen.kt`: Einstiegspunkt und koordinierende Komponente des Hauptbildschirms.
        - `HomeContent.kt`: Reine UI-Layout Komponente für den Homescreen.
        - `HomeState.kt`: Plain State Holder für die UI-Logik (Scroll-Zustand, Fokus).
        - `HomeActions.kt`: Wrapper für Benutzeraktionen zur Reduzierung von Prop-Drilling.
        - `HomeViewModel.kt`: Zuständig für die Kontaktliste, Suche, Filterung und den Home-Screen State. Nutzt ein leichtgewichtiges MVI/UDF-Muster mit dem `HomeIntent` Interface und einem konsolidierten `UserUiState` Flow. **Feature-co-located** neben den zugehörigen Screen-Dateien.
        - ##### 📁 Components (`home.components`)
            - ###### 📁 List (`home.components.list`)
                - `BirthdayDatePickerDialog.kt`: Wiederverwendbarer, modularer Date-Picker Dialog für die bequeme Eingabe von Geburtstagen.
                - `BirthdayDetailPane.kt`: Detail-Paneel zur Anzeige aller Informationen (Details, Aktionen, Geschenkideen) eines Kontakts auf Tablets.
                - `BirthdayItem.kt`: Zentrales Koordinationselement der Kontaktkarte; verwaltet den Expand-State und delegiert an spezialisierte Sub-Komponenten (wie z.B. den BirthdayDatePickerDialog).
                - `BirthdayList.kt`: LazyColumn-Implementierung mit Optimierungen (Auto-Collapse beim Scrollen und Filterwechsel), intelligent unterdrückten Platzierungsanimationen (`skipPlacementAnimation`) bei Filter- und Suchwechseln sowie Einbettung der `LabelFilterBar` als scrollbare Kopfzeile.
                - `BirthdayQuotePlaceholder.kt`: Platzhalter-Paneel mit Zitat für den unselektierten Zustand auf Tablets.
                - `BirthdayStatus.kt`: Anzeige des Alters und der verbleibenden Tage.
                - `ConfettiEffect.kt`: Hochperformantes Partikelsystem für Geburtstags-Animationen.
                - `ContactImage.kt`: Komponente für das Kontaktbild oder die Initialen.
                - `FastScrollbar.kt`: Hochperformante Scrollbar mit Sub-Pixel-Präzision (unterstützt einen `headerCount`-Offset für Kopfzeilen). Intern sauber strukturiert durch private Sub-Composables (`ScrollbarBubble` und `ScrollbarTrackAndThumb`) zur Trennung von Layout, Gesten- und Zeichenlogik. Nutzt die `Popup`-API für die Bubble, um Clipping und Z-Order-Überlagerungen zu vermeiden.
                - `GiftIdeaList.kt`: Spezialisierte Komponente für die Inline-Verwaltung von Geschenkideen.
                - `LabelFilterBar.kt`: Chip-Leiste zur Filterung nach Labels (nun als scrollbarer Listen-Header in die BirthdayList integriert).
            - ###### 📁 TopBar (`home.components.topbar`)
                - `SearchBar.kt`: M3 SearchBar-Integration.
            - ###### 📁 Actions (`home.components.actions`)
                - `ContactActionRow.kt`: Reihe mit Messenger- und Kontakt-Aktionen.
                - `HomeFAB.kt`: Multifunktionaler FAB mit Morphing-Animation.
                - `MessengerApp.kt`: Enum zur Definition unterstützter Messenger und deren Branding.
    - #### 📁 Onboarding (`onboarding`)
        - `OnboardingScreen.kt`: Multi-Page Flow für die initiale Konfiguration.
        - `OnboardingViewModel.kt`: Zuständig für den Onboarding-Status und Erststart-Prozess. **Feature-co-located** neben `OnboardingScreen.kt`.
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
        - `labels/LabelViewModel.kt`: Logik für die Label-Verwaltung und Konfiguration. **Feature-co-located**.
        - `notifications/NotificationSettingsScreen.kt`: Konfiguration des Erinnerungssystems.
        - `notifications/NotificationViewModel.kt`: Verwaltung der Benachrichtigungsregeln und deren Synchronisation mit dem WorkManager. **Feature-co-located**.
        - `calendar/CalendarSettingsScreen.kt`: Screen zur detaillierten Kalender-Sync-Konfiguration.
        - `calendar/CalendarViewModel.kt`: ViewModel für die Kalender-Einstellungen. **Feature-co-located**.
        - `backup/BackupScreen.kt`: Screen für den Import/Export von Geschenkideen.
        - `backup/BackupViewModel.kt`: Logik für den Import und Export von Geschenkideen. **Feature-co-located**.
        - `theme/ThemeSettingsScreen.kt`: Einstellungsbildschirm zur Design-Auswahl.
        - `theme/ThemeViewModel.kt`: Hält und aktualisiert den UI-Zustand für das App-Design. **Feature-co-located**.
        - `otherevents/OtherEventsSettingsScreen.kt`: Screen zur Aktivierung des Features für weitere Ereignisse.
        - `sync/SyncSettingsScreen.kt`: Screen zur manuellen Synchronisierung der Kontakte.
        - `about/AboutScreen.kt`: Anzeige von App-Informationen.
        - `about/PrivacyPolicyScreen.kt`: Anzeige der Datenschutzerklärung.
- ### 📁 Models (`ui.model`)
    - `ContactUiModel.kt`: Immutable UI-Modell für Kontakte.
    - `BirthdayTier.kt`: Typsicheres Enum zur Klassifizierung des visuellen Tiers eines Kontakts (`MILESTONE_GOLD`, `MILESTONE_SILVER`, `CHILD`, `REGULAR`). Die Berechnung erfolgt einmalig in `ContactMapper` via `BirthdayTier.from(nextAge?)` und wird über `ContactUiModel.birthdayTier` an die UI übergeben. Ersetzt die doppelte Inline-Logik in `BirthdayItem.kt`.
    - `EventType.kt`: Typsicheres Enum zur Diskriminierung des aktiven Ereignistyps (`BIRTHDAY`, `ANNIVERSARY`, `NAME_DAY`). Ersetzt den zuvor verwendeten stringly-typed Ansatz in `HomeViewModel` und `ContactMapper`.
    - `HomeUiState.kt`: Gebündelter State für den Home-Screen.
    - `CalendarUiState.kt`: Gebündelter State für den Kalender-Einstellungs-Screen.
    - `NotificationUiState.kt`: Gebündelter State für den Benachrichtigungs-Einstellungs-Screen.
    - `ThemeUiState.kt`: Gebündelter State für den Design-Einstellungs-Screen.
    - `LabelManagementModel.kt`: Modell für die Label-Verwaltung.
    - `GiftIdea.kt`: Reines Domänenmodell für Geschenkideen (id, text, isChecked) mit Manipulations-Logik (Hinzufügen, Sortieren, Umschalten).
    - `CoupleSuggestionUiModel.kt`: Immutable UI-Modell für Paar-Vorschläge, entkoppelt die UI-Schicht von der Room-Entity.
    - `SampleData.kt`: Zentraler Ort für Testdaten für Previews und Tests.
- ### 📁 Navigation (`ui.navigation`)
    - `NavRoutes.kt`: Alle 12 typsicheren, serialisierbaren `NavKey`-Routenobjekte (`Home`, `Settings`, `LabelSettings`, `NotificationSettings`, etc.). Zentrale Quelle der Navigationsstruktur.
    - `AppNavHost.kt`: Zentrales Navigations-Composable. Verwaltet den `NavDisplay` (Navigation 3) inklusive Screen-zu-Screen-Transitions (Push/Pop/PredictiveBack mit Parallax-Effekt) und das vollständige Route-zu-Screen-Mapping mit ViewModel-Verknüpfungen.
- ### 📁 UI Components (`ui.components`)
    - `ColorPickerDialog.kt`: Wiederverwendbare, premium Farbauswahl-Komponente mit HSV-Farbraum-Koordinaten (Sättigung/Helligkeit), Hue-Slider, HEX-Texteingabe und Live-Vorschau.
    - `ContactSyncEffect.kt`: Composable Effect, der Änderungen im System-Adressbuch beobachtet und `onSyncNeeded` mit einem 1-Sekunde-Debounce aufruft. Kapselt den `ContentObserver` und deregistriert ihn automatisch mit dem Compose-Lifecycle.
    - `ResponsiveLayout.kt`: Beinhaltet `AdaptiveContentContainer`, `AppResponsiveScaffold` und den globalen `LocalWindowWidthSizeClass` CompositionLocal-Provider zur flexiblen, abfragefreien Größenklassen-Weitergabe (Handy, Tablet, Chromebook).
    - `AppSwitch.kt`: Wiederverwendbare, standardisierte Switch-Komponente, die das Standard-Material-3-Design anwendet und bei Aktivierung automatisch ein Häkchen-Symbol anzeigt.
    - `SettingsComponents.kt`: Wiederverwendbare UI-Komponenten für Einstellungsseiten (`SettingsSectionHeader`, `SettingsCard`, `SettingsSwitchRow`, `SettingsClickableRow`).
    - `StepItem.kt`: Wiederverwendbare, zweispaltige Komponente zur Darstellung von Setup-Schritten mit Nummern oder Icons.
- ### 📁 UI Illustrations (`ui.illustrations`)
    - `WelcomeIllustration.kt`: Native Custom-Animations-Komponente für das Begrüßungs-Onboarding.
    - `ContactsIllustration.kt`: Wiederverwendbare, rein native Custom-Animations-Komponente für Kontakte-Berechtigungen (verschoben aus `ui/components/`).
    - `NotificationsIllustration.kt`: Wiederverwendbare native Custom-Animations-Komponente für Benachrichtigungs-Berechtigungen und Vorschauen.
    - `CalendarIllustration.kt`: Wiederverwendbare native Custom-Animations-Komponente für Kalender-Berechtigungen und Events.
    - `CalendarGuideIllustration.kt`: Wiederverwendbare native Custom-Animations-Komponente für die Kalender-Konfigurationsanleitung.
    - `ReadyIllustration.kt`: Native Custom-Animations-Komponente mit Konfetti-Effekt für den Abschluss.
- ### 📁 Theme (`ui.theme`)
    - `Theme.kt`, `Color.kt`, `Type.kt`, `Shapes.kt`: Design-System Definitionen (Farbschemata, Typografie, Eckenradien), custom Farbschemata für Akzentfarben und AMOLED-Erweiterung.

## 📁 Notification-Infrastruktur (`notification`)
- `NotificationWorker.kt`: Hintergrund-Prozess für die Benachrichtigungs-Logik (`@AssistedInject` Hilt-Worker).
- `SnoozeWorker.kt`: Hintergrund-Prozess für die "Später"-Funktion.
- `NotificationActionReceiver.kt`: Verarbeitet Klicks auf Benachrichtigungs-Buttons (Snooze, Erledigt, Dismissed). Im `AndroidManifest.xml` als Receiver registriert.
- `NotificationHelper.kt`: Hilfsklasse für den System-Notification-Manager (Erstellen und Anzeigen von Benachrichtigungen).

> [!NOTE]
> Dieses Package enthält **keine UI-Komponenten**. Die UI-seitigen Elemente (`EditRuleDialog.kt`, `NotificationRuleItem.kt`) verbleiben in `ui/screens/settings/notifications/components/`.

## 📁 ViewModel (feature-co-located)
> ViewModels liegen **direkt neben ihren Screen-Dateien** (feature-co-located), nicht in einem globalen `viewmodel/`-Package.
> Das Root-Package enthält `AppViewModel.kt` (Activity-weit, keinem einzelnen Screen zugehörig).
> Alle anderen ViewModels befinden sich in ihrem jeweiligen Feature-Package unter `ui/screens/`.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Erweiterungsfunktionen für LocalDate.
- `ContextExtensions.kt`: Hilfsfunktionen für die sichere Navigation im Android-Context.
- `IntentExtras.kt`: Zentrales `object` mit allen `const val`-Schlüsseln für Intent-Extras (`SCROLL_TO_TOP`, `NAVIGATE_TO_NOTIFICATIONS`, `OPEN_SEARCH`, `OPEN_ADD_CONTACT`). Vermeidet duplizierte String-Literale zwischen Widget, NotificationHelper und MainActivity.
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
- `data/repository/ContactRepositoryImplTest.kt`: JVM Unit-Tests für `ContactRepositoryImpl` (Abdeckung von allContacts, labelsEnabled, syncContacts, addGiftIdea, und labelConfigs).
- `util/DateUtilsTest.kt`: Logiktests für Datumsberechnungen (Alter, Tage bis Geburtstag, etc.).
- `domain/usecase/GetPendingNotificationsUseCaseTest.kt`: JVM Unit-Tests zur Überprüfung der Benachrichtigungsregeln und Fälligkeits-Kalkulation.
- `domain/usecase/SnoozeNotificationUseCaseTest.kt`: JVM Unit-Tests zur Überprüfung der Schlummer-Delegation.
- `domain/usecase/GetCoupleSuggestionUseCaseTest.kt`: JVM Unit-Tests zur Überprüfung der Ehepartner-Vorschlagsermittlung (inkl. ungleicher Nachnamen).
- `domain/usecase/ExportGiftIdeasUseCaseTest.kt`: JVM Unit-Tests zum Geschenkideen-Export.
- `domain/usecase/ImportGiftIdeasUseCaseTest.kt`: JVM Unit-Tests zum Geschenkideen-Import.
- `domain/usecase/SetCalendarSyncEnabledUseCaseTest.kt`: JVM Unit-Tests zur Aktivierung/Deaktivierung der Kalendersynchronisation.
- `AppViewModelTest.kt`: Tests für `AppViewModel`: Verifikation der initialen `AppSettings`-Emission, reaktiver Settings-Propagation und einmaligem `syncScheduling()`-Aufruf im `init`.
- `ui/screens/home/HomeViewModelGiftIdeaTest.kt`: Tests für Geschenkideen- und Geburtstags-Intents im `HomeViewModel`. **Feature-co-located** neben `HomeViewModel.kt`.
- `ui/screens/home/HomeViewModelSearchTest.kt`: Tests der Such- und Filterlogik im `HomeViewModel`. **Feature-co-located** neben `HomeViewModel.kt`.
- `ui/screens/home/HomeViewModelTest.kt`: Tests für das reaktive State-Management und die UI-Filterung im `HomeViewModel`. **Feature-co-located** neben `HomeViewModel.kt`.

### 📁 Instrumentierte Integrationstests (`app/src/androidTest`)
Diese Tests erfordern ein Android-Gerät oder einen Emulator (z. B. für Room-Datenbanken, Systemdienste oder Compose-UI-Interaktionen).
- `data/local/MigrationTest.kt`: Automatisierte Datenbank-Migrationstests für die Hauptdatenbank (V5 bis aktuell).
- `data/local/SettingsMigrationTest.kt`: Automatisierte Datenbank-Migrationstests für die Einstellungsdatenbank (V2 bis V6).
- `data/repository/NotificationRepositoryTest.kt`: Integrationstests für das Room-basierte Erinnerungs-Repository.
- `data/repository/SystemContactDataSourceTest.kt`: Tests für den realen Android-Content-Provider des Adressbuchs.
- `data/repository/ContactRepositoryCoupleLinkTest.kt`: Integrationstests für Ehepaar-Verknüpfungen im ContactRepository.
- `data/repository/ContactRepositoryGiftIdeaTest.kt`: Integrationstests für Geschenkideen-Operationen im ContactRepository.
- `data/repository/CalendarSyncRepositoryTest.kt`: Integrationstests für die Kalender-Synchronisation im CalendarSyncRepository.
- `ui/components/BirthdayItemInteractionTest.kt`: Compose UI-Tests für die Interaktionen mit Kontakt-Karten.
- `ui/screens/settings/notifications/components/NotificationHelperTest.kt`: Tests zum Erzeugen und Validieren von System-Notifications.
