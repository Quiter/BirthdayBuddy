# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ==============================================================================
# 1. Crash Reporting & Stacktraces
# ==============================================================================
# Preserve line numbers and source file attributes for crash stacktraces (Play Console / Crashlytics).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve signatures and annotations for reflection, dependency injection, and serialization.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# ==============================================================================
# 2. Room Database (Room 2.8.4)
# ==============================================================================
# Room uses KSP-generated code and ships with its own AAR consumer rules.
# Blanket rules like `-keep @Entity class * { *; }` prevent R8 from inlining or
# removing unused methods (copy, toString, equals, etc.).
# We only preserve fields and constructors of entities if the class is actually used,
# and safeguard TypeConverter methods/constructors.
-keepclassmembers @androidx.room.Entity class * {
    <init>(...);
    <fields>;
}

-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

-keepclassmembers class com.heckmannch.birthdaybuddy.data.local.*Converters {
    public <init>();
}

# ==============================================================================
# 3. Jetpack Navigation 3 & kotlinx.serialization
# ==============================================================================
# kotlinx.serialization ships embedded consumer rules that protect serializers.
# Retain constructors and serializers for used @Serializable classes without
# locking down all methods, allowing R8 to obfuscate and optimize unused members.
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <init>(...);
    *** Companion;
    *** $serializer;
}

-keepclasseswithmembers @kotlinx.serialization.Serializable class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ==============================================================================
# 4. AppFunctions (Android AI agent integration, API 36+)
# ==============================================================================
# Keep the generated AppFunctionService entry point without locking its internal members.
-keepnames class com.heckmannch.birthdaybuddy.domain.appfunctions.BirthdayBuddyGeneratedAppFunctionService

# Protect fields and constructors of @AppFunctionSerializable data models for IPC schema resolution.
-keepclassmembers @androidx.appfunctions.AppFunctionSerializable class * {
    <init>(...);
    <fields>;
}

