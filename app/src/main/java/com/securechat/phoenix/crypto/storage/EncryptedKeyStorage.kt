package com.securechat.phoenix.crypto.storage

import android.util.Base64
import com.securechat.phoenix.crypto.db.KeyDao
import com.securechat.phoenix.crypto.db.IdentityKeyEntity
import com.securechat.phoenix.crypto.db.PreKeyEntity
import com.securechat.phoenix.crypto.db.RemoteIdentityKeyEntity
import com.securechat.phoenix.crypto.db.SignedPreKeyEntity
import com.securechat.phoenix.crypto.db.KeyMetadataEntity
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted implementation of SignalKeyStorage using Room + SQLCipher.
 * All keys are stored in an encrypted database backed by Android Keystore.
 */
@Singleton
class EncryptedKeyStorage @Inject constructor(
    private val keyDao: KeyDao
) : SignalKeyStorage {

    companion object {
        private const val KEY_REGISTRATION_ID = "registration_id"
        private const val KEY_NEXT_PRE_KEY_ID = "next_pre_key_id"
        private const val KEY_IDENTITY_PUBLIC = "identity_public"
        private const val KEY_IDENTITY_PRIVATE = "identity_private"
    }

    // --- Identity Key Pair ---

    override suspend fun saveIdentityKeyPair(keyPair: IdentityKeyPair) {
        keyDao.insertIdentityKey(
            IdentityKeyEntity(
                id = "local",
                publicKey = encode(keyPair.publicKey.serialize()),
                privateKey = encode(keyPair.privateKey.serialize())
            )
        )
    }

    override suspend fun getIdentityKeyPair(): IdentityKeyPair? {
        val entity = keyDao.getIdentityKey("local") ?: return null
        val publicKey = IdentityKey(decode(entity.publicKey), 0)
        val privateKey = Curve.decodePrivatePoint(decode(entity.privateKey))
        return IdentityKeyPair(publicKey, privateKey)
    }

    override suspend fun saveRemoteIdentityKey(userId: String, identityKey: IdentityKey) {
        keyDao.insertRemoteIdentityKey(
            RemoteIdentityKeyEntity(
                userId = userId,
                publicKey = encode(identityKey.serialize())
            )
        )
    }

    override suspend fun getRemoteIdentityKey(userId: String): IdentityKey? {
        val entity = keyDao.getRemoteIdentityKey(userId) ?: return null
        return IdentityKey(decode(entity.publicKey), 0)
    }

    // --- Registration ID ---

    override suspend fun saveRegistrationId(id: Int) {
        keyDao.insertMetadata(KeyMetadataEntity(KEY_REGISTRATION_ID, id.toString()))
    }

    override suspend fun getRegistrationId(): Int? {
        return keyDao.getMetadata(KEY_REGISTRATION_ID)?.value?.toIntOrNull()
    }

    // --- Signed Pre-Keys ---

    override suspend fun saveSignedPreKey(record: SignedPreKeyRecord) {
        keyDao.insertSignedPreKey(
            SignedPreKeyEntity(
                keyId = record.id,
                record = encode(record.serialize()),
                timestamp = record.timestamp,
                isCurrent = true
            )
        )
        // Mark previous ones as not current
        keyDao.markAllSignedPreKeysNotCurrent(record.id)
    }

    override suspend fun getSignedPreKey(keyId: Int): SignedPreKeyRecord? {
        val entity = keyDao.getSignedPreKey(keyId) ?: return null
        return SignedPreKeyRecord(decode(entity.record))
    }

    override suspend fun getCurrentSignedPreKey(): SignedPreKeyRecord? {
        val entity = keyDao.getCurrentSignedPreKey() ?: return null
        return SignedPreKeyRecord(decode(entity.record))
    }

    override suspend fun removeSignedPreKey(keyId: Int) {
        keyDao.deleteSignedPreKey(keyId)
    }

    // --- One-Time Pre-Keys ---

    override suspend fun savePreKeys(records: List<PreKeyRecord>) {
        val entities = records.map { record ->
            PreKeyEntity(
                keyId = record.id,
                record = encode(record.serialize())
            )
        }
        keyDao.insertPreKeys(entities)
    }

    override suspend fun getPreKey(keyId: Int): PreKeyRecord? {
        val entity = keyDao.getPreKey(keyId) ?: return null
        return PreKeyRecord(decode(entity.record))
    }

    override suspend fun removePreKey(keyId: Int) {
        keyDao.deletePreKey(keyId)
    }

    override suspend fun getPreKeyCount(): Int {
        return keyDao.getPreKeyCount()
    }

    // --- Pre-Key ID tracking ---

    override suspend fun saveNextPreKeyId(id: Int) {
        keyDao.insertMetadata(KeyMetadataEntity(KEY_NEXT_PRE_KEY_ID, id.toString()))
    }

    override suspend fun getNextPreKeyId(): Int {
        return keyDao.getMetadata(KEY_NEXT_PRE_KEY_ID)?.value?.toIntOrNull() ?: 1
    }

    // --- Encoding helpers ---

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(base64: String): ByteArray =
        Base64.decode(base64, Base64.NO_WRAP)
}
