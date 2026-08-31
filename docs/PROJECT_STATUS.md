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

## 🏗 Technischer Stack (Evergreen via `libs.versions.toml`)
- **Version Catalog:** Alle Abhängigkeiten werden über [`gradle/libs.versions.toml`](file:///c:/Users/chris/AndroidStudioProjects/BirthdayBuddy/gradle/libs.versions.toml) (SSOT) zentral und aktuell gehalten.
- **Build:** AGP 9.x+, Java 17 (Toolchain), modernes Gradle, Strict R-Class Flags (`nonTransitiveRClass`, `nonFinalResIds`).
- **UI:** 100% Jetpack Compose mit aktuellem BOM, Material 3 Adaptive Layouts, Navigation 3 (`NavDisplay`), Glance (Homescreen-Widget).
- **Data & Background:** Room (mit KSP & Auto-Migrationen), WorkManager, Hilt DI.
- **AI & Integrations:** Android AppFunctions (`domain/appfunctions/` für Android 16+).
- **Testing:** JVM Unit-Tests, Roborazzi Screenshot-Tests, instrumentierte Room-Migrationstests.

## ✨ Kern-Features (Status Quo)
- **Home:** Master-Detail Layout, Gmail-Style Suche, adaptive M3 Fade-Out Fast-Scrollbar, Konfetti-Effekt.
- **Events:** Ehepaar-Verknüpfung für Hochzeitstage, Namenstage-Integration, Multi-Calendar Sync.
- **Onboarding:** Adaptiver Flow mit 100% nativen Compose-Illustrationen (Lottie-free).
- **Widget:** Jetpack Glance mit transparentem Hintergrund & transluzenten M3 Karten.
- **Settings:** Custom Accent Color Picker (HSV-basiert), Master-Detail Tablet Layout, Backup/Restore.

## 📜 Aktuelle Struktur-Meilensteine
*Details siehe `CHANGELOG.md`*

- **Automatisierte Agenten-Skills (Current):** Ablösung manueller Skill-Kopien durch Git-Submodule-freie, gradle-gestützte Repository-Klone (`checkAgentSkills`/`updateAgentSkills`) unter Ausschluss von Git (`.gitignore`). Dynamische Skill-Registrierung über `skills.json`.
- **AGP 9.0 Readiness & Gradle Hardening:** Migration auf deklarative `plugins { ... }` Blöcke, Aktivierung strikter AGP 9 Flags (`nonTransitiveRClass`, `nonFinalResIds`) und Festschreibung von Java 17 als Standard.
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
