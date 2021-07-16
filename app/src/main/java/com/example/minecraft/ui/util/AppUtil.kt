package com.example.minecraft.ui.util

import android.content.Context
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.animation.content.Content
import com.google.android.gms.ads.interstitial.InterstitialAd
import java.io.InputStream

class AppUtil {
    companion object{
        const val REVARD_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val REVARD_AD_ID = "ca-app-pub-3940256099942544/2247696110"
        const val INTERSTIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val APP_OPEN_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"
    }

    fun readTextFile(context: Context, @RawRes resource: Int): String {
        val inputStream: InputStream = context.resources.openRawResource(resource)
        return inputStream.bufferedReader().use { it.readText() }
    }
}


// adb shell pm clear com.android.vending