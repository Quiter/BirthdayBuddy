# Project Structure: BirthdayBuddy

## 📁 Root & App Scope
- `MainActivity.kt`: Schlanker Einstiegspunkt der App (Splash-Screen, Edge-to-Edge, Theme, `IntentParser` -> `AppAction`).
- `BirthdayBuddyApplication.kt`: Hilt-Application-Klasse zur DI- und WorkManager-Initialisierung.
- `AppViewModel.kt`: Activity-weites ViewModel für globale `AppSettings`, Sync-Triggering & `AppAction`-Buffering.
- `BootReceiver.kt`: BroadcastReceiver für System-Events (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, etc.) zur automatisierten Neuplanung von Alarmen & Widgets.
- Meta & Dokumentation: `PROJECT_STATUS.md`, `PROJECT_STRUCTURE.md`, `CHANGELOG.md`, `scripts/` (Hilfsskripte), `.agents/` (Projekt-Richtlinien & Skills).

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Modul für Singleton-Instanzen, `@Reusable` DAO-Bindings und Coroutine-Dispatcher (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`, `@ApplicationScope`).
- `HelperBindingsModule.kt`: Hilt-Modul für Repositories, Providers, Schedulers und Updaters.

## 📁 Data Layer (`data`)
- **Local Database (`data.local`)**:
  - `AppDatabase.kt` (Systemkontakte-Cache, Benachrichtigungen) & `SettingsDatabase.kt` (Einstellungen, Regeln, Labels, Geschenkideen-Backup).
  - Entitäten: `AppSettings`, `Contact`, `ContactUserData`, `LabelConfig`, `NotificationRule`, `PendingNotification`.
  - DAOs: `AppSettingsDao`, `ContactDao`, `ContactUserDataDao`, `LabelConfigDao`, `NotificationRuleDao`, `PendingNotificationDao`.
  - TypeConverter: `Converters` (LocalDate, Listen), `GiftIdeaConverters`.
  - `ContactLabels.kt`: Deprecated Identifier-Delegation (Rückwärtskompatibilität).
- **Repository Implementierungen (`data.repository`)**:
  - `ContactRepositoryImpl.kt`: Kontaktsynchronisation, Caching & Label-Management (Room, ContentResolver).
  - `GiftIdeaRepositoryImpl.kt`: Geschenkideen-CRUD, atomarer DB-Commit & JSON-Backup.
  - `CoupleRepositoryImpl.kt`: Verknüpfung verheirateter Ehepartner & Vorschlagsverwaltung.
  - `SettingsRepositoryImpl.kt`: Thread-sichere Mutation von `AppSettings` via `Mutex`.
  - `NotificationRepositoryImpl.kt`, `CalendarSyncRepositoryImpl.kt`, `TimeRepositoryImpl.kt`.
  - Helpers & Data Sources: `GiftIdeaBackupManager`, `BirthdayWidgetUpdater`, `NotificationSchedulerImpl`, `SystemCalendarDataSourceImpl`, `SystemCalendarInfo`, `SystemContactDataSource`.
- **Mapper & Provider (`data.mapper`, `data.permission`, `data.util`)**:
  - Mapper: `ContactDbMapper`, `AppSettingsMapper`.
  - Berechtigungen: `AndroidPermissionChecker.kt`.
  - Utilities: `AndroidCalendarStringProvider.kt`, `AndroidDeviceRegionProvider.kt`.

## 📁 Domain Layer (`domain`)
- **Repositories (`domain.repository`)**: Interfaces für `ContactRepository`, `GiftIdeaRepository`, `CoupleRepository`, `SettingsRepository`, `NotificationRepository`, `CalendarSyncRepository`, `TimeRepository`, `NotificationScheduler`, `WidgetUpdater`.
- **Permission & Models (`domain.permission`, `domain.model`)**:
  - `PermissionChecker`: Plattformunabhängige Berechtigungsprüfung.
  - Modelle: `ContactLabels` (Pseudo-Labels), `CoupleSuggestion`, `GiftIdea`, `EventType` (Enum), `MessengerApp` (Enum).
- **Use Cases (`domain.usecase`)**:
  - Kontakte & Filter: `GetContactsUseCase`, `GetAvailableLabelsUseCase`.
  - Benachrichtigungen: `GetPendingNotificationsUseCase`, `SnoozeNotificationUseCase`, `SyncNotificationSchedulingUseCase`, `MarkNotificationAsDoneUseCase`, `DismissNotificationUseCase`, `CleanupOldNotificationsUseCase`.
  - Paar-Verknüpfungen: `GetCoupleSuggestionUseCase`, `LinkAsCoupleUseCase`, `UnlinkCoupleUseCase`, `IgnoreCoupleSuggestionUseCase`.
  - Geschenkideen: `ExportGiftIdeasUseCase`, `ImportGiftIdeasUseCase`.
  - Kalender & Widgets: `SyncCalendarUseCase`, `SetCalendarSyncEnabledUseCase`, `UpdateCalendarColorUseCase`, `ScheduleDailyWidgetUpdateUseCase`.
- **Utilities (`domain.util`)**: `CalendarStringProvider`, `ContactFilterLogic`, `DateUtils` (Datumsberechnungen), `DeviceRegionProvider`, `NotificationKeyUtils`, `PhoneNumberNormalizer` (E.164 Normalisierung).

## 📁 Platform Layer (`platform`)
- **AppFunctions (`platform.appfunctions`)** *(Android 16+ / AI-Agent Integration)*:
  - `BirthdayAppFunctionService.kt`: Abstrakter `AppFunctionService` für KI-Agenten & System-Shortcuts.
  - Modelle (`platform.appfunctions.model`): `@AppFunctionSerializable` DTOs `UpcomingBirthday.kt`, `ContactBirthday.kt`.
  - Funktionen: `getUpcomingBirthdays`, `getContactBirthday`, `sendBirthdayMessage`, `addBirthdayToContact`.

## 📁 UI Layer (`ui`)
- **Navigation & Central (`ui.navigation`)**:
  - `NavRoutes.kt`: Typsichere Navigation-3-Routen (`NavKey`).
  - `AppAction.kt`: Typsichere App- und Navigations-Events.
  - `AppNavHost.kt`: Zentrales Nav-Host-Composable (Transitions, Route-to-Screen-Mapping).
- **Mappers & UI-Modelle (`ui.mapper`, `ui.model`)**:
  - Mapper: `ContactUiMapper`, `CoupleSuggestionUiMapper`.
  - Modelle: `ContactUiModel`, `CoupleSuggestionUiModel`, `HomeUiState`, `BirthdayTier` (Enum), `LabelManagementModel`, `SampleData`.
- **UI Components & Illustrations (`ui.components`, `ui.illustrations`)**:
  - Komponenten: `ColorPickerDialog`, `ResponsiveLayout` (WindowSizeClass & Responsive Scaffold), `AppSwitch`, `SettingsComponents`, `StepItem`.
  - Native Illustrationen: `WelcomeIllustration`, `ContactsIllustration`, `NotificationsIllustration`, `CalendarIllustration`, `CalendarGuideIllustration`, `ReadyIllustration`.
- **Theme (`ui.theme`)**: `Theme.kt`, `Color.kt`, `ColorPresets.kt`, `Type.kt`, `Shapes.kt`, `Dimensions.kt`.
- **Feature Screens (`ui.screens`)** *(Co-located ViewModels, States & Components)*:
  - **Home (`home/`)**: `HomeScreen`, `HomeContent`, `HomeNavKey`, `HomeState`, `HomeActions`, `HomeIntent`, `HomeUiEvent`, `HomeViewModel`.
    - Components: `BirthdayList`, `BirthdayItem`, `BirthdayDetailPane`, `GiftIdeaList`, `FastScrollbar`, `ScrollbarBubble`, `LabelFilterBar`, `LabelSidebar`, `HomeTopBar`, `SearchBar`, `HomeFAB`, `ContactActionRow`, `MessengerAppUi`, `ConfettiEffect`, etc.
  - **Onboarding (`onboarding/`)**: `OnboardingScreen`, `OnboardingViewModel`, `OnboardingUiState` & Pages (`WelcomePage`, `ContactsPage`, `NotificationsPage`, `CalendarPage`, `CalendarGuidePage`, `ReadyPage`, `OnboardingFooter`).
  - **Settings (`settings/`)**: `SettingsScreen` & Sub-Features:
    - `labels/`: `LabelSettingsScreen`, `LabelViewModel`, `LabelUiState`.
    - `notifications/`: `NotificationSettingsScreen`, `NotificationViewModel`, `NotificationUiState`.
    - `calendar/`: `CalendarSettingsScreen`, `CalendarViewModel`, `CalendarUiState`.
    - `backup/`: `BackupScreen`, `BackupViewModel`, `BackupUiState`.
    - `theme/`: `ThemeSettingsScreen`, `ThemeViewModel`, `ThemeUiState`.
    - `otherevents/`: `OtherEventsSettingsScreen`.
    - `sync/`: `SyncSettingsScreen`, `SyncViewModel`.
    - `about/`: `AboutScreen`, `PrivacyPolicyScreen`.

## 📁 Notification-Infrastruktur (`notification`)
- `NotificationWorker.kt`: Worker für Benachrichtigungs-Verarbeitung (`@AssistedInject`).
- `NotificationAlarmReceiver.kt`: Receiver für exakte Alarme (`AlarmManager`).
- `SnoozeWorker.kt`: Hintergrund-Prozess für Schlummer-Funktion.
- `NotificationActionReceiver.kt`: Interaktions-Handler für Notification-Buttons (Snooze, Erledigt, Verworfen).
- `NotificationHelper.kt` & `NotificationTextFormatter.kt`: System-Notification-Manager Wrapper & Textgenerierung.

## 📁 Utilities (`util` & `ui.util`)
- `util/`: `AlarmScheduler.kt`, `JsonUtils.kt` (SSOT JSON), `DateUtils.kt`, `StringUtils.kt`, `ContextExtensions.kt`, `IntentExtras.kt`, `IntentParser.kt`, `MessengerUtils.kt`, `WidgetUpdater.kt`, `NotificationScheduler.kt`.
- `ui.util/`: `ContactActions.kt` (System-Intents & Aktionen).

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`, `BirthdayWidgetReceiver.kt`, `BirthdayWidgetWorker.kt`, `WidgetLayoutHelper.kt`: Glance-basierte Home-Screen-Widgets & Layoutberechnungen.
- `WidgetUpdateAlarmReceiver.kt`: Exakter Mitternacht-Alarm-Receiver (00:01 Uhr) zur Widget-Aktualisierung.

## 🌍 Ressourcen (`res`, `res-messenger`)
- `res/values/strings.xml` (EN) & `res/values-de/strings.xml` (DE).
- `res/raw/privacy_policy.md` (EN) & `res/raw-de/privacy_policy.md` (DE).
- `res-messenger/drawable/`: Marken-Icons für Messenger-Apps (WhatsApp, Signal, etc.).

## 🧪 Testing (`app/src/test` & `app/src/androidTest`)

### 📁 Lokale JVM Unit-Tests (`app/src/test`)
- **Infrastructure & Root**: `MainDispatcherRule.kt`, `AppViewModelTest.kt`, `BootReceiverTest.kt`.
- **Data Layer**: `ContactDaoTest`, `PendingNotificationDaoTest`, `ConvertersTest`, `GiftIdeaConvertersTest`, DB-Mapper-Tests, Repository-Tests (`ContactRepositoryImplTest`, `CoupleRepositoryImplTest`, `GiftIdeaRepositoryImplTest`, `NotificationRepositoryImplTest`, `CalendarSyncRepositoryImplTest`, etc.).
- **Domain Layer**: Tests für Use Cases (`GetContactsUseCaseTest`, `GetPendingNotificationsUseCaseTest`, etc.) und Utilities (`PhoneNumberNormalizerTest`, `NotificationKeyUtilsTest`, `ContactFilterLogicTest`).
- **Platform & Notification**: `BirthdayAppFunctionServiceTest`, `NotificationAlarmReceiverTest`, `NotificationActionReceiverTest`, `NotificationTextFormatterTest`, `NotificationWorkerTest`, `SnoozeWorkerTest`.
- **UI & ViewModels**: Feature-co-located ViewModel-Tests (`HomeViewModelTest`, `OnboardingViewModelTest`, `BackupViewModelTest`, `CalendarViewModelTest`, `LabelViewModelTest`, `ThemeViewModelTest`, `SyncViewModelTest`), Compose-Komponenten-Tests (`GiftIdeaListTest`, `SearchBarTest`, `FastScrollbarBoundsTest`).
- **Utilities & Widgets**: `AlarmSchedulerTest`, `IntentParserTest`, `IntentExtrasTest`, `WidgetLayoutHelperTest`, `BirthdayWidgetWorkerTest`.

### 📁 Instrumentierte Integrationstests (`app/src/androidTest`)
- DB-Migrationstests: `MigrationTest.kt` (V1–V11), `SettingsMigrationTest.kt` (V1–V10).
- Provider & Integration: `NotificationRepositoryTest`, `SystemContactDataSourceTest`, `CalendarSyncRepositoryTest`, `BirthdayItemInteractionTest`.

### 📸 Screenshot-Tests (`app/src/test/java/.../screenshot/`)
Roborazzi-basierte Screenshot-Tests (JVM, ohne Emulator).
- **Testklassen**: `BaseScreenshotTest`, `HomeContentScreenshotTest`, `BirthdayListScreenshotTest`, `BirthdayItemScreenshotTest`, `OnboardingScreenshotTest`, `SettingsOverviewScreenshotTest`, `ThemeSettingsScreenshotTest`, `CalendarSettingsScreenshotTest`, `NotificationSettingsScreenshotTest`, `BackupScreenshotTest`, `LabelSettingsScreenshotTest`, `SyncScreenshotTest`.
- **Golden-Images**: `app/src/test/snapshots/images/`
- **Größen-Matrix**: `phoneSize` (360x640 dp), `phoneTallSize` (360x1000 dp), `tabletSize` (840x640 dp).
- **Gradle-Tasks**: `./gradlew recordRoborazziDebug`, `./gradlew verifyRoborazziDebug`, `./gradlew compareRoborazziDebug`.
