package com.heckmannch.birthdaybuddy.ui.screens.home.components.cards

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.ContactImage
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayGold
import com.heckmannch.birthdaybuddy.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.KidColors
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.birthdayGoldColor

/**
 * Scope provided to [ContactCardLayout] content.
 */
@Stable
class ContactCardScope(
    val row: RowScope,
    val style: GlassCardStyle,
    val colors: List<Color>
)

/**
 * Line descriptor for the subtitle/status area under the contact name.
 */
@Immutable
data class ContactCardLine(
    val text: String,
    val icon: ImageVector? = null,
    val emphasized: Boolean = false
)

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
            line?.let { ContactCardSubtitle(it) }
        }
    }
}

@Composable
private fun ContactCardScope.ContactCardSubtitle(line: ContactCardLine) {
    val color = if (line.emphasized) style.titleColor else style.subtitleColor
    Row(
        horizontalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        line.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = line.text,
            style = style.subtitleStyle,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Chip/Badge tag rendered inside a frosted glass card.
 */
@Composable
fun ContactCardScope.ContactCardTag(
    text: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = style.chipContainerColor,
    contentColor: Color = style.chipContentColor
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CornerRadiusSmall),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingSmall, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
            text?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Shared layout for Morphe-style frosted glass cards in BirthdayBuddy.
 * Uses a 2-pass frosted glass sweep gradient background and gradient edge border.
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
    val baseStyle = glassCardStyle(subtitleAlpha = 0.85f)
    val cardStyle = baseStyle.onCard(gradientColors)
    val shape = RoundedCornerShape(cardStyle.cardRadius)
    val view = LocalView.current

    val colors = cardStyle.cardColors(gradientColors)
    val baseColor = colors.firstOrNull() ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val midColor = colors.getOrElse(1) { baseColor }
    val endColor = colors.lastOrNull() ?: baseColor

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardStyle.cardHeight)
            .pressScale(
                interactionSource = interactionSource,
                label = "contact_card_press_scale"
            )
            .clip(shape)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val cr = CornerRadius(cardStyle.cardRadius.toPx())
                val rtl = layoutDirection.isRtl

                val fill = if (cardStyle.monochrome) {
                    SolidColor(cardStyle.cardColor)
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.70f),
                            midColor.copy(alpha = 0.58f),
                            endColor.copy(alpha = 0.64f)
                        ),
                        start = Offset(startEdgeX(w, rtl), h),
                        end = Offset(endEdgeX(w, rtl), 0f)
                    )
                }

                val border = when {
                    cardStyle.monochrome || !cardStyle.outlines -> null
                    colors.all { it == baseColor } -> SolidColor(cardStyle.flatEdgeColor)
                    else -> Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            midColor.copy(alpha = 0.30f),
                            endColor.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.20f)
                        ),
                        start = Offset(startEdgeX(w, rtl), 0f),
                        end = Offset(endEdgeX(w, rtl), h)
                    )
                }
                val borderStroke = Stroke(width = CardBorder.Width.toPx())

                onDrawWithContent {
                    drawRoundRect(brush = fill, cornerRadius = cr)
                    drawContent()
                    if (border != null) {
                        drawRoundRect(
                            brush = border,
                            cornerRadius = cr,
                            style = borderStroke
                        )
                    }
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                },
                onLongClick = if (onLongClick != null) {
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongClick()
                    }
                } else null
            )
            .semantics { if (selected != null) this.selected = selected }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardStyle.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(cardStyle.contentSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scope = remember(cardStyle, colors) { ContactCardScope(this, cardStyle, colors) }
            scope.content()

            AnimatedVisibility(
                visible = trailing != null && selected == null
            ) {
                trailing?.invoke(scope)
            }

            SelectionMark(
                selected = selected,
                ink = cardStyle.titleColor.takeIf { !cardStyle.monochrome }
            )
        }
    }
}

/**
 * Main Morphe-styled glassmorphic contact/birthday card component.
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
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val gradientColors = remember(contact.isToday, contact.birthdayTier, surfaceHigh, surfaceLow, primaryContainer) {
        if (contact.isToday) {
            when (contact.birthdayTier) {
                BirthdayTier.MILESTONE_GOLD -> listOf(BirthdayGold, Color(0xFFFFB700))
                BirthdayTier.MILESTONE_SILVER -> listOf(BirthdaySilver, Color(0xFFB0BEC5))
                BirthdayTier.CHILD -> KidColors
                BirthdayTier.REGULAR -> listOf(primaryContainer, surfaceHigh)
            }
        } else {
            listOf(surfaceHigh, surfaceLow)
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
            ) {
                if (contact.isToday) {
                    ContactCardTag(
                        text = "Heute!",
                        icon = Icons.Default.Cake
                    )
                }
            }
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
 * Shimmer loading placeholder card matching Morphe's AppLoadingCard style.
 */
@Composable
fun ContactCardLoading(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerLow
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val pulseAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val shimmerOffsetState = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val cardStyle = glassCardStyle().onCard(gradientColors)
    val shape = RoundedCornerShape(cardStyle.cardRadius)
    val rtl = isRtl()
    val skeletonColor = cardStyle.titleColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardStyle.cardHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    brush = startToEndGradient(
                        colors = cardStyle.cardColors(gradientColors).map { it.copy(alpha = pulseAlphaState.value) },
                        rtl = rtl
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val progress = (shimmerOffsetState.value + 1f) / 3f
                    val shimmerWidth = w * 0.4f
                    val x = progress * (w + shimmerWidth) - shimmerWidth
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                skeletonColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            start = Offset(x, 0f),
                            end = Offset(x + shimmerWidth, h)
                        )
                    )
                }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardStyle.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(cardStyle.contentSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(cardStyle.iconSize)
                    .clip(RoundedCornerShape(cardStyle.iconSize * 0.3f))
                    .background(skeletonColor.copy(alpha = 0.2f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(cardStyle.textSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(CornerRadiusSmall))
                        .background(skeletonColor.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(CornerRadiusSmall))
                        .background(skeletonColor.copy(alpha = 0.15f))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactCardPreview() {
    MaterialTheme {
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
