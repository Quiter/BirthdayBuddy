import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
imports_to_add = '''import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
'''
content = content.replace('import androidx.navigation3.runtime.NavEntry\n', imports_to_add + 'import androidx.navigation3.runtime.NavEntry\n')

# We need to change the body of SettingsContent.
# Find the start of SettingsContent and the end of it.
start_idx = content.find('private fun SettingsContent(')
end_idx = content.find('}\n\n@Composable\nprivate fun SettingsFooter()')

if start_idx != -1 and end_idx != -1:
    new_settings_content = '''private fun SettingsContent(
    onNavigateToSync: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOtherEvents: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val windowSizeClass = LocalWindowSizeClass.current
    
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }

    val listDetailStrategy = rememberListDetailSceneStrategy<SettingsNavKey>(directive = directive)
    val backStack = rememberNavBackStack<SettingsNavKey>(SettingsNavKey.SettingsMenu)

    val activeTab = (backStack.lastOrNull() as? SettingsNavKey.SettingsDetail)?.tab

    val menuItems = remember {
        listOf(
            SettingsMenuItemData(
                titleRes = R.string.settings_notifications_title,
                descRes = R.string.settings_notifications_desc,
                icon = Icons.Default.Notifications,
                tab = SettingsTab.NOTIFICATIONS,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_calendar_title,
                descRes = R.string.settings_calendar_desc,
                icon = Icons.Default.DateRange,
                tab = SettingsTab.CALENDAR,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_labels_title,
                descRes = R.string.settings_labels_desc,
                icon = Icons.AutoMirrored.Filled.Label,
                tab = SettingsTab.LABELS,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_backup_title,
                descRes = R.string.settings_backup_desc,
                icon = Icons.Default.Share,
                tab = SettingsTab.BACKUP,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_theme_title,
                descRes = R.string.settings_theme_desc,
                icon = Icons.Default.Palette,
                tab = SettingsTab.THEME,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_sync_title,
                descRes = R.string.settings_sync_desc,
                icon = Icons.Default.Refresh,
                tab = SettingsTab.SYNC,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_other_events_title,
                descRes = R.string.settings_other_events_desc,
                icon = Icons.Default.Star,
                tab = SettingsTab.OTHER_EVENTS,
                onClick = {}
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_about_title,
                descRes = R.string.settings_about_desc,
                icon = Icons.Default.Info,
                tab = SettingsTab.ABOUT,
                onClick = {}
            )
        )
    }

    AppResponsiveScaffold(
        useAdaptiveWidth = false,
        topBar = {}
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            entryProvider = entryProvider {
                entry<SettingsNavKey.SettingsMenu>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        TopAppBar(
                            title = { Text(stringResource(R.string.settings_title)) },
                            navigationIcon = {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag("settings_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.notifications_back),
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(menuItems, key = { it.tab }) { item ->
                                val isSelected = activeTab == item.tab || (activeTab == SettingsTab.PRIVACY_POLICY && item.tab == SettingsTab.ABOUT)
                                SettingsMenuItem(
                                    titleRes = item.titleRes,
                                    descRes = item.descRes,
                                    icon = item.icon,
                                    isSelected = isSelected,
                                    useTabletStyle = !windowSizeClass.isWidthCompact,
                                    onClick = {
                                        backStack.removeIf { it is SettingsNavKey.SettingsDetail }
                                        backStack.add(SettingsNavKey.SettingsDetail(item.tab))
                                    }
                                )
                            }
                        }

                        SettingsFooter()
                    }
                }

                entry<SettingsNavKey.SettingsDetail>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { key ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (key.tab) {
                            SettingsTab.NOTIFICATIONS -> {
                                NotificationSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.CALENDAR -> {
                                CalendarSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.LABELS -> {
                                LabelSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.BACKUP -> {
                                BackupScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.THEME -> {
                                ThemeSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.SYNC -> {
                                SyncSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.OTHER_EVENTS -> {
                                OtherEventsSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.ABOUT -> {
                                AboutScreen(
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                    onNavigateToPrivacyPolicy = {
                                        backStack.add(SettingsNavKey.SettingsDetail(SettingsTab.PRIVACY_POLICY))
                                    }
                                )
                            }
                            SettingsTab.PRIVACY_POLICY -> {
                                PrivacyPolicyScreen(
                                    showBackButton = true,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
'''
    content = content[:start_idx] + new_settings_content + content[end_idx:]
    with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Done")
