package com.heckmannch.birthdaybuddy.ui.screens.settings.about

import androidx.window.core.layout.WindowSizeClass
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    windowSizeClass: WindowSizeClass,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            packageInfo.versionName ?: "2.1.0"
        } catch (_: Exception) {
            "2.1.0"
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    AppResponsiveScaffold(
        windowSizeClass = windowSizeClass,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.notifications_back)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SpacingNormal,
                    top = paddingValues.calculateTopPadding() + SpacingNormal,
                    end = SpacingNormal,
                    bottom = paddingValues.calculateBottomPadding() + SpacingNormal
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(180.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(SpacingLarge)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingLarge))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SpacingExtraLarge))

            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SpacingNormal)
            )

            Spacer(modifier = Modifier.height(SpacingExtraLarge))

            TextButton(onClick = onNavigateToPrivacyPolicy) {
                Text(
                    text = stringResource(R.string.about_privacy_policy),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(SpacingExtraLarge))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingNormal))
            Text(
                text = stringResource(R.string.settings_made_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingNormal))
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    BirthdayBuddyTheme {
        AboutScreen(
            windowSizeClass = WindowSizeClass(360, 640),
            onNavigateBack = {},
            onNavigateToPrivacyPolicy = {}
        )
    }
}
