package com.sashtech.mehndidesignsimple.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import com.sashtech.mehndidesignsimple.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DesignDetailUiState(
    val design: MehndiDesign? = null,
    val isFavorite: Boolean = false,
    val isDownloading: Boolean = false,
    val relatedDesigns: List<MehndiDesign> = emptyList(),
    val isLoading: Boolean = true
)

class DesignDetailViewModel(
    private val designId: String,
    private val repository: MehndiRepository
) : ViewModel() {

    private val _isDownloading = MutableStateFlow(false)

    val uiState: StateFlow<DesignDetailUiState> = combine(
        repository.getDesignById(designId),
        repository.isFavorite(designId),
        repository.getLatestDesigns(10),
        _isDownloading
    ) { design, isFav, allLatest, downloading ->
        val related = if (design != null) {
            allLatest.filter { 
                it.id != design.id && (it.category.equals(design.category, ignoreCase = true) || MehndiDesign.matchesDifficulty(it.difficulty, design.difficulty))
            }.take(6)
        } else emptyList()

        DesignDetailUiState(
            design = design,
            isFavorite = isFav,
            isDownloading = downloading,
            relatedDesigns = related,
            isLoading = design == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DesignDetailUiState(isLoading = true)
    )

    fun toggleFavorite() {
        val currentDesign = uiState.value.design ?: return
        viewModelScope.launch {
            repository.toggleFavorite(currentDesign)
        }
    }

    fun downloadImage(context: Context) {
        val currentDesign = uiState.value.design ?: return
        val url = currentDesign.getDownloadImageUrl()
        if (url.isBlank()) return

        viewModelScope.launch {
            _isDownloading.value = true
            ImageUtils.saveImageToGallery(context, url, currentDesign.title)
            _isDownloading.value = false
        }
    }

    fun shareDesign(context: Context) {
        val currentDesign = uiState.value.design ?: return
        ImageUtils.shareDesign(context, currentDesign.title, currentDesign.getDetailImageUrl())
    }
}
