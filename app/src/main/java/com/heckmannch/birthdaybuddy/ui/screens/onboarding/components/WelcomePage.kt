package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.illustrations.WelcomeIllustration
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

@Composable
fun WelcomePage(windowWidthSizeClass: WindowWidthSizeClass) {
    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            WelcomeIllustration(modifier)
        },
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc)
    )
}

@Preview(showSystemUi = true)
@Composable
private fun WelcomePagePreview() {
    BirthdayBuddyTheme {
        WelcomePage(windowWidthSizeClass = WindowWidthSizeClass.Compact)
    }
}
