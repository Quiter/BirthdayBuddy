package com.heckmannch.birthdaybuddy.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.repository.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * EntryPoint für Hilt, da GlanceAppWidget keine Constructor Injection unterstützt.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun birthdayRepository(): BirthdayRepository
    fun filterManager(): FilterManager
}

/**
 * Modernes Homescreen-Widget basierend auf Jetpack Glance.
 * 
 * Hinweis zu @SuppressLint("RestrictedApi"): 
 * Die Glance-Bibliothek (1.1.1) hat einen Bug in der Lint-Erkennung von ColorProvider.
 * Wir unterdrücken diesen Fehler hier global für die Klasse, da der Zugriff auf die
 * öffentliche Hilfsfunktion ColorProvider(resId = ...) technisch absolut korrekt ist.
 */
@SuppressLint("RestrictedApi")
class BirthdayGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.birthdayRepository()
        val filterManager = entryPoint.filterManager()

        val initialContacts = loadWidgetData(repository, filterManager)

        provideContent {
            GlanceTheme {
                WidgetContent(initialContacts)
            }
        }
    }

    private suspend fun loadWidgetData(
        repository: BirthdayRepository,
        filterManager: FilterManager
    ): List<BirthdayContact> {
        val contacts = repository.allBirthdays.first()
        val prefs = filterManager.preferencesFlow.first()
        
        return contacts.filter { contact ->
            val isExcluded = contact.labels.any { prefs.excludedLabels.contains(it) || prefs.widgetExcludedLabels.contains(it) }
            val isSelected = prefs.widgetSelectedLabels.isEmpty() || contact.labels.any { prefs.widgetSelectedLabels.contains(it) }
            !isExcluded && isSelected
        }
        .sortedBy { it.remainingDays }
    }

    @Composable
    private fun WidgetContent(contacts: List<BirthdayContact>) {
        val context = LocalContext.current
        val size = LocalSize.current
        
        val minItemHeight = 64.dp
        val maxItems = (size.height.value / minItemHeight.value).toInt().coerceAtLeast(1)
        val displayedContacts = contacts.take(maxItems)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .appWidgetBackground()
                .padding(8.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (displayedContacts.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.main_no_birthdays),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    displayedContacts.forEach { contact ->
                        BirthdayWidgetItem(
                            contact = contact,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BirthdayWidgetItem(contact: BirthdayContact, modifier: GlanceModifier = GlanceModifier) {
        val context = LocalContext.current
        val isBirthdayToday = contact.remainingDays == 0
        
        val isKidBirthday = contact.age in 1..9
        val isRoundBirthday = contact.age > 0 && contact.age % 10 == 0

        // Nutzung der öffentlichen ColorProvider-API via Ressourcen
        val circleColor = when {
            !isBirthdayToday -> ColorProvider(resId = R.color.widget_upcoming)
            isKidBirthday -> ColorProvider(resId = R.color.widget_kid)
            isRoundBirthday -> ColorProvider(resId = R.color.widget_gold)
            else -> ColorProvider(resId = R.color.widget_silver)
        }

        val circleTextColor = if (isBirthdayToday) {
            ColorProvider(resId = R.color.black)
        } else {
            ColorProvider(resId = R.color.white)
        }

        Box(modifier = modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(12.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = contact.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                    
                    val infoText = when (contact.remainingDays) {
                        0 -> context.getString(R.string.widget_today) + " 🎉"
                        1 -> context.getString(R.string.widget_tomorrow)
                        else -> context.getString(R.string.widget_in_days, contact.remainingDays)
                    }
                    
                    Text(
                        text = infoText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 12.sp
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(circleColor)
                        .cornerRadius(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ageText = if (contact.age < 0) "?" else contact.age.toString()
                    Text(
                        text = ageText,
                        style = TextStyle(
                            color = circleTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

class BirthdayGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthdayGlanceWidget()
}
