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
) : Parcelable

fun AddonEntity.toAddonModel (): AddonModel {
    return AddonModel(
        behavior = this.behavior,
        description = this.description,
        image = this.image,
        preview = this.preview,
        resource = this.resource,
        title = this.title
    )
}