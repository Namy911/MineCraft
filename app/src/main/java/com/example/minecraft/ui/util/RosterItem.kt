package com.example.minecraft.ui.util

import com.example.minecraft.data.model.AddonModel
import com.google.android.gms.ads.nativead.NativeAd

abstract class RosterItem(
    var rosterType: String
){
    class TYPE {
        companion object {
            val FOOTER = "footer"
            val ADDON = "addon"
            val ADS = "ads"
        }
    }
}
class FooterItem: RosterItem(TYPE.FOOTER)

class AdsItem(val ads: NativeAd): RosterItem(TYPE.ADS)