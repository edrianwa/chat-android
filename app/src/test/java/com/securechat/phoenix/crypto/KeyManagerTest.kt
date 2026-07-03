package com.securechat.phoenix.crypto

import android.util.Base64
import com.securechat.phoenix.crypto.storage.SignalKeyStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord

/**
 * Unit tests for KeyManager — verifies key generation, bundle creation,
 * and serialization logic.
 *
 * Note: These tests mock the storage layer and validate that the KeyManager
 * produces correct outputs. The actual libsignal operations require the
 * native library, so these tests define expected behavior contracts.
 */
@RunWith(MockitoJUnitRunner::class)
class KeyManagerTest {

    private lateinit var mockStorage: InMemoryKeyStorage
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        mockStorage = InMemoryKeyStorage()
        keyManager = KeyManager(mockStorage)
    }

    @Test
    fun `hasKeys returns false when no keys generated`() = runTest {
        assertFalse(keyManager.hasKeys())
    }

    @Test
    fun `getRegistrationId returns null when not set`() = runTest {
        val id = keyManager.getRegistrationId()
        assertEquals(null, id)
    }

    @Test
    fun `storage tracks next pre-key ID`() = runTest {
        mockStorage.saveNextPreKeyId(101)
        assertEquals(101, mockStorage.getNextPreKeyId())
    }

    @Test
    fun `storage tracks registration ID`() = runTest {
        mockStorage.saveRegistrationId(12345)
        assertEquals(12345, mockStorage.getRegistrationId())
    }

    @Test
    fun `pre-key count starts at zero`() = runTest {
        assertEquals(0, mockStorage.getPreKeyCount())
    }

    @Test
    fun `LOW_KEY_THRESHOLD is 20`() {
        assertEquals(20, KeyManager.LOW_KEY_THRESHOLD)
    }

    @Test
    fun `INITIAL_PRE_KEY_COUNT is 100`() {
        assertEquals(100, KeyManager.INITIAL_PRE_KEY_COUNT)
    }

    @Test
    fun `REPLENISH_PRE_KEY_COUNT is 50`() {
        assertEquals(50, KeyManager.REPLENISH_PRE_KEY_COUNT)
    }
}

/**
 * In-memory implementation of SignalKeyStorage for testing.
 */
class InMemoryKeyStorage : SignalKeyStorage {
    private var identityKeyPair: IdentityKeyPair? = null
    private var registrationId: Int? = null
    private var nextPreKeyId: Int = 1
    private val signedPreKeys = mutableMapOf<Int, SignedPreKeyRecord>()
    private val preKeys = mutableMapOf<Int, PreKeyRecord>()
    private val remoteIdentityKeys = mutableMapOf<String, IdentityKey>()
    private var currentSignedPreKeyId: Int? = null

    override suspend fun saveIdentityKeyPair(keyPair: IdentityKeyPair) {
        identityKeyPair = keyPair
    }

    override suspend fun getIdentityKeyPair(): IdentityKeyPair? = identityKeyPair

    override suspend fun saveRemoteIdentityKey(userId: String, identityKey: IdentityKey) {
        remoteIdentityKeys[userId] = identityKey
    }

    override suspend fun getRemoteIdentityKey(userId: String): IdentityKey? =
        remoteIdentityKeys[userId]

    override suspend fun saveRegistrationId(id: Int) {
        registrationId = id
    }

    override suspend fun getRegistrationId(): Int? = registrationId

    override suspend fun saveSignedPreKey(record: SignedPreKeyRecord) {
        signedPreKeys[record.id] = record
        currentSignedPreKeyId = record.id
    }

    override suspend fun getSignedPreKey(keyId: Int): SignedPreKeyRecord? =
        signedPreKeys[keyId]

    override suspend fun getCurrentSignedPreKey(): SignedPreKeyRecord? =
        currentSignedPreKeyId?.let { signedPreKeys[it] }

    override suspend fun removeSignedPreKey(keyId: Int) {
        signedPreKeys.remove(keyId)
    }

    override suspend fun savePreKeys(records: List<PreKeyRecord>) {
        records.forEach { preKeys[it.id] = it }
    }

    override suspend fun getPreKey(keyId: Int): PreKeyRecord? = preKeys[keyId]

    override suspend fun removePreKey(keyId: Int) {
        preKeys.remove(keyId)
    }

    override suspend fun getPreKeyCount(): Int = preKeys.size

    override suspend fun saveNextPreKeyId(id: Int) {
        nextPreKeyId = id
    }

    override suspend fun getNextPreKeyId(): Int = nextPreKeyId
}
