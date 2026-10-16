package com.mrdastbaaf.mdbapp.core.ui.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A user-facing message that may come from either side of the app.
 *
 * The project rule is that every user-facing string lives in `strings.xml` and
 * none is hardcoded in Kotlin. But server errors are an exception that cannot be
 * avoided: the WordPress bridge returns high-quality Persian for every error
 * code it documents (`«مشاهده این جلسه نیازمند خرید یا تمدید اشتراک معتبر است.»`),
 * and that text is more accurate than anything the client could synthesise — so
 * it must be shown verbatim rather than replaced.
 *
 * This type lets both cases travel through the same `UiState` field without the
 * ViewModel needing a `Context`, and without leaking a raw server string into a
 * place that expects a resource id.
 */
sealed interface UiMessage {

    /** A string from `strings.xml`. The normal case. */
    data class Res(@StringRes val id: Int) : UiMessage

    /**
     * Text supplied at runtime — a server error message, or a formatted string
     * the ViewModel has already resolved.
     */
    data class Text(val value: String) : UiMessage

    /** `stringResource` for [Res], the literal for [Text]. */
    @Composable
    fun asString(): String = when (this) {
        is Res -> stringResource(id)
        is Text -> value
    }
}

/** Convenience for `state.errorMessage?.asString()`. */
@Composable
fun UiMessage?.asStringOrNull(): String? = this?.asString()
