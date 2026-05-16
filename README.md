# 🎂 BirthdayBuddy

> **"Because Google knows your search history from 2008 but somehow 'forgets' your mom's birthday – so I built the solution that actually works."**

BirthdayBuddy is a modern, high-performance birthday management app for Android. Let's be honest: Google is great at many things, but reliable birthday notifications isn't one of them. Whether it's hidden Assistant settings, flaky Calendar syncs, or the mysterious disappearance of notification toggles – we've all been there. 

BirthdayBuddy fixes this by treating your **Android System Contacts** as the single source of truth, adding a layer of rock-solid reliability and a gorgeous Material 3 interface.

---

## 🚀 Why BirthdayBuddy?

1.  **Stop the Ghosting:** No more "Oh, was that yesterday?" moments. We use a dedicated WorkManager-based notification engine that doesn't care about your phone's deep sleep or Google's mood.
2.  **System-First:** No proprietary contact database. Your data stays in the Android Contacts Provider. If you delete this app, your birthdays are still there. (But why would you? Look at those animations!)
3.  **Privacy-Minded:** No cloud, no tracking, no "AI-powered birthday suggestions" sent to some server. It's just you, your phone, and your friends' birth dates.
4.  **Performance:** Designed for 60fps scrolling and instant filtering, even if you happen to know everyone in the city.

---

## ✨ Key Features

*   **Intelligent Sync:** Uses `LOOKUP_KEY` for cross-device consistency. Your contacts are the boss; we are just the very efficient assistant.
*   **GPU-Accelerated UI:** All animations use `graphicsLayer` to keep things buttery smooth. 
*   **Sticky-Swipe Actions:** "Now Playing Style" gestures to manage gift ideas, edit contacts, or hide that one uncle you'd rather ignore.
*   **High-End Fast-Scrollbar:** A Google Photos style scrollbar with a predictive month-bubble for lightning-fast navigation.
*   **Smart Search:** Keyword-based search that finds "John Doe" even if you type "Doe John". Because order shouldn't matter.
*   **Gift Management:** A Google Keep style checklist for each contact. Because a birthday without a gift idea is just a missed opportunity for stress.

---

## 🛠 Tech Stack & Architecture

*   **UI:** 100% Jetpack Compose (BOM 2026.04) with Material 3.
*   **Architecture:** Clean, modularized architecture with **5 specialized ViewModels** (`Home`, `Notification`, `Settings`, `Label`, `Backup`) for maximum maintainability.
*   **Persistence:** Room v6 (serving as a high-performance cache and storage for gift ideas & app settings).
*   **DI:** Hilt for clean dependency management.
*   **Background:** Self-rescheduling WorkManager tasks for midnight widget updates and notification precision.

---

## 📦 Architecture Highlights

*   **Separation of Concerns:** Pure data layer with specialized `SystemContactDataSource`.
*   **Stateless Components:** UI components leverage State Hoisting for testability.
*   **Re-Composition Guard:** Strategic use of `derivedStateOf`, `remember`, and `distinctUntilChanged` to keep the UI thread focused on rendering, not calculating.
*   **Scalability:** SQL chunking and Kotlin Sequences ensure large contact lists are handled with ease.

---

**Developed with ❤️ – Because forgetting birthdays is so 2023.**
