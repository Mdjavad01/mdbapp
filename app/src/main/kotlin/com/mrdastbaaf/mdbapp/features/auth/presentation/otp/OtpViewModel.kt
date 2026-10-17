package com.mrdastbaaf.mdbapp.features.auth.presentation.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.ui.state.toUiMessage
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.core.util.PersonNameValidator
import com.mrdastbaaf.mdbapp.features.auth.domain.model.SignupName
import com.mrdastbaaf.mdbapp.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Step two of sign-in: the OTP, plus a name for a new account.
 *
 * Two behaviours here are driven directly by the server's contract:
 *
 *  - **Name fields appear only for a new account.** `POST /auth/request-otp`
 *    returns `isRegistered`, which tells us whether `verify-otp` will create a
 *    user. The server treats both name parts as *optional* and silently falls
 *    back to `هنرجو NNNN`, so the client is the only place that can insist — and
 *    it must, or every new account ends up unnamed.
 *
 *  - **The resend cooldown mirrors the server's.** The bridge rejects a second
 *    code for the same number within two minutes with `429 rate_limited`. Running
 *    the same countdown locally means the user sees a timer instead of an error.
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val phone: String = savedStateHandle.get<String>(ARG_PHONE).orEmpty()

    /** `"1"` when the account already exists, `"0"` when `verify-otp` will create it. */
    private val isRegistered: Boolean =
        savedStateHandle.get<String>(ARG_REGISTERED) == REGISTERED_TRUE

    private val _state = MutableStateFlow(
        OtpUiState(
            phone = phone,
            displayPhone = PersianFormat.toPersianDigits(phone),
            isNewAccount = !isRegistered,
            resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
        ),
    )
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private val _events = Channel<OtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var countdownJob: Job? = null

    init {
        startResendCountdown()
    }

    // ── Input ───────────────────────────────────────────────────────────────

    fun onCodeChange(raw: String) {
        // Accept Persian-Indic digits (a Persian keyboard produces them) and keep
        // only the digits, so pasting "کد: ۱۲۳۴۵۶" still works.
        val digitsOnly = PersianFormat.toAsciiDigits(raw).filter { it.isDigit() }.take(OTP_LENGTH)
        _state.update { it.copy(code = digitsOnly, errorMessage = null) }
    }

    fun onFirstNameChange(raw: String) {
        _state.update { it.copy(firstName = raw, errorMessage = null) }
    }

    fun onLastNameChange(raw: String) {
        _state.update { it.copy(lastName = raw, errorMessage = null) }
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting) return

        validate(current)?.let { message ->
            _state.update { it.copy(errorMessage = message) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }

            val name = if (current.isNewAccount) {
                SignupName(
                    firstName = PersonNameValidator.sanitize(current.firstName),
                    lastName = PersonNameValidator.sanitize(current.lastName),
                )
            } else {
                null
            }

            authRepository.verifyOtp(phone = phone, code = current.code, name = name)
                .onSuccess { profile ->
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(OtpEvent.SignedIn(profile.fullName))
                }
                .onFailure { throwable ->
                    val failure = throwable as? Failure ?: Failure.Unknown(cause = throwable)
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            // `invalid_otp` and `otp_locked` both arrive with the
                            // remaining-attempt count already in the message.
                            errorMessage = failure.toUiMessage(),
                        )
                    }
                }
        }
    }

    fun onResend() {
        val current = _state.value
        if (current.isResending || current.resendSecondsRemaining > 0) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true, errorMessage = null) }

            authRepository.requestOtp(phone)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isResending = false,
                            code = "",
                            smsWarning = result.smsWarning,
                            resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
                        )
                    }
                    startResendCountdown()
                }
                .onFailure { throwable ->
                    val failure = throwable as? Failure ?: Failure.Unknown(cause = throwable)
                    _state.update {
                        it.copy(
                            isResending = false,
                            errorMessage = failure.toUiMessage(),
                        )
                    }
                }
        }
    }

    /** The user chose a different number; the caller pops back to the login screen. */
    fun onChangePhone() {
        viewModelScope.launch { _events.send(OtpEvent.ChangePhone) }
    }

    // ── Internals ───────────────────────────────────────────────────────────

    /**
     * Client-side validation, mirroring what the server will enforce anyway.
     *
     * Returns the message to show, or `null` when the form is good to submit.
     */
    private fun validate(state: OtpUiState): UiMessage? = when {
        state.code.length != OTP_LENGTH -> UiMessage.Res(R.string.auth_otp_incomplete)

        state.isNewAccount && state.firstName.isBlank() && state.lastName.isBlank() ->
            UiMessage.Res(R.string.auth_name_required)

        state.isNewAccount && !PersonNameValidator.isAcceptable(state.firstName) ->
            UiMessage.Res(R.string.auth_name_latin)

        state.isNewAccount && !PersonNameValidator.isAcceptable(state.lastName) ->
            UiMessage.Res(R.string.auth_name_latin)

        state.isNewAccount && PersonNameValidator.isTooLong(state.firstName) ->
            UiMessage.Res(R.string.auth_name_too_long)

        state.isNewAccount && PersonNameValidator.isTooLong(state.lastName) ->
            UiMessage.Res(R.string.auth_name_too_long)

        else -> null
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.resendSecondsRemaining > 0) {
                delay(1_000L)
                _state.update {
                    it.copy(resendSecondsRemaining = (it.resendSecondsRemaining - 1).coerceAtLeast(0))
                }
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val ARG_PHONE = "phone"
        const val ARG_REGISTERED = "registered"
        const val REGISTERED_TRUE = "1"

        const val OTP_LENGTH = 6

        /** The bridge's per-phone cooldown in `handle_request_otp`. */
        private const val RESEND_COOLDOWN_SECONDS = 120
    }
}

data class OtpUiState(
    val phone: String = "",
    /** Persian-Indic rendering for display; the ASCII [phone] is what goes on the wire. */
    val displayPhone: String = "",
    val isNewAccount: Boolean = false,
    val code: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isSubmitting: Boolean = false,
    val isResending: Boolean = false,
    val resendSecondsRemaining: Int = 0,
    val errorMessage: UiMessage? = null,
    /** Present only when the server could not send the SMS at all. */
    val smsWarning: String? = null,
) {
    val canSubmit: Boolean get() = code.length == OtpViewModel.OTP_LENGTH && !isSubmitting
    val canResend: Boolean get() = resendSecondsRemaining == 0 && !isResending
}

sealed interface OtpEvent {
    data class SignedIn(val fullName: String) : OtpEvent
    data object ChangePhone : OtpEvent
}
