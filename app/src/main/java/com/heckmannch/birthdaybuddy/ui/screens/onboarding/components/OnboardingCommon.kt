package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.components.LottieIllustration

@Composable
fun OnboardingPageWrapper(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        content = content
    )
}

/**
 * Ein adaptives Onboarding-Seitenlayout, das auf Smartphones eine vertikale Spalte
 * und auf breiteren Bildschirmen (Tablets, Chromebooks) ein edles Zwei-Spalten-Layout darstellt.
 */
@Composable
fun OnboardingAdaptivePage(
    windowWidthSizeClass: WindowWidthSizeClass,
    illustration: @Composable (Modifier) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    extraContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        OnboardingPageWrapper(modifier = modifier) {
            illustration(Modifier)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (extraContent != null) {
                Spacer(modifier = Modifier.height(24.dp))
                extraContent()
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Linke Spalte: Illustration/Animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                illustration(Modifier)
            }

            // Rechte Spalte: Titel, Beschreibung und Aktionen (scrollbar bei Bedarf)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (extraContent != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    extraContent()
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    title: String,
    description: String,
    windowWidthSizeClass: WindowWidthSizeClass,
    icon: Painter? = null,
    @RawRes lottieRes: Int? = null,
    tint: Color = Color.Unspecified
) {
    OnboardingAdaptivePage(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            if (lottieRes != null) {
                LottieIllustration(
                    resId = lottieRes,
                    modifier = modifier.size(200.dp)
                )
            } else if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = modifier.size(120.dp),
                    tint = tint
                )
            }
        },
        title = title,
        description = description
    )
}
