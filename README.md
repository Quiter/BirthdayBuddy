# 🎂 BirthdayBuddy 2

> **"Because the first version was a mess – and Google just can't get it right."**

BirthdayBuddy 2 is the definitive answer to unreliable birthday reminders. While Google keeps hiding reminders in the calendar, moving them to Assistant, or letting them disappear into the system void, this app brings everything together in one place: **Stable, modern, and reliable.**

---

## 🚀 Why this app?

1.  **V1 was a dead end:** My first attempt was technically messy ("crap"). Version 2 is a complete rewrite with a modern architecture.
2.  **Google Frustration:** I was tired of hoping that Google would keep calendar sync or Assistant notifications stable. BirthdayBuddy 2 is the solution that just works.
3.  **Centralization:** One place for all birthdays, without having to search through three different Google apps.

---

## ✨ Key Features

*   **Single Source of Truth:** The app uses the **Android ContactsProvider** directly. Your contacts stay in the system – the app only enriches them locally without duplicating data.
*   **Material 3 Design:** A state-of-the-art UI built with Jetpack Compose, featuring "Gmail-style" search and an extremely high-performance fast-scrollbar.
*   **Intelligent Widget:** A reactive homescreen widget (Jetpack Glance) that automatically updates at midnight.
*   **Reliable Notifications:** Powered by WorkManager, notifications arrive exactly when you need them – independent of Google servers.
*   **Hybrid Filtering:** A smart filter bar that only appears when you actually need it (based on your contact labels).

---

## 🛠 Tech Stack

This app is a playground for modern Android development:

*   **UI:** 100% Jetpack Compose (BOM 2026.05) with Material 3.
*   **Persistence:** Room v6 (for labels, sync metadata, and settings).
*   **Background:** WorkManager for precise widget updates and daily notification checks.
*   **Preferences:** Jetpack DataStore for reactive settings.
*   **Architecture:** Clean Architecture with ViewModel, Repository Pattern, and stable identifiers (`LOOKUP_KEY`) for cross-device consistency.
*   **Optimization:** R8 Minification & Resource Shrinking for a minimal APK size.

---

## 📦 Installation & Sync

Just install, grant contact access, and hit "Sync". The app automatically detects all birthdays in your system and organizes them clearly.

*No cloud, no tracking – everything stays on your device.*

---

**Developed with ❤️ in Dortmund, Germany.**
