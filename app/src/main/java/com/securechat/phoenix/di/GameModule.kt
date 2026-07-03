package com.securechat.phoenix.di

import android.content.Context
import com.securechat.phoenix.game.data.GamePreferences
import com.securechat.phoenix.game.sound.GameSoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    @Provides
    @Singleton
    fun provideGamePreferences(
        @ApplicationContext context: Context
    ): GamePreferences {
        return GamePreferences(context)
    }

    @Provides
    @Singleton
    fun provideGameSoundManager(
        @ApplicationContext context: Context
    ): GameSoundManager {
        return GameSoundManager(context)
    }
}
