package com.example.minecraft.ui.util

import android.content.Context
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.animation.content.Content
import java.io.InputStream

class AppUtil {

    fun readTextFile(context: Context, @RawRes resource: Int): String {
        val inputStream: InputStream = context.resources.openRawResource(resource)
        return inputStream.bufferedReader().use { it.readText() }
    }
}