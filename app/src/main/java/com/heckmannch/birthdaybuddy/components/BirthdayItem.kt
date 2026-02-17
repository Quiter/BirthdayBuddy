package com.heckmannch.birthdaybuddy.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.model.BirthdayContact

// Farbrechner für das Alter (Rot-Verlauf)
@Composable
fun getAgeColor(age: Int): Color {
    val isDark = isSystemInDarkTheme()

    val youngColor = if (isDark) Color(0xFFFFA4A4) else Color(0xFFFF5252)
    val oldColor = if (isDark) Color(0xFFD32F2F) else Color(0xFF8B0000)

    val clampedAge = age.coerceIn(0, 100)
    val fraction = clampedAge / 100f

    return lerp(youngColor, oldColor, fraction)
}

// NEU: Farbrechner für die restlichen Tage (Blau-Verlauf & Gold)
@Composable
fun getDaysColor(days: Int): Color {
    // Spezialfall: Heute! (0 Tage)
    if (days == 0) return Color(0xFFFFC107) // Ein schönes, gut lesbares Gold/Amber

    val isDark = isSystemInDarkTheme()

    // Wenig Tage = Hellblau | Viele Tage = Dunkelblau
    val nearColor = if (isDark) Color(0xFF80D8FF) else Color(0xFF00BFFF)
    val farColor = if (isDark) Color(0xFF5C6BC0) else Color(0xFF00008B) // Im Dark Mode etwas heller als reines Dunkelblau

    // Wir kappen bei 365 Tagen (falls Schaltjahre etc. reinspielen)
    val clampedDays = days.coerceIn(1, 365)

    // 1 Tag -> fraction fast 0 -> nearColor (Hellblau)
    // 365 Tage -> fraction fast 1 -> farColor (Dunkelblau)
    val fraction = clampedDays / 365f

    return lerp(nearColor, farColor, fraction)
}

@Composable
fun BirthdayItem(contact: BirthdayContact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Linke Seite: Name und Datum
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Datum: ${contact.birthday}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Rechte Seite: Alter und Tage
            Column(horizontalAlignment = Alignment.End) {
                // Alter mit rotem Farbverlauf
                Text(
                    text = buildAnnotatedString {
                        append("Wird ")
                        withStyle(style = SpanStyle(color = getAgeColor(contact.age))) {
                            append("${contact.age}")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                // NEU: Resttage mit blauem Farbverlauf (und Gold für 0)
                Text(
                    text = buildAnnotatedString {
                        append("in ")
                        // Wichtig: fontWeight = FontWeight.Bold hebt die Zahl noch etwas besser hervor!
                        withStyle(style = SpanStyle(color = getDaysColor(contact.remainingDays), fontWeight = FontWeight.Bold)) {
                            append("${contact.remainingDays}")
                        }
                        append(" Tagen")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}