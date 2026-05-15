package com.heckmannch.birthdaybuddy.ui.screens.settings.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var policyText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            context.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use {
                policyText = it.readText()
            }
        } catch (e: Exception) {
            policyText = "Error loading privacy policy."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.notifications_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = policyText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun PrivacyPolicyScreenPreview() {
    MaterialTheme {
        PrivacyPolicyScreen(onNavigateBack = {})
    }
}
