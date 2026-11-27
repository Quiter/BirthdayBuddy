package com.heckmannch.birthdaybuddy.ui.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.ContactImage
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayGoldGradient
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayKidGradient
import com.heckmannch.birthdaybuddy.ui.theme.BirthdaySilverGradient
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.birthdayGoldColor
import com.heckmannch.birthdaybuddy.ui.theme.birthdayRegularTodayCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.defaultCardGradient

/**
 * Scope alias for [GlassCardScope] used in contact/birthday cards.
 */
typealias ContactCardScope = GlassCardScope

/**
 * Line descriptor alias for [GlassCardLine] used in contact/birthday cards.
 */
typealias ContactCardLine = GlassCardLine

/**
 * Frosted glass contact item content displaying the avatar, name, status line, and tags.
 */
@Composable
fun ContactCardScope.ContactCardContent(
    contact: ContactUiModel,
    subtitleLine: ContactCardLine?,
    tags: @Composable RowScope.() -> Unit = {}
) {
    Box(modifier = Modifier.size(style.iconSize)) {
        ContactImage(
            imageUri = contact.imageUri,
            fullName = contact.fullName,
            initials = contact.initials,
            lookupKey = contact.lookupKey,
            secondImageUri = contact.secondImageUri,
            secondInitials = contact.secondInitials,
            secondFullName = contact.secondFullName,
            size = style.iconSize
        )
    }

    Column(
        modifier = with(row) { Modifier.weight(1f) },
        verticalArrangement = Arrangement.spacedBy(style.textSpacing)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = contact.fullName,
                style = style.titleStyle,
                color = style.titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (contact.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeExtraSmall),
                    tint = birthdayGoldColor
                )
            }

            tags()
        }

        AnimatedContent(
            targetState = subtitleLine,
            label = "contact_card_subtitle"
        ) { line ->
            line?.let { GlassCardSubtitle(it) }
        }
    }
}

/**
 * Fixed-height frosted glass card layout for contacts on the Home screen.
 * Delegates directly to [GlassCardLayout].
 */
@Composable
fun ContactCardLayout(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    trailing: (@Composable ContactCardScope.() -> Unit)? = null,
    content: @Composable ContactCardScope.() -> Unit
) {
    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        fixedHeight = true,
        trailing = trailing,
        content = content
    )
}

/**
 * Main glassmorphic contact/birthday card component.
 * Gradient colors are resolved centrally via `Color.kt` based on the birthday tier.
 */
@Composable
fun ContactCard(
    contact: ContactUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    startSwipeAction: SwipeAction? = null,
    endSwipeAction: SwipeAction? = null,
    trailing: (@Composable ContactCardScope.() -> Unit)? = null
) {
    val defaultGradient = defaultCardGradient()
    val regularTodayGradient = birthdayRegularTodayCardGradient()

    val gradientColors = remember(contact.isToday, contact.birthdayTier, defaultGradient, regularTodayGradient) {
        if (contact.isToday) {
            when (contact.birthdayTier) {
                BirthdayTier.MILESTONE_GOLD -> BirthdayGoldGradient
                BirthdayTier.MILESTONE_SILVER -> BirthdaySilverGradient
                BirthdayTier.CHILD -> BirthdayKidGradient
                BirthdayTier.REGULAR -> regularTodayGradient
            }
        } else {
            defaultGradient
        }
    }

    val subtitleLine = remember(contact.dateText, contact.daysUntilNext, contact.isToday) {
        when {
            contact.isToday -> ContactCardLine(
                text = contact.dateText,
                icon = Icons.Default.Cake,
                emphasized = true
            )
            else -> ContactCardLine(
                text = contact.dateText,
                icon = Icons.Default.Cake
            )
        }
    }

    SwipeableCardContainer(
        modifier = modifier,
        start = startSwipeAction,
        end = endSwipeAction,
        enabled = startSwipeAction != null || endSwipeAction != null
    ) {
        ContactCardLayout(
            gradientColors = gradientColors,
            onClick = onClick,
            onLongClick = onLongClick,
            selected = selected,
            trailing = trailing
        ) {
            ContactCardContent(
                contact = contact,
                subtitleLine = subtitleLine
            )
        }
    }
}

/**
 * Alias for [ContactCard] designed specifically for birthday lists.
 */
@Composable
fun BirthdayCard(
    contact: ContactUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    startSwipeAction: SwipeAction? = null,
    endSwipeAction: SwipeAction? = null,
    trailing: (@Composable ContactCardScope.() -> Unit)? = null
) {
    ContactCard(
        contact = contact,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        selected = selected,
        startSwipeAction = startSwipeAction,
        endSwipeAction = endSwipeAction,
        trailing = trailing
    )
}

/**
 * Shimmer loading placeholder card for contacts.
 */
@Composable
fun ContactCardLoading(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = defaultCardGradient()
) {
    GlassCardLoading(
        modifier = modifier,
        gradientColors = gradientColors
    )
}

@Preview(showBackground = true)
@Composable
private fun ContactCardPreview() {
    BirthdayBuddyTheme {
        Column(
            modifier = Modifier.padding(SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            ContactCard(
                contact = SampleData.contact1,
                onClick = {}
            )
            ContactCard(
                contact = SampleData.contact2,
                onClick = {},
                selected = true
            )
            ContactCardLoading()
        }
    }
}
