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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import android.content.Intent
import androidx.glance.LocalContext
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.background
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import com.heckmannch.birthdaybuddy2.MainActivity
import com.heckmannch.birthdaybuddy2.viewmodel.safeDaysUntilNext
import com.heckmannch.birthdaybuddy2.viewmodel.safeNextAge
import kotlinx.coroutines.flow.combine
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class BirthdayWidget : GlanceAppWidget() {
    // SizeMode.Exact ist nötig, um LocalSize.current für die Berechnung zu erhalten
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val contactDao = db.contactDao()
        val labelConfigDao = db.labelConfigDao()
        
        provideContent {
            val contactsState = androidx.compose.runtime.produceState(initialValue = emptyList()) {
                combine(
                    contactDao.getAllContacts(),
                    labelConfigDao.getAllConfigs(),
                ) { list, configs ->
                    val ignoredLabels = configs.asSequence()
                        .filter { it.isIgnored }
                        .map { it.name }
                        .toSet()
                    list.asSequence()
                        .filter { contact ->
                            contact.labels.none { it in ignoredLabels }
                        }
                        .sortedBy { it.birthday.safeDaysUntilNext() }
                        .toList()
                }.collect {
                    value = it
                }
            }
            val contacts = contactsState.value
            
            GlanceTheme {
                WidgetContent(contacts)
            }
        }
    }

    @Composable
    private fun WidgetContent(contacts: List<Contact>) {
        val size = LocalSize.current
        val context = LocalContext.current
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
                .clickable(
                    actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            putExtra("SCROLL_TO_TOP", true)
                            // SINGLE_TOP verhindert unnötiges Neuerstellen der Activity
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                    ),
                ),
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
                    val daysLeft = contact.birthday.safeDaysUntilNext()
                    val nextAgeValue = contact.birthday.safeNextAge()
                    
                    val dateText = if (contact.birthday.year == 1900) {
                        contact.birthday.format(dayMonthFormatter)
                    } else {
                        contact.birthday.format(dateFormatter)
                    }

                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
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
                                        fontSize = 12.sp,
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
                                            fontSize = 12.sp,
                                        )
                                    )
                                }
                                Text(
                                    text = if (daysLeft == 0L) "Heute!" else "In $daysLeft T.",
                                    style = TextStyle(
                                        color = if (daysLeft == 0L) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 11.sp,
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
