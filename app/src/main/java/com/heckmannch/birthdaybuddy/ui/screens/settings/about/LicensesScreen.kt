package com.heckmannch.birthdaybuddy.ui.screens.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val description: String
)

@Composable
fun LicensesScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val libraries = listOf(
        OpenSourceLibrary("Jetpack Compose", "Google", "Apache 2.0", "Modern toolkit for building native Android UI."),
        OpenSourceLibrary("Material 3", "Google", "Apache 2.0", "Material Design 3 components and theming for Compose."),
        OpenSourceLibrary("Jetpack Navigation 3", "Google", "Apache 2.0", "Type-safe navigation library for Jetpack Compose."),
        OpenSourceLibrary("Hilt", "Google", "Apache 2.0", "Dependency injection library for Android."),
        OpenSourceLibrary("Room", "Google", "Apache 2.0", "SQLite object mapping library."),
        OpenSourceLibrary("Coroutines & KotlinX Serialization", "JetBrains", "Apache 2.0", "Asynchronous programming and JSON serialization.")
    )

    SettingsDetailScaffold(
        title = stringResource(R.string.opensource_licenses),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues.withSettingsInsets())
        ) {
            SettingsSection(title = stringResource(R.string.opensource_licenses)) {
                SettingsGroup {
                    libraries.forEach { lib ->
                        SettingsItem(
                            onClick = {},
                            title = lib.name,
                            subtitle = "${lib.author} • ${lib.license}\n${lib.description}"
                        )
                    }
                }
            }
        }
    }
}
