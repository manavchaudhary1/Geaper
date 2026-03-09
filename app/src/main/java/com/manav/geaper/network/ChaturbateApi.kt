package com.manav.geaper.network

import com.manav.geaper.network.HttpClientProvider.client
import com.manav.geaper.network.model.ChaturbateRoom
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChaturbateApi {

    suspend fun getOnlineRooms(): List<ChaturbateRoom> =
        withContext(Dispatchers.IO) {
            client.get(
                "https://chaturbate.com/affiliates/api/onlinerooms/?format=json&wm=jeQ1L"
            ).body()
        }
}