package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlin.math.roundToInt

/**
 * Immutable data holder representing an item in the [LiquidGlassBottomNavBar].
 *
 * @property selectedIcon ImageVector for the active selected state.
 * @property unselectedIcon ImageVector for the inactive unselected state.
 * @property label String description of the item displayed underneath or for accessibility.
 */
@Immutable
data class NavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
)

/**
 * A native, high-performance Liquid Glass Bottom Navigation Bar component.
 *
 * Simulates glassmorphism using multi-stage translucent surface color gradients and subtle
 * light reflections (without hardware-heavy AGSL shaders or RenderEffect blurs), while animating
 * a glowing liquid indicator bubble across navigation selections using spring physics.
 *
 * @param items List of [NavItem] entries displayed in the navigation bar.
 * @param selectedIndex The 0-based index of the currently active navigation item.
 * @param onItemSelected Callback invoked when a navigation item is tapped.
 * @param modifier Optional [Modifier] for positioning and layout adjustments.
 * @param containerHeight Fixed height of the glass bar container (default: 64.dp).
 * @param barCornerRadius Corner rounding of the glass shape (default: [CornerRadiusExtraLarge]).
 */
@Composable
fun LiquidGlassBottomNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 64.dp,
    barCornerRadius: Dp = CornerRadiusExtraLarge,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Glass Tints built on top of Material 3 surface colors
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryColor = MaterialTheme.colorScheme.primary

    val glassBackgroundBrush = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                surfaceColor.copy(alpha = 0.75f),
                surfaceColor.copy(alpha = 0.55f)
            )
        )
    }

    val glassBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.25f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingNormal)
            .padding(bottom = navBarPadding.calculateBottomPadding() + SpacingSmall),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Liquid Glass Outer Shell
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .clip(RoundedCornerShape(barCornerRadius))
                .background(glassBackgroundBrush)
                .border(
                    width = 1.dp,
                    brush = glassBorderBrush,
                    shape = RoundedCornerShape(barCornerRadius)
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalItems = items.size
                if (totalItems > 0) {
                    val itemWidth = maxWidth / totalItems

                    // Spring animation for the liquid indicator bubble center position
                    val targetXOffset = (itemWidth * selectedIndex) + (itemWidth / 2) - 26.dp
                    val animatedXOffset by animateFloatAsState(
                        targetValue = targetXOffset.value,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "LiquidSpringOffset"
                    )

                    // Layer 3: Liquid Indicator Radial Glow Bubble
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = animatedXOffset.dp.toPx().roundToInt(), y = 0) }
                            .align(Alignment.CenterStart)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.35f),
                                        primaryColor.copy(alpha = 0.05f)
                                    )
                                )
                            )
                    )
                }

                // Layer 4 & 5: Interactive Navigation Icons & Labels
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "IconScaleAnimation"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onItemSelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(IconSizeNormal)
                                        .scale(iconScale),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium)
                                    }
                                )

                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium)
                                    },
                                    modifier = Modifier.padding(top = SpacingExtraSmall)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiquidGlassBottomNavBarPreview() {
    BirthdayBuddyTheme {
        var selectedIndex by remember { mutableIntStateOf(1) }
        val items = listOf(
            NavItem(
                selectedIcon = Icons.AutoMirrored.Filled.Label,
                unselectedIcon = Icons.AutoMirrored.Outlined.Label,
                label = stringResource(R.string.nav_labels)
            ),
            NavItem(
                selectedIcon = Icons.Filled.DateRange,
                unselectedIcon = Icons.Outlined.DateRange,
                label = stringResource(R.string.nav_list)
            ),
            NavItem(
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                label = stringResource(R.string.settings_title)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.BottomCenter
        ) {
            LiquidGlassBottomNavBar(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        }
    }
}
