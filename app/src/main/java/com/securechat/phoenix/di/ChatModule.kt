package com.securechat.phoenix.di

import android.content.Context
import com.securechat.phoenix.chat.data.ChatDatabase
import com.securechat.phoenix.chat.data.ChatRepository
import com.securechat.phoenix.chat.data.ContactDao
import com.securechat.phoenix.chat.data.MessageDao
import com.securechat.phoenix.chat.network.ChatSocketClient
import com.securechat.phoenix.crypto.network.KeyBundleRepository
import com.securechat.phoenix.crypto.session.SessionDao
import com.securechat.phoenix.crypto.session.SignalSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase {
        return ChatDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: ChatDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: ChatDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        messageDao: MessageDao,
        sessionManager: SignalSessionManager,
        keyBundleRepository: KeyBundleRepository
    ): ChatRepository {
        return ChatRepository(messageDao, sessionManager, keyBundleRepository)
    }

    @Provides
    @Singleton
    fun provideChatSocketClient(
        chatRepository: ChatRepository,
        tokenManager: com.securechat.phoenix.auth.TokenManager
    ): ChatSocketClient {
        return ChatSocketClient(chatRepository, tokenManager)
    }
}
