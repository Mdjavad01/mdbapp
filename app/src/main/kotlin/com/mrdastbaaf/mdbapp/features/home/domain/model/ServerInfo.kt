package com.mrdastbaaf.mdbapp.features.home.domain.model

/**
 * What the app knows about the backend it is talking to.
 *
 * A domain model, not the wire DTO: the DTO mirrors the endpoint's snake_case
 * keys and nullable fields, while this is the shape the UI actually reasons
 * about. Keeping them apart is what allows the endpoint to change without the
 * screen changing.
 */
data class ServerInfo(
    val pwaVersion: String,
    val pluginVersion: String,
    val databaseVersion: String,
    val serverTime: String,
    /** Server clock at the moment of the response, in epoch seconds. */
    val serverEpochSeconds: Long?,
) {
    /** True when the bridge answered with enough data to be considered healthy. */
    val isHealthy: Boolean get() = pluginVersion.isNotBlank()
}
