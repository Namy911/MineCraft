package com.example.minecraft.di

import android.app.Activity
import android.app.Application
import android.content.Context
import com.example.minecraft.ui.util.TrialManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun provideTrialManager(@ApplicationContext context: Context): TrialManager{
        return TrialManager(context)
    }
}