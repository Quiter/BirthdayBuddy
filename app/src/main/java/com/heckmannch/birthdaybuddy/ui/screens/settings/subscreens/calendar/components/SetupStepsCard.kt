package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.StepItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SectionCard
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

private const val TAG = "SetupStepsCard"

/**
 * Displays the progressive, sequential steps required to set up calendar synchronization
 * inside an expressive [SectionCard].
 */
@Composable
internal fun SetupStepsCard(
    hasPermission: Boolean,
    calendarSyncEnabled: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(SpacingNormal)) {
            StepItem(
                stepNumber = 1,
                title = stringResource(R.string.calendar_guide_step1_title),
                description = stringResource(R.string.calendar_guide_step1_desc),
                isCompleted = hasPermission,
                actionButton = if (!hasPermission) {
                    {
                        Button(onClick = onRequestPermission) {
                            Text(stringResource(R.string.calendar_settings_permission_btn))
                        }
                    }
                } else {
                    null
                },
            )

            StepItem(
                stepNumber = 2,
                title = stringResource(R.string.calendar_guide_step2_title),
                description = stringResource(R.string.calendar_guide_step2_desc),
                isCompleted = calendarSyncEnabled && hasPermission,
                isLocked = !hasPermission,
            )

            StepItem(
                stepNumber = 3,
                title = stringResource(R.string.calendar_guide_step3_title),
                description = stringResource(R.string.calendar_guide_step3_desc),
                isCompleted = false,
                isLocked = !(calendarSyncEnabled && hasPermission),
                icon = Icons.Rounded.CalendarMonth,
            )

            if (calendarSyncEnabled && hasPermission) {
                Button(
                    onClick = { openDefaultCalendarApp(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeMedium),
                        )
                        Spacer(modifier = Modifier.width(SpacingSmall))
                        Text(stringResource(R.string.onboarding_calendar_guide_btn))
                    }
                }
            }
        }
    }
}

private fun openDefaultCalendarApp(context: Context) {
    try {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        android.content.ContentUris.appendId(builder, System.currentTimeMillis())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = builder.build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Standard-Kalenderansicht konnte nicht geöffnet werden, versuche Fallback-Kategorie", e)
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Log.e(TAG, "Could not open calendar app", e2)
        }
    }
}
