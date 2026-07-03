package com.securechat.phoenix.data

/**
 * Data class representing a mapping between a passcode and a screen.
 */
data class PasscodeMapping(
    val passcodeHash: String,
    val screenId: String
)

/**
 * Repository interface for managing passcode storage and retrieval.
 */
interface PasscodeRepository {
    /** Get the screen mapping for a given passcode. Returns null if not found. */
    suspend fun getPasscodeMapping(passcode: String): PasscodeMapping?

    /** Save a passcode-to-screen mapping */
    suspend fun savePasscodeMapping(passcode: String, screenId: String)

    /** Check if any passcodes have been configured */
    suspend fun hasPasscodesConfigured(): Boolean

    /** Clear all stored passcode mappings */
    suspend fun clearAll()
}

class PasscodeRepositoryImpl(
    private val secureStore: SecurePasscodeStore
) : PasscodeRepository {

    override suspend fun getPasscodeMapping(passcode: String): PasscodeMapping? {
        return secureStore.findMapping(passcode)
    }

    override suspend fun savePasscodeMapping(passcode: String, screenId: String) {
        secureStore.storeMapping(passcode, screenId)
    }

    override suspend fun hasPasscodesConfigured(): Boolean {
        return secureStore.hasStoredPasscodes()
    }

    override suspend fun clearAll() {
        secureStore.clearAll()
    }
}
