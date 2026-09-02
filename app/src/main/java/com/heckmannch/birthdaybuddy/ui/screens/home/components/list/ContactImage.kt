package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ContactAvatarHeaderSize
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Computes a robust cache key for contact avatar images used in Coil memory and disk caches.
 * Combining lookupKey (if available) with imageUri ensures stable cache keys across list renders and fast scrolling.
 */
fun getAvatarCacheKey(imageUri: String, lookupKey: String? = null): String {
    return if (!lookupKey.isNullOrBlank()) "avatar_${lookupKey}_${imageUri}" else "avatar_${imageUri}"
}

/**
 * Displays a contact's avatar image or initials fallback.
 *
 * Supports both single contact avatars and overlapping dual-avatar layouts for paired / couple contacts
 * (e.g. joint anniversaries or partnered birthdays).
 *
 * ### Size & Typography Scaling Logic:
 * - Dynamic corner radius scaling: Outer corner is sized proportionally at `size * 0.3f`.
 * - In dual-avatar mode, nested avatars scale to `size * 0.7f` with corner radius `nestedSize * 0.28f`.
 *   The secondary avatar (bottom-right) is surrounded by a surface cutout border overlay.
 * - Single avatar initials typography adapts automatically: [MaterialTheme.typography.titleLarge] for sizes
 *   larger than [ContactImageSizeSmall] (e.g. expanded items, header, or detail views), otherwise [MaterialTheme.typography.titleSmall].
 * - In dual-avatar mode, text style scales between [MaterialTheme.typography.titleSmall] and [MaterialTheme.typography.labelSmall].
 *
 * @param imageUri URI string for the primary contact's photo, or `null` to show [initials].
 * @param fullName Full display name of the primary contact (used for accessibility content description).
 * @param initials Initials of the primary contact displayed when [imageUri] is unavailable.
 * @param modifier [Modifier] applied to the outer avatar container.
 * @param lookupKey Optional contact lookup key used for generating stable Coil memory and disk cache keys.
 * @param secondImageUri URI string for the optional secondary contact in a couple/paired event.
 * @param secondInitials Initials of the secondary contact displayed when [secondImageUri] is unavailable.
 * @param secondFullName Full display name of the secondary contact (used for accessibility description).
 * @param secondLookupKey Optional contact lookup key for the secondary contact's avatar caching.
 * @param size Outer dimension size of the avatar container. Defaults to [ContactImageSizeSmall].
 */
@Composable
fun ContactImage(
    imageUri: String?,
    fullName: String,
    initials: String,
    modifier: Modifier = Modifier,
    lookupKey: String? = null,
    secondImageUri: String? = null,
    secondInitials: String? = null,
    secondFullName: String? = null,
    secondLookupKey: String? = null,
    size: Dp = ContactImageSizeSmall,
) {
    val outerCorner = size * 0.3f
    val nestedSize = size * 0.7f
    val nestedCorner = nestedSize * 0.28f
    val secondAvatarPadding = (size * 0.0375f).coerceAtLeast(SelectedBorderWidth)
    val secondAvatarCorner = nestedCorner * 0.875f

    val firstCacheKey = remember(imageUri, lookupKey) {
        imageUri?.let { getAvatarCacheKey(it, lookupKey) }
    }
    val secondCacheKey = remember(secondImageUri, secondLookupKey) {
        secondImageUri?.let { getAvatarCacheKey(it, secondLookupKey) }
    }

    val isDualAvatar = !secondInitials.isNullOrBlank() || !secondImageUri.isNullOrBlank()

    if (!isDualAvatar) {
        SingleAvatar(
            modifier = modifier.size(size),
            imageUri = imageUri,
            cacheKey = firstCacheKey,
            fullName = fullName,
            initials = initials,
            shape = RoundedCornerShape(outerCorner),
            textStyle = if (size > ContactImageSizeSmall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        val resolvedSecondFullName = secondFullName?.takeIf { it.isNotBlank() }
            ?: secondInitials?.takeIf { it.isNotBlank() }
            ?: fullName

        // Overlapping dual avatar layout for couples
        Box(
            modifier = modifier.size(size)
        ) {
            // First contact (top-left)
            SingleAvatar(
                modifier = Modifier
                    .size(nestedSize)
                    .align(Alignment.TopStart),
                imageUri = imageUri,
                cacheKey = firstCacheKey,
                fullName = fullName,
                initials = initials,
                shape = RoundedCornerShape(nestedCorner),
                textStyle = if (size > ContactImageSizeSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Second contact (bottom-right) with a surface border overlay
            Box(
                modifier = Modifier
                    .size(nestedSize)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(nestedCorner))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(secondAvatarPadding)
            ) {
                SingleAvatar(
                    modifier = Modifier.fillMaxSize(),
                    imageUri = secondImageUri,
                    cacheKey = secondCacheKey,
                    fullName = resolvedSecondFullName,
                    initials = secondInitials ?: "",
                    shape = RoundedCornerShape(secondAvatarCorner),
                    textStyle = if (size > ContactImageSizeSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SingleAvatar(
    modifier: Modifier = Modifier,
    imageUri: String?,
    cacheKey: String?,
    fullName: String,
    initials: String,
    shape: Shape,
    textStyle: TextStyle,
    containerColor: Color,
    contentColor: Color,
) {
    if (imageUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .build(),
            contentDescription = stringResource(R.string.item_image_desc, fullName),
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = textStyle,
                color = contentColor
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ContactImagePreview() {
    BirthdayBuddyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SpacingNormal),
                verticalArrangement = Arrangement.spacedBy(SpacingNormal)
            ) {
                // Row 1: Standard List Size (40.dp) - Single Initials, Image URI, Couple Initials
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Einzelner Kontakt mit Initialen
                    ContactImage(
                        imageUri = null,
                        fullName = "Max Mustermann",
                        initials = "MM",
                        size = ContactImageSizeSmall
                    )

                    // 2. Einzelner Kontakt mit Bild-URI
                    ContactImage(
                        imageUri = "content://com.android.contacts/display_photo/1",
                        fullName = "Erika Mustermann",
                        initials = "EM",
                        size = ContactImageSizeSmall
                    )

                    // 3. Paar-Kontakt mit Initialen (Primary- & Secondary-Container)
                    ContactImage(
                        imageUri = null,
                        fullName = "Max Mustermann",
                        initials = "M",
                        secondImageUri = null,
                        secondFullName = "Erika Mustermann",
                        secondInitials = "E",
                        size = ContactImageSizeSmall
                    )
                }

                // Row 2: Vergrößerte Kontakte (z. B. Detail- / Header-Größe)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 4. Vergrößerter Kontakt (Header-Größe)
                    ContactImage(
                        imageUri = null,
                        fullName = "Max Mustermann",
                        initials = "MM",
                        size = ContactAvatarHeaderSize
                    )

                    // Vergrößerter Paar-Kontakt (Header-Größe)
                    ContactImage(
                        imageUri = null,
                        fullName = "Max Mustermann",
                        initials = "M",
                        secondImageUri = null,
                        secondFullName = "Erika Mustermann",
                        secondInitials = "E",
                        size = ContactAvatarHeaderSize
                    )

                    // Großer Detail-Kontakt (Large 96.dp)
                    ContactImage(
                        imageUri = null,
                        fullName = "Lukas Kind",
                        initials = "LK",
                        size = ContactImageSizeLarge
                    )
                }
            }
        }
    }
}


