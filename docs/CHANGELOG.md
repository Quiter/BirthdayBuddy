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
    - **Documentation:** Recorded the design constraint regarding legacy database migrations in `docs/PROJECT_STATUS.md` and registered the new file in `docs/PROJECT_STRUCTURE.md`.
