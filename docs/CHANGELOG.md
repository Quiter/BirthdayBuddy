# Changelog: BirthdayBuddy

> **Note:** Historische Einträge (Meilensteine 1-76) wurden nach [CHANGELOG_ARCHIVE.md](CHANGELOG_ARCHIVE.md) verschoben, um das Kontext-Fenster für LLM-Sessions zu optimieren.

77. **Advanced Onboarding & Stability Overhaul:**
    - **Onboarding Screen:** Einführung eines geführten Multi-Page Onboarding-Screens (Willkommen, Kontakte, Benachrichtigungen) mit `HorizontalPager`. Ersetzt die fehleranfälligen Dialog-Popups.
    - **UX Polish:** Deaktivierung des freien Swipens im Onboarding zur Sicherstellung einer vollständigen Konfiguration. Implementierung von intelligenten "Einstellungen öffnen" Fallbacks und "Ohne Kontakte fortfahren" Optionen.
    - **Branding:** Integration des farbigen App-Logos im Willkommens-Screen zur Stärkung der Markenidentität.
    - **Technical Stability:** Implementierung eines `Mutex` im `NotificationRepository` zur Beseitigung von Race-Conditions bei parallelen Einstellungs-Updates.
    - **Automated Testing:** Einführung einer Test-Infrastruktur mit Unit-Tests (`DateUtils`) und Instrumented Tests (`NotificationRepository`).
    - **CI/CD Integration:** Automatisierte Test-Ausführung in der GitHub Actions Pipeline vor jedem Release-Build.
    - **Cleanup:** Entfernung veralteter Onboarding-Dialoge und Konsolidierung der UI-Logik im Home-Screen.
78. **Resource Consolidation & Testing Excellence:**
    - **String Cleanup:** Zusammenführung aller Onboarding-Strings in die zentrale `strings.xml` und Entfernung redundanter Ressourcendateien zur besseren Wartbarkeit.
    - **Privacy Policy Extraction:** Auslagerung der umfangreichen Datenschutzerklärung in lokalisierte Markdown-Dateien (`res/raw`), um die App-Ressourcen übersichtlich zu halten und die Lesbarkeit zu verbessern.
    - **Testing Stability:** Vereinheitlichung der Coroutines-Bibliotheken (v1.10.1) zur Behebung von Laufzeitfehlern in der Test-Umgebung.
    - **Test Infrastructure:** Migration der ViewModel-Tests auf instrumentierte Tests (`androidTest`) inklusive Integration von `mockito-android`, um eine stabile Validierung der Such- und Filterlogik auf echten Geräten zu gewährleisten.
    - **Policy Update:** Einführung einer verbindlichen Test-Pflicht vor jedem Release-Build in der Projektdokumentation.
79. **Architecture Best Practices (Restructuring):**
    - **Data Layer:** Bündelung von Datenbank-Entitäten und Repositories unter dem gemeinsamen `data`-Package (`data.local`, `data.repository`).
    - **Mapper Layer:** Einführung des `data.mapper` Packages for reine Logik-Komponenten wie den `ContactMapper`.
    - **UI Models:** Verschiebung von reinen UI-Datenstrukturen (`ContactUiModel`, `HomeUiState`, `GiftIdea`) in das neue `ui.model` Package zur strikten Trennung von ViewModels und deren State-Definitionen.
    - **Project Cleanup:** Vollständige Aktualisierung aller Paket-Deklarationen und Importe im gesamten Projekt sowie Synchronisation der Test-Ordnerstruktur.
80. **UI Polish & Performance (Startup & I18n):**
    - **Instant Start:** Integration der Android Splash Screen API (`core-splashscreen`) zur Eliminierung von Start-Flickern; die App hält das System-Logo nun aktiv fest, bis der Onboarding-Status aus der Datenbank geladen wurde.
    - **Expert Date Logic:** Zentralisierung der Geburtsdatums-Logik in den `DateUtils`. Einführung von `isBirthdayToday`, `hasYear` und einem typsicheren `safeNextAge` (nullable), was den Code in Mappern und Repositories deutlich lesbarer macht.
    - **Full I18n Support:** Umstellung der Datumsformatierung im Mapper und Widget auf systemseitige Patterns (`getBestDateTimePattern`), wodurch Tag und Monat nun weltweit in der jeweils korrekten Reihenfolge und Schreibweise angezeigt werden.
    - **Resource Cleanup:** Entfernung veralteter Onboarding-Dialog-Strings und Konsolidierung der Ressourcen.
81. **ViewModel Refactoring & Modularization:**
    - **Architecture:** Aufteilung des monolithischen `BirthdayViewModel` in fünf spezialisierte ViewModels (`HomeViewModel`, `NotificationViewModel`, `SettingsViewModel`, `LabelViewModel`, `BackupViewModel`). Dies verbessert die Wartbarkeit, reduziert die Code-Komplexität pro Datei und ermöglicht eine gezieltere Testbarkeit.
    - **Navigation:** Anpassung des `NavHost` in der `MainActivity` zur Bereitstellung der spezifischen ViewModels für die jeweiligen Screens.
    - **Testing:** Migration und Umbenennung des Such-Logik-Tests zu `HomeViewModelSearchTest` unter Berücksichtigung der neuen Architektur.
82. **UX & UI Spacing Polish:**
    - **Pull-to-Refresh:** Refactoring der `BirthdayList`, sodass die Pull-to-Refresh Geste nun auch bei einer leeren Liste (Empty State) einwandfrei funktioniert.
    - **Layout Consistency:** Standardisierung der vertikalen Abstände im Home-Header. Einführung eines festen 8dp Paddings für die `SearchBar` zur Gewährleistung eines harmonischen Layout-Rhythmus, unabhängig von der Sichtbarkeit der Label-Filterbar.
83. **UI Interaction Overhaul (Expandable Cards):**
    - **UX Evolution:** Ersatz der versteckten Swipe-Geste durch ein intuitives "Tap-to-Expand" System. Die Kontakt-Karten lassen sich nun durch einfaches Antippen aufklappen.
    - **Direct Access:** Die Aktions-Buttons wurden durch kompakte, intuitive Icons ersetzt. Der Button zum Öffnen des Kontakts ist nun als Icon in der Form des Kontaktbildes direkt darunter platziert.
    - **Messenger Integration:** Einführung von Schnellzugriff-Links für Telefonate, SMS und WhatsApp direkt in der aufgeklappten Karte (vorausgesetzt eine Telefonnummer ist hinterlegt).
    - **Gift Idea Visibility:** Die Verwaltung von Geschenkideen erfolgt nun vollständig inline. Der separate "Hinzufügen"-Button wurde entfernt, da Einträge direkt in der Karte erstellt und bearbeitet werden können.
    - **Data Layer Upgrade:** Die Kontakt-Synchronisation erfasst nun auch Telefonnummern vom System, um die Messenger-Integration zu ermöglichen (Datenbank v4).
    - **Performance:** Implementierung flüssiger Animationen (`animateContentSize`) beim Aufklappen und Reduzierung der UI-Komplexität durch Entfernung der Swipe-Logik und des `GiftIdeaDialog`.
    - **Clean UI:** Bereinigung des Home-Screens von Hilfe-Animationen (Swipe-Hint), da die neue Interaktion selbsterklärend ist.
84. **Architecture Refinement (Component Splitting):**
    - **Modularization:** Aufteilung der massiven `BirthdayItem.kt` in kleinere, spezialisierte Komponenten (`GiftIdeaList`, `ContactActionRow`, `ContactImage`, `BirthdayStatus`).
    - **Maintainability:** Deutliche Verbesserung der Lesbarkeit und Wartbarkeit durch Trennung von Layout-Koordination und spezialisierter UI-Logik.
    - **Reusability:** Die neuen Komponenten sind nun eigenständig und können bei Bedarf an anderen Stellen der App leichter wiederverwendet werden.
85. **UX & Performance Polish (Input & Stability):**
    - **Smooth Input:** Einführung eines lokalen States für Geschenkideen-Textfelder zur Eliminierung von Verzögerungen und Cursor-Sprüngen während der Datenbank-Synchronisation.
    - **Focus Management:** Optimierung der Tastatur-Steuerung im Home-Screen; automatisches Schließen der Tastatur beim Scrollen wurde auf die Suchfunktion begrenzt, um die Bearbeitung von Geschenkideen nicht zu unterbrechen.
    - **Dynamic Labels:** Kontextsensitive Beschriftung für das Hinzufügen von Geschenkideen ("Geschenkidee" bei leerer Liste vs. "Listeneintrag").
    - **Automated UI Testing:** Implementierung von instrumentierten Tests (`BirthdayItemInteractionTest`) zur Absicherung der Interaktions-Logik in den Kontakt-Karten.
    - **Code Quality:** Systematische Bereinigung von IDE-Warnungen (ungenutzte Parameter, KTX-Migration zu `toUri()`).
86. **UX Refinement (Expandable Card Polish):**
    - **Visual Consistency:** Korrektur des Highlight-Effekts beim Aufklappen; die gesamte Karte nutzt nun einen einheitlichen Hintergrund zur besseren Abgrenzung.
    - **Smart Interaction:** Implementierung einer "Auto-Collapse" Logik; geöffnete Karten schließen sich nun automatisch beim Scrollen der Liste.
    - **Label Optimization:** Verlagerung der Label-Anzeige in den erweiterten Bereich der Karte, um die Hauptliste für alle Nutzer (auch ohne eigene Labels) kompakt und übersichtlich zu halten.
    - **Icon Reset:** Rückkehr zum klassischen Kontakt-Symbol für die Verknüpfung zur Android-Kontakte-App zur besseren Wiedererkennbarkeit.
87. **Code Quality & Architecture Refinement (HomeScreen):**
    - **Separation of Concerns:** Auslagerung des `HomeState` in eine eigene Datei (`HomeState.kt`) zur Verschlankung der `HomeScreen.kt`.
    - **State Management:** Konsolidierung der Side-Effects durch Verlagerung der Filter-Reset-Logik in das `HomeViewModel`. Das ViewModel steuert nun zentral den `isResettingFilter` Zustand.
    - **Stability:** Einführung von sicherem Parsen für Kontakt-IDs (`toLongOrNull`) und Bereinigung redundanter `LaunchedEffect`-Blöcke.
    - **Component Polish:** Refactoring von `ContactActionRow` mit sicherem Intent-Handling (try-catch) for Anrufe, SMS und WhatsApp sowie Korrektur der Icon-Inkonsistenzen.
    - **Structure:** Aktualisierung der `PROJECT_STRUCTURE.md` zur Abbildung der neuen Dateistruktur.
88. **Context Optimization & Messenger Upgrade:**
    - **Context Hardening:** Archivierung alter Changelog-Einträge (1-59) in `CHANGELOG_ARCHIVE.md` zur Einsparung von Context-Fenster-Platz für LLMs.
    - **Smart Messenger Detection:** Einführung einer automatischen Erkennung von WhatsApp/Signal Verfügbarkeit via MIME-Type Check im `SystemContactDataSource`.
    - **Database Migration (v5):** Upgrade des Schemas zur persistenten Speicherung der Messenger-Verfügbarkeit pro Kontakt.
    - **UI Finalization:** Dynamische Einblendung der originalen Messenger-Icons (WhatsApp/Signal) in der `ContactActionRow` nur bei tatsächlicher Verfügbarkeit.
89. **Code Quality & ViewModel Refinement:**
    - **Logic Consolidation:** Einführung der privaten Hilfsfunktion `updateContactGiftIdeas` im `HomeViewModel`. Dies eliminiert redundanten Code bei der Manipulation von Geschenkideen und stellt eine konsistente Datenaktualisierung sicher.
    - **Type Safety:** Refactoring des `uiState` Flows im `HomeViewModel`. Die Kombination der Datenströme wurde durch explizite Parameterbenennung lesbarer und typsicherer gestaltet.
    - **Linting & Formatting:** Projektweite Bereinigung von IDE-Warnungen. Einführung von Trailing Commas in allen Home-Komponenten, Korrektur von Namenskonventionen (z.B. `ScrollbarDefaults`) und Optimierung von Side-Effects (z.B. `produceState`).
    - **Stability:** Verbesserung des Scroll-Reset-Mechanismus durch explizite Parameterübergabe in Callbacks.
90. **Visual Delight (Confetti Celebration):**
    - **New Component:** Einführung von `ConfettiEffect.kt`, einem leichtgewichtigen Partikelsystem auf Canvas-Basis.
    - **Contextual Animation:** Die App feiert nun Geburtstage visuell beim Aufklappen einer Kontaktkarte. Der Effekt regnet über den Inhalt der Karte, ohne das Layout zu blockieren.
    - **Age-Based Coloring:** Dynamische Farbwahl für das Konfetti passend zur Umrandung: Gold für runde Geburtstage, bunte `KidColors` für Kinder (0-9) und Silber/Weiß für alle anderen.
    - **Performance:** Nutzung von `matchParentSize()` und `LocalDensity` für eine präzise, GPU-beschleunigte Darstellung unabhängig von der Bildschirmauflösung.
91. **UX Polish (Gift Idea Management):**
    - **Smart Collapsing:** Geschenkideen sind in der Kontaktkarte nun standardmäßig eingeklappt. Ein neuer, interaktiver Toggle-Bereich mit passendem Icon ermöglicht das gezielte Auf- und Zuklappen.
    - **Contextual Reset:** Um die Übersichtlichkeit zu wahren, klappt sich die Geschenkideen-Liste automatisch zu, wenn die Kontaktkarte geschlossen wird (`remember(isExpanded)`).
    - **Navigation Intelligence:** Geöffnete Kontaktkarten schließen sich nun automatisch, wenn der Nutzer die Filter-Kategorie (Label) wechselt oder eine Suche startet, um eine konsistente Ergebnisliste zu gewährleisten.
    - **UI Cleanup:** Entfernung der redundanten "Geschenkideen"-Überschrift innerhalb der Liste, da die neue Toggle-Zeile diese Rolle übernimmt.
    - **Auto-Expand:** Beim Hinzufügen einer neuen Geschenkidee klappt der Bereich automatisch auf, um einen nahtlosen Workflow zu ermöglichen.
92. **Android 15 Compatibility & Technical Debt Cleanup:**
    - **Edge-to-Edge Optimization:** Anpassung des Onboarding-Screens an die erzwungene randlose Anzeige unter Android 15. Verwendung von `Scaffold` und `navigationBarsPadding()`, um die Überlagerung interaktiver Elemente durch die System-Navigation zu verhindern.
    - **API Migration:** Entfernung veralteter XML-Attribute (`statusBarColor`, `navigationBarColor`) im App-Theme zur Erfüllung der neuen Play Store Anforderungen für SDK 35/36.
    - **Code Quality Offensive:** Systematische Behebung von Linter-Warnungen im Home- und Onboarding-Bereich (vereinfachte Booleans, explizite Parameternamen bei `mutableStateOf`).
    - **Stability Validation:** Erfolgreiche Validierung der Gesamtstabilität durch Ausführung der Unit-Test-Suite und Durchführung eines vollständigen Projekt-Builds.
93. **Widget Excellence (3x2 & Adaptive Preview):**
    - **Sizing:** Das Widget wird nun standardmäßig als 3x2 Grid (3 Spalten, 2 Zeilen) vorgeschlagen (`targetCellWidth/Height`), um mehr Platz für die Geburtstagsliste zu bieten.
    - **Adaptive Preview:** Einführung eines hochwertigen Vorschaubildes im Widget-Picker. Durch die Bereitstellung separater Ressourcen in `drawable` und `drawable-night` passt sich die Vorschau nun automatisch dem Hell/Dunkel-Modus des Systems an.
    - **I18n:** Lokalisierung der Widget-Beschreibung und Optimierung der Metadaten (`birthday_widget_info.xml`) für moderne Android-Versionen (API 31+).
    - **UX:** Das Widget führt beim Klick nun gezielt zum Home-Screen mit einem automatischen "Scroll-to-Top" Reset.
94. **Preview Data Centralization:**
    - **Architecture:** Einführung von `SampleData.kt` im `ui.model` Package. Dies dient als zentrale Quelle für konsistente Testdaten über alle Previews hinweg.
    - **HomeScreen:** Refactoring der `HomePreview` zur Nutzung der zentralen `SampleData`, was die Datei übersichtlicher macht.
    - **Component Previews:** Aktualisierung von `BirthdayItem`, `BirthdayList` und `GiftIdeaList` mit aussagekräftigen Previews auf Basis der neuen Sample-Daten.
95. **External Actions & Permission Refactoring:**
    - **Architecture:** Einführung von `ContactActions.kt` (`ui.util`) zur Zentralisierung aller externen Intents (Anrufe, SMS, WhatsApp, Signal, Kontakte öffnen).
    - **Stability:** Einführung einer robusten `findActivity()` Extension (`util`), um Activity-bezogene Operationen (wie Permission-Rationales) sicher aus tief verschachtelten Context-Wrappern heraus auszuführen.
    - **Clean UI:** Massive Reduzierung von Boilerplate-Code in `HomeScreen.kt` und `ContactActionRow.kt` durch Delegation der Intent-Logik an den neuen Handler.
    - **Consistency:** Vereinheitlichung der Rufnummern-Formatierung für WhatsApp und Signal an einer zentralen Stelle.
96. **Prop Drilling Reduction:**
    - **Architecture:** Einführung von `HomeActions.kt` zur Bündelung aller Callbacks des Home-Screens.
    - **Refactoring:** Vereinfachung der `HomeContent` Signatur durch Nutzung des neuen Action-Wrappers. Dies verbessert die Lesbarkeit und vereinfacht die Wartung des Screens.
97. **Test Coverage Offensive:**
    - **Unit Tests:** Einführung von `ContactMapperTest` zur Absicherung der Transformations-Logik und Datumsformatierung.
    - **ViewModel Tests:** Erweiterung der Testsuite um `HomeViewModelTest`, inklusive Validierung der Label-Filterung und des Ignorieren-Status.
    - **Stability:** Sicherstellung korrekter Altersberechnungen und "Heute"-Status Logik über verschiedene Test-Szenarien.
98. **Onboarding UX & Architecture Refactoring:**
    - **Edge-to-Edge Fix:** Behebung von UI-Überlagerungen durch die 3-Button Systemnavigation im Onboarding-Screen. Einführung eines scrollbaren Seiten-Wrappers und eines optisch abgesetzten Footers zur Sicherstellung der Erreichbarkeit aller Steuerelemente.
    - **Modularization:** Aufteilung der monolithischen `OnboardingScreen.kt` in spezialisierte Komponenten (`WelcomePage`, `ContactsPage`, `NotificationsPage`, `ReadyPage`, `OnboardingFooter`).
    - **Code Quality:** Einführung von `OnboardingPageWrapper` in `OnboardingCommon.kt` zur Reduzierung von Code-Duplikaten und Gewährleistung eines einheitlichen Layout-Verhaltens.
    - **Documentation:** Aktualisierung von `PROJECT_STRUCTURE.md` zur Abbildung der neuen Onboarding-Komponentenstruktur.
99. **Global Code Quality & Linting Offensive:**
    - **I18n Excellence:** Migration von String-Ressourcen mit variablen Zahlenwerten zu `plurals` (Quantity Strings) zur Behebung von Linter-Warnungen und Verbesserung der Lokalisierung (z.B. Altersangaben, Import-Bestätigungen).
    - **Type Safety & Inference:** Systematische Bereinigung redundanter Typ-Parameter im gesamten Projekt (z.B. in `BirthdayWidget`, `BirthdayItem`) zur Verschlankung des Codes.
    - **Idiomatic Kotlin:** Refactoring von `when`-Ausdrücken zu Ausdrücken mit Subjekt und Inlining von Hilfsvariablen (z.B. in `SystemContactDataSource`).
    - **UI Stability:** Umstellung kritischer UI-States auf explizite `MutableState`-Objekte zur Vermeidung von "Value never read" Fehlinterpretationen durch die statische Code-Analyse.
    - **Formatting:** Projektweite Konsolidierung von Trailing Commas, Zeilenumbrüchen und Klammersetzung gemäß den neuesten Android-Coding-Standards.
    - **Visual Polish:** Optimierung der Konfetti-Animation durch variable Startpositionen für ein natürlicheres Erscheinungsbild.
100. **Code Analysis & Linting Finalization:**
    - **Final Polish:** Systematische Behebung verbleibender Code-Analysis Warnungen in zentralen Komponenten (`HomeViewModel`, `HomeScreen`, `BirthdayItem`, `FastScrollbar`, `DateUtils`, `SystemContactDataSource`).
    - **Clarity:** Einführung von "Clarifying Parentheses" für komplexe logische Ausdrücke und flächendeckende Integration von Trailing Commas zur Verbesserung der Diff-Lesbarkeit.
    - **Refined State:** Optimierung der `mutableStateOf` Initialisierungen durch explizite Parameterbenennung (`value = ...`).
101. **UX: Lottie Integration & UI Polish:**
    - **Onboarding UX:** Integration der `anim_contacts.json` Lottie-Animation in die `ContactsPage`. Dies sorgt für eine ansprechendere Nutzerführung während der Rechteanfrage.
    - **Empty State Delight:** Die Lottie-Animation wird nun auch im `EmptyListState` des Home-Screens angezeigt, wenn der Zugriff auf Kontakte noch nicht gewährt wurde.
    - **Architecture:** Einführung eines zentralen `ui.components` Packages und Verschiebung der `LottieIllustration` für eine bessere Wiederverwendbarkeit im gesamten Projekt.
    - **Code Quality:** Bereinigung ungenutzter Imports und Korrektur kleinerer Formatierungsfehler im Onboarding-Modul.
102. **Adaptive Design & Stability Overhaul:**
    - **App Framework:** Einführung von `AppResponsiveScaffold` und `AdaptiveContentContainer`. Die App unterstützt nun offiziell verschiedene Formfaktoren (Handy, Tablet, Chromebook) durch Nutzung von `WindowSizeClass`.
    - **UX:** Beseitigung von Layout-Sprüngen ("Jumping UI") beim App-Start durch Optimierung der TopBar-Insets und Einsatz von `animateContentSize`.
    - **Large Screen Optimization:** Auf breiten Bildschirmen wird der Inhalt nun zentriert und in der Breite begrenzt (max. 840dp), um eine bessere Lesbarkeit zu gewährleisten.
    - **Onboarding Fix:** Verbesserung der Onboarding-Seiten für kleine Displayhöhen (z.B. Chromebook im Landscape-Modus) durch flexiblere Abstände und optimiertes Scrolling.
    - **Dependency:** Integration von `material3-window-size-class` zur präzisen Erkennung der Bildschirmkonfiguration.
103. **UX Polish & Search Highlighting:**
    - **Visual Feedback:** Einführung eines dynamischen Highlighting-Effekts für die `SearchBar`. Bei Fokus wird nun eine animierte Umrandung gezeichnet und die Hintergrundfarbe sanft angepasst.
    - **Smart Focus Management:** Optimierung der Tastatur-Steuerung; der Fokus der Suche wird nun zuverlässig bei jeder Listen-Interaktion (Scrollen oder Aufklappen einer Karte) gelöscht, um eine freie Sicht auf die Inhalte zu gewährleisten.
    - **Layout Harmony:** Korrektur des Hintergrund-Übergangs in der `HomeTopBar`. Die Filter-Bar nutzt nun denselben Hintergrund wie die Liste, während der Suchbereich durch Elevation optisch abgehoben bleibt.
    - **Onboarding UX:** Verbesserung der Klickbarkeit und Sichtbarkeit der Onboarding-Buttons auf Geräten mit geringer Displayhöhe (z.B. Chromebooks im Landscape-Modus) durch optimiertes Spacing und Scrolling.
104. **Home Architecture & UX Refinement:**
    - **Sub-Component Grouping:** Strukturierung des `home.components` Ordners in spezialisierte Unterverzeichnisse (`list`, `topbar`, `actions`). Dies verbessert die Übersichtlichkeit bei wachsender Komponentenanzahl.
    - **Prop Drilling Elimination:** Konsequente Nutzung des `HomeActions` Wrappers in der gesamten Home-Hierarchie. Dies reduziert die Komplexität der Funktionssignaturen und vereinfacht die Wartbarkeit.
    - **Dynamic Scrollbar:** Die `FastScrollbar` unterstützt nun kontextsensitive Labels (Monat vs. Anfangsbuchstabe), was die Navigation im "Ohne Datum"-Tab signifikant verbessert.
    - **Padding Rule Compliance:** Refactoring der `BirthdayItem` Abstände auf eine nicht-additive Strategie (`bottom = 8.dp`), um Layout-Sprünge bei Animationen zu vermeiden.
    - **Consistency:** Vereinheitlichung der Accessibility-Beschreibungen und Trailing Commas im Home-Screen Bereich.

