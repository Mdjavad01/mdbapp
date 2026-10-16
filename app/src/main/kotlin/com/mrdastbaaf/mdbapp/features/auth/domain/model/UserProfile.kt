package com.mrdastbaaf.mdbapp.features.auth.domain.model

/**
 * The signed-in user.
 *
 * Mirrors `format_user_profile()` in `class-auth-controller.php` — which is also
 * the shape embedded in the `verify-otp` response and returned by `GET /auth/me`.
 *
 * Note what is **absent**: there is no `completedLessonIds`, no `bookmarkedLessonIds`
 * and no `createdAt`. Those exist only in the web client's TypeScript type as
 * client-side defaults. Progress is a separate call to `GET /me/progress`.
 */
data class UserProfile(
    val id: String,
    /** Raw phone number. Only ever displayed masked — see [maskedPhone]. */
    val phoneNumber: String,
    /** Server-provided mask, e.g. `0912***6789`. Prefer this over a local mask. */
    val maskedPhone: String,
    val fullName: String,
    val role: UserRole,
    val membership: Membership?,
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN

    val hasActiveMembership: Boolean
        get() = membership?.status == MembershipStatus.ACTIVE
}

/**
 * The bridge classifies every account as `admin` or `student`; there is no
 * instructor role in the API. Any unrecognised value degrades to [STUDENT],
 * which is the safe default — it grants nothing.
 */
enum class UserRole {
    STUDENT,
    ADMIN,
    ;

    companion object {
        fun from(raw: String?): UserRole = when (raw?.lowercase()) {
            "admin" -> ADMIN
            else -> STUDENT
        }
    }
}

/**
 * Membership tier. The server ranks these `bronze(1) < silver(2) < gold(3)`;
 * `free` is the absence of a membership rather than a stored tier, but it is
 * included because the client's own types use it.
 */
enum class MembershipTier {
    FREE,
    BRONZE,
    SILVER,
    GOLD,
    ;

    /** Matches `tier_rank()` in `class-membership-helper.php`. */
    val rank: Int
        get() = when (this) {
            FREE -> 0
            BRONZE -> 1
            SILVER -> 2
            GOLD -> 3
        }

    companion object {
        fun from(raw: String?): MembershipTier = when (raw?.lowercase()) {
            "bronze" -> BRONZE
            "silver" -> SILVER
            "gold" -> GOLD
            else -> FREE
        }
    }
}

enum class MembershipStatus {
    ACTIVE,
    EXPIRED,
    PENDING,
    ;

    companion object {
        fun from(raw: String?): MembershipStatus = when (raw?.lowercase()) {
            "active" -> ACTIVE
            "pending" -> PENDING
            else -> EXPIRED
        }
    }
}

/**
 * The active membership.
 *
 * [startedAt] and [expiresAt] are **MySQL local time** strings
 * (`YYYY-MM-DD HH:MM:SS`, no timezone), not ISO 8601 — the bridge uses
 * `current_time('mysql')`. Parsing them as ISO would silently shift every date.
 * Prefer [formattedExpireDateJalali] for display; the server has already done
 * the Jalali conversion and the timezone handling.
 */
data class Membership(
    val tier: MembershipTier,
    val planId: String,
    val planName: String?,
    val status: MembershipStatus,
    val startedAt: String,
    val expiresAt: String,
    val formattedExpireDateJalali: String,
    val daysRemaining: Int,
)

/**
 * The outcome of `POST /auth/request-otp`.
 *
 * [isRegistered] is the reason this endpoint returns more than an acknowledgement:
 * it tells the client whether to render "ورود" or "ثبتنام", and therefore
 * whether to ask for a name on the next screen.
 */
data class OtpRequestResult(
    val isRegistered: Boolean,
    val expiresInSeconds: Int,
    val message: String?,
    /**
     * Present only when the SMS provider is not configured on the server. The
     * code is still valid — it just was not delivered — so this is a warning to
     * surface, not an error to fail on.
     */
    val smsWarning: String?,
)

/** Name captured at signup. Both parts are optional; the server accepts either alone. */
data class SignupName(
    val firstName: String,
    val lastName: String,
) {
    val isEmpty: Boolean get() = firstName.isBlank() && lastName.isBlank()
}
