package com.heckmannch.birthdaybuddy.ui.screens.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PrivacyPolicyScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var policyText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uri = "android.resource://${context.packageName}/${R.raw.privacy_policy}".toUri()
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    policyText = it.readText()
                }
            } catch (_: Exception) {
                policyText = "Error loading privacy policy."
            }
        }
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.settings_privacy_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SpacingNormal,
                    top = paddingValues.calculateTopPadding() + SpacingNormal,
                    end = SpacingNormal,
                    bottom = paddingValues.calculateBottomPadding() + SpacingNormal
                ),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            MarkdownContent(policyText)
            Spacer(modifier = Modifier.height(SpacingExtraLarge))
        }
    }
}

/**
 * Ein sehr simpler Markdown-Renderer für die Datenschutzerklärung.
 */
@Composable
private fun MarkdownContent(text: String) {
    val lines = text.split("\n")
    val linkColor = MaterialTheme.colorScheme.primary

    lines.forEach { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("# ") -> {
                Text(
                    text = trimmed.removePrefix("# "),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = SpacingSmall)
                )
            }

            trimmed.startsWith("## ") -> {
                Text(
                    text = trimmed.removePrefix("## "),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = SpacingMedium)
                )
            }

            trimmed.startsWith("* ") -> {
                Row(modifier = Modifier.padding(start = SpacingSmall)) {
                    Text(stringResource(R.string.bullet_point), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("* "), linkColor),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            trimmed == "***" || trimmed == "---" -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = SpacingSmall))
            }

            trimmed.isNotBlank() -> {
                Text(
                    text = parseMarkdownInline(trimmed, linkColor),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Wandelt Markdown-Formatierungen (Fett, Links) in einen AnnotatedString um.
 */
private fun parseMarkdownInline(
    text: String,
    linkColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val linkRegex = Regex("\\[([^]]+)]\\(([^)]+)\\)")

    return buildAnnotatedString {
        var currentIndex = 0

        linkRegex.findAll(text).forEach { match ->
            // Text vor dem Link verarbeiten (Fett-Check)
            appendBoldText(text.substring(currentIndex, match.range.first))

            val linkText = match.groupValues[1]
            val url = match.groupValues[2]

            // Link hinzufügen
            val start = length
            append(linkText)
            addLink(
                url = LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    )
                ),
                start = start,
                end = length
            )

            currentIndex = match.range.last + 1
        }

        // Restlichen Text nach dem letzten Link verarbeiten
        appendBoldText(text.substring(currentIndex))
    }
}

/**
 * Hilfsfunktion zum Verarbeiten von fettgedrucktem Text.
 */
private fun AnnotatedString.Builder.appendBoldText(text: String) {
    val parts = text.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(part)
            }
        } else {
            append(part)
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun PrivacyPolicyScreenPreview() {
    MaterialTheme {
        PrivacyPolicyScreen(
            onNavigateBack = {}
        )
    }
}
