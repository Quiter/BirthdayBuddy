import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Append NotificationSettings back
content += '\n@Serializable\ndata object NotificationSettings : NavKey\n'

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add NotificationSettings route to AppNavHost.kt
ns_route = '''
                is NotificationSettings -> NavEntry(key) {
                    val notificationViewModel: com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen(
                        viewModel = notificationViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }
'''

# Find the end of the NavDisplay entryProvider map to insert this.
content = content.replace('                else -> throw IllegalArgumentException("Unknown key: ")', ns_route + '                else -> throw IllegalArgumentException("Unknown key: ")')

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/AppNavHost.kt', 'w', encoding='utf-8') as f:
    f.write(content)
