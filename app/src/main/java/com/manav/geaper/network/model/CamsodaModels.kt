package com.manav.geaper.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CamsodaResponse(
    val stream: CamsodaStream? = null
)

@Serializable
data class CamsodaStream(
    val status: String? = null
)