package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.local.MoodleConfigEntity
import com.example.data.local.UserEntity
import com.example.data.moodle.ChatSyncBundleJson
import com.example.data.moodle.MessagePayloadJson
import com.example.data.moodle.UcfMoodleClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
    private val database: AppDatabase,
    val moodleClient: UcfMoodleClient = UcfMoodleClient()
) {
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val moodleConfigDao = database.moodleConfigDao()

    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val allChatsFlow: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()
    val moodleConfigFlow: Flow<MoodleConfigEntity?> = moodleConfigDao.getConfigFlow()

    fun getMessagesFlow(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId)

    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> =
        chatDao.getChatByIdFlow(chatId)

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        // 1. Initialize Moodle configuration if absent
        val existingConfig = moodleConfigDao.getConfig()
        if (existingConfig == null) {
            val defaultConfig = MoodleConfigEntity(
                id = 1,
                host = "https://cursos.ucf.edu.cu/",
                username = "julianrene",
                password = "",
                repoId = 4,
                uploadType = "evidence",
                maxChunkBytes = 4 * 1024 * 1024L,
                lastConnectionStatus = "Listo para conectar"
            )
            moodleConfigDao.saveConfig(defaultConfig)
            moodleClient.configure(defaultConfig.host, defaultConfig.repoId)
        } else {
            moodleClient.configure(existingConfig.host, existingConfig.repoId)
        }

        // 2. Initialize current user if absent
        val currentUser = userDao.getCurrentUser()
        if (currentUser == null) {
            val defaultUser = UserEntity(
                username = "julianrene",
                displayName = "Julián René",
                bio = "Estudiante UCF | Ingeniería Informática",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                isCurrentUser = true
            )
            userDao.insertOrUpdate(defaultUser)
        }

        // 2b. Always ensure the Application Creator & Owner (@Eliel_21) is registered
        val elielAdmin = UserEntity(
            username = "Eliel_21",
            displayName = "Eliel",
            bio = "👑 Creador y Propietario de TeleUCF | Administrador Principal de la Aplicación",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            isCurrentUser = false
        )
        userDao.insertOrUpdate(elielAdmin)

        // 3. Initialize default contacts & chats if empty
        val existingChats = chatDao.getChatById("group_ucf_informatica")
        if (existingChats == null) {
            val defaultChats = listOf(
                ChatEntity(
                    id = "direct_Eliel_21",
                    title = "Eliel 👑",
                    type = "DIRECT",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    description = "👑 Propietario y Creador Oficial de la Aplicación (@Eliel_21)",
                    unreadCount = 1,
                    lastMessageSnippet = "¡Hola! Bienvenidos a TeleUCF. Ante cualquier duda o soporte estoy a su disposición.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 5,
                    pinned = true
                ),
                ChatEntity(
                    id = "group_ucf_informatica",
                    title = "Informática UCF Cienfuegos",
                    type = "GROUP",
                    avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
                    description = "Comunidad de Ingeniería en Ciencias Informáticas de la Universidad de Cienfuegos.",
                    unreadCount = 2,
                    lastMessageSnippet = "¡Compartí las diapositivas de Sistemas Distribuidos!",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 15,
                    pinned = true
                ),
                ChatEntity(
                    id = "group_archivos_moodle",
                    title = "Canal de Recursos y Archivos",
                    type = "GROUP",
                    avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                    description = "Canal de intercambio de recursos académicos, PDFs, apuntes y fotos.",
                    unreadCount = 0,
                    lastMessageSnippet = "Compendio de ejercicios de Programación Móvil compartido.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                    pinned = true
                ),
                ChatEntity(
                    id = "direct_carlos_profe",
                    title = "Prof. Carlos R. (Redes)",
                    type = "DIRECT",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    description = "Docente del Departamento de Computación UCF.",
                    unreadCount = 1,
                    lastMessageSnippet = "Por favor envíame el informe antes del viernes.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
                    pinned = false
                ),
                ChatEntity(
                    id = "direct_elena_ucf",
                    title = "Elena Gómez",
                    type = "DIRECT",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    description = "Estudiante 4to año Informática UCF.",
                    unreadCount = 0,
                    lastMessageSnippet = "Listo, ya revisé el archivo que enviaste.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                    pinned = false
                )
            )
            chatDao.insertAll(defaultChats)

            // Seed initial messages for Eliel (Owner) and group
            val sampleMessages = listOf(
                MessageEntity(
                    id = "msg_eliel_direct_01",
                    chatId = "direct_Eliel_21",
                    senderId = "Eliel_21",
                    senderName = "Eliel",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                    text = "¡Hola! Te doy la bienvenida a TeleUCF. Soy @Eliel_21, creador y propietario de la aplicación. Cualquier duda o sugerencia puedes escribirme por aquí.",
                    isOutgoing = false,
                    status = "DELIVERED"
                ),
                MessageEntity(
                    id = "msg_000_eliel_group",
                    chatId = "group_ucf_informatica",
                    senderId = "Eliel_21",
                    senderName = "Eliel",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 6,
                    text = "¡Hola a todos! Bienvenidos a TeleUCF. Recuerden cumplir las normas de respeto en el grupo. Como creador y administrador estaré acompañándolos.",
                    isOutgoing = false,
                    status = "DELIVERED"
                ),
                MessageEntity(
                    id = "msg_001",
                    chatId = "group_ucf_informatica",
                    senderId = "carlos_profe",
                    senderName = "Prof. Carlos R.",
                    senderAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 4,
                    text = "Buenas tardes a todos los estudiantes de la UCF. Bienvenidos al espacio de intercambio.",
                    isOutgoing = false,
                    status = "DELIVERED"
                ),
                MessageEntity(
                    id = "msg_002",
                    chatId = "group_ucf_informatica",
                    senderId = "elena_ucf",
                    senderName = "Elena Gómez",
                    senderAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                    text = "Aquí les comparto el resumen de la clase en PDF.",
                    attachmentUrl = "https://cursos.ucf.edu.cu/draftfile.php/4/user/draft/928371/Resumen_Tema_3.pdf",
                    attachmentName = "Resumen_Tema_3.pdf",
                    attachmentType = "DOCUMENT",
                    attachmentSize = 1845200L, // 1.76 MB
                    isOutgoing = false,
                    status = "DELIVERED"
                ),
                MessageEntity(
                    id = "msg_003",
                    chatId = "group_ucf_informatica",
                    senderId = "julianrene",
                    senderName = "Julián René",
                    senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                    text = "¡Subí las diapositivas de Sistemas Distribuidos!",
                    attachmentUrl = "https://cursos.ucf.edu.cu/draftfile.php/4/user/draft/928372/Diapositivas_UCF.jpg",
                    attachmentName = "Diapositivas_UCF.jpg",
                    attachmentType = "IMAGE",
                    attachmentSize = 2450000L, // 2.33 MB
                    isOutgoing = true,
                    status = "SENT"
                )
            )
            messageDao.insertAll(sampleMessages)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        attachmentBytes: ByteArray? = null,
        attachmentName: String? = null,
        attachmentType: String? = null,
        mimeType: String = "application/octet-stream",
        onUploadProgress: ((Long, Long) -> Unit)? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val currentUser = userDao.getCurrentUser() ?: UserEntity(
            username = "julianrene",
            displayName = "Julián René"
        )

        val messageId = "msg_${UUID.randomUUID()}"
        var uploadedUrl: String? = null
        val attachmentSize = attachmentBytes?.size?.toLong() ?: 0L

        // If there's an attachment, upload to UCF Moodle under 4MB limit
        if (attachmentBytes != null && !attachmentName.isNullOrEmpty()) {
            if (attachmentSize > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                val sizeMb = String.format("%.2f", attachmentSize / (1024.0 * 1024.0))
                return@withContext Result.failure(
                    IllegalArgumentException("El archivo pesa $sizeMb MB. cursos.ucf.edu.cu tiene límite de 4MB.")
                )
            }

            val uploadResult = moodleClient.uploadFile(
                fileBytes = attachmentBytes,
                fileName = attachmentName,
                mimeType = mimeType,
                onProgress = { sent, total ->
                    onUploadProgress?.invoke(sent, total)
                }
            )

            if (uploadResult.isFailure) {
                return@withContext Result.failure(
                    uploadResult.exceptionOrNull() ?: Exception("Fallo al subir a UCF Moodle")
                )
            }

            uploadedUrl = uploadResult.getOrNull()?.url
        }

        val message = MessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = currentUser.username,
            senderName = currentUser.displayName,
            senderAvatar = currentUser.avatarUrl,
            timestamp = System.currentTimeMillis(),
            text = text,
            attachmentUrl = uploadedUrl,
            attachmentName = attachmentName,
            attachmentType = attachmentType,
            attachmentSize = attachmentSize,
            isOutgoing = true,
            status = "SENT"
        )

        messageDao.insertOrUpdate(message)

        val snippet = when {
            text.isNotBlank() -> text
            attachmentType == "IMAGE" -> "📷 Foto"
            attachmentType == "DOCUMENT" -> "📄 ${attachmentName ?: "Archivo"}"
            else -> "Adjunto"
        }
        chatDao.updateLastMessage(chatId, snippet, message.timestamp)

        Result.success(message)
    }

    suspend fun createGroup(title: String, description: String, avatarUrl: String = ""): ChatEntity =
        withContext(Dispatchers.IO) {
            val id = "group_${UUID.randomUUID().toString().take(8)}"
            val chat = ChatEntity(
                id = id,
                title = title,
                type = "GROUP",
                avatarUrl = avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150" },
                description = description,
                unreadCount = 0,
                lastMessageSnippet = "Grupo creado",
                lastMessageTime = System.currentTimeMillis(),
                pinned = false
            )
            chatDao.insertOrUpdate(chat)

            val welcomeMsg = MessageEntity(
                id = "msg_${UUID.randomUUID()}",
                chatId = id,
                senderId = "system",
                senderName = "Sistema",
                timestamp = System.currentTimeMillis(),
                text = "¡Grupo \"$title\" creado con éxito! Bienvenidos a la conversación.",
                isOutgoing = false,
                status = "DELIVERED"
            )
            messageDao.insertOrUpdate(welcomeMsg)

            chat
        }

    suspend fun checkUsernameAvailability(username: String, currentUserId: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@").lowercase()
            if (clean.isBlank()) return@withContext false
            // The owner handle @Eliel_21 is permanently reserved
            if (clean == "eliel_21" && currentUserId?.lowercase() != "eliel_21") {
                return@withContext false
            }
            val existing = userDao.getUserByUsername(clean)
            if (existing != null && existing.username.lowercase() != currentUserId?.lowercase()) {
                return@withContext false
            }
            true
        }

    suspend fun updateCurrentUserProfile(
        newUsername: String,
        displayName: String,
        bio: String,
        avatarUrl: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanUsername = newUsername.trim().removePrefix("@")
        val currentUser = userDao.getCurrentUser() ?: UserEntity(
            username = "julianrene",
            displayName = displayName,
            isCurrentUser = true
        )

        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de usuario no puede estar vacío."))
        }

        // Validate uniqueness if changing username
        if (!cleanUsername.equals(currentUser.username, ignoreCase = true)) {
            if (cleanUsername.equals("Eliel_21", ignoreCase = true)) {
                return@withContext Result.failure(
                    IllegalArgumentException("El usuario @Eliel_21 es exclusivo del Administrador y Propietario de la aplicación.")
                )
            }
            val existing = userDao.getUserByUsername(cleanUsername)
            if (existing != null) {
                return@withContext Result.failure(
                    IllegalArgumentException("El nombre de usuario @$cleanUsername ya está registrado. Por favor elige otro.")
                )
            }
        }

        val updated = currentUser.copy(
            username = cleanUsername,
            displayName = displayName.ifBlank { cleanUsername },
            bio = bio,
            avatarUrl = avatarUrl,
            isCurrentUser = true
        )
        userDao.insertOrUpdate(updated)
        Result.success(updated)
    }

    suspend fun createDirectChat(username: String, displayName: String, bio: String = ""): ChatEntity =
        withContext(Dispatchers.IO) {
            val clean = username.trim().removePrefix("@")
            val existingUser = userDao.getUserByUsername(clean)
            val user = existingUser ?: UserEntity(
                username = clean,
                displayName = displayName.ifBlank { "@$clean" },
                bio = bio,
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            )
            userDao.insertOrUpdate(user)

            val chatId = "direct_${clean}"
            val existingChat = chatDao.getChatById(chatId)
            if (existingChat != null) {
                return@withContext existingChat
            }

            val chat = ChatEntity(
                id = chatId,
                title = user.displayName,
                type = "DIRECT",
                avatarUrl = user.avatarUrl,
                description = user.bio,
                unreadCount = 0,
                lastMessageSnippet = "Chat iniciado",
                lastMessageTime = System.currentTimeMillis(),
                pinned = false
            )
            chatDao.insertOrUpdate(chat)
            chat
        }

    suspend fun updateCurrentUser(displayName: String, bio: String, avatarUrl: String) =
        withContext(Dispatchers.IO) {
            val current = userDao.getCurrentUser() ?: UserEntity(
                username = "julianrene",
                displayName = displayName,
                isCurrentUser = true
            )
            val updated = current.copy(
                displayName = displayName,
                bio = bio,
                avatarUrl = avatarUrl
            )
            userDao.insertOrUpdate(updated)
        }

    suspend fun saveMoodleConfig(config: MoodleConfigEntity): Unit = withContext(Dispatchers.IO) {
        moodleConfigDao.saveConfig(config)
        moodleClient.configure(config.host, config.repoId)
    }

    suspend fun testMoodleLogin(username: String, pass: String): Result<String> =
        withContext(Dispatchers.IO) {
            val ok = moodleClient.login(username, pass)
            val current = moodleConfigDao.getConfig() ?: MoodleConfigEntity()
            val status = if (ok) "Conectado" else "Error al conectar"
            moodleConfigDao.saveConfig(
                current.copy(
                    username = username,
                    password = pass,
                    lastConnectionStatus = status,
                    lastConnectionTime = System.currentTimeMillis()
                )
            )
            if (ok) Result.success("Conectado con éxito") else Result.failure(Exception("Error de conexión"))
        }

    /**
     * Exports a chat bundle as JSON ("Johnson") string.
     */
    suspend fun exportChatJson(chatId: String): String = withContext(Dispatchers.IO) {
        val chat = chatDao.getChatById(chatId) ?: return@withContext "{}"
        val messages = messageDao.getRecentMessages(chatId, 200).reversed()
        val jsonBundle = ChatSyncBundleJson(
            chatId = chat.id,
            title = chat.title,
            type = chat.type,
            avatarUrl = chat.avatarUrl,
            description = chat.description,
            updatedAt = System.currentTimeMillis(),
            messages = messages.map {
                MessagePayloadJson(
                    id = it.id,
                    chatId = it.chatId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderAvatar = it.senderAvatar,
                    timestamp = it.timestamp,
                    text = it.text,
                    attachmentUrl = it.attachmentUrl,
                    attachmentName = it.attachmentName,
                    attachmentType = it.attachmentType,
                    attachmentSize = it.attachmentSize
                )
            }
        )
        jsonBundle.toJson().toString(2)
    }

    /**
     * Imports a chat bundle from JSON ("Johnson") string.
     */
    suspend fun importChatJson(jsonString: String): Result<ChatEntity> = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject(jsonString)
            val bundle = ChatSyncBundleJson.fromJson(jsonObject)

            if (bundle.chatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("JSON inválido: no contiene chatId."))
            }

            val chat = ChatEntity(
                id = bundle.chatId,
                title = bundle.title,
                type = bundle.type,
                avatarUrl = bundle.avatarUrl,
                description = bundle.description,
                unreadCount = 0,
                lastMessageSnippet = bundle.messages.lastOrNull()?.text ?: "Chat importado",
                lastMessageTime = bundle.updatedAt,
                pinned = false
            )
            chatDao.insertOrUpdate(chat)

            val currentUserId = userDao.getCurrentUser()?.username ?: "julianrene"
            val messageEntities = bundle.messages.map {
                MessageEntity(
                    id = it.id.ifBlank { "msg_${UUID.randomUUID()}" },
                    chatId = bundle.chatId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderAvatar = it.senderAvatar,
                    timestamp = it.timestamp,
                    text = it.text,
                    attachmentUrl = it.attachmentUrl,
                    attachmentName = it.attachmentName,
                    attachmentType = it.attachmentType,
                    attachmentSize = it.attachmentSize,
                    isOutgoing = it.senderId == currentUserId,
                    status = "DELIVERED"
                )
            }
            messageDao.insertAll(messageEntities)

            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads the chat JSON directly to UCF Moodle and returns the public link.
     */
    suspend fun syncChatToMoodle(chatId: String): Result<String> = withContext(Dispatchers.IO) {
        val jsonStr = exportChatJson(chatId)
        val fileName = "teleucf_${chatId}_sync.json"
        moodleClient.uploadJsonSync(fileName, jsonStr)
    }

    /**
     * Downloads and imports chat JSON directly from a UCF Moodle URL.
     */
    suspend fun syncChatFromMoodleUrl(url: String): Result<ChatEntity> = withContext(Dispatchers.IO) {
        val downloadRes = moodleClient.fetchJsonFromUrl(url)
        if (downloadRes.isFailure) {
            return@withContext Result.failure(
                downloadRes.exceptionOrNull() ?: Exception("No se pudo descargar el JSON de Moodle")
            )
        }
        val jsonStr = downloadRes.getOrNull() ?: ""
        importChatJson(jsonStr)
    }
}
