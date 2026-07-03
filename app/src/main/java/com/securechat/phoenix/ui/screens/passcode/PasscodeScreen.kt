package com.securechat.phoenix.ui.screens.passcode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasscodeScreen(
    uiState: PasscodeUiState,
    onDigitPressed: (Int) -> Unit,
    onDeletePressed: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToDestination: (String) -> Unit
) {
    // Navigate to setup if needed
    LaunchedEffect(uiState.needsSetup) {
        if (uiState.needsSetup && !uiState.isLoading) {
            onNavigateToSetup()
        }
    }

    // Navigate when route is resolved
    LaunchedEffect(uiState.resolvedRoute) {
        uiState.resolvedRoute?.let { route ->
            onNavigateToDestination(route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = "Phoenix",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Passcode dots indicator
        PasscodeDots(
            enteredLength = uiState.enteredDigits.length,
            maxLength = uiState.maxLength
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // PIN pad
        PinPad(
            onDigitPressed = onDigitPressed,
            onDeletePressed = onDeletePressed
        )
    }
}

@Composable
private fun PasscodeDots(enteredLength: Int, maxLength: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < enteredLength
            val color = animateColorAsState(
                targetValue = if (isFilled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                },
                animationSpec = tween(150),
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color.value)
            )
        }
    }
}

@Composable
private fun PinPad(
    onDigitPressed: (Int) -> Unit,
    onDeletePressed: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: 1, 2, 3
        PinRow(digits = listOf(1, 2, 3), onDigitPressed = onDigitPressed)
        // Row 2: 4, 5, 6
        PinRow(digits = listOf(4, 5, 6), onDigitPressed = onDigitPressed)
        // Row 3: 7, 8, 9
        PinRow(digits = listOf(7, 8, 9), onDigitPressed = onDigitPressed)
        // Row 4: empty, 0, delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space
            Box(modifier = Modifier.size(72.dp))

            // Zero button
            PinButton(digit = 0, onClick = { onDigitPressed(0) })

            // Delete button
            IconButton(
                onClick = onDeletePressed,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PinRow(digits: List<Int>, onDigitPressed: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        digits.forEach { digit ->
            PinButton(digit = digit, onClick = { onDigitPressed(digit) })
        }
    }
}

@Composable
private fun PinButton(digit: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp)
    ) {
        Text(
            text = digit.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}
