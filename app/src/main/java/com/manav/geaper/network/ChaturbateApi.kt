package com.manav.geaper.network

import com.manav.geaper.network.HttpClientProvider.client
import com.manav.geaper.network.model.ChaturbateRoom
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChaturbateApi {

  /**
   * @param wmToken The affiliate wm= query parameter (editable in Settings). Defaults to the
   *   original token so existing callers are unaffected.
   */
  suspend fun getOnlineRooms(wmToken: String = "jeQ1L"): List<ChaturbateRoom> =
    withContext(Dispatchers.IO) {
      client
        .get("https://chaturbate.com/affiliates/api/onlinerooms/?format=json&wm=$wmToken")
        .body()
    }
}
