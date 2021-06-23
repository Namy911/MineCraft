package com.example.minecraft.data.model

import android.os.Parcelable
import androidx.room.*
import com.example.minecraft.data.network.AddonEntity
import com.example.minecraft.ui.util.RosterItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.parcelize.Parcelize


@Entity(tableName = "addon")
@Parcelize
data class AddonModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id", index = true)
    var id: Int = 0,
    val behavior: String,
    val description: String,
    val image: String,
    val preview: List<String>,
    val resource: String,
    val title: String,
) : Parcelable, RosterItem(RosterItem.TYPE.ADDON) {
    fun convertToAddonEntity(model: AddonModel): AddonEntity{
        return AddonEntity(
            behavior  = model.behavior,
            description = model.description,
            image  = model.image,
            preview = model.preview,
            resource = model.resource,
            title = model.title
        )
    }
    fun convertToAddonModel(model: AddonEntity): AddonModel{
        return AddonModel(
            behavior  = model.behavior,
            description = model.description,
            image  = model.image,
            preview = model.preview,
            resource = model.resource,
            title = model.title,
        )
    }
    @Dao
    interface Store{
        @Query("SELECT * FROM `addon` WHERE  `_id` = :id")
         suspend fun getOne(id: Int): AddonModel

//        @Query("SELECT * FROM `addon`  LIMIT :limit  OFFSET :offset  ")
//        suspend fun getLimit(offset: Int, limit: Int): List<AddonModel>
        @Query("SELECT * FROM `addon`  LIMIT :limit  OFFSET :offset  ")
        fun getLimit(offset: Int, limit: Int): Flow<List<AddonModel>>

        @Query("SELECT * FROM `addon` ")
        fun getAll(): Flow<List<AddonModel>>
        fun getAllDistinct() = getAll().distinctUntilChanged()

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(items: List<AddonModel>)

        @Delete
        suspend fun delete(model: List<AddonModel>)

        @Query("DELETE FROM `addon`")
        suspend fun deleteAll()

        @Transaction
        suspend fun initialization(items: List<AddonModel>){
            deleteAll()
            insertAll(items)
        }
    }
}
