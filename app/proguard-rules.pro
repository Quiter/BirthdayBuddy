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
# Room TypeConverters – not covered by Room's consumer rules
-keep class com.heckmannch.birthdaybuddy.data.local.**Converter* { *; }
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

# Automatischer Schutz für alle serialisierbaren Klassen und Navigations-Routen
-keep @kotlinx.serialization.Serializable class * { *; }

# --- Kotlin Coroutines ---
# Verhindert Probleme bei der Initialisierung des Main-Dispatchers im Release-Build.
-keepnames class kotlinx.coroutines.internal.MainDispatcherLoader {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.HandlerContext {}
