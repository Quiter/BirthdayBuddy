package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideTinySpacer
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium

/**
 * A standardized UI component representing a single step within a multi-step setup or onboarding wizard.
 *
 * NOTE FOR FUTURE LLMs/DEVELOPERS:
 * - This component is intended for sequential checklist tasks (e.g., granting permissions, enabling features, configuring settings guides).
 * - It supports progress indicators (numbers, icons, completed checkmarks, and locked states).
 * - For general, non-sequential clickable rows in settings screens (such as color picker selectors, action buttons, etc.),
 *   do NOT use StepItem. Use [SettingsClickableRow] instead to preserve the semantical structure and visual distinction.
 */
@Composable
fun StepItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    isLocked: Boolean = false,
    stepNumber: Int? = null,
    icon: ImageVector? = null,
    actionButton: (@Composable () -> Unit)? = null
) {
    val contentAlpha = if (isLocked) AlphaEmphasisDisabled else 1f
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = Color(0xFF4CAF50)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Circle indicator
        Box(
            modifier = Modifier
                .size(IconSizeLarge)
                .background(
                    color = when {
                        isCompleted -> successColor.copy(alpha = AlphaContainerSubtle)
                        isLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = AlphaContainerMuted)
                        else -> primaryColor.copy(alpha = AlphaContainerSubtle)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = successColor,
                    modifier = Modifier.size(IconSizeSmall)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLocked) onSurfaceColor.copy(alpha = AlphaEmphasisDisabled) else primaryColor,
                    modifier = Modifier.size(IconSizeExtraSmall)
                )
            } else if (stepNumber != null) {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) onSurfaceColor.copy(alpha = AlphaEmphasisDisabled) else primaryColor
                )
            }
        }

        Spacer(modifier = Modifier.width(SpacingMedium))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = SpacingExtraSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor.copy(alpha = contentAlpha)
            )
            Spacer(modifier = Modifier.height(OnboardingGuideTinySpacer))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
            if (actionButton != null && !isLocked) {
                actionButton()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepItemNumberPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StepItem(
                stepNumber = 1,
                title = "Schritt 1: Berechtigungen",
                description = "Erlaube den Zugriff auf deine Kontakte, um Geburtstage automatisch zu synchronisieren."
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepItemCompletedPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StepItem(
                stepNumber = 2,
                title = "Schritt 2: Aktiviert",
                description = "Der Kalender-Sync ist bereits erfolgreich eingerichtet und aktiv.",
                isCompleted = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepItemLockedPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StepItem(
                stepNumber = 3,
                title = "Schritt 3: Abgeschlossen",
                description = "Dieser Schritt ist gesperrt, bis der vorherige Schritt abgeschlossen wurde.",
                isLocked = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepItemIconPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StepItem(
                icon = Icons.Default.PlayArrow,
                title = "Onboarding starten",
                description = "Tippe unten auf den Button, um den Kalender-Sync-Vorgang zu starten."
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepItemWithButtonPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StepItem(
                stepNumber = 1,
                title = "Aktion erforderlich",
                description = "Bitte gewähre der App Zugriff auf den Kalender.",
                actionButton = {
                    Button(onClick = {}, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Berechtigung erteilen")
                    }
                }
            )
        }
    }
}

