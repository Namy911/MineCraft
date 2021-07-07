package com.example.minecraft.ui.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.example.minecraft.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject


class BillingManager @Inject constructor(
    private val activity: Context
//    @ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BillingFragment"

        const val TRIAL_PRODUCT_YEAR = "android.test.purchased"
        const val TRIAL_PRODUCT_YEAR2 = "android.test.purchased"
        const val TRIAL_PRODUCT_CANCEL = "android.test.canceled"
        const val TRIAL_PRODUCT_REFUNDED = "android.test.refunded"
        const val TRIAL_PRODUCT_NO_ADVAILABLE = "android.test.item_unavailable"

        const val PRODUCT_TYPE = BillingClient.SkuType.INAPP
        const val SUBS_TYPE = BillingClient.SkuType.SUBS
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
//        else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED && purchases != null) {
//            Log.d(TAG, "BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED: ")
//        }
//        Log.d(TAG, "purchasesUpdatedListener: ${billingResult.responseCode}")
    }

//    private val purchasesResponseListener = PurchasesResponseListener { billingResult, purchases ->
//        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//        Log.d(TAG, "purchasesResponseListener: {${purchases.size}}")
//            for (item in purchases) {
//                if (!item.skus.contains(TRIAL_PRODUCT_YEAR)) {
//                    querySkuDetails()
//                } else {
//                    Log.d(TAG, "purchasesResponseListener")
//                }
//            }
//        }
//    }
//    @Inject
    var sharedPreferencesManager = AppSharedPreferencesManager(activity)

    private val acknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                CoroutineScope(SupervisorJob()).launch {
                    sharedPreferencesManager.setBillingAdsSate(true)
                }
                Log.d(TAG, "acknowledgePurchaseResponseListener: ")
            }
        }



    private val consumeResponseListener = ConsumeResponseListener { billingResult, s: String ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            //cumparat, redirect
            Log.d(TAG, "ConsumeResponseListener: ")
        }
    }

    var billingClient = BillingClient.newBuilder(activity)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
//                billingClient.queryPurchasesAsync(PRODUCT_TYPE, purchasesResponseListener)
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val list = billingClient.queryPurchases(PRODUCT_TYPE)
                    val result = checkItemAvailability(list)
                    if (result) {
                        querySkuDetails()
                    } else {
                        Log.d(TAG, "onBillingSetupFinished: Exist item")
                    }
                }
//                else if (billingResult.responseCode != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ) {
//                }
            }

            override fun onBillingServiceDisconnected() {
//                this@BillingManager.startConnection() //???
            }
        })
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.isAcknowledged) {
                setAcknowledgePurchase(purchase)
            } else {
                for (sku in purchase.skus) {
                    if (sku == TRIAL_PRODUCT_YEAR) {
                        setAcknowledgePurchase(purchase)
                    }
//                    else {
//                        setConsumePurchase(purchase)
//                    }
                }
            }
        }
    }

    private fun setAcknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(
            acknowledgePurchaseParams, acknowledgePurchaseResponseListener
        )
    }

//    private fun setConsumePurchase(purchase: Purchase) {
//        val consumeParams = ConsumeParams.newBuilder()
//            .setPurchaseToken(purchase.purchaseToken)
//            .build()
//        billingClient.consumeAsync(
//            consumeParams, consumeResponseListener
//        )
//    }

    //
    fun querySkuDetails() {
        val skuList = ArrayList<String>()
        skuList.add(TRIAL_PRODUCT_YEAR)

        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(PRODUCT_TYPE)

        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            billingClient.querySkuDetailsAsync(params.build()) { _, skuDetailsList ->
                if (skuDetailsList != null) {
                    for (skuDetails in skuDetailsList) {
                        buyItem(skuDetails)
                    }
                }
            }
        }
    }

    private fun checkItemAvailability(purchase: Purchase.PurchasesResult): Boolean {
        val items: MutableList<Purchase>? = purchase.purchasesList
        if (items != null) {
            for (item in items) {
                if (item.skus.contains(TRIAL_PRODUCT_YEAR)) {
                    return false
                }
            }
        }
        return true
    }

    private fun buyItem(skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        billingClient.launchBillingFlow(activity as AppCompatActivity, flowParams)
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}