package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R

@Composable
fun WelcomePage() {
    OnboardingPageContent(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc),
        icon = painterResource(R.drawable.ic_app_logo),
        // lottieRes = R.raw.anim_welcome,
    )
}
