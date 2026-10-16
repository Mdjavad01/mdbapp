package com.mrdastbaaf.mdbapp.features.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes for the auth endpoints.
 *
 * Every field is nullable and defaulted. Two reasons, both learned from reading
 * the plugin source:
 *
 *  1. The bridge returns different key sets in different situations —
 *     `smsWarning` appears only when the SMS provider is unconfigured, and
 *     `membership` is `null` for a user with no subscription.
 *  2. A non-null field would turn a server-side addition or omission into a
 *     crash, which is exactly the failure mode the app must avoid on a live
 *     backend it does not control.
 */

// ── POST /auth/request-otp ──────────────────────────────────────────────────

@Serializable
data class RequestOtpRequest(
    /** Canonical `09XXXXXXXXX`. Normalised by `PhoneNormalizer` before it gets here. */
    val phone: String,
)

@Serializable
data class RequestOtpResponse(
    val success: Boolean? = null,
    val message: String? = null,
    /** ⚠️ The only snake_case key in the whole API, sitting beside camelCase ones. */
    @SerialName("expires_in_seconds") val expiresInSeconds: Int? = null,
    val isRegistered: Boolean? = null,
    val smsWarning: String? = null,
)

// ── POST /auth/verify-otp ───────────────────────────────────────────────────

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    @SerialName("otp_code") val otpCode: String,
    /**
     * Omitted (`null`) for an existing account. `explicitNulls = false` in the
     * Json config means these keys are absent from the body rather than sent as
     * `null`, which matters because the plugin reads them with `get_param()`.
     */
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
)

@Serializable
data class VerifyOtpResponse(
    val success: Boolean? = null,
    /** Opaque `{user_id}.{secret}`. Valid for 30 days. There is no refresh. */
    val token: String? = null,
    val user: UserProfileDto? = null,
)

// ── GET /auth/me ────────────────────────────────────────────────────────────

@Serializable
data class UserProfileDto(
    val id: String? = null,
    val phoneNumber: String? = null,
    val maskedPhone: String? = null,
    val fullName: String? = null,
    val role: String? = null,
    val membership: MembershipDto? = null,
)

@Serializable
data class MembershipDto(
    val tier: String? = null,
    val planId: String? = null,
    val planName: String? = null,
    val status: String? = null,
    /** MySQL local time, not ISO 8601. */
    val startedAt: String? = null,
    val expiresAt: String? = null,
    val formattedExpireDateJalali: String? = null,
    val daysRemaining: Int? = null,
)

// ── POST /auth/logout ───────────────────────────────────────────────────────

@Serializable
data class LogoutResponse(
    val success: Boolean? = null,
    val message: String? = null,
)
