package com.securechat.phoenix.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securechat.phoenix.chat.ui.ChatMessageScreen
import com.securechat.phoenix.chat.ui.ChatViewModel
import com.securechat.phoenix.chat.ui.ContactsScreen
import com.securechat.phoenix.chat.ui.ContactsViewModel
import com.securechat.phoenix.chat.ui.ConversationListScreen
import com.securechat.phoenix.chat.ui.MainScaffold
import com.securechat.phoenix.chat.ui.ProfileScreen
import com.securechat.phoenix.chat.ui.ProfileViewModel
import com.securechat.phoenix.game.ui.GameScreen
import com.securechat.phoenix.game.ui.GameSettingsScreen
import com.securechat.phoenix.settings.AdminPanelScreen
import com.securechat.phoenix.settings.PasscodeEntry
import com.securechat.phoenix.settings.PasscodeSettingsScreen
import com.securechat.phoenix.settings.PrivacySettings
import com.securechat.phoenix.settings.PrivacySettingsScreen
import com.securechat.phoenix.settings.StorageScreen
import com.securechat.phoenix.settings.UserSettingsScreen
import com.securechat.phoenix.ui.screens.passcode.PasscodeScreen
import com.securechat.phoenix.ui.screens.passcode.PasscodeViewModel
import com.securechat.phoenix.ui.screens.setup.SetupScreen
import com.securechat.phoenix.ui.screens.setup.SetupViewModel

@Composable
fun PhoenixNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Passcode.route
    ) {
        composable(Destination.Passcode.route) {
            val viewModel: PasscodeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            PasscodeScreen(
                uiState = uiState,
                onDigitPressed = viewModel::onDigitPressed,
                onDeletePressed = viewModel::onDeletePressed,
                onNavigateToSetup = {
                    navController.navigate(Destination.Setup.route) {
                        popUpTo(Destination.Passcode.route) { inclusive = true }
                    }
                },
                onNavigateToDestination = { route ->
                    navController.navigate(route) {
                        popUpTo(Destination.Passcode.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Destination.Setup.route) {
            val viewModel: SetupViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            SetupScreen(
                uiState = uiState,
                onChatPasscodeChanged = viewModel::onChatPasscodeChanged,
                onDecoyPasscodeChanged = viewModel::onDecoyPasscodeChanged,
                onConfirmSetup = viewModel::onConfirmSetup,
                onSetupComplete = {
                    navController.navigate(Destination.Passcode.route) {
                        popUpTo(Destination.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        // Main screen with bottom nav (Chat, Calls, Settings)
        composable(Destination.Chat.route) {
            val viewModel: ChatViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            var currentTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(0) }

            MainScaffold(
                currentTab = currentTab,
                onTabChanged = { tab -> currentTab = tab }
            ) {
                when (currentTab) {
                    0 -> {
                        // Chat tab
                        ConversationListScreen(
                            conversations = uiState.conversations,
                            contactNames = uiState.contactNames,
                            onConversationClick = { chatId ->
                                navController.navigate(Destination.ChatConversation.withChatId(chatId))
                            },
                            onNewChat = {
                                navController.navigate(Destination.Contacts.route)
                            },
                            onProfileClick = {
                                navController.navigate(Destination.Profile.route)
                            },
                            onSettingsClick = {
                                navController.navigate(Destination.Settings.route)
                            }
                        )
                    }
                    1 -> {
                        // Calls tab
                        com.securechat.phoenix.call.ui.CallHistoryScreen()
                    }
                    2 -> {
                        // Settings tab
                        UserSettingsScreen(
                            isAdmin = false,
                            onBack = { currentTab = 0 },
                            onProfileClick = { navController.navigate(Destination.Profile.route) },
                            onPasscodeClick = { navController.navigate(Destination.PasscodeSettings.route) },
                            onPrivacyClick = { navController.navigate(Destination.PrivacySettings.route) },
                            onStorageClick = { navController.navigate(Destination.StorageSettings.route) },
                            onAdminClick = { navController.navigate(Destination.AdminPanel.route) },
                            onDeleteAccount = {}
                        )
                    }
                }
            }
        }

        // Chat Conversation
        composable(
            route = Destination.ChatConversation.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val viewModel: ChatViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            viewModel.openChat(chatId)

            // Photo preview state — holds URI of selected/taken photo before sending
            var pendingPhotoUri by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null)
            }

            // Gallery picker
            val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    pendingPhotoUri = uri // Show preview, don't send yet
                }
            }

            // Camera capture
            var cameraUri by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null)
            }
            val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
            ) { success ->
                if (success && cameraUri != null) {
                    pendingPhotoUri = cameraUri // Show preview, don't send yet
                }
            }

            // Camera permission
            val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && cameraUri != null) {
                    cameraLauncher.launch(cameraUri!!)
                }
            }

            // Show attach options dialog
            var showAttachOptions by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            if (showAttachOptions) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showAttachOptions = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55000000))
                            .clickable { showAttachOptions = false },
                        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                    ) {
                        Box(modifier = Modifier.clickable(enabled = false) {}) {
                            com.securechat.phoenix.chat.ui.AttachBottomSheet(
                                onDismiss = { showAttachOptions = false },
                                onGallery = {
                                    showAttachOptions = false
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                        )
                                    )
                                },
                                onCamera = {
                                    showAttachOptions = false
                                    navController.navigate(Destination.Camera.withChatId(chatId))
                                },
                                onImageSelected = { uri ->
                                    pendingPhotoUri = uri
                                }
                            )
                        }
                    }
                }
            }

            // Photo preview overlay (shown when photo is selected but not yet sent)
            if (pendingPhotoUri != null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChatMessageScreen(
                        chatId = chatId,
                        displayName = uiState.contactNames[chatId] ?: "",
                        messages = uiState.messages,
                        onSendMessage = { content -> viewModel.sendMessage(chatId, content) },
                        onVoiceCall = { navController.navigate(Destination.VoiceCall.withChatId(chatId)) },
                        onVideoCall = { navController.navigate(Destination.VideoCall.withChatId(chatId)) },
                        onAttachMedia = { showAttachOptions = true },
                        onBack = { navController.popBackStack() }
                    )

                    // Full-screen photo preview overlay
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            // Image preview
                            coil.compose.AsyncImage(
                                model = pendingPhotoUri,
                                contentDescription = "Photo preview",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )

                            // Bottom bar: Cancel + Send
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                // Cancel button
                                androidx.compose.material3.TextButton(
                                    onClick = { pendingPhotoUri = null }
                                ) {
                                    androidx.compose.material3.Text(
                                        "✕ Cancel",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 18.sp
                                    )
                                }

                                // Send button
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(com.securechat.phoenix.ui.theme.ChatColors.Green)
                                        .clickable {
                                            // Send photo as message (URI stored for upload)
                                            viewModel.sendMessage(chatId, "image:${pendingPhotoUri}")
                                            pendingPhotoUri = null
                                        },
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(
                                        "➤",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                ChatMessageScreen(
                    chatId = chatId,
                    displayName = uiState.contactNames[chatId] ?: "",
                    messages = uiState.messages,
                    onSendMessage = { content -> viewModel.sendMessage(chatId, content) },
                    onVoiceCall = { navController.navigate(Destination.VoiceCall.withChatId(chatId)) },
                    onVideoCall = { navController.navigate(Destination.VideoCall.withChatId(chatId)) },
                    onAttachMedia = { showAttachOptions = true },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Contacts
        composable(Destination.Contacts.route) {
            val viewModel: ContactsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ContactsScreen(
                uiState = uiState,
                onContactClick = { userId ->
                    navController.navigate(Destination.ChatConversation.withChatId(userId)) {
                        popUpTo(Destination.Contacts.route) { inclusive = true }
                    }
                },
                onAddContact = { idNumber ->
                    viewModel.searchUserById(idNumber)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Profile
        composable(Destination.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val profile by viewModel.profile.collectAsState()
            val isUploading by viewModel.isUploading.collectAsState()

            // Photo picker for avatar
            val avatarPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    viewModel.uploadAvatar(uri)
                }
            }

            ProfileScreen(
                profile = profile,
                isUploading = isUploading,
                onUpdateName = viewModel::updateDisplayName,
                onUpdateAbout = viewModel::updateAbout,
                onAvatarSelected = { viewModel.uploadAvatar(it) },
                onChangeAvatar = {
                    avatarPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Settings Hub
        composable(Destination.Settings.route) {
            UserSettingsScreen(
                isAdmin = false, // TODO: read from TokenManager role
                onBack = { navController.popBackStack() },
                onProfileClick = { navController.navigate(Destination.Profile.route) },
                onPasscodeClick = { navController.navigate(Destination.PasscodeSettings.route) },
                onPrivacyClick = { navController.navigate(Destination.PrivacySettings.route) },
                onStorageClick = { navController.navigate(Destination.StorageSettings.route) },
                onAdminClick = { navController.navigate(Destination.AdminPanel.route) },
                onDeleteAccount = { /* TODO: confirmation dialog + API call */ }
            )
        }

        // Passcode Settings
        composable(Destination.PasscodeSettings.route) {
            PasscodeSettingsScreen(
                passcodes = listOf(
                    PasscodeEntry("••••••", "Secure Chat", "chat"),
                    PasscodeEntry("••••••", "Flying Phoenix", "decoy_game")
                ),
                onBack = { navController.popBackStack() }
            )
        }

        // Privacy Settings
        composable(Destination.PrivacySettings.route) {
            PrivacySettingsScreen(
                settings = PrivacySettings(),
                onBack = { navController.popBackStack() },
                onLastSeenChange = { /* TODO: API call */ },
                onReadReceiptsChange = { /* TODO: API call */ },
                onPhotoVisibilityChange = { /* TODO: API call */ }
            )
        }

        // Storage
        composable(Destination.StorageSettings.route) {
            StorageScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Admin Panel
        composable(Destination.AdminPanel.route) {
            AdminPanelScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Decoy Game
        composable(Destination.DecoyGame.route) {
            GameScreen(
                onNavigateToSettings = {
                    navController.navigate(Destination.GameSettings.route)
                }
            )
        }

        composable(Destination.GameSettings.route) {
            GameSettingsScreen(onBack = { navController.popBackStack() })
        }

        // Custom Camera
        composable(
            route = Destination.Camera.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val viewModel: ChatViewModel = hiltViewModel()

            com.securechat.phoenix.chat.camera.CameraScreen(
                onPhotoCaptured = { uri ->
                    viewModel.sendMessage(chatId, "image:$uri")
                    navController.popBackStack()
                },
                onVideoCaptured = { uri ->
                    viewModel.sendMessage(chatId, "video:$uri")
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }

        // Voice Call
        composable(
            route = Destination.VoiceCall.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val viewModel: com.securechat.phoenix.call.CallViewModel = hiltViewModel()
            val session by viewModel.callSession.collectAsState()
            val permissionNeeded by viewModel.permissionNeeded.collectAsState()

            // Request permission when needed
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted -> viewModel.onPermissionResult(granted) }

            androidx.compose.runtime.LaunchedEffect(permissionNeeded) {
                permissionNeeded?.let { permissionLauncher.launch(it) }
            }

            // Start call on first composition
            androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.startCall() }

            // Navigate back when call ends
            androidx.compose.runtime.LaunchedEffect(session.state) {
                if (session.state == com.securechat.phoenix.call.CallState.ENDED ||
                    session.state == com.securechat.phoenix.call.CallState.FAILED ||
                    session.state == com.securechat.phoenix.call.CallState.REJECTED ||
                    session.state == com.securechat.phoenix.call.CallState.MISSED) {
                    kotlinx.coroutines.delay(1500) // Show status briefly
                    navController.popBackStack()
                }
            }

            com.securechat.phoenix.call.ui.InCallScreen(
                session = session,
                onEndCall = { viewModel.endCall() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() }
            )
        }

        // Video Call
        composable(
            route = Destination.VideoCall.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val viewModel: com.securechat.phoenix.call.CallViewModel = hiltViewModel()
            val session by viewModel.callSession.collectAsState()
            val permissionNeeded by viewModel.permissionNeeded.collectAsState()

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted -> viewModel.onPermissionResult(granted) }

            androidx.compose.runtime.LaunchedEffect(permissionNeeded) {
                permissionNeeded?.let { permissionLauncher.launch(it) }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.setCallType("video")
                viewModel.startCall()
            }

            androidx.compose.runtime.LaunchedEffect(session.state) {
                if (session.state == com.securechat.phoenix.call.CallState.ENDED ||
                    session.state == com.securechat.phoenix.call.CallState.FAILED ||
                    session.state == com.securechat.phoenix.call.CallState.REJECTED ||
                    session.state == com.securechat.phoenix.call.CallState.MISSED) {
                    kotlinx.coroutines.delay(1500)
                    navController.popBackStack()
                }
            }

            com.securechat.phoenix.call.ui.VideoCallScreen(
                session = session,
                isVideoEnabled = true,
                remoteVideoEnabled = false,
                connectionQuality = com.securechat.phoenix.call.ConnectionQuality.GOOD,
                onEndCall = { viewModel.endCall() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleVideo = {},
                onSwitchCamera = {},
                onToggleSpeaker = { viewModel.toggleSpeaker() }
            )
        }
    }
}
