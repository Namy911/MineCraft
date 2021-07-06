package com.example.minecraft.di

import android.content.Context
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @ActivityScoped
//    fun provideTrialManager(@ApplicationContext context: Context, @ApplicationContext scope: CoroutineScope): BillingManager{
    fun provideTrialManager(@ApplicationContext context: Context): BillingManager{
        return BillingManager(context)
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
