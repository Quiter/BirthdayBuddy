package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.StepItem
import com.heckmannch.birthdaybuddy.ui.illustrations.CalendarGuideIllustration
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarCellSize
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun CalendarGuidePage() {
    val context = LocalContext.current

    OnboardingPageTemplate(
        illustration = { modifier ->
            CalendarGuideIllustration(modifier)
        },
        title = stringResource(R.string.onboarding_calendar_guide_title),
        description = stringResource(R.string.onboarding_calendar_guide_desc),
        settingsCard = {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaEmphasisLow)
                )
            ) {
                Column(modifier = Modifier.padding(SpacingNormal)) {
                    // Step 1
                    StepItem(
                        title = stringResource(R.string.onboarding_calendar_guide_step1_title),
                        description = stringResource(R.string.onboarding_calendar_guide_step1_desc),
                        icon = Icons.Default.PlayArrow
                    )

                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Step 2
                    StepItem(
                        title = stringResource(R.string.onboarding_calendar_guide_step2_title),
                        description = stringResource(R.string.onboarding_calendar_guide_step2_desc),
                        icon = Icons.Default.Menu
                    )

                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Step 3
                    StepItem(
                        title = stringResource(R.string.onboarding_calendar_guide_step3_title),
                        description = stringResource(R.string.onboarding_calendar_guide_step3_desc),
                        icon = Icons.Default.Check
                    )
                }
            }
        },
        actionButton = {
            Button(
                onClick = { openDefaultCalendarApp(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SearchBarHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(OnboardingCalendarCellSize)
                    )
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(stringResource(R.string.onboarding_calendar_guide_btn))
                }
            }
        }
    )
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
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Log.e("CalendarSyncRepo", "Could not open calendar app", e2)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CalendarGuidePagePreview() {
    BirthdayBuddyTheme {
        CalendarGuidePage()
    }
}
