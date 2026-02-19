package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.heckmannch.birthdaybuddy.data.BirthdayDatabase
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.FilterManager
import kotlinx.coroutines.flow.combine

/**
 * Das moderne Homescreen-Widget basierend auf Jetpack Glance.
 */
class BirthdayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = BirthdayDatabase.getDatabase(context)
        val filterManager = FilterManager(context)
        
        // Daten für das Widget vorbereiten
        val birthdayFlow = combine(
            database.birthdayDao().getAllBirthdays(),
            filterManager.widgetSelectedLabelsFlow,
            filterManager.widgetExcludedLabelsFlow,
            filterManager.widgetItemCountFlow
        ) { contacts, selected, excluded, count ->
            contacts.filter { contact ->
                val isExcluded = contact.labels.any { excluded.contains(it) }
                val isSelected = selected.isEmpty() || contact.labels.any { selected.contains(it) }
                !isExcluded && isSelected
            }.sortedBy { it.remainingDays }.take(count)
        }

        provideContent {
            val contacts by birthdayFlow.collectAsState(initial = emptyList())
            
            // Nutzt Material 3 Farbschemata
            GlanceTheme {
                WidgetContent(contacts)
            }
        }
    }

    @Composable
    private fun WidgetContent(contacts: List<BirthdayContact>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
        ) {
            Text(
                text = "BirthdayBuddy",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )

            if (contacts.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Geburtstage", style = TextStyle(color = GlanceTheme.colors.onSurface))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(contacts) { contact ->
                        BirthdayItem(contact)
                    }
                }
            }
        }
    }

    @Composable
    private fun BirthdayItem(contact: BirthdayContact) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = contact.name,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
                val infoText = when (contact.remainingDays) {
                    0 -> "Heute! 🎉"
                    1 -> "Morgen"
                    else -> "In ${contact.remainingDays} Tagen"
                }
                Text(
                    text = infoText,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
            Text(
                text = contact.age.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class BirthdayGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthdayGlanceWidget()
}
