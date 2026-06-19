# Project Status: BirthdayBuddy

> **TL;DR:** Moderne M3 App. Android-Kontakte = SSOT. Room = Cache & Settings. Clean Architecture (Hilt, Flow, Compose).

## 🛠 Leitplanken (Regeln & Constraints)
1. **Dokumentation:** Änderungen *zwingend* in `PROJECT_STRUCTURE.md` & `CHANGELOG.md` (nur Anhängen!) loggen.
2. **I18n:** `strings.xml` immer in **DE & EN** gleichzeitig pflegen.
3. **Database Safety:** Schema-Änderung = Version-Bump + Auto-Migration. `fallbackToDestructiveMigration` verboten. Schema-Exports in `app/schemas` einchecken.
4. **Architektur:** Feature-basierte Layer. Spezialisierte ViewModels pro Screen. DI via Hilt.
5. **UI-Prinzipien:** Kontakte nach "Tagen bis Geburtstag" sortiert. Tap-to-Expand Cards (kein Swipe).
6. **Padding-Regel:** Keine additiven vertikalen Paddings. Abstand via `bottom-padding` zur Vermeidung von Layout-Sprüngen bei `AnimatedVisibility`.
7. **Tests:** Vor Releases: `./gradlew test connectedDebugAndroidTest`.

## 🏗 Technischer Stack
- **Build:** AGP 9.2.1, Java 17, Gradle 9.5, Strict R-Class Flags.
- **UI:** Compose (BOM 2026.05.01), Material 3 Adaptive, Glance (Widget).
- **Data:** Room (2.8.4), WorkManager (2.11), Hilt (2.59).
- **Logic:** UDF / MVI Refactored ViewModels, @Immutable UI-Models.

## ✨ Kern-Features (Status Quo)
- **Home:** Master-Detail Layout, Gmail-Style Suche, adaptive M3 Fade-Out Fast-Scrollbar, Konfetti-Effekt.
- **Events:** Ehepaar-Verknüpfung für Hochzeitstage, Namenstage-Integration, Multi-Calendar Sync.
- **Onboarding:** Adaptiver Flow mit 100% nativen Compose-Illustrationen (Lottie-free).
- **Widget:** Jetpack Glance mit transparentem Hintergrund & transluzenten M3 Karten.
- **Settings:** Custom Accent Color Picker (HSV-basiert), Master-Detail Tablet Layout, Backup/Restore.

## 📜 Aktuelle Struktur-Meilensteine
*Details siehe `CHANGELOG.md`*

- **AGP 9.0 Readiness & Gradle Hardening (Current):** Migration auf deklarative `plugins { ... }` Blöcke, Aktivierung strikter AGP 9 Flags (`nonTransitiveRClass`, `nonFinalResIds`) und Festschreibung von Java 17 als Standard.
- **Native Compose Illustrationen & Lottie Purge:** Vollständige Entfernung der Lottie-Bibliothek (~400 KB APK-Ersparnis). Ersetzung durch performante, rein native Compose-Illustrationen (`ContactsIllustration.kt`) mit animierten Shadern und Kreisen.
- **Google Photos-Style FastScrollbar:** Reaktives Ein-/Ausblenden nach 1.5s Inaktivität. Touch-Pass-Through im Leerlauf. M3-Pill-Thumb mit Richtungsindikatoren.
- **Tablet UX Overhaul:** Einführung eines Zitat-Platzhalters bei leeren Detail-Panes, manueller Deselektions-Button (X) und M3-konforme Kapsel-Navigation in den Einstellungen.
- **Modernisiertes Widget-Design:** Redesign für Jetpack Glance mit transparentem Container, transluzenten Karten-Hintergründen (80% Opazität) und optimiertem Spacing für den Launcher.

### 🏛 Frühere Meilensteine (Konsolidiert)
- **Architektur:** Migration zum MVI-Muster (`HomeIntent`), Entkopplung der Repositories von Android-APIs (Calendar DataSource), Einführung von Baseline Profiles & Startup-Optimierung.
- **UI & UX:** Integration des offiziellen `ListDetailPaneScaffold`, Custom HSV Color Picker für Akzentfarben, vollständiges Redesign des Onboardings (Auto-Advance, Pill-Indicator, dynamische Hintergründe).
- **Features:** Einführung der Ehepaar-Verknüpfung (Paar-Karten, konsolidierte Notifikationen), Namenstage & Hochzeitstage Integration, Wheel-DatePicker Overhaul.
- **Stabilität:** Room-Migrationen (V1->V6) mit automatisierter Testabdeckung, WorkManager-Härtung (Midnight Updates), Batterieschonende Inaktivitätsprüfung in `MainActivity`.

## 🎯 Kommende Aufgaben (Backlog)
- **Polish:** Finales Code-Audit der neuen MVI-Strukturen auf Redundanzen.
- **Performance:** Weitere Optimierung der Baseline Profiles für spezifische User Journeys.
- **Plattform:** Erste Evaluation für eine WearOS-Komplikation (Birthday-Countdown).
