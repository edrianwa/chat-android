package com.securechat.phoenix.media.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM file encryption/decryption for media attachments.
 * Each file gets a unique random key that is shared via the Signal session.
 */
object MediaCrypto {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE_BYTES = 12

    /**
     * Generate a random AES-256 key for encrypting a media file.
     * Returns the key bytes (32 bytes).
     */
    fun generateKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(KEY_SIZE_BITS)
        return keyGen.generateKey().encoded
    }

    /**
     * Encrypt a file's bytes with AES-256-GCM.
     * Returns EncryptedMedia containing IV prepended to ciphertext + the key.
     */
    fun encrypt(plainBytes: ByteArray, key: ByteArray): EncryptedMedia {
        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey: SecretKey = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plainBytes)

        // Prepend IV to ciphertext for self-contained decryption
        val output = ByteArray(IV_SIZE_BYTES + ciphertext.size)
        System.arraycopy(iv, 0, output, 0, IV_SIZE_BYTES)
        System.arraycopy(ciphertext, 0, output, IV_SIZE_BYTES, ciphertext.size)

        return EncryptedMedia(
            encryptedBytes = output,
            key = key
        )
    }

    /**
     * Decrypt an encrypted file. Input is IV + ciphertext (as produced by encrypt).
     */
    fun decrypt(encryptedBytes: ByteArray, key: ByteArray): ByteArray {
        require(encryptedBytes.size > IV_SIZE_BYTES) { "Encrypted data too short" }

        val iv = encryptedBytes.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = encryptedBytes.copyOfRange(IV_SIZE_BYTES, encryptedBytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey: SecretKey = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }
}

/**
 * Result of encrypting a media file.
 */
data class EncryptedMedia(
    val encryptedBytes: ByteArray, // IV + ciphertext
    val key: ByteArray             // AES-256 key to share with recipient via Signal
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedMedia) return false
        return encryptedBytes.contentEquals(other.encryptedBytes) && key.contentEquals(other.key)
    }

    override fun hashCode(): Int {
        return 31 * encryptedBytes.contentHashCode() + key.contentHashCode()
    }
}
