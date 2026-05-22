package com.heckmannch.birthdaybuddy.util

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePrefetcherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImagePrefetcher {
    override fun prefetch(imageUris: List<String>) {
        imageUris.forEach { uri ->
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(150)
                .build()
            context.imageLoader.enqueue(request)
        }
    }
}
