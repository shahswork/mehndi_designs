package com.sashtech.mehndidesignsimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.repository.MehndiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<MehndiDesign> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val popularTags: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: MehndiRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearching = MutableStateFlow(false)

    val uiState: StateFlow<SearchUiState> = combine(
        combine(
            _query,
            _query.debounce(300).distinctUntilChanged().flatMapLatest { q ->
                _isSearching.value = q.isNotBlank()
                val flow = repository.searchDesigns(q)
                _isSearching.value = false
                flow
            },
            repository.allFavorites
        ) { currentQuery, searchResults, favorites ->
            Triple(currentQuery, searchResults, favorites)
        },
        combine(
            _isSearching,
            repository.getCategories()
        ) { searching, categories ->
            Pair(searching, categories.map { it.name }.filter { it.isNotBlank() })
        }
    ) { group1, group2 ->
        val (currentQuery, searchResults, favorites) = group1
        val (searching, categoriesTags) = group2
        SearchUiState(
            query = currentQuery,
            results = searchResults,
            favoriteIds = favorites.map { it.id }.toSet(),
            popularTags = categoriesTags,
            isSearching = searching,
            hasSearched = currentQuery.isNotBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun onTagSelected(tag: String) {
        _query.value = tag
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun toggleFavorite(design: MehndiDesign) {
        viewModelScope.launch {
            repository.toggleFavorite(design)
        }
    }
}
