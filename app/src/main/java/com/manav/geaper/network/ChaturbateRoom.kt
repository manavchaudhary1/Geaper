package com.manav.geaper.network.model

import kotlinx.serialization.Serializable

@Serializable data class ChaturbateRoom(val username: String, val current_show: String? = null)
