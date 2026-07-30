import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix SettingsScreen call
old_settings_call = '''                is Settings -> NavEntry(key) {
                    SettingsScreen(
                        onNavigateToLabels = { backStack.add(LabelSettings) },
                        onNavigateToNotifications = { backStack.add(NotificationSettings) },
                        onNavigateToCalendar = { backStack.add(CalendarSettings) },
                        onNavigateToBackup = { backStack.add(BackupSettings) },
                        onNavigateToTheme = { backStack.add(ThemeSettings) },
                        onNavigateToSync = { backStack.add(SyncSettings) },
                        onNavigateToAbout = { backStack.add(About) },
                        onNavigateToOtherEvents = { backStack.add(OtherEventsSettings) },
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }'''

new_settings_call = '''                is Settings -> NavEntry(key) {
                    SettingsScreen {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }'''
content = content.replace(old_settings_call, new_settings_call)

# Remove all the dead navigation branches
dead_branches = [
    r'                is LabelSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is NotificationSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is OtherEventsSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is CalendarSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is BackupSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is ThemeSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is SyncSettings -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is About -> NavEntry\(key\) \{[\s\S]*?\}\n\n',
    r'                is PrivacyPolicy -> NavEntry\(key\) \{[\s\S]*?\}\n\n'
]
for pattern in dead_branches:
    content = re.sub(pattern, '', content)

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt', 'w', encoding='utf-8') as f:
    f.write(content)
