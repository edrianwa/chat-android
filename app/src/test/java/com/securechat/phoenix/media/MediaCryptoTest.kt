package com.securechat.phoenix.media

import com.securechat.phoenix.media.crypto.MediaCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCryptoTest {

    @Test
    fun `generateKey produces 32-byte key`() {
        val key = MediaCrypto.generateKey()
        assertEquals(32, key.size) // 256 bits
    }

    @Test
    fun `generateKey produces unique keys`() {
        val key1 = MediaCrypto.generateKey()
        val key2 = MediaCrypto.generateKey()
        assertNotEquals(key1.toList(), key2.toList())
    }

    @Test
    fun `encrypt produces different output than input`() {
        val plaintext = "Hello, secure media!".toByteArray()
        val key = MediaCrypto.generateKey()
        val encrypted = MediaCrypto.encrypt(plaintext, key)

        assertTrue(encrypted.encryptedBytes.size > plaintext.size)
        assertNotEquals(plaintext.toList(), encrypted.encryptedBytes.toList())
    }

    @Test
    fun `encrypt output includes IV prefix (12 bytes)`() {
        val plaintext = "test data".toByteArray()
        val key = MediaCrypto.generateKey()
        val encrypted = MediaCrypto.encrypt(plaintext, key)

        // IV (12) + ciphertext (at least input length) + GCM tag (16)
        assertTrue(encrypted.encryptedBytes.size >= 12 + plaintext.size + 16)
    }

    @Test
    fun `decrypt recovers original plaintext`() {
        val originalData = "This is a test image file content".toByteArray()
        val key = MediaCrypto.generateKey()

        val encrypted = MediaCrypto.encrypt(originalData, key)
        val decrypted = MediaCrypto.decrypt(encrypted.encryptedBytes, key)

        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun `decrypt with large data works correctly`() {
        // Simulate a 1MB file
        val largeData = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val key = MediaCrypto.generateKey()

        val encrypted = MediaCrypto.encrypt(largeData, key)
        val decrypted = MediaCrypto.decrypt(encrypted.encryptedBytes, key)

        assertArrayEquals(largeData, decrypted)
    }

    @Test(expected = Exception::class)
    fun `decrypt with wrong key throws`() {
        val plaintext = "secret data".toByteArray()
        val correctKey = MediaCrypto.generateKey()
        val wrongKey = MediaCrypto.generateKey()

        val encrypted = MediaCrypto.encrypt(plaintext, correctKey)
        MediaCrypto.decrypt(encrypted.encryptedBytes, wrongKey)
    }

    @Test(expected = Exception::class)
    fun `decrypt with corrupted data throws`() {
        val plaintext = "data".toByteArray()
        val key = MediaCrypto.generateKey()
        val encrypted = MediaCrypto.encrypt(plaintext, key)

        // Corrupt a byte in the ciphertext
        val corrupted = encrypted.encryptedBytes.clone()
        corrupted[20] = (corrupted[20] + 1).toByte()

        MediaCrypto.decrypt(corrupted, key)
    }

    @Test
    fun `same plaintext with same key produces different ciphertext (random IV)`() {
        val plaintext = "determinism test".toByteArray()
        val key = MediaCrypto.generateKey()

        val enc1 = MediaCrypto.encrypt(plaintext, key)
        val enc2 = MediaCrypto.encrypt(plaintext, key)

        // Should be different because of random IV
        assertNotEquals(enc1.encryptedBytes.toList(), enc2.encryptedBytes.toList())

        // But both decrypt to same plaintext
        assertArrayEquals(plaintext, MediaCrypto.decrypt(enc1.encryptedBytes, key))
        assertArrayEquals(plaintext, MediaCrypto.decrypt(enc2.encryptedBytes, key))
    }

    @Test
    fun `EncryptedMedia stores key correctly`() {
        val key = MediaCrypto.generateKey()
        val encrypted = MediaCrypto.encrypt("test".toByteArray(), key)

        assertArrayEquals(key, encrypted.key)
    }

    @Test
    fun `MediaPayload holds all required metadata`() {
        val payload = MediaPayload(
            mediaId = "uuid-media-1",
            mediaUrl = "/api/media/uuid-media-1",
            encryptionKey = "base64-aes-key",
            mimeType = "image/webp",
            fileSize = 102400,
            width = 1080,
            height = 1920,
            thumbnail = "base64-thumbnail"
        )

        assertEquals("uuid-media-1", payload.mediaId)
        assertEquals("image/webp", payload.mimeType)
        assertEquals(102400, payload.fileSize)
        assertEquals(1080, payload.width)
        assertEquals(1920, payload.height)
        assertTrue(payload.thumbnail.isNotEmpty())
    }
}
