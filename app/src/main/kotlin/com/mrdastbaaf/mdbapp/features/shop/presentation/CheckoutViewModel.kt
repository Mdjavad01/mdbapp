package com.mrdastbaaf.mdbapp.features.shop.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.shop.domain.model.CheckoutSession
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShippingDetails
import com.mrdastbaaf.mdbapp.features.shop.domain.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/** What the gateway told us, after the app has confirmed it with the server. */
sealed interface PaymentOutcome {
    data class Success(val message: String?) : PaymentOutcome
    data class Failed(val message: String?) : PaymentOutcome
}

data class CheckoutUiState(
    val product: ShopProduct? = null,
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val postalCode: String = "",
    val error: UiMessage? = null,
    val isSubmitting: Boolean = false,
    /** Non-null while the gateway page is open. */
    val session: CheckoutSession? = null,
    val isVerifying: Boolean = false,
    val outcome: PaymentOutcome? = null,
)

/**
 * The checkout: a shipping form, an order, and the gateway.
 *
 * The payment itself happens in a WebView on Zibal's own page — the app never
 * touches card data. When the gateway redirects to the bridge's callback URL the
 * WebView is torn down and the order is settled here, using the `cb_token` that
 * only that redirect carries.
 */
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShopRepository,
) : ViewModel() {

    private val productId: Int = savedStateHandle.get<Int>(ARG_PRODUCT_ID) ?: 0

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(product = repository.product(productId)) }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(fullName = value, error = null) }

    fun onPhoneChange(value: String) =
        _state.update { it.copy(phone = PersianFormat.toAsciiDigits(value), error = null) }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value, error = null) }

    fun onPostalChange(value: String) = _state.update {
        it.copy(postalCode = PersianFormat.toAsciiDigits(value).filter { c -> c.isDigit() }, error = null)
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.product == null) return

        validate(current)?.let { message ->
            _state.update { it.copy(error = message) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }

            val shipping = ShippingDetails(
                fullName = current.fullName.trim(),
                phone = current.phone.trim(),
                address = current.address.trim(),
                postalCode = current.postalCode.trim(),
            )

            repository.createOrder(productId, shipping).fold(
                onSuccess = { session ->
                    _state.update { it.copy(isSubmitting = false, session = session) }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            error = (throwable as? Failure)?.userMessage
                                ?.let { message -> UiMessage.Text(message) },
                        )
                    }
                },
            )
        }
    }

    /** Called by the WebView when it reaches the bridge's callback URL. */
    fun onPaymentCallback(url: String) {
        val params = queryParams(url)
        val cbToken = params["cb_token"].orEmpty()
        val trackId = params["trackId"]?.toLongOrNull()
            ?: params["track_id"]?.toLongOrNull()
            ?: _state.value.session?.trackId
        val success = params["success"] == "1" || params["success"].equals("true", true)

        if (trackId == null || cbToken.isBlank()) {
            _state.update {
                it.copy(session = null, outcome = PaymentOutcome.Failed(null))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(session = null, isVerifying = true) }

            repository.verifyPayment(trackId, success, cbToken).fold(
                onSuccess = { message ->
                    _state.update {
                        it.copy(isVerifying = false, outcome = PaymentOutcome.Success(message))
                    }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isVerifying = false,
                            outcome = PaymentOutcome.Failed(
                                (throwable as? Failure)?.userMessage,
                            ),
                        )
                    }
                },
            )
        }
    }

    /** The user closed the gateway without paying. */
    fun cancelPayment() {
        _state.update { it.copy(session = null) }
    }

    fun dismissOutcome() {
        _state.update { it.copy(outcome = null) }
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private fun validate(state: CheckoutUiState): UiMessage? = when {
        state.fullName.trim().length < 3 ->
            UiMessage.Res(R.string.checkout_invalid_name)

        !PHONE.matches(state.phone.trim()) ->
            UiMessage.Res(R.string.checkout_invalid_phone)

        state.address.trim().length < 10 ->
            UiMessage.Res(R.string.checkout_invalid_address)

        !POSTAL.matches(state.postalCode.trim()) ->
            UiMessage.Res(R.string.checkout_invalid_postal)

        else -> null
    }

    /**
     * The callback URL is `…#/payment/callback?orderId=…&cb_token=…&trackId=…`,
     * so the parameters live after the fragment and `Uri.getQueryParameter`
     * cannot see them. Parsing the substring by hand is the reliable option.
     */
    private fun queryParams(url: String): Map<String, String> =
        url.substringAfter('?', "")
            .split('&')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    val key = part.substring(0, separator)
                    val value = runCatching {
                        URLDecoder.decode(part.substring(separator + 1), "UTF-8")
                    }.getOrDefault(part.substring(separator + 1))
                    key to value
                }
            }
            .toMap()

    companion object {
        const val ARG_PRODUCT_ID = "productId"

        private val PHONE = Regex("^09\\d{9}$")
        private val POSTAL = Regex("^\\d{10}$")
    }
}
