package com.example.minecraft.data.network

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.minecraft.data.model.AddonModel
import kotlinx.android.parcel.Parcelize


@Parcelize
data class AddonEntity(
    val behavior: String,
    val description: String,
    val image: String,
    val preview: List<String>,
    val resource: String,
    @PrimaryKey
    val title: String
) : Parcelable {
    fun convertToAddonEntity(model: AddonModel): AddonEntity {
        return AddonEntity(
            behavior = model.behavior,
            description = model.description,
            image = model.image,
            preview = model.preview,
            resource = model.resource,
            title = model.title
        )
    }

    fun convertToAddonModel(model: AddonEntity): AddonModel {
        return AddonModel(
            behavior = model.behavior,
            description = model.description,
            image = model.image,
            preview = model.preview,
            resource = model.resource,
            title = model.title
        )
    }
}