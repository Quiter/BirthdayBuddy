plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

configure<com.android.build.api.dsl.TestExtension> {
    namespace = "com.heckmannch.birthdaybuddy.baselineprofile"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.rules)
    implementation(libs.benchmark.macro.junit4)
}
