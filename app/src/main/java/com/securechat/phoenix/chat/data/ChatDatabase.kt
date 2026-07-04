package com.securechat.phoenix.chat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.securechat.phoenix.crypto.session.SessionEntity
import com.securechat.phoenix.crypto.session.SessionDao
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Encrypted Room database for chat messages and Signal sessions.
 */
@Database(
    entities = [
        MessageEntity::class,
        PendingMessageEntity::class,
        ContactEntity::class,
        SessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun sessionDao(): SessionDao

    companion object {
        private const val DATABASE_NAME = "chat_messages.db"
        private const val KEYSTORE_ALIAS = "phoenix_chat_db_key"

        @Volatile
        private var instance: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): ChatDatabase {
            val passphrase = getOrCreateDatabaseKey()
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun getOrCreateDatabaseKey(): ByteArray {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
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
