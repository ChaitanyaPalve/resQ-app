package com.phoenix.phoenixnet.db.di

import android.content.Context
import com.phoenix.phoenixnet.db.VoicePlayerHelper
import com.phoenix.phoenixnet.db.VoiceRecorderHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideVoiceRecorderHelper(@ApplicationContext context: Context): VoiceRecorderHelper {
        return VoiceRecorderHelper(context)
    }

    @Provides
    @Singleton
    fun provideVoicePlayerHelper(@ApplicationContext context: Context): VoicePlayerHelper {
        return VoicePlayerHelper(context)
    }
}
