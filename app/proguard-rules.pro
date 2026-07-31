# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- Crash Reporting & Stacktraces ---
# Preserve line numbers and source file attributes for crash stacktraces (Play Console / Crashlytics).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room Database ---
# Room Entities and DAOs are covered by Room's bundled consumer rules.
# TypeConverters are preserved to safeguard custom type conversions.
-keep class com.heckmannch.birthdaybuddy.data.local.converter.** { *; }

# --- Jetpack Navigation 3 & kotlinx.serialization ---
# Preserve annotations and type signatures required for kotlinx.serialization.
-keepattributes *Annotation*, EnclosingMethod, Signature

# Protect all @Serializable classes (Navigation 3 NavKeys and data models) from stripping fields or constructors.
-keep @kotlinx.serialization.Serializable class * { *; }

