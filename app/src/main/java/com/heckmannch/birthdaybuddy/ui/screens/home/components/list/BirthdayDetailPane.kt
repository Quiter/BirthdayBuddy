package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.lerp
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.components.bars.glassFill
import com.heckmannch.birthdaybuddy.ui.components.cards.CardBorder
import com.heckmannch.birthdaybuddy.ui.components.cards.isRtl
import com.heckmannch.birthdaybuddy.ui.components.layout.FloatingBarsLayout
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsHeaderActionButton
import com.heckmannch.birthdaybuddy.ui.components.settings.cardFill
import com.heckmannch.birthdaybuddy.ui.components.settings.settingsPageBackground
import com.heckmannch.birthdaybuddy.ui.components.settings.verticalScrollFade
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.ContactActionRow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaDetailCardBorder
import com.heckmannch.birthdaybuddy.ui.theme.AlphaDetailInfoIcon
import com.heckmannch.birthdaybuddy.ui.theme.AlphaDetailInfoLabel
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayKidBlue
import com.heckmannch.birthdaybuddy.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderActionSlideOffsetPx
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderActionsDelayMs
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderActionsDurationMs
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderIconDamping
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderIconInitialScale
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderIconStiffness
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderSlideOffsetPx
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderSubtitleStartFraction
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderTextDelayMs
import com.heckmannch.birthdaybuddy.ui.theme.DetailHeaderTextDurationMs
import com.heckmannch.birthdaybuddy.ui.theme.DetailStaggerInitialScale
import com.heckmannch.birthdaybuddy.ui.theme.DetailStaggerOffsetPx
import com.heckmannch.birthdaybuddy.ui.theme.DetailStaggerStepMs
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardIconSize
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAnimDurationMs
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNone
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny
import com.heckmannch.birthdaybuddy.ui.theme.birthdayGoldColor
import com.heckmannch.birthdaybuddy.util.hasYear
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate

private val HeaderBandShape = RoundedCornerShape(
    bottomStart = CornerRadiusLarge,
    bottomEnd = CornerRadiusLarge,
)

/**
 * Contact detail pane modeled after Morphe Manager's `InstalledAppInfoDialog`.
 *
 * Features a pinned frosted tinted header band with a bouncy icon pop-in at top-left,
 * sliding title and birthdate beside it, tonal edit/close actions at top-right, and the
 * [ContactActionRow] directly underneath. Below the header, contact details and the
 * gift ideas list enter with staggered motion inside bordered cards that scroll beneath
 * the blurred header band.
 *
 * @param contact The [ContactUiModel] providing name, avatar, date, labels, and gift ideas.
 * @param newlyAddedIdeaId ID of a newly created gift idea to auto-focus, or `null`.
 * @param actions Bundled [HomeActions] callbacks for contact, birthday, and gift interactions.
 * @param onClose Callback invoked when the user dismisses the detail pane.
 * @param modifier Optional [Modifier] applied to the outer container.
 * @param contentPadding Scaffold and system bar insets keeping the pane clear of top/bottom bars.
 */
@Composable
fun BirthdayDetailPane(
    contact: ContactUiModel,
    newlyAddedIdeaId: String?,
    actions: HomeActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SpacingNone),
) {
    val focusManager = LocalFocusManager.current
    val isInspectionMode = LocalInspectionMode.current
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.isWidthCompact

    val onOpenDatePicker = remember(contact.lookupKey, contact.birthday, actions) {
        {
            val initialDate = contact.birthday ?: LocalDate.now()
            actions.onOpenBirthdayPicker(
                contact.lookupKey,
                contact.birthday?.let { if (it.hasYear) it.year else null },
                initialDate.monthValue,
                initialDate.dayOfMonth,
            )
        }
    }

    val targetAccentColor = resolveContactAccentColor(contact)
    val accentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = SettingsAnimDurationMs),
        label = "detail_accent_color",
    )

    val headerEntrance = rememberDetailHeaderEntrance(contact.id)
    var contentEntered by remember(contact.id) { mutableStateOf(isInspectionMode) }
    LaunchedEffect(contact.id) { contentEntered = true }

    val scrollState = rememberScrollState()
    val haze = rememberHazeState()

    val containerShape = if (isCompact) RectangleShape else RoundedCornerShape(CornerRadiusExtraLarge)
    val outerPaddingModifier = if (isCompact) {
        Modifier.padding(contentPadding)
    } else {
        Modifier
            .padding(contentPadding)
            .padding(SpacingNormal)
    }

    val compactBottomInset = if (isCompact) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        SpacingNone
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(outerPaddingModifier)
            .testTag("birthday_detail_pane"),
        shape = containerShape,
        color = settingsPageBackground(),
        border = if (isCompact) null else CardBorder.neutral,
    ) {
        FloatingBarsLayout(
            modifier = Modifier
                .fillMaxSize()
                .clip(containerShape),
            topBar = {
                DetailHeaderBand(
                    contact = contact,
                    accentColor = accentColor,
                    entrance = headerEntrance,
                    haze = haze,
                    applyStatusBarInsets = isCompact,
                    onOpenDatePicker = onOpenDatePicker,
                    actions = actions,
                    onClose = onClose,
                )
            },
        ) { headerHeight, _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(haze)
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollFade(fadeTop = { scrollState.value > 0 })
                        .verticalScroll(scrollState)
                        .then(
                            if (isCompact) {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(
                            start = SpacingNormal,
                            top = headerHeight + SpacingMedium,
                            end = SpacingNormal,
                            bottom = SpacingLarge + compactBottomInset,
                        ),
                    verticalArrangement = Arrangement.spacedBy(SpacingMedium),
                ) {
                    DetailStaggeredItem(entered = contentEntered, index = 1) {
                        DetailInfoCard(
                            contact = contact,
                            accentColor = accentColor,
                            onEditBirthday = onOpenDatePicker,
                        )
                    }

                    DetailStaggeredItem(entered = contentEntered, index = 2) {
                        DetailGiftIdeasCard(
                            contact = contact,
                            accentColor = accentColor,
                            newlyAddedIdeaId = newlyAddedIdeaId,
                            actions = actions,
                        ) { focusManager.clearFocus() }
                    }
                }
            }
        }
    }
}

/**
 * Resolves the accent color for a contact's detail header and card outlines.
 */
@Composable
private fun resolveContactAccentColor(contact: ContactUiModel): Color {
    if (!contact.isToday) return MaterialTheme.colorScheme.primary
    return when (contact.birthdayTier) {
        BirthdayTier.MILESTONE_GOLD -> birthdayGoldColor
        BirthdayTier.MILESTONE_SILVER -> BirthdaySilver
        BirthdayTier.CHILD -> BirthdayKidBlue
        BirthdayTier.REGULAR -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Entrance animation clocks for [DetailHeaderBand]: the avatar springs in, the name and
 * birth date slide in right after, and the trailing controls and [ContactActionRow] rise last.
 */
@Stable
private class DetailHeaderEntrance(
    val icon: State<Float>,
    val text: State<Float>,
    val actions: State<Float>,
)

@Composable
private fun rememberDetailHeaderEntrance(key: Any): DetailHeaderEntrance {
    val isInspectionMode = LocalInspectionMode.current
    var entered by remember(key) { mutableStateOf(isInspectionMode) }
    LaunchedEffect(key) { entered = true }
    val target = if (entered) 1f else 0f

    val icon = animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = DetailHeaderIconDamping,
            stiffness = DetailHeaderIconStiffness,
        ),
        label = "detail_header_icon_entrance",
    )
    val text = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = DetailHeaderTextDurationMs,
            delayMillis = DetailHeaderTextDelayMs,
            easing = EaseOutCubic,
        ),
        label = "detail_header_text_entrance",
    )
    val actions = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = DetailHeaderActionsDurationMs,
            delayMillis = DetailHeaderActionsDelayMs,
            easing = EaseOutBack,
        ),
        label = "detail_header_actions_entrance",
    )
    return remember(icon, text, actions) { DetailHeaderEntrance(icon, text, actions) }
}

private fun Modifier.headerIconEntrance(entrance: DetailHeaderEntrance): Modifier = graphicsLayer {
    val progress = entrance.icon.value
    val scale = lerp(DetailHeaderIconInitialScale, 1f, progress)
    scaleX = scale
    scaleY = scale
    alpha = progress.coerceIn(0f, 1f)
}

private fun Modifier.headerTitleEntrance(
    entrance: DetailHeaderEntrance,
    rtl: Boolean,
): Modifier = graphicsLayer {
    slideInHorizontally(entrance.text.value, rtl)
}

private fun Modifier.headerSubtitleEntrance(
    entrance: DetailHeaderEntrance,
    rtl: Boolean,
): Modifier = graphicsLayer {
    slideInHorizontally(entrance.text.value.startingAt(DetailHeaderSubtitleStartFraction), rtl)
}

private fun Modifier.headerActionsEntrance(entrance: DetailHeaderEntrance): Modifier = graphicsLayer {
    val progress = entrance.actions.value
    translationY = lerp(DetailHeaderActionSlideOffsetPx, 0f, progress)
    alpha = progress.coerceIn(0f, 1f)
}

private fun GraphicsLayerScope.slideInHorizontally(progress: Float, rtl: Boolean) {
    val startOffset = if (rtl) -DetailHeaderSlideOffsetPx else DetailHeaderSlideOffsetPx
    translationX = lerp(startOffset, 0f, progress)
    alpha = progress.coerceIn(0f, 1f)
}

private fun Float.startingAt(start: Float): Float =
    ((this - start) / (1f - start)).coerceIn(0f, 1f)

/**
 * Staggered entrance wrapper modeled after Morphe Manager's `StaggeredItem`.
 */
@Composable
private fun DetailStaggeredItem(
    entered: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = SettingsAnimDurationMs,
            delayMillis = index * DetailStaggerStepMs,
            easing = EaseOutCubic,
        ),
        label = "detail_stagger_$index",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = lerp(DetailStaggerOffsetPx, 0f, progress)
            val scale = lerp(DetailStaggerInitialScale, 1f, progress)
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}

/**
 * Pinned frosted tinted header band modeled after Morphe Manager's `ListDialogHeader`.
 * Displays the avatar at top-left, name and birth date beside it, edit/close controls at
 * top-right, and [ContactActionRow] directly underneath.
 */
@Composable
private fun DetailHeaderBand(
    contact: ContactUiModel,
    accentColor: Color,
    entrance: DetailHeaderEntrance,
    haze: HazeState,
    applyStatusBarInsets: Boolean,
    onOpenDatePicker: () -> Unit,
    actions: HomeActions,
    onClose: () -> Unit,
) {
    val rtl = isRtl()
    val bandGlassColor = accentColor.copy(alpha = AlphaContainerSubtle)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassFill(
                glass = bandGlassColor,
                shape = HeaderBandShape,
                haze = haze,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (applyStatusBarInsets) {
                        Modifier.windowInsetsPadding(
                            WindowInsets.systemBars.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(top = SpacingNormal, bottom = SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingNormal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            ) {
                ContactImage(
                    imageUri = contact.imageUri,
                    fullName = contact.fullName,
                    initials = contact.initials,
                    lookupKey = contact.lookupKey,
                    secondImageUri = contact.secondImageUri,
                    secondInitials = contact.secondInitials,
                    secondFullName = contact.secondFullName,
                    size = GlassCardIconSize,
                    modifier = Modifier.headerIconEntrance(entrance),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SpacingTiny),
                ) {
                    Row(
                        modifier = Modifier.headerTitleEntrance(entrance, rtl),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
                    ) {
                        Text(
                            text = contact.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (contact.isFavorite) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = stringResource(R.string.contact_favorite_desc),
                                modifier = Modifier.size(IconSizeMedium),
                                tint = birthdayGoldColor,
                            )
                        }
                    }

                    Text(
                        text = contact.dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.headerSubtitleEntrance(entrance, rtl),
                    )
                }

                Row(
                    modifier = Modifier.headerActionsEntrance(entrance),
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsHeaderActionButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.home_edit_contact),
                        onClick = { actions.onEditContact(contact.contactId, contact.lookupKey) },
                        modifier = Modifier.testTag("detail_edit_button"),
                    )
                    SettingsHeaderActionButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.detail_close_desc),
                        onClick = onClose,
                        modifier = Modifier.testTag("detail_close_button"),
                    )
                }
            }

            ContactActionRow(
                contactId = contact.contactId,
                lookupKey = contact.lookupKey,
                phoneNumber = contact.phoneNumber,
                hasBirthday = contact.daysUntilNext != null,
                onAddBirthday = onOpenDatePicker,
                actions = actions,
                isCouple = contact.isCouple,
                modifier = Modifier.headerActionsEntrance(entrance),
            )
        }
    }
}

/**
 * Morphe-style info card containing birthday countdown, upcoming age, and assigned labels.
 */
@Composable
private fun DetailInfoCard(
    contact: ContactUiModel,
    accentColor: Color,
    onEditBirthday: () -> Unit,
) {
    val cardShape = RoundedCornerShape(CornerRadiusLarge)
    val borderColor = accentColor.copy(alpha = AlphaDetailCardBorder)

    val countdownText = when {
        contact.daysUntilNext == null -> stringResource(R.string.item_action_edit_birthday)
        contact.isToday -> stringResource(R.string.item_today)
        else -> pluralStringResource(
            R.plurals.item_days_left,
            contact.daysUntilNext.toInt(),
            contact.daysUntilNext.toInt(),
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = cardFill(),
        border = CardBorder.of(borderColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DetailInfoRow(
                icon = Icons.Rounded.Cake,
                label = stringResource(R.string.detail_birthday_label),
                value = countdownText,
                valueColor = if (contact.isToday || (contact.daysUntilNext == null)) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                valueWeight = if (contact.isToday) FontWeight.SemiBold else FontWeight.Normal,
                onClick = onEditBirthday,
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.EditCalendar,
                        contentDescription = stringResource(R.string.detail_edit_birthday_desc),
                        tint = accentColor,
                        modifier = Modifier.size(IconSizeMedium),
                    )
                },
            )

            if (contact.nextAge != null) {
                DetailCardDivider()
                DetailInfoRow(
                    icon = Icons.Rounded.Celebration,
                    label = stringResource(R.string.detail_age_label),
                    value = stringResource(R.string.widget_turns_age, contact.nextAge),
                    valueColor = accentColor,
                    valueWeight = FontWeight.SemiBold,
                    onClick = onEditBirthday,
                )
            }

            if (contact.labels.isNotEmpty()) {
                DetailCardDivider()
                DetailInfoRow(
                    icon = Icons.AutoMirrored.Rounded.Label,
                    label = stringResource(R.string.nav_tab_labels),
                    value = contact.labels.joinToString(", "),
                )
            }
        }
    }
}

/**
 * Single labeled key-value row modeled after Morphe Manager's `InfoRow`.
 */
@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueWeight: FontWeight = FontWeight.Normal,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = SpacingNormal, vertical = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSizeSmall),
            tint = onSurface.copy(alpha = AlphaDetailInfoIcon),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTiny),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = onSurface.copy(alpha = AlphaDetailInfoLabel),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = valueWeight,
                color = valueColor,
            )
        }

        trailing?.invoke()
    }
}

@Composable
private fun DetailCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = SpacingNormal),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow),
    )
}

/**
 * Morphe-style card hosting the contact's [GiftIdeaList].
 */
@Composable
private fun DetailGiftIdeasCard(
    contact: ContactUiModel,
    accentColor: Color,
    newlyAddedIdeaId: String?,
    actions: HomeActions,
    onClearFocus: () -> Unit,
) {
    val cardShape = RoundedCornerShape(CornerRadiusLarge)
    val borderColor = accentColor.copy(alpha = AlphaDetailCardBorder)
    val filledIdeasCount = remember(contact.giftIdeas) {
        contact.giftIdeas.count { it.text.isNotBlank() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = cardFill(),
        border = CardBorder.of(borderColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingNormal, vertical = SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            ) {
                Surface(
                    modifier = Modifier.size(IconSizeLarge),
                    shape = RoundedCornerShape(CornerRadiusSmall),
                    color = accentColor.copy(alpha = AlphaContainerSubtle),
                    contentColor = accentColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeSmall),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.item_action_gifts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                if (filledIdeasCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(CornerRadiusSmall),
                        color = accentColor.copy(alpha = AlphaContainerSubtle),
                        contentColor = accentColor,
                    ) {
                        Text(
                            text = filledIdeasCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = SpacingSmall,
                                vertical = SpacingTiny,
                            ),
                        )
                    }
                }
            }

            DetailCardDivider()

            GiftIdeaList(
                giftIdeas = contact.giftIdeas,
                newlyAddedId = newlyAddedIdeaId,
                onAddNewIdea = { actions.onAddGiftIdea(contact.lookupKey) },
                onCheckedChange = { idea, checked ->
                    actions.onToggleGiftIdea(contact.lookupKey, idea, checked)
                },
                onTextChange = { idea, newText ->
                    actions.onUpdateGiftIdeaText(contact.lookupKey, idea.id, newText)
                },
                onDelete = { idea ->
                    actions.onDeleteGiftIdea(contact.lookupKey, idea.id)
                },
                onDone = { idea ->
                    if (idea.text.isNotBlank()) {
                        actions.onAddGiftIdea(contact.lookupKey)
                    } else {
                        onClearFocus()
                    }
                },
            )
        }
    }
}

private fun previewSampleContact(): ContactUiModel = ContactUiModel(
    id = "1",
    contactId = "1",
    lookupKey = "k1",
    fullName = "Max Mustermann",
    dateText = "15. August 1991",
    monthName = "August",
    imageUri = null,
    phoneNumber = "+49 123 4567890",
    initials = "MM",
    nextAge = 35,
    daysUntilNext = 12,
    isToday = false,
    isFavorite = true,
    hasWhatsApp = true,
    hasSignal = true,
    labels = listOf("Familie", "Freunde"),
    giftIdeas = listOf(
        GiftIdea(id = "1", text = "Leder-Geldbörse", isChecked = false),
        GiftIdea(id = "2", text = "Konzertkarte", isChecked = true),
        GiftIdea(id = "3", text = "Espressobohnen", isChecked = false),
    ),
    birthday = LocalDate.of(1991, 8, 15),
    birthdayTier = BirthdayTier.REGULAR,
)

@Preview(
    name = "Light Theme - Phone",
    showBackground = true,
    device = Devices.PHONE,
)
@Preview(
    name = "Dark Theme - Phone",
    showBackground = true,
    device = Devices.PHONE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun BirthdayDetailPanePhonePreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640),
        ) {
            BirthdayDetailPane(
                contact = previewSampleContact(),
                newlyAddedIdeaId = null,
                actions = HomeActions.previewDefaults(),
                onClose = {},
            )
        }
    }
}

@Preview(
    name = "Light Theme - Tablet",
    showBackground = true,
    device = Devices.TABLET,
)
@Preview(
    name = "Dark Theme - Tablet",
    showBackground = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun BirthdayDetailPanePreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(840, 640),
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                BirthdayDetailPane(
                    contact = previewSampleContact(),
                    newlyAddedIdeaId = null,
                    actions = HomeActions.previewDefaults(),
                    onClose = {},
                )
            }
        }
    }
}
