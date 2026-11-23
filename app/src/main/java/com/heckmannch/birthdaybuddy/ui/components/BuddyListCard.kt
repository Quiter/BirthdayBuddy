import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaBorderSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisHigh
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerHigh
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.LabelColorDotSize
import com.heckmannch.birthdaybuddy.ui.theme.ListCardElevation
import com.heckmannch.birthdaybuddy.ui.theme.ListCardPaddingHorizontal
import com.heckmannch.birthdaybuddy.ui.theme.ListCardPaddingVertical
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.Shapes
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Modern Morphe-inspired list card for BirthdayBuddy.
 * Serves as a flexible, unified base card component for contacts, labels, settings items, and custom list items.
 *
 * All spacing, padding, corner radii, stroke widths, and alphas strictly adhere to tokens in `Dimensions.kt` and [Shapes].
 */
@Composable
fun BuddyListCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    isExpanded: Boolean = false,
    shape: Shape = Shapes.medium,
    border: BorderStroke? = null,
    containerColor: Color? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    badgeContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null,
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = AlphaSurfaceContainerLow)
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaSurfaceContainerHigh)
            containerColor != null -> containerColor
            isExpanded -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BuddyListCardContainerColor"
    )

    val effectiveBorder = border ?: if (isSelected) {
        BorderStroke(SelectedBorderWidth, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(BorderWidthThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaBorderSubtle))
    }

    val cardAlpha = if (isEnabled) AlphaEmphasisHigh else AlphaEmphasisDisabled

    val clickableModifier = if ((onClick != null) || (onLongClick != null)) {
        Modifier.combinedClickable(
            enabled = isEnabled,
            role = Role.Button,
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = ListCardElevation),
        border = effectiveBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = ListCardPaddingHorizontal, vertical = ListCardPaddingVertical)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(SpacingMedium))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        if (titleContent != null) {
                            titleContent()
                        } else if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (badgeContent != null) {
                            badgeContent()
                        }
                    }

                    if (subtitleContent != null || subtitle != null) {
                        Spacer(modifier = Modifier.height(SpacingExtraSmall))
                        if (subtitleContent != null) {
                            subtitleContent()
                        } else if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(SpacingMedium))
                    trailingContent()
                }
            }

            AnimatedVisibility(
                visible = isExpanded && expandedContent != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(SpacingMedium))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaBorderSubtle),
                        thickness = BorderWidthThin
                    )
                    Spacer(modifier = Modifier.height(SpacingMedium))
                    expandedContent?.invoke()
                }
            }
        }
    }
}

/**
 * Alias for [BuddyListCard] to explicitly support MorpheStyle naming convention.
 */
@Composable
fun MorpheStyleListCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    isExpanded: Boolean = false,
    shape: Shape = Shapes.medium,
    border: BorderStroke? = null,
    containerColor: Color? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    badgeContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null
) = BuddyListCard(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    isSelected = isSelected,
    isEnabled = isEnabled,
    isExpanded = isExpanded,
    shape = shape,
    border = border,
    containerColor = containerColor,
    leadingContent = leadingContent,
    title = title,
    titleContent = titleContent,
    subtitle = subtitle,
    subtitleContent = subtitleContent,
    badgeContent = badgeContent,
    trailingContent = trailingContent,
    expandedContent = expandedContent
)

/**
 * Specialized Contact Card variant built on [BuddyListCard].
 */
@Composable
fun BuddyContactListCard(
    name: String,
    birthdayText: String,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isSelected: Boolean = false,
    isExpanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    onExpandToggle: (() -> Unit)? = null,
    avatarContent: (@Composable () -> Unit)? = null,
    trailingActions: (@Composable () -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null
) {
    BuddyListCard(
        modifier = modifier,
        onClick = onClick,
        isSelected = isSelected,
        isExpanded = isExpanded,
        leadingContent = avatarContent ?: {
            Surface(
                modifier = Modifier.size(ContactImageSizeSmall),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(IconSizeNormal)
                    )
                }
            }
        },
        title = name,
        subtitle = birthdayText,
        badgeContent = if (isFavorite) {
            {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.contact_favorite_desc),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(IconSizeExtraSmall)
                )
            }
        } else null,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingExtraSmall)
            ) {
                trailingActions?.invoke()

                if (onExpandToggle != null) {
                    IconButton(
                        onClick = onExpandToggle,
                        modifier = Modifier.size(IconSizeLarge)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(
                                if (isExpanded) R.string.card_collapse_details else R.string.card_expand_details
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        expandedContent = expandedContent
    )
}

/**
 * Specialized Label Card variant built on [BuddyListCard].
 */
@Composable
fun BuddyLabelListCard(
    labelName: String,
    subtitleText: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    trailingControls: (@Composable () -> Unit)? = null
) {
    BuddyListCard(
        modifier = modifier,
        onClick = onClick,
        isSelected = isSelected,
        isEnabled = isEnabled,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(ContactImageSizeSmall)
                    .clip(Shapes.small)
                    .background(labelColor.copy(alpha = AlphaSurfaceContainerLow)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(LabelColorDotSize)
                        .clip(CircleShape)
                        .background(labelColor)
                )
            }
        },
        title = labelName,
        subtitle = subtitleText,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingExtraSmall)
            ) {
                trailingControls?.invoke()

                if (onEditClick != null) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(IconSizeLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSizeSmall)
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, name = "BuddyListCard - Contact")
@Composable
private fun BuddyContactListCardPreview() {
    var isExpanded by remember { mutableStateOf(false) }
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(SpacingNormal)) {
            BuddyContactListCard(
                name = stringResource(R.string.card_contact_sample_title),
                birthdayText = stringResource(R.string.card_contact_sample_subtitle),
                isFavorite = true,
                isExpanded = isExpanded,
                onClick = { isExpanded = !isExpanded },
                onExpandToggle = { isExpanded = !isExpanded },
                expandedContent = {
                    Text(
                        text = "Geschenkideen: Buch, Kopfhörer, Kaffeemaschine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "BuddyListCard - Label")
@Composable
private fun BuddyLabelListCardPreview() {
    var enabled by remember { mutableStateOf(true) }
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(SpacingNormal)) {
            BuddyLabelListCard(
                labelName = stringResource(R.string.card_label_sample_title),
                subtitleText = stringResource(R.string.card_label_sample_subtitle),
                labelColor = MaterialTheme.colorScheme.tertiary,
                onClick = { },
                trailingControls = {
                    AppSwitch(
                        checked = enabled,
                        onCheckedChange = { isChecked: Boolean -> enabled = isChecked }
                    )
                }
            )
        }
    }
}
