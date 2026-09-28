package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String = "",
    val isCurrentUser: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "DIRECT" or "GROUP"
    val avatarUrl: String = "",
    val description: String = "",
    val unreadCount: Int = 0,
    val lastMessageSnippet: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val cloudMoodleUrl: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val text: String = "",
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null, // "IMAGE", "DOCUMENT", "AUDIO", "OTHER"
    val attachmentSize: Long = 0L, // Size in bytes (<= 4194304 bytes / 4MB)
    val isOutgoing: Boolean = true,
    val status: String = "SENT" // "PENDING", "SENT", "DELIVERED", "FAILED"
)

@Entity(tableName = "moodle_config")
data class MoodleConfigEntity(
    @PrimaryKey val id: Int = 1,
    val host: String = "https://cursos.ucf.edu.cu/",
    val username: String = "",
    val password: String = "",
    val repoId: Int = 4, // Default repo id for UCF Moodle
    val uploadType: String = "evidence", // "evidence" or "draft"
    val maxChunkBytes: Long = 4 * 1024 * 1024L, // 4MB limit exactly as in repo
    val sesskey: String? = null,
    val token: String? = null,
    val lastConnectionStatus: String? = "Desconectado",
    val lastConnectionTime: Long = 0L
)
