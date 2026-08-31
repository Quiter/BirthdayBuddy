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
5. **Over-Engineered Build Logic:** We are already **AGP 9.0 ready** and running on **Java 17**. Why? Because we can. We even stripped out the Lottie library and replaced it with native Compose drawings just to save 400KB of APK space. Efficiency is our middle name (well, actually "Buddy" is, but you get the point).

---

## ✨ Features (Now Extremely Polished)

* **Ehepaar-Verknüpfung (Couple Coupling):** If Max and Erika share the same wedding anniversary, BirthdayBuddy merges them into a joint contact card. They get a single unified push notification and one combined calendar event. No duplicate notification spam, no double gift stress.
* **Triple Calendar Sync:** Splitting calendar sync into three distinct system calendars: Birthdays (Pink 🌸), Anniversaries (Purple 🍇), and Name Days (Orange 🍊). It registers under a custom `"BirthdayBuddy"` local account, so it doesn't get sucked into the Google Sync abyss.
* **Native Custom Illustrations:** We fired the Lottie animations and replaced them with premium, 100% native Compose illustrations. They are smoother, smaller, and don't require an external library that Google might deprecate tomorrow.
* **Google Photos-Style FastScrollbar:** A premium, fade-out scrollbar that stays out of your way until you actually need to find that one person you haven't talked to since 2012.
* **Custom Accent Color Choice:** Don't like our purple? Choose your own! We have a HSV-based color engine that generates a full Material 3 theme from a single HEX code. Take that, Material You!
* **Adaptive Master-Detail Split Screen:** Officially tablet and desktop-proof. On large screens, it loads Google's official `ListDetailPaneScaffold`. It's like having a real computer, but with more birthday cakes.
* **Gmail-Style Smart Search:** Type "Doe John" or "John Doe"—we don't care about the order, we'll find your contact.

---

## 🛠 Tech Stack & Architecture
 
* **UI:** 100% Jetpack Compose (Modern Compose BOM) with Material 3 Adaptive layout and Navigation 3 libraries.
* **Build:** AGP 9.x+ ready, Java 17 toolchain, and strict R-class hardening.
* **Architecture:** Clean, MVI-refactored architecture with feature-co-located ViewModels keeping presentation logic strictly decoupled from system APIs.
* **Persistence:** Room serving as a high-performance local cache with schema-safe migrations.
* **DI:** Hilt with KSP for clean dependency injection.
* **Version Catalog:** Centralized, evergreen dependency management via `gradle/libs.versions.toml`.

---

## 📦 Project Guidelines

We play by the rules (so you don't have to guess how the code works):
1. **Single Source of Truth:** The Android Contacts Provider is the absolute authority.
2. **Zero Layout Jumps:** No additive vertical padding. We hate jittery UIs as much as we hate missed notifications.
3. **Clean Decoupling:** ViewModels are context-free. 
4. **Build Hardening:** We use strict flags (`nonTransitiveRClass`, `nonFinalResIds`) because we take our build speed seriously.

---

**Developed with ❤️ – Because Google forgetting birthdays is so 2023.**
