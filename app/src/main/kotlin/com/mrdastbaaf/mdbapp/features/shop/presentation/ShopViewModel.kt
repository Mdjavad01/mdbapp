package com.mrdastbaaf.mdbapp.features.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopNode
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopPage
import com.mrdastbaaf.mdbapp.features.shop.domain.model.buildShopMenu
import com.mrdastbaaf.mdbapp.features.shop.domain.repository.ShopRepository
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
 * The shop, as a menu and then a section.
 *
 * Two levels of state, deliberately in one ViewModel: the menu (a tree with no
 * products) and the page of whichever node the user has opened. Keeping the
 * path here rather than in navigation means the shop never pushes a destination
 * over the tab — the bottom bar stays put — and going back is a list operation.
 *
 * The shop has 1800 products, so products are never all in memory: a page is
 * 24 rows and [loadMore] appends the next one.
 */
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val repository: ShopRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _menu = MutableStateFlow<UiState<List<ShopNode>>>(UiState.Loading)
    val menu: StateFlow<UiState<List<ShopNode>>> = _menu.asStateFlow()

    /** The open path, root first. Empty means the menu is showing. */
    private val _path = MutableStateFlow<List<ShopNode>>(emptyList())
    val path: StateFlow<List<ShopNode>> = _path.asStateFlow()

    private val _page = MutableStateFlow<UiState<ShopPage>>(UiState.Loading)
    val page: StateFlow<UiState<ShopPage>> = _page.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    val isOffline: StateFlow<Boolean> = connectivityObserver.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !connectivityObserver.currentlyOnline(),
        )

    /** The page number the next [loadMore] should ask for. */
    private var nextPage = 1

    init {
        loadMenu(force = false)
    }

    fun retry() {
        val current = _path.value.lastOrNull()
        if (current == null) loadMenu(force = true) else loadPage(current.category.id, 1, reset = true)
    }

    /** Opens a section. Pushing a group shows its children; pushing a leaf shows its products. */
    fun open(node: ShopNode) {
        _path.value = _path.value + node
        loadPage(node.category.id, 1, reset = true)
    }

    /** One level up. Called by the app bar and by the system back gesture. */
    fun back() {
        val current = _path.value
        if (current.isEmpty()) return

        val next = current.dropLast(1)
        _path.value = next
        val target = next.lastOrNull()
        if (target == null) {
            _page.value = UiState.Loading
        } else {
            loadPage(target.category.id, 1, reset = true)
        }
    }

    /** Jumps to an ancestor from the breadcrumb. */
    fun jumpTo(index: Int) {
        val current = _path.value
        if (index !in current.indices || index == current.lastIndex) return

        val next = current.take(index + 1)
        _path.value = next
        loadPage(next.last().category.id, 1, reset = true)
    }

    fun loadMore() {
        val current = _path.value.lastOrNull() ?: return
        if (_loadingMore.value) return

        val loaded = (_page.value as? UiState.Success)?.data ?: return
        if (!loaded.hasMore) return

        loadPage(current.category.id, nextPage, reset = false)
    }

    private fun loadMenu(force: Boolean) {
        viewModelScope.launch {
            _menu.value = UiState.Loading

            _menu.value = repository.categories(force).fold(
                onSuccess = { categories ->
                    val roots = buildShopMenu(categories)
                    if (roots.isEmpty()) UiState.Empty() else UiState.Success(roots)
                },
                onFailure = { throwable ->
                    UiState.Error(throwable as? Failure ?: Failure.Unknown(cause = throwable))
                },
            )
        }
    }

    private fun loadPage(categoryId: Int, page: Int, reset: Boolean) {
        viewModelScope.launch {
            if (reset) {
                _page.value = UiState.Loading
                nextPage = 1
            } else {
                _loadingMore.value = true
            }

            repository.page(categoryId, page).fold(
                onSuccess = { fresh ->
                    nextPage = fresh.page + 1
                    _page.value = if (!reset) {
                        val existing = (_page.value as? UiState.Success)?.data
                        if (existing == null) {
                            UiState.Success(fresh)
                        } else {
                            UiState.Success(fresh.copy(products = existing.products + fresh.products))
                        }
                    } else if (fresh.products.isEmpty()) {
                        UiState.Empty()
                    } else {
                        UiState.Success(fresh)
                    }
                },
                onFailure = { throwable ->
                    // A failed "more" must not throw away the page already on
                    // screen; only a first page can turn the screen into an error.
                    if (reset) {
                        _page.value = UiState.Error(
                            throwable as? Failure ?: Failure.Unknown(cause = throwable),
                        )
                    }
                },
            )

            _loadingMore.value = false
        }
    }
}
