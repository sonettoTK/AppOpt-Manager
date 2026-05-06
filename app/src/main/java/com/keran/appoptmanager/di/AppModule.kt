package com.keran.appoptmanager.di

import android.content.Context
import com.keran.appoptmanager.data.AppConfigRepository
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.data.SettingsRepository
import com.keran.appoptmanager.data.UpdateRepository
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
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)

    @Provides
    @Singleton
    fun provideAppConfigRepository(
        @ApplicationContext context: Context
    ): AppConfigRepository = AppConfigRepository(context)

    @Provides
    @Singleton
    fun provideInstalledAppRepository(
        @ApplicationContext context: Context
    ): InstalledAppRepository = InstalledAppRepository(context)

    @Provides
    @Singleton
    fun provideUpdateRepository(): UpdateRepository = UpdateRepository()
}
