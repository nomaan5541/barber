package com.example.util

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Zero-trust client gateway for DoorCut upfront booking fee orders & signature verification.
 *
 * SECURITY INVARIANT (`razorpay-node` + `firebase/functions-samples` pattern):
 * - Razorpay secret keys (`RAZORPAY_KEY_SECRET`) MUST NEVER be placed in Android code,
 *   `BuildConfig`, or `.env.example`.
 * - Upfront booking fee orders are created on the server (`/createBookingFeeOrder`) using
 *   authoritative Firestore service & platformConfig prices, and Razorpay signatures are
 *   HMAC-SHA256 verified on the server (`/verifyBookingFeeSignature`), which writes
 *   `/verifiedPayments/{bookingId}` and updates `bookingFeeStatus = "PAID_VERIFIED"`.
 * - When `CLOUD_FUNCTIONS_BASE_URL` is blank (before Firebase Cloud Functions deployment),
 *   bookings are created honestly with `bookingFeeStatus = "UNPAID"` (Pending Server/Admin
 *   Verification) and never spoof `"PAID_VERIFIED"` on the client.
 */
object RazorpayBookingFeeGateway {

    /**
     * Public HTTPS base URL of your deployed Firebase Cloud Functions v2 region
     * (e.g. "https://asia-south1-<your-project-id>.cloudfunctions.net").
     * Safe to store in client code because it is a public endpoint protected by Firebase ID tokens.
     */
    const val CLOUD_FUNCTIONS_BASE_URL: String = ""

    sealed class OrderResult {
        data class Ready(
            val orderId: String,
            val amountPaise: Long,
            val currency: String,
            val publicKeyId: String
        ) : OrderResult()

        data class ServerNotConfigured(val guidance: String) : OrderResult()
        data class Error(val message: String) : OrderResult()
    }

    sealed class VerificationResult {
        data class ServerVerified(val paymentId: String, val orderId: String) : VerificationResult()
        data class PendingServerVerification(val reason: String) : VerificationResult()
        data class Failed(val message: String) : VerificationResult()
    }

    fun isServerEndpointConfigured(): Boolean = CLOUD_FUNCTIONS_BASE_URL.startsWith("https://")

    suspend fun createServerOrder(
        currentUser: FirebaseUser?,
        bookingId: String,
        serviceIds: List<String>
    ): OrderResult = withContext(Dispatchers.IO) {
        if (!isServerEndpointConfigured()) {
            return@withContext OrderResult.ServerNotConfigured(
                "Cloud Functions payment endpoint is not configured yet. Booking will be queued with Booking Fee Pending Verification."
            )
        }
        val user = currentUser ?: return@withContext OrderResult.Error("Sign in required before creating payment order.")
        runCatching {
            val idToken = user.getIdToken(false).await().token.orEmpty()
            val payload = JSONObject().apply {
                put("bookingId", bookingId)
                put("serviceIds", org.json.JSONArray(serviceIds))
            }
            val responseJson = postJson(
                endpoint = "${CLOUD_FUNCTIONS_BASE_URL.trimEnd('/')}/createBookingFeeOrder",
                bearerToken = idToken,
                body = payload
            )
            OrderResult.Ready(
                orderId = responseJson.getString("orderId"),
                amountPaise = responseJson.getLong("amountPaise"),
                currency = responseJson.optString("currency", "INR"),
                publicKeyId = responseJson.getString("keyId")
            )
        }.getOrElse { e ->
            OrderResult.Error(e.localizedMessage ?: "Unable to create Razorpay order on server.")
        }
    }

    suspend fun verifyPaymentOnServer(
        currentUser: FirebaseUser?,
        bookingId: String,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ): VerificationResult = withContext(Dispatchers.IO) {
        if (!isServerEndpointConfigured()) {
            return@withContext VerificationResult.PendingServerVerification(
                "Awaiting server/Admin verification receipt."
            )
        }
        val user = currentUser ?: return@withContext VerificationResult.Failed("Sign in required.")
        runCatching {
            val idToken = user.getIdToken(false).await().token.orEmpty()
            val payload = JSONObject().apply {
                put("bookingId", bookingId)
                put("razorpayOrderId", razorpayOrderId)
                put("razorpayPaymentId", razorpayPaymentId)
                put("razorpaySignature", razorpaySignature)
            }
            val responseJson = postJson(
                endpoint = "${CLOUD_FUNCTIONS_BASE_URL.trimEnd('/')}/verifyBookingFeeSignature",
                bearerToken = idToken,
                body = payload
            )
            if (responseJson.optBoolean("verified", false)) {
                VerificationResult.ServerVerified(
                    paymentId = razorpayPaymentId,
                    orderId = razorpayOrderId
                )
            } else {
                VerificationResult.Failed(responseJson.optString("error", "Signature verification rejected by server."))
            }
        }.getOrElse { e ->
            VerificationResult.Failed(e.localizedMessage ?: "Payment verification request failed.")
        }
    }

    private fun postJson(
        endpoint: String,
        bearerToken: String,
        body: JSONObject
    ): JSONObject {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: ${text.take(180)}")
            }
            JSONObject(text.ifBlank { "{}" })
        } finally {
            conn.disconnect()
        }
    }
}
