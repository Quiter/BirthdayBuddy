package com.heckmannch.birthdaybuddy.ui.screens.home.components.header

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.bars.AppTopHeader
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Top start page header for BirthdayBuddy with integrated search and quick actions.
 *
 * Inherits overall header styling and search transitions from [AppTopHeader].
 *
 * Specific Home screen elements:
 * 1. App logo icon ([R.drawable.ic_app_logo]) and stylized "BirthdayBuddy" title.
 * 2. Integrated expandable search bar.
 * 3. Action bar buttons:
 *    - Add Contact: Triggers system intent via [HomeActions.onAddContact].
 *    - Optional Navigation sidebar toggle drawer button.
 *
 * @param modifier Optional [Modifier].
 * @param searchQuery Current active search query text.
 * @param isSearchActive Whether search bar mode is active.
 * @param placeholder Search bar hint text.
 * @param showSidebar Whether navigation sidebar toggle button should be displayed.
 * @param focusRequester Focus requester for search input field.
 * @param actions User interaction callbacks bundle [HomeActions].
 * @param onToggleSidebar Callback when user taps navigation sidebar toggle button.
 * @param onToggleSearch Callback when search mode is activated or deactivated.
 */
@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    placeholder: String = stringResource(R.string.home_placeholder_search),
    showSidebar: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    actions: HomeActions = HomeActions.previewDefaults(),
    onToggleSidebar: () -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = actions.onToggleSearch,
) {
    AppTopHeader(
        titleContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeLarge),
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        modifier = modifier.testTag("home_top_bar"),
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        onSearchQueryChange = actions.onSearchQueryChange,
        onSearchActiveChange = onToggleSearch,
        searchPlaceholder = placeholder,
        onClearSearch = actions.onClearSearch,
        focusRequester = focusRequester,
        showSearchButton = true,
        navigationIcon = if (showSidebar) {
            {
                IconButton(onClick = onToggleSidebar) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.cd_toggle_sidebar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        actions = {
            TitleAction(
                icon = Icons.Default.PersonAdd,
                contentDescription = stringResource(R.string.home_add_contact),
                onClick = actions.onAddContact,
                style = TitleActionStyle.Plain,
                modifier = Modifier.testTag("add_contact_button"),
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderPreview() {
    BirthdayBuddyTheme {
        HomeHeader(
            searchQuery = "",
            isSearchActive = false,
            placeholder = "Geburtstage durchsuchen...",
            showSidebar = false,
            focusRequester = remember { FocusRequester() },
            actions = SampleData.homeActions,
            onToggleSidebar = {},
        )
    }
}
