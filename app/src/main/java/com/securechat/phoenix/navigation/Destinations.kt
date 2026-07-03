package com.securechat.phoenix.navigation

/**
 * All navigation destinations in the app.
 * Each destination has a unique route string used by Navigation Compose.
 */
sealed class Destination(val route: String) {
    data object Passcode : Destination("passcode")
    data object Setup : Destination("setup")
    data object Chat : Destination("chat")
    data object ChatConversation : Destination("chat/{chatId}") {
        fun withChatId(chatId: String): String = "chat/$chatId"
    }
    data object DecoyGame : Destination("decoy_game")
    data object GameSettings : Destination("game_settings")
}
