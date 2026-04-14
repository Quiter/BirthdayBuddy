package com.heckmannch.birthdaybuddy.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.*
import com.heckmannch.birthdaybuddy.ui.theme.*
import com.heckmannch.birthdaybuddy.utils.getAppVersionName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    onNavigate: (String) -> Unit, 
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val filterManager = viewModel.filterManager
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    val currentTheme by viewModel.themeFlow.collectAsState(initial = 0)
    val currentUser by viewModel.currentUser.collectAsState()
    val versionName = getAppVersionName()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { viewModel.signInWithGoogle(it) }
        } catch (e: Exception) {
            // Fehlerbehandlung
        }
    }
    
    val showThemeDialog = remember { mutableStateOf(false) }
    val dismissThemeDialog = { showThemeDialog.value = false }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.label_selection_back)) 
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .navigationBarsPadding() 
                .padding(bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // SEKTION: KONTO
            SectionHeader(stringResource(R.string.settings_section_account))
            SettingsGroup {
                val accountSubtitle = if (currentUser != null) {
                    stringResource(R.string.settings_account_status_signed_in, currentUser?.email ?: "")
                } else {
                    stringResource(R.string.settings_account_status_signed_out)
                }

                SettingsBlockRow(
                    title = if (currentUser == null) stringResource(R.string.settings_account_signin_google) else stringResource(R.string.settings_account_signout),
                    subtitle = accountSubtitle,
                    icon = if (currentUser == null) Icons.Default.Login else Icons.Default.Logout,
                    iconContainerColor = if (currentUser == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    isTop = true,
                    isBottom = true
                ) {
                    if (currentUser == null) {
                        launcher.launch(viewModel.authRepository.getGoogleSignInClient().signInIntent)
                    } else {
                        viewModel.signOut()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SEKTION: ORGANISATION
            SectionHeader(stringResource(R.string.label_manager_section_org))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.label_manager_title), 
                    subtitle = stringResource(R.string.label_manager_subtitle), 
                    icon = Icons.Default.Style, 
                    iconContainerColor = SettingsColorOrganisation,
                    isTop = true,
                    isBottom = true
                ) { onNavigate("settings_label_manager") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SEKTION: BENACHRICHTIGUNGEN
            SectionHeader(stringResource(R.string.settings_section_notifications))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.settings_alarms_title), 
                    subtitle = stringResource(R.string.settings_alarms_desc), 
                    icon = Icons.Default.Notifications, 
                    iconContainerColor = SettingsColorNotifications,
                    isTop = true,
                    isBottom = true
                ) { onNavigate("settings_alarms") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SEKTION: DESIGN
            SectionHeader(stringResource(R.string.settings_section_display))
            SettingsGroup {
                val themeLabel = when(currentTheme) {
                    1 -> stringResource(R.string.settings_theme_light)
                    2 -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                }
                SettingsBlockRow(
                    title = stringResource(R.string.settings_theme_title), 
                    subtitle = themeLabel, 
                    icon = Icons.Default.Palette, 
                    iconContainerColor = SettingsColorDisplay,
                    isTop = true,
                    isBottom = true
                ) { showThemeDialog.value = true }
            }

            SettingsFooter(versionName) {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Quiter/BirthdayBuddy".toUri()))
            }
        }
    }

    if (showThemeDialog.value) {
        AlertDialog(
            onDismissRequest = dismissThemeDialog,
            title = { Text(stringResource(R.string.settings_theme_dialog_title)) },
            text = {
                Column {
                    ThemeOption(
                        label = stringResource(R.string.settings_theme_system),
                        selected = currentTheme == 0
                    ) {
                        dismissThemeDialog()
                        scope.launch {
                            filterManager.saveTheme(0)
                        }
                    }
                    ThemeOption(
                        label = stringResource(R.string.settings_theme_light),
                        selected = currentTheme == 1
                    ) {
                        dismissThemeDialog()
                        scope.launch {
                            filterManager.saveTheme(1)
                        }
                    }
                    ThemeOption(
                        label = stringResource(R.string.settings_theme_dark),
                        selected = currentTheme == 2
                    ) {
                        dismissThemeDialog()
                        scope.launch {
                            filterManager.saveTheme(2)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = dismissThemeDialog) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
