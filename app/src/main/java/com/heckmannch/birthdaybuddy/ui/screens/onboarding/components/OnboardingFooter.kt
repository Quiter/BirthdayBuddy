package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AdaptiveContentContainer

@Composable
fun OnboardingFooter(
    currentPage: Int,
    pageCount: Int,
    isNextEnabled: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNext: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        AdaptiveContentContainer(windowWidthSizeClass = windowWidthSizeClass) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer links um Dots zu zentrieren
                Spacer(modifier = Modifier.width(80.dp))

                // Dots
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    repeat(pageCount) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (index == currentPage) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // Next Button
                if (currentPage < (pageCount - 1)) {
                    TextButton(
                        onClick = onNext,
                        enabled = isNextEnabled,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            color = if (isNextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }
        }
    }
}
