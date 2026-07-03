package com.securechat.phoenix.crypto.models

/**
 * Represents a full key bundle to be uploaded to the server.
 */
data class KeyBundle(
    val identityKey: String, // Base64-encoded public identity key
    val registrationId: Int,
    val signedPreKey: SignedPreKeyData,
    val oneTimePreKeys: List<PreKeyData>
)

/**
 * Signed pre-key data for transport/storage.
 */
data class SignedPreKeyData(
    val keyId: Int,
    val publicKey: String, // Base64-encoded
    val signature: String  // Base64-encoded
)

/**
 * One-time pre-key data for transport/storage.
 */
data class PreKeyData(
    val keyId: Int,
    val publicKey: String // Base64-encoded
)

/**
 * A fetched key bundle from another user (server response).
 */
data class FetchedKeyBundle(
    val identityKey: String,
    val registrationId: Int,
    val signedPreKey: SignedPreKeyData,
    val oneTimePreKey: PreKeyData?
)

/**
 * Server response for key count.
 */
data class KeyCountResponse(
    val count: Int,
    val threshold: Int
)
