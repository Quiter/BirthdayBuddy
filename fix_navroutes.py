import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt', 'r', encoding='utf-8') as f:
    content = f.read()

keys = [
    r'@Serializable\ndata object LabelSettings : NavKey\n\n',
    r'@Serializable\ndata object NotificationSettings : NavKey\n\n',
    r'@Serializable\ndata object OtherEventsSettings : NavKey\n\n',
    r'@Serializable\ndata object CalendarSettings : NavKey\n\n',
    r'@Serializable\ndata object BackupSettings : NavKey\n\n',
    r'@Serializable\ndata object ThemeSettings : NavKey\n\n',
    r'@Serializable\ndata object SyncSettings : NavKey\n\n',
    r'@Serializable\ndata object About : NavKey\n\n',
    r'@Serializable\ndata object PrivacyPolicy : NavKey\n\n'
]
for pattern in keys:
    content = re.sub(pattern, '', content)

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/navigation/NavRoutes.kt', 'w', encoding='utf-8') as f:
    f.write(content)
