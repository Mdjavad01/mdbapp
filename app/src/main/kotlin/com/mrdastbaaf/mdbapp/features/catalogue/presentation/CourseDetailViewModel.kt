package com.mrdastbaaf.mdbapp.features.catalogue.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.CourseDetail
import com.mrdastbaaf.mdbapp.features.catalogue.domain.repository.CatalogueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One course and its lessons.
 *
 * The slug arrives as a navigation argument. It is the product `post_name`, not
 * a numeric id — `/courses/by-id/{id}` exists, but the slug is what the list
 * returns and what survives a rebuild of the product table.
 */
@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val slug: String = savedStateHandle.get<String>(ARG_SLUG).orEmpty()

    private val _state = MutableStateFlow<UiState<CourseDetail>>(UiState.Loading)
    val state: StateFlow<UiState<CourseDetail>> = _state.asStateFlow()

    val isOffline: StateFlow<Boolean> = connectivityObserver.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !connectivityObserver.currentlyOnline(),
        )

    init {
        refresh()
    }

    fun refresh() {
        if (slug.isBlank()) {
            _state.value = UiState.Error(Failure.NotFound())
            return
        }

        viewModelScope.launch {
            _state.value = UiState.Loading

            repository.getCourse(slug).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { throwable ->
                    _state.value = UiState.Error(
                        throwable as? Failure ?: Failure.Unknown(cause = throwable),
                    )
                },
            )
        }
    }

    companion object {
        /** Must match the `{slug}` placeholder in `Routes.COURSE_DETAIL`. */
        const val ARG_SLUG = "slug"
    }
}
