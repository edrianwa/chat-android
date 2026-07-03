package com.securechat.phoenix.crypto

import com.securechat.phoenix.crypto.models.FetchedKeyBundle
import com.securechat.phoenix.crypto.models.KeyBundle
import com.securechat.phoenix.crypto.models.PreKeyData
import com.securechat.phoenix.crypto.models.SignedPreKeyData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for key bundle data model serialization and structure.
 */
class KeyBundleSerializationTest {

    @Test
    fun `KeyBundle holds all required fields`() {
        val bundle = KeyBundle(
            identityKey = "base64-identity-key",
            registrationId = 12345,
            signedPreKey = SignedPreKeyData(
                keyId = 1,
                publicKey = "base64-spk",
                signature = "base64-sig"
            ),
            oneTimePreKeys = listOf(
                PreKeyData(keyId = 1, publicKey = "otpk-1"),
                PreKeyData(keyId = 2, publicKey = "otpk-2")
            )
        )

        assertEquals("base64-identity-key", bundle.identityKey)
        assertEquals(12345, bundle.registrationId)
        assertEquals(1, bundle.signedPreKey.keyId)
        assertEquals("base64-spk", bundle.signedPreKey.publicKey)
        assertEquals("base64-sig", bundle.signedPreKey.signature)
        assertEquals(2, bundle.oneTimePreKeys.size)
    }

    @Test
    fun `KeyBundle can have empty one-time pre-keys`() {
        val bundle = KeyBundle(
            identityKey = "key",
            registrationId = 1,
            signedPreKey = SignedPreKeyData(1, "pk", "sig"),
            oneTimePreKeys = emptyList()
        )

        assertEquals(0, bundle.oneTimePreKeys.size)
    }

    @Test
    fun `FetchedKeyBundle with one-time pre-key`() {
        val fetched = FetchedKeyBundle(
            identityKey = "remote-identity",
            registrationId = 999,
            signedPreKey = SignedPreKeyData(5, "spk-pub", "spk-sig"),
            oneTimePreKey = PreKeyData(42, "otpk-42")
        )

        assertNotNull(fetched.oneTimePreKey)
        assertEquals(42, fetched.oneTimePreKey?.keyId)
    }

    @Test
    fun `FetchedKeyBundle without one-time pre-key`() {
        val fetched = FetchedKeyBundle(
            identityKey = "remote-identity",
            registrationId = 999,
            signedPreKey = SignedPreKeyData(5, "spk-pub", "spk-sig"),
            oneTimePreKey = null
        )

        assertNull(fetched.oneTimePreKey)
    }

    @Test
    fun `PreKeyData equality`() {
        val key1 = PreKeyData(1, "abc")
        val key2 = PreKeyData(1, "abc")
        assertEquals(key1, key2)
    }

    @Test
    fun `SignedPreKeyData equality`() {
        val spk1 = SignedPreKeyData(1, "pub", "sig")
        val spk2 = SignedPreKeyData(1, "pub", "sig")
        assertEquals(spk1, spk2)
    }

    @Test
    fun `KeyBundle with 100 pre-keys`() {
        val preKeys = (1..100).map { PreKeyData(it, "key-$it") }
        val bundle = KeyBundle(
            identityKey = "identity",
            registrationId = 42,
            signedPreKey = SignedPreKeyData(1, "spk", "sig"),
            oneTimePreKeys = preKeys
        )

        assertEquals(100, bundle.oneTimePreKeys.size)
        assertEquals(1, bundle.oneTimePreKeys.first().keyId)
        assertEquals(100, bundle.oneTimePreKeys.last().keyId)
    }
}
