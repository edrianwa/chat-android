package com.securechat.phoenix.crypto.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identity_keys")
data class IdentityKeyEntity(
    @PrimaryKey val id: String, // "local" for own key pair
    @ColumnInfo(name = "public_key") val publicKey: String,
    @ColumnInfo(name = "private_key") val privateKey: String
)

@Entity(tableName = "remote_identity_keys")
data class RemoteIdentityKeyEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "public_key") val publicKey: String
)

@Entity(tableName = "signed_pre_keys")
data class SignedPreKeyEntity(
    @PrimaryKey @ColumnInfo(name = "key_id") val keyId: Int,
    val record: String, // Serialized SignedPreKeyRecord (Base64)
    val timestamp: Long,
    @ColumnInfo(name = "is_current") val isCurrent: Boolean = true
)

@Entity(tableName = "pre_keys")
data class PreKeyEntity(
    @PrimaryKey @ColumnInfo(name = "key_id") val keyId: Int,
    val record: String // Serialized PreKeyRecord (Base64)
)

@Entity(tableName = "key_metadata")
data class KeyMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)
