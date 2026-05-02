package com.heckmannch.birthdaybuddy2.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.background
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.cornerRadius
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import com.heckmannch.birthdaybuddy2.viewmodel.daysUntilNext
import com.heckmannch.birthdaybuddy2.viewmodel.nextAge
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class BirthdayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val contactDao = AppDatabase.getDatabase(context).contactDao()
        
        provideContent {
            val contacts = androidx.compose.runtime.produceState<List<Contact>>(initialValue = emptyList()) {
                value = contactDao.getAllContacts().first()
                    .sortedBy { it.birthday.daysUntilNext() }
                    .take(5)
            }.value
            
            GlanceTheme {
                WidgetContent(contacts)
            }
        }
    }

    @Composable
    private fun WidgetContent(contacts: List<Contact>) {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMM")

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
        ) {
            if (contacts.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Keine Geburtstage",
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                }
            } else {
                LazyColumn {
                    items(contacts) { contact ->
                        val daysLeft = contact.birthday.daysUntilNext()
                        val nextAgeValue = contact.birthday.nextAge()
                        val initials = contact.fullName.take(1).uppercase()
                        
                        val dateText = if (contact.birthday.year == 1900) {
                            contact.birthday.format(dayMonthFormatter)
                        } else {
                            contact.birthday.format(dateFormatter)
                        }

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Ersatz (Initialen)
                            Box(
                                modifier = GlanceModifier
                                    .size(40.dp)
                                    .background(GlanceTheme.colors.primaryContainer)
                                    .cornerRadius(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = contact.fullName,
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
                                        fontSize = 10.sp
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
