package com.sashtech.mehndidesignsimple.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.data.model.TutorialStep
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import com.sashtech.mehndidesignsimple.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StepByStepUiState(
    val tutorial: StepByStepTutorial? = null,
    val steps: List<TutorialStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isDownloading: Boolean = false,
    val isLoading: Boolean = true
)

class StepByStepViewModel(
    private val tutorialId: String,
    private val repository: MehndiRepository
) : ViewModel() {

    private val _currentStepIndex = MutableStateFlow(0)
    private val _isDownloading = MutableStateFlow(false)

    val uiState: StateFlow<StepByStepUiState> = combine(
        repository.getTutorialById(tutorialId),
        repository.getTutorialSteps(tutorialId),
        repository.isFavorite(tutorialId),
        _currentStepIndex,
        _isDownloading
    ) { tutorial, steps, isFav, stepIndex, downloading ->
        StepByStepUiState(
            tutorial = tutorial,
            steps = steps,
            currentStepIndex = stepIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0)),
            isFavorite = isFav,
            isDownloading = downloading,
            isLoading = tutorial == null && steps.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StepByStepUiState(isLoading = true)
    )

    fun onStepChanged(index: Int) {
        _currentStepIndex.value = index
    }

    fun toggleFavorite() {
        val tutorial = uiState.value.tutorial ?: return
        val pseudoDesign = MehndiDesign(
            id = tutorial.id,
            title = tutorial.title,
            description = tutorial.description,
            categoryId = "step_by_step",
            categoryName = "Step-by-Step Mehndi",
            imageUrl = tutorial.coverImageUrl,
            thumbnailUrl = tutorial.coverImageUrl,
            mediumImageUrl = tutorial.coverImageUrl,
            difficulty = tutorial.difficulty,
            estimatedTime = tutorial.estimatedTime,
            type = "step_by_step"
        )
        viewModelScope.launch {
            repository.toggleFavorite(pseudoDesign)
        }
    }

    fun downloadCurrentStep(context: Context) {
        val state = uiState.value
        val steps = state.steps
        val index = state.currentStepIndex
        if (steps.isEmpty() || index !in steps.indices) return
        val currentStep = steps[index]
        val tutorialTitle = state.tutorial?.title ?: "Tutorial"

        viewModelScope.launch {
            _isDownloading.value = true
            ImageUtils.saveImageToGallery(
                context,
                currentStep.getDisplayImageUrl(),
                "${tutorialTitle}_Step_${currentStep.stepNumber}"
            )
            _isDownloading.value = false
        }
    }

    fun shareTutorial(context: Context) {
        val tutorial = uiState.value.tutorial ?: return
        ImageUtils.shareDesign(context, tutorial.title, tutorial.coverImageUrl)
    }
}
