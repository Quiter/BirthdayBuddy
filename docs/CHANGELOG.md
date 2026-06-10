# Changelog: BirthdayBuddy
> **Note:** Historische Einträge (Meilensteine 1-99) wurden nach [CHANGELOG_ARCHIVE.md](CHANGELOG_ARCHIVE.md) verschoben, um das Kontext-Fenster für LLM-Sessions zu optimieren.

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
104. **Home Architecture & UX Refinement:**
    - **Sub-Component Grouping:** Strukturierung des `home.components` Ordners in spezialisierte Unterverzeichnisse (`list`, `topbar`, `actions`). Dies verbessert die Übersichtlichkeit bei wachsender Komponentenanzahl.
    - **Prop Drilling Elimination:** Konsequente Nutzung des `HomeActions` Wrappers in der gesamten Home-Hierarchie. Dies reduziert die Komplexität der Funktionssignaturen und vereinfacht die Wartbarkeit.
    - **Dynamic Scrollbar:** Die `FastScrollbar` unterstützt nun kontextsensitive Labels (Monat vs. Anfangsbuchstabe), was die Navigation im "Ohne Datum"-Tab signifikant verbessert.
    - **Padding Rule Compliance:** Refactoring der `BirthdayItem` Abstände auf eine nicht-additive Strategie (`bottom = 8.dp`), um Layout-Sprünge bei Animationen zu vermeiden.
    - **Consistency:** Vereinheitlichung der Accessibility-Beschreibungen und Trailing Commas im Home-Screen Bereich.
105. **Database Migration Fix (Stability):**
    - **Robust Migration:** Einführung einer manuellen Migration von Version 1 auf 2. Dies behebt einen kritischen Absturz (`SQLiteException: no such table: pending_notifications`), der bei einigen Nutzern auftrat, deren V1-Datenbank inkonsistent zum erwarteten Schema war.
    - **Safety First:** Die Migration prüft nun aktiv auf die Existenz der Tabelle und der Spalte `dismissCount`, bevor Änderungen vorgenommen werden. Dies stellt sicher, dass sowohl saubere Installationen als auch historisch gewachsene Datenbanken stabil auf V6 aktualisiert werden können.
    - **Quality Assurance:** Einführung von automatisierten Migrationstests (`MigrationTest.kt`), um die Korrektheit der manuellen und automatischen Migrationspfade vor jedem Release sicherzustellen.
106. **Gradle & Dependency Polish:**
    - **Deprecation Fix:** Umstellung der `sourceSets`-Konfiguration im `build.gradle.kts` auf die moderne `assets.directories.add()` API, um Warnungen bei der Schema-Bereitstellung für Tests zu vermeiden.
    - **Dependency Stability:** Korrektur eines Auflösungsfehlers bei der `kotlinx-serialization-json` Bibliothek durch Synchronisation des Version Catalogs.
    - **Code Quality:** Bereinigung redundanter String-Templates in der KSP-Konfiguration.
107. **Adaptive UI & Header UX Fix:**
    - **Trigger Logic:** Refactoring der `isFilterBarVisible` Logik im `HomeState`. Die Filterbar reagiert nun sofort auf den ersten Pixel eines Scroll-Vorgangs (`firstVisibleItemScrollOffset`), was die Navigation deutlich dynamischer macht.
    - **Header Visuals:** Einführung eines soliden Hintergrunds für die gesamte `HomeTopBar`. Dies beseitigt "Geister-Flächen" beim Scrollen, da die Liste nun sauber unter den Header gleitet.
    - **Adaptive Header:** Integration des `AdaptiveContentContainer` in die TopBar. Suche und Filter-Chips sind nun auch auf Tablets und breiten Bildschirmen perfekt mittig über der Liste ausgerichtet.
    - **Animation Cleanup:** Entfernung redundanter `animateContentSize` Aufrufe in der TopBar zugunsten der nativen `AnimatedVisibility` Logik, um Layout-Zittern zu verhindern.
    - **Framework Stability:** Korrektur des `AdaptiveContentContainer` Verhaltens; das erzwungene `fillMaxSize()` wurde entfernt, um eine korrekte Höhenberechnung für Header-Komponenten zu ermöglichen.
108. **Architecture Simplification & Code Cleanup:**
    - **Logic Delegation:** Auslagerung der Manipulations-Logik für Geschenkideen (Sortierung, Status-Wechsel) vom `HomeViewModel` in das `GiftIdea` Modell zur besseren Testbarkeit und Verschlankung der ViewModels.
    - **Filter Refactoring:** Extraktion der komplexen Kontakt-Filterlogik in die spezialisierte Hilfsfunktion `shouldShowContact` im `HomeViewModel`.
    - **Action Consolidation:** Einführung von `requestFilterReset()` zur Vereinheitlichung von Scroll-Reset und Filter-Status Triggern bei Nutzerinteraktionen.
    - **Linting & Stability:** Projektweite Behebung von Code-Analysis Warnungen (z.B. "Assigned value is never read" in der `SearchBar`) und Optimierung von Trailing Commas sowie Lambda-Syntax.
109. **UI & UX Final Polish:**
    - **Adaptive Framework:** Vereinfachung des `AppResponsiveScaffold` zur automatischen Verwaltung interner Paddings, was zu deutlich schlankeren und lesbareren Screen-Implementierungen führt.
    - **Visual Symmetry:** Anpassung der Typografie im `BirthdayStatus` an den `headlineContent` (Name) und `supportingContent` (Datum) zur Schaffung einer harmonischen und balancierten Kartenansicht.
    - **I18n Excellence:** Kleinschreibung des Präfix "in" für die Anzeige verbleibender Tage in Deutsch und Englisch zur Verbesserung des visuellen Rhythmus.
    - **Pull-To-Refresh Stability:** Endgültige Behebung von fälschlichen Refresh-Triggers bei Layout-Änderungen durch konsequente Isolierung der `PullToRefreshBox` vom dynamischen Scaffold-Padding.
    - **Legal UI Polish:** Einführung eines simplen Markdown-Renderers für die Datenschutzerklärung. Die `privacy_policy.md` wurde strukturell überarbeitet (H1/H2 Header, Aufzählungen), um eine ansprechende und lesbare Darstellung der rechtlichen Informationen zu gewährleisten.
110. **ViewModel Refactoring & Clean Architecture Decoupling:**
    - **Onboarding Separation:** Einführung des spezialisierten `OnboardingViewModel.kt`. Das Onboarding ist nun komplett vom `SettingsViewModel` entkoppelt, welches aufgrund von Redundanz vollständig gelöscht wurde.
    - **Clean Architecture Decoupling:** Beseitigung aller direkten Abhängigkeiten von Android `Context`, Glance, WorkManager und Coil aus den ViewModels (`HomeViewModel`, `LabelViewModel`, `BackupViewModel`, `NotificationViewModel`).
    - **Hilt-Injected Helper Interfaces:** Einführung von `WidgetUpdater`, `NotificationScheduler` und `ImagePrefetcher` zur Entkopplung der System-APIs. Dies verbessert die JVM-Unit-Testbarkeit der ViewModels drastisch.
    - **Documentation:** Aktualisierung von `PROJECT_STRUCTURE.md` zur Abbildung der neuen Struktur.
111. **Database Migration Hardening (Update Safety):**
    - **Robust Migration 5->6:** Erhöhung der Ausfallsicherheit bei der Migration von Version 5 auf 6. Die App ermittelt nun zur Laufzeit dynamisch die in `contacts_old` vorhandenen Spalten via `PRAGMA table_info`. Dies fängt Situationen ab, in denen vorherige Migrationsschritte (wie das Hinzufügen von `phoneNumber` in V4 oder Messenger-Flags in V5) auf Nutzersystemen fehlgeschlagen oder übersprungen wurden. Fehlende Spalten erhalten beim Kopieren sichere Standardwerte (`NULL` bzw. `0`).
    - **Wiederherstellungs-Fallback:** Tritt bei der Migration 5->6 dennoch ein kritischer Fehler auf, wird die alte Tabelle `contacts_old` im `catch`-Block automatisch wieder in `contacts` umbenannt. Dadurch wird jeglicher Datenverlust ausgeschlossen und ein stabiler App-Start gewährleistet.
112. **Database Migration Bulletproofing (NOT NULL Constraint Alignments):**
    - **NOT NULL Schema Alignment (V7):** Behebung eines latenten Schema-Fehlers bei der Migration auf V7. Die Spalte `giftIdeas` wurde bei der Tabellenrekonstruktion in `MIGRATION_5_6` fälschlicherweise als nullable (`TEXT`) statt `TEXT NOT NULL` angelegt, was beim anschließenden Validierungsschritt von Room (V7) zu einem Absturz führte.
    - **Shared Table Reconstruction:** Zusammenführung und Modularisierung der Tabellen-Rekonstruktionslogik in die private Hilfsfunktion `recreateContactsTable(db)`. Diese befüllt leere/fehlende JSON-Spalten (`labels` & `giftIdeas`) nun mittels `COALESCE` mit sicheren leeren Arrays (`'[]'`) und erzeugt das exakte V7 `NOT NULL` Schema.
    - **Migration 6->7 Hardening:** Integration der Härtung in `MIGRATION_6_7`. Falls User die fehlerhafte Zwischenversion V6 installiert hatten (wo `giftIdeas` noch nullable war), migriert `MIGRATION_6_7` die Tabelle nun ebenfalls automatisch auf das korrekte V7 `NOT NULL` Constraint.
    - **Test Coverage & Validation:** Behebung von Kompilierungsfehlern in den instrumentierten Repositories-Tests. Erfolgreicher Durchlauf aller 29 automatisierten Migrations- und Systemtests auf dem Android-Emulator.
113. **Responsive Architecture & Lifecycle Hardening:**
    - **Adaptive UI-Architektur:** Einführung von `LocalWindowWidthSizeClass` als `CompositionLocal` zur vereinfachten, abfragefreien Weitergabe der Bildschirmbreitenklasse. Bereinigung des `AppResponsiveScaffold` durch Beseitigung redundanter Überlagerungsebenen (`zIndex(1f)`), Bereitstellung nativer Scaffold-Parameter (FAB-Position, Content-Farbe, Window-Insets) sowie Einführung einer flexiblen `consumePadding` Steuerung.
    - **Akkuschonende Inaktivitätsprüfung:** Ersatz der CPU-intensiven LaunchedEffect-Dauerschleife (`while(true) delay(10000)`) in der `MainActivity` durch einen modernen, lifecycle-basierten `LifecycleEventObserver`. Die Inaktivitätsprüfung (5-Minuten-Limit) erfolgt nun batterieschonend ausschließlich bei der Wiederaufnahme der App in den Vordergrund (`ON_RESUME`).
    - **Robustes Intent-State-Handling:** Behebung von Fehlern bei widget- oder benachrichtigungsbasierten App-Aufrufen im Hintergrund durch Einführung des reaktiven Compose-States `activityIntent`. Intents werden bei `onNewIntent` nun zuverlässig in den State-Tree propagiert und triggern die Navigation.
    - **Hilt-Dependency-Optimierung:** Performanz-Steigerung der Dependency Injection durch Annotation aller Room DAOs (`AppModule.kt`) mit `@Reusable`. Dies ermöglicht eine effiziente Wiederverwendung der Instanzen ohne die Synchronisations- und Speicher-Overheads von `@Singleton`.
    - **Moderne API-Migration:** Ablösung veralteter Lifecycle-Schnittstellen durch Migration auf das moderne `androidx.lifecycle.compose.LocalLifecycleOwner` in der Compose-Schicht.
114. **Notification Settings, UX Polish & Animation Optimization:**
    - **Edit Notification Rule UI Redesign:** Vollständiges Redesign des `EditRuleDialog`. Der veraltete Slider wurde durch ein präzises textbasiertes Eingabefeld (`OutlinedTextField`) für Vorwarnzeiten in Kombination mit einer Einheiten-Auswahl (Tage vs. Wochen via RadioButtons) ersetzt. Integration des System-TimePickers und Hinzufügen von Plural-String-Ressourcen für fehlerfreie Lokalisierung.
    - **Auto-Sync & Preloading Architecture:** Verlagerung des manuellen, rechteabhängigen Kontaktsynchronisations-Triggers aus dem `HomeScreen` direkt in den `init`-Block des `HomeViewModel` zur Vermeidung von Race-Conditions. Verschiebung des Bild-Preloadings aus UI-seitigen `LaunchedEffects` in das ViewModel via `ImagePrefetcher`-Hilfsklasse.
    - **Smooth List Animations:** Einführung eines intelligenten `skipPlacementAnimation`-Status in `BirthdayList` zur vorübergehenden Deaktivierung von Platzierungsanimationen beim Wechsel von Suchanfragen oder Filter-Labels. Optimierung der Ein- und Ausblendanimationen (`fadeInSpec`, `fadeOutSpec`, spring-basierte `placementSpec` in `Modifier.animateItem()`) für flüssige Übergänge.
    - **Onboarding UX Optimization:** Deaktivierung automatischer Seitenwechsel beim Erteilen von Berechtigungen im Onboarding-Screen zur Vermeidung unruhiger UX. Verbesserung des `OnboardingFooter` durch `fillMaxWidth` zur sauberen responsiven Darstellung auf breiten Bildschirmen.
    - **I18n Plurals:** Einführung von Quantity Strings (`dialog_rule_unit_days`, `dialog_rule_unit_weeks`) in Deutsch und Englisch zur korrekten Pluralisierung bei numerischen Angaben im Erinnerungsdialog.
    - **Recomposition Optimization:** Nutzung von `rememberUpdatedState` for `onNavigateToSettings` im `HomeScreen` zur Reduzierung unnötiger Re-Instanziierungen des `HomeActions`-Wrappers.
    - **Version Bump:** Anhebung der App-Version auf Version Name `2.3.18` (Version Code `27`) in `build.gradle.kts`.
115. **Onboarding UX & Database Resiliency:**
    - **Onboarding Streamlining:** Vereinfachung der `ContactsPage` durch Ersetzung der komplexen `LottieIllustration` durch ein Standard-Material-`Icon` für Kontakte, um die UI-Ladezeiten und Rendergeschwindigkeit zu optimieren.
    - **Database Hardening:** Absicherung der Hilt-Datenbank-Erstellung in `AppDatabase.kt` durch ein try-catch-Konstrukt. Bei schwerwiegender Schema-Korruption löscht die App nun die fehlerhafte Datenbankdatei automatisch via `deleteDatabase` und baut eine saubere neue Instanz auf, was App-Startabstürze auf Altgeräten verhindert.
    - **Resource Reorganization:** Ausgliederung von Drittanbieter-Messenger-Drawables (`ic_discord.xml`, `ic_whatsapp.xml` etc.) in einen eigenen Source-Set-Ordner (`src/main/res-messenger/drawable`), um das Hauptressourcen-Verzeichnis sauber zu halten.
116. **Version Upgrade & Warning Resolution:**
    - **Release Version Code:** Anhebung der App-Version auf Version Name `2.4.0` (Version Code `28`) in `build.gradle.kts`.
    - **Code Cleanup & Linting:** Behebung diverser Compiler- und Linter-Warnungen im gesamten Projekt:
      - Beseitigung von `LocalContext.current` Ressourcen-Abfragewarnungen in `ContactActionRow` durch direkte Sortierung der Messenger nach Enum-Namen.
      - Ausrichtung von `AppResponsiveScaffold` an Compose-Designrichtlinien durch Positionierung des `modifier` Parameters als ersten optionalen Parameter.
      - Ergänzung von `@Suppress("unused")` Annotationen in Hilt-DI-Modulen und Bereinigung toten Codes in `MessengerApp`.
      - Verschiebung von `widget_preview.webp` in `drawable-nodpi/` zur Vermeidung von Density-Warnungen.

117. **Dynamic Glance Widget Styling (Widget Optics Redesign):**
    - **Visual Row Design:** Redesign der `BirthdayRow` im Jetpack Glance Homescreen-Widget passend zum kartenbasierten Design der App. Einführung farbiger, abgerundeter Zeilen-Hintergründe (`12.dp` Ecken) basierend auf dem Geburtstags-Status (Gold für runde Geburtstage, Green für Kinder 0-9, Silber für sonstige).
    - **Dynamic Spacing & Heights:** Implementierung einer dynamischen Höhenberechnung (`dynamicBlockHeight`), um die verfügbare Gesamthöhe des Widgets gleichmäßig auf alle gerenderten Geburtstage aufzuteilen. Hinzufügen von Margins zwischen den Zeilen für ein saubereres, luftigeres Layout.

118. **Launcher Icon Restoration & Scale Optimization:**
    - **Directory Restoration:** Rücknahme der fehlerhaften Verschiebung der XML-Dateien von `mipmap-anydpi` zurück nach `mipmap-anydpi-v26/` (`ic_launcher.xml` und `ic_launcher_round.xml`). Dies behebt das Problem, dass Launcher auf API 26+ fälschlicherweise die WebP-Templates mit grünem Hintergrund statt der XML-Definitionsdatei luden.
    - **Scale Optimization:** Korrektur des Skalierungsfaktors der Logo-Gruppe in `ic_launcher_foreground.xml` zurück auf `0.5` und Zentrierungsversatz auf `6`. Das Geschenkbox-Logo sitzt nun wieder perfekt zentriert in kreisförmigen und abgerundeten Schablonen, ohne abgeschnitten oder übergroß zu wirken.

119. **Android Contact Birthday Update & Test Coverage Overhaul:**
    - **Schreibschutz-Behebung (RawContacts):** Behebung des Fehlers beim Schreiben von Geburtstagen direkt in Android-Kontakte durch intelligentes Ausfiltern von schreibgeschützten Dritthersteller-Konten (z. B. WhatsApp, Signal) und gezielte Priorisierung beschreibbarer Google- oder lokaler Rohkontakte.
    - **Silent Runtime Permission:** Einführung einer lautlosen Berechtigungsprüfung für `WRITE_CONTACTS` zur Laufzeit. Falls die Leseberechtigung (`READ_CONTACTS`) bereits vorliegt, holt die App die Schreibberechtigung im Hintergrund unsichtbar ein, um Abstürze zu verhindern und sofortiges Bearbeiten ohne App-Neustart zu ermöglichen.
    - **Fehler-Protokollierung:** Aktives Logging von Fehlern beim Ändern von Kontaktdaten im `SystemContactDataSource` via `Log.e` zur besseren Diagnose statt leisem Verschlucken von Ausnahmen.
    - **Automated Instrumented Testing:**
      - **SystemContactDataSourceTest:** Neuer instrumentierter Test zur präzisen Validierung von Kontakt-Updates und -Inserts sowie der korrekten Selektion beschreibbarer Konten.
      - **NotificationHelperTest:** Neuer instrumentierter Test zur Überprüfung der System-Benachrichtigungen inklusive Polling gegen OS-Latenz und JUnit-`GrantPermissionRule` for Benachrichtigungen ab Android 13.
      - **Test-Dependencies:** Integration der `androidx.test:rules:1.6.1` Abhängigkeit in die `build.gradle.kts` zur Beseitigung von Referenz- und Kompilierungsfehlern in den instrumentierten Testläufen.

120. **Unified Onboarding Architecture & Layout Polish:**
    - **Onboarding-Seiten-Template:** Einführung der standardisierten `OnboardingPageTemplate` Komponente zur einheitlichen Strukturierung aller Onboarding-Seiten (Willkommen, Kontakte, Benachrichtigungen, Kalender, Abschluss-Seite).
    - **Perfekte visuelle Symmetrie:** Ausrichtung aller Hauptelemente (Icon, Titel, Beschreibungstext, Einstellungs-Cards und Buttons) auf exakt denselben horizontalen Linien über den gesamten Onboarding-Prozess hinweg.
    - **Absolute Layout-Stabilität (Sprungfrei):** Beseitigung aller vertikalen Layout-Sprünge (Jumping UI). Durch Umstellung auf Top-Ausrichtung (`Arrangement.Top`) und Einbettung der Aktionsbuttons in eine Box mit fester Höhe (`56.dp`) bleibt der obere Bereich (Icon, Titel, Beschreibung) absolut stabil zentriert, selbst wenn Einstellungsoptionen verändert oder Buttons ein-/ausgeblendet werden.
    - **Konsistente Benutzerführung (Kontakte-Switch):** Ersatz der komplexen Dropdown-Lösung auf der Kontakte-Seite durch eine edle Einstellungs-Card mit einem Switch ("Kontakte synchronisieren"). Dadurch verhält sich die Kontakte-Berechtigung exakt genauso wie die Benachrichtigungen und der Kalender: Das Überspringen deaktiviert den Switch und erfordert wie gewünscht den manuellen Klick auf "Weiter" im Footer.
    - **Visueller Rhythmus:** Anhebung des unteren Abstands zum Footer im Compact-Layout von `8.dp` auf `24.dp` für ein luftigeres und moderneres Gesamtbild.

121. **UX & UI Consistency Refinement:**
    - **Sync Feedback:** Einführung einer Snackbar-Bestätigung im `SyncSettingsScreen`, sobald die manuelle Kontaktsynchronisierung erfolgreich abgeschlossen wurde.
    - **Architecture:** Erweiterung des `HomeViewModel` um ein `syncCompletedEvent` (SharedFlow), um transiente UI-Ereignisse sauber zu entkoppeln.
    - **I18n:** Hinzufügen des neuen `sync_success` Strings in Deutsch und Englisch.
    - **BirthdayItem Layout:** Anpassung des `BirthdayStatus` in der App an die Informationshierarchie des Widgets. Das kommende Alter steht nun oben (fett/primär), während die verbleibenden Tage bzw. der Status "Heute!" darunter angezeigt werden.

122. **UI & Code Polish (FastScrollbar Refactoring & Visual Fix):**
    - **Saubere Komponenten-Strukturierung:** Internes Refactoring von `FastScrollbar.kt` zur Steigerung der Übersichtlichkeit, Lesbarkeit und langfristigen Wartbarkeit.
    - **Sub-Composables:** Erfolgreiche Auslagerung der Month/Letter-Label-Anzeige in ein privates `ScrollbarBubble` Composable sowie des kompletten Zeichen- und Animationsbereichs von Track und Thumb in ein privates `ScrollbarTrackAndThumb` Composable.
    - **Erhalt der Kapselung:** Beibehaltung aller Funktionalitäten und Zustände in einer einzigen Datei, um den globalen Namensraum schlank zu halten und das hochspezialisierte Gesten-Handling an einem Ort gebündelt zu wissen.
    - **Overlay-Schnittstelle (Popup-Bubble):** Umstellung des `ScrollbarBubble` Renderings auf die Jetpack Compose `Popup`-API. Dies verhindert Clipping an den Containergrenzen und stellt sicher, dass die Bubble beim Scrollen ganz oben vor (statt hinter) der `HomeTopBar` gezeichnet wird. Die sanften Ein- und Ausblendungsanimationen bleiben über einen `MutableTransitionState` vollständig erhalten.
    - **Gesten-Stabilität (Jump-Fix):** Deaktivierung der automatischen Fenster-Positionskorrektur (`clippingEnabled = false` in `PopupProperties`). Dies verhindert, dass der Android-Fenstermanager die Bubble bei Annäherung an den oberen Bildschirmrand (durch den negativen Offset zum Ausrichten am Daumen) verzögert korrigiert und nach oben verschiebt ("hüpfen" lässt). Die Position bleibt dadurch absolut stabil und folgt präzise dem Finger.

123. **Performance Optimization & UI Modularization:**
    - **Background Filtering Performance:** Added `.flowOn(Dispatchers.Default)` to the combined `filteredContacts` flow in `HomeViewModel.kt`. This ensures that heavy list processing, especially on larger contact databases (>1000 contacts), runs asynchronously off the Main thread, preserving 60fps scrolling and preventing UI frame drops during active searching or filtering.
    - **DatePicker Modularization:** Extracted the inline `DatePickerDialog` logic from the large `BirthdayItem.kt` file into a dedicated, reusable, and testable `@Composable` file `BirthdayDatePickerDialog.kt` in the home list components directory. This improves the separation of concerns and reduces the code footprint of the contact card component.
    - **Compiler Warnings Resolution:** Addressed Kotlin/Hilt compiler warnings in `SystemContactDataSource.kt`, `CalendarSyncRepository.kt`, and `ContactRepository.kt` by replacing `@ApplicationContext` with the targeted `@param:ApplicationContext` annotation on constructor context properties.
    - **Documentation:** Recorded the design constraint regarding legacy database migrations in `docs/PROJECT_STATUS.md` and registered the new file in `docs/PROJECT_STRUCTURE.md`.

124. **Root Directory Cleanup & Workspace Organization:**
    - **Script Organization:** Moved all test and inspection scripts (`scratch_*.py`) from the root directory to a newly created `scripts/` directory to keep the root directory neat and tidy.
    - **Visual Assets:** Moved all visual validation assets (`screen.png` and `screen_notifications.png`) to the `docs/` directory to prevent cluttering the project root.
    - **Git Ignore Security:** Added explicit rules to `.gitignore` (`/birthday_database*` and `/settings_database*`) to prevent local SQLite testing and cache databases from being accidentally tracked or committed to Git.
    - **Documentation:** Updated `docs/PROJECT_STRUCTURE.md` to document the new `scripts/` directory structure.

125. **Calendar Sync Integration, Dynamic Onboarding & Setup Guide:**
    - **Dynamic Legacy Account Cleanup:** Refactored `CalendarSyncRepository.kt` to dynamically read the account name of any existing calendar. If it does not belong to the highly-compatible local `"phone"` account (such as legacy `"BirthdayBuddy"` or package name accounts), the app automatically purges and recreates it. This makes the calendar instantly visible in the side drawer of Google Calendar and standard system calendar apps.
    - **Step-by-Step Setup Guide (`SetupStepsCard`):** Replaced the simple calendar permission warning card in settings with a highly interactive, responsive step-by-step stepper guide. It dynamically checks off steps with satisfying green ticks in real-time as permissions are granted and sync is activated.
    - **Onboarding Tutorial Page (`CalendarGuidePage`):** Implemented a new onboarding tutorial page that visually details how to display the synced "BirthdayBuddy" calendar in external apps (opening the hamburger menu and checking the box).
    - **Dynamic Onboarding Pages scaling:** Updated `OnboardingScreen.kt` to dynamically scale the onboarding pager count from 5 to 6 pages ONLY when calendar sync is enabled and permission is granted, seamlessly bypassing the tutorial if sync is disabled.
    - **Calendar App Link Intent:** Integrated a robust deep-link button (`openDefaultCalendarApp`) in both the onboarding tutorial and settings pages to launch the user's preferred standard Calendar application using a two-stage Intent routing (direct Calendar URI fallback to `Intent.CATEGORY_APP_CALENDAR`), ensuring compatibility across 100% of Android devices.
    - **Clean Compile & I18n:** Added full English and German translations for all guide steps, rearranged state variable declarations to resolve reference ordering bugs, and resolved the `Assigned value is never read` warning, ensuring a 100% clean Gradle compilation.

126. **Clean Architecture & MVVM Refactoring:**
    - **Image Prefetching:** Relocated the image prefetching logic from `HomeViewModel` into a Compose `LaunchedEffect(uiState.contacts)` in `HomeScreen`, decoupling the ViewModel from Coil (image loading library). Fully deleted the redundant `ImagePrefetcher` interface and implementation.
    - **Reactive Widget Updates:** Centralized all Glance AppWidget updates inside the `ContactRepository` data modification methods (`syncContacts`, `updateGiftIdeas`, `updateLabelConfig`), making widget updates fully reactive. Removed `WidgetUpdater` constructor dependency, imports, and manual update calls from `HomeViewModel`, `BackupViewModel`, and `LabelViewModel`.
    - **Gift Ideas Logic Migration:** Shifted the core business logic of adding, toggling, deleting, and updating gift ideas out of `HomeViewModel` and into `ContactRepository` to establish a clean separation of concerns and a single source of truth.
    - **Test Coverage & Quality:** Cleaned up Hilt instrumented and unit tests to align with the new ViewModel constructors. Verified a 100% successful Gradle build and test completion.
127. **Premium Wheel Date Picker & Scrollbar Overlap UX Overhaul:**
    - **Premium Wheel Date Picker:** Ersetzung des standardmäßigen Material 3 DatePickers durch einen extrem hochwertigen, haptisch optimierten Wheel-DatePicker in Walzenoptik in `BirthdayDatePickerDialog.kt`. Er nutzt Compose Snapping (`rememberSnapFlingBehavior`) und mechanische haptische Ticks (`HapticFeedbackType.LongPress`) beim Einrasten für ein absolut physisches Bediengefühl.
    - **Dynamic Year Toggle:** Integration eines „Inklusive Jahr“ Switches (voll lokalisiert in DE & EN). Beim Deaktivieren blendet sich die Jahres-Walzenspalte aus und Tag sowie Monat zentrieren sich harmonisch.
    - **Leap-Year-Safe Clamping:** Automatische live-Berechnung der Tagesanzahl pro Monat. Der Februar erlaubt den 29. Tag nur dann, wenn ein Schaltjahr aktiv und das Jahr eingeschaltet ist. Ansonsten wird die Auswahl zur Vermeidung ungültiger `LocalDate`-Ausnahmen auf 28 Tage begrenzt.
    - **Smart Date Pre-Selection:** Ergänzung von `birthday: LocalDate?` im `ContactUiModel` zur intelligenten Vorauswahl des bereits hinterlegten Geburtstages beim Öffnen.
    - **Scrollbar Overlap Spacing Fix:** Lösung des Touch-Konflikts an der rechten Bildschirmkante in `BirthdayStatus.kt`. Das Plus-Symbol wird nun durch einen standardmäßigen M3 `IconButton` gerahmt (48.dp Touchtarget) und um `12.dp` nach links verschoben, um es aus der aktiven Scrollbar-Gesten-Zone zu holen. Normale Datumsanzeigen erhalten ein konsistentes `8.dp` Padding zur eleganten optischen Trennung.

128. **FastScrollbar Smooth Bubble & Static Popup Overhaul:**
    - **Lag-free Scrolling:** Migration of the `ScrollbarBubble` from dynamic `Popup` window repositioning to a hybrid static `Popup` approach. The `Popup` window itself now remains 100% static (`IntOffset.Zero`), while the bubble content is translated smoothly using GPU-accelerated `.graphicsLayer { translationY = ... }`.
    - **Z-Index & Overlay Safety:** Preserved the use of the `Popup`-API, ensuring the bubble is drawn in a separate window layer on top of all other elements (including `HomeTopBar`) without clipping.
    - **Boundary Safety:** Implemented a translation clamp (`.coerceAtLeast(0f)`) to gracefully stop the bubble at the very top of the visible screen without going behind the top bar or off-screen.

129. **Integration of "Weitere Ereignisse" (Anniversaries & Name Days):**
    - **Database Schema Upgrades:** Bumped `SettingsDatabase` to version 3 (with `MIGRATION_2_3` to add `otherEventsEnabled`) and `AppDatabase` to version 8 (with `MIGRATION_7_8` to add `anniversary` and `nameDay` columns).
    - **System Contacts Integration:** Expanded `SystemContactDataSource.kt` to extract anniversaries and name days from the system Contacts provider using MIME-type custom label matches (e.g., anniversary/hochzeitstag, name day/namenstag).
    - **Logic Mapping & UI Filters:** Refactored `ContactMapper.kt` to calculate days remaining and count (anniversary year) dynamically. Added `LABEL_ANNIVERSARY` and `LABEL_NAME_DAY` pseudo-labels on the Home screen to filter contacts by event, sorted by the remaining days to that event.
    - **Settings Screen Integration:** Implemented `OtherEventsSettingsScreen.kt` with toggle switches, routing, and immediate cache synchronization upon activation.
    - **Notification Scheduling:** Updated background scheduling in `NotificationWorker.kt` and `SnoozeWorker.kt` using key prefixes to isolate anniversaries/name days and prevent collision with standard birthday keys in database logs.
    - **Calendar Synchronization:** Updated `CalendarSyncRepository.kt` to support syncing wedding anniversaries and name days into the local Android system calendar ("BirthdayBuddy") alongside birthdays when "Weitere Ereignisse" is enabled, complete with English and German translations for event titles and descriptions.
    - **Test Coverage:** Added `SettingsMigrationTest.kt` validating v2 to v3 schema changes, updated `ContactMapperTest.kt` with custom event cases, added ViewModel filtering verification, and resolved flow deadlock issues in mock tests, achieving 100% test completion on Android emulators.

130. **Joint Wedding Anniversary Contact Coupling (Ehepaar-Verknüpfung):**
    - **Logic & Merging:** Implementation of a pairing mechanism for spouses sharing the same wedding anniversary date. Unlinked contacts with the same anniversary date are merged into a single `ContactUiModel` inside the "Hochzeitstag" filter, showing joint names (using smart merging in `DateUtils.kt`, e.g. "Max & Erika Mustermann") and double/overlapping contact avatars in `ContactImage.kt`.
    - **Database Schemas:** Added `spouseLookupKey` to `contacts` (AppDatabase v9, `MIGRATION_8_9`) and to `contact_user_data` (SettingsDatabase v4, `MIGRATION_3_4`) to track pairings bidirectionally. Added `ignoredCouplePairs: List<String>` serialized field in `AppSettings` to keep track of declined suggestions.
    - **Notification & Calendar Integration:** Grouped coupled reminders into a single push notification with custom title ("Morgen ist der 5. Hochzeitstag von Max & Erika Mustermann!") and single description, and wrote a single joint event in the Android system calendar.
    - **UI & Interaction:** Added a `CoupleSuggestionBanner` at the top of the anniversary page proposing to link detected spouses. Added an "Entkoppeln" (Unlink) button inside the action row of merged cards.
131. **Reset Ignored Couple Suggestions via Pull-to-Refresh:**
    - **Manual Suggestion Reset:** Updated manual pull-to-refresh on the Home Screen (`syncContacts(showLoading = true)`) to trigger `contactRepository.clearIgnoredCouplePairs()`, which clears the list of ignored couple suggestion pairs.
    - **Re-Scan Trigger:** This allows users to re-scan and get pairing suggestions again for couples they previously chose to ignore (clicked "Nein" on).
    - **Robust Testing:** Updated database migration tests, unit tests, and instrumented Android tests to cover the new pairing and suggestion reset behaviors, resolving coroutine race conditions.

132. **Visual & Resource Polish (Google Meet Icon Overhaul):**
    - **Optimized SVG/Vector Asset:** Updated `ic_google_meet.xml` to use the optimized vector pathing and a clean yellow-orange gradient matching the updated Google Meet video camera branding.
    - **Color Adjustment:** Shifted Google Meet's branding color definition in `MessengerApp.kt` from teal (`0xFF00897B`) to yellow (`0xFFFFEB3B`) to align with the new vector resource.
    - **Linter & Warning Cleanups:** Addressed minor compiler and linter warnings in `HomeScreen.kt`, `HomeState.kt`, `HomeFAB.kt`, `BirthdayDatePickerDialog.kt`, `BirthdayList.kt`, and `HomeViewModel.kt` (including using explicit `.milliseconds` durations in `delay`).

133. **Bugfix: Calendar Sync Event Duplication Overhaul:**
    - **Physical Event Deletion:** Changed the delete query in `CalendarSyncRepository.kt` to append `CALLER_IS_SYNCADAPTER = true` with `"phone"` as account details when purging old calendar events. This ensures that old events are permanently deleted instead of leaving tombstone entries (`deleted = 1`), preventing events from piling up and duplicating.
    - **Duplicate Calendar Clean-Up:** Refactored `findExistingCalendarId()` to `cleanDuplicateCalendarsAndGetId()`. The repository now scans all local calendars on start and sync, keeping only the first valid `BirthdayBuddyCalendar` (under the `"phone"` account/LOCAL type) and permanently deleting any other duplicate or legacy calendars.
    - **Hardened Delete Calendar:** Updated the `deleteCalendar()` method to sweep through the entire system and delete *all* matching `BirthdayBuddyCalendar` databases if the sync is disabled, preventing orphaned calendars when reinstalling or clearing app storage.

134. **Multi-Calendar Isolation under Custom Account ("BirthdayBuddy"):**
    - **Separate Calendars by Event Type:** Split the single calendar sync into three distinct calendars:
        - **Birthdays (Geburtstage):** Pink color (`#E91E63`)
        - **Anniversaries (Hochzeitstage):** Purple color (`#9C27B0`)
        - **Name Days (Namenstage):** Orange color (`#FF9800`)
    - **Custom Account Labeling:** Changed the local account name from `"phone"` to `"BirthdayBuddy"` (under account type `"LOCAL"`). This re-labels the calendar group in external calendar apps (e.g. Google Calendar side navigation) from "LOCAL" or "phone" to **"BirthdayBuddy"** and allows users to check/uncheck and color-code the three categories independently.
    - **Dynamic ID Lookup:** Deprecated the local database persistence of `calendarId` in `AppSettings` in favor of dynamic name-based lookup of the active calendars. This eliminates potential database-out-of-sync bugs when clearing app storage or updating the app.
    - **Garbage & Orphan Cleanups:** Integrated automatic cleanup inside `cleanCalendars()` to delete old legacy calendars (`\"BirthdayBuddyCalendar\"` on the `\"phone\"` account) and any duplicate calendars, leaving exactly one clean calendar per category.
    - **I18n:** Added localized display names for the three calendar titles in both German and English (`strings.xml`).

135. **In-App Calendar Color Picker Settings:**
    - **In-App Color Selector:** Integrated dynamic calendar color configuration rows directly into the `CalendarSettingsScreen.kt` UI when calendar synchronization is active.
    - **Harmonized Color Palette:** Implemented a premium Material 3 `ColorPickerDialog` containing a grid of 8 harmonized colors (Pink, Violet, Blue, Cyan, Green, Amber, Orange, Brown) with visual selection feedback, allowing custom color selections to bypass Google Calendar's missing native option for custom sync adapters.
    - **Settings Database V5 Migration:** Upgraded `SettingsDatabase` to version 5, adding `birthdayCalendarColor`, `anniversaryCalendarColor`, and `nameDayCalendarColor` columns with native default colors. Wrote `SettingsMigrationTest.kt` unit test cases to verify database structure and default value integrity.
    - **I18n Localization:** Fully localized dialog headers, cancel actions, and setting section descriptions in both English and German.
136. **Master-Detail Tablet Layout & Code Cleanups:**
    - **Responsive Master-Detail Split Screen:** Refactored `HomeScreen.kt` and `BirthdayList.kt` to support a premium Master-Detail layout on tablets and desktops (`windowWidthSizeClass != WindowWidthSizeClass.Compact`).
    - **Selection Highlighting:** Added selection tracking and primary container highlighting to contact item cards (`BirthdayItem.kt`) when rendered inside the Master list.
    - **Detail Panel (BirthdayDetailPane.kt):** Created a dedicated, scrollable details panel on the right pane displaying large contact avatars, name, status, contact actions (Call, SMS, WhatsApp), and the interactive Gift Ideas list.
    - **Proportional Avatar Sizing:** Enhanced `ContactImage.kt` to accept a custom `size: Dp` parameter, allowing avatars to scale proportionally (including nested avatar overlays for couples) in the details panel.
    - **Automatic Selection:** Implemented automatic selection of the first contact in the list on tablets to ensure the details pane is initialized with data on startup, while retaining full inline expandable behavior on phone form factors.
    - **Verification:** Verified compilation and executed gradle unit tests successfully.

137. **Full-Width TopBar & Settings Master-Detail Layout:**
    - **Full-Width Home TopBar:** Removed the zentrierende `AdaptiveContentContainer` from `HomeTopBar.kt`, allowing the search bar and label filter chips to span the entire screen width on tablets for a cleaner, modern look.
    - **Settings Master-Detail Layout:** Implemented a native Android Settings-like split-pane layout for tablets on the `SettingsScreen`. The left pane displays the categories menu (with selected tab highlighting), and the right pane displays the active settings sub-page.
    - **Nested Detail Pane Navigation:** Handled sub-screen transitions (like opening the Privacy Policy inside the About screen) directly within the right detail pane.
    - **Dynamic Back Navigation:** Added a `showBackButton` parameter to all settings sub-screens (Notifications, Calendar, Labels, Backup, Sync, Other Events, About, Privacy Policy) to conditionally hide the top bar back button when they are embedded in the split layout.
    - **Scaffold Width Flex:** Added a `useAdaptiveWidth` flag to `AppResponsiveScaffold` to allow disabling the zentrierende max-width constraint for full-bleed screens.

138. **Google Standard M3 Adaptive ListDetailPaneScaffold Integration:**
    - **Adaptive Layout Upgrade:** Migrated the manual tablet/desktop `Row`-based Master-Detail split layout in `HomeScreen.kt` to the official Google standard `ListDetailPaneScaffold` from the `androidx.compose.material3.adaptive` library.
    - **Smooth Transitions:** Wrapped pane contents in `AnimatedPane` to enable standard layout transitions and posture-aware scaling.
    - **Hybrid Smartphone/Tablet UX:** Retained the inline tap-to-expand list layout on smartphones (`Compact` size class) to preserve existing UX design rules, while dynamically loading the dual-pane scaffold on larger screens (Tablet/Desktop).
    - **Dependencies:** Added `androidx-compose-material3-adaptive`, `androidx-compose-material3-adaptive-layout`, and `androidx-compose-material3-adaptive-navigation` to `libs.versions.toml` and `app/build.gradle.kts`.
    - **Compiler Warnings Cleanups:** Upgraded Compose plugin compiler version to `2.4.0` in `libs.versions.toml`. Cleaned up unused `AboutSection` composable in `AboutScreen.kt` and removed unused context/versionName variables from `SettingsFooter` inside `SettingsScreen.kt` to ensure warning-free compilation.

139. **Codebase, Performance, Lifecycle & Resources Optimization:**
    - **FastScrollbar Performance:** Migrated `thumbOffset` to lambda-based state reading (`() -> Dp`) inside [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt), avoiding parent recompositions during list scrolling and dragging.
    - **Lifecycle Safety:** Replaced `collectAsState()` with `collectAsStateWithLifecycle()` in [CalendarSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/calendar/CalendarSettingsScreen.kt) and [OtherEventsSettingsScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/otherevents/OtherEventsSettingsScreen.kt) to pause flow collection when the app runs in the background.
    - **Adaptive Scaffold UX:** Integrated `BackHandler` and explicit `navigator.navigateTo` calls for the `ListDetailPaneScaffold` in [HomeScreen.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeScreen.kt) to ensure correct pane transitions and system back button behavior.
    - **Permission State UX:** Added a `LaunchedEffect(contacts)` block in [BirthdayList.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/BirthdayList.kt) to re-evaluate contacts permissions dynamically when data changes, fixing stuck permission prompts in EmptyListState.
    - **Resource Clean Up:** Removed unused colors from [colors.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/colors.xml) and obsolete string resources from both English [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) and German [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) to clean up compiler warnings and decrease package footprint.
    - **Legacy delay Warning:** Converted the legacy `delay(Long)` call to the modern `Duration` overload using `.milliseconds` in [FastScrollbar.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/components/list/FastScrollbar.kt) to resolve compiler warnings.

140. **Notification Localization (Plurals) & Code Cleanup:**
    - **Notification Plurals:** Converted 11 static anniversary and nameday notification string resources to `<plurals>` in both [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) and German [strings.xml](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) (e.g., `notif_title_days_anniversary_age`, `notif_title_days_anniversary`, `notif_title_days_nameday`, and their plural counter-parts).
    - **NotificationHelper Integration:** Refactored [NotificationHelper.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/notifications/components/NotificationHelper.kt) to load the new plural resources using `context.resources.getQuantityString(...)` with correct quantity bindings, completely resolving the 11 `PluralsCandidate` compiler warnings.
    - **Gradle/Kotlin Lint Warnings Clean Up:** Suppressed the `NewerVersionAvailable` and `GradleDependency` lint warnings in [build.gradle.kts](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/build.gradle.kts) to allow keeping Kotlin at the stable `2.3.21` version, as raising it to `2.4.0` would cause compatibility issues with the latest Dagger Hilt compiler version.
    - **Encapsulation & Code Auditing:** Made internal-only repository methods private (`getOrCreateCalendar` and `cleanCalendars` in [CalendarSyncRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/CalendarSyncRepository.kt), and `updateGiftIdeas` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt)).
    - **DRY Refactoring:** Extracted duplicated widget updating and calendar syncing logic in `linkAsCouple` and `unlinkCouple` in [ContactRepository.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/data/repository/ContactRepository.kt) into a single private helper function `updateWidgetAndSyncCalendar()`.
    - **State Management:** Added `consumeNewlyAddedIdeaId()` to [HomeViewModel.kt](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/viewmodel/HomeViewModel.kt) to handle resetting newly added gift ideas.
