package com.mrdastbaaf.mdbapp.features.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.features.account.domain.model.MembershipPlan
import com.mrdastbaaf.mdbapp.features.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Plans on their own, for the route the home and course screens link to. */
@HiltViewModel
class PlansViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {

    private val _plans = MutableStateFlow<UiState<List<MembershipPlan>>>(UiState.Loading)
    val plans: StateFlow<UiState<List<MembershipPlan>>> = _plans.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _plans.value = UiState.Loading
            _plans.value = repository.plans().fold(
                onSuccess = { plans ->
                    if (plans.isEmpty()) UiState.Empty() else UiState.Success(plans)
                },
                onFailure = { throwable ->
                    UiState.Error(throwable as? Failure ?: Failure.Unknown(cause = throwable))
                },
            )
        }
    }
}
