package com.mrdastbaaf.mdbapp.features.appcontent.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPageContent
import com.mrdastbaaf.mdbapp.features.appcontent.domain.repository.AppContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One guide page, fetched on open. */
@HiltViewModel
class PageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppContentRepository,
) : ViewModel() {

    private val pageId: String = savedStateHandle.get<String>(ARG_PAGE_ID).orEmpty()

    private val _state = MutableStateFlow<UiState<AppPageContent>>(UiState.Loading)
    val state: StateFlow<UiState<AppPageContent>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (pageId.isBlank()) {
            _state.value = UiState.Error(Failure.NotFound())
            return
        }

        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.pageContent(pageId).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { throwable ->
                    UiState.Error(throwable as? Failure ?: Failure.Unknown(cause = throwable))
                },
            )
        }
    }

    companion object {
        /** Must match the `{pageId}` placeholder in `Routes.PAGE`. */
        const val ARG_PAGE_ID = "pageId"
    }
}
