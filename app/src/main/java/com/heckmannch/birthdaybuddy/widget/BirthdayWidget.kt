package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import com.heckmannch.birthdaybuddy.util.hasYear
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
            val contactsState = produceState(initialValue = emptyList()) {
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
                            (contact.birthday != null) && contact.labels.none { it in ignoredLabels }
                        }
                        .sortedBy { it.birthday!!.safeDaysUntilNext() }
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
        val locale = context.resources.configuration.locales[0]
        val dateFormatter = remember {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        }
        val dayMonthFormatter = remember(locale) {
            DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(locale, "dMMM"),
                locale,
            )
        }

        // Äußeres Column-Padding abziehen (top=4dp, bottom=4dp -> 8dp)
        val availableHeight = (size.height.value - 8).coerceAtLeast(0f)
        val minItemBlockHeight = 58.dp

        // 1. Berechne wie viele Elemente maximal auf die verfügbare Höhe passen
        val maxItems = (availableHeight / minItemBlockHeight.value).toInt().coerceIn(1, 10)
        val displayContacts = contacts.take(maxItems)
        val count = displayContacts.size

        // 2. Verteile die verfügbare Gesamthöhe gleichmäßig auf alle anzuzeigenden Elemente
        val dynamicBlockHeight = if (count > 0) {
            (availableHeight / count).dp
        } else {
            minItemBlockHeight
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 4.dp)
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
                    BirthdayRow(contact, dynamicBlockHeight, dateFormatter, dayMonthFormatter)
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
    private fun BirthdayRow(
        contact: Contact,
        blockHeight: Dp,
        dateFormatter: DateTimeFormatter,
        dayMonthFormatter: DateTimeFormatter,
    ) {
        val birthday = contact.birthday ?: return
        val context = LocalContext.current

        val daysLeft = birthday.safeDaysUntilNext()
        val nextAgeValue = birthday.safeNextAge()
        val hasYear = birthday.hasYear

        val dateText = if (!hasYear) {
            birthday.format(dayMonthFormatter)
        } else {
            birthday.format(dateFormatter)
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

        val itemBgColor = ColorProvider(
            day = Color(0xCCFFFFFF), // ~80% opaque white (Light Theme)
            night = Color(0xCC1E1E1E) // ~80% opaque dark grey (Dark Theme)
        )

        Box(
            modifier = GlanceModifier
                .height(blockHeight)
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(itemBgColor)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
                        if (hasYear && nextAgeValue != null) {
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
}
