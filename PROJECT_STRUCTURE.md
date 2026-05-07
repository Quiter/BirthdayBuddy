# Project Structure: BirthdayBuddy 2

## 📁 Root
- `MainActivity.kt`: Einstiegspunkt der App. Regelt Navigation (NavHost) und Intent-Handling (z.B. vom Widget).

## 📁 Database (`database`)
- `AppDatabase.kt`: Room-Datenbank Definition mit Singleton-Pattern.
- `Contact.kt`: Entity-Klasse für Kontakte inkl. Geschenkideen und Label-Listen.
- `ContactDao.kt`: Data Access Object für Kontakte (CRUD-Operationen & Sync-Logik).
- `LabelConfig.kt`: Entity für Label-Konfigurationen (Sichtbarkeit/Ignorieren).
- `LabelConfigDao.kt`: DAO für Label-Einstellungen.
- `Converters.kt`: TypeConverters für `LocalDate` und Listen-Encoding.

## 📁 Home Screen (`ui.screens.home`)
- `HomeScreen.kt`: Haupt-Container des Home-Screens. Orchestriert TopBar, List und FAB.
- ### 📁 Components (`home.components`)
    - `BirthdayList.kt`: Verwaltet die LazyColumn der Geburtstage und den exklusiven Swipe-Status.
    - `FastScrollbar.kt`: Implementierung der Google-Photos-Style Scrollbar mit Monats-Bubble.
    - `BirthdayItem.kt`: Einzelnes Listenelement mit Sticky-Swipe (Now Playing Style) und Aktions-Buttons.
    - `GiftIdeaDialog.kt`: Checklisten-Dialog (Google Keep Style) für Geschenkideen.
    - `HomeFAB.kt`: Animierter Floating Action Button für Scroll-to-Top und Kontakt-Hinzufügen.
    - `HomeTopBar.kt`: Suchleiste und Filter-Chips Bereich.

## 📁 Settings (`ui.screens.settings`)
- `SettingsScreen.kt`: Haupt-Einstellungsmenü.
- `labels/LabelSettingsScreen.kt`: Verwaltung der Label-Sichtbarkeit und Filter-Regeln.
- `notifications/NotificationSettingsScreen.kt`: Einstellungen für tägliche Erinnerungen und Uhrzeit.
- `notifications/NotificationWorker.kt`: WorkManager-Logik für tägliche Benachrichtigungen.
- `notifications/NotificationHelper.kt`: Utility zum Erstellen und Anzeigen der System-Notifications.
- `notifications/PreferenceManager.kt`: DataStore-Anbindung für Nutzerpräferenzen.

## 📁 Theme & Design (`ui.theme`)
- `Theme.kt`: Material 3 Theme-Definition mit Dynamic Color Support.
- `Color.kt`: Farb-Konstanten (z.B. BirthdayGold, KidColors).
- `Type.kt`: Typografie-Einstellungen.

## 📁 ViewModel (`viewmodel`)
- `BirthdayViewModel.kt`: Zentrales ViewModel (Single Source of Truth). Regelt Sync, Filterung und Business-Logik.

## 📁 Widget (`widget`)
- `BirthdayWidget.kt`: Glance-basierte Widget-UI.
- `BirthdayWidgetReceiver.kt`: Empfänger für Widget-Updates.
- `BirthdayWidgetWorker.kt`: WorkManager für präzise Mitternachts-Updates des Widgets.
