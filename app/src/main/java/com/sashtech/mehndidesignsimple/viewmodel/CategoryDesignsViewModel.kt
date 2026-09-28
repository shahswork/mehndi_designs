package com.sashtech.mehndidesignsimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryDesignsUiState(
    val category: MehndiCategory? = null,
    val designs: List<MehndiDesign> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val selectedDifficultyFilter: String = "All",
    val isLoading: Boolean = true
)

class CategoryDesignsViewModel(
    private val categoryId: String,
    private val repository: MehndiRepository
) : ViewModel() {

    private val _difficultyFilter = MutableStateFlow("All")

    val uiState: StateFlow<CategoryDesignsUiState> = combine(
        repository.getCategoryById(categoryId),
        repository.getDesignsByCategory(categoryId),
        repository.getStepByStepTutorials(),
        repository.allFavorites,
        _difficultyFilter
    ) { category, designs, tutorials, favorites, filter ->
        val isStep = category?.isStepByStep == true || categoryId.equals("step_by_step", ignoreCase = true)
        val resolvedCategory = category ?: MehndiCategory(
            id = categoryId,
            name = MehndiCategory.formatCategoryName(categoryId),
            isStepByStep = isStep
        )

        val rawItems = if (isStep) {
            if (tutorials.isNotEmpty()) {
                tutorials.map { tut ->
                    MehndiDesign(
                        id = tut.id,
                        title = tut.title,
                        description = tut.description,
                        categoryId = tut.categoryId,
                        categoryName = resolvedCategory.name,
                        imageUrl = tut.coverImageUrl,
                        thumbnailUrl = tut.coverImageUrl,
                        mediumImageUrl = tut.coverImageUrl,
                        difficulty = tut.difficulty,
                        estimatedTime = tut.estimatedTime,
                        featured = tut.featured,
                        type = "step_by_step"
                    )
                }
            } else {
                designs
            }
        } else {
            designs
        }

        val filteredDesigns = if (filter.equals("All", ignoreCase = true)) {
            rawItems
        } else {
            rawItems.filter { MehndiDesign.matchesDifficulty(it.difficulty, filter) }
        }

        CategoryDesignsUiState(
            category = resolvedCategory,
            designs = filteredDesigns,
            favoriteIds = favorites.map { it.id }.toSet(),
            selectedDifficultyFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDesignsUiState(
            category = MehndiCategory(
                id = categoryId,
                name = MehndiCategory.formatCategoryName(categoryId),
                isStepByStep = categoryId.equals("step_by_step", ignoreCase = true)
            ),
            isLoading = true
        )
    )

    fun setDifficultyFilter(filter: String) {
        _difficultyFilter.value = filter
    }

    fun toggleFavorite(design: MehndiDesign) {
        viewModelScope.launch {
            repository.toggleFavorite(design)
        }
    }
}
