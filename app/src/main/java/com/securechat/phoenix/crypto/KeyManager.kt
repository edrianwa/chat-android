package com.securechat.phoenix.crypto

import android.util.Base64
import com.securechat.phoenix.crypto.models.FetchedKeyBundle
import com.securechat.phoenix.crypto.models.KeyBundle
import com.securechat.phoenix.crypto.models.PreKeyData
import com.securechat.phoenix.crypto.models.SignedPreKeyData
import com.securechat.phoenix.crypto.storage.SignalKeyStorage
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all Signal Protocol key operations:
 * - Key generation (identity, signed pre-key, one-time pre-keys)
 * - Key bundle creation for server upload
 * - Local key storage (encrypted)
 * - Key rotation and replenishment
 */
@Singleton
class KeyManager @Inject constructor(
    private val keyStorage: SignalKeyStorage
) {
    companion object {
        const val INITIAL_PRE_KEY_COUNT = 100
        const val REPLENISH_PRE_KEY_COUNT = 50
        const val LOW_KEY_THRESHOLD = 20
    }

    /**
     * Generate all keys needed for initial registration.
     * Returns a KeyBundle ready for server upload.
     */
    suspend fun generateRegistrationKeys(): KeyBundle {
        // Generate registration ID
        val registrationId = KeyHelper.generateRegistrationId(false)

        // Generate identity key pair
        val identityKeyPair = generateIdentityKeyPair()

        // Generate signed pre-key
        val signedPreKeyId = SecureRandom().nextInt(0xFFFFFF)
        val signedPreKey = generateSignedPreKey(identityKeyPair, signedPreKeyId)

        // Generate one-time pre-keys
        val startId = 1
        val preKeys = generatePreKeys(startId, INITIAL_PRE_KEY_COUNT)

        // Store locally
        keyStorage.saveRegistrationId(registrationId)
        keyStorage.saveIdentityKeyPair(identityKeyPair)
        keyStorage.saveSignedPreKey(signedPreKey)
        keyStorage.savePreKeys(preKeys)
        keyStorage.saveNextPreKeyId(startId + INITIAL_PRE_KEY_COUNT)

        // Build bundle for server
        return KeyBundle(
            identityKey = encodePublicKey(identityKeyPair.publicKey),
            registrationId = registrationId,
            signedPreKey = SignedPreKeyData(
                keyId = signedPreKey.id,
                publicKey = encodeBytes(signedPreKey.keyPair.publicKey.serialize()),
                signature = encodeBytes(signedPreKey.signature)
            ),
            oneTimePreKeys = preKeys.map { preKey ->
                PreKeyData(
                    keyId = preKey.id,
                    publicKey = encodeBytes(preKey.keyPair.publicKey.serialize())
                )
            }
        )
    }

    /**
     * Generate a new signed pre-key for rotation.
     * Returns the signed pre-key data for server upload.
     */
    suspend fun rotateSignedPreKey(): SignedPreKeyData {
        val identityKeyPair = keyStorage.getIdentityKeyPair()
            ?: throw IllegalStateException("No identity key pair found")

        val newKeyId = SecureRandom().nextInt(0xFFFFFF)
        val signedPreKey = generateSignedPreKey(identityKeyPair, newKeyId)

        keyStorage.saveSignedPreKey(signedPreKey)

        return SignedPreKeyData(
            keyId = signedPreKey.id,
            publicKey = encodeBytes(signedPreKey.keyPair.publicKey.serialize()),
            signature = encodeBytes(signedPreKey.signature)
        )
    }

    /**
     * Generate additional one-time pre-keys for replenishment.
     * Returns the list of pre-key data for server upload.
     */
    suspend fun generateReplenishmentKeys(count: Int = REPLENISH_PRE_KEY_COUNT): List<PreKeyData> {
        val startId = keyStorage.getNextPreKeyId()
        val preKeys = generatePreKeys(startId, count)

        keyStorage.savePreKeys(preKeys)
        keyStorage.saveNextPreKeyId(startId + count)

        return preKeys.map { preKey ->
            PreKeyData(
                keyId = preKey.id,
                publicKey = encodeBytes(preKey.keyPair.publicKey.serialize())
            )
        }
    }

    /**
     * Process a fetched key bundle from another user.
     * Validates the bundle and stores the remote identity key.
     */
    suspend fun processRemoteKeyBundle(
        remoteUserId: String,
        bundle: FetchedKeyBundle
    ): Boolean {
        return try {
            val remoteIdentityKey = decodeIdentityKey(bundle.identityKey)
            keyStorage.saveRemoteIdentityKey(remoteUserId, remoteIdentityKey)
            true
        } catch (e: InvalidKeyException) {
            false
        }
    }

    /**
     * Get the local registration ID.
     */
    suspend fun getRegistrationId(): Int? {
        return keyStorage.getRegistrationId()
    }

    /**
     * Check if keys have been generated (registration complete).
     */
    suspend fun hasKeys(): Boolean {
        return keyStorage.getIdentityKeyPair() != null
    }

    // --- Private key generation helpers ---

    private fun generateIdentityKeyPair(): IdentityKeyPair {
        val keyPair: ECKeyPair = Curve.generateKeyPair()
        return IdentityKeyPair(IdentityKey(keyPair.publicKey), keyPair.privateKey)
    }

    private fun generateSignedPreKey(
        identityKeyPair: IdentityKeyPair,
        keyId: Int
    ): SignedPreKeyRecord {
        val keyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            identityKeyPair.privateKey,
            keyPair.publicKey.serialize()
        )
        val timestamp = System.currentTimeMillis()
        return SignedPreKeyRecord(keyId, timestamp, keyPair, signature)
    }

    private fun generatePreKeys(startId: Int, count: Int): List<PreKeyRecord> {
        return (startId until startId + count).map { id ->
            val keyPair = Curve.generateKeyPair()
            PreKeyRecord(id, keyPair)
        }
    }

    // --- Encoding/Decoding helpers ---

    private fun encodeBytes(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun encodePublicKey(identityKey: IdentityKey): String {
        return Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP)
    }

    private fun decodeIdentityKey(base64Key: String): IdentityKey {
        val bytes = Base64.decode(base64Key, Base64.NO_WRAP)
        return IdentityKey(bytes, 0)
    }
}
