package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
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
import androidx.glance.color.ColorProvider
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
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.util.ContactFilterLogic
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny
import com.heckmannch.birthdaybuddy.ui.theme.WidgetCornerRadius
import com.heckmannch.birthdaybuddy.util.IntentExtras
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
import java.util.Locale

/**
 * Reusable ColorProvider for the item background (~80% opacity)
 * to avoid repeated allocations on every recomposition.
 */
private val WidgetItemBackground = ColorProvider(
    day = Color(0xCCFFFFFF), // ~80% opaque white (Light Theme)
    night = Color(0xCC1E1E1E), // ~80% opaque dark gray (Dark Theme)
)

// Font size constants to avoid magic values
private val WidgetNameFontSize: TextUnit = 14.sp
private val WidgetDateFontSize: TextUnit = 12.sp
private val WidgetAgeFontSize: TextUnit = 12.sp
private val WidgetDaysLeftFontSize: TextUnit = 11.sp

/**
 * Hilt EntryPoint interface for Glance widget instances.
 *
 * Provides access to [ContactRepository] in the Glance lifecycle without standard field injection,
 * as widgets are instantiated by the Android system outside of standard Android/Hilt lifecycles.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun contactRepository(): ContactRepository
}

/**
 * Glance-based AppWidget displaying upcoming birthdays on the user's home screen.
 *
 * Highlights & architectural details:
 * - **Glance Architecture**: Built with Jetpack Glance, bridging Compose-like declarative UI to RemoteViews.
 * - **Exact SizeMode**: Utilizes [SizeMode.Exact] to obtain the precise widget dimensions ([LocalSize])
 *   and adaptively tailor the number of displayed items.
 * - **Dynamic Height Scaling**: Employs [WidgetLayoutHelper] to dynamically distribute available height
 *   across rows, avoiding layout clipping and scroll lag.
 * - **M3 Translucent Design**: Displays cards with ~80% opacity on a transparent widget container
 *   in accordance with Material 3 styling and project conventions.
 */
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
                    repository.labelsEnabled,
                ) { list, configs, labelsEnabled ->
                    ContactFilterLogic.filterForWidget(
                        contacts = list,
                        labelsEnabled = labelsEnabled,
                        configs = configs
                    ).sortedBy { it.birthday?.safeDaysUntilNext() ?: Long.MAX_VALUE }
                }.collect { value = it }
            }

            val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
            GlanceTheme {
                WidgetContent(contacts = contactsState.value, locale = locale)
            }
        }
    }

    @Composable
    private fun WidgetContent(contacts: List<Contact>, locale: Locale) {
        val size = LocalSize.current
        val context = LocalContext.current
        val dateFormatter = remember {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        }
        val dayMonthFormatter = remember(locale) {
            DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(locale, "dMMM"),
                locale,
            )
        }

        // Calculate layout subset and dynamic block height using the stateless helper
        val (displayContacts, dynamicBlockHeight) = WidgetLayoutHelper.calculateLayout(
            totalHeight = size.height,
            contacts = contacts,
            verticalPadding = SpacingExtraSmall * 2,
        )

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall)
                .clickable(
                    actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(IntentExtras.SCROLL_TO_TOP, true)
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

        Box(
            modifier = GlanceModifier
                .height(blockHeight)
                .fillMaxWidth()
                .padding(top = SpacingTiny, bottom = SpacingTiny),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetItemBackground)
                    .cornerRadius(WidgetCornerRadius)
                    .padding(horizontal = SpacingMedium, vertical = SpacingExtraSmall),
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
                                fontSize = WidgetNameFontSize,
                            ),
                        )
                        Text(
                            text = dateText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = WidgetDateFontSize,
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
                                    fontSize = WidgetAgeFontSize,
                                ),
                            )
                        }
                        Text(
                            text = daysLeftText,
                            style = TextStyle(
                                color = if (daysLeft == 0L) {
                                    GlanceTheme.colors.primary
                                } else {
                                    GlanceTheme.colors.onSurfaceVariant
                                },
                                fontWeight = if (daysLeft == 0L) FontWeight.Bold else FontWeight.Normal,
                                fontSize = WidgetDaysLeftFontSize,
                            ),
                        )
                    }
                }
            }
        }
    }
}
