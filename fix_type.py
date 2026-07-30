import re
with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    "val backStack = rememberNavBackStack<SettingsNavKey>(SettingsNavKey.SettingsMenu)",
    "val backStack = rememberNavBackStack(SettingsNavKey.SettingsMenu)"
)

# Wait, in the entryProvider { entry<...>() }, it expects a generic of NavKey.
# If NavDisplay expects NavBackStack<NavKey>, but listDetailStrategy is ListDetailSceneStrategy<SettingsNavKey>, there might be a mismatch.
# Let's change ListDetailSceneStrategy<SettingsNavKey> to ListDetailSceneStrategy<NavKey>
content = content.replace(
    "rememberListDetailSceneStrategy<SettingsNavKey>",
    "rememberListDetailSceneStrategy<NavKey>"
)

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/settings/SettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
