package com.securechat.phoenix.navigation

import com.securechat.phoenix.data.PasscodeRepository

/**
 * Routes passcodes to their assigned destinations.
 * The core routing system that maps user-entered passcodes to screen routes.
 */
interface PasscodeRouter {
    /** Resolve a passcode to a navigation route. Returns null if not found. */
    suspend fun resolveRoute(passcode: String): String?

    /** Check if the initial setup has been completed */
    suspend fun isSetupComplete(): Boolean
}

class PasscodeRouterImpl(
    private val passcodeRepository: PasscodeRepository,
    private val screenRegistry: ScreenRegistry
) : PasscodeRouter {

    override suspend fun resolveRoute(passcode: String): String? {
        val mapping = passcodeRepository.getPasscodeMapping(passcode) ?: return null
        val screen = screenRegistry.findScreen(mapping.screenId)
        return screen?.route
    }

    override suspend fun isSetupComplete(): Boolean {
        return passcodeRepository.hasPasscodesConfigured()
    }
}
