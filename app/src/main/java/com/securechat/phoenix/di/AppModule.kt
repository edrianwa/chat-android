package com.securechat.phoenix.di

import android.content.Context
import com.securechat.phoenix.data.PasscodeRepository
import com.securechat.phoenix.data.PasscodeRepositoryImpl
import com.securechat.phoenix.data.SecurePasscodeStore
import com.securechat.phoenix.navigation.PasscodeRouter
import com.securechat.phoenix.navigation.PasscodeRouterImpl
import com.securechat.phoenix.navigation.ScreenRegistry
import com.securechat.phoenix.navigation.ScreenRegistryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecurePasscodeStore(
        @ApplicationContext context: Context
    ): SecurePasscodeStore {
        return SecurePasscodeStore(context)
    }

    @Provides
    @Singleton
    fun providePasscodeRepository(
        secureStore: SecurePasscodeStore
    ): PasscodeRepository {
        return PasscodeRepositoryImpl(secureStore)
    }

    @Provides
    @Singleton
    fun provideScreenRegistry(): ScreenRegistry {
        return ScreenRegistryImpl()
    }

    @Provides
    @Singleton
    fun providePasscodeRouter(
        passcodeRepository: PasscodeRepository,
        screenRegistry: ScreenRegistry
    ): PasscodeRouter {
        return PasscodeRouterImpl(passcodeRepository, screenRegistry)
    }
}
