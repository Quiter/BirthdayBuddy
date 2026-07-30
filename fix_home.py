import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

imports_to_add = [
    'import androidx.navigation3.runtime.NavBackStack\n',
    'import androidx.navigation3.runtime.entryProvider\n',
    'import androidx.navigation3.runtime.rememberNavBackStack\n'
]

for i, line in enumerate(lines):
    if 'import androidx.navigation3.runtime.NavEntry' in line:
        for imp in reversed(imports_to_add):
            lines.insert(i, imp)
        break

start_line = -1
end_line = -1

for i, line in enumerate(lines):
    if 'if (contacts.isNullOrEmpty() || windowSizeClass.isWidthCompact) {' in line:
        start_line = i
        break

# Find the matching closing bracket for the if-else
for i in range(start_line, len(lines)):
    if lines[i].strip() == '}' and lines[i+1].strip() == '}' and lines[i+2].strip() == '}' and lines[i+3].strip() == '}':
        # Let's count brackets to be precise
        pass

# Counting brackets
count = 0
for i in range(start_line, len(lines)):
    count += lines[i].count('{')
    count -= lines[i].count('}')
    if count == 0:
        end_line = i
        break

print("Start:", start_line, "End:", end_line)

if start_line != -1 and end_line != -1:
    new_block = '''
                val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
                val directive = remember(windowAdaptiveInfo) {
                    calculatePaneScaffoldDirective(windowAdaptiveInfo)
                        .copy(horizontalPartitionSpacerSize = 0.dp)
                }
                val listDetailStrategy =
                    rememberListDetailSceneStrategy<NavKey>(directive = directive)

                val backStack = rememberNavBackStack(HomeNavKey.ContactList)
                val selectedContactId = (backStack.lastOrNull() as? HomeNavKey.ContactDetail)?.contactId

                LaunchedEffect(contacts, selectedContactId) {
                    if (selectedContactId != null && contacts?.none { it.id == selectedContactId } == true) {
                        backStack.removeLastOrNull()
                    }
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategies = listOf(listDetailStrategy),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    entryProvider = entryProvider {
                        entry<HomeNavKey.ContactList>(
                            metadata = ListDetailSceneStrategy.listPane(
                                detailPlaceholder = {
                                    BirthdayQuotePlaceholder(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                BirthdayList(
                                    contacts = contacts,
                                    newlyAddedIdeaId = null, // Idee wird im rechten Paneel hinzugefügt
                                    hasContactPermission = currentUiState.hasContactPermission,
                                    listState = homeState.listState,
                                    availableLabels = currentUiState.availableLabels,
                                    selectedLabel = currentUiState.selectedLabel,
                                    searchQuery = currentUiState.searchQuery,
                                    actions = currentActions,
                                    coupleSuggestion = currentUiState.coupleSuggestion,
                                    selectedContactId = selectedContactId,
                                    onContactSelected = { contact ->
                                        // Remove any existing detail to prevent backstack growth
                                        if (backStack.lastOrNull() is HomeNavKey.ContactDetail) {
                                            backStack.removeLastOrNull()
                                        }
                                        backStack.add(HomeNavKey.ContactDetail(contact.id))
                                    },
                                    onInteraction = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    contentPadding = paddingValues,
                                    showLabelFilter = !showFilterBarInTopBar
                                )

                                val currentShowLabelFilter =
                                    currentUiState.availableLabels.isNotEmpty() && !showFilterBarInTopBar
                                val currentShowCoupleSuggestion =
                                    currentUiState.selectedLabel == ContactLabels.LABEL_ANNIVERSARY && currentUiState.coupleSuggestion != null
                                val currentHeaderCount =
                                    (if (currentShowLabelFilter) 1 else 0) + (if (currentShowCoupleSuggestion) 1 else 0)

                                FastScrollbar(
                                    listState = homeState.listState,
                                    contacts = currentUiState.contacts ?: emptyList(),
                                    getLabel = getScrollLabel,
                                    headerCount = currentHeaderCount,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .padding(top = paddingValues.calculateTopPadding()),
                                    onSetFastScrolling = { homeState.onSetFastScrolling(it) },
                                )
                            }
                        }

                        entry<HomeNavKey.ContactDetail>(
                            metadata = ListDetailSceneStrategy.detailPane()
                        ) { key ->
                            val contact = remember(contacts, key.contactId) {
                                contacts?.find { it.id == key.contactId }
                            }
                            if (contact != null) {
                                BirthdayDetailPane(
                                    contact = contact,
                                    newlyAddedIdeaId = currentUiState.newlyAddedIdeaId,
                                    actions = currentActions,
                                    onClose = {
                                        backStack.removeLastOrNull()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                )
'''
    lines = lines[:start_line] + [new_block] + lines[end_line + 1:]
    with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt', 'w', encoding='utf-8') as f:
        f.writelines(lines)
    print("Done")
else:
    print("Failed to find block")
