package com.securechat.phoenix.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.securechat.phoenix.navigation.PhoenixNavHost
import com.securechat.phoenix.notifications.PhoenixFirebaseService
import com.securechat.phoenix.security.SecurityManager
import com.securechat.phoenix.ui.theme.PhoenixTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var securityManager: SecurityManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FLAG_SECURE: prevent screenshots and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        // Create notification channels
        PhoenixFirebaseService.createNotificationChannels(this)

        // Request notification permission on Android 13+
        requestNotificationPermissionIfNeeded()

        setContent {
            PhoenixTheme {
                PhoenixNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        securityManager.recordInteraction()

        // Auto-lock: if inactivity timeout exceeded, restart to passcode screen
        if (securityManager.shouldAutoLock()) {
            // Restart the activity to force passcode re-entry
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        // Clear clipboard when app goes to background
        securityManager.clearClipboard()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // Reset auto-lock timer on any user interaction
        securityManager.recordInteraction()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
