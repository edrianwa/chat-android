package com.securechat.phoenix.navigation

/**
 * Registry of available screens that can be mapped to passcodes.
 * Supports modular registration of new screens.
 */
interface ScreenRegistry {
    /** Get all registered screen entries */
    fun getRegisteredScreens(): List<ScreenEntry>

    /** Register a new screen that can be assigned to a passcode */
    fun registerScreen(entry: ScreenEntry)

    /** Find a screen by its identifier */
    fun findScreen(screenId: String): ScreenEntry?
}

/**
 * Represents a screen that can be routed to via a passcode.
 */
data class ScreenEntry(
    val id: String,
    val displayName: String,
    val route: String,
    val description: String = ""
)

class ScreenRegistryImpl : ScreenRegistry {
    private val screens = mutableListOf<ScreenEntry>()

    init {
        // Register default screens
        registerScreen(
            ScreenEntry(
                id = "chat",
                displayName = "Secure Chat",
                route = Destination.Chat.route,
                description = "Access the encrypted messaging interface"
            )
        )
        registerScreen(
            ScreenEntry(
                id = "decoy_game",
                displayName = "Decoy Game",
                route = Destination.DecoyGame.route,
                description = "Shows a casual game as a decoy"
            )
        )
    }

    override fun getRegisteredScreens(): List<ScreenEntry> = screens.toList()

    override fun registerScreen(entry: ScreenEntry) {
        if (screens.none { it.id == entry.id }) {
            screens.add(entry)
        }
    }

    override fun findScreen(screenId: String): ScreenEntry? {
        return screens.find { it.id == screenId }
    }
}
