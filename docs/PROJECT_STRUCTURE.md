# Project Structure: BirthdayBuddy

## 📁 Root
- `MainActivity.kt`: Schlanker Einstiegspunkt der App. Verantwortlich für: Splash-Screen, Edge-to-Edge, Theme-Bereitstellung, Intent-Parsing via `IntentParser` in typsichere `AppAction`-Events, Weiterleitung an `AppViewModel` und Bereitstellung des UI-Trees. Die Navigationslogik liegt in `AppNavHost.kt`.
- `BirthdayBuddyApplication.kt`: Hilt-Application Klasse zur Initialisierung der Dependency Injection und Konfiguration des WorkManagers.
- `AppViewModel.kt`: App-weites `@HiltViewModel`, das auf Activity-Ebene gehalten wird (Root-Package, da Activity-weit gültig). Vollständig entkoppelt von Android-Framework-APIs; verwaltet reaktive `AppSettings`, triggert `syncScheduling()` & `WidgetUpdater.scheduleDailyUpdate()` und puffert `AppAction`-Events.
- `BootReceiver.kt`: BroadcastReceiver für Geräteneustart (`BOOT_COMPLETED`), App-Updates (`MY_PACKAGE_REPLACED`) sowie Zeitzonen- und Uhrzeitanpassungen (`TIMEZONE_CHANGED`, `TIME_SET`, `DATE_CHANGED`) zur automatischen Neuplanung von Benachrichtigungen via `syncScheduling()`.
- `AppViewModelTest.kt`: Tests für `AppViewModel`.
- `PROJECT_STATUS.md`: Dokumentation des aktuellen Entwicklungsstands, der Architektur-Constraints und Meilensteine.
- `PROJECT_STRUCTURE.md`: Diese Datei (Struktur-Dokumentation des Projekts).
- `CHANGELOG.md`: Vollständige Historie aller signifikanten Änderungen und Feature-Releases.
- `scripts/`: Ordner für nützliche Hilfs- und Inspektionsskripte (z. B. Python-Skripte zur Validierung von emulatorbasierten Benachrichtigungsregeln und Datenbankabfragen).
- `.agents/`: Enthält agentenspezifische Konfigurationen, darunter Projekt-Richtlinien (`rules/project_guidelines.md`), lokale Workspace-Skills (`skills/`) und per Git-Submodule verlinkte Community-Skills (`external/`).

## 📁 DI (`di`)
- `AppModule.kt`: Hilt-Modul zur Bereitstellung von Singleton-Instanzen, performanten leichtgewichtigen `@Reusable` DAO-Bindings sowie den **Coroutine-Dispatchern**:
  - `@IoDispatcher`: Bereitstellung von `Dispatchers.IO` für Datei-, Datenbank-, Netzwerk- und ContentResolver-Operationen.
  - `@DefaultDispatcher`: Bereitstellung von `Dispatchers.Default` für rechenintensive CPU-Operationen (Diffing, Filterung, O(n)-Transformationen).
  - `@MainDispatcher`: Bereitstellung von `Dispatchers.Main` für UI- und Main-Thread-Operationen.
  - `@ApplicationScope`: Bereitstellung eines prozessweiten CoroutineScopes mit SupervisorJob und Unhandled Exception Handler.
  - **Dispatcher-Injektions-Regel (Google Best Practice)**: Klassen (Repositories, DataSources, ViewModels) dürfen Dispatcher **nicht hardcoden** (`withContext(Dispatchers.IO)` ist verboten). Stattdessen wird `@IoDispatcher private val ioDispatcher: CoroutineDispatcher` per Konstruktor injiziert, um 100% deterministische Unit-Tests (via `TestDispatcher`) zu ermöglichen.
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
    - `ContactLabels.kt` (Veraltet): Hält aus Gründen der Rückwärtskompatibilität eine deprecated Delegation der Pseudo-Label-Identifier, die nun in der Domain-Schicht liegen.
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
    - `ContactDbMapper.kt`: Reine Logik-Komponente zur bidirektionalen Transformation zwischen Datenbank-Entitäten und Domain-Modellen (`ContactEntity` <-> `Contact`) (mittels `@Reusable` für effiziente DI-Instanziierung optimiert).
- ### 📁 Permission (`data.permission`)
    - `AndroidPermissionChecker.kt`: Konkrete plattformspezifische Implementierung des `PermissionChecker` Interfaces unter Verwendung von ContextCompat APIs und App-Kontext.

## 📁 Domain Layer (`domain`)
- ### 📁 Permission (`domain.permission`)
    - `PermissionChecker.kt`: Domänen-Interface zur Definition von Methoden für Berechtigungsabfragen, entkoppelt ViewModels vollständig von Android Platform-APIs.
- ### 📁 Models (`domain.model`)
    - `ContactLabels.kt`: Zentrales `object` mit system-definierten Pseudo-Label-Identifiern (`LABEL_NO_BIRTHDAY`, `LABEL_ANNIVERSARY`, `LABEL_NAME_DAY`). Liegt im Domain-Layer, um die UI-Schicht und Use Cases von der Data-Schicht zu entkoppeln.
    - `CoupleSuggestion.kt`: Reines Domänenmodell für Paar-Kopplungsvorschläge (frei von Android- oder UI-Abhängigkeiten).
    - `GiftIdea.kt`: Reines Domänenmodell für Geschenkideen (id, text, isChecked) mit statischen Hilfsmethoden zum Hinzufügen, Sortieren und Umschalten von Ideen.
    - `EventType.kt`: Typsicheres Enum zur Diskriminierung des aktiven Ereignistyps (`BIRTHDAY`, `ANNIVERSARY`, `NAME_DAY`). Liegt im Domain-Layer, um Tippfehler und stilles Fehlverhalten in der Filter-, Mapping- und Benachrichtigungslogik zu verhindern.
- ### 📁 Use Cases (`domain.usecase`)
    - `GetContactsUseCase.kt`: Kapselt die gesamte Filterlogik für die Home-Kontaktliste. Empfängt reaktive Inputs (Kontakte, Datum, Suchbegriff, Labels, Einstellungen) und gibt einen gefilterten Domain-Fluss `Flow<List<Contact>>` zurück. Enthält die `LabelSettingsState`-Datenklasse. Annotiert mit `@Reusable` (kein Singleton nötig).
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
- ### 📁 Utilities (`domain.util`)
    - `ContactFilterLogic.kt`: Reines Domänen-Hilfsobjekt zur Kapselung der Multi-Label-Filterregeln (Ignorieren und Verbergen) für Benachrichtigungen und Widgets.
    - `NotificationKeyUtils.kt`: Zentrales Utility-Objekt zum sicheren Enkodieren, Dekodieren und Extrahieren des `EventType` für Benachrichtigungs-Lookup-Keys (verhindert Fragilität bei Doppelpunkten im LookupKey).
    - `PhoneNumberNormalizer.kt`: Reines Kotlin-Domänen-Hilfsobjekt zur E.164-konformen Bereinigung von Telefonnummern (inkl. Handhabung von Inlandsvorwahlen mit führender 0, `+` und `00`, redundanten `(0)`-Klammern, fehlerhaften `+0...`-Präfixen und Ziffern-Only-Aufbereitung für WhatsApp URLs).
- ### 📁 AppFunctions (`domain.appfunctions`) — *Android 16+ / AI-Agent Integration*
    - `BirthdayAppFunctionService.kt`: Abstrakte `AppFunctionService`-Unterklasse (alpha10-API), annotiert mit `@AppFunctionServiceEntryPoint` und `@AndroidEntryPoint`. Stellt vier `@AppFunction`-Methoden bereit, die das Android-System und KI-Agenten (Google Assistant, Gemini) aufrufen können, ohne die App-UI zu öffnen. KSP generiert zur Compile-Zeit die konkrete Unterklasse `BirthdayBuddyGeneratedAppFunctionService` sowie das Assets-XML. Abhängigkeiten (`ContactRepository`, `IoDispatcher`) werden per Hilt field-injiziert.
    - `model/UpcomingBirthday.kt`: `@AppFunctionSerializable` Datenklasse für einen Geburtstags-Treffer (Rückgabe von `getUpcomingBirthdays`).
    - `model/ContactBirthday.kt`: `@AppFunctionSerializable` Datenklasse für die Geburtstagsdetails eines einzelnen Kontakts (Rückgabe von `getContactBirthday`).

    **Bereitgestellte AppFunctions:**

    | Funktion | Eingabe | Ausgabe | Beschreibung |
    |---|---|---|---|
    | `getUpcomingBirthdays` | `withinDays: Int = 30` | `List<UpcomingBirthday>` | Gibt Kontakte zurück, deren Geburtstag innerhalb des Fensters liegt; sortiert nach `daysUntil`. |
    | `getContactBirthday` | `contactName: String` | `ContactBirthday?` | Sucht einen Kontakt per (Teil-)Name (case-insensitive); gibt null zurück, wenn kein Treffer. |
    | `sendBirthdayMessage` | `contactId, app` | `PendingIntent` | Öffnet eine Messaging-App für die Telefonnummer des Kontakts (WhatsApp, Signal, Telegram, SMS). |
    | `addBirthdayToContact` | `contactId, year?, month, day` | `PendingIntent` | Öffnet den In-App-Editierscreen per Deep-Link (kein Direktschreiben — User-Bestätigung erforderlich). |

## 📁 UI Layer (`ui`)

- ### 📁 Mappers (`ui.mapper`)
    - `ContactUiMapper.kt`: Mapper zur Konvertierung von Domain-Modellen (`Contact`) in UI-Modelle (`ContactUiModel`). Kapselt visuelle Logik, Formatting, Event-Type-Evaluation und Couple-Pairing/Merging-Logik.
    - `CoupleSuggestionUiMapper.kt`: Mapper zur Konvertierung von Domänenmodellen (`CoupleSuggestion`) in UI-Modelle (`CoupleSuggestionUiModel`).

- ### 📁 Screens (`ui.screens`)
    - #### 📁 Home (`home`)
        - `HomeScreen.kt`: Einstiegspunkt und koordinierende Komponente des Hauptbildschirms.
        - `HomeContent.kt`: Schlanke UI-Layout-Komponente für den Homescreen (~180 Zeilen), die Scaffold, Navigation-Drawer, TopBar, FAB und List-Detail-Container zusammenführt.
        - `HomeNavKey.kt`: Sealed Interface `HomeNavKey` für Navigation 3 List-Detail-Szenen (`ContactList`, `ContactDetail`) und Hilfsfunktionen zur Backstack-Navigation.
        - `HomeState.kt`: Plain State Holder für die UI-Logik (Scroll-Zustand, Fokus).
        - `HomeActions.kt`: Wrapper für Benutzeraktionen zur Reduzierung von Prop-Drilling.
        - `HomeIntent.kt`: Sealed Interface `HomeIntent` und MVI-Intent-Klassen für den Home-Screen.
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
                - `FastScrollbar.kt`: Hochperformante Scrollbar mit Sub-Pixel-Präzision (unterstützt einen `headerCount`-Offset für Kopfzeilen). Enthält die öffentliche API (`FastScrollbar()`, `ScrollSection`), alle State-Variablen, Geometrie-Berechnungen, Drag-Gesture-Handler und die Thumb-UI. Nutzt `ScrollbarBubble.kt` (selbes Package) für die animierte Label-Bubble.
                - `ScrollbarBubble.kt`: Isoliertes, animiertes Label-Bubble-Composable (`internal fun ScrollbarBubble`), das während des Scrollens neben dem Thumb erscheint. Wird aus `FastScrollbar.kt` heraus aufgerufen. Nutzt die `Popup`-API für korrekte Z-Order und kein Clipping.
                - `GiftIdeaList.kt`: Spezialisierte Komponente für die Inline-Verwaltung von Geschenkideen.
                - `HomeListDetailDisplay.kt`: Adaptive Navigation 3 List-Detail-Containerkomponente (`NavDisplay`, Backstack-Management, automatische Pane-Auswahl, FastScrollbar-Anbindung).
            - ###### 📁 Labels (`home.components.labels`)
                - `LabelFilterBar.kt`: Chip-Leiste zur Filterung nach Labels (nun als scrollbarer Listen-Header in die BirthdayList integriert).
                - `LabelSidebar.kt`: Vertikale Sidebar zur Filterung nach Labels auf Tablets/Wide-Screens (unterstützt Expanded/Collapsed-Modi, Tooltips und Marquee-Text-Effekte).
            - ###### 📁 TopBar (`home.components.topbar`)
                - `HomeTopBar.kt`: Top-Bar Komponente für den Home-Screen mit Status-Bar Insets, `SearchBar` und bedarfsweiser `LabelFilterBar`.
                - `SearchBar.kt`: M3 SearchBar-Integration (unterstützt optionales leading navigationIcon).
            - ###### 📁 Actions (`home.components.actions`)
                - `ContactActionRow.kt`: Reihe mit Messenger- und Kontakt-Aktionen.
                - `HomeFAB.kt`: Multifunktionaler FAB mit Morphing-Animation.
                - `MessengerApp.kt`: Enum zur Definition unterstützter Messenger und deren Branding.
    - #### 📁 Onboarding (`onboarding`)
        - `OnboardingScreen.kt`: Multi-Page Flow für die initiale Konfiguration.
        - `OnboardingViewModel.kt`: Zuständig für den Onboarding-Status und Erststart-Prozess. Nutzt MVI-Intents und `onIntent()`. **Feature-co-located** neben `OnboardingScreen.kt`.
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
        - `labels/LabelViewModel.kt`: Logik für die Label-Verwaltung und Konfiguration. Nutzt MVI-Intents, `onIntent()` und konsistenten `uiState: StateFlow<LabelUiState>`. **Feature-co-located**.
        - `notifications/NotificationSettingsScreen.kt`: Konfiguration des Erinnerungssystems.
        - `notifications/NotificationViewModel.kt`: Verwaltung der Benachrichtigungsregeln und deren Synchronisation mit dem WorkManager. **Feature-co-located**.
        - `calendar/CalendarSettingsScreen.kt`: Screen zur detaillierten Kalender-Sync-Konfiguration.
        - `calendar/CalendarViewModel.kt`: ViewModel für die Kalender-Einstellungen. Nutzt MVI-Intents und `onIntent()`. **Feature-co-located**.
        - `backup/BackupScreen.kt`: Screen für den Import/Export von Geschenkideen.
        - `backup/BackupViewModel.kt`: Logik für den Import und Export von Geschenkideen. Nutzt MVI-Intents, `onIntent()` und `uiState`. **Feature-co-located**.
        - `theme/ThemeSettingsScreen.kt`: Einstellungsbildschirm zur Design-Auswahl.
        - `theme/ThemeViewModel.kt`: Hält und aktualisiert den UI-Zustand für das App-Design. Nutzt MVI-Intents und `onIntent()`. **Feature-co-located**.
        - `otherevents/OtherEventsSettingsScreen.kt`: Screen zur Aktivierung des Features für weitere Ereignisse.
        - `sync/SyncSettingsScreen.kt`: Screen zur manuellen Synchronisierung der Kontakte.
        - `sync/SyncViewModel.kt`: ViewModel für die manuelle Synchronisierung. **Feature-co-located**.
        - `about/AboutScreen.kt`: Anzeige von App-Informationen.
        - `about/PrivacyPolicyScreen.kt`: Anzeige der Datenschutzerklärung.
- ### 📁 Models (`ui.model`)
    - `ContactUiModel.kt`: Immutable UI-Modell für Kontakte.
    - `BirthdayTier.kt`: Typsicheres Enum zur Klassifizierung des visuellen Tiers eines Kontakts (`MILESTONE_GOLD`, `MILESTONE_SILVER`, `CHILD`, `REGULAR`). Die Berechnung erfolgt einmalig in `ContactUiMapper` via `BirthdayTier.from(nextAge?)` und wird über `ContactUiModel.birthdayTier` an die UI übergeben. Ersetzt die doppelte Inline-Logik in `BirthdayItem.kt`.
    - `HomeUiState.kt`: Gebündelter State für den Home-Screen.
    - `CalendarUiState.kt`: Gebündelter State für den Kalender-Einstellungs-Screen.
    - `NotificationUiState.kt`: Gebündelter State für den Benachrichtigungs-Einstellungs-Screen.
    - `ThemeUiState.kt`: Gebündelter State für den Design-Einstellungs-Screen.
    - `BackupUiState.kt`: Gebündelter State (`BackupUiState`) und One-Time Status-Events (`BackupMessage`) für den Backup-Screen.
    - `LabelUiState.kt`: Gebündelter State für den Label-Einstellungs-Screen (`labelsEnabled`, `labels`).
    - `LabelManagementModel.kt`: Modell für die Label-Verwaltung.
    - `GiftIdea.kt`: Reines Domänenmodell für Geschenkideen (id, text, isChecked) mit Manipulations-Logik (Hinzufügen, Sortieren, Umschalten).
    - `CoupleSuggestionUiModel.kt`: Immutable UI-Modell für Paar-Vorschläge, entkoppelt die UI-Schicht von der Room-Entity.
    - `SampleData.kt`: Zentraler Ort für Testdaten für Previews und Tests.
- ### 📁 Navigation (`ui.navigation`)
    - `NavRoutes.kt`: Alle 12 typsicheren, serialisierbaren `NavKey`-Routenobjekte (`Home`, `Settings`, `LabelSettings`, `NotificationSettings`, etc.). Zentrale Quelle der Navigationsstruktur.
    - `AppAction.kt`: Sealed Interface `AppAction` für typsichere globale App- und Navigations-Aktionen (z.B. `NavigateToNotifications`, `OpenSearch`, `ScrollToTop`, `OpenAddContact`, `OpenBirthdayPicker`), vollständig entkoppelt von Android-Intents.
    - `AppNavHost.kt`: Zentrales Navigations-Composable. Verwaltet den `NavDisplay` (Navigation 3) inklusive Screen-zu-Screen-Transitions (Push/Pop/PredictiveBack mit Parallax-Effekt), das vollständige Route-zu-Screen-Mapping mit ViewModel-Verknüpfungen und die Ausführung von `AppAction`-Events.
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
    - `Theme.kt`, `Color.kt`, `ColorPresets.kt`, `Type.kt`, `Shapes.kt`: Design-System Definitionen (Farbschemata, Typografie, Eckenradien, Farb-Presets), custom Farbschemata für Akzentfarben und AMOLED-Erweiterung.

## 📁 Notification-Infrastruktur (`notification`)
- `NotificationWorker.kt`: Hintergrund-Prozess für die Benachrichtigungs-Logik (`@AssistedInject` Hilt-Worker).
- `SnoozeWorker.kt`: Hintergrund-Prozess für die "Später"-Funktion.
- `NotificationActionReceiver.kt`: Verarbeitet Klicks auf Benachrichtigungs-Buttons (Snooze, Erledigt, Dismissed). Im `AndroidManifest.xml` als Receiver registriert.
- `NotificationHelper.kt`: Hilfsklasse für den System-Notification-Manager (Erstellen und Anzeigen von Benachrichtigungen).
- `NotificationTextFormatter.kt`: Kapselt die Generierung von Benachrichtigungstiteln und -texten (Geburtstage, Hochzeitstage, Namenstage, Einzelpersonen, Paare und Gruppen).

> [!NOTE]
> Dieses Package enthält **keine UI-Komponenten**. Die UI-seitigen Elemente (`EditRuleDialog.kt`, `NotificationRuleItem.kt`) verbleiben in `ui/screens/settings/notifications/components/`.

## 📁 ViewModel (feature-co-located)
> ViewModels liegen **direkt neben ihren Screen-Dateien** (feature-co-located), nicht in einem globalen `viewmodel/`-Package.
> Das Root-Package enthält `AppViewModel.kt` (Activity-weit, keinem einzelnen Screen zugehörig).
> Alle anderen ViewModels befinden sich in ihrem jeweiligen Feature-Package unter `ui/screens/`.

## 📁 Utilities (`util`)
- `DateUtils.kt`: Robuste Erweiterungsfunktionen für LocalDate.
- `StringUtils.kt`: Hilfsfunktionen für Namens- und String-Operationen (`mergeNames`, `getInitials`).
- `ContextExtensions.kt`: Hilfsfunktionen für die sichere Navigation im Android-Context.
- `IntentExtras.kt`: Zentrales `object` mit allen `const val`-Schlüsseln für Intent-Extras (`SCROLL_TO_TOP`, `NAVIGATE_TO_NOTIFICATIONS`, `OPEN_SEARCH`, `OPEN_ADD_CONTACT`) sowie sicheren, typgeprüften Extraktions- und Bereinigungsfunktionen (`safeGetAndRemoveBooleanExtra`, `safeGetIntExtra`, `safeGetStringArrayExtra`).
- `IntentParser.kt`: Zentraler Parser zur sicheren Umwandlung von Android-`Intent`s in typsichere `AppAction`-Instanzen.
- `MessengerUtils.kt`: Asynchrone, nicht-blockierende Hilfsfunktion `getInstalledMessengersAsync` zur Abfrage installierter Messenger-Apps ohne Thread-Blockierung im UI-Layer.
- `WidgetUpdater.kt` & `BirthdayWidgetUpdater.kt`: Abstraktion und Implementierung zur Glance-unabhängigen Aktualisierung und WorkManager-Planung (`scheduleDailyUpdate()`) des App-Widgets.
- `NotificationScheduler.kt` & `NotificationSchedulerImpl.kt`: Hilfsklassen zur WorkManager-unabhängigen Steuerung von Hintergrund-Workern.

## 📁 UI Utilities (`ui.util`)
- `ContactActions.kt`: Zentraler Handler für externe System-Aktionen (Intents, Permissions).

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`, `BirthdayWidgetReceiver.kt`, `BirthdayWidgetWorker.kt`, `WidgetLayoutHelper.kt`: Glance-basierte Widget-Komponenten und reine Kotlin-Layout-Berechnungen.

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
- `data/mapper/ContactDbMapperTest.kt`: Tests für die bidirektionale Transformation zwischen Datenbank-Entitäten und Domain-Modellen (`toDomain` & `toEntity`).
- `data/repository/ContactRepositoryImplTest.kt`: JVM Unit-Tests für `ContactRepositoryImpl` (Abdeckung von allContacts, labelsEnabled, syncContacts, addGiftIdea, und labelConfigs).
- `data/repository/SystemContactDataSourceTest.kt`: JVM Unit-Tests für `SystemContactDataSource` (Parsing von Datumsformaten inkl. Schaltjahren wie 29. Februar ohne Jahr).
- `util/DateUtilsTest.kt`: Logiktests für Datumsberechnungen (Alter, Tage bis Geburtstag, etc.).
- `util/StringUtilsTest.kt`: Logiktests für String- und Namens-Hilfsfunktionen (`mergeNames`, `getInitials`).
- `util/IntentParserTest.kt`: JVM Unit-Tests für `IntentParser` zur Absicherung der Extraktion aller `AppAction`-Typen aus Android-`Intent`s.
- `domain/usecase/GetContactsUseCaseTest.kt`: JVM Unit-Tests für `GetContactsUseCase` zur Absicherung der Filter- und Pairing-Logik.
- `domain/util/NotificationKeyUtilsTest.kt`: JVM Unit-Tests für `NotificationKeyUtils` (Enkodierung, Dekodierung und EventType-Erkennung inklusive Sonderzeichen & Doppelpunkten im LookupKey).
- `domain/util/PhoneNumberNormalizerTest.kt`: JVM Unit-Tests für `PhoneNumberNormalizer` (vollständige E.164-Testabdeckung für nationale, internationale, klammerbasierte Formate, fehlerhafte Präfixe und länderspezifische Regeln).
- `domain/usecase/GetPendingNotificationsUseCaseTest.kt`: JVM Unit-Tests zur Überprüfung der Benachrichtigungsregeln und Fälligkeits-Kalkulation.
- `domain/usecase/SnoozeNotificationUseCaseTest.kt`: JVM Unit-Tests zur Überprüfung der Schlummer-Delegation.
- `notification/NotificationTextFormatterTest.kt`: JVM Unit-Tests für `NotificationTextFormatter` (vollständige Abdeckung aller EventType-Pfade, Tage-Offsets, Einzelpersonen, Paare und Gruppen).
- `domain/usecase/GetCoupleSuggestionUseCaseTest.kt`: JVM Unit-Tests zur Überprüfung der Ehepartner-Vorschlagsermittlung (inkl. ungleicher Nachnamen).
- `domain/usecase/ExportGiftIdeasUseCaseTest.kt`: JVM Unit-Tests zum Geschenkideen-Export.
- `domain/usecase/ImportGiftIdeasUseCaseTest.kt`: JVM Unit-Tests zum Geschenkideen-Import.
- `domain/usecase/SetCalendarSyncEnabledUseCaseTest.kt`: JVM Unit-Tests zur Aktivierung/Deaktivierung der Kalendersynchronisation.
- `domain/appfunctions/BirthdayAppFunctionServiceTest.kt`: JVM Unit-Tests für `BirthdayAppFunctionService`: Überprüfung der Filter-/Mapping-Logik von `getUpcomingBirthdays` (Fensterfilterung, Sortierung, Jahr-Mapping) und `getContactBirthday` (Teil-Match, Null-Handling, Blanknamen-Fehler, alphabetische Erstauflösung).
- `AppViewModelTest.kt`: Tests für `AppViewModel`: Verifikation der initialen `AppSettings`-Emission, reaktiver Settings-Propagation, `scheduleDailyUpdate()`, `pendingAction`, `handleAction` und `consumeAction` ohne Android-Framework-Abhängigkeiten.
- `BootReceiverTest.kt`: JVM Unit-Tests für `BootReceiver`: Absicherung aller Broadcast-Aktionen (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET`, `DATE_CHANGED`), Ignorieren unpassender Aktionen und Exception-Handling.
- `ui/screens/home/HomeViewModelGiftIdeaTest.kt`: Tests für Geschenkideen- und Geburtstags-Intents im `HomeViewModel`. **Feature-co-located** neben `HomeViewModel.kt`.
- `ui/screens/home/HomeViewModelSearchTest.kt`: Tests der Such- und Filterlogik im `HomeViewModel`. **Feature-co-located** neben `HomeViewModel.kt`.
- `ui/screens/home/HomeViewModelTest.kt`: Tests für das reaktive State-Management und die UI-Filterung im `HomeViewModel`. **Feature-co-located** neben `HomeViewModel.kt`.
- `ui/screens/onboarding/OnboardingViewModelTest.kt`: Tests für den Onboarding-Status und Erststart-Prozess im `OnboardingViewModel`. **Feature-co-located** neben `OnboardingViewModel.kt`.
- `viewmodel/BackupViewModelTest.kt`: Tests für `BackupViewModel` zur Absicherung des Geschenkideen-Im- und Exports.
- `viewmodel/CalendarViewModelTest.kt`: Tests für `CalendarViewModel` zur Absicherung der Kalendereinstellungs- und Synchronisationssteuerung.
- `viewmodel/LabelViewModelTest.kt`: Tests für `LabelViewModel` zur Absicherung der dynamischen Labels-Filterungslogik.
- `viewmodel/ThemeViewModelTest.kt`: Tests für `ThemeViewModel` zur Absicherung der Design-Einstellungen.
- `widget/WidgetLayoutHelperTest.kt`: JVM Unit-Tests für `WidgetLayoutHelper` (Standard-Widget-Größen, kleine Höhen, leere vs. große Kontaktlisten, gleichmäßige Höhenaufteilung).
- `widget/BirthdayWidgetWorkerTest.kt`: JVM Unit-Tests für `BirthdayWidgetWorker` (Scheduling, Worker-Ausführung, Fehler-Handling).


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

### 📸 Screenshot-Tests (`app/src/test/java/.../screenshot/`)
JVM-basierte Compose Screenshot-Tests mit **Roborazzi 1.68.0** und Robolectric. Laufen ohne Emulator.

#### Verzeichnisstruktur
```
screenshot/
├── BaseScreenshotTest.kt              # Abstrakte Basisklasse (ComposeRule, RoborazziRule, Helfer)
├── home/
│   ├── HomeContentScreenshotTest.kt   # HomeContent (Phone/Tablet, Light/Dark, leer, loading)
│   ├── BirthdayListScreenshotTest.kt  # BirthdayList (mit/ohne Kontakte, Shimmer, Labels)
│   └── BirthdayItemScreenshotTest.kt  # BirthdayItem (REGULAR, GOLD, SILVER, CHILD, expanded)
├── onboarding/
│   └── OnboardingScreenshotTest.kt    # OnboardingScreenContent (Welcome, Permissions, Settings, Light/Dark)
└── settings/
    ├── BackupScreenshotTest.kt            # BackupScreenContent (Idle, ExportSuccess, Loading, Tablet)
    ├── CalendarSettingsScreenshotTest.kt  # CalendarSettingsScreenContent (Synced, Custom Colors, Tablet)
    ├── LabelSettingsScreenshotTest.kt     # LabelSettingsScreenContent (an/aus, leer, Tablet)
    ├── NotificationSettingsScreenshotTest.kt # NotificationSettingsContent (Regeln, kein Permission)
    ├── SettingsOverviewScreenshotTest.kt  # SettingsScreen Menu (Compact Phone vs. Split-Pane Tablet)
    ├── SyncSettingsScreenshotTest.kt      # SyncSettingsScreenContent (Idle, Syncing, Tablet)
    └── ThemeSettingsScreenshotTest.kt     # ThemeSettingsScreenContent (System, Light, Dark, AMOLED)
```

#### Golden-Images
- Pfad: `app/src/test/snapshots/images/` (von Git versioniert)
- Namenskonvention: `{TestClass}_{methodName}.png` (automatisch von Roborazzi generiert)

#### Größen-Matrix
| Bezeichnung  | Breite | Höhe   | Klasse              |
|--------------|--------|--------|---------------------|
| `phoneSize`  | 360 dp | 640 dp | Compact width       |
| `phoneTallSize` | 360 dp | 1000 dp | Compact, tall    |
| `tabletSize` | 840 dp | 640 dp | Expanded width      |

#### CI-Befehle
```bash
# Golden-Images initial aufzeichnen (einmalig oder nach bewussten UI-Änderungen)
./gradlew recordRoborazziDebug

# Screenshots gegen Golden-Images validieren (in CI)
./gradlew verifyRoborazziDebug

# Diff-Bericht bei Abweichungen generieren
./gradlew compareRoborazziDebug

# Alle Unit-Tests inkl. Screenshot-Tests ausführen
./gradlew testDebugUnitTest
```

#### Workflow für UI-Änderungen
1. UI-Änderung implementieren
2. `./gradlew verifyRoborazziDebug` ausführen → Diff-Bilder prüfen
3. Wenn Änderung beabsichtigt: `./gradlew recordRoborazziDebug` → Golden-Images aktualisieren
4. Neue Golden-Images committen
