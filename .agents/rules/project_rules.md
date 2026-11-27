# 🤖 BirthdayBuddy: LLM Guidelines & Project Rules

> **Rolle & Kontext**: Du bist ein Senior Android Developer für **BirthdayBuddy** (Android Geburtstags- & Ereignis-Manager).
> - **SSOT**: Systemkontakte = Personen/Geburtstage. Room = Cache/Settings.
> - **Tech-Stack**: Kotlin DSL, Compose M3 Adaptive, Navigation 3, Hilt, Room, WorkManager, AppFunctions (API 36+).

---

## 🚨 UNVERRÜCKBARE KERNREGELN (MUST-FOLLOW)

1. **DOKUMENTATIONSPFLICHT (ABSOLUT PFLICHT FÜR JEDEN TASK)**:
   - 📝 **[`docs/CHANGELOG.md`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/docs/CHANGELOG.md)**: Jede signifikante Code-Änderung chronologisch am Ende anfügen.
   - 📁 **[`docs/PROJECT_STRUCTURE.md`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md)**: Neue/geänderte Dateien, Klassen & Entitäten eintragen.
   - 💬 **KDoc**: Englisches KDoc & erklärende Kommentare für alle neuen/geänderten Klassen & Public-Funktionen.
   - *Ein Task ist OHNE diese Dokumentations-Schritte nicht abgeschlossen!*

2. **DATABASE SAFETY (ROOM)**:
   - `fallbackToDestructiveMigration` ist **strikt verboten** (Zero Data Loss).
   - Jede Schema-Änderung erfordert: DB-Version inkrementieren + `@AutoMigration` definieren + Schema nach `app/schemas` exportieren.

3. **THREADING / DISPATCHER (GOOGLE BEST PRACTICE)**:
   - Niemals `Dispatchers.IO` oder `Dispatchers.Default` direkt im Code instanziieren / aufrufen (`withContext(Dispatchers.IO)` ist verboten).
   - Ausschließlich Hilt-Injektion via `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` nutzen. Alle Suspend-Funktionen in Repositories/DataSources müssen "main-safe" sein.

4. **INTERNATIONALISIERUNG (I18N)**:
   - Strings **immer zeitgleich** in [`res/values-de/strings.xml`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/app/src/main/res/values-de/strings.xml) (DE) und [`res/values/strings.xml`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/app/src/main/res/values/strings.xml) (EN) einpflegen.

5. **UI-TOKENS & SPACING**:
   - Keine additiven vertikalen Paddings (ausschließlich `bottom-padding` verwenden, um Layout-Jumping in `AnimatedVisibility` zu verhindern).
   - Alle Maße, Spacings, Icon-Größen und Opazitäten MÜSSEN aus [`Dimensions.kt`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/app/src/main/java/com/heckmannch/birthdaybuddy/ui/theme/Dimensions.kt) bezogen werden (keine Magic Numbers).

6. **VERSION CATALOG (EVERGREEN)**:
   - Alle Abhängigkeiten, Plugins und SDK-Versionen ausschließlich in [`gradle/libs.versions.toml`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/gradle/libs.versions.toml) pflegen.

---

## 🧩 MODULARE SKILLS (BEDARFSGESTEUERT LADEN)

Für spezifische Aufgabenstellungen existieren modulare, kontextschonende Skills unter `.agents/skills/`:

| Skill | Zweck & Aktivierungstrigger |
|---|---|
| **`projekt-kontext`** | Architektur-Überblick, Clean-Architecture-Layering, Verzeichnisstruktur & Namenskonventionen. |
| **`bb-compose-ui`** | Material 3, `Dimensions.kt`-Tokens, Bottom-Padding, Responsive Layouts (Tablets/Foldables via `ResponsiveLayout.kt`), Edge-to-Edge (`AppResponsiveScaffold`), Coil-Caching. |
| **`bb-navigation`** | Navigation 3 (`NavDisplay`), Serializable Keys in `NavRoutes.kt`, `AppNavHost.kt`, ViewModel-Lifecycle (`rememberViewModelStoreNavEntryDecorator()`), Hilt Assisted Injection. |
| **`bb-room-database`** | Room Entities, DAOs, Migrationen, Schema-Export nach `app/schemas`, `AppDatabase` vs. `SettingsDatabase`. |
| **`bb-coroutines-threading`** | Coroutines, Hilt Dispatcher-Injektion (`@IoDispatcher`), Main-Safety, Threading-Regeln. |
| **`bb-testing-qa`** | Unit Tests (`MainDispatcherRule`), Roborazzi Screenshot-Tests, Quality Gates (`./gradlew test`). |
| **`bb-i18n-docs`** | Zweisprachigkeit (`values-de` / `values`), Dokumentations-Pflichten (`CHANGELOG.md`, `PROJECT_STRUCTURE.md`, KDoc). |
| **`bb-glance-widget`** | Homescreen-Widget mit Jetpack Glance, transluzente M3-Karten, Worker & AlarmReceiver Update-Mechanismus. |

---

## 🗺️ SCHNELLE ORIENTIERUNG (MAP & DOCS)

- 📜 **Änderungshistorie**: [`docs/CHANGELOG.md`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/docs/CHANGELOG.md) (Neueste Einträge am Ende anfügen!)
- 📁 **Komponenten-Lexikon**: [`docs/PROJECT_STRUCTURE.md`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/docs/PROJECT_STRUCTURE.md)
- 📌 **Projektstatus & Roadmap**: [`docs/PROJECT_STATUS.md`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/docs/PROJECT_STATUS.md)
- 📦 **Abhängigkeiten (SSOT)**: [`gradle/libs.versions.toml`](file:///E:/Benutzer/Christof/Entwicklung/StudioProjects/BirthdayBuddy/gradle/libs.versions.toml)

---

## 📝 4-SCHRITT WORKFLOW FÜR ÄNDERUNGEN

1. **Domain & Data**: Models in `domain/model/`, Room in `data/local/`, UseCases in `domain/usecase/` (mit `@IoDispatcher` / `@DefaultDispatcher`).
2. **UI & ViewModels**: ViewModels feature-co-located in `ui/screens/<feature>/` (MVI `onIntent`), UI mit M3 & `Dimensions.kt`.
3. **Strings & Nav**: Strings synchron DE/EN pflegen, Routen in `NavRoutes.kt` & `AppNavHost.kt`.
4. **Testen & Dokumentieren**: Unit-/Screenshot-Tests ausführen, `CHANGELOG.md` & `PROJECT_STRUCTURE.md` aktualisieren & KDoc schreiben.
