package com.mrdastbaaf.mdbapp.core.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * The error envelope the WordPress bridge returns for every `WP_Error`:
 *
 * ```json
 * { "code": "forbidden", "message": "مشاهده این جلسه نیازمند …", "data": { "status": 403 } }
 * ```
 *
 * All fields are nullable: an error body can be absent entirely (a gateway 502
 * with an HTML body, for instance), and a parse failure must never mask the
 * original HTTP status.
 */
@Serializable
data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null,
    val data: ApiErrorDataDto? = null,
)

@Serializable
data class ApiErrorDataDto(
    @SerialName("status") val status: Int? = null,
)

/**
 * A non-2xx response, carrying everything needed to build a [Failure].
 *
 * Thrown by [com.mrdastbaaf.mdbapp.core.network.safeApiCall] and converted to a
 * [Failure] there, so no layer above ever sees an HTTP status code.
 */
class ApiException(
    val httpStatus: Int,
    val errorCode: String?,
    val serverMessage: String?,
    cause: Throwable? = null,
) : Exception(serverMessage ?: "HTTP $httpStatus", cause) {

    /**
     * Maps the response onto the closed [Failure] set.
     *
     * `serverMessage` is carried through untouched: the bridge returns
     * human-quality Persian for every documented error code, and that text is
     * more accurate than anything the client could synthesise.
     */
    fun toFailure(): Failure = when (httpStatus) {
        400 -> Failure.Validation(userMessage = serverMessage, code = errorCode)
        401 -> Failure.Unauthorized(userMessage = serverMessage)
        403 -> Failure.Forbidden(userMessage = serverMessage, code = errorCode)
        404 -> Failure.NotFound(userMessage = serverMessage)
        409 -> Failure.Conflict(userMessage = serverMessage, code = errorCode)
        422 -> Failure.Validation(userMessage = serverMessage, code = errorCode)
        429 -> Failure.RateLimited(userMessage = serverMessage)
        in 500..599 -> Failure.Server(userMessage = serverMessage, httpStatus = httpStatus)
        else -> Failure.Server(userMessage = serverMessage, httpStatus = httpStatus)
    }
}

/**
 * Converts a transport-level throwable into a [Failure].
 *
 * Order matters: [SocketTimeoutException] and [UnknownHostException] are both
 * [IOException]s, so the specific cases must come first.
 */
fun Throwable.toFailure(): Failure = when (this) {
    is ApiException -> toFailure()
    is SocketTimeoutException -> Failure.Timeout()
    is UnknownHostException -> Failure.NoConnection()
    is SSLException -> Failure.NoConnection()
    is IOException -> Failure.NoConnection()
    is HttpException -> Failure.Server(httpStatus = code())
    else -> Failure.Unknown(cause = this)
}
