package com.securechat.phoenix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securechat.phoenix.chat.ui.ChatMessageScreen
import com.securechat.phoenix.chat.ui.ChatViewModel
import com.securechat.phoenix.chat.ui.ContactItem
import com.securechat.phoenix.chat.ui.ContactsScreen
import com.securechat.phoenix.chat.ui.ConversationListScreen
import com.securechat.phoenix.chat.ui.ProfileScreen
import com.securechat.phoenix.chat.ui.ProfileViewModel
import com.securechat.phoenix.chat.ui.UserProfile
import com.securechat.phoenix.game.ui.GameScreen
import com.securechat.phoenix.game.ui.GameSettingsScreen
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

        // Chat List (WhatsApp-style)
        composable(Destination.Chat.route) {
            val viewModel: ChatViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ConversationListScreen(
                conversations = uiState.conversations,
                onConversationClick = { chatId ->
                    navController.navigate(Destination.ChatConversation.withChatId(chatId))
                },
                onNewChat = {
                    navController.navigate(Destination.Contacts.route)
                },
                onProfileClick = {
                    navController.navigate(Destination.Profile.route)
                }
            )
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

            ChatMessageScreen(
                chatId = chatId,
                messages = uiState.messages,
                onSendMessage = { content -> viewModel.sendMessage(chatId, content) },
                onBack = { navController.popBackStack() }
            )
        }

        // Contacts
        composable(Destination.Contacts.route) {
            // TODO: Wire to real contacts ViewModel
            ContactsScreen(
                contacts = emptyList(),
                onContactClick = { userId ->
                    navController.navigate(Destination.ChatConversation.withChatId(userId)) {
                        popUpTo(Destination.Contacts.route) { inclusive = true }
                    }
                },
                onAddContact = { idNumber ->
                    // TODO: API call to search user by ID, add to contacts
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Profile
        composable(Destination.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val profile by viewModel.profile.collectAsState()

            ProfileScreen(
                profile = profile,
                onUpdateName = viewModel::updateDisplayName,
                onUpdateAbout = viewModel::updateAbout,
                onChangeAvatar = { /* TODO: Camera/gallery picker */ },
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
            GameSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
