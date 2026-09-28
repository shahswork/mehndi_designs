package com.sashtech.mehndidesignsimple.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.ui.components.DesignGrid
import com.sashtech.mehndidesignsimple.ui.components.MehndiTopBar
import com.sashtech.mehndidesignsimple.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onDesignClick: (MehndiDesign) -> Unit,
    onSearchClick: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = listOf("All", "Designs", "Tutorials")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MehndiTopBar(
            title = "My Favorites",
            subtitle = "Saved Henna Designs & Tutorials",
            showSearch = true,
            onSearchClick = onSearchClick
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filter ->
                val selected = uiState.selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setFilter(filter) },
                    label = {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        val designs = uiState.favorites.map { it.toMehndiDesign() }
        val favoriteIds = designs.map { it.id }.toSet()

        DesignGrid(
            designs = designs,
            favoriteIds = favoriteIds,
            onDesignClick = onDesignClick,
            onFavoriteClick = { viewModel.removeFavorite(it.id) },
            emptyMessage = "You haven't saved any designs yet.",
            emptyActionText = "Explore Designs",
            onEmptyAction = onExploreClick
        )
    }
}
