package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about

import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.ui.components.settings.SectionCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsNotice
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSemanticTone
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Precompiled regular expression for parsing Markdown links (`[label](url)`).
 */
private val LINK_REGEX = Regex("\\[([^]]+)]\\(([^)]+)\\)")

private const val TAG = "PrivacyPolicyScreen"

/**
 * Hilt EntryPoint exposing [IoDispatcher] for composables without a dedicated ViewModel.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrivacyPolicyEntryPoint {
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher
}

private sealed interface PrivacyPolicyUiState {
    data object Loading : PrivacyPolicyUiState
    data class Success(val text: String) : PrivacyPolicyUiState
    data class Error(val fallbackText: String) : PrivacyPolicyUiState
}

/**
 * Expressive Privacy Policy subpage rendering localized Markdown from raw resources.
 */
@Composable
fun PrivacyPolicyScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    val errorMessage = stringResource(R.string.settings_privacy_load_error)

    val ioDispatcher = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                PrivacyPolicyEntryPoint::class.java,
            ).ioDispatcher()
        }.getOrElse { Dispatchers.IO }
    }

    val uiState by produceState<PrivacyPolicyUiState>(
        initialValue = PrivacyPolicyUiState.Loading,
        configuration,
        ioDispatcher,
    ) {
        value = try {
            val text = withContext(ioDispatcher) {
                loadPrivacyPolicyText(resources, errorMessage)
            }
            if (text == errorMessage) {
                PrivacyPolicyUiState.Error(errorMessage)
            } else {
                PrivacyPolicyUiState.Success(text)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unerwarteter Fehler beim asynchronen Laden der Datenschutzerklärung", e)
            PrivacyPolicyUiState.Error(errorMessage)
        }
    }

    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_privacy_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) {
        when (val state = uiState) {
            is PrivacyPolicyUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SpacingExtraLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PrivacyPolicyUiState.Success -> {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(SpacingNormal)) {
                        MarkdownContent(state.text)
                    }
                }
            }

            is PrivacyPolicyUiState.Error -> {
                SettingsNotice(
                    message = state.fallbackText,
                    icon = Icons.Rounded.ErrorOutline,
                    tone = SettingsSemanticTone.Error,
                )
            }
        }
    }
}

internal sealed interface MarkdownBlock {
    data class Heading1(val text: String) : MarkdownBlock
    data class Heading2(val text: String) : MarkdownBlock
    data class BulletItem(val text: AnnotatedString) : MarkdownBlock
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock
    data object Divider : MarkdownBlock
}

internal fun parseMarkdownBlocks(
    text: String,
    linkColor: Color,
): List<MarkdownBlock> {
    return text.lineSequence()
        .map { it.trim() }
        .mapNotNull { trimmed ->
            when {
                trimmed.startsWith("# ") -> MarkdownBlock.Heading1(trimmed.removePrefix("# "))
                trimmed.startsWith("## ") -> MarkdownBlock.Heading2(trimmed.removePrefix("## "))
                trimmed.startsWith("* ") -> MarkdownBlock.BulletItem(
                    parseMarkdownInline(trimmed.removePrefix("* "), linkColor),
                )

                trimmed == "***" || trimmed == "---" -> MarkdownBlock.Divider
                trimmed.isNotBlank() -> MarkdownBlock.Paragraph(
                    parseMarkdownInline(trimmed, linkColor),
                )

                else -> null
            }
        }
        .toList()
}

@Composable
private fun MarkdownContent(text: String) {
    val linkColor = MaterialTheme.colorScheme.primary
    val blocks = remember(text, linkColor) {
        parseMarkdownBlocks(text, linkColor)
    }
    val bulletPoint = stringResource(R.string.bullet_point)

    blocks.forEach { block ->
        when (block) {
            is MarkdownBlock.Heading1 -> {
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = SpacingSmall),
                )
            }

            is MarkdownBlock.Heading2 -> {
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = SpacingMedium),
                )
            }

            is MarkdownBlock.BulletItem -> {
                Row(modifier = Modifier.padding(start = SpacingSmall)) {
                    Text(
                        text = bulletPoint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            MarkdownBlock.Divider -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = SpacingSmall))
            }

            is MarkdownBlock.Paragraph -> {
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun parseMarkdownInline(
    text: String,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    var currentIndex = 0

    LINK_REGEX.findAll(text).forEach { match ->
        appendBoldText(text.substring(currentIndex, match.range.first))

        val linkText = match.groupValues[1]
        val url = match.groupValues[2]

        val start = length
        append(linkText)
        addLink(
            url = LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold,
                    )
                ),
            ),
            start = start,
            end = length,
        )

        currentIndex = match.range.last + 1
    }

    appendBoldText(text.substring(currentIndex))
}

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

/**
 * Synchronously reads the localized privacy policy Markdown text from raw resources.
 */
internal fun loadPrivacyPolicyText(resources: Resources, errorMessage: String): String {
    return try {
        resources.openRawResource(R.raw.privacy_policy).bufferedReader().use {
            it.readText()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Fehler beim Laden der Datenschutzerklärung aus raw-Ressourcen", e)
        errorMessage
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyPolicyScreenPreview() {
    BirthdayBuddyTheme {
        PrivacyPolicyScreen(onNavigateBack = {})
    }
}
