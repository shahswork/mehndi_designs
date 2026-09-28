package com.sashtech.mehndidesignsimple.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import com.sashtech.mehndidesignsimple.utils.NetworkObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val categories: List<MehndiCategory> = emptyList(),
    val featuredDesigns: List<MehndiDesign> = emptyList(),
    val popularDesigns: List<MehndiDesign> = emptyList(),
    val latestDesigns: List<MehndiDesign> = emptyList(),
    val stepTutorials: List<StepByStepTutorial> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: MehndiRepository
) : ViewModel() {

    private val isOfflineFlow = MutableStateFlow(false)
    private val isRefreshingFlow = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            repository.getCategories(),
            repository.getFeaturedDesigns(10),
            repository.getPopularDesigns(15)
        ) { cats, feat, pop -> Triple(cats, feat, pop) },
        combine(
            repository.getLatestDesigns(24),
            repository.getStepByStepTutorials(10),
            repository.allFavorites
        ) { latest, tuts, favs -> Triple(latest, tuts, favs) },
        combine(isOfflineFlow, isRefreshingFlow) { offline, refreshing -> Pair(offline, refreshing) }
    ) { group1, group2, status ->
        val (categories, featured, popular) = group1
        val (latest, tutorials, favorites) = group2
        val (offline, refreshing) = status
        HomeUiState(
            categories = categories,
            featuredDesigns = featured,
            popularDesigns = popular,
            latestDesigns = latest,
            stepTutorials = tutorials,
            favoriteIds = favorites.map { it.id }.toSet(),
            isLoading = false,
            isRefreshing = refreshing,
            isOffline = offline,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun refreshData() {
        if (isRefreshingFlow.value) return
        viewModelScope.launch {
            isRefreshingFlow.value = true
            try {
                repository.refreshAllData()
            } finally {
                isRefreshingFlow.value = false
            }
        }
    }

    fun monitorNetwork(context: Context) {
        viewModelScope.launch {
            NetworkObserver.observeNetwork(context).collect { online ->
                isOfflineFlow.value = !online
                if (online) {
                    // Trigger background refresh of categories when connection recovers
                    repository.refreshCategories()
                }
            }
        }
    }

    fun toggleFavorite(design: MehndiDesign) {
        viewModelScope.launch {
            repository.toggleFavorite(design)
        }
    }
}

