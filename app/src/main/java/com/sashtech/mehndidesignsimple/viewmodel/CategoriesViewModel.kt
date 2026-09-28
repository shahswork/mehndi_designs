package com.sashtech.mehndidesignsimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<MehndiCategory> = emptyList(),
    val stepTutorials: List<StepByStepTutorial> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class CategoriesViewModel(
    private val repository: MehndiRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.getCategories(),
        repository.getStepByStepTutorials(),
        _isRefreshing
    ) { categories, tutorials, isRefreshing ->
        CategoriesUiState(
            categories = categories,
            stepTutorials = tutorials,
            isLoading = false,
            isRefreshing = isRefreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true, isRefreshing = false)
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshCategories()
            } catch (_: Exception) {
            } finally {
                delay(400)
                _isRefreshing.value = false
            }
        }
    }
}
