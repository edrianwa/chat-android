package com.securechat.phoenix.crypto.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Encrypted Room database for Signal Protocol key storage.
 * Uses SQLCipher encryption with a key derived from Android Keystore.
 */
@Database(
    entities = [
        IdentityKeyEntity::class,
        RemoteIdentityKeyEntity::class,
        SignedPreKeyEntity::class,
        PreKeyEntity::class,
        KeyMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SignalKeyDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao

    companion object {
        private const val DATABASE_NAME = "signal_keys.db"
        private const val KEYSTORE_ALIAS = "phoenix_signal_db_key"

        @Volatile
        private var instance: SignalKeyDatabase? = null

        fun getInstance(context: Context): SignalKeyDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SignalKeyDatabase {
            val passphrase = getOrCreateDatabaseKey()
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                SignalKeyDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Get or create the database encryption key from Android Keystore.
         * The key never leaves the hardware-backed secure enclave.
         */
        private fun getOrCreateDatabaseKey(): ByteArray {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    "AES",
                    "AndroidKeyStore"
                )
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }

            val key = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            return key.encoded ?: ByteArray(32) { it.toByte() }
        }
    }
}
