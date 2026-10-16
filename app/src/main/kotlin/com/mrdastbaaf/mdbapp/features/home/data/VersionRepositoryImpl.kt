package com.mrdastbaaf.mdbapp.features.home.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.dto.VersionDto
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.features.home.domain.model.ServerInfo
import com.mrdastbaaf.mdbapp.features.home.domain.repository.VersionRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to `GET /version`.
 *
 * The mapping from [VersionDto] to [ServerInfo] is where the endpoint's
 * nullable snake_case fields become a non-null domain object. Missing values
 * degrade to `"—"` rather than throwing: a version probe is diagnostic, and
 * failing the whole call because one field is absent would be the wrong
 * trade-off.
 */
@Singleton
class VersionRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
) : VersionRepository {

    override suspend fun fetchServerInfo(): Result<ServerInfo> {
        val result = safeApiCall(json) { api.getVersion() }

        return result.mapCatching { dto -> dto.toDomain() }
    }

    private fun VersionDto.toDomain(): ServerInfo {
        val plugin = pluginVersion.orEmpty()
        if (plugin.isBlank() && pwaVersion.isNullOrBlank()) {
            // A 200 with no version fields means we reached something that is not
            // the bridge — a cached error page, or the WAF answering for us.
            throw Failure.InvalidResponse()
        }

        return ServerInfo(
            pwaVersion = pwaVersion.orEmpty().ifBlank { UNKNOWN },
            pluginVersion = plugin.ifBlank { UNKNOWN },
            databaseVersion = dbVersion.orEmpty().ifBlank { UNKNOWN },
            serverTime = serverTime.orEmpty().ifBlank { UNKNOWN },
            serverEpochSeconds = timestamp,
        )
    }

    private companion object {
        const val UNKNOWN = "—"
    }
}
