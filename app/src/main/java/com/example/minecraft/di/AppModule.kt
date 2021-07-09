package com.example.minecraft.di

import android.content.Context
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.AppUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton



@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    @Provides
    fun provideAppUtil(): AppUtil {
        return AppUtil()
    }

    @Provides
    @Singleton
    fun provideAppSharedPreferencesManager(@ApplicationContext context: Context): AppSharedPreferencesManager {
        return AppSharedPreferencesManager(context)
    }

    @ApplicationScope
    @Singleton
    @Provides
    fun provideAppScope() = CoroutineScope(SupervisorJob())

}
    @Retention(AnnotationRetention.RUNTIME)
    @Qualifier
    annotation class ApplicationScope
