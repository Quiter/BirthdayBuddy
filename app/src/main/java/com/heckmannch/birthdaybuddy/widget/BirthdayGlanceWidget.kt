package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
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
 */
class BirthdayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Abhängigkeiten über den Hilt EntryPoint beziehen
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.birthdayRepository()
        val filterManager = entryPoint.filterManager()

        // Wir laden die Daten einmalig für das initiale Rendering
        // Glance bietet keine native Flow-Observation im Composable wie "collectAsState" in der UI
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
        .take(prefs.widgetItemCount)
    }

    @Composable
    private fun WidgetContent(contacts: List<BirthdayContact>) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .appWidgetBackground()
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (contacts.isEmpty()) {
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
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(contacts) { contact ->
                        BirthdayWidgetItem(contact)
                    }
                }
            }
        }
    }

    @Composable
    private fun BirthdayWidgetItem(contact: BirthdayContact) {
        val context = LocalContext.current
        
        Column(modifier = GlanceModifier.padding(vertical = 4.dp)) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
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
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ageText = if (contact.age < 0) "?" else contact.age.toString()
                    Text(
                        text = ageText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
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
