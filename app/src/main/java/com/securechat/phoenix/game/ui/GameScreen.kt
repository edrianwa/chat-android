package com.securechat.phoenix.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Full game screen composable with settings button overlay.
 * This is the entry point for the Flying Phoenix game.
 */
@Composable
fun GameScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val screenState by viewModel.screenState.collectAsState()

    if (screenState.isLoading) return

    Box(modifier = Modifier.fillMaxSize()) {
        // Main game canvas
        FlyingPhoenixGame(
            highScore = screenState.highScore,
            onScoreUpdate = viewModel::onScoreUpdate,
            onGameEvent = viewModel::onGameEvent,
            modifier = Modifier.fillMaxSize()
        )

        // Settings gear icon (top-right)
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Game Settings",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
