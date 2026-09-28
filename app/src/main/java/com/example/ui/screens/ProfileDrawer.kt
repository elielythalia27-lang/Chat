package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.TelegramAvatar
import com.example.ui.theme.MoodleOrange
import com.example.ui.theme.TelegramBlue
import kotlinx.coroutines.launch
import com.example.ui.theme.TelegramDarkSurfaceVariant

@Composable
fun ProfileDrawerContent(
    viewModel: MainViewModel,
    onCloseDrawer: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Google Play compliant visual photo picker for profile photo
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Update profile avatar URL to the chosen URI string
            currentUser?.let { user ->
                viewModel.updateProfile(user.displayName, user.bio, uri.toString())
            }
        }
    }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Header with User Profile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TelegramDarkSurfaceVariant)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            TelegramAvatar(
                                imageUrl = currentUser?.avatarUrl,
                                name = currentUser?.displayName ?: "Usuario",
                                size = 64.dp,
                                onClick = {
                                    profilePhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )

                            // Camera change icon badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue)
                                    .clickable {
                                        profilePhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cambiar Foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Perfil",
                                tint = TelegramBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = currentUser?.displayName ?: "Mi Usuario",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    Text(
                        text = "@${currentUser?.username ?: "usuario"}",
                        fontSize = 13.sp,
                        color = TelegramBlue
                    )

                    if (!currentUser?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser!!.bio,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TelegramBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TeleUCF • Conectado",
                            fontSize = 11.sp,
                            color = TelegramBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Items
            NavigationDrawerItem(
                label = { Text("Mis Chats", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Message, contentDescription = null, tint = TelegramBlue) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.ChatList)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Editar Perfil y Usuario", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = TelegramBlue) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showEditProfileDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

            NavigationDrawerItem(
                label = { Text("Ajustes de Privacidad y Cuenta", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    showEditProfileDialog = true
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    // Edit Profile Dialog with Username Uniqueness Verification
    if (showEditProfileDialog) {
        val coroutineScope = rememberCoroutineScope()
        var editUsername by remember(currentUser) { mutableStateOf(currentUser?.username ?: "") }
        var editName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
        var editBio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }
        var editAvatar by remember(currentUser) { mutableStateOf(currentUser?.avatarUrl ?: "") }
        var usernameError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
            title = { Text("Editar Perfil") },
            text = {
                Column {
                    Text(
                        "Información pública y nombre de usuario único:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = {
                            editUsername = it.trim().removePrefix("@")
                            usernameError = null
                        },
                        label = { Text("Nombre de usuario (@usuario)") },
                        prefix = { Text("@") },
                        isError = usernameError != null,
                        supportingText = {
                            if (usernameError != null) {
                                Text(usernameError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            } else {
                                Text("Verificación obligatoria: no se permiten duplicados", fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre para mostrar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Info / Biografía") },
                        placeholder = { Text("ej. Estudiante UCF") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editAvatar,
                        onValueChange = { editAvatar = it },
                        label = { Text("URL de Foto de Perfil") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editUsername.trim().removePrefix("@")
                        if (clean.isBlank()) {
                            usernameError = "El nombre de usuario no puede estar vacío."
                            return@Button
                        }
                        if (clean.equals("Eliel_21", ignoreCase = true) && currentUser?.username != "Eliel_21") {
                            usernameError = "El usuario @Eliel_21 está reservado para el Propietario de la app."
                            return@Button
                        }

                        isSaving = true
                        coroutineScope.launch {
                            val isAvailable = viewModel.checkUsernameAvailability(clean)
                            if (!isAvailable) {
                                usernameError = "El usuario @$clean ya está registrado. Elige otro."
                                isSaving = false
                            } else {
                                viewModel.updateProfileWithUsername(
                                    newUsername = clean,
                                    displayName = editName.trim(),
                                    bio = editBio.trim(),
                                    avatarUrl = editAvatar.trim()
                                ) { success, errorMsg ->
                                    isSaving = false
                                    if (success) {
                                        showEditProfileDialog = false
                                    } else {
                                        usernameError = errorMsg
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Verificando..." else "Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
