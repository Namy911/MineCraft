package com.example.minecraft.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TaskModel(
    val id: Int,
    val image: String,
    val desc: String
) : Parcelable {
}