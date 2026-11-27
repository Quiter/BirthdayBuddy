package com.heckmannch.birthdaybuddy.ui.components.cards

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisHigh
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardBorderEnd
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardBorderHighlight
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardBorderMid
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardBorderSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardFillEnd
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardFillMid
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardFillStart
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardSubtitle
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.ChipPaddingVertical
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardSkeletonSubtitleHeight
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardSkeletonTitleHeight
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardSubtitleIconSize
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardTagIconSize
import com.heckmannch.birthdaybuddy.ui.theme.GlassHighlightColor
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.defaultCardGradient

/**
 * Scope provided to [GlassCardLayout] header row content.
 */
@Stable
class GlassCardScope(
    val row: RowScope,
    val style: GlassCardStyle,
    val colors: List<Color>
)

/**
 * Line descriptor for a card's subtitle/status area.
 */
@Immutable
data class GlassCardLine(
    val text: String,
    val icon: ImageVector? = null,
    val emphasized: Boolean = false
)

/**
 * Central frosted glass card layout shared across Home, Labels, and Settings screens.
 *
 * Uses a 2-pass frosted glass sweep gradient background, subtle gradient sheen border,
 * and press-scale spring animation. Supports:
 * - Click/navigation actions (e.g., opening contact detail or settings sub-screen)
 * - Optional inline expansion via [expanded] and [expandedContent] (e.g., Labels and expandable Settings)
 * - Custom gradient tinting controlled via `Color.kt` (e.g., birthday tiers, ignored/hidden labels)
 */
@Composable
fun GlassCardLayout(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = defaultCardGradient(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean? = null,
    expanded: Boolean = false,
    fixedHeight: Boolean = false,
    trailing: (@Composable GlassCardScope.() -> Unit)? = null,
    expandedContent: (@Composable ColumnScope.(GlassCardStyle) -> Unit)? = null,
    content: @Composable GlassCardScope.() -> Unit
) {
    val baseStyle = glassCardStyle(subtitleAlpha = AlphaGlassCardSubtitle)
    val cardStyle = baseStyle.onCard(gradientColors)
    val shape = RoundedCornerShape(cardStyle.cardRadius)
    val view = LocalView.current

    val colors = cardStyle.cardColors(gradientColors)
    val baseColor = colors.firstOrNull() ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val midColor = colors.getOrElse(1) { baseColor }
    val endColor = colors.lastOrNull() ?: baseColor

    val interactionSource = remember { MutableInteractionSource() }
    val isInteractive = enabled && (onClick != null || onLongClick != null)

    val containerHeightModifier = if (fixedHeight && expandedContent == null) {
        Modifier.height(cardStyle.cardHeight)
    } else {
        Modifier
            .defaultMinSize(minHeight = cardStyle.cardHeight)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    }

    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                if (onClick != null) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                }
            },
            onLongClick = if (onLongClick != null) {
                {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            } else null
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(containerHeightModifier)
            .alpha(if (enabled) AlphaEmphasisHigh else AlphaEmphasisDisabled)
            .pressScale(
                interactionSource = interactionSource,
                enabled = isInteractive,
                label = "glass_card_press_scale"
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
                            baseColor.copy(alpha = AlphaGlassCardFillStart),
                            midColor.copy(alpha = AlphaGlassCardFillMid),
                            endColor.copy(alpha = AlphaGlassCardFillEnd)
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
                            GlassHighlightColor.copy(alpha = AlphaGlassCardBorderHighlight),
                            midColor.copy(alpha = AlphaGlassCardBorderMid),
                            endColor.copy(alpha = AlphaGlassCardBorderEnd),
                            GlassHighlightColor.copy(alpha = AlphaGlassCardBorderSubtle)
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
            .then(if (expandedContent == null) clickModifier else Modifier)
            .semantics { if (selected != null) this.selected = selected }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides cardStyle.titleColor,
            LocalCardBackground provides colors.blend()
        ) {
            if (fixedHeight && expandedContent == null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = cardStyle.contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(cardStyle.contentSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scope = remember(cardStyle, colors) { GlassCardScope(this, cardStyle, colors) }
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
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = cardStyle.cardHeight)
                            .then(if (expandedContent != null) clickModifier else Modifier)
                            .padding(
                                horizontal = cardStyle.contentPadding,
                                vertical = SpacingMedium
                            ),
                        horizontalArrangement = Arrangement.spacedBy(cardStyle.contentSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val scope = remember(cardStyle, colors) { GlassCardScope(this, cardStyle, colors) }
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

                    AnimatedVisibility(
                        visible = expanded && expandedContent != null,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = cardStyle.contentPadding,
                                    end = cardStyle.contentPadding,
                                    bottom = cardStyle.contentPadding
                                )
                        ) {
                            HorizontalDivider(
                                color = cardStyle.flatEdgeColor,
                                thickness = BorderWidthThin,
                                modifier = Modifier.padding(bottom = SpacingMedium)
                            )
                            expandedContent?.invoke(this, cardStyle)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtitle row rendered inside a [GlassCardLayout].
 */
@Composable
fun GlassCardScope.GlassCardSubtitle(
    line: GlassCardLine,
    maxLines: Int = 1
) {
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
                modifier = Modifier.size(GlassCardSubtitleIconSize)
            )
        }
        Text(
            text = line.text,
            style = style.subtitleStyle,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Chip/Badge tag rendered inside a frosted glass card.
 */
@Composable
fun GlassCardScope.GlassCardTag(
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
            modifier = Modifier.padding(horizontal = SpacingSmall, vertical = ChipPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(GlassCardTagIconSize)
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
 * Standard expand/collapse icon button harmonized with [GlassCardStyle].
 */
@Composable
fun GlassCardExpandButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    style: GlassCardStyle,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(IconSizeLarge),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = style.chipContainerColor,
            contentColor = style.chipContentColor
        )
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSizeMedium)
        )
    }
}

/**
 * Shimmer loading placeholder card matching [GlassCardLayout].
 */
@Composable
fun GlassCardLoading(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = defaultCardGradient()
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
                        .height(GlassCardSkeletonTitleHeight)
                        .clip(RoundedCornerShape(CornerRadiusSmall))
                        .background(skeletonColor.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(GlassCardSkeletonSubtitleHeight)
                        .clip(RoundedCornerShape(CornerRadiusSmall))
                        .background(skeletonColor.copy(alpha = 0.15f))
                )
            }
        }
    }
}
