package com.sashtech.mehndidesignsimple.utils

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.load
import coil.request.Disposable
import coil.request.ImageRequest
import com.sashtech.mehndidesignsimple.R

/**
 * Utility singleton for dynamic image loading across Android View & Compose components.
 * Loads dynamic URLs from Firebase Storage / Firestore / Realtime DB.
 * Handles null/empty URLs and network failures gracefully using the neutral
 * "No Image Available" blank placeholder graphic.
 */
object MehndiImageLoader {

    /**
     * Builds a standard Coil ImageRequest configured with fallback, placeholder, and error drawables.
     */
    fun buildImageRequest(
        context: Context,
        imageUrl: String?,
        crossfadeDurationMs: Int = 200
    ): ImageRequest {
        val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() }

        return ImageRequest.Builder(context)
            .data(cleanUrl)
            .placeholder(R.drawable.ic_no_image_available)
            .error(R.drawable.ic_no_image_available)
            .fallback(R.drawable.ic_no_image_available)
            .crossfade(crossfadeDurationMs)
            .build()
    }

    /**
     * Loads a dynamic Firebase image into a standard Android ImageView (e.g. inside RecyclerView Adapters).
     */
    fun loadImage(
        imageView: ImageView,
        imageUrl: String?,
        crossfadeDurationMs: Int = 200
    ): Disposable {
        val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() }

        return imageView.load(cleanUrl) {
            placeholder(R.drawable.ic_no_image_available)
            error(R.drawable.ic_no_image_available)
            fallback(R.drawable.ic_no_image_available)
            crossfade(crossfadeDurationMs)
        }
    }
}
