package com.mrdastbaaf.mdbapp.features.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCatalog
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

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val repository: ShopRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ShopCatalog>>(UiState.Loading)
    val state: StateFlow<UiState<ShopCatalog>> = _state.asStateFlow()

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

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        viewModelScope.launch {
            _state.value = UiState.Loading

            _state.value = repository.catalog(force).fold(
                onSuccess = { catalog ->
                    if (catalog.isEmpty) UiState.Empty() else UiState.Success(catalog)
                },
                onFailure = { throwable ->
                    UiState.Error(throwable as? Failure ?: Failure.Unknown(cause = throwable))
                },
            )
        }
    }
}
