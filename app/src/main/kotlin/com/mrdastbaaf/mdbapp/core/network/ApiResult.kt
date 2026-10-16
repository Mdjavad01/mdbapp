package com.mrdastbaaf.mdbapp.core.network

import com.mrdastbaaf.mdbapp.core.error.ApiErrorDto
import com.mrdastbaaf.mdbapp.core.error.ApiException
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.error.toFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Runs a Retrofit call and folds every outcome into `Result<T>`.
 *
 * Repositories use this so that no `try/catch` and no HTTP status code leaks
 * upwards. `CancellationException` is deliberately re-thrown: swallowing it
 * would break structured concurrency and leave coroutines that never finish.
 */
suspend fun <T> safeApiCall(
    json: Json,
    block: suspend () -> T,
): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (http: HttpException) {
        Result.failure(http.toApiException(json))
    } catch (throwable: Throwable) {
        Result.failure(throwable.toFailure())
    }
}

/**
 * Extracts the bridge's error envelope from a failed response.
 *
 * If the body is missing or is not the expected JSON shape (a proxy error page,
 * for instance), the HTTP status alone drives the resulting [Failure] rather
 * than the parse failure masking it.
 */
private fun HttpException.toApiException(json: Json): ApiException {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val parsed = raw
        ?.takeIf { it.isNotBlank() }
        ?.let { body -> runCatching { json.decodeFromString<ApiErrorDto>(body) }.getOrNull() }

    return ApiException(
        httpStatus = code(),
        errorCode = parsed?.code,
        serverMessage = parsed?.message,
        cause = this,
    )
}

/** Unwraps the [Failure] carried by a failed `Result`, if there is one. */
fun Throwable.asFailure(): Failure = this as? Failure ?: toFailure()

/** True when the failure is an expired/absent session, so the UI must log out. */
fun Result<*>.isUnauthorized(): Boolean =
    exceptionOrNull().asFailureOrNull() is Failure.Unauthorized

/** True when the failure is purely connectivity, so retrying is meaningful. */
fun Result<*>.isNetworkFailure(): Boolean = exceptionOrNull().asFailureOrNull().let {
    it is Failure.NoConnection || it is Failure.Timeout
}

private fun Throwable?.asFailureOrNull(): Failure? = this?.asFailure()
