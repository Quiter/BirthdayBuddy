package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.layout.PinnedHeaderPage
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsHeaderControlActive
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsHeaderControlIdle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSubtitleMuted
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAnimDurationMs
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAnimDurationShortMs
import com.heckmannch.birthdaybuddy.ui.theme.SettingsContentMaxWidth
import com.heckmannch.birthdaybuddy.ui.theme.SettingsEdgeFadeWidth
import com.heckmannch.birthdaybuddy.ui.theme.SettingsHeaderControlSize
import com.heckmannch.birthdaybuddy.ui.theme.SettingsMorphFadeFraction
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Resolves the canonical canvas background for Settings screens across Light, Dark, and AMOLED themes.
 */
@Composable
fun settingsPageBackground(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (colorScheme.isDarkSurface()) {
        colorScheme.background
    } else {
        colorScheme.surfaceContainerLow
    }
}

/**
 * Applies a subtle vertical alpha fade near the top edge of a scrolling container when
 * content scrolls beneath a pinned translucent header.
 */
fun Modifier.verticalScrollFade(
    topFadeHeight: Dp = SettingsEdgeFadeWidth,
    fadeTop: () -> Boolean = { true },
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        if (!fadeTop()) return@drawWithContent
        val fadePx = topFadeHeight.toPx().coerceAtMost(size.height * SettingsMorphFadeFraction)
        if (fadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = fadePx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }

/**
 * Expressive tonal pill/circle action button used in [SettingsPageHeader].
 */
@Composable
fun SettingsHeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val baseCorners = CornerRadii.uniform(CornerRadiusExtraLarge)
    val shape = rememberPressShape(base = baseCorners, interactionSource = interactionSource)

    val containerTarget = if (active) {
        colorScheme.primaryContainer.copy(alpha = AlphaSettingsHeaderControlActive)
    } else {
        colorScheme.surfaceContainerHighest.copy(alpha = AlphaSettingsHeaderControlIdle + AlphaSettingsHeaderControlIdle)
    }
    val tintTarget = if (active) colorScheme.primary else colorScheme.onSurface

    val containerColor by animateColorAsState(
        targetValue = containerTarget,
        animationSpec = tween(SettingsAnimDurationShortMs),
        label = "headerControlBg",
    )
    val tintColor by animateColorAsState(
        targetValue = tintTarget,
        animationSpec = tween(SettingsAnimDurationShortMs),
        label = "headerControlTint",
    )

    Box(
        modifier = modifier
            .size(SettingsHeaderControlSize)
            .background(containerColor, shape)
            .shapedClickable(
                shape = shape,
                interactionSource = interactionSource,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(IconSizeMedium),
        )
    }
}

/**
 * Morphe-style settings header bar featuring a morphing title-to-search-pill transition,
 * tonal navigation/action controls, and preserved accessibility/UI test tags.
 */
@Composable
fun SettingsPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {},
    showSearchButton: Boolean = false,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    searchPlaceholder: String = stringResource(R.string.settings_search),
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActiveChange: (Boolean) -> Unit = {},
    onClearSearch: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        } else {
            keyboardController?.hide()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = SettingsContentMaxWidth),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton && !isSearchActive) {
            SettingsHeaderActionButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
                onClick = onNavigateBack,
                modifier = Modifier.testTag("settings_back_button"),
            )
        }

        AnimatedContent(
            targetState = isSearchActive,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                fadeIn(tween(SettingsAnimDurationMs)) togetherWith fadeOut(tween(SettingsAnimDurationShortMs))
            },
            label = "settingsHeaderSearchMorph",
        ) { searching ->
            if (searching) {
                SettingsHeaderSearchPill(
                    query = searchQuery,
                    placeholder = searchPlaceholder,
                    focusRequester = focusRequester,
                    onQueryChange = onSearchQueryChange,
                    onClear = onClearSearch,
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showSearchButton) {
            if (isSearchActive) {
                SettingsHeaderActionButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close_search),
                    onClick = {
                        onClearSearch()
                        onSearchActiveChange(false)
                    },
                    active = true,
                    modifier = Modifier.testTag("close_search_button"),
                )
            } else {
                SettingsHeaderActionButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_open_search),
                    onClick = { onSearchActiveChange(true) },
                    modifier = Modifier.testTag("open_search_button"),
                )
            }
        }

        if (!isSearchActive && trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
private fun SettingsHeaderSearchPill(
    query: String,
    placeholder: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val keyboardController = LocalSoftwareKeyboardController.current
    val pillShape = RoundedCornerShape(CornerRadiusExtraLarge)
    val textFieldState = rememberTextFieldState(initialText = query)

    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { current ->
                if (current != query) {
                    onQueryChange(current)
                }
            }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsHeaderControlSize)
            .background(colorScheme.surfaceContainerHighest, pillShape)
            .padding(start = SpacingMedium, end = SpacingExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
            modifier = Modifier.size(IconSizeSmall),
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (textFieldState.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                state = textFieldState,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { keyboardController?.hide() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("search_field"),
            )
        }

        if (textFieldState.text.isNotEmpty()) {
            IconButton(
                onClick = {
                    textFieldState.clearText()
                    onQueryChange("")
                    onClear()
                },
                modifier = Modifier
                    .size(SettingsHeaderControlSize - SpacingSmall)
                    .background(Color.Transparent, CircleShape)
                    .testTag("clear_search_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSizeSmall),
                )
            }
        }
    }
}

/**
 * Standardized expressive scaffold for settings subpages built on [PinnedHeaderPage] with
 * centered [SettingsContentMaxWidth] content, vertical scroll, edge-to-edge navigation insets,
 * and optional snackbar host.
 */
@Composable
fun SettingsSubpageScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    bottomExtraPadding: Dp = 0.dp,
    headerTrailingContent: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(settingsPageBackground()),
    ) {
        PinnedHeaderPage(
            scrollState = scrollState,
            header = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    SettingsPageHeader(
                        title = title,
                        showBackButton = showBackButton,
                        onNavigateBack = onNavigateBack,
                        showSearchButton = false,
                        trailingContent = headerTrailingContent,
                    )
                }
            },
        ) { insets ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollFade(fadeTop = { scrollState.value > 0 })
                    .verticalScroll(scrollState)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = SettingsContentMaxWidth)
                        .fillMaxWidth()
                        .padding(
                            start = insets.horizontal,
                            end = insets.horizontal,
                            top = insets.top + SpacingSmall,
                            bottom = SpacingLarge + bottomExtraPadding,
                        )
                        .navigationBarsPadding(),
                ) {
                    SettingsColumn {
                        content()
                    }
                    Spacer(modifier = Modifier.height(SpacingSmall))
                }
            }
        }

        SettingsSnackbarContainer(
            bottomPadding = bottomExtraPadding,
            snackbarHost = snackbarHost,
        )
    }
}

/**
 * Overlay container helper for pinned-header pages that host a snackbar at the bottom edge.
 */
@Composable
private fun BoxScope.SettingsSnackbarContainer(
    bottomPadding: Dp = 0.dp,
    snackbarHost: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = bottomPadding + SpacingNormal),
    ) {
        snackbarHost()
    }
}
