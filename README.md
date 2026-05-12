# 🎂 BirthdayBuddy

> **"Because Google keeps losing your birthdays – so I built the solution that just works."**

BirthdayBuddy is a modern, reliable, and high-performance birthday management app for Android. While other apps rely on flaky calendar syncs or hidden assistant settings, BirthdayBuddy treats your **Android System Contacts** as the single source of truth, enriching them with powerful features and a state-of-the-art Material 3 interface.

---

## 🚀 Why BirthdayBuddy?

1.  **System-First:** No proprietary database for contacts. Your data stays where it belongs – in the Android Contacts Provider.
2.  **Modern Architecture:** A complete rewrite (V3) from the ground up using the latest Android best practices (BOM 2026.04).
3.  **Performance & UX:** Designed for 60fps scrolling and instant updates, even with thousands of contacts.
4.  **Privacy:** No cloud, no tracking, no account required. Everything stays on your device.

---

## ✨ Key Features

*   **Intelligent Sync:** Uses `LOOKUP_KEY` and `localId` for cross-device consistency and robust system-sync handling.
*   **GPU-Accelerated UI:** All animations (FAB, Swiping, Transitions) use `graphicsLayer` for GPU-side calculations, ensuring a stutter-free experience.
*   **Sticky-Swipe Actions:** "Now Playing Style" swipe gestures for quick access to gift ideas, contact editing, or ignoring specific people.
*   **Smart Filter Bar:** A hybrid filtering system that intelligently shows or hides labels based on your active usage.
*   **Reactive Widget:** A Jetpack Glance homescreen widget that updates precisely at midnight (00:01 AM) via WorkManager.
*   **Gift Management:** A Google Keep style checklist for each contact with local persistence and JSON-based Backup/Restore.
*   **High-End Fast-Scrollbar:** A Google Photos style scrollbar with a predictive month-bubble and 48dp touch-area for precision navigation.
*   **Smart Search:** Keyword-based search logic that finds contacts regardless of name order.

---

## 🛠 Tech Stack

*   **UI:** 100% Jetpack Compose (BOM 2026.04) with Material 3.
*   **Persistence:** Room v6 (with Batch-Insert optimizations).
*   **Background:** WorkManager for midnight widget refreshes and daily notification checks.
*   **DI:** Hilt for clean dependency management (ViewModels, Repositories, DAOs, DataSources).
*   **Architecture:** Clean Repository Pattern with specialized DataSources and MVI-style UI state management.
*   **Time Management:** Custom reactive `TimeRepository` for automated UI updates at midnight.
*   **Image Loading:** Coil for efficient contact photo rendering.

---

## 📦 Architecture Highlights

*   **Separation of Concerns:** Decoupled data layer using specialized `SystemContactDataSource` and `GiftIdeaBackupManager`.
*   **Stateless Components:** UI components utilize State Hoisting for maximum testability and reusability.
*   **Lifecycle Awareness:** Uses `collectAsStateWithLifecycle` to minimize resource consumption.
*   **Memoization:** Strategic use of `derivedStateOf` and `remember` to prevent redundant UI computations.
*   **Scalability:** Optimized for large datasets using Kotlin Sequences, SQL chunking, and parallel background processing.

---

**Developed with ❤️ – Stable, Modern, and Future-Proof.**
