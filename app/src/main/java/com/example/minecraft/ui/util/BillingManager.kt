package com.example.minecraft.ui.util

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import kotlinx.coroutines.*


//class BillingManager @Inject constructor(
class BillingManager (
    private val activity: Context,
    private val redirect: () -> Unit
//    @ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BillingManager"

        const val TRIAL_PRODUCT_YEAR = "android.test.purchased"
        const val PRODUCT_TYPE = BillingClient.SkuType.INAPP

        var BILLING_FLAG_STATE = false
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            redirect.invoke()
        }
    }

    var sharedPreferencesManager = AppSharedPreferencesManager(activity)

    private val acknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                CoroutineScope(SupervisorJob()).launch {
                scope.launch {
                    sharedPreferencesManager.setBillingAdsSate(true)
                }
            }
        }

    var billingClient = BillingClient.newBuilder(activity)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()


    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val result = checkItemAvailability()
                    if (result) {
                        querySkuDetails()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                this@BillingManager.startConnection() //???
            }
        })
    }

    fun setSubsState() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val result = checkItemAvailability()
                    if (result) {
                        BILLING_FLAG_STATE = true
                        CoroutineScope(SupervisorJob()).launch {
                            sharedPreferencesManager.setBillingAdsSate(false)
                        }
                    }else{
                        BILLING_FLAG_STATE = false
                        scope.launch {
//                        CoroutineScope(SupervisorJob()).launch {
                            sharedPreferencesManager.setBillingAdsSate(true)
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                this@BillingManager.startConnection() //???
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
    //
    fun querySkuDetails() {
        val skuList = ArrayList<String>()
        skuList.add(TRIAL_PRODUCT_YEAR)

        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(PRODUCT_TYPE)

//        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            scope.launch {
            billingClient.querySkuDetailsAsync(params.build()) { _, skuDetailsList ->
                if (skuDetailsList != null) {
                    for (skuDetails in skuDetailsList) {
                        buyItem(skuDetails)
                    }
                }
            }
        }
    }

    fun checkItemAvailability(): Boolean {
        val items = billingClient.queryPurchases(PRODUCT_TYPE).purchasesList
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