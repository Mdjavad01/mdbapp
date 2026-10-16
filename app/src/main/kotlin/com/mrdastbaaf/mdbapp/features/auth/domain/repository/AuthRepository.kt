package com.mrdastbaaf.mdbapp.features.auth.domain.repository

import com.mrdastbaaf.mdbapp.features.auth.domain.model.OtpRequestResult
import com.mrdastbaaf.mdbapp.features.auth.domain.model.SignupName
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile

/**
 * The auth surface the app depends on.
 *
 * Everything returns `Result<T>` carrying a
 * [com.mrdastbaaf.mdbapp.core.error.Failure], so no caller has to know about
 * HTTP status codes, the `WP_Error` envelope, or Retrofit.
 */
interface AuthRepository {

    /**
     * Requests an OTP for [phone], which must already be in canonical
     * `09XXXXXXXXX` form — normalisation is the caller's job, because only the
     * caller knows what the user typed.
     */
    suspend fun requestOtp(phone: String): Result<OtpRequestResult>

    /**
     * Verifies the code and, on success, **stores the token**.
     *
     * Storing the credential is part of this call rather than a separate step:
     * there is a window between receiving a token and persisting it during which
     * a process death would lose the session, and the repository is the only
     * place that can close it.
     *
     * [name] is required only for a new account; pass `null` for an existing one.
     */
    suspend fun verifyOtp(
        phone: String,
        code: String,
        name: SignupName?,
    ): Result<UserProfile>

    /** Re-reads the profile. Used on cold start and after returning to the foreground. */
    suspend fun fetchProfile(): Result<UserProfile>

    /**
     * Revokes the token server-side and clears it locally.
     *
     * Always succeeds from the caller's point of view: a network failure during
     * logout must still leave the device signed out, or the user would be stuck
     * in a session they explicitly ended.
     */
    suspend fun logout(): Result<Unit>
}
