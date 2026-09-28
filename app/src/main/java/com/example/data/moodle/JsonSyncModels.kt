package com.example.data.moodle

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data structures for exchanging information via JSON ("Johnson")
 * stored and retrieved through UCF Moodle (cursos.ucf.edu.cu).
 */
data class UserProfileJson(
    val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String = "",
    val lastSeen: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("username", username)
        put("displayName", displayName)
        put("bio", bio)
        put("avatarUrl", avatarUrl)
        put("lastSeen", lastSeen)
    }

    companion object {
        fun fromJson(json: JSONObject): UserProfileJson = UserProfileJson(
            username = json.optString("username", ""),
            displayName = json.optString("displayName", "Usuario"),
            bio = json.optString("bio", ""),
            avatarUrl = json.optString("avatarUrl", ""),
            lastSeen = json.optLong("lastSeen", System.currentTimeMillis())
        )
    }
}

data class MessagePayloadJson(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val text: String = "",
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null, // "IMAGE", "DOCUMENT", "AUDIO", "OTHER"
    val attachmentSize: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("chatId", chatId)
        put("senderId", senderId)
        put("senderName", senderName)
        put("senderAvatar", senderAvatar)
        put("timestamp", timestamp)
        put("text", text)
        if (attachmentUrl != null) put("attachmentUrl", attachmentUrl)
        if (attachmentName != null) put("attachmentName", attachmentName)
        if (attachmentType != null) put("attachmentType", attachmentType)
        put("attachmentSize", attachmentSize)
    }

    companion object {
        fun fromJson(json: JSONObject): MessagePayloadJson = MessagePayloadJson(
            id = json.optString("id", ""),
            chatId = json.optString("chatId", ""),
            senderId = json.optString("senderId", ""),
            senderName = json.optString("senderName", "Usuario"),
            senderAvatar = json.optString("senderAvatar", ""),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            text = json.optString("text", ""),
            attachmentUrl = if (json.has("attachmentUrl")) json.getString("attachmentUrl") else null,
            attachmentName = if (json.has("attachmentName")) json.getString("attachmentName") else null,
            attachmentType = if (json.has("attachmentType")) json.getString("attachmentType") else null,
            attachmentSize = json.optLong("attachmentSize", 0L)
        )
    }
}

data class ChatSyncBundleJson(
    val chatId: String,
    val title: String,
    val type: String, // "DIRECT" or "GROUP"
    val avatarUrl: String = "",
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<MessagePayloadJson> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("chatId", chatId)
        put("title", title)
        put("type", type)
        put("avatarUrl", avatarUrl)
        put("description", description)
        put("updatedAt", updatedAt)
        val arr = JSONArray()
        messages.forEach { arr.put(it.toJson()) }
        put("messages", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): ChatSyncBundleJson {
            val list = mutableListOf<MessagePayloadJson>()
            val arr = json.optJSONArray("messages")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)
                    if (item != null) {
                        list.add(MessagePayloadJson.fromJson(item))
                    }
                }
            }
            return ChatSyncBundleJson(
                chatId = json.optString("chatId", ""),
                title = json.optString("title", "Chat"),
                type = json.optString("type", "DIRECT"),
                avatarUrl = json.optString("avatarUrl", ""),
                description = json.optString("description", ""),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                messages = list
            )
        }
    }
}
