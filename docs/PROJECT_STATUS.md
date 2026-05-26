# Project Status: BirthdayBuddy

> **TL;DR:** Moderne M3 App. Android-Kontakte = SSOT. Room = Cache & Settings. Clean Architecture (Hilt, Flow, Compose).

## 🛠 Leitplanken (Regeln & Constraints)
1. **Dokumentation:** Änderungen *zwingend* in `PROJECT_STRUCTURE.md` & `CHANGELOG.md` (nur Anhängen!) loggen.
2. **I18n:** `strings.xml` immer in **DE & EN** gleichzeitig pflegen.
3. **Database Safety:** Schema-Änderung = Version-Bump + Auto-Migration. `fallbackToDestructiveMigration` verboten. Schema-Exports in `app/schemas` einchecken.
4. **Architektur:** Feature-basierte Layer. Spezialisierte ViewModels pro Screen. DI via Hilt.
5. **UI-Prinzipien:** Kontakte nach "Tagen bis Geburtstag" sortiert. Tap-to-Expand Cards (kein Swipe).
6. **Padding-Regel:** Keine additiven vertikalen Paddings. Abstand zwischen Komponenten wird primär durch die obere Komponente (`bottom-padding`) gesteuert, um Layout-Sprünge bei `AnimatedVisibility` zu vermeiden.
7. **Tests:** Vor Releases: `./gradlew test connectedDebugAndroidTest`.
8. **Kommunikation:** GitHub-Summaries: Kurz, technisch, englisch. Play Store Notes: XML-Format (DE/EN).

## 🏗 Technischer Stack
- **UI:** Compose (BOM 2026.05.01), Material 3, Glance (Widget).
- **Data:** Room (2.8.4), WorkManager (2.11), Hilt (2.59).
- **Logic:** `ContactUiModel` (@Immutable), JSON-Mapping für GiftIdeas/Labels.

## ✨ Kern-Features (Status Quo)
- **Home:** Expandable Cards, Gmail-Style Suche mit Fokus-Highlighting, Label-Filter, Fast-Scrollbar, Konfetti-Effekt.
- **Messenger:** Smarte Erkennung (WhatsApp/Signal) via MIME-Type Check.
- **Onboarding:** Adaptiver Permission-Flow mit Lottie-Animationen (Kontakte/Benachrichtigungen).
- **Gift Ideas:** Inline-Edit mit lokalem State (kein Lag), JSON-persistiert, jetzt platzsparend einklappbar.
- **Widget:** Jetpack Glance mit präzisen Mitternachts-Updates, optimiert für 3x2 Layout mit adaptivem Dark-Mode Preview.

## 📜 Aktuelle Struktur-Meilensteine
*Historie siehe `CHANGELOG.md`*

- **UX & UI Consistency Refinement (Current):** Einführung einer Snackbar-Bestätigung im `SyncSettingsScreen` nach erfolgreicher manueller Synchronisierung via `syncCompletedEvent`. Anpassung des `BirthdayStatus` Layouts in der App zur Angleichung an die Informationshierarchie des Widgets (Alter oben/fett, Resttage unten).
- **Onboarding-Seiten-Template & Layout-Stabilität:** Einführung des `OnboardingPageTemplate` zur Vereinheitlichung aller Onboarding-Seiten und Beseitigung von Layout-Sprüngen durch fixe Button-Container und Top-Ausrichtung.
- **Widget Optics Redesign & Launcher Icon Restoration:** Visuelles Redesign des Jetpack Glance Homescreen-Widgets mit dynamisch höhenverteilten Karten, abgerundeten Ecken (`12.dp`) und getönten HSL-Hintergrundfarben passend zum Alter/Status. Behebung von Android Studio-Fehlzuordnungen durch Reaktivierung des API-spezifischen Ordners `mipmap-anydpi-v26/` sowie Revertierung der Logo-Skalierung auf `0.5` für perfekte, kreisrunde Launcher-Darstellung.
- **Onboarding UX & Database Resiliency:** Vereinfachung des Berechtigungs-Onboardings durch performantere Icons, Auslagerung von Messenger-Icons in einen dedizierten `res-messenger` Ressourcen-Zweig und Implementierung eines automatischen, ausfallsicheren try-catch Datenbank-Wiederherstellungsfallbacks (`deleteDatabase`) in `AppDatabase.kt`.
- **Notification Settings Redesign & UX Polish:** Vollständiges Redesign des `EditRuleDialog` mit präziser textbasierter Eingabe und RadioButtons zur Auswahl der Einheit (Tage/Wochen). Beseitigung von UI-Zuckungen in der Hauptliste durch Einführung eines intelligenten `skipPlacementAnimation`-Status beim Filtern/Suchen. Entkopplung von Synchronisations- und Preloading-Triggern aus dem `HomeScreen` direkt in den `init`-Block des `HomeViewModel` zur Vermeidung von Race Conditions.
- **Adaptive UI & Battery Optimization:** Einführung des `LocalWindowWidthSizeClass` CompositionLocal-Frameworks für schlankere und flexiblere responsive Layouts. Härtung der `MainActivity` mit batterieschonender, lifecycle-basierter Inaktivitätsprüfung und robuster Compose-seitiger Intent-State-Verarbeitung. Hilt-Optimierung durch `@Reusable`-Scoping aller Room-DAOs.
- **Refactoring:** Strukturierung der Home-Komponenten in Sub-Packages und Eliminierung von Prop-Drilling durch konsequente Nutzung des `HomeActions` Wrappers.
- **UX & Animation:** Integration von Lottie-Animationen im Onboarding und Home-Screen zur emotionalen Nutzerführung.
- **Messenger Logic:** Dynamische Erkennung von WhatsApp/Signal Verfügbarkeit pro Kontakt.
- **Architecture Swap:** Umstellung auf spezialisierte ViewModels (`Home`, `Notification`, `Settings`, `Label`, `Backup`).
- **Adaptive UI:** Einführung eines responsiven App-Gerüsts (`AppResponsiveScaffold`) zur Unterstützung verschiedener Formfaktoren (Phone, Tablet, Chromebook) und Beseitigung von Layout-Sprüngen beim Start.
- **Persistence:** Alle App-Settings & Benachrichtigungs-Regeln sind in Room konsolidiert.
- **Stability Fix:** Manuelle Migration 1->2 zur Behebung von Schema-Inkonsistenzen bei Bestandsnutzern (inkl. instrumentiertem Migrationstest).
- **Architecture Simplification:** Radikale Verschlankung der UI-Komponenten und ViewModels. Delegierung von Sortier-Logik an Modelle und Zentralisierung des Layout-Managements im adaptiven Scaffold zur Reduzierung von Redundanzen.


## 🎯 Kommende Aufgaben (Backlog)
- **Polish:** Code-Review auf Redundanzen nach großen Refactorings.
- **UX:** Weitere Lottie-Animationen für das Onboarding (Welcome/Ready) integrieren, sobald Assets verfügbar sind.
- **Plattform:** Vorbereitung für WearOS.
