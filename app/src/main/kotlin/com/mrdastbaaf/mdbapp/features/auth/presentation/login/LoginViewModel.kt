package com.mrdastbaaf.mdbapp.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.ui.state.toUiMessage
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.core.util.PhoneNormalizer
import com.mrdastbaaf.mdbapp.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phone-number entry.
 *
 * Holds the **raw** input rather than a normalised value. A Persian keyboard
 * produces `۰۹۱۲…` and rewriting the field under the user's cursor as they type
 * is worse than accepting it and normalising on submit — which is exactly what
 * [PhoneNormalizer] exists for. The button's enabled state is driven by the
 * normalised result, so an unusable number can never be submitted.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPhoneChange(raw: String) {
        // Cap the length so a paste of something enormous cannot be typed into
        // the field; the real validation happens on submit.
        val capped = raw.take(MAX_INPUT_LENGTH)
        _state.update { it.copy(phoneInput = capped, errorMessage = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting) return

        val normalized = PhoneNormalizer.normalize(current.phoneInput)
        if (normalized == null) {
            _state.update { it.copy(errorMessage = UiMessage.Res(R.string.auth_phone_invalid)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }

            authRepository.requestOtp(normalized)
                .onSuccess { result ->
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(
                        LoginEvent.OtpRequested(
                            phone = normalized,
                            isRegistered = result.isRegistered,
                            message = result.message,
                            smsWarning = result.smsWarning,
                        ),
                    )
                }
                .onFailure { throwable ->
                    val failure = throwable as? Failure ?: Failure.Unknown(throwable)
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            // The bridge's Persian rate-limit and role messages are
                            // far more specific than anything we could synthesise,
                            // so they are shown verbatim.
                            errorMessage = failure.toUiMessage(),
                        )
                    }
                }
        }
    }

    /** Clears the error once the user starts correcting the input. */
    fun onErrorShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 20
    }
}

data class LoginUiState(
    val phoneInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: UiMessage? = null,
) {
    /** Drives the submit button's enabled state. */
    val canSubmit: Boolean get() = PhoneNormalizer.isValid(phoneInput) && !isSubmitting

    /** The number as it will be displayed on the next screen, with Persian digits. */
    val displayPhone: String get() = PersianFormat.toPersianDigits(phoneInput)
}

sealed interface LoginEvent {
    data class OtpRequested(
        val phone: String,
        val isRegistered: Boolean,
        val message: String?,
        val smsWarning: String?,
    ) : LoginEvent
}
