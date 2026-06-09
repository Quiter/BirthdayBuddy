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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.R

@Composable
fun ContactImage(
    imageUri: String?,
    fullName: String,
    initials: String,
    modifier: Modifier = Modifier,
    secondImageUri: String? = null,
    secondInitials: String? = null,
    secondFullName: String? = null,
    size: Dp = 40.dp,
) {
    val outerCorner = remember(size) { size * 0.3f }
    val nestedSize = remember(size) { size * 0.7f }
    val nestedCorner = remember(nestedSize) { nestedSize * 0.28f }

    if (secondInitials == null) {
        if (imageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
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
                    style = if (size > 40.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
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
                            .crossfade(true)
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
                            style = if (size > 40.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
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
                    .padding(remember(size) { (size * 0.0375f).coerceAtLeast(1.5.dp) })
                    .clip(RoundedCornerShape(remember(nestedCorner) { nestedCorner * 0.875f }))
            ) {
                if (secondImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(secondImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.item_image_desc, secondFullName ?: ""),
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
                            style = if (size > 40.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
