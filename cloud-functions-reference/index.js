/**
 * DoorCut Server-Side Payment & Trusted Booking Functions Reference
 * Patterned after:
 *   - firebase/functions-samples (HTTPS + Firebase Auth ID Token verification)
 *   - razorpay/razorpay-node (Server-side order creation & HMAC-SHA256 signature verification)
 *
 * Deployment steps (requires Firebase Blaze plan for outbound Razorpay API calls):
 *   1. Copy this folder to a `functions/` directory with `firebase-functions`, `firebase-admin`, and `razorpay`.
 *   2. Store your Razorpay credentials in Secret Manager (NEVER in Android APK code):
 *        firebase functions:secrets:set RAZORPAY_KEY_ID
 *        firebase functions:secrets:set RAZORPAY_KEY_SECRET
 *   3. Deploy:
 *        firebase deploy --only functions
 *   4. Paste the deployed region URL into `RazorpayBookingFeeGateway.CLOUD_FUNCTIONS_BASE_URL`.
 */

const crypto = require("node:crypto");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const Razorpay = require("razorpay");

initializeApp();

const DATABASE_ID = "ai-studio-android-doorcut-be8341c2-b2be-4071-826a-b9ba90d231fd";
const db = getFirestore(DATABASE_ID);

const RAZORPAY_KEY_ID = defineSecret("RAZORPAY_KEY_ID");
const RAZORPAY_KEY_SECRET = defineSecret("RAZORPAY_KEY_SECRET");

async function requireAuthenticatedUid(req) {
  const header = req.headers.authorization || "";
  if (!header.startsWith("Bearer ")) {
    throw new Error("Missing Firebase Bearer ID token.");
  }
  const idToken = header.slice("Bearer ".length).trim();
  const decoded = await getAuth().verifyIdToken(idToken);
  return decoded.uid;
}

/**
 * 1. Create Razorpay Order for ONLY the upfront slot confirmation fee,
 *    computed strictly from server-side Firestore `services` + `platformConfig/global`.
 */
exports.createBookingFeeOrder = onRequest(
  { region: "asia-south1", secrets: [RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET], cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") return res.status(405).json({ error: "POST required" });
      const uid = await requireAuthenticatedUid(req);
      const { bookingId, serviceIds } = req.body || {};
      if (!bookingId || !Array.isArray(serviceIds) || serviceIds.length === 0) {
        return res.status(400).json({ error: "bookingId and non-empty serviceIds required" });
      }

      // Compute subtotal from authoritative server-side Firestore services
      let serviceSubtotalPaise = 0;
      for (const id of serviceIds.slice(0, 15)) {
        const snap = await db.collection("services").doc(String(id)).get();
        if (!snap.exists || snap.data().isActive !== true) {
          return res.status(400).json({ error: `Inactive or unknown service: ${id}` });
        }
        serviceSubtotalPaise += Number(snap.data().pricePaise || 0);
      }

      const cfgSnap = await db.collection("platformConfig").doc("global").get();
      const cfg = cfgSnap.exists ? cfgSnap.data() : {};
      const feeType = cfg.bookingFeeType || "FIXED";
      let amountPaise = Number(cfg.fixedBookingFeePaise || 2900);
      if (feeType === "PERCENTAGE" && Number(cfg.percentageBookingFeeBps || 0) > 0) {
        amountPaise = Math.floor((serviceSubtotalPaise * Number(cfg.percentageBookingFeeBps)) / 10000);
      }

      const razorpay = new Razorpay({
        key_id: RAZORPAY_KEY_ID.value(),
        key_secret: RAZORPAY_KEY_SECRET.value(),
      });

      const order = await razorpay.orders.create({
        amount: amountPaise,
        currency: "INR",
        receipt: `dc_${bookingId}`.slice(0, 40),
        notes: { bookingId: String(bookingId), customerId: uid },
      });

      return res.status(200).json({
        orderId: order.id,
        amountPaise,
        currency: "INR",
        keyId: RAZORPAY_KEY_ID.value(),
      });
    } catch (err) {
      return res.status(401).json({ error: err.message || "Unable to create order" });
    }
  }
);

/**
 * 2. Verify Razorpay HMAC-SHA256 payment signature on the server, write
 *    `/verifiedPayments/{bookingId}`, and mark `bookingFeeStatus = "PAID_VERIFIED"`.
 */
exports.verifyBookingFeeSignature = onRequest(
  { region: "asia-south1", secrets: [RAZORPAY_KEY_SECRET], cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") return res.status(405).json({ error: "POST required" });
      const uid = await requireAuthenticatedUid(req);
      const { bookingId, razorpayOrderId, razorpayPaymentId, razorpaySignature } = req.body || {};
      if (!bookingId || !razorpayOrderId || !razorpayPaymentId || !razorpaySignature) {
        return res.status(400).json({ verified: false, error: "Missing payment verification fields" });
      }

      const expectedSignature = crypto
        .createHmac("sha256", RAZORPAY_KEY_SECRET.value())
        .update(`${razorpayOrderId}|${razorpayPaymentId}`)
        .digest("hex");

      const sigBuffer = Buffer.from(String(razorpaySignature), "utf8");
      const expBuffer = Buffer.from(expectedSignature, "utf8");
      if (sigBuffer.length !== expBuffer.length || !crypto.timingSafeEqual(sigBuffer, expBuffer)) {
        return res.status(400).json({ verified: false, error: "Invalid Razorpay HMAC signature" });
      }

      const bookingRef = db.collection("bookings").doc(String(bookingId));
      const verifiedRef = db.collection("verifiedPayments").doc(String(bookingId));

      await db.runTransaction(async (tx) => {
        const bookingSnap = await tx.get(bookingRef);
        const amountPaise = bookingSnap.exists
          ? Number(bookingSnap.data().platformBookingFeePaise || 2900)
          : 2900;
        const customerId = bookingSnap.exists ? String(bookingSnap.data().customerId) : uid;
        if (customerId !== uid) {
          throw new Error("Caller does not own this booking.");
        }

        tx.set(verifiedRef, {
          bookingId: String(bookingId),
          customerId: uid,
          amountPaise,
          provider: "RAZORPAY_WEBHOOK",
          orderId: String(razorpayOrderId),
          paymentId: String(razorpayPaymentId),
          verifiedBy: "cloud_function_hmac",
          verifiedAt: FieldValue.serverTimestamp(),
        });

        if (bookingSnap.exists) {
          tx.update(bookingRef, {
            bookingFeeStatus: "PAID_VERIFIED",
            bookingFeeTxRef: String(razorpayPaymentId),
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      });

      return res.status(200).json({ verified: true });
    } catch (err) {
      return res.status(400).json({ verified: false, error: err.message || "Verification failed" });
    }
  }
);
