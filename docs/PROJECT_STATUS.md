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
- **UI:** Compose (BOM 2026.05.00), Material 3, Glance (Widget).
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

- **Refactoring (Current):** Strukturierung der Home-Komponenten in Sub-Packages und Eliminierung von Prop-Drilling durch konsequente Nutzung des `HomeActions` Wrappers.
- **UX & Animation:** Integration von Lottie-Animationen im Onboarding und Home-Screen zur emotionalen Nutzerführung.
- **Messenger Logic:** Dynamische Erkennung von WhatsApp/Signal Verfügbarkeit pro Kontakt.
- **Architecture Swap:** Umstellung auf spezialisierte ViewModels (`Home`, `Notification`, `Settings`, `Label`, `Backup`).
- **Adaptive UI:** Einführung eines responsiven App-Gerüsts (`AppResponsiveScaffold`) zur Unterstützung verschiedener Formfaktoren (Phone, Tablet, Chromebook) und Beseitigung von Layout-Sprüngen beim Start.
- **Persistence:** Alle App-Settings & Benachrichtigungs-Regeln sind in Room konsolidiert.
- **Stability Fix:** Manuelle Migration 1->2 zur Behebung von Schema-Inkonsistenzen bei Bestandsnutzern (inkl. instrumentiertem Migrationstest).

## 🎯 Kommende Aufgaben (Backlog)
- **Polish:** Code-Review auf Redundanzen nach großen Refactorings.
- **UX:** Weitere Lottie-Animationen für das Onboarding (Welcome/Ready) integrieren, sobald Assets verfügbar sind.
- **Plattform:** Vorbereitung für WearOS.
