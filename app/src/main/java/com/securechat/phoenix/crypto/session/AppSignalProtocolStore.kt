package com.securechat.phoenix.crypto.session

import com.securechat.phoenix.crypto.db.KeyDao
import com.securechat.phoenix.crypto.db.IdentityKeyEntity
import com.securechat.phoenix.crypto.db.PreKeyEntity
import com.securechat.phoenix.crypto.db.RemoteIdentityKeyEntity
import com.securechat.phoenix.crypto.db.SignedPreKeyEntity
import com.securechat.phoenix.crypto.db.KeyMetadataEntity
import android.util.Base64
import kotlinx.coroutines.runBlocking
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore
import org.signal.libsignal.protocol.state.SignalProtocolStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of Signal Protocol's combined store interface.
 * Bridges the Signal Protocol library with our encrypted Room database.
 *
 * Note: The Signal library calls these methods synchronously, so we use
 * runBlocking to bridge to our suspend DAO methods. In production, consider
 * a custom implementation that avoids main-thread blocking.
 */
@Singleton
class AppSignalProtocolStore @Inject constructor(
    private val keyDao: KeyDao,
    private val sessionDao: SessionDao
) : SignalProtocolStore {

    // --- IdentityKeyStore ---

    override fun getIdentityKeyPair(): IdentityKeyPair {
        return runBlocking {
            val entity = keyDao.getIdentityKey("local")
                ?: throw IllegalStateException("No identity key pair")
            val publicKey = IdentityKey(decode(entity.publicKey), 0)
            val privateKey = Curve.decodePrivatePoint(decode(entity.privateKey))
            IdentityKeyPair(publicKey, privateKey)
        }
    }

    override fun getLocalRegistrationId(): Int {
        return runBlocking {
            keyDao.getMetadata("registration_id")?.value?.toIntOrNull() ?: 0
        }
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        runBlocking {
            keyDao.insertRemoteIdentityKey(
                RemoteIdentityKeyEntity(
                    userId = address.name,
                    publicKey = encode(identityKey.serialize())
                )
            )
        }
        return true
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        // Trust on first use (TOFU)
        val stored = runBlocking { keyDao.getRemoteIdentityKey(address.name) }
        if (stored == null) return true
        val storedKey = IdentityKey(decode(stored.publicKey), 0)
        return storedKey == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return runBlocking {
            val entity = keyDao.getRemoteIdentityKey(address.name) ?: return@runBlocking null
            IdentityKey(decode(entity.publicKey), 0)
        }
    }

    // --- PreKeyStore ---

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return runBlocking {
            val entity = keyDao.getPreKey(preKeyId)
                ?: throw org.signal.libsignal.protocol.InvalidKeyIdException("No pre-key: $preKeyId")
            PreKeyRecord(decode(entity.record))
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        runBlocking {
            keyDao.insertPreKeys(listOf(PreKeyEntity(preKeyId, encode(record.serialize()))))
        }
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return runBlocking { keyDao.getPreKey(preKeyId) != null }
    }

    override fun removePreKey(preKeyId: Int) {
        runBlocking { keyDao.deletePreKey(preKeyId) }
    }

    // --- SignedPreKeyStore ---

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return runBlocking {
            val entity = keyDao.getSignedPreKey(signedPreKeyId)
                ?: throw org.signal.libsignal.protocol.InvalidKeyIdException("No signed pre-key: $signedPreKeyId")
            SignedPreKeyRecord(decode(entity.record))
        }
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        // Return just the current one
        return runBlocking {
            val entity = keyDao.getCurrentSignedPreKey() ?: return@runBlocking emptyList()
            listOf(SignedPreKeyRecord(decode(entity.record)))
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        runBlocking {
            keyDao.insertSignedPreKey(
                SignedPreKeyEntity(
                    keyId = signedPreKeyId,
                    record = encode(record.serialize()),
                    timestamp = record.timestamp,
                    isCurrent = true
                )
            )
        }
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return runBlocking { keyDao.getSignedPreKey(signedPreKeyId) != null }
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        runBlocking { keyDao.deleteSignedPreKey(signedPreKeyId) }
    }

    // --- SessionStore ---

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        return runBlocking {
            val entity = sessionDao.getSession(address.name, address.deviceId)
            if (entity != null) {
                SessionRecord(decode(entity.record))
            } else {
                SessionRecord()
            }
        }
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> {
        return addresses.map { loadSession(it) }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        return runBlocking { sessionDao.getSubDeviceSessions(name) }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        runBlocking {
            sessionDao.insertSession(
                SessionEntity(
                    recipientId = address.name,
                    deviceId = address.deviceId,
                    record = encode(record.serialize())
                )
            )
        }
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return runBlocking { sessionDao.getSession(address.name, address.deviceId) != null }
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        runBlocking { sessionDao.deleteSession(address.name, address.deviceId) }
    }

    override fun deleteAllSessions(name: String) {
        runBlocking { sessionDao.deleteAllSessions(name) }
    }

    // --- SenderKeyStore (required by SignalProtocolStore) ---

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: java.util.UUID, record: SenderKeyRecord) {
        // Not used for 1-on-1 messaging, no-op
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: java.util.UUID): SenderKeyRecord? {
        // Not used for 1-on-1 messaging
        return null
    }

    // --- KyberPreKeyStore (required by SignalProtocolStore, not used for classic X3DH) ---

    override fun loadKyberPreKey(kyberPreKeyId: Int): org.signal.libsignal.protocol.state.KyberPreKeyRecord {
        throw org.signal.libsignal.protocol.InvalidKeyIdException("Kyber pre-keys not supported")
    }

    override fun loadKyberPreKeys(): List<org.signal.libsignal.protocol.state.KyberPreKeyRecord> {
        return emptyList()
    }

    override fun storeKyberPreKey(kyberPreKeyId: Int, record: org.signal.libsignal.protocol.state.KyberPreKeyRecord) {
        // No-op
    }

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean {
        return false
    }

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int) {
        // No-op
    }

    // --- Encoding helpers ---
    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(base64: String): ByteArray = Base64.decode(base64, Base64.NO_WRAP)
}
