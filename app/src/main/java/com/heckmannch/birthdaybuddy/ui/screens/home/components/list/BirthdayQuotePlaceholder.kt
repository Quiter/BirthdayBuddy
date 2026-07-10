package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaBorderSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarDisabled
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CardCornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.CardCornerRadiusNormal
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayQuoteIconContainerSize
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun BirthdayQuotePlaceholder(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(SpacingNormal),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(CardCornerRadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Premium Icon Container
            Surface(
                modifier = Modifier.size(BirthdayQuoteIconContainerSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaOnboardingCalendarDisabled),
                border = BorderStroke(
                    width = BorderWidthThick,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaBorderSubtle)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        modifier = Modifier.size(ContactImageSizeNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingLarge))

            Text(
                text = stringResource(R.string.detail_placeholder_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SpacingNormal))

            // Beautiful Quote Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadiusNormal),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaEmphasisLow),
                border = BorderStroke(
                    width = BorderWidthThin,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaOnboardingCalendarDisabled)
                )
            ) {
                Text(
                    text = stringResource(R.string.detail_placeholder_quote),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(SpacingLarge)
                )
            }

            Spacer(modifier = Modifier.height(SpacingNormal))

            Text(
                text = stringResource(R.string.detail_placeholder_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium),
                textAlign = TextAlign.Center
            )
        }
    }
}
