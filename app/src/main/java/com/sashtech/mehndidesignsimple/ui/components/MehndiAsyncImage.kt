package com.sashtech.mehndidesignsimple.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.sashtech.mehndidesignsimple.R

/**
 * Universal Image loader component for Mehndi Design.
 * Loads dynamic images from Firebase Storage / URLs.
 * If the URL is null, blank, or fails to load, gracefully displays a neutral
 * "No Image Available" blank placeholder graphic without breaking layout.
 */
@Composable
fun MehndiAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeDurationMs: Int = 200,
    showLoadingSpinner: Boolean = false
) {
    val cleanUrl = imageUrl?.trim().orEmpty()

    if (cleanUrl.isEmpty()) {
        // Render neutral "No Image Available" graphic directly for null/empty URLs
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_no_image_available),
                contentDescription = contentDescription ?: "No Image Available",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (showLoadingSpinner) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(cleanUrl)
                .crossfade(crossfadeDurationMs)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Image(
                    painter = painterResource(id = R.drawable.ic_no_image_available),
                    contentDescription = contentDescription ?: "No Image Available",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(cleanUrl)
                .placeholder(R.drawable.ic_no_image_available)
                .error(R.drawable.ic_no_image_available)
                .fallback(R.drawable.ic_no_image_available)
                .crossfade(crossfadeDurationMs)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
