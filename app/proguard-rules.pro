# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- Crash Reporting & Stacktraces ---
# Preserve line numbers and source file attributes for crash stacktraces (Play Console / Crashlytics).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room Database ---
# Safeguard Room Entities, DAOs, Database classes, and TypeConverters from code shrinking and reflection issues (Project Guidelines §2.2).
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.heckmannch.birthdaybuddy.data.local.*Converters { *; }
-keep class * {
    @androidx.room.TypeConverter <methods>;
}

# --- Jetpack Navigation 3 & kotlinx.serialization ---
# Preserve annotations and type signatures required for kotlinx.serialization.
-keepattributes *Annotation*, EnclosingMethod, Signature

# Protect all @Serializable classes (Navigation 3 NavKeys and data models) from stripping fields or constructors.
-keep @kotlinx.serialization.Serializable class * { *; }

# --- AppFunctions ---
# Preserve @AppFunctionSerializable data classes so KSP-generated XML schema and runtime
# reflection can resolve all fields correctly in release builds.
-keep @androidx.appfunctions.AppFunctionSerializable class * { *; }

# Keep the KSP-generated AppFunctionService subclass referenced by name in AndroidManifest.xml.
-keep class com.heckmannch.birthdaybuddy.domain.appfunctions.BirthdayBuddyGeneratedAppFunctionService { *; }
