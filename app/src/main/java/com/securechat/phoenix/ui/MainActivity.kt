package com.securechat.phoenix.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.securechat.phoenix.navigation.PhoenixNavHost
import com.securechat.phoenix.ui.theme.PhoenixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoenixTheme {
                PhoenixNavHost()
            }
        }
    }
}
