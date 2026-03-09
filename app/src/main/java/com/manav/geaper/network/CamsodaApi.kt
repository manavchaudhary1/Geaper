package com.manav.geaper.network

import com.manav.geaper.network.model.CamsodaResponse
import io.ktor.client.call.*
import io.ktor.client.request.*

class CamsodaApi {

    suspend fun getStatus(username: String): String {

        val url = "https://www.camsoda.com/api/v1/chat/react/$username"

        val response: CamsodaResponse =
            HttpClientProvider.client.get(url).body()

        return response.stream?.status ?: "offline"
    }
}