package com.securechat.phoenix.di

import android.content.Context
import com.securechat.phoenix.crypto.KeyManager
import com.securechat.phoenix.crypto.db.KeyDao
import com.securechat.phoenix.crypto.db.SignalKeyDatabase
import com.securechat.phoenix.crypto.network.KeyBundleApi
import com.securechat.phoenix.crypto.network.KeyBundleRepository
import com.securechat.phoenix.crypto.storage.EncryptedKeyStorage
import com.securechat.phoenix.crypto.storage.SignalKeyStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideSignalKeyDatabase(
        @ApplicationContext context: Context
    ): SignalKeyDatabase {
        return SignalKeyDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideKeyDao(database: SignalKeyDatabase): KeyDao {
        return database.keyDao()
    }

    @Provides
    @Singleton
    fun provideSignalKeyStorage(keyDao: KeyDao): SignalKeyStorage {
        return EncryptedKeyStorage(keyDao)
    }

    @Provides
    @Singleton
    fun provideKeyManager(keyStorage: SignalKeyStorage): KeyManager {
        return KeyManager(keyStorage)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // localhost from Android emulator
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideKeyBundleApi(retrofit: Retrofit): KeyBundleApi {
        return retrofit.create(KeyBundleApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKeyBundleRepository(api: KeyBundleApi): KeyBundleRepository {
        return KeyBundleRepository(api)
    }
}
