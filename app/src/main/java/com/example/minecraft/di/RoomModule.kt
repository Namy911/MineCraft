package com.example.minecraft.di

import android.content.Context
import androidx.room.Room
import com.example.minecraft.data.db.TaskDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, TaskDataBase::class.java, "myBd.db")
            .build()

    @Singleton
    @Provides
    fun providePersonStore(db: TaskDataBase) = db.addonStore()
}