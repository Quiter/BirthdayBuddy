import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_preview = '''        SettingsScreen(
            onNavigateToSync = {},
            onNavigateToLabels = {},
            onNavigateToNotifications = {},
            onNavigateToCalendar = {},
            onNavigateToBackup = {},
            onNavigateToTheme = {},
            onNavigateToAbout = {},
            onNavigateToOtherEvents = {},
            onNavigateBack = {}
        )'''

new_preview = '''        SettingsScreen(
            onNavigateBack = {}
        )'''

content = content.replace(old_preview, new_preview)

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
