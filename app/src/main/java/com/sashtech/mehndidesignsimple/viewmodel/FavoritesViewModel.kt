package com.sashtech.mehndidesignsimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.local.FavoriteDesignEntity
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<FavoriteDesignEntity> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = false
)

class FavoritesViewModel(
    private val repository: MehndiRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("All")

    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.allFavorites,
        _selectedFilter
    ) { allFavs, filter ->
        val filtered = when (filter) {
            "Tutorials" -> allFavs.filter { it.isTutorial }
            "Designs" -> allFavs.filter { !it.isTutorial }
            else -> allFavs
        }
        FavoritesUiState(
            favorites = filtered,
            selectedFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState(isLoading = true)
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun removeFavorite(designId: String) {
        viewModelScope.launch {
            repository.removeFavorite(designId)
        }
    }
}
