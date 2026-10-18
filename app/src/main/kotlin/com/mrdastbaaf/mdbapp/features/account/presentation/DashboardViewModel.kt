package com.mrdastbaaf.mdbapp.features.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.account.domain.model.Order
import com.mrdastbaaf.mdbapp.features.account.domain.repository.AccountRepository
import com.mrdastbaaf.mdbapp.features.auth.data.CurrentUserStore
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the account tab: who is signed in, what the plans are, what they bought.
 *
 * Plans and orders load independently — a failing orders call must not blank the
 * plan list, because the plans are the part a user without a subscription came
 * for.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AccountRepository,
    currentUserStore: CurrentUserStore,
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = currentUserStore.profile

    private val _plans = MutableStateFlow<UiState<List<MembershipPlan>>>(UiState.Loading)
    val plans: StateFlow<UiState<List<MembershipPlan>>> = _plans.asStateFlow()

    private val _orders = MutableStateFlow<UiState<List<Order>>>(UiState.Loading)
    val orders: StateFlow<UiState<List<Order>>> = _orders.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadPlans()
        loadOrders()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _plans.value = UiState.Loading
            _plans.value = repository.plans().fold(
                onSuccess = { plans ->
                    if (plans.isEmpty()) UiState.Empty() else UiState.Success(plans)
                },
                onFailure = { throwable -> throwable.toState() },
            )
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _orders.value = UiState.Loading
            _orders.value = repository.orders().fold(
                onSuccess = { orders ->
                    if (orders.isEmpty()) UiState.Empty() else UiState.Success(orders)
                },
                onFailure = { throwable -> throwable.toState() },
            )
        }
    }

    private fun Throwable.toState(): UiState<Nothing> =
        UiState.Error(this as? Failure ?: Failure.Unknown(cause = this))
}
