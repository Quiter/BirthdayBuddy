package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AdaptiveContentContainer
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

@Composable
fun OnboardingFooter(
    currentPage: Int,
    pageCount: Int,
    isNextEnabled: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                // Back Button or Spacer to keep symmetry
                if (currentPage > 0) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                // Dynamic Pill Dots
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = index == currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "dot_width"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            },
                            label = "dot_color"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(color)
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
                            color = if (isNextEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun OnboardingFooterPreview() {
    BirthdayBuddyTheme {
        OnboardingFooter(
            currentPage = 0,
            pageCount = 3,
            isNextEnabled = true,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onBack = {},
            onNext = {}
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun OnboardingFooterMiddlePagePreview() {
    BirthdayBuddyTheme {
        OnboardingFooter(
            currentPage = 1,
            pageCount = 3,
            isNextEnabled = true,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onBack = {},
            onNext = {}
        )
    }
}

