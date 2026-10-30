package com.mrdastbaaf.mdbapp.features.appcontent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPage
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.Hero
import com.mrdastbaaf.mdbapp.features.appcontent.domain.repository.AppContentRepository
import com.mrdastbaaf.mdbapp.features.catalogue.domain.model.Course
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

data class HomeUiState(
    val hero: Hero?,
    val featuredTitle: String?,
    val featuredSubtitle: String?,
    val featured: List<Course>,
    val pages: List<AppPage>,
)

/**
 * The home screen: hero copy from the panel, the courses it features, and the
 * guide pages.
 *
 * The hero is the only part that can fail the screen — it is the reason the tab
 * exists. Pages and featured courses degrade to empty, because a home with a
 * hero and no guides is still a working home.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val content: AppContentRepository,
    private val catalogue: CatalogueRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeUiState>>(UiState.Loading)
    val state: StateFlow<UiState<HomeUiState>> = _state.asStateFlow()

    val isOffline: StateFlow<Boolean> = connectivityObserver.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !connectivityObserver.currentlyOnline(),
        )

    init {
        load(force = false)
    }

    /** Pull-to-refresh: the only path allowed to bypass the repository cache. */
    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        viewModelScope.launch {
            _state.value = UiState.Loading

            val config = content.home(force).getOrElse { throwable ->
                _state.value = UiState.Error(
                    throwable as? Failure ?: Failure.Unknown(cause = throwable),
                )
                return@launch
            }

            val pages = content.pages().getOrDefault(emptyList())
            val courses = catalogue.getCourses().getOrDefault(emptyList())

            // The panel's curated ids win. When the owner has not curated
            // anything the row falls back to the catalogue order, so the section
            // is never an empty strip.
            val featured = if (config.featuredCourseIds.isEmpty()) {
                courses.take(FALLBACK_FEATURED_COUNT)
            } else {
                config.featuredCourseIds.mapNotNull { id ->
                    courses.firstOrNull { it.productId == id }
                }
            }

            _state.value = UiState.Success(
                HomeUiState(
                    hero = config.hero,
                    featuredTitle = config.featuredTitle,
                    featuredSubtitle = config.featuredSubtitle,
                    featured = featured,
                    pages = pages,
                ),
            )
        }
    }

    private companion object {
        const val FALLBACK_FEATURED_COUNT = 6
    }
}
