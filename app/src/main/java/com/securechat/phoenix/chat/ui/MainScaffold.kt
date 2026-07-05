package com.securechat.phoenix.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.phoenix.ui.theme.ChatColors

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Main scaffold with glassmorphism bottom navigation bar.
 * Tabs: Chat, Calls, Settings — each navigates to real content.
 */
@Composable
fun MainScaffold(
    currentTab: Int,
    onTabChanged: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val navItems = listOf(
        BottomNavItem("Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
        BottomNavItem("Calls", Icons.Filled.Call, Icons.Outlined.Call),
        BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)) {
            content()
        }

        // Glassmorphism bottom nav bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Glass background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        color = if (MaterialTheme.colorScheme.background == ChatColors.SurfaceDark)
                            Color(0xCC1F2C34) // Dark glass
                        else
                            Color(0xCCFFFFFF) // Light glass
                    )
            )

            // Nav items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    GlassNavItem(
                        item = item,
                        isSelected = currentTab == index,
                        onClick = { onTabChanged(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = if (isSelected) {
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChatColors.Teal.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            } else {
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = if (isSelected) ChatColors.Teal else ChatColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            color = if (isSelected) ChatColors.Teal else ChatColors.TextSecondary
        )
    }
}
