package com.securechat.phoenix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

            viewModel.openChat(chatId)

            // Media picker launcher — shows photos AND videos from gallery
            val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    // TODO: pass uri to MediaRepository.uploadImage(uri, chatId, messageId)
                    viewModel.sendMessage(chatId, "[Media attached]")
                }
            }

            ChatMessageScreen(
                chatId = chatId,
                displayName = uiState.contactNames[chatId] ?: "",
                messages = uiState.messages,
                onSendMessage = { content -> viewModel.sendMessage(chatId, content) },
                onVoiceCall = {
                    navController.navigate(Destination.VoiceCall.withChatId(chatId))
                },
                onVideoCall = {
                    navController.navigate(Destination.VideoCall.withChatId(chatId))
                },
                onAttachMedia = {
                    mediaPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
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
