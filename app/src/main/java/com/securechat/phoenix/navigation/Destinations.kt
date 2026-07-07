package com.securechat.phoenix.navigation

sealed class Destination(val route: String) {
    data object Passcode : Destination("passcode")
    data object Setup : Destination("setup")
    data object Chat : Destination("chat")
    data object ChatConversation : Destination("chat/{chatId}") {
        fun withChatId(chatId: String): String = "chat/$chatId"
    }
    data object Contacts : Destination("contacts")
    data object Profile : Destination("profile")
    data object Settings : Destination("settings")
    data object PasscodeSettings : Destination("settings/passcode")
    data object PrivacySettings : Destination("settings/privacy")
    data object StorageSettings : Destination("settings/storage")
    data object AdminPanel : Destination("admin")
    data object Camera : Destination("camera/{chatId}") {
        fun withChatId(chatId: String): String = "camera/$chatId"
    }
    data object VoiceCall : Destination("call/voice/{chatId}") {
        fun withChatId(chatId: String): String = "call/voice/$chatId"
    }
    data object VideoCall : Destination("call/video/{chatId}") {
        fun withChatId(chatId: String): String = "call/video/$chatId"
    }
    data object DecoyGame : Destination("decoy_game")
    data object GameSettings : Destination("game_settings")
}
