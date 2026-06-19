# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- Crash Reporting & Stacktraces ---
# Zeilennummern in Crash-Stacktraces beibehalten (Play Console / Crashlytics).
-keepattributes SourceFile,LineNumberTable
# Originaldateinamen durch generischen Platzhalter ersetzen (Größenersparnis).
-renamesourcefileattribute SourceFile

# --- Room Database ---
# Room Entities und DAOs schützen, damit das Mapping zur Laufzeit funktioniert.
-keep class com.heckmannch.birthdaybuddy.data.local.** { *; }
-keep @androidx.room.Entity class *
-keep class * { @androidx.room.Dao *; }

# --- Hilt & WorkManager ---
# Hilt benötigt Zugriff auf generierte Klassen und Worker-Fabriken.
-keep class androidx.hilt.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- kotlinx.serialization (Jetpack Navigation 2.8+) ---
# Notwendig für die typsichere Navigation. Verhindert das Wegschneiden der Serializer.
-keepattributes *Annotation*, EnclosingMethod, Signature
-keep class * extends kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    <fields>;
}
-keepclassmembers class * {
    public static ** Companion;
}
-keep class **$$serializer { *; }

# Expliziter Schutz für die Navigations-Routen (Objects in MainActivity.kt)
-keep class com.heckmannch.birthdaybuddy.Home { *; }
-keep class com.heckmannch.birthdaybuddy.Onboarding { *; }
-keep class com.heckmannch.birthdaybuddy.Settings { *; }
-keep class com.heckmannch.birthdaybuddy.LabelSettings { *; }
-keep class com.heckmannch.birthdaybuddy.NotificationSettings { *; }
-keep class com.heckmannch.birthdaybuddy.OtherEventsSettings { *; }
-keep class com.heckmannch.birthdaybuddy.CalendarSettings { *; }
-keep class com.heckmannch.birthdaybuddy.BackupSettings { *; }
-keep class com.heckmannch.birthdaybuddy.ThemeSettings { *; }
-keep class com.heckmannch.birthdaybuddy.SyncSettings { *; }
-keep class com.heckmannch.birthdaybuddy.About { *; }
-keep class com.heckmannch.birthdaybuddy.PrivacyPolicy { *; }

# --- Kotlin Coroutines ---
# Verhindert Probleme bei der Initialisierung des Main-Dispatchers im Release-Build.
-keepnames class kotlinx.coroutines.internal.MainDispatcherLoader {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.HandlerContext {}
