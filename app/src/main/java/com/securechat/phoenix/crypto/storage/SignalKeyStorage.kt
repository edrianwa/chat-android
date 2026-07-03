package com.securechat.phoenix.crypto.storage

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord

/**
 * Interface for encrypted Signal key storage.
 * Implementation uses Room + SQLCipher backed by Android Keystore.
 */
interface SignalKeyStorage {
    // Identity
    suspend fun saveIdentityKeyPair(keyPair: IdentityKeyPair)
    suspend fun getIdentityKeyPair(): IdentityKeyPair?
    suspend fun saveRemoteIdentityKey(userId: String, identityKey: IdentityKey)
    suspend fun getRemoteIdentityKey(userId: String): IdentityKey?

    // Registration
    suspend fun saveRegistrationId(id: Int)
    suspend fun getRegistrationId(): Int?

    // Signed Pre-Keys
    suspend fun saveSignedPreKey(record: SignedPreKeyRecord)
    suspend fun getSignedPreKey(keyId: Int): SignedPreKeyRecord?
    suspend fun getCurrentSignedPreKey(): SignedPreKeyRecord?
    suspend fun removeSignedPreKey(keyId: Int)

    // One-Time Pre-Keys
    suspend fun savePreKeys(records: List<PreKeyRecord>)
    suspend fun getPreKey(keyId: Int): PreKeyRecord?
    suspend fun removePreKey(keyId: Int)
    suspend fun getPreKeyCount(): Int

    // Pre-key ID tracking
    suspend fun saveNextPreKeyId(id: Int)
    suspend fun getNextPreKeyId(): Int
}
