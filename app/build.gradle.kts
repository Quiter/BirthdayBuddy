import java.time.Duration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.heckmannch.birthdaybuddy"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.heckmannch.birthdaybuddy"
        minSdk = 28
        targetSdk = 37
        versionCode = 47
        versionName = "2.15.0"

        testInstrumentationRunner = "com.heckmannch.birthdaybuddy.HiltTestRunner"
    }

    sourceSets {
        named("main") {
            res.directories += listOf("src/main/res", "src/main/res-messenger")
        }
        named("androidTest") {
            assets.directories += "schemas"
        }
    }


    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters += listOf("en", "de")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.kotlin_module"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "**/*.kotlin_module"
            excludes += "META-INF/io.coil-kt.coil3:coil-network-core.kotlin_module"
            excludes += "/META-INF/io.coil-kt.coil3:coil-network-core.kotlin_module"
            excludes += "**/io.coil-kt.coil3*"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        disable += "NewerVersionAvailable"
        disable += "GradleDependency"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(3))
    maxHeapSize = "2048m"
    testLogging {
        events("started", "passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    // Baseline Profile
    baselineProfile(project(":baselineprofile"))
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui.core)
    implementation(libs.bundles.compose.adaptive)
    implementation(libs.bundles.navigation3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.room)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.glance)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material)
    implementation(libs.material.color.utilities)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // AppFunctions (Android AI agent integration, API 36+)
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)

    ksp(libs.androidx.room.compiler)

    // Unit-Tests & Mocking: Mockito für Standard-Unit-Tests, MockK für statische Mocks (mockkStatic) & Object-Mocks
    testImplementation(libs.bundles.testing.unit)
    testImplementation(libs.mockito.kotlin)

    // Screenshot Tests mit Roborazzi (JVM-basiert, kein Emulator nötig)
    testImplementation(libs.bundles.testing.roborazzi)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    // Instrumented Tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    // Instrumented Mocking auf ART (Dexmaker / ByteBuddy Android)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("appfunctions:aggregateAppFunctions", "true")
}

kotlin {
    jvmToolchain(17)
}

