package com.sashtech.mehndidesignsimple.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import com.sashtech.mehndidesignsimple.utils.NetworkObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val repository: MehndiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StartupState>(StartupState.Loading("Checking saved designs..."))
    val uiState: StateFlow<StartupState> = _uiState.asStateFlow()

    private var isInitialized = false

    fun initializeApp(context: Context) {
        if (isInitialized && _uiState.value is StartupState.HomeReady) return
        isInitialized = true

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = StartupState.Loading("Checking saved designs...")

            val isOnline = NetworkObserver.isOnline(context)
            val hasCache = repository.hasCachedHomeData()

            if (isOnline) {
                _uiState.value = StartupState.Loading("Loading inspiration...")

                // Preload Firebase data with a strict timeout (max 3.5 seconds)
                val success = repository.preloadInitialHomeData(context, timeoutMs = 3500L)

                if (success || repository.hasCachedHomeData()) {
                    _uiState.value = StartupState.HomeReady
                } else {
                    // Remote failed or returned empty and no cache exists
                    _uiState.value = StartupState.NoInternetNoCache(
                        "Unable to load designs. Please check your connection and tap Retry."
                    )
                }
            } else {
                // Device is offline
                if (hasCache) {
                    // Previously synchronized Firebase data exists locally
                    _uiState.value = StartupState.OfflineCached
                } else {
                    // First launch + No internet + No cache
                    _uiState.value = StartupState.NoInternetNoCache(
                        "Connect to the internet to load the latest designs."
                    )
                }
            }
        }
    }

    fun retry(context: Context) {
        isInitialized = false
        initializeApp(context)
    }
}
