package com.securechat.phoenix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securechat.phoenix.game.ui.GameScreen
import com.securechat.phoenix.game.ui.GameSettingsScreen
import com.securechat.phoenix.ui.screens.chat.ChatScreen
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

        composable(Destination.Chat.route) {
            ChatScreen()
        }

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
