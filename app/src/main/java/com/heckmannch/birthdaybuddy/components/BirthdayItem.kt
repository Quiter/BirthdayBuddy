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

// Farbrechner für das Alter
@Composable
fun getAgeColor(age: Int): Color {
    val isDark = isSystemInDarkTheme()
    val youngColor = if (isDark) Color(0xFFFFA4A4) else Color(0xFFFF5252)
    val oldColor = if (isDark) Color(0xFFD32F2F) else Color(0xFF8B0000)
    val clampedAge = age.coerceIn(0, 100)
    val fraction = clampedAge / 100f
    return lerp(youngColor, oldColor, fraction)
}

// Farbrechner für die restlichen Tage
@Composable
fun getDaysColor(days: Int): Color {
    if (days == 0) return Color(0xFFFFC107) // Gold für "Heute"
    val isDark = isSystemInDarkTheme()
    val nearColor = if (isDark) Color(0xFF80D8FF) else Color(0xFF00BFFF)
    val farColor = if (isDark) Color(0xFF5C6BC0) else Color(0xFF00008B)
    val clampedDays = days.coerceIn(1, 365)
    val fraction = clampedDays / 365f
    return lerp(nearColor, farColor, fraction)
}

// NEU: Übersetzt YYYY-MM-DD zu DD.MM.YYYY
fun formatGermanDate(dateString: String): String {
    return try {
        if (dateString.startsWith("--")) {
            // Fall: Kein Jahr angegeben (z.B. --05-24)
            val month = dateString.substring(2, 4)
            val day = dateString.substring(5, 7)
            "$day.$month."
        } else {
            // Fall: Mit Jahr angegeben (z.B. 1990-05-24)
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                "$day.$month.$year"
            } else {
                dateString // Falls es ein ganz komisches Format ist, geben wir es einfach so zurück
            }
        }
    } catch (e: Exception) {
        dateString
    }
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
                // NEU: Hier rufen wir unsere Formatierungs-Funktion auf!
                Text(
                    text = "Datum: ${formatGermanDate(contact.birthday)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Rechte Seite: Alter und Tage
            Column(horizontalAlignment = Alignment.End) {
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

                Text(
                    text = buildAnnotatedString {
                        append("in ")
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