package com.securechat.phoenix.crypto.session

import android.util.Base64
import com.securechat.phoenix.crypto.models.FetchedKeyBundle
import com.securechat.phoenix.crypto.storage.SignalKeyStorage
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.PreKeyBundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Signal Protocol sessions — X3DH key agreement and Double Ratchet.
 * Handles session establishment, message encryption/decryption.
 */
@Singleton
class SignalSessionManager @Inject constructor(
    private val keyStorage: SignalKeyStorage,
    private val protocolStore: AppSignalProtocolStore
) {

    /**
     * Establish a Signal session with a remote user using their key bundle.
     * Uses X3DH key agreement protocol.
     */
    suspend fun establishSession(remoteUserId: String, bundle: FetchedKeyBundle): Boolean {
        return try {
            val address = SignalProtocolAddress(remoteUserId, 1)

            // Decode remote keys from Base64
            val remoteIdentityKey = IdentityKey(
                Base64.decode(bundle.identityKey, Base64.NO_WRAP), 0
            )
            val remoteSignedPreKey = Curve.decodePoint(
                Base64.decode(bundle.signedPreKey.publicKey, Base64.NO_WRAP), 0
            )
            val remoteSignedPreKeySignature = Base64.decode(
                bundle.signedPreKey.signature, Base64.NO_WRAP
            )

            val remoteOneTimePreKey: ECPublicKey? = bundle.oneTimePreKey?.let {
                Curve.decodePoint(Base64.decode(it.publicKey, Base64.NO_WRAP), 0)
            }

            // Build pre-key bundle for session establishment
            val preKeyBundle = if (remoteOneTimePreKey != null && bundle.oneTimePreKey != null) {
                PreKeyBundle(
                    bundle.registrationId,
                    1, // device ID
                    bundle.oneTimePreKey.keyId,
                    remoteOneTimePreKey,
                    bundle.signedPreKey.keyId,
                    remoteSignedPreKey,
                    remoteSignedPreKeySignature,
                    remoteIdentityKey
                )
            } else {
                PreKeyBundle(
                    bundle.registrationId,
                    1,
                    0,
                    null,
                    bundle.signedPreKey.keyId,
                    remoteSignedPreKey,
                    remoteSignedPreKeySignature,
                    remoteIdentityKey
                )
            }

            // Process X3DH
            val sessionBuilder = SessionBuilder(protocolStore, address)
            sessionBuilder.process(preKeyBundle)

            // Store remote identity
            keyStorage.saveRemoteIdentityKey(remoteUserId, remoteIdentityKey)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Encrypt a plaintext message for a recipient.
     * Returns Base64-encoded ciphertext.
     */
    fun encryptMessage(recipientId: String, plaintext: String): EncryptedMessage? {
        return try {
            val address = SignalProtocolAddress(recipientId, 1)
            val cipher = SessionCipher(protocolStore, address)
            val ciphertextMessage: CiphertextMessage = cipher.encrypt(plaintext.toByteArray(Charsets.UTF_8))

            EncryptedMessage(
                ciphertext = Base64.encodeToString(ciphertextMessage.serialize(), Base64.NO_WRAP),
                type = ciphertextMessage.type
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypt a received ciphertext message.
     * Returns the plaintext string.
     */
    fun decryptMessage(senderId: String, ciphertext: String, messageType: Int): String? {
        return try {
            val address = SignalProtocolAddress(senderId, 1)
            val cipher = SessionCipher(protocolStore, address)
            val ciphertextBytes = Base64.decode(ciphertext, Base64.NO_WRAP)

            val plaintext: ByteArray = when (messageType) {
                CiphertextMessage.PREKEY_TYPE -> {
                    val preKeyMessage = PreKeySignalMessage(ciphertextBytes)
                    cipher.decrypt(preKeyMessage)
                }
                CiphertextMessage.WHISPER_TYPE -> {
                    val signalMessage = SignalMessage(ciphertextBytes)
                    cipher.decrypt(signalMessage)
                }
                else -> return null
            }

            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Check if a session exists with a remote user.
     */
    fun hasSession(remoteUserId: String): Boolean {
        val address = SignalProtocolAddress(remoteUserId, 1)
        return protocolStore.containsSession(address)
    }
}

/**
 * Represents an encrypted message ready for transmission.
 */
data class EncryptedMessage(
    val ciphertext: String, // Base64-encoded
    val type: Int           // CiphertextMessage type (PREKEY_TYPE or WHISPER_TYPE)
)
