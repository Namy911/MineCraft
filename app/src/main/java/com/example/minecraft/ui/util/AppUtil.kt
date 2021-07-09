package com.example.minecraft.ui.util

import android.content.Context
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.animation.content.Content
import java.io.InputStream

class AppUtil {
    companion object{
        const val REVARD_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    fun readTextFile(context: Context, @RawRes resource: Int): String {
        val inputStream: InputStream = context.resources.openRawResource(resource)
        return inputStream.bufferedReader().use { it.readText() }
    }
}