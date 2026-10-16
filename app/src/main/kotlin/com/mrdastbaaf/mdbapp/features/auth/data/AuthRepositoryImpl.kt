package com.mrdastbaaf.mdbapp.features.auth.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.util.PersonNameValidator
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.MembershipDto
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.RequestOtpRequest
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.UserProfileDto
import com.mrdastbaaf.mdbapp.features.auth.data.remote.dto.VerifyOtpRequest
import com.mrdastbaaf.mdbapp.features.auth.domain.model.Membership
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipStatus
import com.mrdastbaaf.mdbapp.features.auth.domain.model.MembershipTier
import com.mrdastbaaf.mdbapp.features.auth.domain.model.OtpRequestResult
import com.mrdastbaaf.mdbapp.features.auth.domain.model.SignupName
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserRole
import com.mrdastbaaf.mdbapp.features.auth.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth against the WordPress bridge.
 *
 * The DTO → domain mapping lives here and nowhere else. Everything nullable on
 * the wire becomes a definite value in the domain, so no screen ever has to ask
 * "could `fullName` be null?".
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
    private val sessionManager: SessionManager,
    private val currentUserStore: CurrentUserStore,
) : AuthRepository {

    override suspend fun requestOtp(phone: String): Result<OtpRequestResult> =
        safeApiCall(json) { api.requestOtp(RequestOtpRequest(phone = phone)) }
            .mapCatching { dto ->
                OtpRequestResult(
                    // Absent `isRegistered` is treated as "new user": asking for a
                    // name unnecessarily is a minor annoyance, whereas skipping it
                    // for a new account leaves the user named "هنرجو 1234".
                    isRegistered = dto.isRegistered == true,
                    expiresInSeconds = dto.expiresInSeconds ?: DEFAULT_OTP_TTL_SECONDS,
                    message = dto.message,
                    smsWarning = dto.smsWarning,
                )
            }

    override suspend fun verifyOtp(
        phone: String,
        code: String,
        name: SignupName?,
    ): Result<UserProfile> {
        val result = safeApiCall(json) {
            api.verifyOtp(
                VerifyOtpRequest(
                    phone = phone,
                    otpCode = code,
                    // Sanitised with the server's own rule so what the user sees
                    // is what the account stores. `null` for an existing account
                    // keeps the keys out of the body entirely.
                    firstName = name?.firstName?.let(PersonNameValidator::sanitize)?.takeIf { it.isNotEmpty() },
                    lastName = name?.lastName?.let(PersonNameValidator::sanitize)?.takeIf { it.isNotEmpty() },
                ),
            )
        }

        return result.mapCatching { dto ->
            val token = dto.token
                ?.takeIf { it.isNotBlank() }
                ?: throw Failure.InvalidResponse("پاسخ سرور بدون توکن ورود بود.")

            // Validate the whole payload before persisting anything. If the
            // profile is missing the call has failed, and storing the token first
            // would leave the app "signed in" with no identity to show — a state
            // the UI has no sensible way to render.
            val profile = dto.user?.toDomain()
                ?: throw Failure.InvalidResponse("پاسخ سرور بدون اطلاعات کاربر بود.")

            // Persist before returning: a process death between here and the
            // caller would otherwise lose a session the server already issued.
            sessionManager.onSignedIn(token)
            currentUserStore.set(profile)

            profile
        }
    }

    override suspend fun fetchProfile(): Result<UserProfile> =
        safeApiCall(json) { api.getMe() }
            .mapCatching { dto ->
                dto.toDomain().also(currentUserStore::set)
            }

    override suspend fun logout(): Result<Unit> {
        // Best effort: revoke server-side, but never let a network failure keep
        // the user signed in on a device they asked to sign out of.
        val remote = safeApiCall(json) { api.logout() }

        sessionManager.onSignedOut()
        currentUserStore.clear()

        return remote.map { }
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun UserProfileDto.toDomain(): UserProfile {
        val id = id?.takeIf { it.isNotBlank() }
            ?: throw Failure.InvalidResponse("پاسخ سرور بدون شناسهٔ کاربر بود.")

        return UserProfile(
            id = id,
            phoneNumber = phoneNumber.orEmpty(),
            // The server always sends a mask; falling back to the raw number
            // would leak it into the UI, so an absent mask degrades to the raw
            // value only because there is nothing safer to show.
            maskedPhone = maskedPhone?.takeIf { it.isNotBlank() } ?: phoneNumber.orEmpty(),
            fullName = fullName.orEmpty().ifBlank { "هنرجو" },
            role = UserRole.from(role),
            membership = membership?.toDomain(),
        )
    }

    private fun MembershipDto.toDomain(): Membership? {
        val tier = MembershipTier.from(tier)
        // A membership with no tier is not a membership — the bridge omits the
        // whole object in that case, so reaching here means something is wrong.
        if (tier == MembershipTier.FREE) return null

        return Membership(
            tier = tier,
            planId = planId.orEmpty(),
            planName = planName,
            status = MembershipStatus.from(status),
            startedAt = startedAt.orEmpty(),
            expiresAt = expiresAt.orEmpty(),
            formattedExpireDateJalali = formattedExpireDateJalali.orEmpty(),
            daysRemaining = daysRemaining?.coerceAtLeast(0) ?: 0,
        )
    }

    private companion object {
        /** The bridge's `CODE_TTL_SECONDS`. */
        const val DEFAULT_OTP_TTL_SECONDS = 120
    }
}
