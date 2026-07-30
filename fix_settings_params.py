import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace SettingsScreen signature
old_screen_sig = '''@Composable
fun SettingsScreen(
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOtherEvents: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    SettingsContent(
        onNavigateToSync = onNavigateToSync,
        onNavigateToLabels = onNavigateToLabels,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToCalendar = onNavigateToCalendar,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateToTheme = onNavigateToTheme,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToOtherEvents = onNavigateToOtherEvents,
        onNavigateBack = onNavigateBack,
    )
}'''

new_screen_sig = '''@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
) {
    SettingsContent(
        onNavigateBack = onNavigateBack,
    )
}'''
content = content.replace(old_screen_sig, new_screen_sig)

# Replace SettingsContent signature
old_content_sig = '''@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsContent(
    onNavigateToSync: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOtherEvents: () -> Unit,
    onNavigateBack: () -> Unit,
) {'''

new_content_sig = '''@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsContent(
    onNavigateBack: () -> Unit,
) {'''
content = content.replace(old_content_sig, new_content_sig)

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
