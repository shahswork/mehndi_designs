package com.sashtech.mehndidesignsimple.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign

@Composable
fun DesignGrid(
    designs: List<MehndiDesign>,
    favoriteIds: Set<String>,
    onDesignClick: (MehndiDesign) -> Unit,
    onFavoriteClick: (MehndiDesign) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    emptyMessage: String = "No Mehndi designs found.",
    emptyActionText: String? = null,
    onEmptyAction: (() -> Unit)? = null
) {
    if (designs.isEmpty()) {
        EmptyState(
            message = emptyMessage,
            actionText = emptyActionText,
            onActionClick = onEmptyAction,
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = designs,
                key = { it.id.ifEmpty { it.imageUrl.orEmpty() } }
            ) { design ->
                DesignCard(
                    design = design,
                    isFavorite = favoriteIds.contains(design.id),
                    onDesignClick = onDesignClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}
