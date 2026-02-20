# 🎂 BirthdayBuddy

**A modern, reliable birthday reminder app for Android.**

*Because Google Calendar notifications shouldn't be a gamble.*

BirthdayBuddy was created out of the frustration that system calendar notifications are often unreliable. This app ensures you never miss a birthday again by syncing directly with your Android contacts and providing a highly customizable notification and management system.

---

## ✨ Features

-   **🔄 Seamless Sync:** Automatically imports and synchronizes birthdays from your Android contact list.
-   **🏷️ Central Label Manager:** A powerful, Material 3-based management tool to control visibility for:
    -   **Main App:** Keep your list clean.
    -   **Homescreen Widget:** See only who matters most on your start screen.
    -   **Notifications:** Decide exactly who should trigger an alarm.
-   **🚫 Global Block List:** Completely hide specific labels (e.g., "Work") from the entire app with one click.
-   **📱 Modern Widget:** A sleek, glanceable homescreen widget built with Jetpack Glance.
-   **🔔 Smart Notifications:** Configure multiple lead times (e.g., 1 day before, 1 week before) and set a custom time for daily reminders.
-   **🎨 Material 3 Design:** Fully follows the latest Android design guidelines with dynamic colors, pill-shaped indicators, and smooth animations.
-   **🌍 Fully Localized:** Complete support for **English** and **German**.

---

## 🛠️ Tech Stack

BirthdayBuddy is built with the latest Android development tools and best practices:

-   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with **Material 3**.
-   **Architecture:** MVVM with `AndroidViewModel` and StateFlow.
-   **Database:** [Room](https://developer.android.com/training/data-storage/room) for local persistence.
-   **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for daily sync and notification checks.
-   **Widget:** [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) for modern app widgets.
-   **Storage:** [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) for user settings.
-   **Navigation:** [Type-Safe Navigation](https://developer.android.com/guide/navigation/design/type-safety) (Jetpack Navigation 2.8+).

---

## 🚀 Getting Started

1.  **Clone the repository.**
2.  **Open in Android Studio** (Ladybug or newer recommended).
3.  **Build and Run** on an Android device (API 28+).
4.  **Grant Contact Permissions:** The app will ask for permission to read your contacts to display birthdays.

---

## 📸 Screenshots

*(Add your screenshots here later to make it even more attractive!)*

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

### 🇩🇪 Deutsch
BirthdayBuddy ist eine moderne Android-App, die zuverlässig an Geburtstage erinnert. Da Google Kalender-Benachrichtigungen oft unzuverlässig sind, bietet diese App eine eigene Logik, die sich direkt mit den Systemkontakten synchronisiert. Mit dem neuen **Label Manager** lassen sich Filter für die App, das Widget und Benachrichtigungen zentral steuern.
