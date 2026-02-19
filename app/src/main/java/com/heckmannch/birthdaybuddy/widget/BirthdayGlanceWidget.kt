package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.heckmannch.birthdaybuddy.BirthdayApplication
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.flow.combine

/**
 * Modernes Homescreen-Widget basierend auf Jetpack Glance.
 */
class BirthdayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as BirthdayApplication).container
        val repository = appContainer.birthdayRepository
        val filterManager = appContainer.filterManager

        val widgetDataFlow = combine(
            repository.allBirthdays,
            filterManager.widgetSelectedLabelsFlow,
            filterManager.widgetExcludedLabelsFlow,
            filterManager.widgetItemCountFlow
        ) { contacts, selected, excluded, count ->
            contacts.filter { contact ->
                val isExcluded = contact.labels.any { excluded.contains(it) }
                val isSelected = selected.isEmpty() || contact.labels.any { selected.contains(it) }
                !isExcluded && isSelected
            }
            .sortedBy { it.remainingDays }
            .take(count)
        }

        provideContent {
            val contacts by widgetDataFlow.collectAsState(initial = emptyList())
            
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
                .appWidgetBackground()
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // App-Name entfernt, Liste startet direkt oder zeigt Empty State
            if (contacts.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.main_no_birthdays),
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
