package com.mrdastbaaf.mdbapp.features.home.domain.repository

import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo

/**
 * Reads the backend's identity/version information.
 *
 * An interface rather than a concrete class so the ViewModel can be tested
 * against a fake without a network stack — the unit tests the brief asks for
 * depend on this boundary existing from the start rather than being retrofitted.
 */
interface VersionRepository {
    suspend fun fetchServerInfo(): Result<ServerInfo>
}
