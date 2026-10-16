package com.mrdastbaaf.mdbapp.features.auth.data

import com.mrdastbaaf.mdbapp.features.auth.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the signed-in user's profile.
 *
 * Deliberately **not persisted**. The profile is a cache, and the authoritative
 * copy is one `GET /auth/me` away — writing it to disk would mean a stale name
 * or an expired membership tier could survive a server-side change, and it would
 * put personal data in a file that has no need to hold it. The token is the only
 * thing this app persists, and it lives in the Keystore.
 *
 * On cold start the store is empty and the app re-fetches, which is also what
 * re-validates the session: if the token has expired, `GET /auth/me` returns 401
 * and the auth interceptor signs the user out.
 */
@Singleton
class CurrentUserStore @Inject constructor() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    fun set(profile: UserProfile?) {
        _profile.value = profile
    }

    fun clear() {
        _profile.value = null
    }
}
