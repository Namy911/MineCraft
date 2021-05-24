package com.example.minecraft.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.minecraft.data.db.util.Converter
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.network.AddonEntity

@Database(entities = [AddonModel::class, AddonEntity::class], version = 1)
@TypeConverters(Converter::class)
abstract class TaskDataBase : RoomDatabase() {
    abstract fun addonStore(): AddonModel.Store
}