plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.heckmannch.birthdaybuddy.baselineprofile"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(17)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.rules)
    implementation(libs.benchmark.macro.junit4)
}
