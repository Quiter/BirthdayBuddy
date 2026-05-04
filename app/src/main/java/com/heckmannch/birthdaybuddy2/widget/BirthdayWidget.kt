package com.heckmannch.birthdaybuddy2.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.background
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import com.heckmannch.birthdaybuddy2.viewmodel.daysUntilNext
import com.heckmannch.birthdaybuddy2.viewmodel.nextAge
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class BirthdayWidget : GlanceAppWidget() {
    // SizeMode.Exact ist nötig, um LocalSize.current für die Berechnung zu erhalten
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val contactDao = AppDatabase.getDatabase(context).contactDao()
        
        provideContent {
            val contacts = androidx.compose.runtime.produceState<List<Contact>>(initialValue = emptyList()) {
                value = contactDao.getAllContacts().first()
                    .sortedBy { it.birthday.daysUntilNext() }
            }.value
            
            GlanceTheme {
                WidgetContent(contacts)
            }
        }
    }

    @Composable
    private fun WidgetContent(contacts: List<Contact>) {
        val size = LocalSize.current
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMM")

        // Wir gehen von einer minimalen Item-Höhe von ca. 56dp aus
        val minItemHeight = 56.dp
        val maxItems = (size.height.value / minItemHeight.value).toInt().coerceAtLeast(1)
        val displayContacts = contacts.take(maxItems)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
        ) {
            if (displayContacts.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Keine Geburtstage",
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                }
            } else {
                // Wir nutzen Boxen mit defaultWeight(1f), damit sie den Platz gleichmäßig füllen
                displayContacts.forEach { contact ->
                    val daysLeft = contact.birthday.daysUntilNext()
                    val nextAgeValue = contact.birthday.nextAge()
                    
                    val dateText = if (contact.birthday.year == 1900) {
                        contact.birthday.format(dayMonthFormatter)
                    } else {
                        contact.birthday.format(dateFormatter)
                    }

                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = contact.fullName,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = dateText,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (contact.birthday.year != 1900) {
                                    Text(
                                        text = "wird $nextAgeValue",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                Text(
                                    text = if (daysLeft == 0L) "Heute!" else "In $daysLeft T.",
                                    style = TextStyle(
                                        color = if (daysLeft == 0L) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
