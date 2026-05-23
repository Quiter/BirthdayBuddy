package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R

@Composable
fun ContactsPage(
    windowWidthSizeClass: WindowWidthSizeClass,
    isGranted: Boolean,
    onGrant: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingAdaptivePage(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    modifier = modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = stringResource(R.string.onboarding_contacts_title),
        description = stringResource(R.string.onboarding_contacts_desc),
        extraContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isGranted) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onGrant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_contacts_btn))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_contacts_skip),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_contacts_granted),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}
