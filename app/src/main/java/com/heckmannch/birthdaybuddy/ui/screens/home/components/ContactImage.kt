package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUri) {
        if (imageUri != null) {
            ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .build()
        } else null
    }

    Surface(
        modifier = modifier.size(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.item_image_desc, fullName),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(text = initials, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
