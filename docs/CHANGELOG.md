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
