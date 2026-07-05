package com.securechat.phoenix.di

import android.content.Context
import com.securechat.phoenix.security.PanicWipeManager
import com.securechat.phoenix.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun providePanicWipeManager(@ApplicationContext context: Context): PanicWipeManager {
        return PanicWipeManager(context)
    }
}
