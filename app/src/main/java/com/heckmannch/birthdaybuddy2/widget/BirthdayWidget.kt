package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.database.Contact
import com.heckmannch.birthdaybuddy.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.safeDaysUntilNext
import com.heckmannch.birthdaybuddy.util.safeNextAge
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun contactRepository(): ContactRepository
}

class BirthdayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        ).contactRepository()

        provideContent {
            val contactsState = produceState<List<Contact>>(initialValue = emptyList()) {
                combine(
                    repository.allContacts,
                    repository.labelConfigs,
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
                }.collect { value = it }
            }

            GlanceTheme {
                WidgetContent(contactsState.value)
            }
        }
    }

    @Composable
    private fun WidgetContent(contacts: List<Contact>) {
        val size = LocalSize.current
        val context = LocalContext.current

        // Minimale Item-Höhe von 56dp für Touch-Ziele und Lesbarkeit
        val minItemHeight = 56.dp
        // Glance Columns haben ein Limit von 10 Kindern
        val maxItems = (size.height.value / minItemHeight.value).toInt().coerceIn(1, 10)
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
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                    ),
                ),
        ) {
            if (displayContacts.isEmpty()) {
                EmptyState()
            } else {
                displayContacts.forEach { contact ->
                    BirthdayRow(contact)
                }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        val context = LocalContext.current
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = context.getString(R.string.widget_no_birthdays),
                style = TextStyle(color = GlanceTheme.colors.onSurface),
            )
        }
    }

    @Composable
    private fun ColumnScope.BirthdayRow(contact: Contact) {
        val context = LocalContext.current
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMM")

        val daysLeft = contact.birthday.safeDaysUntilNext()
        val nextAgeValue = contact.birthday.safeNextAge()
        val hasYear = contact.birthday.year != 1900

        val dateText = if (!hasYear) {
            contact.birthday.format(dayMonthFormatter)
        } else {
            contact.birthday.format(dateFormatter)
        }

        val daysLeftText = if (daysLeft == 0L) {
            context.getString(R.string.item_today)
        } else {
            context.resources.getQuantityString(
                R.plurals.item_days_left,
                daysLeft.toInt(),
                daysLeft.toInt(),
            )
        }

        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = contact.fullName,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        ),
                    )
                    Text(
                        text = dateText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (hasYear) {
                        Text(
                            text = context.getString(R.string.widget_turns_age, nextAgeValue),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                    Text(
                        text = daysLeftText,
                        style = TextStyle(
                            color = if (daysLeft == 0L) {
                                GlanceTheme.colors.error
                            } else {
                                GlanceTheme.colors.onSurfaceVariant
                            },
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
    }
}
