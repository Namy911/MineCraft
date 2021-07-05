package com.example.minecraft.ui.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.Purchase.PurchasesResult


class TrialManager(val context: Context) {
    companion object {
        private const val TAG = "TrialManager"
        const val TRIAL_PRODUCT = "ui.util.trial.exist"
        const val TRIAL_PRODUCT_MONTH = "ui.util.trial.not.exist"
        const val TRIAL_PRODUCT_YEAR = "ui.util.trial.name"
        const val PRODUCT_TYPE = BillingClient.SkuType.INAPP
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            // realizarea cumparaturii
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases){
                    handlePurchase(purchase)
                    Log.d(TAG, "purchasesUpdatedListener: ")
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else {
                // Handle any other error codes.
            }
        }

    private val acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK){
            //cumparat, redirect
            Log.d(TAG, "acknowledgePurchaseResponseListener: ")
        }
    }


    private val consumeResponseListener =  ConsumeResponseListener { billingResult, s ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK){
            //cumparat, redirect
            Log.d(TAG, "ConsumeResponseListener: ")
        }
    }



    var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()
//        .startConnection(object : BillingClientStateListener {
//            override fun onBillingSetupFinished(billingResult: BillingResult) {
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    querySkuDetails()
//                }
//            }
//            override fun onBillingServiceDisconnected() {
//            }
//        })

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                }
            }
            override fun onBillingServiceDisconnected() {
//                billingClient.startConnection(this)
            }
        })
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(
                    acknowledgePurchaseParams, acknowledgePurchaseResponseListener
                )
            }
        }
        if (!purchase.isAcknowledged) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.consumeAsync(
                            consumeParams, consumeResponseListener
                    )
                }
            }
    }
    // onClick ??????
    fun querySkuDetails() {
        val skuList = ArrayList<String>()
        skuList.add(TRIAL_PRODUCT)
        skuList.add(TRIAL_PRODUCT_MONTH)
        skuList.add(TRIAL_PRODUCT_YEAR)

        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(PRODUCT_TYPE)

//        withContext(Dispatchers.IO) {
            billingClient.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
                if (skuDetailsList != null) {
                    for(skuDetails in skuDetailsList) {
//                        buyItem(skuDetails)
                    }
                }
            }
        }

//    fun buyItem(skuDetails: SkuDetails) {
//        val flowParams = BillingFlowParams.newBuilder()
//            .setSkuDetails(skuDetails)
//            .build()
//            billingClient.launchBillingFlow(context as Activity, flowParams).responseCode
//    }
}