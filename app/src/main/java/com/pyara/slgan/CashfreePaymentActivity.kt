package com.pyara.slgan

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.CFTheme.CFThemeBuilder
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.ui.api.CFDropCheckoutPayment.CFDropCheckoutPaymentBuilder
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class CashfreePaymentActivity : ComponentActivity(), CFCheckoutResponseCallback {
    private val workerUrl = "https://slgan-cashfree-api.amruth4official.workers.dev"
    private val executor = Executors.newSingleThreadExecutor()
    private var documentId: String = ""
    private var paymentStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty()
        if (documentId.isBlank()) {
            finishWithMessage("Order reference is missing.")
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finishWithMessage("Please sign in again before payment.")
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->
                val idToken = tokenResult.token
                if (idToken.isNullOrBlank()) {
                    finishWithMessage("Could not verify your login. Please sign in again.")
                } else {
                    executor.execute { createCashfreeOrder(idToken) }
                }
            }
            .addOnFailureListener { error ->
                finishWithMessage(error.localizedMessage ?: "Could not verify your login.")
            }
    }

    private fun createCashfreeOrder(idToken: String) {
        try {
            val response = workerPost("/create-order", idToken)
            val orderId = response.optString("order_id")
            val sessionId = response.optString("payment_session_id")
            if (orderId.isBlank() || sessionId.isBlank()) {
                throw IllegalStateException("Payment session was not returned by the server.")
            }
            runOnUiThread {
                try {
                    CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
                    val session = CFSession.CFSessionBuilder()
                        .setEnvironment(CFSession.Environment.SANDBOX)
                        .setPaymentSessionID(sessionId)
                        .setOrderId(orderId)
                        .build()
                    val theme = CFThemeBuilder()
                        .setNavigationBarBackgroundColor("#222222")
                        .setNavigationBarTextColor("#FFFFFF")
                        .setButtonBackgroundColor("#222222")
                        .setButtonTextColor("#FFFFFF")
                        .setPrimaryTextColor("#222222")
                        .setSecondaryTextColor("#666666")
                        .build()
                    val payment = CFDropCheckoutPaymentBuilder()
                        .setSession(session)
                        .setCFNativeCheckoutUITheme(theme)
                        .build()
                    paymentStarted = true
                    CFPaymentGatewayService.getInstance().doPayment(this, payment)
                } catch (error: CFException) {
                    Log.e(TAG, "Cashfree checkout launch failed", error)
                    finishWithMessage(error.localizedMessage ?: "Could not open Cashfree checkout.")
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Cashfree order creation failed", error)
            runOnUiThread {
                finishWithMessage(error.localizedMessage ?: "Could not start payment.")
            }
        }
    }

    override fun onPaymentVerify(orderID: String) {
        verifyOnServer("Payment submitted. Verifying securely…")
    }

    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderID: String) {
        Log.w(TAG, "Cashfree payment callback: ${cfErrorResponse.message}")
        verifyOnServer("Checking final payment status…")
    }

    private fun verifyOnServer(message: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finishWithMessage("Payment submitted. Sign in to refresh its status.")
            return
        }
        user.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                val idToken = tokenResult.token
                if (idToken.isNullOrBlank()) {
                    finishWithMessage("Payment submitted. Could not verify login; check Orders later.")
                } else {
                    executor.execute {
                        try {
                            val result = workerPost("/verify-payment", idToken)
                            val status = result.optString("status")
                            runOnUiThread {
                                when (status) {
                                    "PAID" -> finishWithMessage("Payment successful. Your order is confirmed.")
                                    "PENDING" -> finishWithMessage("Payment is pending. Check Orders again shortly.")
                                    else -> finishWithMessage("Payment not confirmed. No payment was marked as paid.")
                                }
                            }
                        } catch (error: Exception) {
                            Log.e(TAG, "Payment verification failed", error)
                            runOnUiThread {
                                finishWithMessage("Could not verify payment yet. Please check Orders later.")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                finishWithMessage("Payment submitted. Could not verify login; check Orders later.")
            }
    }

    private fun workerPost(path: String, idToken: String): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(workerUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $idToken")
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().put("documentId", documentId)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val result = JSONObject(body.ifBlank { "{}" })
            if (status !in 200..299) {
                throw IllegalStateException(result.optString("error").ifBlank { "Payment server error ($status)." })
            }
            return result
        } finally {
            connection?.disconnect()
        }
    }

    private fun finishWithMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    companion object {
        const val EXTRA_DOCUMENT_ID = "com.pyara.slgan.CASHFREE_DOCUMENT_ID"
        private const val TAG = "SLGANCashfree"
    }
}
