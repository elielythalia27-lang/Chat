package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.MessageEntity
import com.example.data.local.MoodleConfigEntity
import com.example.data.local.UserEntity
import com.example.data.moodle.UcfMoodleClient
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

sealed class Screen {
    object ChatList : Screen()
    data class ChatDetail(val chatId: String) : Screen()
}

data class UploadProgressState(
    val isUploading: Boolean = false,
    val fileName: String = "",
    val bytesSent: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = ChatRepository(database)

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Active Chat Selection
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Upload Progress
    private val _uploadProgress = MutableStateFlow(UploadProgressState())
    val uploadProgress: StateFlow<UploadProgressState> = _uploadProgress.asStateFlow()

    // UI Snackbars / Alerts
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // Search query in chat list
    val searchQuery = MutableStateFlow("")

    // Tab filter: 0 = Todos, 1 = Privados, 2 = Grupos
    val selectedTab = MutableStateFlow(0)

    // Current user
    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Moodle Config
    val moodleConfig: StateFlow<MoodleConfigEntity?> = repository.moodleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered chats list
    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        repository.allChatsFlow,
        searchQuery,
        selectedTab
    ) { chats, query, tab ->
        chats.filter { chat ->
            val matchesQuery = query.isBlank() ||
                    chat.title.contains(query, ignoreCase = true) ||
                    chat.lastMessageSnippet.contains(query, ignoreCase = true)

            val matchesTab = when (tab) {
                1 -> chat.type == "DIRECT"
                2 -> chat.type == "GROUP"
                else -> true
            }

            matchesQuery && matchesTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Entity
    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    // Active Messages
    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.ChatDetail) {
            _activeChatId.value = screen.chatId
            observeChat(screen.chatId)
        }
    }

    fun navigateBack() {
        val current = _currentScreen.value
        if (current !is Screen.ChatList) {
            _currentScreen.value = Screen.ChatList
            _activeChatId.value = null
        }
    }

    private fun observeChat(chatId: String) {
        viewModelScope.launch {
            repository.getChatByIdFlow(chatId).collect {
                _activeChat.value = it
            }
        }
        viewModelScope.launch {
            repository.getMessagesFlow(chatId).collect {
                _activeMessages.value = it
            }
        }
    }

    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            val res = repository.sendMessage(chatId = chatId, text = text)
            if (res.isFailure) {
                _snackbarEvent.emit("Error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun sendAttachment(uri: Uri, isImage: Boolean) {
        val chatId = _activeChatId.value ?: return
        val context = getApplication<Application>()

        viewModelScope.launch {
            try {
                var fileName = "adjunto_${System.currentTimeMillis()}"
                var fileSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                // Check 4MB limit before reading
                if (fileSize > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                    val sizeMb = String.format("%.2f", fileSize / (1024.0 * 1024.0))
                    _snackbarEvent.emit("El archivo pesa $sizeMb MB. El límite máximo es de 4.0 MB.")
                    return@launch
                }

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.use { it.readBytes() } ?: return@launch

                if (bytes.size > UcfMoodleClient.MAX_FILE_SIZE_BYTES) {
                    val sizeMb = String.format("%.2f", bytes.size / (1024.0 * 1024.0))
                    _snackbarEvent.emit("El archivo supera el límite de 4.0 MB ($sizeMb MB).")
                    return@launch
                }

                val mimeType = context.contentResolver.getType(uri) ?: if (isImage) "image/jpeg" else "application/octet-stream"
                val attachmentType = if (isImage || mimeType.startsWith("image")) "IMAGE" else "DOCUMENT"

                _uploadProgress.value = UploadProgressState(
                    isUploading = true,
                    fileName = fileName,
                    bytesSent = 0,
                    totalBytes = bytes.size.toLong(),
                    percentage = 0
                )

                val result = repository.sendMessage(
                    chatId = chatId,
                    text = "",
                    attachmentBytes = bytes,
                    attachmentName = fileName,
                    attachmentType = attachmentType,
                    mimeType = mimeType,
                    onUploadProgress = { sent, total ->
                        val pct = if (total > 0) ((sent * 100) / total).toInt() else 0
                        _uploadProgress.value = UploadProgressState(
                            isUploading = true,
                            fileName = fileName,
                            bytesSent = sent,
                            totalBytes = total,
                            percentage = pct
                        )
                    }
                )

                _uploadProgress.value = UploadProgressState(isUploading = false)

                if (result.isSuccess) {
                    _snackbarEvent.emit("✓ Archivo enviado con éxito")
                } else {
                    _snackbarEvent.emit("Error: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uploadProgress.value = UploadProgressState(isUploading = false)
                _snackbarEvent.emit("Error al procesar archivo: ${e.message}")
            }
        }
    }

    fun isAppOwner(username: String?): Boolean {
        if (username == null) return false
        val clean = username.trim().removePrefix("@")
        return clean.equals("Eliel_21", ignoreCase = true)
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val current = currentUser.value?.username
        return repository.checkUsernameAvailability(username, current)
    }

    fun updateProfileWithUsername(
        newUsername: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateCurrentUserProfile(newUsername, displayName, bio, avatarUrl)
            if (result.isSuccess) {
                _snackbarEvent.emit("Perfil actualizado correctamente")
                onComplete(true, "Perfil actualizado")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al actualizar perfil"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun createGroup(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val group = repository.createGroup(title, description)
            _snackbarEvent.emit("Grupo \"${group.title}\" creado")
            navigateTo(Screen.ChatDetail(group.id))
        }
    }

    fun createDirectChat(username: String, displayName: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            val chat = repository.createDirectChat(username, displayName)
            _snackbarEvent.emit("Chat con ${chat.title} iniciado")
            navigateTo(Screen.ChatDetail(chat.id))
        }
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.updateCurrentUser(displayName, bio, avatarUrl)
            _snackbarEvent.emit("Perfil actualizado")
        }
    }

    fun testMoodleLogin(username: String, pass: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.testMoodleLogin(username, pass)
            if (res.isSuccess) {
                val msg = res.getOrNull() ?: "Conectado"
                _snackbarEvent.emit(msg)
                onComplete(true, msg)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Fallo de conexión"
                _snackbarEvent.emit(err)
                onComplete(false, err)
            }
        }
    }

    fun saveMoodleConfig(config: MoodleConfigEntity) {
        viewModelScope.launch {
            repository.saveMoodleConfig(config)
            _snackbarEvent.emit("Configuración de cursos.ucf.edu.cu guardada")
        }
    }

    fun syncChatToMoodleCloud(chatId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.syncChatToMoodle(chatId)
            if (res.isSuccess) {
                val url = res.getOrNull() ?: ""
                _snackbarEvent.emit("Chat respaldado en JSON en Moodle UCF: $url")
                onResult(url)
            } else {
                _snackbarEvent.emit("Error al sincronizar: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun importChatFromJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.importChatJson(jsonString)
            if (res.isSuccess) {
                val chat = res.getOrNull()!!
                _snackbarEvent.emit("Chat \"${chat.title}\" sincronizado desde JSON")
                onResult(true, chat.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al procesar JSON"
                _snackbarEvent.emit(err)
                onResult(false, err)
            }
        }
    }

    fun downloadChatFromMoodleUrl(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.syncChatFromMoodleUrl(url)
            if (res.isSuccess) {
                val chat = res.getOrNull()!!
                _snackbarEvent.emit("Chat descargado e importado de UCF Moodle")
                onResult(true, chat.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Error al descargar de UCF"
                _snackbarEvent.emit(err)
                onResult(false, err)
            }
        }
    }

    suspend fun getExportJson(chatId: String): String {
        return repository.exportChatJson(chatId)
    }
}
