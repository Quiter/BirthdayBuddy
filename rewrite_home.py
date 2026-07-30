import re

with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
imports_to_add = '''import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
'''
content = content.replace('import androidx.navigation3.runtime.NavEntry\n', imports_to_add + 'import androidx.navigation3.runtime.NavEntry\n')

# Find the start of the layout block.
# We want to replace from:
#                 if (contacts.isNullOrEmpty() || windowSizeClass.isWidthCompact) {
# down to the end of NavDisplay inside PullToRefreshBox.
# It ends around line 339                 } (closing PullToRefreshBox lambda).
start_pattern = "if (contacts.isNullOrEmpty() || windowSizeClass.isWidthCompact) {"
end_pattern = "                    )\n                }\n            }\n        }"

start_idx = content.find(start_pattern)
end_idx = content.find(end_pattern, start_idx)

if start_idx != -1 and end_idx != -1:
    new_layout = '''
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
                                    newlyAddedIdeaId = null, // Idea is added in right pane
                                    hasContactPermission = currentUiState.hasContactPermission,
                                    listState = homeState.listState,
                                    availableLabels = currentUiState.availableLabels,
                                    selectedLabel = currentUiState.selectedLabel,
                                    searchQuery = currentUiState.searchQuery,
                                    actions = currentActions,
                                    coupleSuggestion = currentUiState.coupleSuggestion,
                                    selectedContactId = selectedContactId,
                                    onContactSelected = { contact ->
                                        backStack.removeIf { it is HomeNavKey.ContactDetail }
                                        backStack.add(HomeNavKey.ContactDetail(contact.id))
                                    },
                                    onInteraction = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    contentPadding = paddingValues, // from original if block
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
                                        .fillMaxHeight(),
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
                    }'''
    # We replace from start_idx up to the "                    )\n                }" part.
    # The end_idx corresponds to the start of "                    )\n                }\n            }\n        }"
    # Wait, let's just make sure we capture correctly.
    content = content[:start_idx] + new_layout.lstrip('\n') + content[end_idx + 21:]

    with open('app/src/main/java/com/heckmannch/birthdaybuddy/ui/screens/home/HomeContent.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Done")
else:
    print("Could not find blocks")
