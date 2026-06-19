package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowHeightSizeClass

/**
 * Ein einheitliches, adaptives Onboarding-Gerüst, das perfekt zentrierte,
 * sprungfreie und pixelgenau ausgerichtete Layouts garantiert.
 */
@Composable
fun OnboardingPageTemplate(
    windowWidthSizeClass: WindowWidthSizeClass,
    illustration: @Composable (Modifier) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    settingsCard: @Composable (ColumnScope.() -> Unit)? = null,
    actionButton: @Composable (BoxScope.() -> Unit)? = null
) {
    val windowHeightSizeClass = LocalWindowHeightSizeClass.current
    val isShortScreen = windowHeightSizeClass == WindowHeightSizeClass.Compact

    if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = if (isShortScreen) 16.dp else 32.dp,
                    bottom = if (isShortScreen) 16.dp else 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Oberer Bereich
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Fester Container für das Icon/Illustration
                Box(
                    modifier = Modifier
                        .height(if (isShortScreen) 120.dp else 160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    illustration(Modifier)
                }

                Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 32.dp))

                // Titel
                Text(
                    text = title,
                    style = if (isShortScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 32.dp))

                // Beschreibungstext
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                if (settingsCard != null) {
                    Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 24.dp))
                    // Einstellungen (Switches/Zusammenfassungen)
                    settingsCard()
                }
            }

            // Feste untere Sektion für den Button
            Spacer(modifier = Modifier.height(if (isShortScreen) 8.dp else 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isShortScreen) 48.dp else 56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (actionButton != null) {
                    actionButton()
                }
            }
        }
    } else {
        // Zwei-Spalten-Layout für Tablets/Breitbild
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    horizontal = 48.dp,
                    vertical = if (isShortScreen) 16.dp else 32.dp
                ),
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

            // Rechte Spalte: Titel, Beschreibung, Einstellungen und Aktionen
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
                    style = if (isShortScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(if (isShortScreen) 8.dp else 16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (settingsCard != null) {
                    Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 24.dp))
                    settingsCard()
                }
                if (actionButton != null) {
                    Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        actionButton()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun OnboardingPageTemplatePreview() {
    MaterialTheme {
        Surface {
            OnboardingPageTemplate(
                windowWidthSizeClass = WindowWidthSizeClass.Compact,
                illustration = { modifier ->
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        modifier = modifier.size(120.dp)
                    )
                },
                title = "Einstellungen",
                description = "Konfiguriere deine Benachrichtigungen für anstehende Geburtstage.",
                settingsCard = {
                    Text(
                        "Hier könnten Einstellungen stehen",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                actionButton = {
                    Button(onClick = {}) {
                        Text("Weiter")
                    }
                }
            )
        }
    }
}
