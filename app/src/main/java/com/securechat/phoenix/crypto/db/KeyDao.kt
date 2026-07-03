package com.securechat.phoenix.crypto.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KeyDao {
    // Identity Keys
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentityKey(entity: IdentityKeyEntity)

    @Query("SELECT * FROM identity_keys WHERE id = :id")
    suspend fun getIdentityKey(id: String): IdentityKeyEntity?

    // Remote Identity Keys
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteIdentityKey(entity: RemoteIdentityKeyEntity)

    @Query("SELECT * FROM remote_identity_keys WHERE user_id = :userId")
    suspend fun getRemoteIdentityKey(userId: String): RemoteIdentityKeyEntity?

    // Signed Pre-Keys
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignedPreKey(entity: SignedPreKeyEntity)

    @Query("SELECT * FROM signed_pre_keys WHERE key_id = :keyId")
    suspend fun getSignedPreKey(keyId: Int): SignedPreKeyEntity?

    @Query("SELECT * FROM signed_pre_keys WHERE is_current = 1 LIMIT 1")
    suspend fun getCurrentSignedPreKey(): SignedPreKeyEntity?

    @Query("UPDATE signed_pre_keys SET is_current = 0 WHERE key_id != :exceptKeyId")
    suspend fun markAllSignedPreKeysNotCurrent(exceptKeyId: Int)

    @Query("DELETE FROM signed_pre_keys WHERE key_id = :keyId")
    suspend fun deleteSignedPreKey(keyId: Int)

    // Pre-Keys
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreKeys(entities: List<PreKeyEntity>)

    @Query("SELECT * FROM pre_keys WHERE key_id = :keyId")
    suspend fun getPreKey(keyId: Int): PreKeyEntity?

    @Query("DELETE FROM pre_keys WHERE key_id = :keyId")
    suspend fun deletePreKey(keyId: Int)

    @Query("SELECT COUNT(*) FROM pre_keys")
    suspend fun getPreKeyCount(): Int

    // Metadata
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(entity: KeyMetadataEntity)

    @Query("SELECT * FROM key_metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): KeyMetadataEntity?
}
