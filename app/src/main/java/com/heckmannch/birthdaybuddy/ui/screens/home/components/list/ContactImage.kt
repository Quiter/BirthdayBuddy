package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth

/**
 * Computes a robust cache key for contact avatar images used in Coil memory and disk caches.
 * Combining lookupKey (if available) with imageUri ensures stable cache keys across list renders and fast scrolling.
 */
fun getAvatarCacheKey(imageUri: String, lookupKey: String? = null): String {
    return if (!lookupKey.isNullOrBlank()) "avatar_${lookupKey}_${imageUri}" else "avatar_${imageUri}"
}

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
    val outerCorner = remember(size) { size * 0.3f }
    val nestedSize = remember(size) { size * 0.7f }
    val nestedCorner = remember(nestedSize) { nestedSize * 0.28f }

    val firstCacheKey = remember(imageUri, lookupKey) {
        imageUri?.let { getAvatarCacheKey(it, lookupKey) }
    }
    val secondCacheKey = remember(secondImageUri, secondLookupKey) {
        secondImageUri?.let { getAvatarCacheKey(it, secondLookupKey) }
    }

    if (secondInitials == null) {
        if (imageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .memoryCacheKey(firstCacheKey)
                    .diskCacheKey(firstCacheKey)
                    .build(),
                contentDescription = stringResource(R.string.item_image_desc, fullName),
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(outerCorner)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(outerCorner))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = if (size > ContactImageSizeSmall) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        // Overlapping dual avatar layout for couples
        Box(
            modifier = modifier.size(size)
        ) {
            // First contact (top-left)
            Box(
                modifier = Modifier
                    .size(nestedSize)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(nestedCorner))
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .memoryCacheKey(firstCacheKey)
                            .diskCacheKey(firstCacheKey)
                            .build(),
                        contentDescription = stringResource(R.string.item_image_desc, fullName),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = if (size > ContactImageSizeSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Second contact (bottom-right) with a surface border overlay
            Box(
                modifier = Modifier
                    .size(nestedSize)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(nestedCorner))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(remember(size) { (size * 0.0375f).coerceAtLeast(SelectedBorderWidth) })
                    .clip(RoundedCornerShape(remember(nestedCorner) { nestedCorner * 0.875f }))
            ) {
                if (secondImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(secondImageUri)
                            .memoryCacheKey(secondCacheKey)
                            .diskCacheKey(secondCacheKey)
                            .build(),
                        contentDescription = stringResource(
                            R.string.item_image_desc,
                            secondFullName ?: ""
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = secondInitials,
                            style = if (size > ContactImageSizeSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

