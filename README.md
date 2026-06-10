# 🎂 BirthdayBuddy

> **"Because Google can track your location down to the exact toilet stall you're sitting in, but somehow 'forgets' to notify you when your spouse's wedding anniversary is *today*."**

BirthdayBuddy is a modern, ridiculously over-engineered birthday, wedding anniversary, and name day management app for Android. 

### 😠 The Backstory: Google vs. Your Relationships
Let’s be honest. Google is amazing at organizing the world's information, but when it comes to notifying you of your mom's birthday, it behaves like a teenager who forgot to do their chores. One day the notifications are in Google Calendar, the next they are hidden deep inside Google Assistant settings, and the day after they're completely gone because a product manager needed to hit their promotion milestone.

BirthdayBuddy was born out of pure frustration. It treats your **Android System Contacts** as the absolute Single Source of Truth (SSOT). We don't host your data in some flaky cloud, we don't try to upsell you Google One space, and we certainly don't forget when people were born. If Google's UI breaks, your contact data remains safe in the Android OS, and BirthdayBuddy will make sure you see it.

---

## 🚀 Why BirthdayBuddy?

1. **Anti-Ghosting Notification Engine:** Powered by self-rescheduling WorkManager tasks that run at midnight. We don't care about your phone's deep sleep mode or Google's mood swings—our notifications will fire.
2. **Couch-Avoidance Technology ("Other Events"):** We sync and display wedding anniversaries and name days, because forgetting your spouse's anniversary is a one-way ticket to sleeping on the living room sofa.
3. **No Proprietary Databases:** Your contacts stay in the Android Contacts Provider. If you uninstall this app in a fit of rage, your data is still there. (But why would you? Look at those animations!)
4. **Privacy-Obsessed:** No cloud, no trackers, no AI that analyzes your friendships. Just you, your device, and a list of people you probably should text.

---

## ✨ Features (Now Extremely Polished)

* **Ehepaar-Verknüpfung (Couple Coupling):** If Max and Erika share the same wedding anniversary, BirthdayBuddy merges them into a joint contact card displaying double overlapping avatars. They get a single unified push notification and one combined calendar event. No duplicate notification spam, no double gift stress.
* **Triple Calendar Sync under `"BirthdayBuddy"` Account:** Splitting calendar sync into three distinct system calendars: Birthdays (Pink 🌸), Anniversaries (Purple 🍇), and Name Days (Orange 🍊). It registers under a custom `"BirthdayBuddy"` local account, making it instantly visible, hideable, and color-codeable in external calendar apps (yes, even Google Calendar).
* **In-App Calendar Color Picker:** A Material 3 `ColorPickerDialog` with 8 harmonized colors so you can style your calendar categories directly from the app settings.
* **Premium Wheel Date Picker:** Custom haptically satisfying date scroll wheels (`rememberSnapFlingBehavior` + mechanical ticks). Includes a live-calculated "Include Year" switch that is leap-year safe (no February 29th crashes on non-leap years!).
* **Adaptive Master-Detail Split Screen:** Officially tablet and desktop-proof. On large screens (`WindowWidthSizeClass != Compact`), it loads Google's official `ListDetailPaneScaffold` with smooth posture-aware transitions. It also features a two-pane split Settings layout.
* **GPU-Accelerated FastScrollbar:** Smooth scrolling for when you have too many acquaintances. It uses a static Popup window combined with GPU-accelerated translation layers (`graphicsLayer`) to prevent any UI lag.
* **Gmail-Style Smart Search:** Type "Doe John" or "John Doe"—we don't care about the order, we'll find your contact.
* **Google Keep Style Gift Ideas:** A quick, collapsible checklist for each contact to manage gift ideas locally, complete with auto-focus and auto-sorting of completed items.

---

## 🛠 Tech Stack & Architecture

* **UI:** 100% Jetpack Compose (BOM 2026.05.01) with Material 3 Adaptive layout libraries.
* **Architecture:** Clean, modularized architecture with **6 specialized ViewModels** (`Home`, `Onboarding`, `Notification`, `Settings`, `Label`, `Backup`) keeping presentation logic strictly decoupled from system APIs.
* **Persistence:** Room v6 serving as a high-performance local cache and database for settings, notification rules, and gift ideas.
* **DI:** Hilt for clean dependency injection, with `@Reusable` scoped Room DAOs to minimize DI overhead.
* **Localization (i18n):** Deep translation in both German (DE) and English (EN), including dynamic notification `<plurals>` formatting for correct grammatical day counts ("In 1 day" vs. "In 5 days").

---

## 📦 Project Guidelines

We play by the rules (so you don't have to guess how the code works):
1. **Single Source of Truth:** The Android Contacts Provider is the absolute authority for contact details.
2. **Database Migrations:** Schema changes require a version bump and auto/manual migrations. `fallbackToDestructiveMigration` is forbidden unless it's a controlled fallback for extremely legacy versions.
3. **No Layout Jumps:** Zero additive vertical padding. Space between components is managed by the top component's bottom-margin, preventing layout jitters during animations.
4. **Clean Decoupling:** ViewModels are context-free. System helpers like `NotificationScheduler`, `WidgetUpdater`, and `ImagePrefetcher` are injected via Hilt interfaces to ensure JVM Unit Testability.

---

**Developed with ❤️ – Because Google forgetting birthdays is so 2023.**
