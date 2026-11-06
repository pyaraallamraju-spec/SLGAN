package com.pyara.slgan

import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.view.View
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.view.WindowManager
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.speech.RecognizerIntent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import java.util.concurrent.TimeUnit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val SLGAN_ORDER_EMAIL_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxzmv15O2r71h7FQkQ_CaI00GSHsyGPZ3QFIB08m8Xw8TxjJjYwFtuGFHz71iDPKEBR/exec"

private fun requestSlganOrderEmail(
    idToken: String,
    orderId: String,
    onResult: (success: Boolean, message: String) -> Unit
) {
    Thread {
        var connection: HttpURLConnection? = null
        var success = false
        var message = "Email service could not be reached."
        try {
            val payload = JSONObject()
                .put("idToken", idToken)
                .put("orderId", orderId)
                .put("documentId", orderId)
            connection = (URL(SLGAN_ORDER_EMAIL_WEB_APP_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "text/plain; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }?.take(3000).orEmpty()
            val responseJson = runCatching { JSONObject(responseBody) }.getOrNull()
            val responseMessage = responseJson?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: responseJson?.optString("error")?.takeIf { it.isNotBlank() }
            val explicitSuccess = responseJson?.optBoolean("success", false) == true ||
                responseJson?.optBoolean("ok", false) == true ||
                responseJson?.optString("status")?.equals("sent", ignoreCase = true) == true
            success = status in 200..299 && (
                explicitSuccess ||
                    (responseJson == null && responseBody.contains("success", ignoreCase = true) &&
                        responseBody.contains("sent", ignoreCase = true))
                )
            message = responseMessage ?: when {
                status !in 200..299 -> "Email service returned HTTP $status."
                success -> "Email service confirmed the request."
                responseBody.isBlank() -> "Email service returned an empty response."
                else -> "Email service did not confirm sending. Response: " + responseBody.take(350)
            }
            if (success) {
                android.util.Log.i("SLGANOrderEmail", "Email service confirmed order $orderId: $message")
            } else {
                android.util.Log.e("SLGANOrderEmail", "Email service failed for $orderId (HTTP $status): $message")
            }
        } catch (error: Exception) {
            message = error.localizedMessage ?: error.javaClass.simpleName
            android.util.Log.e("SLGANOrderEmail", "Email request failed for $orderId: $message", error)
        } finally {
            connection?.disconnect()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(success, message)
            }
        }
    }.start()
}

private data class AppPalette(
    val themeId: Int,
    val forest: Color,
    val leaf: Color,
    val cream: Color,
    val paleGreen: Color,
    val ink: Color,
    val muted: Color,
    val gold: Color
)

private val defaultAppPalette = AppPalette(
    themeId = 1,
    forest = Color(0xFF762A24),
    leaf = Color(0xFF8A4B2A),
    cream = Color(0xFFFFF7E8),
    paleGreen = Color(0xFFF5E5C8),
    ink = Color(0xFF38251F),
    muted = Color(0xFF806D60),
    gold = Color(0xFFD9A441)
)

// One observable palette keeps all customer-facing colors in sync with a single recomposition.
private var currentAppPalette by mutableStateOf(defaultAppPalette)
private val Forest get() = currentAppPalette.forest
private val Leaf get() = currentAppPalette.leaf
private val Cream get() = currentAppPalette.cream
private val PaleGreen get() = currentAppPalette.paleGreen
private val Ink get() = currentAppPalette.ink
private val Muted get() = currentAppPalette.muted
private val Gold get() = currentAppPalette.gold
private val activeAppThemeId get() = currentAppPalette.themeId

private val AdminBackground = Color(0xFFF1F2F4)
private val AdminLightGray = Color(0xFFF8F9FA)
private val AdminSurface = Color.White
private val AdminInk = Color(0xFF25282D)
private val AdminMuted = Color(0xFF707780)

private fun applyAppPalette(themeId: Int) {
    currentAppPalette = when (themeId) {
        3 -> AppPalette(
            themeId = 3,
            forest = Color(0xFFF4C96B),
            leaf = Color(0xFFDDAE4F),
            cream = Color(0xFF0B1118),
            paleGreen = Color(0xFF142130),
            ink = Color(0xFFF4F1E8),
            muted = Color(0xFFAAB4C0),
            gold = Color(0xFFF4C96B)
        )
        4 -> AppPalette(
            themeId = 4,
            forest = Color(0xFF075B2A),
            leaf = Color(0xFF2E8B45),
            cream = Color(0xFFF1FAF0),
            paleGreen = Color(0xFFDDF3DF),
            ink = Color(0xFF12351E),
            muted = Color(0xFF607568),
            gold = Color(0xFF2E8B45)
        )
        2 -> AppPalette(
            themeId = 2,
            forest = Color(0xFF214B35),
            leaf = Color(0xFF557A45),
            cream = Color(0xFFFFF9E9),
            paleGreen = Color(0xFFE7EBD4),
            ink = Color(0xFF20372A),
            muted = Color(0xFF687663),
            gold = Color(0xFFC9A34A)
        )
        else -> defaultAppPalette
    }
}

private data class DefaultBtnTheme(
    val name: String = "default-btn",
    val background: Color = Color(0xFFE2E2E2),
    val border: Color = Color(0xFFB5B5B5),
    val content: Color = Color(0xFF000000),
    val cornerRadius: Dp = 14.dp,
    val height: Dp = 58.dp
)

private val DefaultBtn = DefaultBtnTheme()
private fun appThemedColor(original: Color): Color {
    val themeId = activeAppThemeId
    if (themeId != 3 && themeId != 4) return original

    val red = original.red
    val green = original.green
    val blue = original.blue
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val saturation = if (max <= 0f) 0f else delta / max
    val brightness = 0.299f * red + 0.587f * green + 0.114f * blue
    val hue = when {
        delta <= 0.00001f -> 0f
        max == red -> (60f * ((green - blue) / delta) + 360f) % 360f
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }

    // Preserve strong semantic red (errors) and green (success) indicators.
    if (saturation > 0.48f && (hue < 12f || hue > 345f || hue in 85f..165f)) return original

    return when {
        brightness > 0.91f && saturation < 0.16f ->
            if (themeId == 3) Color(0xFF142130) else Color(0xFFF1FAF0)
        brightness > 0.72f && saturation < 0.28f ->
            if (themeId == 3) Color(0xFF0B1118) else Color(0xFFDDF3DF)
        brightness < 0.22f && saturation < 0.45f ->
            if (themeId == 3) Color(0xFFF4F1E8) else Color(0xFF12351E)
        saturation < 0.22f && brightness < 0.68f ->
            if (themeId == 3) Color(0xFFAAB4C0) else Color(0xFF607568)
        hue in 15f..65f && saturation >= 0.12f ->
            if (themeId == 3) Color(0xFFF4C96B) else Color(0xFF075B2A)
        else -> original
    }
}

private val LocalAppLanguage = staticCompositionLocalOf { "English" }

private fun tr(text: String, language: String): String {
    if (language == "English") return text
    val te = mapOf(
        "Home" to "హోమ్", "Categories" to "వర్గాలు", "Cart" to "కార్ట్", "Profile" to "ప్రొఫైల్",
        "Your account" to "మీ ఖాతా", "Manage your personal details and delivery addresses." to "మీ వ్యక్తిగత వివరాలు, డెలివరీ చిరునామాలను నిర్వహించండి.",
        "Profile" to "ప్రొఫైల్", "First name" to "పేరు", "Last name" to "ఇంటి పేరు",
        "Nickname" to "ముద్దుపేరు", "Mobile number" to "మొబైల్ నంబర్", "Enter mobile number" to "మొబైల్ నంబర్ నమోదు చేయండి",
        "Gender" to "లింగం", "Select gender" to "లింగాన్ని ఎంచుకోండి", "Female" to "స్త్రీ", "Male" to "పురుషుడు",
        "Non-binary" to "నాన్-బైనరీ", "Prefer not to say" to "చెప్పదలచుకోలేదు", "Save profile" to "ప్రొఫైల్ సేవ్ చేయండి",
        "Saved successfully." to "విజయవంతంగా సేవ్ అయింది.", "Saving…" to "సేవ్ అవుతోంది…", "Loading profile…" to "ప్రొఫైల్ లోడ్ అవుతోంది…",
        "Sign out" to "సైన్ అవుట్", "Enter delivery details below." to "డెలివరీ వివరాలను క్రింద నమోదు చేయండి.",
                        "Home" to "ఇల్లు", "Work" to "పని", "Other" to "ఇతర",
                                                        "All" to "అన్నీ", "Lehyas" to "లేహ్యాలు", "Ghrutas" to "ఘృతాలు", "Pills" to "మాత్రలు",
        "Tailas" to "తైలాలు", "Churnas" to "చూర్ణాలు", "Rasayanas" to "రసాయనాలు", "Rasayogas" to "రసయోగాలు", "Rasayoga" to "రసయోగం", "Bhasmas" to "భస్మాలు",
        "Voice search" to "వాయిస్ సెర్చ్",
        "Voice search is not available on this device." to "ఈ పరికరంలో వాయిస్ సెర్చ్ అందుబాటులో లేదు.",
        "Sign in with your registered email and password." to "మీ నమోదైన ఇమెయిల్ మరియు పాస్‌వర్డ్‌తో సైన్ ఇన్ చేయండి.",
        "Mobile-number login is not available yet." to "మొబైల్ నంబర్ లాగిన్ ఇంకా అందుబాటులో లేదు.",
        "View product" to "ఉత్పత్తి వివరాలు",
        "Price not available yet." to "ధర ఇంకా అందుబాటులో లేదు.",
        "Product details" to "ఉత్పత్తి వివరాలు",
        "Description" to "వివరణ",
        "Usage" to "వాడే విధానం",
        "Cures" to "ఉపయోగ సూచనలు",
        "Major ingredients" to "ప్రధాన పదార్థాలు",
        "About" to "వివరాలు", "Medicines" to "మందులు", "Care" to "జాగ్రత్తలు", "Net quantity" to "నికర పరిమాణం", "Price inclusive of applicable taxes" to "వర్తించే పన్నులతో సహా ధర",
        "No online payment. Place your order and we will confirm delivery details." to "ఆన్‌లైన్ చెల్లింపు లేదు. ఆర్డర్ చేయండి; డెలివరీ వివరాలను మేము నిర్ధారిస్తాము.",
        "Place order" to "ఆర్డర్ చేయండి",
        "Customer name *" to "కస్టమర్ పేరు *",
        "Email for invoice *" to "ఇన్‌వాయిస్ కోసం ఇమెయిల్ *",
        "Phone Number" to "ఫోన్ నంబర్",
                "Payment: none. This order request does not collect payment." to "చెల్లింపు లేదు. ఈ ఆర్డర్ అభ్యర్థనలో డబ్బు వసూలు చేయబడదు.",
        "Cancel" to "రద్దు చేయండి",
        "My Orders" to "నా ఆర్డర్లు",
        "You have no previous orders yet." to "మీకు ఇంకా పాత ఆర్డర్లు లేవు.",
        "View / Download Invoice" to "ఇన్‌వాయిస్ చూడండి / డౌన్‌లోడ్ చేయండి",
        "Previous Orders" to "మునుపటి ఆర్డర్లు",
        "Order History" to "ఆర్డర్ చరిత్ర",
        "View complete invoice" to "పూర్తి ఇన్‌వాయిస్ చూడండి",
        "Open invoice PDF" to "ఇన్‌వాయిస్ PDF తెరవండి",
        "ORDER INVOICE" to "ఆర్డర్ ఇన్‌వాయిస్",
        "Invoice No: ${'\$' }invoiceNo" to "ఇన్‌వాయిస్ నం.: ${'\$' }invoiceNo",
        "Order ID: ${'\$' }orderId" to "ఆర్డర్ ID: ${'\$' }orderId",
        "Order date: ${'\$' }created" to "ఆర్డర్ తేదీ: ${'\$' }created",
        "BILL TO" to "బిల్ వివరాలు",
        "ITEM DETAILS" to "వస్తువు వివరాలు",
        "No item details were saved with this order." to "ఈ ఆర్డర్‌తో వస్తువుల వివరాలు సేవ్ కాలేదు.",
        "GRAND TOTAL" to "మొత్తం",
        "Payment has not been collected unless the status above says otherwise." to "పై స్థితిలో వేరుగా చూపకపోతే చెల్లింపు స్వీకరించలేదు.",
        "Close" to "మూసివేయండి",
        "Open PDF" to "PDF తెరవండి",
        "Diseases" to "వ్యాధులు",
        "All Diseases" to "అన్ని వ్యాధులు",
        "Tuberculosis (TB)" to "క్షయవ్యాధి (టీబీ)",
        "Tuberculosis" to "క్షయవ్యాధి",
        "Cough" to "దగ్గు",
        "Flu" to "ఫ్లూ",
        "Fever" to "జ్వరం",
        "Obesity" to "ఊబకాయం",
        "Select a disease to view." to "అందుబాటులో ఉన్న జాబితా నుంచి వ్యాధిని ఎంచుకోండి.",
        "Select disease" to "వ్యాధిని ఎంచుకోండి",
        "Loading diseases from database…" to "డేటాబేస్ నుంచి వ్యాధులు లోడ్ అవుతున్నాయి…",
        "Could not load diseases: ${'\$' }error" to "వ్యాధులను లోడ్ చేయలేకపోయాం: ${'\$' }error",
        "No matching diseases found." to "సరిపోలే వ్యాధులు కనబడలేదు.",
        "DISEASE OVERVIEW" to "వ్యాధి వివరాలు",
        "What is this disease?" to "ఈ వ్యాధి అంటే ఏమిటి?",
        "Medicines available" to "అందుబాటులో ఉన్న మందులు",
        "Choose a specific disease to see matching medicines." to "సంబంధిత మందులను చూడటానికి ఒక నిర్దిష్ట వ్యాధిని ఎంచుకోండి.",
        "Use medicines only with guidance from a qualified healthcare professional." to "అర్హత కలిగిన వైద్య నిపుణుల సూచనతో మాత్రమే మందులను వాడండి.",
        "Information not available." to "సమాచారం అందుబాటులో లేదు.",
        "Pack size: ${'\$' }{product.subtitle}" to "ప్యాక్ పరిమాణం: ${'\$' }{product.subtitle}",
        "Category: ${'\$' }{product.category}" to "వర్గం: ${'\$' }{product.category}",
        "Quantity / stock: ${'\$' }{product.quantity}" to "పరిమాణం / నిల్వ: ${'\$' }{product.quantity}",
        "Search a place or address below, then tap a result to fill the address fields." to "క్రింద స్థలం లేదా చిరునామాను వెతికి, వివరాలు నింపడానికి ఫలితాన్ని ఎంచుకోండి.",
        "All" to "అన్నీ", "Lehyas" to "లేహ్యాలు", "Ghrutas" to "ఘృతాలు", "Pills" to "మాత్రలు", "Tailas" to "తైలాలు", "Churnas" to "చూర్ణాలు", "Rasayanas" to "రసాయనాలు", "Bhasmas" to "భస్మాలు",
        "Language" to "భాష", "Select language" to "భాషను ఎంచుకోండి", "Save language preference" to "భాష ప్రాధాన్యతను సేవ్ చేయండి",
        "Email address" to "ఇమెయిల్ చిరునామా", "Password (at least 6 characters)" to "పాస్‌వర్డ్ (కనీసం 6 అక్షరాలు)", "Secure email and password sign-in" to "ఇమెయిల్ మరియు పాస్‌వర్డ్‌తో సురక్షిత లాగిన్",
        "Rooted in Tradition. Inspired by Nature." to "సంప్రదాయంలో పాతుకుపోయి, ప్రకృతి స్ఫూర్తితో.", "ROOTED IN NATURE" to "ప్రకృతిలో వేళ్లూనుకున్నది", "Everyday wellness,\\nwith a herbal touch." to "ప్రతిరోజూ ఆరోగ్యం,\\nమూలికలతో.", "Explore collection  →" to "ఉత్పత్తులను చూడండి  →",
        "SRI LAKSHMI GANAPATHI" to "శ్రీ లక్ష్మీ గణపతి", "AYURVEDA NILAYAM" to "ఆయుర్వేద నిలయం",
        "TRADITION • TRUST • WELLNESS" to "సంప్రదాయం • నమ్మకం • ఆరోగ్యం", "Preparing your wellness experience" to "మీ ఆరోగ్య అనుభవాన్ని సిద్ధం చేస్తున్నాం",
        "Sri Lakshmi Ganapathi" to "శ్రీ లక్ష్మీ గణపతి", "Ayurveda Nilayam" to "ఆయుర్వేద నిలయం",
        "Estd. 1900 by Vidyaraju Sri Allamraju Perraju Sharma" to "పెర్రాజు శర్మచే 1900లో స్థాపితం", "Nature's care, rooted in tradition." to "ప్రకృతి సంరక్షణ,\nసంప్రదాయ మూలాలతో.", "Sri Lakshmi Ganapathi Ayurveda Nilayam" to "శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయం",
        "OUR HERITAGE" to "మన వారసత్వం", "DISCOVER SLGAN" to "SLGANను తెలుసుకోండి", "SLGAN SIGNATURE PRODUCT" to "SLGAN ప్రత్యేక ఉత్పత్తి",
        "Traditional Ayurvedic care, rooted in nature" to "ప్రకృతిలో పాతుకుపోయిన సంప్రదాయ ఆయుర్వేద సంరక్షణ", "A signature SLGAN product" to "SLGAN ప్రత్యేక ఉత్పత్తి",
        "Search products" to "ఉత్పత్తులను వెతకండి", "Shop By Category" to "వర్గాల వారీగా కొనండి", "Could not load products: ${'\$' }productsError" to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం: ${'\$' }productsError",
        "Made with care for your everyday rituals." to "మీ రోజువారీ అవసరాల కోసం శ్రద్ధగా తయారు చేయబడింది.", "Your cart" to "మీ కార్ట్", "Subtotal" to "మొత్తం", "Checkout" to "చెక్‌అవుట్", "Demo only: checkout and payments are not connected." to "ఇది డెమో మాత్రమే: చెక్‌అవుట్ మరియు చెల్లింపులు ఇంకా కనెక్ట్ కాలేదు.", "Find the collection for your routine." to "మీ అవసరానికి తగిన ఉత్పత్తుల సమాహారాన్ని కనుగొనండి.",
        "Language preference saved." to "భాష ప్రాధాన్యత సేవ్ అయింది.", "Couldn't save language. Please try again." to "భాషను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "English" to "English", "Telugu" to "తెలుగు", "Hindi" to "హిందీ",
        "Clear your cart?" to "మీ కార్ట్‌ను ఖాళీ చేయాలా?",
        "Clear cart" to "కార్ట్ ఖాళీ చేయండి",
        "Checkout" to "చెక్‌అవుట్",
        "Your Cart Is Empty" to "మీ కార్ట్ ఖాళీగా ఉంది",
        "Explore our Ayurvedic products and add your favourites." to "మా ఆయుర్వేద ఉత్పత్తులను చూసి మీకు నచ్చినవి జోడించండి.",
        "Your Ayurvedic Order" to "మీ ఆయుర్వేద ఆర్డర్",
        "Save for later" to "తర్వాత కోసం సేవ్ చేయండి",
        "You Might Also Like" to "మీకు ఇవి కూడా నచ్చవచ్చు",
        "ADD" to "జోడించండి",
        "Bill Details" to "బిల్లు వివరాలు",
        "Items total" to "వస్తువుల మొత్తం",
        "Delivery charge" to "డెలివరీ ఛార్జీ",
        "Confirmed at order" to "ఆర్డర్ సమయంలో నిర్ధారిస్తాం",
        "Grand Total" to "మొత్తం చెల్లించాల్సిన మొత్తం",
        "No online payment is collected in this app." to "ఈ యాప్‌లో ఆన్‌లైన్ చెల్లింపులు స్వీకరించబడవు.",
        "Instructions" to "సూచనలు",
        "Add any special instructions for your order" to "మీ ఆర్డర్‌కు సంబంధించిన ప్రత్యేక సూచనలు జోడించండి",
        "Delivery Instructions" to "డెలివరీ సూచనలు",
        "Ordering For Someone Else?" to "మరొకరి కోసం ఆర్డర్ చేస్తున్నారా?",
        "Recipient details can be added at checkout." to "చెక్‌అవుట్‌లో స్వీకర్త వివరాలను జోడించవచ్చు.",
        "Cancellation Policy" to "రద్దు విధానం",
        "Product Design" to "ఉత్పత్తి రూపకల్పన",
        "Change" to "మార్చండి",
        "No address added" to "చిరునామా జోడించలేదు",
        "Add" to "జోడించండి",
        "TOTAL" to "మొత్తం",
        "Place order  ›" to "ఆర్డర్ చేయండి  ›",
        "Choose Delivery Address" to "డెలివరీ చిరునామాను ఎంచుకోండి",
        "Select the saved address for this order." to "ఈ ఆర్డర్ కోసం సేవ్ చేసిన చిరునామాను ఎంచుకోండి.",
        "No saved addresses found. Add a delivery address from your profile before placing an order." to "సేవ్ చేసిన చిరునామాలు లేవు. ఆర్డర్ చేయడానికి ముందు మీ ప్రొఫైల్‌లో డెలివరీ చిరునామా జోడించండి.",
        "Default address" to "డిఫాల్ట్ చిరునామా",
        "Add New Address" to "కొత్త చిరునామా జోడించండి",
        "Confirm Address" to "చిరునామాను నిర్ధారించండి",
        "Search your orders" to "మీ ఆర్డర్లను వెతకండి",
        "Your Failed Orders" to "విఫలమైన మీ ఆర్డర్లు",
        "ORDER ID" to "ఆర్డర్ ID",
        "Current Orders" to "ప్రస్తుత ఆర్డర్లు",
        "Loading your orders…" to "మీ ఆర్డర్లు లోడ్ అవుతున్నాయి…",
        "No orders placed today" to "ఈరోజు ఇంకా ఆర్డర్లు చేయలేదు",
        "Click to order  →" to "ఆర్డర్ చేయడానికి క్లిక్ చేయండి  →",
        "Reorder" to "మళ్లీ ఆర్డర్ చేయండి",
        "Previous Orders" to "మునుపటి ఆర్డర్లు",
        "Your order history" to "మీ ఆర్డర్ చరిత్ర",
        "No previous orders yet. Orders from earlier days will appear here." to "ఇంకా మునుపటి ఆర్డర్లు లేవు. గత ఆర్డర్లు ఇక్కడ కనిపిస్తాయి.",
        "Rate Your Order" to "మీ ఆర్డర్‌కు రేటింగ్ ఇవ్వండి",
        "How was your order experience?" to "మీ ఆర్డర్ అనుభవం ఎలా ఉంది?",
        "Submit" to "సమర్పించండి",
        "Order Info" to "ఆర్డర్ సమాచారం",
        "Delete order" to "ఆర్డర్ తొలగించండి",
        "Share order items" to "ఆర్డర్ వస్తువులను షేర్ చేయండి",
        "Delete order?" to "ఆర్డర్ తొలగించాలా?",
        "Delete" to "తొలగించండి",
        "Order Summary" to "ఆర్డర్ సారాంశం",
        "Download Invoice" to "ఇన్‌వాయిస్ డౌన్‌లోడ్ చేయండి",
        "MRP / subtotal" to "MRP / ఉప మొత్తం",
        "Delivery charges" to "డెలివరీ ఛార్జీలు",
        "FREE" to "ఉచితం",
        "Bill total" to "బిల్లు మొత్తం",
        "Order Details" to "ఆర్డర్ వివరాలు",
        "Order ID" to "ఆర్డర్ ID",
        "Payment" to "చెల్లింపు",
        "Phone" to "ఫోన్",
        "Order placed" to "ఆర్డర్ చేసిన తేదీ",
        "Need help with your order?" to "మీ ఆర్డర్ గురించి సహాయం కావాలా?",
        "About any issues related to your order" to "మీ ఆర్డర్‌కు సంబంధించిన సమస్యల గురించి",
        "Chat with us" to "మాతో చాట్ చేయండి",
        "Repeat Order" to "మళ్లీ ఆర్డర్ చేయండి",
        "VIEW CART ON NEXT STEP" to "తదుపరి దశలో కార్ట్ చూడండి",
        "Back to Order History" to "ఆర్డర్ చరిత్రకు తిరిగి వెళ్లండి",
        "Confirm password" to "పాస్‌వర్డ్‌ను నిర్ధారించండి",
        "Create account" to "ఖాతా సృష్టించండి",
        "Already have an account? Sign in" to "ఇప్పటికే ఖాతా ఉందా? సైన్ ఇన్ చేయండి",
        "New? Create account" to "కొత్తవారా? ఖాతా సృష్టించండి",
        "Forgot Password?" to "పాస్‌వర్డ్ మర్చిపోయారా?",
        "OR" to "లేదా",
        "Please wait…" to "దయచేసి వేచి ఉండండి…",
        "Continue with Google" to "Googleతో కొనసాగించండి",
        "View Products" to "ఉత్పత్తులను చూడండి",
        "Search Results" to "శోధన ఫలితాలు",
        "Products could not be loaded. Check your internet and Firestore rules." to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం. ఇంటర్నెట్ మరియు Firestore నియమాలను తనిఖీ చేయండి.",
        "No products found in this category. Try All or another search." to "ఈ వర్గంలో ఉత్పత్తులు లేవు. అన్నీ లేదా మరో శోధనను ప్రయత్నించండి.",
        "Signature Products" to "ప్రత్యేక ఉత్పత్తులు",
        "Patent Product" to "పేటెంట్ ఉత్పత్తి",
        "Unlike product" to "ఇష్టమైన వాటి నుంచి తొలగించండి",
        "Like product" to "ఉత్పత్తిని ఇష్టపడండి",
        "Please sign in to rate this product." to "ఈ ఉత్పత్తికి రేటింగ్ ఇవ్వడానికి సైన్ ఇన్ చేయండి.",
        "Rating saved" to "రేటింగ్ సేవ్ అయింది",
        "Could not save rating." to "రేటింగ్‌ను సేవ్ చేయలేకపోయాం.",
        "No ratings" to "ఇంకా రేటింగ్‌లు లేవు",
        "Signature product" to "ప్రత్యేక ఉత్పత్తి",
        "Out of Stock" to "స్టాక్‌లో లేదు",
        "Back to products" to "ఉత్పత్తులకు తిరిగి వెళ్లండి",
        "Back to product search" to "ఉత్పత్తుల శోధనకు తిరిగి వెళ్లండి",
        "Share product" to "ఉత్పత్తిని షేర్ చేయండి",
        "Product Information" to "ఉత్పత్తి సమాచారం",
        "Save product" to "ఉత్పత్తిని సేవ్ చేయండి",
        "Information not available." to "సమాచారం అందుబాటులో లేదు.",
        "About" to "గురించి",
        "Customer Ratings & Reviews" to "కస్టమర్ రేటింగ్‌లు మరియు సమీక్షలు",
        "View cart" to "కార్ట్ చూడండి",
        "Out of stock" to "స్టాక్ లేదు",
        "Add to Cart" to "కార్ట్‌కు జోడించండి",
        "Write A Review" to "సమీక్ష రాయండి",
        "Close review" to "సమీక్షను మూసివేయండి",
        "Your review (optional)" to "మీ సమీక్ష (ఐచ్ఛికం)",
        "Share your experience with this product" to "ఈ ఉత్పత్తితో మీ అనుభవాన్ని పంచుకోండి",
        "Saved" to "సేవ్ అయింది",
        "Saved! Thank you for your review." to "సేవ్ అయింది! మీ సమీక్షకు ధన్యవాదాలు.",
        "Review saved successfully" to "సమీక్ష విజయవంతంగా సేవ్ అయింది",
        "Could not save rating. Please try again." to "రేటింగ్‌ను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "Save" to "సేవ్ చేయండి",
        "Finish editing instructions" to "సూచనల సవరణను పూర్తి చేయండి",
        "Edit instructions" to "సూచనలను సవరించండి",
        "Complete address not available" to "పూర్తి చిరునామా అందుబాటులో లేదు",
        "Address details unavailable" to "చిరునామా వివరాలు అందుబాటులో లేవు",
        "Order successful" to "ఆర్డర్ విజయవంతమైంది",
        "Order failed" to "ఆర్డర్ విఫలమైంది",
        "Your order has been saved" to "మీ ఆర్డర్ సేవ్ అయింది",
        "Please review your order" to "దయచేసి మీ ఆర్డర్‌ను పరిశీలించండి",
        "Your order was saved." to "మీ ఆర్డర్ సేవ్ అయింది.",
        "Your order could not be placed." to "మీ ఆర్డర్‌ను నమోదు చేయలేకపోయాం.",
        "ORDER STATUS" to "ఆర్డర్ స్థితి",
        "Pending verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Received" to "స్వీకరించబడింది",
        "Show all disease options" to "అన్ని వ్యాధి ఎంపికలను చూపించండి",
        "DISEASE DETAILS" to "వ్యాధి వివరాలు",
        "Close disease details and clear search" to "వ్యాధి వివరాలను మూసివేసి శోధనను క్లియర్ చేయండి",
        "About This Condition" to "ఈ వ్యాధి గురించి",
        "Suggested Medicines" to "సూచించిన మందులు",
        "Precautions & care" to "జాగ్రత్తలు మరియు సంరక్షణ",
        "Search products" to "ఉత్పత్తులను వెతకండి",
        "No products in this category" to "ఈ వర్గంలో ఉత్పత్తులు లేవు",
        "Decrease quantity" to "పరిమాణాన్ని తగ్గించండి",
        "Increase quantity" to "పరిమాణాన్ని పెంచండి",
        "Your wishlist" to "మీ విష్‌లిస్ట్",
        "saved products" to "సేవ్ చేసిన ఉత్పత్తులు",
        "Couldn't load your wishlist" to "మీ విష్‌లిస్ట్‌ను లోడ్ చేయలేకపోయాం",
        "Please check your connection and try again." to "దయచేసి మీ ఇంటర్నెట్ కనెక్షన్ తనిఖీ చేసి మళ్లీ ప్రయత్నించండి.",
        "Your wishlist is empty" to "మీ విష్‌లిస్ట్ ఖాళీగా ఉంది",
        "Tap the heart on any product to save it here." to "ఏదైనా ఉత్పత్తిపై హార్ట్ గుర్తును నొక్కి ఇక్కడ సేవ్ చేయండి.",
        "Continue shopping" to "షాపింగ్ కొనసాగించండి",
        "Choose Your Language" to "మీ భాషను ఎంచుకోండి",
        "Select your preferred language for SLGAN." to "SLGAN కోసం మీకు నచ్చిన భాషను ఎంచుకోండి.",
        "Selected" to "ఎంచుకున్నారు",
        "Not selected" to "ఎంచుకోలేదు",
        "Continue" to "కొనసాగించండి",
        "Log out from?" to "ఎక్కడి నుంచి లాగ్ అవుట్ చేయాలి?",
        "Choose where you want to end your SLGAN session." to "మీ SLGAN సెషన్‌ను ఎక్కడ ముగించాలో ఎంచుకోండి.",
        "Current Device" to "ప్రస్తుత పరికరం",
        "Sign out from this phone" to "ఈ ఫోన్ నుంచి సైన్ అవుట్ చేయండి",
        "All Devices" to "అన్ని పరికరాలు",
        "Requires secure server support" to "సురక్షిత సర్వర్ మద్దతు అవసరం",
        "SLGAN Assistant" to "SLGAN సహాయకుడు",
        "How Can We Help?" to "మేము మీకు ఎలా సహాయం చేయగలం?",
        "Send message" to "సందేశం పంపండి",
        "Address Options" to "చిరునామా ఎంపికలు",
        "Delete address" to "చిరునామాను తొలగించండి",
        "Edit address" to "చిరునామాను సవరించండి",
        "Set as delivery address" to "డెలివరీ చిరునామాగా సెట్ చేయండి",
        "Delete address?" to "చిరునామాను తొలగించాలా?",
        "Are you sure you want to delete this saved address?" to "ఈ సేవ్ చేసిన చిరునామాను ఖచ్చితంగా తొలగించాలనుకుంటున్నారా?",
        "Address deleted" to "చిరునామా తొలగించబడింది",
        "Address saved successfully" to "చిరునామా విజయవంతంగా సేవ్ అయింది",
        "Saved Addresses" to "సేవ్ చేసిన చిరునామాలు",
        "Keep delivery locations for future orders." to "భవిష్యత్ ఆర్డర్ల కోసం డెలివరీ చిరునామాలను సేవ్ చేయండి.",
        "No saved addresses yet" to "ఇంకా చిరునామాలు సేవ్ చేయలేదు",
        "Your saved delivery addresses will appear here." to "మీరు సేవ్ చేసిన డెలివరీ చిరునామాలు ఇక్కడ కనిపిస్తాయి.",
        "DELIVERY LOCATION" to "డెలివరీ స్థానం",
        "Do you want this order at your current location?" to "ఈ ఆర్డర్‌ను మీ ప్రస్తుత స్థానానికి డెలివరీ చేయాలా?",
        "Yes, deliver at my current location" to "అవును, నా ప్రస్తుత స్థానానికి డెలివరీ చేయండి",
        "No, at some other location" to "కాదు, వేరే స్థానానికి",
        "Are you sure?" to "మీకు ఖచ్చితంగా తెలుసా?",
        "Any changes on this screen may be lost if you continue" to "కొనసాగితే ఈ స్క్రీన్‌లో చేసిన మార్పులు కోల్పోవచ్చు",
        "Edit address details" to "చిరునామా వివరాలను సవరించండి",
        "Add address details" to "చిరునామా వివరాలను జోడించండి",
        "Address Details" to "చిరునామా వివరాలు",
        "Select a city" to "నగరాన్ని ఎంచుకోండి",
        "Select" to "ఎంచుకోండి",
        "Search for a city" to "నగరం కోసం వెతకండి",
        "Top cities" to "ప్రధాన నగరాలు",
        "Other cities" to "ఇతర నగరాలు",
        "Please select a city" to "దయచేసి నగరాన్ని ఎంచుకోండి",
        "Select an area, street" to "ప్రాంతం లేదా వీధిని ఎంచుకోండి",
        "Search for an area, street" to "ప్రాంతం లేదా వీధి కోసం వెతకండి",
        "Please select an area, street" to "దయచేసి ప్రాంతం లేదా వీధిని ఎంచుకోండి",
        "Enter complete address*" to "పూర్తి చిరునామాను నమోదు చేయండి*",
        "Please enter complete address" to "దయచేసి పూర్తి చిరునామాను నమోదు చేయండి",
        "Example: Flat/house no., building, street, landmark" to "ఉదాహరణ: ఫ్లాట్/ఇంటి నంబర్, భవనం, వీధి, గుర్తింపు స్థలం",
        "Add Google Maps link (optional)" to "Google Maps లింక్ జోడించండి (ఐచ్ఛికం)",
        "Contact Details" to "సంప్రదింపు వివరాలు",
        "Myself" to "నేను",
        "Someone else" to "మరొకరు",
        "Receiver's name*" to "స్వీకర్త పేరు*",
        "Your name*" to "మీ పేరు*",
        "Please enter receiver's name" to "దయచేసి స్వీకర్త పేరును నమోదు చేయండి",
        "Receiver's phone number*" to "స్వీకర్త ఫోన్ నంబర్*",
        "Your phone number*" to "మీ ఫోన్ నంబర్*",
        "Please enter a valid phone number" to "దయచేసి సరైన ఫోన్ నంబర్ నమోదు చేయండి",
        "Save address as" to "చిరునామాను ఇలా సేవ్ చేయండి",
        "Next" to "తదుపరి",
        "Review Address" to "చిరునామాను పరిశీలించండి",
        "Finding the nearest area on the map…" to "మ్యాప్‌లో సమీప ప్రాంతాన్ని కనుగొంటున్నాం…",
        "Delivery Details" to "డెలివరీ వివరాలు",
        "Receiver" to "స్వీకర్త",
        "  Receiver:  \$receiverName" to "  స్వీకర్త:  \$receiverName",
        "  Mobile:  \$receiverPhone" to "  మొబైల్:  \$receiverPhone",
        "  Complete address:  \$fullAddress" to "  పూర్తి చిరునామా:  \$fullAddress",
        "Save Address" to "చిరునామాను సేవ్ చేయండి",
        "Country" to "దేశం",
        "Select country" to "దేశాన్ని ఎంచుకోండి",
        "Date of birth" to "పుట్టిన తేదీ",
        "Select date of birth" to "పుట్టిన తేదీని ఎంచుకోండి",
        "Choose date" to "తేదీని ఎంచుకోండి",
        "Save profile" to "ప్రొఫైల్‌ను సేవ్ చేయండి",
        "Couldn't save profile. Please try again." to "ప్రొఫైల్‌ను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "Request To Delete Account" to "ఖాతా తొలగింపు కోసం అభ్యర్థించండి",
        "Request to closure of your account" to "మీ ఖాతాను మూసివేయమని అభ్యర్థించండి",
        "Request account deletion" to "ఖాతా తొలగింపు అభ్యర్థన",
        "This will open a request email to SLGAN support. Your account will not be deleted automatically. Do you want to continue?" to "ఇది SLGAN మద్దతుకు అభ్యర్థన ఇమెయిల్‌ను తెరుస్తుంది. మీ ఖాతా ఆటోమేటిక్‌గా తొలగించబడదు. కొనసాగించాలా?",
        "No email app is available." to "ఇమెయిల్ యాప్ అందుబాటులో లేదు.",
        "About Us" to "మా గురించి",
        "Our Heritage" to "మన వారసత్వం",
        "Contact SLGAN" to "SLGANను సంప్రదించండి",
        "We are happy to hear from you. Contact us or visit our location." to "మీ నుంచి వినడం మాకు ఆనందం. మమ్మల్ని సంప్రదించండి లేదా మా చిరునామాను సందర్శించండి.",
        "Business Address" to "వ్యాపార చిరునామా",
        "Get directions" to "దారి చూపించండి",
        "Call SLGAN" to "SLGANకు కాల్ చేయండి",
        "Your Information" to "మీ సమాచారం",
        "About And Support" to "మా గురించి మరియు సహాయం",
        "Other Information" to "ఇతర సమాచారం",
        "Your wellness, rooted in tradition" to "సంప్రదాయంలో పాతుకుపోయిన మీ ఆరోగ్యం",
        "Explore our Ayurvedic collection and updates." to "మా ఆయుర్వేద ఉత్పత్తులు మరియు తాజా సమాచారాన్ని చూడండి.",
        "Help and support" to "సహాయం మరియు మద్దతు",
        "Personal information" to "వ్యక్తిగత సమాచారం",
        "My orders" to "నా ఆర్డర్లు", "My cart" to "నా కార్ట్", "Need help?" to "సహాయం కావాలా?", "Address book" to "చిరునామా పుస్తకం", "My Wishlist" to "నా విష్‌లిస్ట్", "Diseases and remedies" to "వ్యాధులు మరియు నివారణలు",
        "Share the app" to "యాప్‌ను షేర్ చేయండి",
        "Please sign in again to save your language." to "మీ భాషను సేవ్ చేయడానికి మళ్లీ సైన్ ఇన్ చేయండి.",
        "Language preference saved." to "భాష ప్రాధాన్యత సేవ్ అయింది.",
        "Couldn't save language. Please try again." to "భాషను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "Log out from?" to "ఎక్కడి నుంచి లాగ్ అవుట్ చేయాలి?",
        "Choose where you want to end your SLGAN session." to "మీ SLGAN సెషన్‌ను ఎక్కడ ముగించాలో ఎంచుకోండి.",
        "Sign out from current device" to "ప్రస్తుత పరికరం నుంచి సైన్ అవుట్ చేయండి",
        "All devices" to "అన్ని పరికరాలు",
        "Could not open this image. Please choose another." to "ఈ చిత్రాన్ని తెరవలేకపోయాం. దయచేసి మరొకదాన్ని ఎంచుకోండి.",
        "Profile picture updated." to "ప్రొఫైల్ చిత్రం నవీకరించబడింది.",
        "Please try again" to "దయచేసి మళ్లీ ప్రయత్నించండి",
        "Could not process image. Please choose another." to "చిత్రాన్ని ప్రాసెస్ చేయలేకపోయాం. దయచేసి మరొకదాన్ని ఎంచుకోండి.",
        "Account profile picture" to "ఖాతా ప్రొఫైల్ చిత్రం",
        "Account profile" to "ఖాతా ప్రొఫైల్",
        "Edit profile picture" to "ప్రొఫైల్ చిత్రాన్ని సవరించండి",
        "Selected" to "ఎంచుకున్నారు",
        "Not selected" to "ఎంచుకోలేదు",
        "Current Device" to "ప్రస్తుత పరికరం",
        "All Devices" to "అన్ని పరికరాలు",
        "Requires secure server support" to "సురక్షిత సర్వర్ మద్దతు అవసరం",
        "SLGAN Assistant" to "SLGAN సహాయకుడు",
        "Help & Support · Telugu + English" to "సహాయం మరియు మద్దతు · తెలుగు + English",
        "Send message" to "సందేశం పంపండి",
        "Your order was saved." to "మీ ఆర్డర్ సేవ్ అయింది.",
        "Your order could not be placed." to "మీ ఆర్డర్‌ను నమోదు చేయలేకపోయాం.",
        "Order successful" to "ఆర్డర్ విజయవంతమైంది",
        "Order failed" to "ఆర్డర్ విఫలమైంది",
        "ORDER STATUS" to "ఆర్డర్ స్థితి",
        "Pending verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Date not available" to "తేదీ అందుబాటులో లేదు",
        "Unable to share order items" to "ఆర్డర్ వస్తువులను షేర్ చేయలేకపోయాం",
        "Could not identify this order." to "ఈ ఆర్డర్‌ను గుర్తించలేకపోయాం.",
        "Order deleted" to "ఆర్డర్ తొలగించబడింది",
        "Could not delete order" to "ఆర్డర్‌ను తొలగించలేకపోయాం",
        "Order ID copied" to "ఆర్డర్ ID కాపీ చేయబడింది",
        "Show all disease options" to "అన్ని వ్యాధి ఎంపికలను చూపించండి",
        "Close disease details and clear search" to "వ్యాధి వివరాలను మూసివేసి శోధనను క్లియర్ చేయండి",
        "Clear search" to "శోధనను క్లియర్ చేయండి",
        "ADD" to "జోడించండి",
        "Add another" to "మరొకటి జోడించండి",
        "Date not available" to "తేదీ అందుబాటులో లేదు",
        "Arrived at \$created" to "\$created న డెలివరీ అయింది",
        "Order status: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}" to "ఆర్డర్ స్థితి: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
        "Name not provided" to "పేరు ఇవ్వలేదు",
        "Patent Product" to "పేటెంట్ ఉత్పత్తి",
        "Signature Products" to "ప్రత్యేక ఉత్పత్తులు",
        "Product details are not available in the database yet." to "ఉత్పత్తి వివరాలు ఇంకా డేటాబేస్‌లో అందుబాటులో లేవు.",
        "Add to cart" to "కార్ట్‌కు జోడించండి",
        " in cart" to " కార్ట్‌లో ఉన్నాయి",
        "Rate product" to "ఉత్పత్తికి రేటింగ్ ఇవ్వండి",
        "Order saved. Email service confirmed." to "ఆర్డర్ సేవ్ అయింది. ఇమెయిల్ సేవ నిర్ధారించింది.",
        "Order saved, but email failed. " to "ఆర్డర్ సేవ్ అయింది, కానీ ఇమెయిల్ పంపడం విఫలమైంది. ",
        "SLGAN catalogue products are shown for reference only. There is no single medicine suitable for all diseases. Choose treatment only after a qualified healthcare professional identifies the condition." to "SLGAN ఉత్పత్తుల జాబితా సూచన కోసం మాత్రమే చూపబడుతుంది. అన్ని వ్యాధులకు సరిపోయే ఒకే ఔషధం లేదు. అర్హత కలిగిన వైద్య నిపుణుడు వ్యాధిని నిర్ధారించిన తర్వాత మాత్రమే చికిత్సను ఎంచుకోండి.",
        "Do not self-diagnose or start medicines based only on general information." to "సాధారణ సమాచారాన్ని మాత్రమే ఆధారంగా చేసుకుని స్వయంగా వ్యాధిని నిర్ధారించుకోవద్దు లేదా మందులు ప్రారంభించవద్దు.",
        "Consult a qualified healthcare professional for diagnosis and a treatment plan suited to the specific condition." to "వ్యాధి నిర్ధారణ మరియు తగిన చికిత్స ప్రణాళిక కోసం అర్హత కలిగిన వైద్య నిపుణుడిని సంప్రదించండి.",
        "Use medicines only as prescribed; do not stop prescribed treatment without medical advice." to "వైద్యుడు సూచించిన విధంగానే మందులు వాడండి; వైద్య సలహా లేకుండా చికిత్సను ఆపవద్దు.",
        "Maintain appropriate hygiene, balanced nutrition, safe physical activity, and recommended vaccinations where applicable." to "తగిన పరిశుభ్రత, సమతుల ఆహారం, సురక్షితమైన శారీరక చురుకుదనం పాటించండి; అవసరమైన టీకాలు వేయించుకోండి.",
        "Seek urgent medical care for severe symptoms such as breathing difficulty, chest pain, confusion, severe dehydration, or sudden worsening." to "శ్వాస తీసుకోవడంలో ఇబ్బంది, ఛాతి నొప్పి, గందరగోళం, తీవ్రమైన నీరసం లేదా లక్షణాలు అకస్మాత్తుగా పెరిగితే వెంటనే వైద్య సహాయం పొందండి.",
        "Information not available" to "సమాచారం అందుబాటులో లేదు",
        "Collapse \$title" to "\$title మూసివేయండి",
        "Expand \$title" to "\$title విస్తరించండి",
        "Profile picture updated." to "ప్రొఫైల్ చిత్రం నవీకరించబడింది.",
        "Your Information" to "మీ సమాచారం",
        "About And Support" to "మా గురించి మరియు సహాయం",
        "Other Information" to "ఇతర సమాచారం",
        "Privacy Policy" to "గోప్యతా విధానం",
        "Terms & Conditions" to "నిబంధనలు మరియు షరతులు",
        "Social Media" to "సోషల్ మీడియా",
        "Follow" to "అనుసరించండి",
        "Follow us on Facebook" to "Facebookలో మమ్మల్ని అనుసరించండి",
        "Open Facebook" to "Facebook తెరవండి",
        "Account Privacy" to "ఖాతా గోప్యత",
        "Account Privacy And Policy" to "ఖాతా గోప్యత మరియు విధానం",
        "Read less" to "తక్కువ చదవండి",
        "Read more" to "మరింత చదవండి",
        "Request To Delete Account" to "ఖాతా తొలగింపు కోసం అభ్యర్థించండి",
        "Request to closure of your account" to "మీ ఖాతాను మూసివేయమని అభ్యర్థించండి",
        "Contact SLGAN" to "SLGANను సంప్రదించండి",
        "Business Address" to "వ్యాపార చిరునామా",
        "Get directions" to "దారి చూపించండి",
        "Call SLGAN" to "SLGANకు కాల్ చేయండి",
        "Saved Addresses" to "సేవ్ చేసిన చిరునామాలు",
        "Add address" to "చిరునామా జోడించండి",
        "Select address" to "చిరునామాను ఎంచుకోండి",
        "Selected address" to "ఎంచుకున్న చిరునామా",
        "Share address" to "చిరునామాను షేర్ చేయండి",
        "Close" to "మూసివేయండి",
        "Back" to "వెనక్కి",
        "Delete" to "తొలగించండి",
        "Cancel" to "రద్దు చేయండి",
        "Yes" to "అవును",
        "No" to "కాదు",
        "Edit" to "సవరించండి",
        "Phone Number" to "ఫోన్ నంబర్",
        "Mobile number" to "మొబైల్ నంబర్",
        "Complete address" to "పూర్తి చిరునామా",
        "Receiver" to "స్వీకర్త",
        "Select date of birth" to "పుట్టిన తేదీని ఎంచుకోండి",
        "Save profile" to "ప్రొఫైల్‌ను సేవ్ చేయండి",
        "Products Catalogue" to "ఉత్పత్తుల కేటలాగ్",
        "Products" to "ఉత్పత్తులు", "Orders" to "ఆర్డర్లు", "Diseases" to "వ్యాధులు", "Customers" to "వినియోగదారులు",
        "Dashboard" to "డాష్‌బోర్డ్", "Welcome back" to "తిరిగి స్వాగతం", "Administrator" to "నిర్వాహకుడు",
        "Live Overview" to "ప్రత్యక్ష సమీక్ష", "Current records across your store" to "మీ స్టోర్‌లోని ప్రస్తుత రికార్డులు",
        "LIVE" to "ప్రత్యక్షం", "OVERVIEW" to "సారాంశం", "MANAGEMENT" to "నిర్వహణ",
        "Orders Management" to "ఆర్డర్ల నిర్వహణ", "Diseases Management" to "వ్యాధుల నిర్వహణ", "Customers Management" to "వినియోగదారుల నిర్వహణ",
        "App Themes" to "యాప్ థీమ్‌లు", "Sliders & Banners" to "స్లైడర్లు & బ్యానర్లు",
        "Add, edit and manage products" to "ఉత్పత్తులను జోడించండి, సవరించండి మరియు నిర్వహించండి",
        "View and manage customer orders" to "కస్టమర్ ఆర్డర్లను చూడండి మరియు నిర్వహించండి",
        "Manage disease records" to "వ్యాధుల రికార్డులను నిర్వహించండి", "View registered customers" to "నమోదైన వినియోగదారులను చూడండి",
        "Apply Default or Theme 1 across the app" to "యాప్ అంతటా డిఫాల్ట్ లేదా థీమ్ 1 వర్తింపజేయండి",
        "Manage home page slider images and visibility" to "హోమ్ పేజీ స్లైడర్ చిత్రాలు మరియు కనిపించే విధానాన్ని నిర్వహించండి",
        "Choose the appearance for the customer app. The admin screens keep their separate neutral theme." to "కస్టమర్ యాప్ రూపాన్ని ఎంచుకోండి. అడ్మిన్ స్క్రీన్‌లు ప్రత్యేక న్యూట్రల్ థీమ్‌ను ఉపయోగిస్తాయి.",
        "Classic Maroon" to "క్లాసిక్ మెరూన్", "Botanical Green" to "బొటానికల్ గ్రీన్", "Dark Mode" to "డార్క్ మోడ్", "Green Mode" to "గ్రీన్ మోడ్",
        "Add Product" to "ఉత్పత్తిని జోడించండి", "Edit Product" to "ఉత్పత్తిని సవరించండి", "Product Name" to "ఉత్పత్తి పేరు",
        "Description" to "వివరణ", "Usage" to "వినియోగం", "Ingredients" to "పదార్థాలు", "Cures" to "ఉపశమనాలు",
        "Available" to "అందుబాటులో ఉంది", "Out of stock" to "స్టాక్ లేదు", "Active" to "యాక్టివ్", "Hidden" to "దాచబడింది",
        "Order ID" to "ఆర్డర్ ఐడి", "Order Status" to "ఆర్డర్ స్థితి", "Pending Verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Customer Details" to "కస్టమర్ వివరాలు", "Delivery Address" to "డెలివరీ చిరునామా", "Update Status" to "స్థితిని నవీకరించండి",
        "Mark as Delivered" to "డెలివరీ అయినట్లు గుర్తించండి", "No records found" to "రికార్డులు కనబడలేదు",
        "Refresh" to "రిఫ్రెష్", "Upload Image" to "చిత్రాన్ని అప్‌లోడ్ చేయండి", "Remove Image" to "చిత్రాన్ని తొలగించండి",
        "Disease Name" to "వ్యాధి పేరు", "Add Disease" to "వ్యాధిని జోడించండి", "Update Disease" to "వ్యాధిని నవీకరించండి",
        "Customer Name" to "కస్టమర్ పేరు", "Registered Customers" to "నమోదైన వినియోగదారులు",
        "View Order" to "ఆర్డర్‌ను చూడండి", "Subtotal" to "ఉప మొత్తం", "Delivery" to "డెలివరీ", "Grand Total" to "మొత్తం",
        "Invoice" to "ఇన్వాయిస్", "Open invoice PDF" to "ఇన్వాయిస్ PDF తెరవండి",
        "Manage, add and organize your products" to "మీ ఉత్పత్తులను నిర్వహించండి, జోడించండి మరియు క్రమబద్ధీకరించండి",
        "Search products…" to "ఉత్పత్తులను వెతకండి…",
        " products" to " ఉత్పత్తులు",
        "Hide all" to "అన్నీ దాచండి",
        "Show all" to "అన్నీ చూపించండి",
        "Product" to "ఉత్పత్తి",
        "Price" to "ధర",
        "Stock" to "స్టాక్",
        "Show/Hide" to "చూపించు/దాచు",
        "Actions" to "చర్యలు",
        "Could not load products" to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం",
        "No products found" to "ఉత్పత్తులు కనబడలేదు",
        "Hidden" to "దాచబడింది",
        "Product in stock" to "ఉత్పత్తి స్టాక్‌లో ఉంది",
        "Product out of stock" to "ఉత్పత్తి స్టాక్‌లో లేదు",
        "Could not update product stock" to "ఉత్పత్తి స్టాక్‌ను నవీకరించలేకపోయాం",
        "Product shown" to "ఉత్పత్తి చూపించబడింది",
        "Product hidden" to "ఉత్పత్తి దాచబడింది",
        "View details" to "వివరాలు చూడండి",
        "Show product" to "ఉత్పత్తిని చూపించండి",
        "Hide product" to "ఉత్పత్తిని దాచండి",
        "Delete product" to "ఉత్పత్తిని తొలగించండి",
        "Add New Product" to "కొత్త ఉత్పత్తిని జోడించండి",
        "Delete product?" to "ఉత్పత్తిని తొలగించాలా?",
        "Delete" to "తొలగించండి",
        "Cancel" to "రద్దు చేయండి",
        "View Product" to "ఉత్పత్తిని చూడండి",
        "Complete catalogue information" to "పూర్తి కేటలాగ్ సమాచారం",
        "Edit" to "సవరించండి",
        "IN STOCK" to "స్టాక్‌లో ఉంది",
        "OUT OF STOCK" to "స్టాక్ లేదు",
        "PRODUCT INFORMATION" to "ఉత్పత్తి సమాచారం",
        "No additional fields are saved for this product." to "ఈ ఉత్పత్తికి అదనపు వివరాలు సేవ్ చేయలేదు.",
        "Add product" to "ఉత్పత్తిని జోడించండి",
        "Edit product" to "ఉత్పత్తిని సవరించండి",
        "Product Information" to "ఉత్పత్తి సమాచారం",
        "Product name *" to "ఉత్పత్తి పేరు *",
        "Product name (Telugu)" to "ఉత్పత్తి పేరు (తెలుగు)",
        "Product name (Hindi)" to "ఉత్పత్తి పేరు (హిందీ)",
        "Category *" to "వర్గం *",
        "Add category" to "వర్గాన్ని జోడించండి",
        "Price (₹) *" to "ధర (₹) *",
        "Weight *" to "బరువు *",
        "Units *" to "యూనిట్లు *",
        "Select weight unit" to "బరువు యూనిట్ ఎంచుకోండి",
        "No units found in product database" to "ఉత్పత్తి డేటాబేస్‌లో యూనిట్లు కనబడలేదు",
        "Qty *" to "పరిమాణం *",
        "Product availability" to "ఉత్పత్తి లభ్యత",
        "Product image" to "ఉత్పత్తి చిత్రం",
        "Product image preview" to "ఉత్పత్తి చిత్రం ప్రివ్యూ",
        "Upload product image" to "ఉత్పత్తి చిత్రాన్ని అప్‌లోడ్ చేయండి",
        "The image is stored online using a filename based on the product name and updates in the app after saving." to "ఉత్పత్తి పేరు ఆధారంగా ఫైల్ పేరుతో చిత్రం ఆన్‌లైన్‌లో నిల్వ అవుతుంది. సేవ్ చేసిన తర్వాత యాప్‌లో నవీకరించబడుతుంది.",
        "Image URL (optional)" to "చిత్ర URL (ఐచ్ఛికం)",
        "Description" to "వివరణ",
        "Usage / directions" to "వాడే విధానం / సూచనలు",
        "Cures / indications" to "ఉపయోగాలు / సూచనలు",
        "Major ingredients" to "ప్రధాన పదార్థాలు",
        "Is signature product" to "ప్రత్యేక ఉత్పత్తి",
        "Yes" to "అవును",
        "No" to "కాదు",
        "Enter category" to "వర్గాన్ని నమోదు చేయండి",
        "Category added" to "వర్గం జోడించబడింది",
        "Could not add category" to "వర్గాన్ని జోడించలేకపోయాం",
        "Saving…" to "సేవ్ అవుతోంది…",
        "Add" to "జోడించండి",
        "Save product" to "ఉత్పత్తిని సేవ్ చేయండి",
        "Product saved successfully." to "ఉత్పత్తి విజయవంతంగా సేవ్ అయింది.",
        "Failed to save product" to "ఉత్పత్తిని సేవ్ చేయడం విఫలమైంది",
        "Save failed" to "సేవ్ చేయడం విఫలమైంది",
        "SLGAN ADMIN" to "SLGAN నిర్వాహకుడు",
        "Dashboard" to "డ్యాష్‌బోర్డ్",
        "Welcome back" to "తిరిగి స్వాగతం",
        "Administrator" to "నిర్వాహకుడు",
        "Sign out" to "సైన్ అవుట్",
        "Live Overview" to "ప్రత్యక్ష అవలోకనం",
        "Current records across your store" to "మీ స్టోర్‌లోని ప్రస్తుత రికార్డులు",
        "App Themes" to "యాప్ థీమ్‌లు",
        "Choose the appearance for the customer app. The admin screens keep their separate neutral theme." to "కస్టమర్ యాప్ రూపాన్ని ఎంచుకోండి. అడ్మిన్ స్క్రీన్‌లు ప్రత్యేక న్యూట్రల్ థీమ్‌లో ఉంటాయి.",
        "Apply" to "వర్తింపజేయండి",
        "Theme changes affect colors and backgrounds only. Home page structure, product data, images and navigation remain unchanged." to "థీమ్ మార్పులు రంగులు మరియు నేపథ్యాలకే వర్తిస్తాయి. హోమ్ పేజీ నిర్మాణం, ఉత్పత్తి డేటా, చిత్రాలు మరియు నావిగేషన్ మారవు.",
        "HOME PAGE SLIDERS" to "హోమ్ పేజీ స్లైడర్లు",
        "Choose Active Design" to "సక్రియ డిజైన్‌ను ఎంచుకోండి",
        "Show Slider On Home" to "హోమ్‌లో స్లైడర్ చూపించండి",
        "Slider is visible on the home page" to "హోమ్ పేజీలో స్లైడర్ కనిపిస్తుంది",
        "Slider is hidden from the home page" to "హోమ్ పేజీ నుంచి స్లైడర్ దాచబడింది",
        "Choose a slider design and tap Apply. Slider 4 automatically follows the selected app theme." to "స్లైడర్ డిజైన్‌ను ఎంచుకుని వర్తింపజేయండి. స్లైడర్ 4 ఎంచుకున్న యాప్ థీమ్‌ను అనుసరిస్తుంది.",
        "SELECTED" to "ఎంచుకున్నారు",
        "Applying…" to "వర్తింపజేస్తోంది…",
        "Add disease" to "వ్యాధిని జోడించండి",
        "Add a disease to the app catalogue." to "యాప్ కేటలాగ్‌కు ఒక వ్యాధిని జోడించండి.",
        "Disease name *" to "వ్యాధి పేరు *",
        "e.g. Cough" to "ఉదా: దగ్గు",
        "Enter details (optional)" to "వివరాలు నమోదు చేయండి (ఐచ్ఛికం)",
        "Save disease" to "వ్యాధిని సేవ్ చేయండి",
        "Diseases" to "వ్యాధులు",
        "Customers" to "కస్టమర్లు",
        "Manage customer accounts" to "కస్టమర్ ఖాతాలను నిర్వహించండి",
        "Search name, email or mobile…" to "పేరు, ఇమెయిల్ లేదా మొబైల్ వెతకండి…",
        "Latest" to "తాజావి",
        "Name A–Z" to "పేరు A–Z",
        "Name Z–A" to "పేరు Z–A",
        "Enabled" to "ప్రారంభించబడింది",
        "Disabled" to "నిలిపివేయబడింది",
        "Account status" to "ఖాతా స్థితి",
        "No customers yet" to "ఇంకా కస్టమర్లు లేరు",
        "No matching customers" to "సరిపోలే కస్టమర్లు లేరు",
        "Try changing your search or filters." to "మీ శోధన లేదా ఫిల్టర్లను మార్చి చూడండి.",
        "Email not provided" to "ఇమెయిల్ ఇవ్వలేదు",
        "Customer Details" to "కస్టమర్ వివరాలు",
        "Customer ID: \$uid" to "కస్టమర్ ID: \$uid",
        "Edit Customer" to "కస్టమర్‌ను సవరించండి",
        "Email" to "ఇమెయిల్",
        "Customer updated" to "కస్టమర్ వివరాలు నవీకరించబడ్డాయి",
        "Manage Orders" to "ఆర్డర్లను నిర్వహించండి",
        "Orders Library" to "ఆర్డర్ల లైబ్రరీ",
        "Review and manage customer orders" to "కస్టమర్ ఆర్డర్లను పరిశీలించి నిర్వహించండి",
        "Search order" to "ఆర్డర్ వెతకండి",
        "Order ID, customer or mobile" to "ఆర్డర్ ID, కస్టమర్ లేదా మొబైల్",
        "Search orders" to "ఆర్డర్లను వెతకండి",
        "Filter Orders By Status" to "స్థితి ఆధారంగా ఆర్డర్లను ఫిల్టర్ చేయండి",
        "Pending" to "పెండింగ్‌లో ఉంది",
        "Pending Verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Accepted" to "ఆమోదించబడింది",
        "Rejected" to "తిరస్కరించబడింది",
        "No orders yet" to "ఇంకా ఆర్డర్లు లేవు",
        "No orders match your search" to "మీ శోధనకు సరిపడే ఆర్డర్లు లేవు",
        "Item details are not available for this order." to "ఈ ఆర్డర్‌కు వస్తువుల వివరాలు అందుబాటులో లేవు.",
        "Qty: " to "పరిమాణం: ",
        " each" to " ఒక్కొక్కటి",
        " more items" to " మరిన్ని వస్తువులు",
        "Total amount" to "మొత్తం మొత్తం",
        "Hide details" to "వివరాలను దాచండి",
        "Information not available" to "సమాచారం అందుబాటులో లేదు",
        "Reject" to "తిరస్కరించండి",
        "Approve" to "ఆమోదించండి",
        "Order rejected." to "ఆర్డర్ తిరస్కరించబడింది.",
        "Order approved." to "ఆర్డర్ ఆమోదించబడింది.",
        "Try again" to "మళ్లీ ప్రయత్నించండి",
        "Social Media" to "సోషల్ మీడియా",
        "Connect with SLGAN" to "SLGANతో కనెక్ట్ అవ్వండి",
        "Follow Sri Lakshmi Ganapathi Ayurveda Nilayam on social media for updates and information." to "తాజా సమాచారం మరియు అప్‌డేట్‌ల కోసం శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయాన్ని సోషల్ మీడియాలో అనుసరించండి.",
        "View live followers on Instagram" to "Instagramలో తాజా ఫాలోవర్ల సంఖ్య చూడండి",
        "Follow" to "అనుసరించండి",
        "Follow us on Facebook" to "Facebookలో మమ్మల్ని అనుసరించండి",
        "Open Facebook" to "Facebook తెరవండి",
        "Account Privacy" to "ఖాతా గోప్యత",
        "Account Privacy And Policy" to "ఖాతా గోప్యత మరియు విధానం",
        "Read less" to "తక్కువ చదవండి",
        "Read more" to "మరింత చదవండి",
        "About Us" to "మా గురించి",
        "Our Heritage" to "మన వారసత్వం",
        "Contact SLGAN" to "SLGANను సంప్రదించండి",
        "We are happy to hear from you. Contact us or visit our location." to "మీ నుంచి వినడం మాకు ఆనందం. మమ్మల్ని సంప్రదించండి లేదా మా చిరునామాను సందర్శించండి.",
        "Business Address" to "వ్యాపార చిరునామా",
        "Get directions" to "దారి చూపించండి",
        "Call SLGAN" to "SLGANకు కాల్ చేయండి",
        "Your wishlist" to "మీ విష్‌లిస్ట్",
        "saved products" to "సేవ్ చేసిన ఉత్పత్తులు",
        "Tap the heart on any product to save it here." to "ఏదైనా ఉత్పత్తిపై హార్ట్ గుర్తును నొక్కి ఇక్కడ సేవ్ చేయండి.",
        "Continue shopping" to "షాపింగ్ కొనసాగించండి",
        "Patent Product" to "పేటెంట్ ఉత్పత్తి",
        "Signature product" to "ప్రత్యేక ఉత్పత్తి",
        "Add another" to "మరొకటి జోడించండి",
        "Order successful" to "ఆర్డర్ విజయవంతమైంది",
        "Order failed" to "ఆర్డర్ విఫలమైంది",
        "Your order has been saved" to "మీ ఆర్డర్ సేవ్ అయింది",
        "Please review your order" to "దయచేసి మీ ఆర్డర్‌ను పరిశీలించండి",
        "Your order was saved." to "మీ ఆర్డర్ సేవ్ అయింది.",
        "Your order could not be placed." to "మీ ఆర్డర్‌ను నమోదు చేయలేకపోయాం.",
        "ORDER STATUS" to "ఆర్డర్ స్థితి",
        "Pending verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Arrived at \$created" to "\$created న డెలివరీ అయింది",
        "Order status: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}" to "ఆర్డర్ స్థితి: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
        "Date not available" to "తేదీ అందుబాటులో లేదు",
        "Unable to share order items" to "ఆర్డర్ వస్తువులను షేర్ చేయలేకపోయాం",
        "Could not identify this order." to "ఈ ఆర్డర్‌ను గుర్తించలేకపోయాం.",
        "Order deleted" to "ఆర్డర్ తొలగించబడింది",
        "Could not delete order" to "ఆర్డర్‌ను తొలగించలేకపోయాం",
        "Order ID copied" to "ఆర్డర్ ID కాపీ చేయబడింది",
        "Show all disease options" to "అన్ని వ్యాధి ఎంపికలను చూపించండి",
        "Close disease details and clear search" to "వ్యాధి వివరాలను మూసివేసి శోధనను క్లియర్ చేయండి",
        "Collapse \$title" to "\$title మూసివేయండి",
        "Expand \$title" to "\$title విస్తరించండి",
        "Clear search" to "శోధనను క్లియర్ చేయండి",
        "Decrease quantity" to "పరిమాణాన్ని తగ్గించండి",
        "Increase quantity" to "పరిమాణాన్ని పెంచండి",
        "Add to Cart" to "కార్ట్‌కు జోడించండి",
        "Out of stock" to "స్టాక్ లేదు",
        "Information not available." to "సమాచారం అందుబాటులో లేదు.",
        "Customer Ratings & Reviews" to "కస్టమర్ రేటింగ్‌లు మరియు సమీక్షలు",
        "Write A Review" to "సమీక్ష రాయండి",
        "Close review" to "సమీక్షను మూసివేయండి",
        "Your review (optional)" to "మీ సమీక్ష (ఐచ్ఛికం)",
        "Share your experience with this product" to "ఈ ఉత్పత్తితో మీ అనుభవాన్ని పంచుకోండి",
        "Saved! Thank you for your review." to "సేవ్ అయింది! మీ సమీక్షకు ధన్యవాదాలు.",
        "Review saved successfully" to "సమీక్ష విజయవంతంగా సేవ్ అయింది",
        "Could not save rating. Please try again." to "రేటింగ్‌ను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "Help & Support · Telugu + English" to "సహాయం మరియు మద్దతు · తెలుగు + English",
        "Quick answers for products, orders and delivery. AI replies become available when the secure support service is configured." to "ఉత్పత్తులు, ఆర్డర్లు మరియు డెలివరీ గురించి త్వరిత సమాధానాలు. సురక్షిత మద్దతు సేవ ఏర్పాటు చేసిన తర్వాత AI సమాధానాలు అందుబాటులో ఉంటాయి.",
        "Thinking…" to "సమాధానం సిద్ధం చేస్తోంది…",
        "Type in Telugu or English…" to "తెలుగు లేదా English‌లో టైప్ చేయండి…",
        "AI can make mistakes. For medical advice or dosage, consult a qualified clinician." to "AI సమాధానాల్లో తప్పులు ఉండవచ్చు. వైద్య సలహా లేదా మోతాదు కోసం అర్హత కలిగిన వైద్యుడిని సంప్రదించండి.",
        "SLGAN support assistant" to "SLGAN సహాయక సేవ",
        "Send message" to "సందేశం పంపండి",
        "Account profile picture" to "ఖాతా ప్రొఫైల్ చిత్రం",
        "Account profile" to "ఖాతా ప్రొఫైల్",
        "Edit profile picture" to "ప్రొఫైల్ చిత్రాన్ని సవరించండి",
        "Could not load saved addresses" to "సేవ్ చేసిన చిరునామాలను లోడ్ చేయలేకపోయాం",
        "Default delivery address updated" to "డిఫాల్ట్ డెలివరీ చిరునామా నవీకరించబడింది",
        "Could not update default address" to "డిఫాల్ట్ చిరునామాను నవీకరించలేకపోయాం",
        "Add address" to "చిరునామా జోడించండి",
        "Selected address" to "ఎంచుకున్న చిరునామా",
        "Select address" to "చిరునామాను ఎంచుకోండి",
        "Address Options" to "చిరునామా ఎంపికలు",
        "Share address" to "చిరునామాను షేర్ చేయండి",
        "Couldn't detect your location. Please enter the address manually." to "మీ స్థానాన్ని గుర్తించలేకపోయాం. దయచేసి చిరునామాను మాన్యువల్‌గా నమోదు చేయండి.",
        "Address not found. Try entering a nearby area and city to place the pin." to "చిరునామా కనబడలేదు. పిన్‌ను ఉంచడానికి సమీప ప్రాంతం మరియు నగరాన్ని నమోదు చేయండి.",
        "Please wait for the map or enter a nearby area and city to select your delivery pin." to "మ్యాప్ కోసం వేచి ఉండండి లేదా డెలివరీ పిన్ ఎంచుకోవడానికి సమీప ప్రాంతం మరియు నగరాన్ని నమోదు చేయండి.",
        "Please sign in to save an address." to "చిరునామాను సేవ్ చేయడానికి సైన్ ఇన్ చేయండి.",
        "Could not save address. Please try again." to "చిరునామాను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "Could not verify saved addresses. Please try again." to "సేవ్ చేసిన చిరునామాలను ధృవీకరించలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "Address deleted" to "చిరునామా తొలగించబడింది",
        "Address saved successfully" to "చిరునామా విజయవంతంగా సేవ్ అయింది",
        "Profile picture updated." to "ప్రొఫైల్ చిత్రం నవీకరించబడింది.",
        "Could not process image. Please choose another." to "చిత్రాన్ని ప్రాసెస్ చేయలేకపోయాం. దయచేసి మరొకదాన్ని ఎంచుకోండి.",
        "Please sign in again to save your language." to "మీ భాషను సేవ్ చేయడానికి మళ్లీ సైన్ ఇన్ చేయండి.",
        "Language preference saved." to "భాష ప్రాధాన్యత సేవ్ అయింది.",
        "Couldn't save language. Please try again." to "భాషను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "This app can sign out this device only. Signing out all devices requires secure server support." to "ఈ యాప్‌లో ఈ పరికరం నుంచి మాత్రమే సైన్ అవుట్ చేయవచ్చు. అన్ని పరికరాల నుంచి సైన్ అవుట్ చేయడానికి సురక్షిత సర్వర్ మద్దతు అవసరం.",
        "Do you want this order at your current location?" to "ఈ ఆర్డర్‌ను మీ ప్రస్తుత స్థానానికి డెలివరీ చేయాలా?",
        "Selected" to "ఎంచుకున్నారు",
        "Not selected" to "ఎంచుకోలేదు",
        "Sign out from current device" to "ప్రస్తుత పరికరం నుంచి సైన్ అవుట్ చేయండి",
        "All devices" to "అన్ని పరికరాలు",
        "AI can make mistakes. For medical advice or dosage, consult a qualified clinician." to "AI సమాధానాల్లో తప్పులు ఉండవచ్చు. వైద్య సలహా లేదా మోతాదు కోసం అర్హత కలిగిన వైద్యుడిని సంప్రదించండి.",
        "Collapse \$title" to "\$title మూసివేయండి",
        "Expand \$title" to "\$title విస్తరించండి",
        "Address details unavailable" to "చిరునామా వివరాలు అందుబాటులో లేవు",
        "Name not provided" to "పేరు ఇవ్వలేదు",
        "View Products" to "ఉత్పత్తులను చూడండి",
        "Name not provided" to "పేరు ఇవ్వలేదు",
        "Complete address not available" to "పూర్తి చిరునామా అందుబాటులో లేదు",
        "Delivery address" to "డెలివరీ చిరునామా",
        "Other cities" to "ఇతర నగరాలు",
        "Search Results" to "శోధన ఫలితాలు",
        "Edit address details" to "చిరునామా వివరాలను సవరించండి",
        "Add address details" to "చిరునామా వివరాలను జోడించండి",
        "Receiver's name*" to "స్వీకర్త పేరు*",
        "Your name*" to "మీ పేరు*",
        "Receiver's phone number*" to "స్వీకర్త ఫోన్ నంబర్*",
        "Your phone number*" to "మీ ఫోన్ నంబర్*",
        "Any changes on this screen may be lost if you continue" to "కొనసాగితే ఈ స్క్రీన్‌లో చేసిన మార్పులు కోల్పోవచ్చు",
        "Yes" to "అవును",
        "No" to "కాదు",
        " items" to " వస్తువులు",
        " products" to " ఉత్పత్తులు",
        "orders" to "ఆర్డర్లు",
        "Orders" to "ఆర్డర్లు",
        "All" to "అన్నీ",
        "Pending" to "పెండింగ్‌లో ఉంది",
        "Pending Verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Accepted" to "ఆమోదించబడింది",
        "Rejected" to "తిరస్కరించబడింది",
        "Customer" to "కస్టమర్",
        "No phone" to "ఫోన్ నంబర్ లేదు",
        "ORDER ITEMS (" to "ఆర్డర్ వస్తువులు (",
        "Qty: " to "పరిమాణం: ",
        " each" to " ఒక్కొక్కటి",
        " more items" to " మరిన్ని వస్తువులు",
        "Total amount" to "మొత్తం మొత్తం",
        "Hide details" to "వివరాలను దాచండి",
        "View details" to "వివరాలు చూడండి",
        "Order ID: " to "ఆర్డర్ ID: ",
        "Email: " to "ఇమెయిల్: ",
        "Order time: " to "ఆర్డర్ సమయం: ",
        "Could not load diseases" to "వ్యాధులను లోడ్ చేయలేకపోయాం",
        "Check that the latest Firestore rules are published." to "తాజా Firestore నియమాలు ప్రచురించబడ్డాయో లేదో తనిఖీ చేయండి.",
        "Search diseases" to "వ్యాధులను వెతకండి",
        "Disease name or description" to "వ్యాధి పేరు లేదా వివరణ",
        "Email not provided" to "ఇమెయిల్ ఇవ్వలేదు",
        "Enabled" to "ప్రారంభించబడింది",
        "Disabled" to "నిలిపివేయబడింది",
        "First name" to "మొదటి పేరు",
        "Last name" to "ఇంటి పేరు",
        "Nickname" to "ముద్దుపేరు",
        "Gender" to "లింగం",
        "Close" to "మూసివేయండి",
        "AYURVEDA NILAYAM" to "ఆయుర్వేద నిలయం",
        "Preparing your wellness experience" to "మీ ఆరోగ్య అనుభవాన్ని సిద్ధం చేస్తున్నాం",
        "Ayurveda Nilayam" to "ఆయుర్వేద నిలయం",
        "Nature's care, rooted in tradition." to "ప్రకృతి సంరక్షణ, సంప్రదాయ మూలాలతో.",
        "Password" to "పాస్‌వర్డ్",
        "TRUSTED SINCE 1990" to "1990 నుంచి విశ్వసనీయత",
        "Authentic Ayurvedic Formulations" to "ప్రామాణిక ఆయుర్వేద ఔషధ తయారీలు",
        "Shop By Category" to "వర్గాల వారీగా కొనండి",
        "Could not load products: \$productsError" to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం: \$productsError",
        "Net quantity" to "నికర పరిమాణం",
        "See product pack" to "ఉత్పత్తి ప్యాక్‌ను చూడండి",
        "Price inclusive of applicable taxes" to "వర్తించే పన్నులతో సహా ధర",
        "This will remove all \${selected.sumOf { cart[it.id] ?: 0 }} items from your cart. You can add them again later." to "ఇది మీ కార్ట్‌లోని \${selected.sumOf { cart[it.id] ?: 0 }} వస్తువులను తొలగిస్తుంది. మీరు వాటిని తర్వాత మళ్లీ జోడించవచ్చు.",
        "\$itemCount items" to "\$itemCount వస్తువులు",
        "Review cart before placing order" to "ఆర్డర్ చేయడానికి ముందు కార్ట్‌ను పరిశీలించండి",
        "{count} items" to "{count} వస్తువులు",
        "Orders are saved as pending verification. Please contact the shop for changes after placing an order." to "ఆర్డర్లు ధృవీకరణ పెండింగ్‌లో సేవ్ అవుతాయి. ఆర్డర్ చేసిన తర్వాత మార్పుల కోసం దుకాణాన్ని సంప్రదించండి.",
        "Product images, bottle designs and packaging shown in the app are for illustration and design reference only. The actual product appearance, bottle shape, label, colour and packaging may vary from the images displayed. The contents and product details will correspond to the product supplied." to "యాప్‌లో చూపించే ఉత్పత్తి చిత్రాలు, సీసా డిజైన్‌లు మరియు ప్యాకేజింగ్ కేవలం నమూనా మరియు డిజైన్ సూచన కోసం మాత్రమే. అసలు ఉత్పత్తి రూపం, సీసా ఆకారం, లేబుల్, రంగు మరియు ప్యాకేజింగ్ చూపిన చిత్రాల కంటే భిన్నంగా ఉండవచ్చు. సరఫరా చేసే ఉత్పత్తికి అనుగుణంగా దాని విషయాలు మరియు వివరాలు ఉంటాయి.",
        "Deliver to:" to "డెలివరీ చిరునామా:",
        "Saved address" to "సేవ్ చేసిన చిరునామా",
        "\${failedOrders.size} order(s)" to "\${failedOrders.size} ఆర్డర్లు",
        "View" to "చూడండి",
        "Your order is saved. The invoice is not available here yet; please check your email or contact the shop." to "మీ ఆర్డర్ సేవ్ అయింది. ఇన్‌వాయిస్ ఇంకా ఇక్కడ అందుబాటులో లేదు; దయచేసి మీ ఇమెయిల్ తనిఖీ చేయండి లేదా దుకాణాన్ని సంప్రదించండి.",
        "Placed today" to "ఈరోజు చేసిన ఆర్డర్",
        "Order options" to "ఆర్డర్ ఎంపికలు",
        "\$star stars" to "\$star నక్షత్రాలు",
        "\$selectedRating out of 5" to "5లో \$selectedRating",
        "This will remove the order from your order history. This action cannot be undone." to "ఇది మీ ఆర్డర్ చరిత్ర నుంచి ఆర్డర్‌ను తొలగిస్తుంది. ఈ చర్యను తిరిగి మార్చలేరు.",
        "\${invoiceItems.size} item(s) in this order" to "ఈ ఆర్డర్‌లో \${invoiceItems.size} వస్తువులు",
        "Item" to "వస్తువు",
        "Copy order ID" to "ఆర్డర్ ID కాపీ చేయండి",
        "Select gender" to "లింగాన్ని ఎంచుకోండి",
        "Loading profile…" to "ప్రొఫైల్ లోడ్ అవుతోంది…",
        "Please wait a moment" to "దయచేసి కొద్దిసేపు వేచి ఉండండి",
        "Connect with SLGAN" to "SLGANతో కనెక్ట్ అవ్వండి",
        "Follow Sri Lakshmi Ganapathi Ayurveda Nilayam on social media for updates and information." to "తాజా సమాచారం మరియు వివరాల కోసం శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయాన్ని సోషల్ మీడియాలో అనుసరించండి.",
        "View live followers on Instagram" to "Instagramలో తాజా ఫాలోవర్ల సంఖ్య చూడండి",
        "Loading your wishlist…" to "మీ విష్‌లిస్ట్ లోడ్ అవుతోంది…",
        "Could not save rating." to "రేటింగ్‌ను సేవ్ చేయలేకపోయాం.",
        "Please sign in to rate this product." to "ఈ ఉత్పత్తికి రేటింగ్ ఇవ్వడానికి సైన్ ఇన్ చేయండి.",
        "Rating saved" to "రేటింగ్ సేవ్ అయింది",
        "Out of Stock" to "స్టాక్‌లో లేదు",
        "Order successful" to "ఆర్డర్ విజయవంతమైంది",
        "Order failed" to "ఆర్డర్ విఫలమైంది",
        "Your order has been saved" to "మీ ఆర్డర్ సేవ్ అయింది",
        "Please review your order" to "దయచేసి మీ ఆర్డర్‌ను పరిశీలించండి",
        "Your order was saved." to "మీ ఆర్డర్ సేవ్ అయింది.",
        "Your order could not be placed." to "మీ ఆర్డర్‌ను నమోదు చేయలేకపోయాం.",
        "ORDER STATUS" to "ఆర్డర్ స్థితి",
        "Pending verification" to "ధృవీకరణ పెండింగ్‌లో ఉంది",
        "Any changes on this screen may be lost if you continue" to "కొనసాగితే ఈ స్క్రీన్‌లో చేసిన మార్పులు కోల్పోవచ్చు",
        "Other" to "ఇతర",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam was established in 1900 by Sri Vidya Raju Allamraju Perraju Garu at Uppada Kothapalli." to "శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయం 1900లో శ్రీ విద్యా రాజు అల్లంరాజు పేర్రాజు గారిచే ఉప్పాడ కొత్తపల్లిలో స్థాపించబడింది.",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN) respects your privacy. We use account information such as your name, contact details, wishlist, and order information to provide app features and support your purchases. We do not ask you to share your password or one-time verification codes with anyone." to "శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయం (SLGAN) మీ గోప్యతను గౌరవిస్తుంది. యాప్ సేవలు మరియు కొనుగోళ్లకు మద్దతు ఇవ్వడానికి మీ పేరు, సంప్రదింపు వివరాలు, విష్‌లిస్ట్ మరియు ఆర్డర్ సమాచారాన్ని ఉపయోగిస్తాము. మీ పాస్‌వర్డ్ లేదా ఒకసారి ఉపయోగించే ధృవీకరణ కోడ్‌లను ఎవరితోనూ పంచుకోవాలని మేము అడగము.",
        "Your information is used to operate account, address, wishlist, and order features. Access to information should be limited to what is needed to provide these services. Please keep your sign-in credentials private and contact SLGAN support if you have questions or believe your account has been accessed without permission. Do not submit sensitive financial information in chat or support messages." to "మీ సమాచారం ఖాతా, చిరునామా, విష్‌లిస్ట్ మరియు ఆర్డర్ సేవలను నిర్వహించడానికి ఉపయోగించబడుతుంది. ఈ సేవలకు అవసరమైన సమాచారానికే ప్రాప్యత పరిమితం చేయాలి. మీ సైన్-ఇన్ వివరాలను రహస్యంగా ఉంచండి. మీకు సందేహాలు ఉన్నా లేదా మీ ఖాతాను అనుమతి లేకుండా ఉపయోగించారని భావించినా SLGAN మద్దతును సంప్రదించండి. చాట్ లేదా మద్దతు సందేశాల్లో సున్నితమైన ఆర్థిక సమాచారాన్ని పంపవద్దు.",
        "Tap the heart on any product to save it here." to "ఏదైనా ఉత్పత్తిపై ఉన్న హార్ట్ గుర్తును నొక్కితే ఇక్కడ సేవ్ అవుతుంది.",
        "\${savedProducts.size} saved products" to "\${savedProducts.size} సేవ్ చేసిన ఉత్పత్తులు",
        "Please sign in to like products." to "ఉత్పత్తులను ఇష్టమైన వాటిలో చేర్చడానికి సైన్ ఇన్ చేయండి.",
        "Could not remove like." to "ఇష్టమైన వాటి నుంచి తొలగించలేకపోయాం.",
        "Could not like product." to "ఉత్పత్తిని ఇష్టమైన వాటిలో చేర్చలేకపోయాం.",
"OR" to "లేదా", "LIVE" to "ప్రత్యక్షం", "OVERVIEW" to "సారాంశం", "MANAGEMENT" to "నిర్వహణ",
        "Disease Library" to "వ్యాధుల జాబితా", "Manage diseases shown in the customer app" to "కస్టమర్ యాప్‌లో చూపే వ్యాధులను నిర్వహించండి",
        "Could not load diseases" to "వ్యాధులను లోడ్ చేయలేకపోయాం", "TOTAL" to "మొత్తం",
        "Instagram" to "ఇన్‌స్టాగ్రామ్", "Facebook" to "ఫేస్‌బుక్", "Estd. 1900" to "స్థాపితం 1900",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam" to "శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయం",
        "Could not load products" to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం", "Latest details could not be refreshed" to "తాజా వివరాలను రిఫ్రెష్ చేయలేకపోయాం",
        "Could not load customers" to "కస్టమర్లను లోడ్ చేయలేకపోయాం", "Could not load orders" to "ఆర్డర్లను లోడ్ చేయలేకపోయాం",
        "products" to "ఉత్పత్తులు", "disease catalogue items" to "వ్యాధుల జాబితా అంశాలు", "Showing" to "చూపిస్తోంది",
        "of" to "లో", "orders • newest first" to "ఆర్డర్లు • కొత్తవి ముందు", "SORT" to "క్రమబద్ధీకరణ",
        "ORDER ITEMS" to "ఆర్డర్ వస్తువులు", "Qty" to "పరిమాణం", "each" to "ప్రతి ఒక్కటి", "more items" to "మరిన్ని వస్తువులు",
        "Order ID" to "ఆర్డర్ ID", "Email" to "ఇమెయిల్", "Order time" to "ఆర్డర్ సమయం",
        "Information not available" to "సమాచారం అందుబాటులో లేదు",
        "Check that the latest Firestore rules are published." to "తాజా Firestore నియమాలు ప్రచురించబడ్డాయో తనిఖీ చేయండి.",
        "Tap to rate" to "రేటింగ్ ఇవ్వడానికి నొక్కండి",
        "Your review" to "మీ సమీక్ష",
        "Review" to "సమీక్ష",
        "Receiver" to "స్వీకర్త",
        "Mobile" to "మొబైల్",
        "Complete address" to "పూర్తి చిరునామా",
        "Review products before placing order" to "ఆర్డర్ చేసే ముందు ఉత్పత్తులను సమీక్షించండి",
        "Arrived at" to "చేరుకున్న సమయం",
        "Order status" to "ఆర్డర్ స్థితి",
        "item(s) in this order" to "ఈ ఆర్డర్‌లోని వస్తువులు",
        "Collapse" to "మూసివేయండి",
        "Expand" to "విస్తరించండి",
        "Customer ID" to "కస్టమర్ ID",
        "saved products" to "సేవ్ చేసిన ఉత్పత్తులు",
        "Could not load products" to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం",
        "items" to "వస్తువులు",
        "Tap to rate" to "రేటింగ్ ఇవ్వడానికి నొక్కండి",
        "Your review" to "మీ సమీక్ష",
    )
    val hi = mapOf(
        "Home" to "होम", "Categories" to "श्रेणियाँ", "Cart" to "कार्ट", "Profile" to "प्रोफ़ाइल",
        "Your account" to "आपका खाता", "Manage your personal details and delivery addresses." to "अपनी व्यक्तिगत जानकारी और डिलीवरी पते प्रबंधित करें।",
        "First name" to "पहला नाम", "Last name" to "उपनाम", "Nickname" to "उपनाम", "Mobile number" to "मोबाइल नंबर",
        "Enter mobile number" to "मोबाइल नंबर दर्ज करें", "Gender" to "लिंग", "Select gender" to "लिंग चुनें",
        "Female" to "महिला", "Male" to "पुरुष", "Non-binary" to "नॉन-बाइनरी", "Prefer not to say" to "बताना नहीं चाहते",
        "Save profile" to "प्रोफ़ाइल सहेजें", "Saved successfully." to "सफलतापूर्वक सहेजा गया।",
        "Saving…" to "सहेज रहे हैं…", "Loading profile…" to "प्रोफ़ाइल लोड हो रही है…", "Sign out" to "साइन आउट",
        "Enter delivery details below." to "नीचे डिलीवरी विवरण दर्ज करें।",
        "Home" to "घर", "Work" to "काम", "Other" to "अन्य",
        "Recipient name *" to "प्राप्तकर्ता का नाम *", "House / flat, building, street *" to "मकान / फ्लैट, भवन, सड़क *",
        "Area / village / locality" to "क्षेत्र / गाँव / इलाका", "Landmark (optional)" to "नज़दीकी पहचान (वैकल्पिक)",
        "City / town *" to "शहर / कस्बा *", "State *" to "राज्य *", "PIN code *" to "पिन कोड *",
                "No addresses yet" to "अभी कोई पता नहीं", "All" to "सभी", "Lehyas" to "लेह्य", "Ghrutas" to "घृत",
        "Pills" to "गोलियाँ", "Tailas" to "तेल", "Churnas" to "चूर्ण", "Rasayanas" to "रसायन", "Rasayogas" to "रसयोग", "Rasayoga" to "रसयोग", "Bhasmas" to "भस्म",
        "Voice search" to "वॉइस सर्च",
        "Voice search is not available on this device." to "इस डिवाइस पर वॉइस सर्च उपलब्ध नहीं है।",
        "Sign in with your registered email and password." to "अपने पंजीकृत ईमेल और पासवर्ड से साइन इन करें।",
        "Mobile-number login is not available yet." to "मोबाइल नंबर से लॉगिन अभी उपलब्ध नहीं है।",
        "View product" to "उत्पाद देखें",
        "Price not available yet." to "कीमत अभी उपलब्ध नहीं है।",
        "Product details" to "उत्पाद विवरण",
        "Description" to "विवरण",
        "Usage" to "उपयोग विधि",
        "Cures" to "उपयोग संबंधी जानकारी",
        "Major ingredients" to "मुख्य सामग्री", "About" to "विवरण", "Net quantity" to "शुद्ध मात्रा", "Price inclusive of applicable taxes" to "लागू कर सहित कीमत",
        "No online payment. Place your order and we will confirm delivery details." to "ऑनलाइन भुगतान उपलब्ध नहीं है। ऑर्डर दें; हम डिलीवरी विवरण की पुष्टि करेंगे।",
        "Place order" to "ऑर्डर करें",
        "Customer name *" to "ग्राहक का नाम *",
        "Email for invoice *" to "इनवॉइस के लिए ईमेल *",
        "Phone Number" to "फ़ोन नंबर",
                "Payment: none. This order request does not collect payment." to "कोई भुगतान नहीं। इस ऑर्डर अनुरोध में पैसे नहीं लिए जाते।",
        "Cancel" to "रद्द करें",
        "My Orders" to "मेरे ऑर्डर",
        "You have no previous orders yet." to "आपके अभी तक कोई पुराने ऑर्डर नहीं हैं।",
        "View / Download Invoice" to "इनवॉइस देखें / डाउनलोड करें",
        "Previous Orders" to "पिछले ऑर्डर",
        "Order History" to "ऑर्डर इतिहास",
        "View complete invoice" to "पूरा इनवॉइस देखें",
        "Open invoice PDF" to "इनवॉइस PDF खोलें",
        "ORDER INVOICE" to "ऑर्डर इनवॉइस",
        "Invoice No: ${'\$' }invoiceNo" to "इनवॉइस नंबर: ${'\$' }invoiceNo",
        "Order ID: ${'\$' }orderId" to "ऑर्डर ID: ${'\$' }orderId",
        "Order date: ${'\$' }created" to "ऑर्डर की तारीख: ${'\$' }created",
        "BILL TO" to "बिल प्राप्तकर्ता",
        "ITEM DETAILS" to "वस्तु विवरण",
        "No item details were saved with this order." to "इस ऑर्डर के साथ वस्तु विवरण सेव नहीं हुए।",
        "GRAND TOTAL" to "कुल योग",
        "Payment has not been collected unless the status above says otherwise." to "ऊपर अलग स्थिति न हो तो भुगतान प्राप्त नहीं हुआ है।",
        "Close" to "बंद करें",
        "Open PDF" to "PDF खोलें",
        "Diseases" to "रोग",
        "All Diseases" to "सभी रोग",
        "Find a disease from the available list." to "सूची में से रोग खोजें।",
        "Select disease" to "रोग चुनें",
        "Loading diseases from database…" to "डेटाबेस से रोग लोड हो रहे हैं…",
        "Could not load diseases: ${'\$' }error" to "रोग लोड नहीं हो सके: ${'\$' }error",
        "No matching diseases found." to "कोई मेल खाता रोग नहीं मिला।",
        "DISEASE OVERVIEW" to "रोग का विवरण",
        "What is this disease?" to "यह रोग क्या है?",
        "Medicines available" to "उपलब्ध दवाएँ",
        "Choose a specific disease to see matching medicines." to "संबंधित दवाएँ देखने के लिए कोई विशिष्ट रोग चुनें।",
        "Use medicines only with guidance from a qualified healthcare professional." to "दवाओं का उपयोग केवल योग्य स्वास्थ्य विशेषज्ञ की सलाह से करें।",
        "Information not available." to "जानकारी उपलब्ध नहीं है।",
        "Pack size: ${'\$' }{product.subtitle}" to "पैक का आकार: ${'\$' }{product.subtitle}",
        "Category: ${'\$' }{product.category}" to "श्रेणी: ${'\$' }{product.category}",
        "Quantity / stock: ${'\$' }{product.quantity}" to "मात्रा / स्टॉक: ${'\$' }{product.quantity}",
        "Search a place or address below, then tap a result to fill the address fields." to "नीचे स्थान या पता खोजें और जानकारी भरने के लिए परिणाम चुनें।",
        "All" to "सभी", "Lehyas" to "लेह्य", "Ghrutas" to "घृत", "Pills" to "गोलियाँ", "Tailas" to "तेल", "Churnas" to "चूर्ण", "Rasayanas" to "रसायन", "Bhasmas" to "भस्म",
        "Language" to "भाषा", "Select language" to "भाषा चुनें", "Save language preference" to "भाषा प्राथमिकता सहेजें",
        "Email address" to "ईमेल पता", "Password (at least 6 characters)" to "पासवर्ड (कम से कम 6 अक्षर)", "Secure email and password sign-in" to "ईमेल और पासवर्ड से सुरक्षित साइन-इन",
        "Rooted in Tradition. Inspired by Nature." to "परंपरा में निहित, प्रकृति से प्रेरित।", "ROOTED IN NATURE" to "प्रकृति से जुड़ा", "Everyday wellness,\\nwith a herbal touch." to "हर दिन स्वास्थ्य,\\nजड़ी-बूटियों के साथ।", "Explore collection  →" to "उत्पाद देखें  →",
        "SRI LAKSHMI GANAPATHI" to "श्री लक्ष्मी गणपति", "AYURVEDA NILAYAM" to "आयुर्वेद निलयम",
        "TRADITION • TRUST • WELLNESS" to "परंपरा • विश्वास • स्वास्थ्य", "Preparing your wellness experience" to "आपके स्वास्थ्य अनुभव की तैयारी हो रही है",
        "Sri Lakshmi Ganapathi" to "श्री लक्ष्मी गणपति", "Ayurveda Nilayam" to "आयुर्वेद निलयम",
        "Estd. 1900 by Vidyaraju Sri Allamraju Perraju Sharma" to "पेर्राजू शर्मा द्वारा 1900 में स्थापित", "Nature's care, rooted in tradition." to "प्रकृति की देखभाल,\nपरंपरा की जड़ों से।", "Sri Lakshmi Ganapathi Ayurveda Nilayam" to "श्री लक्ष्मी गणपति आयुर्वेद निलयम",
        "OUR HERITAGE" to "हमारी विरासत", "DISCOVER SLGAN" to "SLGAN को जानें", "SLGAN SIGNATURE PRODUCT" to "SLGAN विशेष उत्पाद",
        "Traditional Ayurvedic care, rooted in nature" to "प्रकृति से जुड़ी पारंपरिक आयुर्वेदिक देखभाल", "A signature SLGAN product" to "SLGAN विशेष उत्पाद",
        "Search products" to "उत्पाद खोजें", "Shop By Category" to "श्रेणी के अनुसार खरीदें", "Could not load products: ${'\$' }productsError" to "उत्पाद लोड नहीं हो सके: ${'\$' }productsError",
        "Made with care for your everyday rituals." to "आपकी रोज़मर्रा की ज़रूरतों के लिए सावधानी से बनाया गया।", "Your cart" to "आपकी कार्ट", "Subtotal" to "उप-योग", "Checkout" to "चेकआउट", "Demo only: checkout and payments are not connected." to "यह केवल डेमो है: चेकआउट और भुगतान जुड़े नहीं हैं।", "Find the collection for your routine." to "अपनी दिनचर्या के लिए उत्पाद खोजें।",
        "Language preference saved." to "भाषा प्राथमिकता सहेजी गई।", "Couldn't save language. Please try again." to "भाषा सहेजी नहीं जा सकी। फिर प्रयास करें।",
        "English" to "English", "Telugu" to "तेलुगु", "Hindi" to "हिन्दी",
        "Clear your cart?" to "अपनी कार्ट खाली करें?",
        "Clear cart" to "कार्ट खाली करें",
        "Checkout" to "चेकआउट",
        "Your Cart Is Empty" to "आपकी कार्ट खाली है",
        "Explore our Ayurvedic products and add your favourites." to "हमारे आयुर्वेदिक उत्पाद देखें और पसंदीदा उत्पाद जोड़ें।",
        "Your Ayurvedic Order" to "आपका आयुर्वेदिक ऑर्डर",
        "Save for later" to "बाद के लिए सेव करें",
        "You Might Also Like" to "आपको ये भी पसंद आ सकते हैं",
        "ADD" to "जोड़ें",
        "Bill Details" to "बिल विवरण",
        "Items total" to "आइटम का कुल",
        "Delivery charge" to "डिलीवरी शुल्क",
        "Confirmed at order" to "ऑर्डर के समय पुष्टि होगी",
        "Grand Total" to "कुल राशि",
        "No online payment is collected in this app." to "इस ऐप में ऑनलाइन भुगतान नहीं लिया जाता।",
        "Instructions" to "निर्देश",
        "Add any special instructions for your order" to "अपने ऑर्डर के लिए विशेष निर्देश जोड़ें",
        "Delivery Instructions" to "डिलीवरी निर्देश",
        "Ordering For Someone Else?" to "क्या किसी और के लिए ऑर्डर कर रहे हैं?",
        "Recipient details can be added at checkout." to "चेकआउट पर प्राप्तकर्ता का विवरण जोड़ सकते हैं।",
        "Cancellation Policy" to "रद्द करने की नीति",
        "Product Design" to "उत्पाद डिज़ाइन",
        "Change" to "बदलें",
        "No address added" to "कोई पता नहीं जोड़ा गया",
        "Add" to "जोड़ें",
        "TOTAL" to "कुल",
        "Place order  ›" to "ऑर्डर करें  ›",
        "Choose Delivery Address" to "डिलीवरी पता चुनें",
        "Select the saved address for this order." to "इस ऑर्डर के लिए सेव किया गया पता चुनें।",
        "No saved addresses found. Add a delivery address from your profile before placing an order." to "कोई सेव किया हुआ पता नहीं मिला। ऑर्डर देने से पहले प्रोफ़ाइल में डिलीवरी पता जोड़ें।",
        "Default address" to "डिफ़ॉल्ट पता",
        "Add New Address" to "नया पता जोड़ें",
        "Confirm Address" to "पता पुष्टि करें",
        "Search your orders" to "अपने ऑर्डर खोजें",
        "Your Failed Orders" to "आपके असफल ऑर्डर",
        "ORDER ID" to "ऑर्डर ID",
        "Current Orders" to "वर्तमान ऑर्डर",
        "Loading your orders…" to "आपके ऑर्डर लोड हो रहे हैं…",
        "No orders placed today" to "आज कोई ऑर्डर नहीं दिया गया",
        "Click to order  →" to "ऑर्डर करने के लिए क्लिक करें  →",
        "Reorder" to "दोबारा ऑर्डर करें",
        "Previous Orders" to "पिछले ऑर्डर",
        "Your order history" to "आपका ऑर्डर इतिहास",
        "No previous orders yet. Orders from earlier days will appear here." to "अभी तक कोई पुराने ऑर्डर नहीं हैं। पुराने ऑर्डर यहाँ दिखाई देंगे।",
        "Rate Your Order" to "अपने ऑर्डर को रेट करें",
        "How was your order experience?" to "आपका ऑर्डर अनुभव कैसा रहा?",
        "Submit" to "जमा करें",
        "Order Info" to "ऑर्डर जानकारी",
        "Delete order" to "ऑर्डर हटाएँ",
        "Share order items" to "ऑर्डर के आइटम साझा करें",
        "Delete order?" to "ऑर्डर हटाएँ?",
        "Delete" to "हटाएँ",
        "Order Summary" to "ऑर्डर सारांश",
        "Download Invoice" to "इनवॉइस डाउनलोड करें",
        "MRP / subtotal" to "MRP / उप-योग",
        "Delivery charges" to "डिलीवरी शुल्क",
        "FREE" to "मुफ़्त",
        "Bill total" to "बिल कुल",
        "Order Details" to "ऑर्डर विवरण",
        "Order ID" to "ऑर्डर ID",
        "Payment" to "भुगतान",
        "Phone" to "फ़ोन",
        "Order placed" to "ऑर्डर की तारीख",
        "Need help with your order?" to "अपने ऑर्डर में मदद चाहिए?",
        "About any issues related to your order" to "आपके ऑर्डर से जुड़ी किसी भी समस्या के बारे में",
        "Chat with us" to "हमसे चैट करें",
        "Repeat Order" to "फिर से ऑर्डर करें",
        "VIEW CART ON NEXT STEP" to "अगले चरण में कार्ट देखें",
        "Back to Order History" to "ऑर्डर इतिहास पर वापस जाएँ",
        "Confirm password" to "पासवर्ड की पुष्टि करें",
        "Create account" to "खाता बनाएँ",
        "Already have an account? Sign in" to "पहले से खाता है? साइन इन करें",
        "New? Create account" to "नए हैं? खाता बनाएँ",
        "Forgot Password?" to "पासवर्ड भूल गए?",
        "OR" to "या",
        "Please wait…" to "कृपया प्रतीक्षा करें…",
        "Continue with Google" to "Google से जारी रखें",
        "View Products" to "उत्पाद देखें",
        "Search Results" to "खोज परिणाम",
        "Products could not be loaded. Check your internet and Firestore rules." to "उत्पाद लोड नहीं हो सके। इंटरनेट और Firestore नियम जाँचें।",
        "No products found in this category. Try All or another search." to "इस श्रेणी में कोई उत्पाद नहीं मिला। सभी या दूसरी खोज आज़माएँ।",
        "Signature Products" to "विशेष उत्पाद",
        "Patent Product" to "पेटेंट उत्पाद",
        "Unlike product" to "पसंदीदा से हटाएँ",
        "Like product" to "उत्पाद पसंद करें",
        "Please sign in to rate this product." to "इस उत्पाद को रेट करने के लिए साइन इन करें।",
        "Rating saved" to "रेटिंग सहेजी गई",
        "Could not save rating." to "रेटिंग सहेजी नहीं जा सकी।",
        "No ratings" to "अभी कोई रेटिंग नहीं",
        "Signature product" to "विशेष उत्पाद",
        "Out of Stock" to "स्टॉक में नहीं",
        "Back to products" to "उत्पादों पर वापस जाएँ",
        "Back to product search" to "उत्पाद खोज पर वापस जाएँ",
        "Share product" to "उत्पाद साझा करें",
        "Product Information" to "उत्पाद जानकारी",
        "Save product" to "उत्पाद सहेजें",
        "Information not available." to "जानकारी उपलब्ध नहीं है।",
        "About" to "जानकारी",
        "Customer Ratings & Reviews" to "ग्राहक रेटिंग और समीक्षाएँ",
        "View cart" to "कार्ट देखें",
        "Out of stock" to "स्टॉक में नहीं",
        "Add to Cart" to "कार्ट में जोड़ें",
        "Write A Review" to "समीक्षा लिखें",
        "Close review" to "समीक्षा बंद करें",
        "Your review (optional)" to "आपकी समीक्षा (वैकल्पिक)",
        "Share your experience with this product" to "इस उत्पाद के साथ अपना अनुभव साझा करें",
        "Saved" to "सहेजा गया",
        "Saved! Thank you for your review." to "सहेजा गया! आपकी समीक्षा के लिए धन्यवाद।",
        "Review saved successfully" to "समीक्षा सफलतापूर्वक सहेजी गई",
        "Could not save rating. Please try again." to "रेटिंग सहेजी नहीं जा सकी। फिर कोशिश करें।",
        "Save" to "सहेजें",
        "Finish editing instructions" to "निर्देश संपादन पूरा करें",
        "Edit instructions" to "निर्देश संपादित करें",
        "Complete address not available" to "पूरा पता उपलब्ध नहीं है",
        "Address details unavailable" to "पते का विवरण उपलब्ध नहीं है",
        "Order successful" to "ऑर्डर सफल",
        "Order failed" to "ऑर्डर विफल",
        "Your order has been saved" to "आपका ऑर्डर सहेजा गया है",
        "Please review your order" to "कृपया अपना ऑर्डर जाँचें",
        "Your order was saved." to "आपका ऑर्डर सहेजा गया।",
        "Your order could not be placed." to "आपका ऑर्डर नहीं किया जा सका।",
        "ORDER STATUS" to "ऑर्डर स्थिति",
        "Pending verification" to "सत्यापन लंबित",
        "Received" to "प्राप्त हुआ",
        "Show all disease options" to "सभी रोग विकल्प दिखाएँ",
        "DISEASE DETAILS" to "रोग विवरण",
        "Close disease details and clear search" to "रोग विवरण बंद करें और खोज साफ़ करें",
        "About This Condition" to "इस स्थिति के बारे में",
        "Suggested Medicines" to "सुझाई गई दवाएँ",
        "Precautions & care" to "सावधानियाँ और देखभाल",
        "Search products" to "उत्पाद खोजें",
        "No products in this category" to "इस श्रेणी में कोई उत्पाद नहीं",
        "Decrease quantity" to "मात्रा घटाएँ",
        "Increase quantity" to "मात्रा बढ़ाएँ",
        "Your wishlist" to "आपकी इच्छा-सूची",
        "saved products" to "सहेजे गए उत्पाद",
        "Couldn't load your wishlist" to "आपकी इच्छा-सूची लोड नहीं हो सकी",
        "Please check your connection and try again." to "कृपया इंटरनेट कनेक्शन जाँचें और फिर प्रयास करें।",
        "Your wishlist is empty" to "आपकी इच्छा-सूची खाली है",
        "Tap the heart on any product to save it here." to "किसी भी उत्पाद पर दिल का निशान दबाकर उसे यहाँ सहेजें।",
        "Continue shopping" to "खरीदारी जारी रखें",
        "Choose Your Language" to "अपनी भाषा चुनें",
        "Select your preferred language for SLGAN." to "SLGAN के लिए अपनी पसंदीदा भाषा चुनें।",
        "Selected" to "चयनित",
        "Not selected" to "चयनित नहीं",
        "Continue" to "जारी रखें",
        "Log out from?" to "कहाँ से लॉग आउट करना है?",
        "Choose where you want to end your SLGAN session." to "चुनें कि आप अपना SLGAN सत्र कहाँ समाप्त करना चाहते हैं।",
        "Current Device" to "वर्तमान डिवाइस",
        "Sign out from this phone" to "इस फ़ोन से साइन आउट करें",
        "All Devices" to "सभी डिवाइस",
        "Requires secure server support" to "सुरक्षित सर्वर सहायता आवश्यक है",
        "SLGAN Assistant" to "SLGAN सहायक",
        "How Can We Help?" to "हम आपकी कैसे मदद कर सकते हैं?",
        "Send message" to "संदेश भेजें",
        "Address Options" to "पता विकल्प",
        "Delete address" to "पता हटाएँ",
        "Edit address" to "पता संपादित करें",
        "Set as delivery address" to "डिलीवरी पता सेट करें",
        "Delete address?" to "पता हटाएँ?",
        "Are you sure you want to delete this saved address?" to "क्या आप यह सहेजा गया पता हटाना चाहते हैं?",
        "Address deleted" to "पता हटा दिया गया",
        "Address saved successfully" to "पता सफलतापूर्वक सहेजा गया",
        "Saved Addresses" to "सहेजे गए पते",
        "Keep delivery locations for future orders." to "भविष्य के ऑर्डर के लिए डिलीवरी पते सहेजें।",
        "No saved addresses yet" to "अभी कोई पता सहेजा नहीं गया",
        "Your saved delivery addresses will appear here." to "आपके सहेजे गए डिलीवरी पते यहाँ दिखाई देंगे।",
        "DELIVERY LOCATION" to "डिलीवरी स्थान",
        "Do you want this order at your current location?" to "क्या आप यह ऑर्डर अपने वर्तमान स्थान पर चाहते हैं?",
        "Yes, deliver at my current location" to "हाँ, मेरे वर्तमान स्थान पर डिलीवर करें",
        "No, at some other location" to "नहीं, किसी अन्य स्थान पर",
        "Are you sure?" to "क्या आपको यक़ीन है?",
        "Any changes on this screen may be lost if you continue" to "जारी रखने पर इस स्क्रीन के बदलाव खो सकते हैं",
        "Edit address details" to "पते का विवरण संपादित करें",
        "Add address details" to "पते का विवरण जोड़ें",
        "Address Details" to "पते का विवरण",
        "Select a city" to "शहर चुनें",
        "Select" to "चुनें",
        "Search for a city" to "शहर खोजें",
        "Top cities" to "प्रमुख शहर",
        "Other cities" to "अन्य शहर",
        "Please select a city" to "कृपया शहर चुनें",
        "Select an area, street" to "क्षेत्र या सड़क चुनें",
        "Search for an area, street" to "क्षेत्र या सड़क खोजें",
        "Please select an area, street" to "कृपया क्षेत्र या सड़क चुनें",
        "Enter complete address*" to "पूरा पता दर्ज करें*",
        "Please enter complete address" to "कृपया पूरा पता दर्ज करें",
        "Example: Flat/house no., building, street, landmark" to "उदाहरण: फ्लैट/मकान नंबर, इमारत, सड़क, पहचान-चिह्न",
        "Add Google Maps link (optional)" to "Google Maps लिंक जोड़ें (वैकल्पिक)",
        "Contact Details" to "संपर्क विवरण",
        "Myself" to "स्वयं",
        "Someone else" to "कोई अन्य",
        "Receiver's name*" to "प्राप्तकर्ता का नाम*",
        "Your name*" to "आपका नाम*",
        "Please enter receiver's name" to "कृपया प्राप्तकर्ता का नाम दर्ज करें",
        "Receiver's phone number*" to "प्राप्तकर्ता का फ़ोन नंबर*",
        "Your phone number*" to "आपका फ़ोन नंबर*",
        "Please enter a valid phone number" to "कृपया सही फ़ोन नंबर दर्ज करें",
        "Save address as" to "पता इस रूप में सहेजें",
        "Next" to "अगला",
        "Review Address" to "पते की समीक्षा करें",
        "Finding the nearest area on the map…" to "मानचित्र पर निकटतम क्षेत्र खोज रहे हैं…",
        "Delivery Details" to "डिलीवरी विवरण",
        "Receiver" to "प्राप्तकर्ता",
        "  Receiver:  \$receiverName" to "  प्राप्तकर्ता:  \$receiverName",
        "  Mobile:  \$receiverPhone" to "  मोबाइल:  \$receiverPhone",
        "  Complete address:  \$fullAddress" to "  पूरा पता:  \$fullAddress",
        "Save Address" to "पता सहेजें",
        "Country" to "देश",
        "Select country" to "देश चुनें",
        "Date of birth" to "जन्म तिथि",
        "Select date of birth" to "जन्म तिथि चुनें",
        "Choose date" to "तिथि चुनें",
        "Save profile" to "प्रोफ़ाइल सहेजें",
        "Couldn't save profile. Please try again." to "प्रोफ़ाइल सहेजी नहीं जा सकी। फिर प्रयास करें।",
        "Request To Delete Account" to "खाता हटाने का अनुरोध करें",
        "Request to closure of your account" to "अपना खाता बंद करने का अनुरोध करें",
        "Request account deletion" to "खाता हटाने का अनुरोध",
        "This will open a request email to SLGAN support. Your account will not be deleted automatically. Do you want to continue?" to "इससे SLGAN सहायता के लिए अनुरोध ईमेल खुलेगा। आपका खाता अपने आप नहीं हटेगा। क्या आप जारी रखना चाहते हैं?",
        "No email app is available." to "कोई ईमेल ऐप उपलब्ध नहीं है।",
        "About Us" to "हमारे बारे में",
        "Our Heritage" to "हमारी विरासत",
        "Contact SLGAN" to "SLGAN से संपर्क करें",
        "We are happy to hear from you. Contact us or visit our location." to "हमें आपसे सुनकर खुशी होगी। हमसे संपर्क करें या हमारे स्थान पर आएँ।",
        "Business Address" to "व्यावसायिक पता",
        "Get directions" to "रास्ता देखें",
        "Call SLGAN" to "SLGAN को कॉल करें",
        "Your Information" to "आपकी जानकारी",
        "About And Support" to "हमारे बारे में और सहायता",
        "Other Information" to "अन्य जानकारी",
        "Your wellness, rooted in tradition" to "परंपरा में निहित आपका स्वास्थ्य",
        "Explore our Ayurvedic collection and updates." to "हमारे आयुर्वेदिक उत्पाद और अपडेट देखें।",
        "Help and support" to "सहायता और समर्थन",
        "Personal information" to "व्यक्तिगत जानकारी",
        "My orders" to "मेरे ऑर्डर", "My cart" to "मेरी कार्ट", "Need help?" to "मदद चाहिए?", "Address book" to "पता पुस्तिका", "My Wishlist" to "मेरी विशलिस्ट", "Diseases and remedies" to "रोग और उपचार",
        "Share the app" to "ऐप साझा करें",
        "Please sign in again to save your language." to "अपनी भाषा सहेजने के लिए फिर से साइन इन करें।",
        "Language preference saved." to "भाषा प्राथमिकता सहेजी गई।",
        "Couldn't save language. Please try again." to "भाषा सहेजी नहीं जा सकी। फिर प्रयास करें।",
        "Log out from?" to "कहाँ से लॉग आउट करना है?",
        "Choose where you want to end your SLGAN session." to "चुनें कि आप अपना SLGAN सत्र कहाँ समाप्त करना चाहते हैं।",
        "Sign out from current device" to "वर्तमान डिवाइस से साइन आउट करें",
        "All devices" to "सभी डिवाइस",
        "Could not open this image. Please choose another." to "यह चित्र नहीं खुल सका। कृपया दूसरा चुनें।",
        "Profile picture updated." to "प्रोफ़ाइल चित्र अपडेट किया गया।",
        "Please try again" to "कृपया फिर प्रयास करें",
        "Could not process image. Please choose another." to "चित्र संसाधित नहीं हो सका। कृपया दूसरा चुनें।",
        "Account profile picture" to "खाता प्रोफ़ाइल चित्र",
        "Account profile" to "खाता प्रोफ़ाइल",
        "Edit profile picture" to "प्रोफ़ाइल चित्र संपादित करें",
        "Selected" to "चयनित",
        "Not selected" to "चयनित नहीं",
        "Current Device" to "वर्तमान डिवाइस",
        "All Devices" to "सभी डिवाइस",
        "Requires secure server support" to "सुरक्षित सर्वर सहायता आवश्यक है",
        "SLGAN Assistant" to "SLGAN सहायक",
        "Help & Support · Telugu + English" to "सहायता और समर्थन · तेलुगु + English",
        "Send message" to "संदेश भेजें",
        "Your order was saved." to "आपका ऑर्डर सहेजा गया।",
        "Your order could not be placed." to "आपका ऑर्डर नहीं किया जा सका।",
        "Order successful" to "ऑर्डर सफल",
        "Order failed" to "ऑर्डर विफल",
        "ORDER STATUS" to "ऑर्डर स्थिति",
        "Pending verification" to "सत्यापन लंबित",
        "Date not available" to "तारीख उपलब्ध नहीं",
        "Unable to share order items" to "ऑर्डर की वस्तुएँ साझा नहीं की जा सकीं",
        "Could not identify this order." to "इस ऑर्डर की पहचान नहीं हो सकी।",
        "Order deleted" to "ऑर्डर हटा दिया गया",
        "Could not delete order" to "ऑर्डर हटाया नहीं जा सका",
        "Order ID copied" to "ऑर्डर ID कॉपी किया गया",
        "Show all disease options" to "सभी रोग विकल्प दिखाएँ",
        "Close disease details and clear search" to "रोग विवरण बंद करें और खोज साफ़ करें",
        "Clear search" to "खोज साफ़ करें",
        "ADD" to "जोड़ें",
        "Add another" to "एक और जोड़ें",
        "Date not available" to "तारीख उपलब्ध नहीं",
        "Arrived at \$created" to "\$created को डिलीवर हुआ",
        "Order status: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}" to "ऑर्डर स्थिति: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
        "Name not provided" to "नाम नहीं दिया गया",
        "Patent Product" to "पेटेंट उत्पाद",
        "Signature Products" to "विशेष उत्पाद",
        "Product details are not available in the database yet." to "उत्पाद का विवरण अभी डेटाबेस में उपलब्ध नहीं है।",
        "Add to cart" to "कार्ट में जोड़ें",
        " in cart" to " कार्ट में",
        "Rate product" to "उत्पाद को रेट करें",
        "Order saved. Email service confirmed." to "ऑर्डर सहेजा गया। ईमेल सेवा ने पुष्टि की।",
        "Order saved, but email failed. " to "ऑर्डर सहेजा गया, लेकिन ईमेल विफल रहा। ",
        "SLGAN catalogue products are shown for reference only. There is no single medicine suitable for all diseases. Choose treatment only after a qualified healthcare professional identifies the condition." to "SLGAN कैटलॉग के उत्पाद केवल संदर्भ के लिए दिखाए गए हैं। सभी बीमारियों के लिए एक ही दवा उपयुक्त नहीं होती। बीमारी की पहचान के बाद ही योग्य स्वास्थ्य पेशेवर की सलाह से उपचार चुनें।",
        "Do not self-diagnose or start medicines based only on general information." to "केवल सामान्य जानकारी के आधार पर स्वयं निदान न करें और दवाएँ शुरू न करें।",
        "Consult a qualified healthcare professional for diagnosis and a treatment plan suited to the specific condition." to "निदान और उपयुक्त उपचार योजना के लिए योग्य स्वास्थ्य पेशेवर से सलाह लें।",
        "Use medicines only as prescribed; do not stop prescribed treatment without medical advice." to "दवाएँ केवल निर्धारित तरीके से लें; चिकित्सकीय सलाह के बिना उपचार बंद न करें।",
        "Maintain appropriate hygiene, balanced nutrition, safe physical activity, and recommended vaccinations where applicable." to "उचित स्वच्छता, संतुलित पोषण और सुरक्षित शारीरिक गतिविधि बनाए रखें; जहाँ आवश्यक हो अनुशंसित टीके लगवाएँ।",
        "Seek urgent medical care for severe symptoms such as breathing difficulty, chest pain, confusion, severe dehydration, or sudden worsening." to "साँस लेने में कठिनाई, सीने में दर्द, भ्रम, गंभीर निर्जलीकरण या अचानक हालत बिगड़ने पर तुरंत चिकित्सा सहायता लें।",
        "Information not available" to "जानकारी उपलब्ध नहीं है",
        "Collapse \$title" to "\$title बंद करें",
        "Expand \$title" to "\$title विस्तृत करें",
        "Profile picture updated." to "प्रोफ़ाइल चित्र अपडेट किया गया।",
        "Your Information" to "आपकी जानकारी",
        "About And Support" to "हमारे बारे में और सहायता",
        "Other Information" to "अन्य जानकारी",
        "Privacy Policy" to "गोपनीयता नीति",
        "Terms & Conditions" to "नियम और शर्तें",
        "Social Media" to "सोशल मीडिया",
        "Follow" to "फ़ॉलो करें",
        "Follow us on Facebook" to "Facebook पर हमें फ़ॉलो करें",
        "Open Facebook" to "Facebook खोलें",
        "Account Privacy" to "खाता गोपनीयता",
        "Account Privacy And Policy" to "खाता गोपनीयता और नीति",
        "Read less" to "कम पढ़ें",
        "Read more" to "और पढ़ें",
        "Request To Delete Account" to "खाता हटाने का अनुरोध करें",
        "Request to closure of your account" to "अपना खाता बंद करने का अनुरोध करें",
        "Contact SLGAN" to "SLGAN से संपर्क करें",
        "Business Address" to "व्यावसायिक पता",
        "Get directions" to "रास्ता देखें",
        "Call SLGAN" to "SLGAN को कॉल करें",
        "Saved Addresses" to "सहेजे गए पते",
        "Add address" to "पता जोड़ें",
        "Select address" to "पता चुनें",
        "Selected address" to "चयनित पता",
        "Share address" to "पता साझा करें",
        "Close" to "बंद करें",
        "Back" to "वापस",
        "Delete" to "हटाएँ",
        "Cancel" to "रद्द करें",
        "Yes" to "हाँ",
        "No" to "नहीं",
        "Edit" to "संपादित करें",
        "Phone Number" to "फ़ोन नंबर",
        "Mobile number" to "मोबाइल नंबर",
        "Complete address" to "पूरा पता",
        "Receiver" to "प्राप्तकर्ता",
        "Select date of birth" to "जन्म तिथि चुनें",
        "Save profile" to "प्रोफ़ाइल सहेजें",
        "Products Catalogue" to "उत्पाद सूची",
        "Products" to "उत्पाद", "Orders" to "ऑर्डर", "Diseases" to "रोग", "Customers" to "ग्राहक",
        "Dashboard" to "डैशबोर्ड", "Welcome back" to "वापसी पर स्वागत है", "Administrator" to "प्रशासक",
        "Live Overview" to "लाइव अवलोकन", "Current records across your store" to "आपके स्टोर के वर्तमान रिकॉर्ड",
        "LIVE" to "लाइव", "OVERVIEW" to "अवलोकन", "MANAGEMENT" to "प्रबंधन",
        "Orders Management" to "ऑर्डर प्रबंधन", "Diseases Management" to "रोग प्रबंधन", "Customers Management" to "ग्राहक प्रबंधन",
        "App Themes" to "ऐप थीम", "Sliders & Banners" to "स्लाइडर और बैनर",
        "Add, edit and manage products" to "उत्पाद जोड़ें, संपादित करें और प्रबंधित करें",
        "View and manage customer orders" to "ग्राहक ऑर्डर देखें और प्रबंधित करें",
        "Manage disease records" to "रोग रिकॉर्ड प्रबंधित करें", "View registered customers" to "पंजीकृत ग्राहक देखें",
        "Apply Default or Theme 1 across the app" to "पूरे ऐप में डिफ़ॉल्ट या थीम 1 लागू करें",
        "Manage home page slider images and visibility" to "होम पेज स्लाइडर चित्र और दृश्यता प्रबंधित करें",
        "Choose the appearance for the customer app. The admin screens keep their separate neutral theme." to "ग्राहक ऐप का रूप चुनें। एडमिन स्क्रीन अपनी अलग न्यूट्रल थीम रखती हैं।",
        "Classic Maroon" to "क्लासिक मैरून", "Botanical Green" to "बॉटनिकल ग्रीन", "Dark Mode" to "डार्क मोड", "Green Mode" to "ग्रीन मोड",
        "Add Product" to "उत्पाद जोड़ें", "Edit Product" to "उत्पाद संपादित करें", "Product Name" to "उत्पाद का नाम",
        "Description" to "विवरण", "Usage" to "उपयोग", "Ingredients" to "सामग्री", "Cures" to "लाभ",
        "Available" to "उपलब्ध", "Out of stock" to "स्टॉक में नहीं", "Active" to "सक्रिय", "Hidden" to "छिपा हुआ",
        "Order ID" to "ऑर्डर आईडी", "Order Status" to "ऑर्डर स्थिति", "Pending Verification" to "सत्यापन लंबित",
        "Customer Details" to "ग्राहक विवरण", "Delivery Address" to "डिलीवरी पता", "Update Status" to "स्थिति अपडेट करें",
        "Mark as Delivered" to "डिलीवर के रूप में चिह्नित करें", "No records found" to "कोई रिकॉर्ड नहीं मिला",
        "Refresh" to "रीफ़्रेश", "Upload Image" to "चित्र अपलोड करें", "Remove Image" to "चित्र हटाएँ",
        "Disease Name" to "रोग का नाम", "Add Disease" to "रोग जोड़ें", "Update Disease" to "रोग अपडेट करें",
        "Customer Name" to "ग्राहक का नाम", "Registered Customers" to "पंजीकृत ग्राहक",
        "View Order" to "ऑर्डर देखें", "Subtotal" to "उप-योग", "Delivery" to "डिलीवरी", "Grand Total" to "कुल योग",
        "Invoice" to "चालान", "Open invoice PDF" to "चालान PDF खोलें",
        "Manage, add and organize your products" to "अपने उत्पादों का प्रबंधन, जोड़ें और व्यवस्थित करें",
        "Search products…" to "उत्पाद खोजें…",
        " products" to " उत्पाद",
        "Hide all" to "सभी छिपाएँ",
        "Show all" to "सभी दिखाएँ",
        "Product" to "उत्पाद",
        "Price" to "कीमत",
        "Stock" to "स्टॉक",
        "Show/Hide" to "दिखाएँ/छिपाएँ",
        "Actions" to "कार्रवाई",
        "Could not load products" to "उत्पाद लोड नहीं हो सके",
        "No products found" to "कोई उत्पाद नहीं मिला",
        "Hidden" to "छिपा हुआ",
        "Product in stock" to "उत्पाद स्टॉक में है",
        "Product out of stock" to "उत्पाद स्टॉक में नहीं है",
        "Could not update product stock" to "उत्पाद स्टॉक अपडेट नहीं हो सका",
        "Product shown" to "उत्पाद दिखाया गया",
        "Product hidden" to "उत्पाद छिपाया गया",
        "View details" to "विवरण देखें",
        "Show product" to "उत्पाद दिखाएँ",
        "Hide product" to "उत्पाद छिपाएँ",
        "Delete product" to "उत्पाद हटाएँ",
        "Add New Product" to "नया उत्पाद जोड़ें",
        "Delete product?" to "उत्पाद हटाएँ?",
        "Delete" to "हटाएँ",
        "Cancel" to "रद्द करें",
        "View Product" to "उत्पाद देखें",
        "Complete catalogue information" to "पूरी उत्पाद सूची जानकारी",
        "Edit" to "संपादित करें",
        "IN STOCK" to "स्टॉक में",
        "OUT OF STOCK" to "स्टॉक में नहीं",
        "PRODUCT INFORMATION" to "उत्पाद जानकारी",
        "No additional fields are saved for this product." to "इस उत्पाद के लिए कोई अतिरिक्त विवरण सहेजा नहीं गया है।",
        "Add product" to "उत्पाद जोड़ें",
        "Edit product" to "उत्पाद संपादित करें",
        "Product Information" to "उत्पाद जानकारी",
        "Product name *" to "उत्पाद का नाम *",
        "Product name (Telugu)" to "उत्पाद का नाम (तेलुगु)",
        "Product name (Hindi)" to "उत्पाद का नाम (हिंदी)",
        "Category *" to "श्रेणी *",
        "Add category" to "श्रेणी जोड़ें",
        "Price (₹) *" to "कीमत (₹) *",
        "Weight *" to "वज़न *",
        "Units *" to "इकाइयाँ *",
        "Select weight unit" to "वज़न की इकाई चुनें",
        "No units found in product database" to "उत्पाद डेटाबेस में कोई इकाई नहीं मिली",
        "Qty *" to "मात्रा *",
        "Product availability" to "उत्पाद उपलब्धता",
        "Product image" to "उत्पाद चित्र",
        "Product image preview" to "उत्पाद चित्र पूर्वावलोकन",
        "Upload product image" to "उत्पाद चित्र अपलोड करें",
        "The image is stored online using a filename based on the product name and updates in the app after saving." to "चित्र उत्पाद के नाम पर आधारित फ़ाइल नाम से ऑनलाइन सहेजा जाता है और सहेजने के बाद ऐप में अपडेट होता है।",
        "Image URL (optional)" to "चित्र URL (वैकल्पिक)",
        "Description" to "विवरण",
        "Usage / directions" to "उपयोग / निर्देश",
        "Cures / indications" to "लाभ / संकेत",
        "Major ingredients" to "मुख्य सामग्री",
        "Is signature product" to "विशेष उत्पाद है",
        "Yes" to "हाँ",
        "No" to "नहीं",
        "Enter category" to "श्रेणी दर्ज करें",
        "Category added" to "श्रेणी जोड़ी गई",
        "Could not add category" to "श्रेणी नहीं जोड़ी जा सकी",
        "Saving…" to "सहेजा जा रहा है…",
        "Add" to "जोड़ें",
        "Save product" to "उत्पाद सहेजें",
        "Product saved successfully." to "उत्पाद सफलतापूर्वक सहेजा गया।",
        "Failed to save product" to "उत्पाद सहेजना विफल रहा",
        "Save failed" to "सहेजना विफल रहा",
        "SLGAN ADMIN" to "SLGAN व्यवस्थापक",
        "Dashboard" to "डैशबोर्ड",
        "Welcome back" to "वापस स्वागत है",
        "Administrator" to "व्यवस्थापक",
        "Sign out" to "साइन आउट",
        "Live Overview" to "लाइव अवलोकन",
        "Current records across your store" to "आपके स्टोर के वर्तमान रिकॉर्ड",
        "App Themes" to "ऐप थीम",
        "Choose the appearance for the customer app. The admin screens keep their separate neutral theme." to "ग्राहक ऐप का रूप चुनें। व्यवस्थापक स्क्रीन अलग तटस्थ थीम में रहेंगी।",
        "Apply" to "लागू करें",
        "Theme changes affect colors and backgrounds only. Home page structure, product data, images and navigation remain unchanged." to "थीम परिवर्तन केवल रंग और पृष्ठभूमि बदलते हैं। होम पेज संरचना, उत्पाद डेटा, चित्र और नेविगेशन अपरिवर्तित रहते हैं।",
        "HOME PAGE SLIDERS" to "होम पेज स्लाइडर",
        "Choose Active Design" to "सक्रिय डिज़ाइन चुनें",
        "Show Slider On Home" to "होम पर स्लाइडर दिखाएँ",
        "Slider is visible on the home page" to "स्लाइडर होम पेज पर दिखाई देता है",
        "Slider is hidden from the home page" to "स्लाइडर होम पेज से छिपा है",
        "Choose a slider design and tap Apply. Slider 4 automatically follows the selected app theme." to "स्लाइडर डिज़ाइन चुनें और लागू करें दबाएँ। स्लाइडर 4 चुनी गई ऐप थीम का अनुसरण करता है।",
        "SELECTED" to "चयनित",
        "Applying…" to "लागू किया जा रहा है…",
        "Add disease" to "रोग जोड़ें",
        "Add a disease to the app catalogue." to "ऐप सूची में रोग जोड़ें।",
        "Disease name *" to "रोग का नाम *",
        "e.g. Cough" to "जैसे खाँसी",
        "Enter details (optional)" to "विवरण दर्ज करें (वैकल्पिक)",
        "Save disease" to "रोग सहेजें",
        "Diseases" to "रोग",
        "Customers" to "ग्राहक",
        "Manage customer accounts" to "ग्राहक खातों का प्रबंधन करें",
        "Search name, email or mobile…" to "नाम, ईमेल या मोबाइल खोजें…",
        "Latest" to "नवीनतम",
        "Name A–Z" to "नाम A–Z",
        "Name Z–A" to "नाम Z–A",
        "Enabled" to "सक्रिय",
        "Disabled" to "निष्क्रिय",
        "Account status" to "खाता स्थिति",
        "No customers yet" to "अभी कोई ग्राहक नहीं",
        "No matching customers" to "कोई मेल खाता ग्राहक नहीं",
        "Try changing your search or filters." to "अपनी खोज या फ़िल्टर बदलकर देखें।",
        "Email not provided" to "ईमेल नहीं दिया गया",
        "Customer Details" to "ग्राहक विवरण",
        "Customer ID: \$uid" to "ग्राहक ID: \$uid",
        "Edit Customer" to "ग्राहक संपादित करें",
        "Email" to "ईमेल",
        "Customer updated" to "ग्राहक अपडेट किया गया",
        "Manage Orders" to "ऑर्डर प्रबंधित करें",
        "Orders Library" to "ऑर्डर लाइब्रेरी",
        "Review and manage customer orders" to "ग्राहक ऑर्डर की समीक्षा और प्रबंधन करें",
        "Search order" to "ऑर्डर खोजें",
        "Order ID, customer or mobile" to "ऑर्डर ID, ग्राहक या मोबाइल",
        "Search orders" to "ऑर्डर खोजें",
        "Filter Orders By Status" to "स्थिति के अनुसार ऑर्डर फ़िल्टर करें",
        "Pending" to "लंबित",
        "Pending Verification" to "सत्यापन लंबित",
        "Accepted" to "स्वीकृत",
        "Rejected" to "अस्वीकृत",
        "No orders yet" to "अभी कोई ऑर्डर नहीं",
        "No orders match your search" to "आपकी खोज से कोई ऑर्डर मेल नहीं खाता",
        "Item details are not available for this order." to "इस ऑर्डर के आइटम विवरण उपलब्ध नहीं हैं।",
        "Qty: " to "मात्रा: ",
        " each" to " प्रत्येक",
        " more items" to " और आइटम",
        "Total amount" to "कुल राशि",
        "Hide details" to "विवरण छिपाएँ",
        "Information not available" to "जानकारी उपलब्ध नहीं है",
        "Reject" to "अस्वीकार करें",
        "Approve" to "स्वीकृत करें",
        "Order rejected." to "ऑर्डर अस्वीकार किया गया।",
        "Order approved." to "ऑर्डर स्वीकृत किया गया।",
        "Try again" to "फिर प्रयास करें",
        "Social Media" to "सोशल मीडिया",
        "Connect with SLGAN" to "SLGAN से जुड़ें",
        "Follow Sri Lakshmi Ganapathi Ayurveda Nilayam on social media for updates and information." to "अपडेट और जानकारी के लिए श्री लक्ष्मी गणपति आयुर्वेद निलयम को सोशल मीडिया पर फ़ॉलो करें।",
        "View live followers on Instagram" to "Instagram पर लाइव फ़ॉलोअर्स देखें",
        "Follow" to "फ़ॉलो करें",
        "Follow us on Facebook" to "Facebook पर हमें फ़ॉलो करें",
        "Open Facebook" to "Facebook खोलें",
        "Account Privacy" to "खाता गोपनीयता",
        "Account Privacy And Policy" to "खाता गोपनीयता और नीति",
        "Read less" to "कम पढ़ें",
        "Read more" to "और पढ़ें",
        "About Us" to "हमारे बारे में",
        "Our Heritage" to "हमारी विरासत",
        "Contact SLGAN" to "SLGAN से संपर्क करें",
        "We are happy to hear from you. Contact us or visit our location." to "हमें आपसे सुनकर खुशी होगी। हमसे संपर्क करें या हमारे स्थान पर आएँ।",
        "Business Address" to "व्यावसायिक पता",
        "Get directions" to "रास्ता देखें",
        "Call SLGAN" to "SLGAN को कॉल करें",
        "Your wishlist" to "आपकी इच्छा-सूची",
        "saved products" to "सहेजे गए उत्पाद",
        "Tap the heart on any product to save it here." to "किसी भी उत्पाद पर दिल का निशान दबाकर उसे यहाँ सहेजें।",
        "Continue shopping" to "खरीदारी जारी रखें",
        "Patent Product" to "पेटेंट उत्पाद",
        "Signature product" to "विशेष उत्पाद",
        "Add another" to "एक और जोड़ें",
        "Order successful" to "ऑर्डर सफल",
        "Order failed" to "ऑर्डर विफल",
        "Your order has been saved" to "आपका ऑर्डर सहेजा गया है",
        "Please review your order" to "कृपया अपना ऑर्डर जाँचें",
        "Your order was saved." to "आपका ऑर्डर सहेजा गया।",
        "Your order could not be placed." to "आपका ऑर्डर नहीं किया जा सका।",
        "ORDER STATUS" to "ऑर्डर स्थिति",
        "Pending verification" to "सत्यापन लंबित",
        "Arrived at \$created" to "\$created को डिलीवर हुआ",
        "Order status: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}" to "ऑर्डर स्थिति: \${status.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
        "Date not available" to "तारीख उपलब्ध नहीं",
        "Unable to share order items" to "ऑर्डर की वस्तुएँ साझा नहीं की जा सकीं",
        "Could not identify this order." to "इस ऑर्डर की पहचान नहीं हो सकी।",
        "Order deleted" to "ऑर्डर हटा दिया गया",
        "Could not delete order" to "ऑर्डर हटाया नहीं जा सका",
        "Order ID copied" to "ऑर्डर ID कॉपी किया गया",
        "Show all disease options" to "सभी रोग विकल्प दिखाएँ",
        "Close disease details and clear search" to "रोग विवरण बंद करें और खोज साफ़ करें",
        "Collapse \$title" to "\$title बंद करें",
        "Expand \$title" to "\$title विस्तृत करें",
        "Clear search" to "खोज साफ़ करें",
        "Decrease quantity" to "मात्रा घटाएँ",
        "Increase quantity" to "मात्रा बढ़ाएँ",
        "Add to Cart" to "कार्ट में जोड़ें",
        "Out of stock" to "स्टॉक में नहीं",
        "Information not available." to "जानकारी उपलब्ध नहीं है।",
        "Customer Ratings & Reviews" to "ग्राहक रेटिंग और समीक्षाएँ",
        "Write A Review" to "समीक्षा लिखें",
        "Close review" to "समीक्षा बंद करें",
        "Your review (optional)" to "आपकी समीक्षा (वैकल्पिक)",
        "Share your experience with this product" to "इस उत्पाद के साथ अपना अनुभव साझा करें",
        "Saved! Thank you for your review." to "सहेजा गया! आपकी समीक्षा के लिए धन्यवाद।",
        "Review saved successfully" to "समीक्षा सफलतापूर्वक सहेजी गई",
        "Could not save rating. Please try again." to "रेटिंग सहेजी नहीं जा सकी। फिर कोशिश करें।",
        "Help & Support · Telugu + English" to "सहायता और समर्थन · तेलुगु + English",
        "Quick answers for products, orders and delivery. AI replies become available when the secure support service is configured." to "उत्पादों, ऑर्डर और डिलीवरी के लिए त्वरित उत्तर। सुरक्षित सहायता सेवा कॉन्फ़िगर होने पर AI उत्तर उपलब्ध होंगे।",
        "Thinking…" to "उत्तर तैयार किया जा रहा है…",
        "Type in Telugu or English…" to "तेलुगु या English में लिखें…",
        "AI can make mistakes. For medical advice or dosage, consult a qualified clinician." to "AI से गलतियाँ हो सकती हैं। चिकित्सा सलाह या खुराक के लिए योग्य चिकित्सक से परामर्श करें।",
        "SLGAN support assistant" to "SLGAN सहायता सहायक",
        "Send message" to "संदेश भेजें",
        "Account profile picture" to "खाता प्रोफ़ाइल चित्र",
        "Account profile" to "खाता प्रोफ़ाइल",
        "Edit profile picture" to "प्रोफ़ाइल चित्र संपादित करें",
        "Could not load saved addresses" to "सहेजे गए पते लोड नहीं हो सके",
        "Default delivery address updated" to "डिफ़ॉल्ट डिलीवरी पता अपडेट किया गया",
        "Could not update default address" to "डिफ़ॉल्ट पता अपडेट नहीं हो सका",
        "Add address" to "पता जोड़ें",
        "Selected address" to "चयनित पता",
        "Select address" to "पता चुनें",
        "Address Options" to "पता विकल्प",
        "Share address" to "पता साझा करें",
        "Couldn't detect your location. Please enter the address manually." to "आपका स्थान नहीं मिल सका। कृपया पता स्वयं दर्ज करें।",
        "Address not found. Try entering a nearby area and city to place the pin." to "पता नहीं मिला। पिन लगाने के लिए पास का क्षेत्र और शहर दर्ज करें।",
        "Please wait for the map or enter a nearby area and city to select your delivery pin." to "मानचित्र की प्रतीक्षा करें या डिलीवरी पिन चुनने के लिए पास का क्षेत्र और शहर दर्ज करें।",
        "Please sign in to save an address." to "पता सहेजने के लिए साइन इन करें।",
        "Could not save address. Please try again." to "पता सहेजा नहीं जा सका। फिर प्रयास करें।",
        "Could not verify saved addresses. Please try again." to "सहेजे गए पतों की पुष्टि नहीं हो सकी। फिर प्रयास करें।",
        "Address deleted" to "पता हटा दिया गया",
        "Address saved successfully" to "पता सफलतापूर्वक सहेजा गया",
        "Profile picture updated." to "प्रोफ़ाइल चित्र अपडेट किया गया।",
        "Could not process image. Please choose another." to "चित्र संसाधित नहीं हो सका। कृपया दूसरा चुनें।",
        "Please sign in again to save your language." to "अपनी भाषा सहेजने के लिए फिर से साइन इन करें।",
        "Language preference saved." to "भाषा प्राथमिकता सहेजी गई।",
        "Couldn't save language. Please try again." to "भाषा सहेजी नहीं जा सकी। फिर प्रयास करें।",
        "This app can sign out this device only. Signing out all devices requires secure server support." to "यह ऐप केवल इस डिवाइस से साइन आउट कर सकता है। सभी डिवाइस से साइन आउट करने के लिए सुरक्षित सर्वर सहायता आवश्यक है।",
        "Do you want this order at your current location?" to "क्या आप यह ऑर्डर अपने वर्तमान स्थान पर चाहते हैं?",
        "Selected" to "चयनित",
        "Not selected" to "चयनित नहीं",
        "Sign out from current device" to "वर्तमान डिवाइस से साइन आउट करें",
        "All devices" to "सभी डिवाइस",
        "AI can make mistakes. For medical advice or dosage, consult a qualified clinician." to "AI से गलतियाँ हो सकती हैं। चिकित्सा सलाह या खुराक के लिए योग्य चिकित्सक से परामर्श करें।",
        "Collapse \$title" to "\$title बंद करें",
        "Expand \$title" to "\$title विस्तृत करें",
        "Address details unavailable" to "पते का विवरण उपलब्ध नहीं है",
        "Name not provided" to "नाम नहीं दिया गया",
        "View Products" to "उत्पाद देखें",
        "Name not provided" to "नाम नहीं दिया गया",
        "Complete address not available" to "पूरा पता उपलब्ध नहीं है",
        "Delivery address" to "डिलीवरी पता",
        "Other cities" to "अन्य शहर",
        "Search Results" to "खोज परिणाम",
        "Edit address details" to "पते का विवरण संपादित करें",
        "Add address details" to "पते का विवरण जोड़ें",
        "Receiver's name*" to "प्राप्तकर्ता का नाम*",
        "Your name*" to "आपका नाम*",
        "Receiver's phone number*" to "प्राप्तकर्ता का फ़ोन नंबर*",
        "Your phone number*" to "आपका फ़ोन नंबर*",
        "Any changes on this screen may be lost if you continue" to "जारी रखने पर इस स्क्रीन के बदलाव खो सकते हैं",
        "Yes" to "हाँ",
        "No" to "नहीं",
        " items" to " आइटम",
        " products" to " उत्पाद",
        "orders" to "ऑर्डर",
        "Orders" to "ऑर्डर",
        "All" to "सभी",
        "Pending" to "लंबित",
        "Pending Verification" to "सत्यापन लंबित",
        "Accepted" to "स्वीकृत",
        "Rejected" to "अस्वीकृत",
        "Customer" to "ग्राहक",
        "No phone" to "फ़ोन नंबर नहीं",
        "ORDER ITEMS (" to "ऑर्डर आइटम (",
        "Qty: " to "मात्रा: ",
        " each" to " प्रत्येक",
        " more items" to " और आइटम",
        "Total amount" to "कुल राशि",
        "Hide details" to "विवरण छिपाएँ",
        "View details" to "विवरण देखें",
        "Order ID: " to "ऑर्डर ID: ",
        "Email: " to "ईमेल: ",
        "Order time: " to "ऑर्डर समय: ",
        "Could not load diseases" to "रोग लोड नहीं हो सके",
        "Check that the latest Firestore rules are published." to "जाँचें कि नवीनतम Firestore नियम प्रकाशित हैं।",
        "Search diseases" to "रोग खोजें",
        "Disease name or description" to "रोग का नाम या विवरण",
        "Email not provided" to "ईमेल नहीं दिया गया",
        "Enabled" to "सक्रिय",
        "Disabled" to "निष्क्रिय",
        "First name" to "पहला नाम",
        "Last name" to "उपनाम",
        "Nickname" to "उपनाम",
        "Gender" to "लिंग",
        "Close" to "बंद करें",
        "AYURVEDA NILAYAM" to "आयुर्वेद निलयम",
        "Preparing your wellness experience" to "आपके स्वास्थ्य अनुभव की तैयारी हो रही है",
        "Ayurveda Nilayam" to "आयुर्वेद निलयम",
        "Nature's care, rooted in tradition." to "प्रकृति की देखभाल, परंपरा की जड़ों से।",
        "Password" to "पासवर्ड",
        "TRUSTED SINCE 1990" to "1990 से विश्वसनीय",
        "Authentic Ayurvedic Formulations" to "प्रामाणिक आयुर्वेदिक औषधीय निर्माण",
        "Shop By Category" to "श्रेणी के अनुसार खरीदें",
        "Could not load products: \$productsError" to "उत्पाद लोड नहीं हो सके: \$productsError",
        "Net quantity" to "शुद्ध मात्रा",
        "See product pack" to "उत्पाद पैक देखें",
        "Price inclusive of applicable taxes" to "लागू कर सहित कीमत",
        "This will remove all \${selected.sumOf { cart[it.id] ?: 0 }} items from your cart. You can add them again later." to "इससे आपके कार्ट के सभी \${selected.sumOf { cart[it.id] ?: 0 }} आइटम हट जाएँगे। आप उन्हें बाद में फिर जोड़ सकते हैं।",
        "\$itemCount items" to "\$itemCount आइटम",
        "Review cart before placing order" to "ऑर्डर देने से पहले कार्ट की समीक्षा करें",
        "{count} items" to "{count} आइटम",
        "Orders are saved as pending verification. Please contact the shop for changes after placing an order." to "ऑर्डर सत्यापन लंबित स्थिति में सहेजे जाते हैं। ऑर्डर देने के बाद बदलाव के लिए दुकान से संपर्क करें।",
        "Product images, bottle designs and packaging shown in the app are for illustration and design reference only. The actual product appearance, bottle shape, label, colour and packaging may vary from the images displayed. The contents and product details will correspond to the product supplied." to "ऐप में दिखाए गए उत्पाद चित्र, बोतल डिज़ाइन और पैकेजिंग केवल उदाहरण और डिज़ाइन संदर्भ के लिए हैं। वास्तविक उत्पाद का रूप, बोतल का आकार, लेबल, रंग और पैकेजिंग प्रदर्शित चित्रों से भिन्न हो सकते हैं। सामग्री और उत्पाद विवरण आपूर्ति किए गए उत्पाद के अनुरूप होंगे।",
        "Deliver to:" to "डिलीवरी पता:",
        "Saved address" to "सहेजा गया पता",
        "\${failedOrders.size} order(s)" to "\${failedOrders.size} ऑर्डर",
        "View" to "देखें",
        "Your order is saved. The invoice is not available here yet; please check your email or contact the shop." to "आपका ऑर्डर सहेजा गया है। इनवॉइस अभी यहाँ उपलब्ध नहीं है; कृपया अपना ईमेल देखें या दुकान से संपर्क करें।",
        "Placed today" to "आज दिया गया",
        "Order options" to "ऑर्डर विकल्प",
        "\$star stars" to "\$star सितारे",
        "\$selectedRating out of 5" to "5 में से \$selectedRating",
        "This will remove the order from your order history. This action cannot be undone." to "इससे ऑर्डर आपके इतिहास से हट जाएगा। यह कार्रवाई वापस नहीं की जा सकती।",
        "\${invoiceItems.size} item(s) in this order" to "इस ऑर्डर में \${invoiceItems.size} आइटम",
        "Item" to "आइटम",
        "Copy order ID" to "ऑर्डर ID कॉपी करें",
        "Select gender" to "लिंग चुनें",
        "Loading profile…" to "प्रोफ़ाइल लोड हो रही है…",
        "Please wait a moment" to "कृपया एक क्षण प्रतीक्षा करें",
        "Connect with SLGAN" to "SLGAN से जुड़ें",
        "Follow Sri Lakshmi Ganapathi Ayurveda Nilayam on social media for updates and information." to "अपडेट और जानकारी के लिए श्री लक्ष्मी गणपति आयुर्वेद निलयम को सोशल मीडिया पर फ़ॉलो करें।",
        "View live followers on Instagram" to "Instagram पर लाइव फ़ॉलोअर्स देखें",
        "Loading your wishlist…" to "आपकी इच्छा-सूची लोड हो रही है…",
        "Could not save rating." to "रेटिंग सहेजी नहीं जा सकी।",
        "Please sign in to rate this product." to "इस उत्पाद को रेट करने के लिए साइन इन करें।",
        "Rating saved" to "रेटिंग सहेजी गई",
        "Out of Stock" to "स्टॉक में नहीं",
        "Order successful" to "ऑर्डर सफल",
        "Order failed" to "ऑर्डर विफल",
        "Your order has been saved" to "आपका ऑर्डर सहेजा गया है",
        "Please review your order" to "कृपया अपना ऑर्डर जाँचें",
        "Your order was saved." to "आपका ऑर्डर सहेजा गया।",
        "Your order could not be placed." to "आपका ऑर्डर नहीं किया जा सका।",
        "ORDER STATUS" to "ऑर्डर स्थिति",
        "Pending verification" to "सत्यापन लंबित",
        "Any changes on this screen may be lost if you continue" to "जारी रखने पर इस स्क्रीन के बदलाव खो सकते हैं",
        "Other" to "अन्य",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam was established in 1900 by Sri Vidya Raju Allamraju Perraju Garu at Uppada Kothapalli." to "श्री लक्ष्मी गणपति आयुर्वेद निलयम की स्थापना 1900 में श्री विद्या राजू अल्लमराजू पेर्राजू गरु ने उप्पाडा कोठापल्ली में की थी।",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN) respects your privacy. We use account information such as your name, contact details, wishlist, and order information to provide app features and support your purchases. We do not ask you to share your password or one-time verification codes with anyone." to "श्री लक्ष्मी गणपति आयुर्वेद निलयम (SLGAN) आपकी गोपनीयता का सम्मान करता है। ऐप सुविधाएँ देने और आपकी खरीदारी में सहायता के लिए हम आपका नाम, संपर्क विवरण, इच्छा-सूची और ऑर्डर जानकारी उपयोग करते हैं। हम आपसे पासवर्ड या एक बार उपयोग होने वाले सत्यापन कोड किसी के साथ साझा करने के लिए नहीं कहते।",
        "Your information is used to operate account, address, wishlist, and order features. Access to information should be limited to what is needed to provide these services. Please keep your sign-in credentials private and contact SLGAN support if you have questions or believe your account has been accessed without permission. Do not submit sensitive financial information in chat or support messages." to "आपकी जानकारी का उपयोग खाता, पता, इच्छा-सूची और ऑर्डर सुविधाएँ चलाने के लिए किया जाता है। जानकारी तक पहुँच इन सेवाओं के लिए आवश्यक सीमा तक ही होनी चाहिए। अपने साइन-इन विवरण निजी रखें और कोई प्रश्न हो या खाते में अनधिकृत पहुँच का संदेह हो तो SLGAN सहायता से संपर्क करें। चैट या सहायता संदेशों में संवेदनशील वित्तीय जानकारी न भेजें।",
        "Tap the heart on any product to save it here." to "किसी भी उत्पाद पर दिल का निशान दबाकर उसे यहाँ सहेजें।",
        "\${savedProducts.size} saved products" to "\${savedProducts.size} सहेजे गए उत्पाद",
        "Please sign in to like products." to "उत्पाद पसंद करने के लिए साइन इन करें।",
        "Could not remove like." to "पसंद से हटाया नहीं जा सका।",
        "Could not like product." to "उत्पाद पसंद नहीं किया जा सका।",
"OR" to "या", "LIVE" to "लाइव", "OVERVIEW" to "अवलोकन", "MANAGEMENT" to "प्रबंधन",
        "Disease Library" to "रोग सूची", "Manage diseases shown in the customer app" to "ग्राहक ऐप में दिखाए जाने वाले रोगों का प्रबंधन करें",
        "Could not load diseases" to "रोग लोड नहीं हो सके", "TOTAL" to "कुल",
        "Instagram" to "इंस्टाग्राम", "Facebook" to "फेसबुक", "Estd. 1900" to "स्थापित 1900",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam" to "श्री लक्ष्मी गणपति आयुर्वेद निलयम",
        "Could not load products" to "उत्पाद लोड नहीं हो सके", "Latest details could not be refreshed" to "नवीनतम विवरण रीफ़्रेश नहीं हो सके",
        "Could not load customers" to "ग्राहक लोड नहीं हो सके", "Could not load orders" to "ऑर्डर लोड नहीं हो सके",
        "products" to "उत्पाद", "disease catalogue items" to "रोग सूची आइटम", "Showing" to "दिखा रहे हैं",
        "of" to "में से", "orders • newest first" to "ऑर्डर • नवीनतम पहले", "SORT" to "क्रमबद्ध करें",
        "ORDER ITEMS" to "ऑर्डर आइटम", "Qty" to "मात्रा", "each" to "प्रति", "more items" to "और आइटम",
        "Order ID" to "ऑर्डर ID", "Email" to "ईमेल", "Order time" to "ऑर्डर समय",
        "Information not available" to "जानकारी उपलब्ध नहीं है",
        "Check that the latest Firestore rules are published." to "जाँचें कि नवीनतम Firestore नियम प्रकाशित हैं।",
        "Tap to rate" to "रेटिंग देने के लिए टैप करें",
        "Your review" to "आपकी समीक्षा",
        "Review" to "समीक्षा",
        "Receiver" to "प्राप्तकर्ता",
        "Mobile" to "मोबाइल",
        "Complete address" to "पूरा पता",
        "Review products before placing order" to "ऑर्डर देने से पहले उत्पादों की समीक्षा करें",
        "Arrived at" to "पहुँचने का समय",
        "Order status" to "ऑर्डर की स्थिति",
        "item(s) in this order" to "इस ऑर्डर में आइटम",
        "Collapse" to "समेटें",
        "Expand" to "विस्तार करें",
        "Customer ID" to "ग्राहक ID",
        "saved products" to "सहेजे गए उत्पाद",
        "Could not load products" to "उत्पाद लोड नहीं हो सके",
        "items" to "आइटम",
    )
    return when (language) { "Telugu" -> te[text] ?: text; "Hindi" -> hi[text] ?: text; else -> text }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 4102)
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) { AyurvedicApp() } }
    }
}


private fun transliterateLatinToTelugu(value: String): String {
    val consonants = listOf(
        "ksh" to "క్ష", "chh" to "ఛ", "Sh" to "ష", "sh" to "శ",
        "kh" to "ఖ", "gh" to "ఘ", "ch" to "చ", "jh" to "ఝ",
        "th" to "థ", "dh" to "ధ", "ph" to "ఫ", "bh" to "భ",
        "ng" to "ఙ", "ny" to "ఞ", "jn" to "జ్ఞ",
        "k" to "క", "g" to "గ", "c" to "క", "j" to "జ",
        "T" to "ట", "D" to "డ", "N" to "ణ", "t" to "త", "d" to "ద",
        "n" to "న", "p" to "ప", "b" to "బ", "m" to "మ",
        "y" to "య", "r" to "ర", "l" to "ల", "v" to "వ",
        "w" to "వ", "s" to "స", "h" to "హ", "f" to "ఫ",
        "z" to "జ", "q" to "క", "x" to "క్స"
    ).sortedByDescending { it.first.length }
    val vowels = listOf(
        "aa" to "ా", "A" to "ా", "ee" to "ీ", "ii" to "ీ",
        "oo" to "ూ", "uu" to "ూ", "ai" to "ై", "au" to "ౌ",
        "R" to "ృ", "ri" to "ృ", "e" to "ె", "i" to "ి",
        "o" to "ొ", "u" to "ు", "a" to ""
    ).sortedByDescending { it.first.length }
    val independentVowels = mapOf(
        "aa" to "ఆ", "A" to "ఆ", "ee" to "ఈ", "ii" to "ఈ",
        "oo" to "ఊ", "uu" to "ఊ", "ai" to "ఐ", "au" to "ఔ",
        "R" to "ఋ", "ri" to "ఋ", "a" to "అ", "i" to "ఇ",
        "u" to "ఉ", "e" to "ఎ", "o" to "ఒ"
    )
    val input = value.trim()
    if (input.none { it in 'A'..'Z' || it in 'a'..'z' }) return input
    val out = StringBuilder()
    var index = 0
    while (index < input.length) {
        val current = input[index]
        if (!current.isLetter()) {
            out.append(current)
            index++
            continue
        }
        val consonant = consonants.firstOrNull { (token, _) ->
            input.startsWith(token, index, ignoreCase = false) ||
                (token.all { it.isLowerCase() } && input.startsWith(token, index, ignoreCase = true))
        }
        if (consonant != null) {
            out.append(consonant.second)
            index += consonant.first.length
            val vowel = vowels.firstOrNull { (token, _) ->
                input.startsWith(token, index, ignoreCase = false) ||
                    (token.all { it.isLowerCase() } && input.startsWith(token, index, ignoreCase = true))
            }
            if (vowel != null) {
                out.append(vowel.second)
                index += vowel.first.length
            } else {
                out.append('్')
            }
            continue
        }
        val vowel = vowels.firstOrNull { (token, _) ->
            input.startsWith(token, index, ignoreCase = false) ||
                (token.all { it.isLowerCase() } && input.startsWith(token, index, ignoreCase = true))
        }
        if (vowel != null) {
            out.append(independentVowels[vowel.first] ?: vowel.second)
            index += vowel.first.length
        } else {
            out.append(current)
            index++
        }
    }
    return out.toString()
}

private fun localizedProductName(name: String, language: String): String {
    if (language == "English") return name
    val telugu = mapOf(
        "Mahakanakasindhura Rasam" to "మహాకనకసింధూర రసం",
        "Kaamsya (Kanchu)" to "కాంస్య (కంచు)",
        "Kaamsya Kanchu" to "కాంస్య కంచు",
        "Kravyadi Rasam" to "క్రవ్యాది రసం",
        "Siddha Makaradhwajam" to "సిద్ధ మకరధ్వజం",
        "Poornachandrodayam" to "పూర్ణచంద్రోదయం",
        "Mahavatavidhwamsa Rasam" to "మహావాతవిధ్వంస రసం",
        "Mahavata Rasam" to "మహావాత రసం",
        "Swarna Kanta Vallabha Rasam" to "స్వర్ణ కంఠవల్లభ రసం",
        "Kanthavallabha Rasam" to "కంఠవల్లభ రసం",
        "Pralayagni Rudra Rasam" to "ప్రళయాగ్ని రుద్ర రసం",
        "Sleshantaka Rasam (name unclear)" to "శ్లేషాంతక రసం",
        "Lokanatha Rasam" to "లోకనాథ రసం",
        "Vyadhihara Rasam" to "వ్యాధిహర రసం",
        "Hemasundara Rasam" to "హేమసుందర రసం",
        "Sleshma Gajamkusam" to "శ్లేష్మ గజాంకుశం",
        "Lakshmi Vilasa Rasam" to "లక్ష్మీ విలాస రసం",
        "Vijaya Chintamani" to "విజయ చింతామణి",
        "Rajita Loha Rasayanam" to "రజిత లోహ రసాయనం",
        "Rasa Chintamani" to "రస చింతామణి",
        "Swarna Tamra Rasayanam" to "స్వర్ణ తామ్ర రసాయనం",
        "Tamra Rasayanam" to "తామ్ర రసాయనం",
        "Tamra Vanga Rasayanam" to "తామ్ర వంగ రసాయనం",
        "Chaturloha Rasayanam" to "చతుర్లోహ రసాయనం",
        "Asta Loha Rasayanam" to "అష్ట లోహ రసాయనం",
        "Raja Mruganka Rasam" to "రాజ మృగాంక రసం",
        "Mahaloha Rasayanam" to "మహాలోహ రసాయనం",
        "Loha Rasayanam" to "లోహ రసాయనం",
        "Meha Vanga Raajeyem" to "మేహ వంగ రాజేయం",
        "Rajita Meha Vanga Raajeyem" to "రజిత మేహ వంగ రాజేయం",
        "Sootikaabharanam" to "సూతికాభరణం",
        "Swarna Suryavarti" to "స్వర్ణ సూర్యవర్తి",
        "Kanaka Bala Suryodayam" to "కనక బాల సూర్యోదయం",
        "Mahila Priya" to "మహిళా ప్రియ",
        "Swasaamrutam" to "శ్వాసామృతం",
        "Jaambu Paanam" to "జాంబు పానం",
        "Jeeva Satvam" to "జీవ సత్వం",
        "Mahaabala" to "మహాబల",
        "Ganeshamrutham" to "గణేశామృతం",
        "Gadaa Sanjeevi" to "గదా సంజీవి",
        "Maadeephala Rasayanam" to "మాదీఫల రసాయనం",
        "Jambeera Rasayanam" to "జంబీర రసాయనం",
        "Sitopaladi Churnam" to "సితోపలాది చూర్ణం",
        "Talisadi Churnam" to "తాలిసాది చూర్ణం",
        "Pushyanuga Churnam" to "పుష్యానుగ చూర్ణం",
        "Gangadhara Churnam" to "గంగాధర చూర్ణం",
        "Lavanadi Churnam" to "లవణాది చూర్ణం",
        "Chaturjaataka Churnam" to "చతుర్జాతక చూర్ణం",
        "Sudarshana Churnam" to "సుదర్శన చూర్ణం",
        "Bhaskara Lavana Churnam" to "భాస్కర లవణ చూర్ణం",
        "Maha Drakshaadi Churnam" to "మహా ద్రాక్షాది చూర్ణం",
        "Rasottamaadi Churnam" to "రసోత్తమాది చూర్ణం",
        "Hingvashtaka Churnam" to "హింగ్వాష్టక చూర్ణం",
        "Chandanaadi Churnam" to "చందనాది చూర్ణం",
        "Ashwagandha Churnam" to "అశ్వగంధ చూర్ణం",
        "Triphala Churnam" to "త్రిఫల చూర్ణం",
        "Dantadhavana Churnam" to "దంతధావన చూర్ణం",
        "Vaasadi Dhoomapaanadi Churnam" to "వాసాది ధూమపానాది చూర్ణం",
        "Tippasattu" to "తిప్పసత్తు",
        "Patcha Karpuram" to "పచ్చ కర్పూరం",
        "Garudamjanam" to "గరుడాంజనం",
        "Saraswathi Ghritam" to "సరస్వతీ ఘృతం",
        "Phala Ghritam" to "ఫల ఘృతం",
        "Shatadhouta Ghritam" to "శతధౌత ఘృతం",
        "Kalyana Ghritam" to "కల్యాణ ఘృతం",
        "Surya-prabha Kashaya Ghritam" to "సూర్యప్రభ కషాయ ఘృతం",
        "Bhringaraja Tailam" to "భృంగరాజ తైలం",
        "Amlaki Tailam" to "ఆమలకి తైలం",
        "Aragwadhadi Tailam" to "అరగ్వధాది తైలం",
        "Chandanaadi Tailam" to "చందనాది తైలం",
        "Narayana Tailam" to "నారాయణ తైలం",
        "Vishnupushpi Tailam" to "విష్ణుపుష్పి తైలం",
        "Amruthaadi Tailam" to "అమృతాది తైలం",
        "Sahacharaadi Tailam" to "సహచరాది తైలం",
        "Mahanarayana Tailam" to "మహానారాయణ తైలం",
        "Bala Tailam" to "బల తైలం",
        "Maha Vishagarbha Tailam" to "మహా విషగర్భ తైలం",
        "Pinda Tailam" to "పిండ తైలం",
        "Tamra Tailam" to "తామ్ర తైలం",
        "Karna Roganthaka Tailam" to "కర్ణ రోగాంతక తైలం",
        "Mahaa Moosha Tailam" to "మహా మూష తైలం",
        "Vidangaadi Tailam" to "విడంగాది తైలం",
        "Poorna Chandrodayam" to "పూర్ణ చంద్రోదయం",
        "Vasantha Kusumakaram" to "వసంత కుసుమాకరం",
        "Rajita Chandrodayam" to "రజిత చంద్రోదయం",
        "Bruhat Vaata Chintamani" to "బృహత్ వాత చింతామణి",
        "Maha Panchabhana Rasam" to "మహా పంచభాణ రసం",
        "Tippasattu" to "తిప్పసత్తు",
        "Siddha Makaradhwajam" to "సిద్ధ మకరధ్వజం",
        "Karupura Silajith" to "కర్పూర శిలాజిత్",
        "Karpoora Silajith" to "కర్పూర శిలాజిత్",
        "Karpura Silajith" to "కర్పూర శిలాజిత్",
        "Sammohan asthra" to "సమ్మోహనాస్త్రం",
        "Sammohan Astra" to "సమ్మోహనాస్త్రం",
        "Sammohanāstra" to "సమ్మోహనాస్త్రం",
        "Sootikaavaata Raksham" to "సూతికావాత రక్షం",
        "Sootika Vata Raksham" to "సూతికావాత రక్షం",
        "Sootikavata Raksham" to "సూతికావాత రక్షం",
        "Vykranta" to "విక్రాంత"
    )
    val hindi = mapOf(
        "Mahakanakasindhura Rasam" to "महाकनकसिंधूर रस",
        "Kaamsya (Kanchu)" to "कांस्य (कंचु)",
        "Kaamsya Kanchu" to "कांस्य कंचु",
        "Kravyadi Rasam" to "क्रव्यादि रस",
        "Siddha Makaradhwajam" to "सिद्ध मकरध्वज",
        "Poornachandrodayam" to "पूर्णचन्द्रोदय",
        "Mahavatavidhwamsa Rasam" to "महावातविध्वंस रस",
        "Mahavata Rasam" to "महावात रस",
        "Swarna Kanta Vallabha Rasam" to "स्वर्ण कंठवल्लभ रस",
        "Kanthavallabha Rasam" to "कंठवल्लभ रस",
        "Pralayagni Rudra Rasam" to "प्रलयाग्नि रुद्र रस",
        "Sleshantaka Rasam (name unclear)" to "श्लेषांतक रस",
        "Lokanatha Rasam" to "लोकनाथ रस",
        "Vyadhihara Rasam" to "व्याधिहर रस",
        "Hemasundara Rasam" to "हेमसुंदर रस",
        "Sleshma Gajamkusam" to "श्लेष्म गजांकुश",
        "Lakshmi Vilasa Rasam" to "लक्ष्मी विलास रस",
        "Vijaya Chintamani" to "विजय चिंतामणि",
        "Rajita Loha Rasayanam" to "रजित लोह रसायन",
        "Rasa Chintamani" to "रस चिंतामणि",
        "Swarna Tamra Rasayanam" to "स्वर्ण ताम्र रसायन",
        "Tamra Rasayanam" to "ताम्र रसायन",
        "Tamra Vanga Rasayanam" to "ताम्र वंग रसायन",
        "Chaturloha Rasayanam" to "चतुर्लोह रसायन",
        "Asta Loha Rasayanam" to "अष्ट लोह रसायन",
        "Raja Mruganka Rasam" to "राज मृगांक रस",
        "Mahaloha Rasayanam" to "महोलोह रसायन",
        "Loha Rasayanam" to "लोह रसायन",
        "Meha Vanga Raajeyem" to "मेह वंग राजेयम",
        "Rajita Meha Vanga Raajeyem" to "रजित मेह वंग राजेयम",
        "Sootikaabharanam" to "सूतिकाभरण",
        "Swarna Suryavarti" to "स्वर्ण सूर्यवर्ती",
        "Kanaka Bala Suryodayam" to "कनक बाल सूर्योदय",
        "Mahila Priya" to "महिला प्रिय",
        "Swasaamrutam" to "श्वासामृत",
        "Jaambu Paanam" to "जाम्बु पान",
        "Jeeva Satvam" to "जीव सत्व",
        "Mahaabala" to "महाबल",
        "Ganeshamrutham" to "गणेशामृत",
        "Gadaa Sanjeevi" to "गदा संजीवी",
        "Maadeephala Rasayanam" to "मादीफल रसायन",
        "Jambeera Rasayanam" to "जम्बीर रसायन",
        "Sitopaladi Churnam" to "सितोपलादि चूर्ण",
        "Talisadi Churnam" to "तालिसादि चूर्ण",
        "Pushyanuga Churnam" to "पुष्यानुग चूर्ण",
        "Gangadhara Churnam" to "गंगाधर चूर्ण",
        "Lavanadi Churnam" to "लवणादि चूर्ण",
        "Chaturjaataka Churnam" to "चतुर्जातक चूर्ण",
        "Sudarshana Churnam" to "सुदर्शन चूर्ण",
        "Bhaskara Lavana Churnam" to "भास्कर लवण चूर्ण",
        "Maha Drakshaadi Churnam" to "महा द्राक्षादि चूर्ण",
        "Rasottamaadi Churnam" to "रसॊत्तमादि चूर्ण",
        "Hingvashtaka Churnam" to "हिंग्वाष्टक चूर्ण",
        "Chandanaadi Churnam" to "चन्दनादि चूर्ण",
        "Ashwagandha Churnam" to "अश्वगंधा चूर्ण",
        "Triphala Churnam" to "त्रिफला चूर्ण",
        "Dantadhavana Churnam" to "दंतधावन चूर्ण",
        "Vaasadi Dhoomapaanadi Churnam" to "वासादि धूमपानादि चूर्ण",
        "Tippasattu" to "तिप्पसत्तु",
        "Patcha Karpuram" to "पच्चा कपूर",
        "Garudamjanam" to "गरुड़ांजन",
        "Saraswathi Ghritam" to "सरस्वती घृत",
        "Phala Ghritam" to "फल घृत",
        "Shatadhouta Ghritam" to "शतधौत घृत",
        "Kalyana Ghritam" to "कल्याण घृत",
        "Surya-prabha Kashaya Ghritam" to "सूर्यप्रभा कषाय घृत",
        "Bhringaraja Tailam" to "भृंगराज तैल",
        "Amlaki Tailam" to "आमलकी तैल",
        "Aragwadhadi Tailam" to "अरग्वधादि तैल",
        "Chandanaadi Tailam" to "चन्दनादि तैल",
        "Narayana Tailam" to "नारायण तैल",
        "Vishnupushpi Tailam" to "विष्णुपुष्पी तैल",
        "Amruthaadi Tailam" to "अमृतादि तैल",
        "Sahacharaadi Tailam" to "सहचरादि तैल",
        "Mahanarayana Tailam" to "महानारायण तैल",
        "Bala Tailam" to "बल तैल",
        "Maha Vishagarbha Tailam" to "महा विषगर्भ तैल",
        "Pinda Tailam" to "पिंड तैल",
        "Tamra Tailam" to "ताम्र तैल",
        "Karna Roganthaka Tailam" to "कर्ण रोगांतक तैल",
        "Mahaa Moosha Tailam" to "महा मूष तैल",
        "Vidangaadi Tailam" to "विडंगादि तैल",
        "Poorna Chandrodayam" to "पूर्ण चन्द्रोदय",
        "Vasantha Kusumakaram" to "वसंत कुसुमाकर",
        "Rajita Chandrodayam" to "रजित चन्द्रोदय",
        "Bruhat Vaata Chintamani" to "बृहत वात चिंतामणि",
        "Maha Panchabhana Rasam" to "महा पंचभाण रस",
        "Tippasattu" to "तिप्पसत्तु",
        "Siddha Makaradhwajam" to "सिद्ध मकरध्वज",
        "Karupura Silajith" to "कर्पूर शिलाजीत",
        "Karpoora Silajith" to "कर्पूर शिलाजीत",
        "Karpura Silajith" to "कर्पूर शिलाजीत",
        "Sammohan asthra" to "सम्मोहनास्त्र",
        "Sammohan Astra" to "सम्मोहनास्त्र",
        "Sammohanāstra" to "सम्मोहनास्त्र",
        "Sootikaavaata Raksham" to "सूतिका वात रक्षम्",
        "Sootika Vata Raksham" to "सूतिका वात रक्षम्",
        "Sootikavata Raksham" to "सूतिका वात रक्षम्",
        "Vykranta" to "विक्रांत"
    )
    fun key(value: String) = value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    return when (language) {
        "Telugu" -> telugu.entries.firstOrNull { key(it.key) == key(name) }?.value ?: transliterateLatinToTelugu(name)
        "Hindi" -> hindi.entries.firstOrNull { key(it.key) == key(name) }?.value ?: name
        else -> name
    }
}

private data class Product(val id: String, val name: String, val subtitle: String, val price: Double, val category: String, val emoji: String, val description: String, val imageUrl: String, val quantity: String, val usage: String, val cures: String, val majorIngredients: String, val signature: Boolean = false, val productStock: Int = 1, val isHidden: Boolean = false, val isActive: Boolean = true)
private data class ProductRatingSummary(val average: Double = 0.0, val count: Int = 0, val myRating: Int = 0)
private data class ProductLikeSummary(val count: Int = 0, val likedByMe: Boolean = false)
private val categories = listOf("All", "Lehyas", "Ghrutas", "Pills", "Tailas", "Churnas", "Rasayanas", "Bhasmas")

// Offline fallback keeps the disease selector usable until Firestore is populated.
private val fallbackDiseases = listOf("All Diseases", "Cough", "Fever", "Obesity", "Tuberculosis (TB)")

private fun localProductImageFor(context: android.content.Context, productName: String): String {
    val folder = "drawable/products"
    fun normalize(value: String): String = value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        .removeSuffix("churnas").removeSuffix("churnam").removeSuffix("choornas").removeSuffix("choornam")
        .removeSuffix("product").removeSuffix("profuct")
    val normalizedName = normalize(productName)
    val assets = try {
        context.assets.list(folder)?.toList().orEmpty()
    } catch (_: Exception) {
        emptyList()
    }
    val match = assets.firstOrNull { filename ->
        val stem = normalize(filename.substringBeforeLast('.'))
        stem == normalizedName ||
            stem.replace("mahad", "maha") == normalizedName ||
            (stem == "rasottamaadi" && normalizedName == "rasottamadi") ||
            (stem == "dantadhavana" && normalizedName == "dantadhavanam") ||
            (stem == "chyavanaprasha" && normalizedName in setOf("chyavanaprasha", "chyavanaprasham", "chyavanaprash", "chyavanaprasa", "chyanaprasa", "chyanaprasham", "chyanaprash"))
    }
    return match?.let { "file:///android_asset/$folder/${it.replace(" ", "%20")}" }.orEmpty()
}

private fun productFromFirestore(data: Map<String, Any?>, context: android.content.Context, keepUnnamedForAdmin: Boolean = false): Product? {
    val id = data["id"]?.toString().orEmpty()
    val name = listOf("product_name", "name", "productName", "product_title", "title", "medicine_name", "productname", "item_name", "itemName", "medicineName", "display_name", "displayName").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    if (name.isBlank() && !keepUnnamedForAdmin) return null
    val displayName = name.ifBlank { "Unnamed product (${id.take(8).ifBlank { "record" }})" }
    // Show only the pack weight beneath the product name, e.g. "5g".
    // Quantity is intentionally hidden; avoid separators such as "1 · 5 · g".
    val weight = data["product_weight"]?.toString()?.trim().orEmpty()
    val units = data["product_units"]?.toString()?.trim().orEmpty()
    val cleanWeight = weight.removeSuffix(units).trim().removeSuffix(".").trim()
    val details = if (cleanWeight.isNotBlank() && units.isNotBlank()) "$cleanWeight$units" else cleanWeight.ifBlank { units }
    val price = data["product_price"]?.toString()?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    val rawCategory = listOf("product_category", "category", "category_name", "productCategory", "categoryName", "product_type", "productType", "type", "product_category_name").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val categoryKey = rawCategory.lowercase().replace(Regex("[^a-z]"), "")
    val category = when {
        categoryKey.contains("lehya") || categoryKey.contains("lehyam") || categoryKey.contains("lehyalu") -> "Lehyas"
        categoryKey.contains("grutha") || categoryKey.contains("ghrita") || categoryKey.contains("ghrutha") || categoryKey == "ghee" -> "Ghrutas"
        categoryKey.contains("pill") || categoryKey == "vati" || categoryKey == "vatis" -> "Pills"
        categoryKey.contains("taila") || categoryKey == "oil" || categoryKey == "oils" -> "Tailas"
        categoryKey.contains("churna") || categoryKey.contains("choorna") -> "Churnas"
        categoryKey.contains("rasayana") -> "Rasayanas"
        categoryKey.contains("rasayoga") -> "Rasayogas"
        categoryKey.contains("bhasma") -> "Bhasmas"
        else -> rawCategory
    }
    val emoji = when (category) {
        "Lehyas" -> "🫙"
        "Ghrutas" -> "🫙"
        "Pills" -> "💊"
        "Tailas" -> "🫙"
        "Churnas" -> "🌾"
        "Rasayogas" -> "🪷"
        "Rasayanas" -> "🪴"
                            "Rasayogas" -> "🪷"
                                        "Rasayogas" -> "🪷"
        "Bhasmas" -> "🪨"
        else -> "🌿"
    }
    val description = listOf("product_description", "description", "product_details", "details").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val remoteImageUrl = listOf("product_image", "image_url", "imageUrl", "image", "product_image_url").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") } }.orEmpty()
    // Automatically find artwork in app/src/main/assets/drawable/products by matching its filename to the product name.
    val localProductImage = localProductImageFor(context, name)
    val normalizedProductImageName = displayName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    // Chyavanaprasha has several common catalogue spellings; prefer the bundled artwork
    // for these aliases so a stale or incorrect Firestore URL cannot hide the image.
    val isChyavanaprasha = normalizedProductImageName in setOf(
        "chyavanaprasha", "chyavanaprasham", "chyavanaprash", "chyavanaprasa",
        "chyanaprasa", "chyanaprasham", "chyanaprash"
    )
    val imageUrl = if (isChyavanaprasha) localProductImage.ifBlank { remoteImageUrl } else remoteImageUrl.ifBlank { localProductImage }
    val quantity = listOf("product_quantity", "quantity", "stock").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val usage = listOf("product_usage", "usage", "how_to_use", "directions").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val cures = listOf("product_cures", "cures", "benefits", "indications").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val majorIngredients = listOf("product_major_ingredients", "major_ingredients", "ingredients", "product_ingredients").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    return Product(id.ifBlank { displayName.hashCode().toString() }, displayName, details.ifBlank { "Traditional Ayurvedic product" }, price, category, emoji, description, imageUrl, quantity, usage, cures, majorIngredients, signature = data["signature"]?.toString()?.toIntOrNull() == 1 || data["signature"]?.toString()?.equals("true", ignoreCase = true) == true, productStock = data["product_stock"]?.toString()?.toIntOrNull()?.coerceIn(0, 1) ?: 1, isHidden = data["product_hidden"]?.toString()?.equals("true", ignoreCase = true) == true, isActive = data["product_active"]?.toString()?.equals("false", ignoreCase = true) != true)
}
@Composable
private fun AyurvedicApp() {
    val appContext = LocalContext.current
    // Match system bars to the active customer theme; admin content keeps its own neutral palette.
    val systemBarCream = if (activeAppThemeId == 3) Color(0xFF0B1118) else if (activeAppThemeId == 4) Color(0xFFF1FAF0) else Color.White
    SideEffect {
        (appContext as? Activity)?.window?.let { appWindow ->
            val creamColor = systemBarCream.toArgb()
            // Keep the camera/status-bar cutout area owned by the system bars and white,
            // rather than allowing edge-to-edge app content to draw behind it.
            WindowCompat.setDecorFitsSystemWindows(appWindow, true)
            appWindow.navigationBarColor = creamColor
            // Use the exact same system-bar color for the top camera/status-bar area.
            appWindow.statusBarColor = appWindow.navigationBarColor
            appWindow.decorView.systemUiVisibility = if (activeAppThemeId == 3) {
                appWindow.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                appWindow.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
    val auth = remember { FirebaseAuth.getInstance() }
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }
    var authResolved by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var adminProductsOpen by remember { mutableStateOf(false) }
    var adminOrdersOpen by remember { mutableStateOf(false) }
    var adminCustomersOpen by remember { mutableStateOf(false) }
    var adminDiseasesOpen by remember { mutableStateOf(false) }
    var adminBannersOpen by remember { mutableStateOf(false) }
    var adminThemesOpen by remember { mutableStateOf(false) }
    var adminRoleResolved by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(6000)
        showSplash = false
    }
    DisposableEffect(Unit) {
        val themeRegistration = FirebaseFirestore.getInstance().collection("appSettings").document("appTheme")
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    val settings = snapshot
                    val lightThemeEnabled = settings?.getBoolean("lightThemeEnabled") ?: true
                    val activeTheme = if (lightThemeEnabled) (settings?.getLong("activeTheme") ?: 1L).toInt().coerceIn(1, 4) else 1
                    applyAppPalette(activeTheme)
                }
            }
        onDispose { themeRegistration.remove() }
    }
    DisposableEffect(auth) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            signedIn = firebaseAuth.currentUser != null
            authResolved = true
        }
        auth.addAuthStateListener(authStateListener)
        onDispose { auth.removeAuthStateListener(authStateListener) }
    }
    DisposableEffect(signedIn, auth.currentUser?.uid) {
        val user = auth.currentUser
        if (!signedIn || user == null) {
            isAdmin = false
            adminRoleResolved = true
            onDispose { }
        } else {
            adminRoleResolved = false
            val registration = FirebaseFirestore.getInstance()
                .collection("customers").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isAdmin = false
                        adminRoleResolved = true
                    } else if (((snapshot?.contains("is_active") == true && snapshot.getBoolean("is_active") == false) || (snapshot?.contains("is_active") != true && snapshot?.contains("enabled") == true && snapshot.getBoolean("enabled") == false))) {
                        isAdmin = false
                        adminRoleResolved = true
                        Toast.makeText(
                            appContext,
                            "This account has been disabled. Please contact the store administrator.",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                    } else {
                        isAdmin = snapshot?.getBoolean("is_admin") == true
                        adminRoleResolved = true
                    }
                }
            onDispose { registration.remove() }
        }
    }
    var userInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("+91") }
    var phoneOtpCode by remember { mutableStateOf("") }
    var phoneOtpSent by remember { mutableStateOf(false) }
    var emailOtpCode by remember { mutableStateOf("") }
    var emailOtpSent by remember { mutableStateOf(false) }
    var phoneVerificationId by remember { mutableStateOf<String?>(null) }
    var phoneResendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var confirmPassword by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            isLoading = false
            val signInFailure = try {
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)
                null
            } catch (error: com.google.android.gms.common.api.ApiException) {
                "Google sign-in failed (status ${error.statusCode}): ${error.statusMessage ?: error.localizedMessage ?: "unknown error"}"
            } catch (error: Exception) {
                error.localizedMessage
            }
            // Google status 12501 means the account chooser was cancelled; do not show it as an authentication error.
            authError = if (signInFailure?.contains("status 12501") == true) {
                null
            } else {
                signInFailure ?: "Google sign-in was cancelled (result code ${result.resultCode})."
            }
        } else {
            try {
                val accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = accountTask.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    isLoading = false
                    authError = "Google sign-in token missing. Check Firebase Google provider setup."
                } else {
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    val displayName = user.displayName.orEmpty().trim()
                                    val nameParts = displayName.split(Regex("\\s+")).filter { it.isNotBlank() }
                                    val profile = mutableMapOf<String, Any>(
                                        "email" to (user.email ?: account.email.orEmpty()),
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                    if (nameParts.isNotEmpty()) profile["firstName"] = nameParts.first()
                                    if (nameParts.size > 1) profile["lastName"] = nameParts.drop(1).joinToString(" ")
                                    if (!user.photoUrl?.toString().isNullOrBlank()) profile["photoUrl"] = user.photoUrl.toString()
                                    FirebaseData.saveCustomerProfile(user.uid, profile) { error ->
                                        if (error != null) android.util.Log.w("SLGAN_AUTH", "Google profile save failed", error)
                                    }
                                }
                                authError = null
                            } else {
                                authError = task.exception?.localizedMessage ?: "Google sign-in failed. Please try again."
                            }
                        }
                }
            } catch (error: Exception) {
                isLoading = false
                authError = error.localizedMessage ?: "Google sign-in failed. Please try again."
            }
        }
    }
    val onGoogleSignIn: () -> Unit = {
        val webClientIdResource = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (webClientIdResource == 0) {
            authError = "Google sign-in setup is incomplete. Enable Google in Firebase Authentication and update google-services.json."
        } else {
            try {
                isLoading = true
                authError = null
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(appContext.getString(webClientIdResource))
                    .build()
                googleSignInLauncher.launch(GoogleSignIn.getClient(appContext, options).signInIntent)
            } catch (error: Exception) {
                isLoading = false
                authError = error.localizedMessage ?: "Could not start Google sign-in."
            }
        }
    }
    var selectedTab by remember { mutableStateOf("Home") }
    var previousTab by remember { mutableStateOf("Home") }
    // Keep a small history of visited bottom-navigation tabs so Back returns to the
    // immediately previous tab instead of always jumping to Home.
    val tabHistory = remember { mutableStateListOf("Home") }
    LaunchedEffect(selectedTab) {
        if (tabHistory.lastOrNull() != selectedTab) {
            tabHistory.add(selectedTab)
            while (tabHistory.size > 20) tabHistory.removeAt(0)
        }
    }
    var latestOrderId by remember { mutableStateOf<String?>(null) }
    var latestOrderSuccess by remember { mutableStateOf<Boolean?>(null) }
    var latestOrderInvoiceUrl by remember { mutableStateOf<String?>(null) }
    var latestOrderMessage by remember { mutableStateOf<String?>(null) }
    val languagePreferences = remember { appContext.getSharedPreferences("slgan_preferences", android.content.Context.MODE_PRIVATE) }
    var appLanguage by remember { mutableStateOf(languagePreferences.getString("app_language", "English")?.takeIf { it in listOf("English", "Telugu", "Hindi") } ?: "English") }
    LaunchedEffect(appLanguage) { languagePreferences.edit().putString("app_language", appLanguage).apply() }
    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var liveProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var adminLiveProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var productsLoading by remember { mutableStateOf(true) }
    var productsError by remember { mutableStateOf<String?>(null) }
    var liveDiseases by remember { mutableStateOf<List<String>>(emptyList()) }
    var diseasesLoading by remember { mutableStateOf(true) }
    var diseasesError by remember { mutableStateOf<String?>(null) }
    var productRatings by remember { mutableStateOf<Map<String, ProductRatingSummary>>(emptyMap()) }
    var productLikes by remember { mutableStateOf<Map<String, ProductLikeSummary>>(emptyMap()) }
    DisposableEffect(signedIn, auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        val registration = FirebaseFirestore.getInstance().collection("productLikes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val grouped = snapshot.documents.mapNotNull { doc ->
                    val productId = doc.getString("productId")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val userId = doc.getString("userId")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    productId to (userId == uid)
                }.groupBy({ it.first }, { it.second })
                productLikes = grouped.mapValues { (_, users) -> ProductLikeSummary(count = users.size, likedByMe = uid != null && users.any { it }) }
            }
        onDispose { registration.remove() }
    }
    val toggleProductLike: (Product) -> Unit = { product ->
        val user = auth.currentUser
        if (user == null) Toast.makeText(appContext, "Please sign in to like products.", Toast.LENGTH_SHORT).show()
        else {
            val likeRef = FirebaseFirestore.getInstance().collection("productLikes").document(product.id + "_" + user.uid)
            if (productLikes[product.id]?.likedByMe == true) {
                likeRef.delete().addOnFailureListener { error -> Toast.makeText(appContext, error.localizedMessage ?: "Could not remove like.", Toast.LENGTH_SHORT).show() }
            } else {
                likeRef.set(hashMapOf("productId" to product.id, "productName" to product.name, "userId" to user.uid, "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                    .addOnFailureListener { error -> Toast.makeText(appContext, error.localizedMessage ?: "Could not like product.", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    DisposableEffect(signedIn) {
        if (signedIn) {
            val registration = FirebaseData.observeProducts(
                onProducts = { records ->
                    // A malformed Firestore document must not crash the entire app after login.
                    val parsedProducts = records.mapNotNull { record ->
                        try {
                            productFromFirestore(record, appContext)
                        } catch (error: Exception) {
                            android.util.Log.w("SLGAN_PRODUCTS", "Skipping an invalid product record", error)
                            null
                        }
                    }
                    liveProducts = parsedProducts
                    adminLiveProducts = records.mapNotNull { record ->
                        try {
                            productFromFirestore(record, appContext, keepUnnamedForAdmin = true)
                        } catch (error: Exception) {
                            android.util.Log.w("SLGAN_PRODUCTS", "Could not parse admin product record", error)
                            null
                        }
                    }
                    productsLoading = false
                    productsError = null
                },
                onError = { error ->
                    productsError = error.localizedMessage ?: "Unable to load products."
                    productsLoading = false
                }
            )
            onDispose { registration.remove() }
        } else {
            liveProducts = emptyList()
            adminLiveProducts = emptyList()
            productsLoading = true
            productsError = null
            onDispose { }
        }
    }
    // Rating records are public-read; each signed-in customer maintains one rating per product.
    DisposableEffect(signedIn) {
        if (signedIn) {
            val uid = auth.currentUser?.uid.orEmpty()
            val registration = FirebaseFirestore.getInstance().collection("productRatings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val grouped = snapshot.documents.mapNotNull { doc ->
                        val productId = doc.getString("productId")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val rating = doc.getLong("rating")?.toInt()?.takeIf { it in 1..5 } ?: return@mapNotNull null
                        Triple(productId, rating, doc.getString("userId") == uid)
                    }.groupBy { it.first }
                    productRatings = grouped.mapValues { (_, records) ->
                        ProductRatingSummary(average = records.map { it.second }.average(), count = records.size,
                            myRating = records.firstOrNull { it.third }?.second ?: 0)
                    }
                }
            onDispose { registration.remove() }
        } else {
            productRatings = emptyMap()
            onDispose { }
        }
    }
    // Disease catalogue is public-read data; load it regardless of sign-in state.
    DisposableEffect(Unit) {
        diseasesLoading = true
        val registration = FirebaseData.observeDiseases(
            onDiseases = { records ->
                liveDiseases = records
                diseasesLoading = false
                diseasesError = null
            },
            onError = { error ->
                diseasesError = error.localizedMessage ?: "Unable to load diseases."
                diseasesLoading = false
            }
        )
        onDispose { registration.remove() }
    }
    val cart = remember { mutableStateMapOf<String, Int>() }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val incomingProductId = remember {
        (appContext as? android.app.Activity)?.intent?.data?.let { uri ->
            when {
                uri.scheme == "https" && uri.host == "slgan-a364e.web.app" && uri.path == "/product" ->
                    uri.getQueryParameter("id")
                uri.scheme == "slgan" && uri.host == "product" ->
                    uri.lastPathSegment
                else -> null
            }
        }
    }
    LaunchedEffect(incomingProductId, liveProducts) {
        if (!incomingProductId.isNullOrBlank()) {
            liveProducts.firstOrNull { it.id == incomingProductId }?.let { sharedProduct ->
                selectedProduct = sharedProduct
            }
        }
    }
    var lastBackPressedAt by remember { mutableStateOf(0L) }
    BackHandler(enabled = !showSplash && authResolved) {
        when {
            isAdmin && adminThemesOpen -> adminThemesOpen = false
            isAdmin && adminBannersOpen -> adminBannersOpen = false
            isAdmin && adminCustomersOpen -> adminCustomersOpen = false
            isAdmin && adminDiseasesOpen -> adminDiseasesOpen = false
            isAdmin && adminOrdersOpen -> adminOrdersOpen = false
            isAdmin && adminProductsOpen -> adminProductsOpen = false
            selectedProduct != null -> selectedProduct = null
            tabHistory.size > 1 -> {
                // Drop the current tab, then restore the preceding tab.
                if (tabHistory.lastOrNull() == selectedTab) tabHistory.removeAt(tabHistory.lastIndex)
                selectedTab = tabHistory.lastOrNull() ?: "Home"
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedAt < 2000L) {
                    (appContext as? android.app.Activity)?.finish()
                } else {
                    lastBackPressedAt = now
                    android.widget.Toast.makeText(
                        appContext,
                        "Press back again to exit",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, secondary = Leaf, background = Cream, surface = Cream, onSurface = Ink)) {
        Surface(Modifier.fillMaxSize(), color = if (showSplash || !authResolved) Color.White else Cream) {
            if (showSplash || !authResolved) {
                SplashScreen()
            } else if (!signedIn) LoginScreen(
                value = userInput,
                onValueChange = { userInput = it },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                isCreatingAccount = isCreatingAccount,
                onToggleMode = { isCreatingAccount = !isCreatingAccount; authError = null; confirmPassword = "" },
                errorMessage = authError,
                loading = isLoading,
                onGoogleSignIn = onGoogleSignIn,
                emailOtpCode = emailOtpCode,
                onEmailOtpCodeChange = { emailOtpCode = it.filter(Char::isDigit).take(6) },
                emailOtpSent = emailOtpSent,
                onSendEmailOtp = {
                    val email = userInput.trim().lowercase()
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        authError = "Enter a valid email address first."
                    } else {
                        isLoading = true
                        authError = null
                        requestEmailOtp(email, "send") { ok, message, _ ->
                            isLoading = false
                            authError = message
                            if (ok) { emailOtpSent = true; emailOtpCode = "" }
                        }
                    }
                },
                onVerifyEmailOtp = {
                    val email = userInput.trim().lowercase()
                    if (emailOtpCode.length != 6) {
                        authError = "Enter the 6-digit email code."
                    } else {
                        isLoading = true
                        authError = null
                        requestEmailOtp(email, "verify", emailOtpCode) { ok, message, customToken ->
                            if (!ok || customToken.isNullOrBlank()) {
                                isLoading = false
                                authError = message.ifBlank { "Email verification did not return a Firebase sign-in token. Update the Apps Script deployment." }
                            } else {
                                auth.signInWithCustomToken(customToken).addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        authError = null
                                        auth.currentUser?.uid?.let { uid ->
                                            FirebaseData.saveCustomerProfile(uid, mapOf("email" to email, "updatedAt" to System.currentTimeMillis())) { error ->
                                                if (error != null) android.util.Log.w("SLGAN_AUTH", "Email OTP profile save failed", error)
                                            }
                                        }
                                    } else authError = task.exception?.localizedMessage ?: "Could not create a Firebase session for this email."
                                }
                            }
                        }
                    }
                },
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                otpCode = phoneOtpCode,
                onOtpCodeChange = { phoneOtpCode = it.filter(Char::isDigit).take(6) },
                otpSent = phoneOtpSent,
                onSendPhoneOtp = {
                    val number = phoneNumber.trim().replace(" ", "")
                    val activity = appContext as? Activity
                    if (activity == null || !number.matches(Regex("^\\+[1-9]\\d{7,14}$"))) {
                        authError = "Enter a valid phone number with country code, e.g. +919876543210."
                    } else {
                        isLoading = true
                        authError = null
                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                    isLoading = false
                                    if (!task.isSuccessful) authError = task.exception?.localizedMessage ?: "Phone verification failed."
                                }
                            }
                            override fun onVerificationFailed(error: com.google.firebase.FirebaseException) {
                                isLoading = false
                                authError = error.localizedMessage ?: "Could not send SMS. Check Firebase phone-auth settings."
                            }
                            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                                isLoading = false
                                phoneVerificationId = id
                                phoneResendToken = token
                                phoneOtpSent = true
                                authError = "Verification code sent by SMS."
                            }
                        }
                        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber(number)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(callbacks)
                        phoneResendToken?.let { optionsBuilder.setForceResendingToken(it) }
                        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
                    }
                },
                onVerifyPhoneOtp = {
                    val id = phoneVerificationId
                    if (id.isNullOrBlank() || phoneOtpCode.length != 6) {
                        authError = "Enter the 6-digit SMS code first."
                    } else {
                        isLoading = true
                        authError = null
                        val credential = PhoneAuthProvider.getCredential(id, phoneOtpCode)
                        auth.signInWithCredential(credential).addOnCompleteListener { task ->
                            isLoading = false
                            if (!task.isSuccessful) authError = task.exception?.localizedMessage ?: "Incorrect or expired SMS code."
                        }
                    }
                },
                onForgotPassword = {
                    val email = userInput.trim().lowercase()
                    when {
                        email.isBlank() -> {
                            authError = "Enter your registered email address first."
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            authError = "Enter a valid email address before requesting a password reset."
                        }
                        else -> {
                            isLoading = true
                            authError = null
                            requestEmailOtp(email, "passwordReset") { ok, message, _ ->
                                isLoading = false
                                authError = if (ok) message else
                                    message.ifBlank { "Could not send password reset email. Please try again later." }
                            }
                        }
                    }
                },
                onContinue = {
                    val email = userInput.trim()
                    when {
                        email.isBlank() -> {
                            authError = "Enter your email address to sign in."
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            authError = "Enter a valid email address (for example, name@example.com)."
                        }
                        password.isBlank() -> {
                            authError = "Enter your password to sign in."
                        }
                        password.length < 6 -> {
                            authError = "Your password must contain at least 6 characters."
                        }
                        isCreatingAccount && password != confirmPassword -> {
                            authError = "Password and confirm password do not match."
                        }
                        else -> {
                            isLoading = true
                            authError = null
                            val task = if (isCreatingAccount) auth.createUserWithEmailAndPassword(email, password)
                                       else auth.signInWithEmailAndPassword(email, password)
                            task.addOnCompleteListener { result ->
                                isLoading = false
                                if (result.isSuccessful) {
                                    // Let FirebaseAuth.AuthStateListener be the single source of truth
                                    // for navigation. Avoid switching screens before currentUser is ready.
                                    auth.currentUser?.uid?.let { uid ->
                                        FirebaseData.saveCustomerProfile(
                                            uid,
                                            mapOf("email" to email, "updatedAt" to System.currentTimeMillis())
                                        ) { error ->
                                            if (error != null) {
                                                android.util.Log.w("SLGAN_AUTH", "Profile save failed after sign-in", error)
                                            }
                                        }
                                    }
                                } else {
                                    authError = userFriendlyAuthError(result.exception, isCreatingAccount)
                                }
                            }
                        }
                    }
                }
            )
            else if (!adminRoleResolved) Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) { AppLoader("Checking account permissions…", messageColor = Ink) }
            else if (isAdmin && adminCustomersOpen) AdminCustomersScreen(onBack = { adminCustomersOpen = false })
            else if (isAdmin && adminDiseasesOpen) AdminDiseasesScreen(onBack = { adminDiseasesOpen = false })
            else if (isAdmin && adminBannersOpen) BannerManagementScreen(onBack = { adminBannersOpen = false })
            else if (isAdmin && adminThemesOpen) ThemeManagementScreen(onBack = { adminThemesOpen = false })
            else if (isAdmin && adminOrdersOpen) AdminOrdersScreen(onBack = { adminOrdersOpen = false })
            else if (isAdmin && adminProductsOpen) AdminProductsScreen(
                products = adminLiveProducts,
                loading = productsLoading,
                loadError = productsError,
                onBack = { adminProductsOpen = false }
            )
            else if (isAdmin) AdminDashboardScreen(
                email = auth.currentUser?.email.orEmpty(),
                productCount = adminLiveProducts.size,
                productsLoading = productsLoading,
                onProductsClick = { adminProductsOpen = true },
                onOrdersClick = { adminOrdersOpen = true },
                onCustomersClick = { adminCustomersOpen = true },
                onDiseasesClick = { adminDiseasesOpen = true },
                onBannersClick = { adminBannersOpen = true },
                onThemesClick = { adminThemesOpen = true },
                onSignOut = {
                    auth.signOut()
                    adminProductsOpen = false
                    adminOrdersOpen = false
                    adminCustomersOpen = false
                    adminDiseasesOpen = false
                    adminBannersOpen = false
                    adminThemesOpen = false
                    selectedTab = "Home"
                    selectedProduct = null
                    cart.clear()
                }
            )
            else if (selectedProduct != null) ProductDetailsPage(
                product = selectedProduct!!,
                ratingSummary = productRatings[selectedProduct!!.id] ?: ProductRatingSummary(),
                likeSummary = productLikes[selectedProduct!!.id] ?: ProductLikeSummary(),
                onToggleLike = { toggleProductLike(selectedProduct!!) },
                cartCount = cart[selectedProduct!!.id] ?: 0,
                onBack = { selectedProduct = null },
                onCartClick = { selectedProduct = null; previousTab = selectedTab; selectedTab = "Cart" },
                onNavigateTab = { route ->
                    selectedProduct = null
                    if (route != selectedTab) previousTab = selectedTab
                    if (route == "Home") { selectedCategory = "All"; search = "" }
                    if (route == "Categories") selectedCategory = "All"
                    selectedTab = route
                },
                onAdd = { id -> cart[id] = (cart[id] ?: 0) + 1 },
                onRemove = { id -> val n = cart[id] ?: 0; if (n <= 1) cart.remove(id) else cart[id] = n - 1 }
            )
            else ShopScreen(selectedTab, previousTab, { nextTab -> if (nextTab != selectedTab) previousTab = selectedTab; if (nextTab == "Home") { selectedCategory = "All"; search = "" }; selectedTab = nextTab }, appLanguage, { appLanguage = it }, selectedCategory, { selectedCategory = it }, search, { search = it }, cart, liveProducts.filterNot { it.isHidden }, productRatings, productLikes, toggleProductLike, productsLoading, productsError, liveDiseases.ifEmpty { fallbackDiseases }, diseasesLoading, if (liveDiseases.isEmpty()) null else diseasesError, latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage,
                { id -> cart[id] = (cart[id] ?: 0) + 1 },
                { id -> val n = cart[id] ?: 0; if (n <= 1) cart.remove(id) else cart[id] = n - 1 },
                { orderId, success, invoiceUrl, message ->
                    latestOrderId = orderId.ifBlank { null }
                    latestOrderSuccess = success
                    latestOrderInvoiceUrl = invoiceUrl
                    latestOrderMessage = message
                    if (success) cart.clear()
                    selectedTab = "Orders"
                },
                { auth.signOut(); signedIn = false; password = "" },
                { product -> selectedProduct = product },
                { cart.clear() })
        }
    }
    }
}
@Composable
private fun SplashScreen() {
    val splashContext = LocalContext.current
    DisposableEffect(splashContext) {
        val splashMusic = android.media.MediaPlayer.create(
            splashContext.applicationContext,
            R.raw.splash_music
        )
        splashMusic?.setOnCompletionListener {
            // The six-second intro finishes naturally; release when the splash leaves.
        }
        splashMusic?.start()
        onDispose {
            try {
                splashMusic?.let { player ->
                    if (player.isPlaying) player.stop()
                    player.release()
                }
            } catch (error: Exception) {
                android.util.Log.w("SLGANMusic", "Unable to stop splash music cleanly.", error)
            }
        }
    }
    // Keep splash artwork in sync with the currently selected app palette.
    val botanicalTheme = Forest == appThemedColor(Color(0xFF214B35))
    val splashBackground = Color.White
    val splashTint = if (botanicalTheme) appThemedColor(Color(0xFF557A45)) else appThemedColor(Color(0xFF8A4B2A))
    val splashHalo = if (botanicalTheme) appThemedColor(Color(0xFFB9D5A6)) else appThemedColor(Color(0xFFE8C7A0))
    val splashRing = if (botanicalTheme) appThemedColor(Color(0xFF9EBB86)) else appThemedColor(Color(0xFFD9A441))
    val motion = rememberInfiniteTransition(label = "natureSplash")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )
    val halo by motion.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "softHalo"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Image(
            painter = painterResource(R.drawable.slgan_login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            // Preserve the complete approved artwork; Crop can cut off its right-side border.
            contentScale = ContentScale.FillBounds
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.42f)))
        // Let every product-category emoji drift down like a falling leaf.
        val categoryEmojis = listOf("🌿", "🍃", "🌱", "🍂", "🌿", "🍃", "🌾", "🌱", "🗂️", "🍯", "🏺", "💊", "🫙", "🌿", "🍃", "🌱", "🪨", "🍂", "🌿", "🍃", "🌱", "🌾", "🍃", "🌿")
        val leafPositions = listOf(0.02f, 0.105f, 0.19f, 0.275f, 0.36f, 0.445f, 0.53f, 0.615f, 0.70f, 0.785f, 0.87f, 0.955f, 0.06f, 0.145f, 0.23f, 0.315f, 0.40f, 0.485f, 0.57f, 0.655f, 0.74f, 0.825f, 0.91f, 0.98f)
        leafPositions.forEachIndexed { index, fraction ->
            val fall by motion.animateFloat(
                initialValue = -80f - (index * 95f),
                targetValue = maxHeight.value + 100f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5200 + (index % 7) * 450, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "fallingLeaf$index"
            )
            val sway by motion.animateFloat(
                initialValue = -12f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1700 + index * 130, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "leafSway$index"
            )
            Text(
                text = categoryEmojis[index % categoryEmojis.size],
                modifier = Modifier
                    .offset(x = maxWidth * fraction - 12.dp + sway.dp, y = fall.dp)
                    .graphicsLayer { rotationZ = (fall * 0.8f) % 360f; alpha = 0.82f },
                fontSize = if (index % 3 == 0) 25.sp else 19.sp
            )
        }

        // Light, clean center stage for the rotating company logo.
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(224.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(220.dp).graphicsLayer { alpha = halo }
                        .background(splashHalo, CircleShape)
                )
                Box(
                    Modifier.size(202.dp)
                        .border(1.dp, splashRing.copy(alpha = 0.75f), CircleShape)
                )
                Box(
                    Modifier.size(178.dp)
                        .background(splashBackground, CircleShape)
                        .border(2.dp, Gold, CircleShape)
                        .padding(13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(id = R.drawable.slgan_screen_logo), contentDescription = "Sri Lakshmi Ganapathi Ayurveda Nilayam logo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().clip(CircleShape).background(Cream).graphicsLayer { rotationZ = rotation })
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                tr("SRI LAKSHMI GANAPATHI", LocalAppLanguage.current),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                tr("AYURVEDA NILAYAM", LocalAppLanguage.current),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = appThemedColor(Color(0xFF38251F)),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                tr("TRADITION • TRUST • WELLNESS", LocalAppLanguage.current),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = splashTint,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Preparing your wellness experience", LocalAppLanguage.current),
                fontSize = 11.sp,
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
@Composable
private fun LoginScreen(
    value: String,
    onValueChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isCreatingAccount: Boolean,
    onToggleMode: () -> Unit,
    errorMessage: String?,
    loading: Boolean,
    onGoogleSignIn: () -> Unit,
    emailOtpCode: String,
    onEmailOtpCodeChange: (String) -> Unit,
    emailOtpSent: Boolean,
    onSendEmailOtp: () -> Unit,
    onVerifyEmailOtp: () -> Unit,
    onContinue: () -> Unit,
    onForgotPassword: () -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    otpCode: String,
    onOtpCodeChange: (String) -> Unit,
    otpSent: Boolean,
    onSendPhoneOtp: () -> Unit,
    onVerifyPhoneOtp: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Image(
            painter = painterResource(R.drawable.slgan_login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            // Show the entire background artwork on every screen size; do not crop its edges.
            contentScale = ContentScale.FillBounds
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(Modifier.height(14.dp))
        val logoScale = remember { Animatable(0.55f) }
        val logoRotation = remember { Animatable(-18f) }
        val logoAlpha = remember { Animatable(0f) }
        val sparkleTransition = rememberInfiniteTransition(label = "logoCombinedEffect")
        val haloAlpha by sparkleTransition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "goldHalo"
        )
        val leafDrift by sparkleTransition.animateFloat(
            initialValue = -7f,
            targetValue = 7f,
            animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "leafDrift"
        )
        val shineX by sparkleTransition.animateFloat(
            initialValue = -78f,
            targetValue = 78f,
            animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "goldShine"
        )
        LaunchedEffect(Unit) {
            logoAlpha.animateTo(1f, tween(500))
            logoScale.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
            logoRotation.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
        }
        Box(
            Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(120.dp)
                    .graphicsLayer { alpha = haloAlpha }
                    .border(2.dp, Gold.copy(alpha = haloAlpha), CircleShape)
            )
            Text(
                "🍃",
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer { translationY = leafDrift; rotationZ = -25f },
                fontSize = 18.sp
            )
            Text(
                "🍃",
                modifier = Modifier.align(Alignment.BottomEnd).graphicsLayer { translationY = -leafDrift; rotationZ = 25f },
                fontSize = 25.sp
            )
            Image(painter = painterResource(id = R.drawable.slgan_screen_logo), contentDescription = "Sri Lakshmi Ganapathi Ayurveda Nilayam logo", contentScale = ContentScale.Fit, modifier = Modifier.size(108.dp).clip(CircleShape).background(Cream).graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    rotationZ = logoRotation.value
                })
            Box(
                Modifier.offset(x = shineX.dp)
                    .width(18.dp).height(130.dp)
                    .graphicsLayer { rotationZ = 24f; alpha = 0.22f }
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White, Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tr("Sri Lakshmi Ganapathi", LocalAppLanguage.current).uppercase(),
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            tr("Ayurveda Nilayam", LocalAppLanguage.current).uppercase(),
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = appThemedColor(Color(0xFF38251F)),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Image(
            painter = painterResource(id = R.drawable.slgan_divider_gold),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(30.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Estd. 1900 by Vidyaraju Sri Allamraju Perraju Sharma", LocalAppLanguage.current),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            tr("Nature's care, rooted in tradition.", LocalAppLanguage.current),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            color = appThemedColor(Color(0xFF2E7D32)),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            placeholder = { Text(tr("Email address", LocalAppLanguage.current), color = Muted, fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Ink) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Ink
            )
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(1.dp).background(Muted.copy(alpha = 0.45f)))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            placeholder = { Text(tr("Password", LocalAppLanguage.current), color = Muted, fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Ink) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = Ink
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Ink
            )
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(1.dp).background(Muted.copy(alpha = 0.45f)))
        if (isCreatingAccount) {
            Spacer(Modifier.height(12.dp))
            var showConfirmPassword by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                placeholder = { Text(tr("Confirm password", LocalAppLanguage.current), color = Muted, fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Ink) },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide confirm password" else "Show confirm password",
                            tint = Ink
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showConfirmPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Forest
                )
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(1.dp).background(Muted.copy(alpha = 0.45f)))
        }
        if (errorMessage != null) {
            val isSuccessMessage = errorMessage.startsWith("If an account exists for this email")
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSuccessMessage) appThemedColor(Color(0xFFEAF5EC)) else appThemedColor(Color(0xFFFFEEEE)))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    if (isSuccessMessage) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (isSuccessMessage) appThemedColor(Color(0xFF237A3B)) else appThemedColor(Color(0xFFB3261E)),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    errorMessage,
                    color = if (isSuccessMessage) appThemedColor(Color(0xFF237A3B)) else appThemedColor(Color(0xFF9E211B)),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        // The PNG already contains the pill-shaped artwork. Keep the Material
        // button from clipping that artwork at its rounded bounds.
        val signInShape = RectangleShape
        Button(
            onClick = onContinue,
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(76.dp),
            shape = signInShape,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = appThemedColor(Color(0xFF4A3028)),
                disabledContainerColor = Color.Transparent,
                disabledContentColor = appThemedColor(Color(0xFF4A3028)).copy(alpha = 0.6f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = appThemedColor(Color(0xFFB88A36)), strokeWidth = 2.dp)
            } else if (isCreatingAccount) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌿", fontSize = 19.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(tr("Create account", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = appThemedColor(Color(0xFF4A3028)))
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = appThemedColor(Color(0xFFB88A36)))
                    Spacer(Modifier.width(12.dp))
                    Text("🌿", fontSize = 19.sp)
                }
            } else {
                Image(
                    painter = painterResource(R.drawable.slgan_sign_in_button),
                    contentDescription = "Sign in",
                    // Preserve the source artwork's wide aspect ratio; avoid stretching
                    // the lettering and gold border to fill the button's height.
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).offset(y = (-6).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onToggleMode, enabled = !loading, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                Text(if (isCreatingAccount) tr("Already have an account? Sign in", LocalAppLanguage.current) else tr("New? Create account", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
            }
            if (!isCreatingAccount) {
                TextButton(onClick = onForgotPassword, enabled = !loading, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                    Text(tr("Forgot Password?", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(Gold.copy(alpha = 0.55f)))
            Text(tr("OR", LocalAppLanguage.current), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Box(Modifier.weight(1f).height(1.dp).background(Gold.copy(alpha = 0.55f)))
        }
        OutlinedButton(
            onClick = onGoogleSignIn,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
        ) {
            Text("G", color = appThemedColor(Color(0xFF4285F4)), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(12.dp))
            Text(if (loading) tr("Please wait…", LocalAppLanguage.current) else tr("Continue with Google", LocalAppLanguage.current), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("Sign in with your registered email and password.", LocalAppLanguage.current), fontSize = 13.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(24.dp))
        }
    }
}

private fun userFriendlyAuthError(error: Exception?, creatingAccount: Boolean = false): String {
    val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
    return when (code) {
        "ERROR_INVALID_EMAIL" -> "That email address doesn't look right. Check it and try again."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Go back to Sign in instead."
        "ERROR_WEAK_PASSWORD" -> "Your password is too easy to guess. Please choose a stronger password with at least 6 characters."
        "ERROR_USER_NOT_FOUND" -> "We couldn't find an account with this email. Check the address or create a new account."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "The email or password is incorrect. Check both and try again, or use Forgot Password."
        "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact SLGAN support for help."
        "ERROR_TOO_MANY_REQUESTS" -> "There have been too many attempts. Please wait a little while and try again."
        "ERROR_NETWORK_REQUEST_FAILED" -> "We couldn't connect to the internet. Check your connection and try again."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Email and password sign-in is not enabled. Please contact SLGAN support."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with this email using a different sign-in method. Try Google sign-in."
        else -> if (creatingAccount) {
            "We couldn't create your account right now. Please check your details and internet connection, then try again."
        } else {
            "We couldn't sign you in. Check your email and password, then try again. If needed, use Forgot Password."
        }
    }
}

private fun requestEmailOtp(
    email: String,
    action: String,
    code: String = "",
    callback: (Boolean, String, String?) -> Unit
) {
    val endpoint = BuildConfig.EMAIL_OTP_SCRIPT_URL
    if (endpoint.isBlank()) {
        callback(false, "Email OTP service is not configured.", null)
        return
    }
    Thread {
        var ok = false
        var message = "Could not contact the email verification service. Please try again."
        var customToken: String? = null
        var connection: java.net.HttpURLConnection? = null
        try {
            connection = (java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            val payloadObject = org.json.JSONObject().put("action", action).put("email", email)
            if (action == "verify") payloadObject.put("code", code)
            if (action == "passwordReset") {
                val ipConnection = (java.net.URL("https://api.ipify.org").openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val publicIp = try {
                    ipConnection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().trim() }
                } finally {
                    ipConnection.disconnect()
                }
                if (publicIp.isBlank() || publicIp.length > 64) {
                    throw IllegalStateException("Could not determine network IP for password reset. Please try again.")
                }
                payloadObject.put("userIp", publicIp)
            }
            val payload = payloadObject.toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = org.json.JSONObject(body)
            ok = json.optBoolean("ok", false)
            message = json.optString("message").takeIf { it.isNotBlank() }
                ?: if (ok) "Verification request completed." else "Email verification failed."
            customToken = json.optString("customToken").takeIf { it.isNotBlank() && it != "null" }
            if (status !in 200..299) {
                ok = false
                message = "Email verification service returned HTTP $status."
            }
        } catch (error: Exception) {
            android.util.Log.w("SLGAN_EMAIL_OTP", "Email OTP request failed", error)
            message = error.localizedMessage ?: message
        } finally {
            connection?.disconnect()
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            callback(ok, message, customToken)
        }
    }.start()
}

private data class HomeBannerSlide(
    val productName: String,
    val tagline: String,
    val fallbackImageUrl: String,
    val isCompanySlide: Boolean = false,
    val drawableResId: Int? = null
)

@Composable
private fun HomeBannerSlider(
    products: List<Product>,
    onViewProduct: (String) -> Unit
) {
    var selectedDesign by remember { mutableIntStateOf(1) }
    var sliderEnabled by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("appSettings").document("homeBanner")
            .addSnapshotListener { snapshot, _ ->
                selectedDesign = (snapshot?.getLong("activeDesign") ?: 1L).toInt().coerceIn(1, 4)
                sliderEnabled = snapshot?.getBoolean("enabled") ?: true
            }
        onDispose { registration.remove() }
    }
    if (!sliderEnabled) return
    val slides = remember(products) {
        val signatureSlides = products.filter { it.signature }.map { product ->
            HomeBannerSlide(
                product.name,
                product.description.ifBlank { "A signature SLGAN product" },
                product.imageUrl.ifBlank {
                    when (product.name.lowercase().filter { it.isLetterOrDigit() }) {
                        "mahilapriya" -> "https://images.unsplash.com/photo-1547887538-e3a2f32cb1cc?auto=format&fit=crop&w=1200&q=85"
                        "swasaamrutam" -> "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?auto=format&fit=crop&w=1200&q=85"
                        "jeevasatvam" -> "https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?auto=format&fit=crop&w=1200&q=85"
                        "mahaabala" -> "https://images.unsplash.com/photo-1603909223429-69bb7101f420?auto=format&fit=crop&w=1200&q=85"
                        "ganeshamrutham" -> "https://images.unsplash.com/photo-1603909223429-69bb7101f420?auto=format&fit=crop&w=1200&q=85"
                        else -> ""
                    }
                }
            )
        }
        val swaasamruthamSlide = signatureSlides.firstOrNull {
            it.productName.contains("swaasamrutham", ignoreCase = true) ||
                it.productName.contains("swasaamrutam", ignoreCase = true)
        }
        val orderedSignatureSlides = if (swaasamruthamSlide != null) {
            val otherSlides = signatureSlides.filterNot { it == swaasamruthamSlide }
            if (otherSlides.isNotEmpty()) {
                listOf(otherSlides.first(), swaasamruthamSlide) + otherSlides.drop(1)
            } else listOf(swaasamruthamSlide)
        } else signatureSlides
        listOf(
            HomeBannerSlide(
                productName = "Sri Lakshmi Ganapathi Ayurveda Nilayam",
                tagline = "Estd. 1900 by Sri Vidyaraju Allamraju Perraju Sharma",
                fallbackImageUrl = "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=1400&q=85",
                isCompanySlide = true
            )
        ) + orderedSignatureSlides
    }
    var activeSlide by remember { mutableIntStateOf(0) }
    LaunchedEffect(slides.size) {
        if (slides.isNotEmpty()) while (true) {
            delay(6500)
            activeSlide = (activeSlide + 1) % slides.size
        }
    }
    LaunchedEffect(slides.size) {
        if (activeSlide >= slides.size) activeSlide = 0
    }
    val openProduct: (String) -> Unit = { productName ->
        val matchingProduct = products.firstOrNull { it.name.trim().equals(productName, ignoreCase = true) }
        if (matchingProduct != null) {
            onViewProduct(productName)
        } else {
            // Keep the product discoverable if its Firestore record has not been added yet.
            onViewProduct(productName)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
        ) {
            AnimatedContent(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slides.size, activeSlide) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDrag += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                if (kotlin.math.abs(totalDrag) > 35f && slides.isNotEmpty()) {
                                    activeSlide = if (totalDrag < 0f) (activeSlide + 1) % slides.size
                                        else (activeSlide - 1 + slides.size) % slides.size
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = { totalDrag = 0f }
                        )
                    },
                targetState = activeSlide,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
                },
                label = "homeBannerSlide"
            ) { index ->
                val slide = slides[index]
                val productImage = products.firstOrNull { it.name.trim().equals(slide.productName, ignoreCase = true) }?.imageUrl.orEmpty()
                val normalizedBannerName = slide.productName.lowercase().filter { it.isLetterOrDigit() }
                val isSwaasamrutamBanner = normalizedBannerName.contains("swaasamrutam") || normalizedBannerName.contains("swasaamrutam")
                val isMahilaPriyaBanner = normalizedBannerName.contains("mahilapriya")
                val isJeevasatvamBanner = normalizedBannerName.contains("jeevasatvam")
                val isMahabalaBanner = normalizedBannerName.contains("mahabala") || normalizedBannerName.contains("mahaabala")
                val isGaneshamruthamBanner = normalizedBannerName.contains("ganeshamrutham")
                val isArtworkBanner = isSwaasamrutamBanner || isMahilaPriyaBanner || isJeevasatvamBanner || isMahabalaBanner || isGaneshamruthamBanner
                Box(Modifier.fillMaxSize()) {
                    val bannerModel: Any = when {
                        slide.isCompanySlide -> R.drawable.slga_banner
                        isSwaasamrutamBanner -> R.drawable.swaasamrutam_banner
                        isMahilaPriyaBanner -> R.drawable.mahilapriya_banner
                        isJeevasatvamBanner -> R.drawable.jeevasatvam_banner
                        isMahabalaBanner -> R.drawable.mahabala_banner
                        isGaneshamruthamBanner -> R.drawable.ganeshamrutham_banner
                        selectedDesign == 4 -> {
                        when (index % 5) {
                            0 -> "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=1400&q=85"
                            1 -> "https://images.unsplash.com/photo-1515586000433-45406d8e6662?auto=format&fit=crop&w=1400&q=85"
                            2 -> "https://images.unsplash.com/photo-1501004318641-b39e6451bec6?auto=format&fit=crop&w=1400&q=85"
                            3 -> "https://images.unsplash.com/photo-1470058869958-2a77ade41c02?auto=format&fit=crop&w=1400&q=85"
                            else -> "https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=1400&q=85"
                        }
                        }
                        else -> slide.drawableResId ?: productImage.ifBlank { slide.fallbackImageUrl.ifBlank { "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=1400&q=85" } }
                    }
                    val bannerPainter = rememberAsyncImagePainter(model = bannerModel)
                    Image(
                        painter = bannerPainter,
                        contentDescription = when {
                            isSwaasamrutamBanner -> "Swaasamrutam Ayurvedic product banner"
                            isMahilaPriyaBanner -> "Mahila Priya Ayurvedic product banner"
                            isJeevasatvamBanner -> "Jeevasatvam Ayurvedic product banner"
                            isMahabalaBanner -> "Mahabala Ayurvedic product banner"
                            isGaneshamruthamBanner -> "Ganeshamrutham Ayurvedic product banner"
                            selectedDesign == 4 -> "Botanical herbal leaves background"
                            else -> slide.productName
                        },
                        contentScale = if (isArtworkBanner || selectedDesign == 4 || slide.isCompanySlide) ContentScale.Crop else ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().then(
                            if (isArtworkBanner) Modifier.clickable { openProduct(slide.productName) } else Modifier
                        )
                    )
                    if (!isArtworkBanner && bannerPainter.state is coil.compose.AsyncImagePainter.State.Loading) {
                        Box(
                            Modifier.fillMaxSize().background(appThemedColor(Color(0x22FFFFFF))),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = Forest,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    // Keep the trust line anchored near the bottom of every banner slide.
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 22.dp, end = 22.dp, bottom = 2.dp)
                            .offset(y = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "🌿", fontSize = 13.sp)
                        Text(
                            text = tr("TRUSTED SINCE 1990", LocalAppLanguage.current),
                            color = appThemedColor(Color(0xFF2E7D32)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            maxLines = 1
                        )
                    }
                    // Display readable title, tagline, and product action on every slide.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterStart)
                            // Nudge the banner title and View Products link upward slightly.
                            .padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 44.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Keep product name at the top, signature tagline below, and trust line separated beneath it.
                        Text(
                            modifier = Modifier.animateEnterExit(
                                enter = fadeIn(animationSpec = tween(950, delayMillis = 220, easing = FastOutSlowInEasing)) +
                                    slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(950, delayMillis = 220, easing = FastOutSlowInEasing))
                            ),
                            text = if (slide.isCompanySlide) tr("Authentic Ayurvedic Formulations", LocalAppLanguage.current)
                            else localizedProductName(slide.productName, LocalAppLanguage.current),
                            color = appThemedColor(Color(0xFF1F1B16)),
                            fontSize = if (slide.isCompanySlide) 19.sp else 19.sp,
                            lineHeight = if (slide.isCompanySlide) 24.sp else 23.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = if (slide.isCompanySlide) 3 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!slide.isCompanySlide) {
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = tr(slide.tagline, LocalAppLanguage.current),
                                modifier = Modifier.animateEnterExit(
                                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 480, easing = FastOutSlowInEasing)) +
                                        slideInVertically(initialOffsetY = { it / 7 }, animationSpec = tween(1000, delayMillis = 480, easing = FastOutSlowInEasing))
                                ),
                                color = appThemedColor(Color(0xFF5B3428)),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.width(150.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.weight(1f).height(1.dp).background(Gold))
                            Text("❋", color = Gold, fontSize = 14.sp)
                            Box(Modifier.weight(1f).height(1.dp).background(Gold))
                        }
                        Spacer(Modifier.height(2.dp))
                        if (!slide.isCompanySlide) {
                            Row(
                                modifier = Modifier
                                    .offset(x = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Transparent)
                                    .clickable { openProduct(slide.productName) }
                                    .padding(horizontal = 2.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    tr("View Products", LocalAppLanguage.current),
                                    color = Ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Ink,
                                    modifier = Modifier.size(15.dp).align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.indices.forEach { index ->
                val selected = index == activeSlide
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (selected) 22.dp else 7.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.Black else appThemedColor(Color(0xFFE1E5EC)))
                        .clickable { activeSlide = index }
                )
            }
        }
    }
}


@Composable
private fun AdminProductsScreen(
    products: List<Product>,
    loading: Boolean,
    loadError: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchText by remember { mutableStateOf("") }
    var selectedAdminCategory by remember { mutableStateOf("All") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var addingProduct by remember { mutableStateOf(false) }
    var viewingProduct by remember { mutableStateOf<Product?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }
    var visibilityUpdating by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    if (editingProduct != null || addingProduct) {
        AdminProductEditor(
            product = editingProduct,
            onBack = { editingProduct = null; addingProduct = false },
            onSaved = { editingProduct = null; addingProduct = false }
        )
        return
    }
    if (viewingProduct != null) {
        AdminProductDetailsScreen(
            product = viewingProduct!!,
            onBack = { viewingProduct = null },
            onEdit = { product -> editingProduct = product; viewingProduct = null }
        )
        return
    }
    // Build the admin category filters from live product records supplied by Firestore,
    // rather than maintaining a separate hard-coded category list.
    val adminCategories = remember(products) {
        val preferredOrder = listOf("Lehyas", "Ghrutas", "Pills", "Tailas", "Churnas", "Rasayanas", "Rasayogas", "Bhasmas")
        // Always show all standard category filters; do not hide a category merely
        // because old product records use a different spelling.
        val otherCategories = products.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .filterNot { value -> preferredOrder.any { it.equals(value, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
        listOf("All") + preferredOrder + otherCategories
    }
    fun categoryEmoji(category: String): String = when (category.trim().lowercase()) {
        "all" -> "🌿"
        "lehyas" -> "🍯"
        "grutas", "ghritas", "ghrutas" -> "🫙"
        "pills" -> "💊"
        "tailas" -> "🫙"
        "churnas" -> "🌾"
        "rasayanas" -> "🪴"
                                        "rasayogas", "rasayoga" -> "🪷"
        "rasayogas", "rasayoga" -> "🪷"
        "bhasmas" -> "🪨"
        else -> "🌿"
    }
    val filtered = remember(products, searchText, selectedAdminCategory) {
        fun normalizedCategory(value: String): String {
            val key = value.trim().lowercase().replace(Regex("[^a-z]"), "")
            return when {
                key.contains("lehya") || key.contains("lehyam") || key.contains("lehyalu") -> "Lehyas"
                key.contains("grutha") || key.contains("ghrita") || key.contains("ghrutha") || key == "ghee" -> "Ghrutas"
                key.contains("pill") || key == "vati" || key == "vatis" -> "Pills"
                key.contains("taila") || key == "oil" || key == "oils" -> "Tailas"
                key.contains("churna") || key.contains("choorna") -> "Churnas"
                key.contains("rasayana") -> "Rasayanas"
                key.contains("rasayoga") -> "Rasayogas"
                key.contains("bhasma") -> "Bhasmas"
                else -> value.trim()
            }
        }
        products.filter { product ->
            (selectedAdminCategory == "All" || normalizedCategory(product.category).equals(normalizedCategory(selectedAdminCategory), ignoreCase = true)) &&
            (searchText.isBlank() ||
                product.name.contains(searchText, ignoreCase = true) ||
                product.category.contains(searchText, ignoreCase = true) ||
                product.id.contains(searchText, ignoreCase = true))
        }.sortedBy { it.name.lowercase() }
    }
    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to dashboard", tint = AdminInk) }
            Column(Modifier.weight(1f)) {
                Text(tr("Products Catalogue", LocalAppLanguage.current), color = AdminInk, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(tr("Manage, add and organize your products", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchText, onValueChange = { searchText = it },
                modifier = Modifier.weight(1f).height(54.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                placeholder = { Text(tr("Search products…", LocalAppLanguage.current), color = AdminMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AdminInk) },
                trailingIcon = { if (searchText.isNotBlank()) IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Close, "Clear search") } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdminInk, unfocusedBorderColor = AdminInk,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedTextColor = AdminInk, unfocusedTextColor = AdminInk, cursorColor = AdminInk
                )
            )
            Spacer(Modifier.width(8.dp))
            Box {
                Surface(
                    modifier = Modifier.clickable { filterMenuOpen = true },
                    shape = RoundedCornerShape(12.dp), color = AdminLightGray,
                    border = BorderStroke(1.dp, AdminInk)
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FilterList, "Filter categories", tint = AdminInk)
                    }
                }
                DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                    adminCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(if (category == "All") tr("All", LocalAppLanguage.current) + " (" + products.size + ")" else tr(category, LocalAppLanguage.current)) },
                            onClick = { selectedAdminCategory = category; filterMenuOpen = false }
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            adminCategories.forEach { item ->
                FilterChip(
                    selected = selectedAdminCategory == item, onClick = { selectedAdminCategory = item },
                    label = { Text(if (item == "All") tr("All", LocalAppLanguage.current) + " (${products.size})" else tr(item, LocalAppLanguage.current), fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminInk, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = AdminInk)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size}${tr(" products", LocalAppLanguage.current)}", color = AdminInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val categoryProducts = products.filter { selectedAdminCategory == "All" || it.category.equals(selectedAdminCategory, ignoreCase = true) }
            val categoryHasVisibleProducts = categoryProducts.any { !it.isHidden }
            TextButton(enabled = !visibilityUpdating && categoryProducts.isNotEmpty(), onClick = {
                visibilityUpdating = true
                val hide = categoryHasVisibleProducts
                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                categoryProducts.filter { it.isHidden != hide }.forEach { item -> batch.update(db.collection("products").document(item.id), "product_hidden", hide) }
                batch.commit().addOnSuccessListener {
                    Toast.makeText(context, if (hide) "All products hidden" else "All products shown", Toast.LENGTH_SHORT).show()
                    visibilityUpdating = false
                }.addOnFailureListener { error ->
                    Toast.makeText(context, error.localizedMessage ?: "Could not update product visibility", Toast.LENGTH_LONG).show()
                    visibilityUpdating = false
                }
            }, colors = ButtonDefaults.textButtonColors(contentColor = AdminInk)) {
                if (visibilityUpdating) CircularProgressIndicator(Modifier.size(14.dp), color = AdminInk, strokeWidth = 2.dp)
                else Text(if (categoryHasVisibleProducts) tr("Hide all", LocalAppLanguage.current) else tr("Show all", LocalAppLanguage.current), fontSize = 11.sp)
            }
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 10.dp), color = AdminInk, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Product", LocalAppLanguage.current), Modifier.weight(2.5f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(tr("Price", LocalAppLanguage.current), Modifier.weight(1f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(tr("Stock", LocalAppLanguage.current), Modifier.weight(0.85f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(tr("Show/Hide", LocalAppLanguage.current), Modifier.weight(1f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(tr("Actions", LocalAppLanguage.current), Modifier.weight(0.9f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
        when {
            loading -> AppLoader("Loading products…", compact = true, messageColor = AdminInk)
            loadError != null && products.isEmpty() -> Text(tr("Could not load products", LocalAppLanguage.current) + ": $loadError", Modifier.padding(18.dp), color = AdminInk)
            filtered.isEmpty() -> EmptyState("📦", "No products found", if (searchText.isBlank()) "Tap + to create your first product." else "Try another product name or category.")
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(filtered, key = { it.id }) { product ->
                    Surface(Modifier.fillMaxWidth(), color = if (product.isHidden) Color(0xFFF7F7F7) else Color.White, border = BorderStroke(0.5.dp, AdminInk)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(AdminLightGray), contentAlignment = Alignment.Center) {
                                    if (product.imageUrl.isNotBlank()) AsyncImage(model = product.imageUrl, contentDescription = product.name, modifier = Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                                    else Text(product.emoji, fontSize = 21.sp)
                                }
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, color = AdminInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(product.subtitle.ifBlank { product.category }, color = AdminMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (product.isHidden) Text(tr("Hidden", LocalAppLanguage.current), color = AdminMuted, fontSize = 9.sp)
                                }
                            }
                            Text(if (product.price > 0) "₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price) else "—",
                                Modifier.weight(1f), color = AdminInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Box(Modifier.weight(0.85f), contentAlignment = Alignment.Center) {
                                Switch(
                                    checked = product.productStock == 1,
                                    onCheckedChange = { inStock ->
                                        FirebaseFirestore.getInstance().collection("products").document(product.id)
                                            .update("product_stock", if (inStock) 1 else 0)
                                            .addOnSuccessListener { Toast.makeText(context, if (inStock) "Product in stock" else "Product out of stock", Toast.LENGTH_SHORT).show() }
                                            .addOnFailureListener { error -> Toast.makeText(context, error.localizedMessage ?: "Could not update product stock", Toast.LENGTH_LONG).show() }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF2E7D32),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFC62828)
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Switch(
                                    checked = !product.isHidden,
                                    onCheckedChange = { visible ->
                                        FirebaseFirestore.getInstance().collection("products").document(product.id)
                                            .update("product_hidden", !visible)
                                            .addOnSuccessListener { Toast.makeText(context, if (visible) "Product shown" else "Product hidden", Toast.LENGTH_SHORT).show() }
                                            .addOnFailureListener { error -> Toast.makeText(context, error.localizedMessage ?: "Could not update product visibility", Toast.LENGTH_LONG).show() }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2E7D32), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFC62828)),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                            Row(Modifier.weight(0.9f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingProduct = product }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Edit, "Edit ${product.name}", tint = AdminInk, modifier = Modifier.size(16.dp))
                                }
                                var menuOpen by remember(product.id) { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.MoreVert, "More actions", tint = AdminInk, modifier = Modifier.size(17.dp)) }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(text = { Text(tr("View details", LocalAppLanguage.current)) }, onClick = { menuOpen = false; viewingProduct = product })
                                        DropdownMenuItem(text = { Text(if (product.isHidden) tr("Show product", LocalAppLanguage.current) else tr("Hide product", LocalAppLanguage.current)) }, onClick = {
                                            menuOpen = false
                                            FirebaseFirestore.getInstance().collection("products").document(product.id).update("product_hidden", !product.isHidden)
                                                .addOnSuccessListener { Toast.makeText(context, if (product.isHidden) "Product shown" else "Product hidden", Toast.LENGTH_SHORT).show() }
                                                .addOnFailureListener { error -> Toast.makeText(context, error.localizedMessage ?: "Could not update visibility", Toast.LENGTH_LONG).show() }
                                        })
                                        DropdownMenuItem(text = { Text(tr("Delete product", LocalAppLanguage.current)) }, onClick = { menuOpen = false; deletingProduct = product })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { addingProduct = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).height(50.dp),
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AdminInk, contentColor = Color.White)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text(tr("Add New Product", LocalAppLanguage.current), fontWeight = FontWeight.Bold)
        }
    }
    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            containerColor = AdminSurface,
            title = { Text(tr("Delete product?", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${product.name}\" from the catalogue? This cannot be undone.", color = AdminInk) },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseFirestore.getInstance().collection("products").document(product.id).delete()
                        .addOnSuccessListener { Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show() }
                    deletingProduct = null
                }) { Text(tr("Delete", LocalAppLanguage.current), color = AdminInk) }
            },
            dismissButton = { TextButton(onClick = { deletingProduct = null }) { Text(tr("Cancel", LocalAppLanguage.current), color = AdminMuted) } }
        )
    }
}


@Composable
private fun AdminProductDetailsScreen(
    product: Product,
    onBack: () -> Unit,
    onEdit: (Product) -> Unit
) {
    var fields by remember(product.id) { mutableStateOf<Map<String, Any>?>(null) }
    var loadingDetails by remember(product.id) { mutableStateOf(true) }
    var detailsError by remember(product.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(product.id) {
        FirebaseFirestore.getInstance().collection("products").document(product.id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    fields = document.data ?: emptyMap()
                    detailsError = null
                } else {
                    detailsError = "Product record was not found in Firestore."
                }
                loadingDetails = false
            }
            .addOnFailureListener { error ->
                detailsError = error.localizedMessage ?: "Could not load product details."
                loadingDetails = false
            }
    }

    val data = fields.orEmpty()
    val hiddenKeys = setOf("image_url", "imageUrl", "product_image", "password", "passwordHash")
    fun displayLabel(key: String): String = key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .trim()
        .split(Regex("\\s+"))
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to products", tint = AdminInk) }
            Column(Modifier.weight(1f)) {
                Text(tr("View Product", LocalAppLanguage.current), color = AdminInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(tr("Complete catalogue information", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp)
            }
            TextButton(onClick = { onEdit(product) }) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = AdminInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(tr("Edit", LocalAppLanguage.current), color = AdminInk)
            }
        }
        if (loadingDetails) {
            AppLoader("Loading product details…", compact = true)
        } else if (detailsError != null && data.isEmpty()) {
            Text(detailsError.orEmpty(), Modifier.padding(20.dp), color = AdminInk)
            TextButton(onClick = onBack) { Text(tr("Back to products", LocalAppLanguage.current), color = AdminInk) }
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val image = listOf("product_image_url", "image_url", "imageUrl", "image", "productImage").firstNotNullOfOrNull { key -> data[key]?.toString()?.takeIf { it.startsWith("http") } }.orEmpty()
                        if (image.isNotBlank()) {
                            AsyncImage(model = image, contentDescription = product.name, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F2F4)), contentAlignment = Alignment.Center) {
                                Text(product.emoji, fontSize = 48.sp)
                            }
                        }
                        Text(localizedProductName(product.name, LocalAppLanguage.current), color = AdminInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (product.price > 0) "₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price) else "Price not set",
                            color = AdminInk, fontSize = 21.sp, fontWeight = FontWeight.Bold
                        )
                        Text(if (product.productStock == 1) tr("IN STOCK", LocalAppLanguage.current) else tr("OUT OF STOCK", LocalAppLanguage.current), color = if (product.productStock == 1) AdminInk else AdminInk, fontWeight = FontWeight.Bold)
                    }
                }
                Text(tr("PRODUCT INFORMATION", LocalAppLanguage.current), color = AdminInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                if (data.isEmpty()) {
                    Text(tr("No additional fields are saved for this product.", LocalAppLanguage.current), color = AdminMuted)
                } else {
                    data.toSortedMap().filterKeys { key -> key !in hiddenKeys && !key.equals("image", true) && !key.equals("product_image", true) }.forEach { (key, value) ->
                        val valueText = when (value) {
                            is Map<*, *> -> value.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                            is List<*> -> value.joinToString(", ")
                            else -> value.toString()
                        }
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AdminSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(displayLabel(key), color = AdminInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(valueText.ifBlank { "—" }, color = AdminInk, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
                if (detailsError != null) Text(tr("Latest details could not be refreshed", LocalAppLanguage.current) + ": ${detailsError}", color = AdminMuted, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminProductEditor(
    product: Product?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val isNew = product == null
    var name by remember(product?.id) { mutableStateOf(product?.name.orEmpty()) }
    var category by remember(product?.id) { mutableStateOf(product?.category ?: "Lehyas") }
    // Keep the category selector usable immediately, even before Firestore responds
    // or when the categories collection is empty/unavailable.
    val defaultCategoryOptions = listOf(
        "Lehyas" to "🍯",
        "Ghrutas" to "🫙",
        "Pills" to "💊",
        "Tailas" to "🪔",
        "Churnas" to "🌿",
        "Rasayanas" to "🪴",
        "Rasayogas" to "🪷",
        "Bhasmas" to "🪨"
    )
    var categoryOptions by remember { mutableStateOf(defaultCategoryOptions) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var savingCategory by remember { mutableStateOf(false) }
    var categorySeedStarted by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val registration = db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                // Retain built-in options when Firestore cannot be reached.
                if (categoryOptions.isEmpty()) categoryOptions = defaultCategoryOptions
            } else {
                if (snapshot.isEmpty && !categorySeedStarted) {
                    categorySeedStarted = true
                    val defaults = listOf(
                        Triple("lehyas", "Lehyas", "🍯"),
                        Triple("grutas", "Ghrutas", "🫙"),
                        Triple("pills", "Pills", "💊"),
                        Triple("tailas", "Tailas", "🪔"),
                        Triple("churnas", "Churnas", "🌿"),
                        Triple("rasayanas", "Rasayanas", "🪴"),
                        Triple("rasayogas", "Rasayogas", "🪷"),
                        Triple("bhasmas", "Bhasmas", "🪨")
                    )
                    defaults.forEach { (id, label, emoji) ->
                        db.collection("categories").document(id).set(
                            mapOf("category_name" to label, "name" to label, "emoji" to emoji)
                        )
                    }
                } else {
                    val firestoreOptions = snapshot.documents.mapNotNull { doc ->
                        val label = (doc.getString("category_name") ?: doc.getString("name") ?: doc.id).trim()
                        if (label.isBlank()) null else label to doc.getString("emoji").orEmpty()
                    }
                    // Always retain the built-in category choices, even if Firestore
                    // contains an incomplete/legacy categories collection.
                    val mergedOptions = (defaultCategoryOptions + firestoreOptions)
                        .map { (label, emoji) ->
                            val key = label.lowercase().replace(Regex("[^a-z]"), "")
                            val canonical = when (key) {
                                "lehya", "lehyam", "lehyas", "lehyalu" -> "Lehyas"
                                "grutha", "gruthas", "ghrita", "ghritas", "ghrutha", "ghrutas" -> "Ghrutas"
                                "pill", "pills", "vati", "vatis" -> "Pills"
                                "taila", "tailam", "tailas" -> "Tailas"
                                "churna", "churnas", "choorna", "choornas" -> "Churnas"
                                "rasayana", "rasayanas" -> "Rasayanas"
                                "rasayoga", "rasayogas" -> "Rasayogas"
                                "bhasma", "bhasmas" -> "Bhasmas"
                                else -> label
                            }
                            val fallbackEmoji = defaultCategoryOptions.firstOrNull { it.first == canonical }?.second.orEmpty()
                            canonical to emoji.ifBlank { fallbackEmoji }
                        }
                        .distinctBy { it.first.lowercase() }
                    categoryOptions = if (category.isNotBlank() && mergedOptions.none { it.first.equals(category, ignoreCase = true) }) {
                        mergedOptions + (category.trim() to "")
                    } else mergedOptions
                }
            }
        }
        onDispose { registration.remove() }
    }
    var price by remember(product?.id) { mutableStateOf(if (product == null || product.price == 0.0) "" else product.price.toString()) }
    var weight by remember(product?.id) { mutableStateOf(product?.subtitle?.filter { it.isDigit() || it == '.' }.orEmpty()) }
    var units by remember(product?.id) { mutableStateOf(product?.subtitle?.filter { it.isLetter() }.orEmpty()) }
    var unitOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    unitOptions = snapshot.documents.mapNotNull { it.getString("product_units")?.trim() }
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase() }
                        .sortedBy { it.lowercase() }
                    if (units.isNotBlank() && unitOptions.none { it.equals(units, ignoreCase = true) }) {
                        unitOptions = (unitOptions + units.trim()).distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                    }
                }
            }
        onDispose { registration.remove() }
    }
    var quantity by remember(product?.id) { mutableStateOf(product?.quantity?.ifBlank { "1" } ?: "1") }
    var imageUrl by remember(product?.id) { mutableStateOf(product?.imageUrl.orEmpty()) }
    var message by remember { mutableStateOf<String?>(null) }
    var uploadingImage by remember(product?.id) { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val cleanName = name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')
            if (cleanName.isBlank()) {
                Toast.makeText(context, "Enter the product name before uploading an image.", Toast.LENGTH_LONG).show()
            } else {
                uploadingImage = true
                message = "Uploading product image…"
                val imageRef = FirebaseStorage.getInstance().reference.child("products/${cleanName}_product")
                imageRef.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw (task.exception ?: Exception("Image upload failed"))
                        imageRef.downloadUrl
                    }
                    .addOnSuccessListener { downloadUri ->
                        imageUrl = downloadUri.toString()
                        uploadingImage = false
                        message = "Image uploaded. Save the product to publish it."
                    }
                    .addOnFailureListener { error ->
                        uploadingImage = false
                        message = "Image upload failed: ${error.localizedMessage ?: "Check Firebase Storage setup and rules."}"
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
    var description by remember(product?.id) { mutableStateOf(product?.description.orEmpty()) }
    var usage by remember(product?.id) { mutableStateOf(product?.usage.orEmpty()) }
    var cures by remember(product?.id) { mutableStateOf(product?.cures.orEmpty()) }
    var ingredients by remember(product?.id) { mutableStateOf(product?.majorIngredients.orEmpty()) }
    var signature by remember(product?.id) { mutableStateOf(product?.signature ?: false) }
    var productStock by remember(product?.id) { mutableStateOf(product?.productStock ?: 1) }
    var saving by remember { mutableStateOf(false) }
    var showValidationErrors by remember(product?.id) { mutableStateOf(false) }
    val invalidName = showValidationErrors && name.isBlank()
    val invalidCategory = showValidationErrors && category.isBlank()
    val invalidPrice = showValidationErrors && price.toDoubleOrNull() == null
    val invalidWeight = showValidationErrors && (weight.toDoubleOrNull() == null || (weight.toDoubleOrNull() ?: 0.0) <= 0)
    val invalidUnits = showValidationErrors && units.isBlank()
    val invalidQuantity = showValidationErrors && (quantity.toIntOrNull() == null || (quantity.toIntOrNull() ?: 0) <= 0)
    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !saving) { Icon(Icons.Default.ArrowBack, "Back", tint = AdminInk) }
            Text(if (isNew) tr("Add product", LocalAppLanguage.current) else tr("Edit product", LocalAppLanguage.current), color = AdminInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(tr("Product Information", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(name, { name = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Product name *", LocalAppLanguage.current)) }, isError = invalidName, singleLine = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).menuAnchor(),
                            enabled = true,
                            label = { Text(tr("Category *", LocalAppLanguage.current)) },
                            isError = invalidCategory,
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp).background(AdminBackground)
                        ) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(listOf(option.second, option.first).filter { it.isNotBlank() }.joinToString(" "), color = AdminInk) },
                                    onClick = { category = option.first; message = null; categoryMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = { newCategoryName = ""; showAddCategoryDialog = true }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add category", tint = AdminInk)
                }
            }
            OutlinedTextField(price, { price = it.filter { ch -> ch.isDigit() || ch == '.' }; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Price (₹) *", LocalAppLanguage.current)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = invalidPrice, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' }; message = null }, modifier = Modifier.weight(1f), label = { Text(tr("Weight *", LocalAppLanguage.current)) }, isError = invalidWeight, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { unitMenuExpanded = true },
                        label = { Text(tr("Units *", LocalAppLanguage.current)) },
                        isError = invalidUnits,
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { unitMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select weight unit")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 280.dp).background(AdminBackground)
                    ) {
                        if (unitOptions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(tr("No units found in product database", LocalAppLanguage.current), color = AdminMuted) },
                                onClick = { unitMenuExpanded = false },
                                enabled = false
                            )
                        } else {
                            unitOptions.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, color = AdminInk) },
                                    onClick = { units = unit; message = null; unitMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    quantity,
                    { quantity = it; message = null },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr("Qty *", LocalAppLanguage.current)) },
                    isError = invalidQuantity,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("Product availability", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (productStock == 1) tr("IN STOCK", LocalAppLanguage.current) else tr("OUT OF STOCK", LocalAppLanguage.current), color = if (productStock == 1) AdminInk else AdminInk, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = productStock == 1,
                        onCheckedChange = { productStock = if (it) 1 else 0 },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32),
                            checkedBorderColor = Color(0xFF2E7D32),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFC62828),
                            uncheckedBorderColor = Color(0xFFC62828)
                        )
                    )
                }
            }
            Text(tr("Product image", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.SemiBold)
            if (imageUrl.isNotBlank()) {
                AsyncImage(model = imageUrl, contentDescription = "Product image preview", modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F2F4)), contentScale = ContentScale.Fit)
            }
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                enabled = !uploadingImage,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AdminInk,
                    disabledContentColor = AdminInk.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, AdminInk)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = AdminInk)
                Spacer(Modifier.width(8.dp))
                Text(if (uploadingImage) "Uploading image…" else "Upload product image", color = AdminInk)
            }
            Text(tr("The image is stored online using a filename based on the product name and updates in the app after saving.", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp)
            OutlinedTextField(imageUrl, { imageUrl = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Image URL (optional)", LocalAppLanguage.current)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), minLines = 1)
            OutlinedTextField(description, { description = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Description", LocalAppLanguage.current)) }, minLines = 3)
            OutlinedTextField(usage, { usage = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Usage / directions", LocalAppLanguage.current)) }, minLines = 2)
            OutlinedTextField(cures, { cures = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Cures / indications", LocalAppLanguage.current)) }, minLines = 2)
            OutlinedTextField(ingredients, { ingredients = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Major ingredients", LocalAppLanguage.current)) }, minLines = 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Is signature product", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.SemiBold)
                    Text(if (signature) "Yes" else "No", color = if (signature) AdminInk else AdminMuted, fontSize = 12.sp)
                }
                Switch(
                    checked = signature,
                    onCheckedChange = { signature = it; message = null },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2E7D32),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFC62828)
                    )
                )
            }
            if (message != null) Text(message!!, color = if (message!!.startsWith("Saved")) AdminInk else AdminInk, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { if (!savingCategory) showAddCategoryDialog = false },
            containerColor = AdminSurface,
            title = { Text(tr("Add category", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Enter category", LocalAppLanguage.current)) },
                    singleLine = true,
                    enabled = !savingCategory
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newCategoryName.isNotBlank() && !savingCategory,
                    onClick = {
                        val label = newCategoryName.trim()
                        val categoryId = label.lowercase(Locale.ROOT)
                            .replace(Regex("[^a-z0-9]+"), "_").trim('_')
                        if (label.isNotBlank() && categoryId.isNotBlank()) {
                            savingCategory = true
                            FirebaseFirestore.getInstance().collection("categories")
                                .document(categoryId)
                                .set(mapOf("category_name" to label, "name" to label, "emoji" to "🌿"), com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    category = label
                                    message = null
                                    savingCategory = false
                                    showAddCategoryDialog = false
                                    Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { error ->
                                    savingCategory = false
                                    Toast.makeText(context, error.localizedMessage ?: "Could not add category", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                ) { Text(if (savingCategory) tr("Saving…", LocalAppLanguage.current) else tr("Add", LocalAppLanguage.current), color = AdminInk) }
            },
            dismissButton = {
                TextButton(onClick = { if (!savingCategory) showAddCategoryDialog = false }) {
                    Text(tr("Cancel", LocalAppLanguage.current), color = AdminMuted)
                }
            }
        )
    }
        Button(
            onClick = {
                if (uploadingImage) {
                    message = "Wait for the image upload to finish."
                } else if (name.isBlank() || category.isBlank() || price.toDoubleOrNull() == null || weight.toDoubleOrNull() == null || weight.toDoubleOrNull()!! <= 0 || units.isBlank() || quantity.toIntOrNull() == null || quantity.toIntOrNull()!! <= 0) {
                    showValidationErrors = true
                    message = null
                } else {
                    saving = true
                    message = null
                    val fields = hashMapOf<String, Any>(
                        "product_name" to name.trim(),
                        "product_category" to category.trim(),
                        "product_price" to (price.toDoubleOrNull() ?: 0.0),
                        "product_weight" to weight.trim(),
                        "product_units" to units.trim(),
                        "product_quantity" to quantity.trim(),
                        "product_stock" to productStock,
                        "product_image" to imageUrl.trim(),
                        "image_url" to imageUrl.trim(),
                        "imageUrl" to imageUrl.trim(),
                        "product_image_url" to imageUrl.trim(),
                        "product_description" to description.trim(),
                        "product_usage" to usage.trim(),
                        "product_cures" to cures.trim(),
                        "product_major_ingredients" to ingredients.trim(),
                        "signature" to signature,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    val db = FirebaseFirestore.getInstance().collection("products")
                    val task = if (product == null) {
                        fields["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                        db.document().set(fields)
                    } else db.document(product.id).set(fields, com.google.firebase.firestore.SetOptions.merge())
                    task.addOnSuccessListener {
                        saving = false
                        Toast.makeText(context, "Product saved successfully.", Toast.LENGTH_SHORT).show()
                        message = "Saved successfully."
                        onSaved()
                    }.addOnFailureListener { error ->
                        saving = false
                        val errorMessage = error.localizedMessage ?: "Check Firestore permissions."
                        Toast.makeText(context, "Failed to save product: $errorMessage", Toast.LENGTH_SHORT).show()
                        message = "Save failed: $errorMessage"
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminInk, contentColor = Color.White)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.Save, null, tint = Color.White)
            Spacer(Modifier.width(8.dp)); Text(if (saving) tr("Saving…", LocalAppLanguage.current) else tr("Save product", LocalAppLanguage.current), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun AdminDashboardScreen(
    email: String,
    productCount: Int,
    productsLoading: Boolean,
    onProductsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onDiseasesClick: () -> Unit,
    onBannersClick: () -> Unit,
    onThemesClick: () -> Unit,
    onSignOut: () -> Unit
) {
    var orderCount by remember { mutableIntStateOf(0) }
    var diseaseCount by remember { mutableIntStateOf(0) }
    var customerCount by remember { mutableIntStateOf(0) }
    var ordersLoading by remember { mutableStateOf(true) }
    var diseasesLoading by remember { mutableStateOf(true) }
    var customersLoading by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val ordersListener = db.collection("orders").addSnapshotListener { snapshot, error ->
            if (error == null) orderCount = snapshot?.size() ?: 0
            ordersLoading = false
        }
        val diseasesListener = db.collection("diseases").addSnapshotListener { snapshot, error ->
            if (error == null) diseaseCount = snapshot?.size() ?: 0
            diseasesLoading = false
        }
        val customersListener = db.collection("customers").addSnapshotListener { snapshot, error ->
            if (error == null) customerCount = snapshot?.documents?.count { it.getBoolean("is_admin") != true } ?: 0
            customersLoading = false
        }
        onDispose {
            ordersListener.remove()
            diseasesListener.remove()
            customersListener.remove()
        }
    }

    val currentLanguage = LocalAppLanguage.current
    val metrics = remember(productCount, orderCount, diseaseCount, customerCount, currentLanguage) {
        listOf(
            Triple(tr("Products", currentLanguage), productCount, Icons.Default.Inventory2),
            Triple(tr("Orders", currentLanguage), orderCount, Icons.Default.ReceiptLong),
            Triple(tr("Diseases", currentLanguage), diseaseCount, Icons.Default.MedicalServices),
            Triple(tr("Customers", currentLanguage), customerCount, Icons.Default.PeopleAlt)
        )
    }
    val metricLoading = listOf(productsLoading, ordersLoading, diseasesLoading, customersLoading)
    val maxMetric = remember(metrics) { metrics.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1 }

    Column(
        Modifier.fillMaxSize()
            .background(AdminBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Compact admin identity header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr("SLGAN ADMIN", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(3.dp))
                Text(tr("Dashboard", LocalAppLanguage.current), color = AdminInk, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text(tr("Welcome back", LocalAppLanguage.current), color = AdminMuted, fontSize = 14.sp)
                Text(email.ifBlank { "Administrator" }, color = AdminInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = AdminInk)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Analytics panel uses the live totals already supplied by Firestore listeners.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Live Overview", LocalAppLanguage.current), color = AdminInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Current records across your store", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp)
                    }
                    Surface(shape = RoundedCornerShape(9.dp), color = AdminLightGray) {
                        Text(tr("LIVE", LocalAppLanguage.current), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = AdminInk, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth().height(104.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    metrics.forEachIndexed { index, metric ->
                        val fraction = (metric.second.toFloat() / maxMetric.toFloat()).coerceIn(0.08f, 1f)
                        val barColor = listOf(Color(0xFF2E9D62), Color(0xFFE05252), Color(0xFF2E9D62), Color(0xFFE05252))[index]
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            if (metricLoading[index]) {
                                CircularProgressIndicator(Modifier.size(14.dp), color = AdminInk, strokeWidth = 2.dp)
                            } else {
                                Text(metric.second.toString(), color = AdminInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier.width(27.dp).weight(1f).clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)).background(Color(0xFFF0F1F2)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight(fraction).clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)).background(barColor)
                                )
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(metric.first, color = AdminMuted, fontSize = 10.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(tr("OVERVIEW", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))

        // Compact metric tiles
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.take(2).forEachIndexed { index, metric ->
                val tileTint = listOf(AdminLightGray, AdminLightGray)[index]
                val iconTint = listOf(AdminInk, AdminInk)[index]
                val action = if (index == 0) onProductsClick else onOrdersClick
                Card(
                    Modifier.weight(1f).clickable(onClick = action),
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tileTint), contentAlignment = Alignment.Center) {
                            Icon(metric.third, null, tint = iconTint, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.height(9.dp))
                        if (metricLoading[index]) {
                            Box(Modifier.width(48.dp).height(25.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFEDEFF1)))
                        } else {
                            Text(metric.second.toString(), color = AdminInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(metric.first, color = AdminInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.drop(2).forEachIndexed { index, metric ->
                val tileTint = listOf(AdminLightGray, AdminLightGray)[index]
                val iconTint = listOf(AdminInk, AdminInk)[index]
                val action = if (index == 0) onDiseasesClick else onCustomersClick
                Card(
                    Modifier.weight(1f).clickable(onClick = action),
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tileTint), contentAlignment = Alignment.Center) {
                            Icon(metric.third, null, tint = iconTint, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.height(9.dp))
                        if (metricLoading[index + 2]) {
                            Box(Modifier.width(48.dp).height(25.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFEDEFF1)))
                        } else {
                            Text(metric.second.toString(), color = AdminInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(metric.first, color = AdminInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(tr("MANAGEMENT", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))

        @Composable
        fun ManagementRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
            Card(
                Modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(AdminLightGray), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = AdminInk, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr(title, LocalAppLanguage.current), color = AdminInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(tr(subtitle, LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = AdminMuted)
                }
            }
            Spacer(Modifier.height(9.dp))
        }

        ManagementRow(Icons.Default.Inventory2, "Products Catalogue", "Add, edit and manage products", onProductsClick)
        ManagementRow(Icons.Default.ReceiptLong, "Orders Management", "View and manage customer orders", onOrdersClick)
        ManagementRow(Icons.Default.MedicalServices, "Diseases Management", "Manage disease records", onDiseasesClick)
        ManagementRow(Icons.Default.PeopleAlt, "Customers Management", "View registered customers", onCustomersClick)
        ManagementRow(Icons.Default.Palette, "App Themes", "Apply Default or Theme 1 across the app", onThemesClick)
        ManagementRow(Icons.Default.ViewCarousel, "Sliders & Banners", "Manage home page slider images and visibility", onBannersClick)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ThemeManagementScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var selectedTheme by remember { mutableIntStateOf(3) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val listener = db.collection("appSettings").document("appTheme").addSnapshotListener { snap, error ->
            if (error == null) selectedTheme = (snap?.getLong("activeTheme") ?: 3L).toInt().coerceIn(1, 4)
        }
        onDispose { listener.remove() }
    }
    val themes = listOf(
        Triple(1, "Classic Maroon", "Theme 1 · Traditional maroon, cream and gold"),
        Triple(2, "Botanical Green", "Theme 2 · Natural forest green with warm cream"),
        Triple(3, "Dark Mode", "Theme 3 · Premium charcoal with warm gold accents"),
        Triple(4, "Green Mode", "Theme 4 · Fresh botanical green with light backgrounds")
    )
    Column(Modifier.fillMaxSize().background(AdminBackground).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = AdminInk) }
            Column(Modifier.weight(1f)) {
                Text(tr("SLGAN ADMIN", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                Text(tr("App Themes", LocalAppLanguage.current), color = AdminInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Text(tr("Choose the appearance for the customer app. The admin screens keep their separate neutral theme.", LocalAppLanguage.current), color = AdminMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        themes.forEach { (id, name, description) ->
            val dark = id == 3
            val bg = when (id) {
                1 -> Color(0xFFFFF7E8)
                2 -> Color(0xFFFFF9E9)
                3 -> Color(0xFF0B1118)
                else -> Color(0xFFF1FAF0)
            }
            val surface = when (id) { 3 -> Color(0xFF142130); else -> Color.White }
            val accent = when (id) {
                1 -> Color(0xFF762A24)
                2 -> Color(0xFF214B35)
                3 -> Color(0xFFF4C96B)
                else -> Color(0xFF075B2A)
            }
            val mainText = when (id) {
                1 -> Color(0xFF38251F)
                2 -> Color(0xFF20372A)
                3 -> Color(0xFFF4F1E8)
                else -> Color(0xFF12351E)
            }
            val secondary = when (id) {
                1 -> Color(0xFF806D60)
                2 -> Color(0xFF687663)
                3 -> Color(0xFFAAB4C0)
                else -> Color(0xFF607568)
            }
            Card(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(if (selectedTheme == id) 2.dp else 1.dp, if (selectedTheme == id) accent else Color(0xFFB5B5B5)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = AdminInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(description, color = AdminMuted, fontSize = 11.sp)
                        }
                        if (selectedTheme == id) Icon(Icons.Default.CheckCircle, "Selected", tint = AdminInk)
                    }
                    Spacer(Modifier.height(11.dp))
                    // Compact live palette preview; layout mirrors the existing home page without changing it.
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Spa, null, tint = bg, modifier = Modifier.size(19.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Sri Lakshmi Ganapathi", color = mainText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Ayurveda Nilayam", color = secondary, fontSize = 9.sp)
                            }
                            Surface(shape = RoundedCornerShape(7.dp), color = accent) {
                                Text("3", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = bg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = surface, border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("Search products…", LocalAppLanguage.current), color = secondary, fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = surface) {
                            Row(Modifier.fillMaxWidth().padding(9.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf("All", "Lehyas", "Grutas", "Pills").forEachIndexed { index, label ->
                                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = if (index == 0) accent else bg) {
                                        Text(label, Modifier.padding(vertical = 8.dp), color = if (index == 0) bg else mainText, fontSize = 9.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Mahabala", "Sleshamrutham", "Jeevasatvam").forEach { product ->
                                Surface(Modifier.weight(1f), shape = RoundedCornerShape(9.dp), color = surface, border = BorderStroke(0.5.dp, accent.copy(alpha = 0.25f))) {
                                    Column(Modifier.padding(8.dp)) {
                                        Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp)).background(when (id) { 1 -> Color(0xFFF5E5C8); 2 -> Color(0xFFE7EBD4); 3 -> Color(0xFF26394A); else -> Color(0xFFE2F2E2) }))
                                        Spacer(Modifier.height(5.dp))
                                        Text(product, color = mainText, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("₹350", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(bg, surface, accent, mainText, secondary).forEach { swatch ->
                            Box(Modifier.size(23.dp).clip(CircleShape).background(swatch).border(1.dp, Color(0xFFB5B5B5), CircleShape))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                selectedTheme = id
                                saving = true
                                message = null
                                db.collection("appSettings").document("appTheme")
                                    .set(mapOf("activeTheme" to id, "lightThemeEnabled" to true, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener {
                                        applyAppPalette(id)
                                        saving = false
                                        message = "$name applied to the customer app."
                                    }
                                    .addOnFailureListener { e ->
                                        saving = false
                                        message = "Could not apply theme: ${e.localizedMessage ?: "Check Firestore permissions."}"
                                    }
                            },
                            enabled = !saving,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = if (dark) Color(0xFF0B1118) else Color.White)
                        ) {
                            if (saving && selectedTheme == id) CircularProgressIndicator(Modifier.size(16.dp), color = if (dark) Color(0xFF0B1118) else Color.White, strokeWidth = 2.dp)
                            else Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Apply $name", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        message?.let { Text(it, color = AdminInk, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
        Text(tr("Theme changes affect colors and backgrounds only. Home page structure, product data, images and navigation remain unchanged.", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp)
    }
}
@Composable
private fun BannerManagementScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var activeDesign by remember { mutableIntStateOf(1) }
    var sliderEnabled by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val registration = db.collection("appSettings").document("homeBanner").addSnapshotListener { snap, _ ->
            activeDesign = (snap?.getLong("activeDesign") ?: 1L).toInt().coerceIn(1, 4)
            sliderEnabled = snap?.getBoolean("enabled") ?: true
        }
        onDispose { registration.remove() }
    }
    Column(Modifier.fillMaxSize().background(AdminBackground).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp).clickable { onBack() },
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminInk, modifier = Modifier.size(24.dp))
                }
            }
            Column { Text(tr("HOME PAGE SLIDERS", LocalAppLanguage.current), color = AdminInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold); Text(tr("Choose Active Design", LocalAppLanguage.current), color = AdminInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AdminSurface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Show Slider On Home", LocalAppLanguage.current), color = AdminInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (sliderEnabled) "Slider is visible on the home page" else "Slider is hidden from the home page", color = AdminMuted, fontSize = 12.sp)
                }
                Switch(
                    checked = sliderEnabled,
                    onCheckedChange = { enabled ->
                    sliderEnabled = enabled
                    message = null
                    db.collection("appSettings").document("homeBanner")
                        .set(mapOf("enabled" to enabled, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                        .addOnFailureListener { e ->
                            sliderEnabled = !enabled
                            message = "Could not update slider visibility: ${e.localizedMessage ?: "Check Firestore permissions."}"
                        }
                        .addOnSuccessListener { message = if (enabled) "Home slider enabled." else "Home slider disabled." }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2E7D32),
                        checkedBorderColor = Color(0xFF2E7D32),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFC62828),
                        uncheckedBorderColor = Color(0xFFC62828)
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("Choose a slider design and tap Apply. Slider 4 automatically follows the selected app theme.", LocalAppLanguage.current), color = AdminMuted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        (1..4).forEach { design ->
            val colors = when (design) { 4 -> listOf(AdminInk, AdminInk.copy(alpha = 0.92f), AdminInk, AdminInk); 3 -> listOf(Color(0xFFF8F0DC), AdminInk, AdminInk); 2 -> listOf(AdminInk, AdminInk, AdminInk); else -> listOf(AdminInk, AdminInk, AdminInk) }
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { activeDesign = design; message = null }, shape = RoundedCornerShape(20.dp), border = BorderStroke(if (activeDesign == design) 2.dp else 1.dp, Color(0xFFB5B5B5)), colors = CardDefaults.cardColors(containerColor = AdminSurface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        val previewArtwork = when (design) {
                            1 -> R.drawable.slga_banner
                            2 -> R.drawable.slgan_banner_02_traditional_formulations
                            3 -> R.drawable.slgan_banner_03_natural_family_care
                            else -> R.drawable.slgan_banner_06_tradition_purity_wellness
                        }
                        Image(
                            painter = painterResource(id = previewArtwork),
                            contentDescription = "Slider $design promotional banner preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (design == 4) {
                            val themeOverlay = when (activeAppThemeId) {
                                3 -> Color(0xFF0B1118).copy(alpha = 0.30f)
                                4 -> Color(0xFF075B2A).copy(alpha = 0.22f)
                                2 -> Color(0xFF214B35).copy(alpha = 0.20f)
                                else -> Color(0xFF25282D).copy(alpha = 0.22f)
                            }
                            Box(Modifier.fillMaxSize().background(themeOverlay))
                        }
                        Box(Modifier.fillMaxSize().padding(14.dp)) {
                            Column(
                                Modifier.align(Alignment.CenterStart).fillMaxWidth(0.82f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    when (design) {
                                        4 -> "✦  THEME-ADAPTIVE SIGNATURE"
                                        3 -> "PURE AYURVEDA"
                                        2 -> "BOTANICAL WELLNESS"
                                        else -> "SLGAN · TRUSTED SINCE 1990"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    when (design) {
                                        4 -> "Wellness, Inspired by Nature"
                                        3 -> "Pure Ayurveda for Better Living"
                                        2 -> "Nature’s Goodness"
                                        else -> "Authentic Ayurvedic Formulations"
                                    },
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    when (design) {
                                        4 -> "Traditional wisdom · Trusted since 1990"
                                        3 -> "Traditional wisdom · Natural wellness"
                                        2 -> "Herbs · Roots · Everyday wellness"
                                        else -> "Traditional wisdom · Natural wellness"
                                    },
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 10.sp
                                )
                            }
                            Icon(
                                if (activeDesign == design) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = if (activeDesign == design) "Selected slider" else "Select slider",
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(when (design) { 1 -> "Slider 1 · Current banner"; 2 -> "Slider 2 · Nature’s Goodness"; 3 -> "Slider 3 · AdminBackground & Green"; else -> "Slider 4 · Theme-Adaptive Signature" }, color = AdminInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(when (design) { 1 -> "Keep the existing look"; 2 -> "Deep botanical green, herbal imagery and gold accents"; 3 -> "Fresh botanical, natural look"; else -> "Uses the selected app theme’s AdminInk and AdminInk palette, with high-contrast text and button styling." }, color = AdminMuted, fontSize = 12.sp)
                    if (activeDesign == design) Text(tr("SELECTED", LocalAppLanguage.current), color = AdminInk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Button(onClick = {
            saving = true; message = null
            db.collection("appSettings").document("homeBanner")
                .set(mapOf("activeDesign" to activeDesign, "enabled" to sliderEnabled, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { saving = false; message = "Slider $activeDesign applied to the home page." }
                .addOnFailureListener { e -> saving = false; message = "Could not apply slider: ${e.localizedMessage ?: "Check Firestore permissions."}" }
        }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AdminInk, contentColor = Color.White)) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp)); Text(if (saving) "Applying…" else "Apply Slider $activeDesign", fontWeight = FontWeight.Bold)
        }
        message?.let { Spacer(Modifier.height(10.dp)); Text(it, color = if (it.startsWith("Could not")) AdminInk else AdminInk, fontSize = 13.sp) }
    }
}

@Composable
private fun AdminDiseasesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    var records by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var diseaseName by remember { mutableStateOf("") }
    var diseaseDescription by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val registration = db.collection("diseases").addSnapshotListener { snapshot, error ->
            if (error != null) {
                loadError = error.localizedMessage ?: "Unable to load diseases."
                loading = false
            } else {
                records = snapshot?.documents?.map { doc ->
                    val data = doc.data.orEmpty()
                    val rawDiseaseId = data["disease_id"]
                    val numericDiseaseId = when (rawDiseaseId) {
                        is Number -> rawDiseaseId.toLong()
                        else -> Regex("(?i)^disease(\\d+)$").matchEntire(rawDiseaseId?.toString() ?: doc.id)
                            ?.groupValues?.getOrNull(1)?.toLongOrNull()
                            ?: (rawDiseaseId as? String)?.toLongOrNull()
                    }
                    // Convert legacy IDs such as disease003 into numeric Firestore fields.
                    if (numericDiseaseId != null && rawDiseaseId !is Number) {
                        db.collection("diseases").document(doc.id)
                            .update("disease_id", numericDiseaseId)
                    }
                    data + mapOf(
                        "_documentId" to doc.id,
                        "disease_id" to (numericDiseaseId ?: rawDiseaseId ?: doc.id),
                        "diseases_name" to (data["diseases_name"]?.toString() ?: ""),
                        "disease_desc" to (data["disease_desc"]?.toString() ?: "")
                    )
                }?.sortedBy { it["diseases_name"].toString().lowercase() }.orEmpty()
                loadError = null
                loading = false
            }
        }
        onDispose { registration.remove() }
    }

    fun categoryForDisease(disease: Map<String, Any?>): String {
        val text = (disease["diseases_name"]?.toString().orEmpty() + " " + disease["disease_desc"]?.toString().orEmpty()).lowercase()
        return when {
            listOf("cough", "asthma", "flu", "tuberculosis", "tb", "breath", "lung", "chest", "respiratory", "throat").any { it in text } -> "Respiratory"
            listOf("stomach", "abdomen", "digest", "liver", "gastric", "acidity", "constipation", "diarrh", "ulcer", "bowel").any { it in text } -> "Digestive"
            listOf("skin", "itch", "rash", "eczema", "psoriasis", "acne", "allergy").any { it in text } -> "Skin"
            listOf("bone", "joint", "arthritis", "back pain", "muscle", "knee", "spine").any { it in text } -> "Bone & Joints"
            listOf("heart", "blood pressure", "hypertension", "diabetes", "sugar", "obesity", "thyroid", "kidney", "urinary").any { it in text } -> "Metabolic & Other"
            else -> "General"
        }
    }
    val categories = listOf("All", "General", "Respiratory", "Digestive", "Skin", "Bone & Joints", "Metabolic & Other")
    val filtered = remember(records, search, selectedCategory) {
        records.filter { disease ->
            val matchesSearch = (disease["diseases_name"]?.toString().orEmpty() + " " + disease["disease_desc"]?.toString().orEmpty()).contains(search.trim(), ignoreCase = true)
            matchesSearch && (selectedCategory == "All" || categoryForDisease(disease) == selectedCategory)
        }
    }
    val groupedDiseases = remember(filtered) { filtered.groupBy { categoryForDisease(it) }.toSortedMap() }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!saving) showAddDialog = false },
            containerColor = AdminSurface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(Modifier.size(54.dp).background(AdminLightGray, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = AdminInk, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text(tr("Add disease", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Add a disease to the app catalogue.", LocalAppLanguage.current), color = AdminMuted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = diseaseName,
                        onValueChange = { diseaseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Disease name *", LocalAppLanguage.current)) },
                        placeholder = { Text(tr("e.g. Cough", LocalAppLanguage.current)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = diseaseDescription,
                        onValueChange = { diseaseDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Description", LocalAppLanguage.current)) },
                        placeholder = { Text(tr("Enter details (optional)", LocalAppLanguage.current)) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !saving && diseaseName.trim().isNotEmpty(),
                    onClick = {
                        val cleanName = diseaseName.trim()
                        val duplicate = records.any { it["diseases_name"]?.toString()?.equals(cleanName, ignoreCase = true) == true }
                        if (duplicate) {
                            Toast.makeText(context, "This disease already exists.", Toast.LENGTH_SHORT).show()
                        } else {
                            saving = true
                            val ref = db.collection("diseases").document()
                            val diseasesCollection = db.collection("diseases")
                            val counterRef = db.collection("system_counters").document("disease_id")
                            // Firestore transactions can read documents, not an entire collection query.
                            // Read existing IDs first, then atomically advance the shared counter.
                            diseasesCollection.get()
                                .addOnSuccessListener { diseaseSnapshot ->
                                    val maxExistingId = diseaseSnapshot.documents.mapNotNull { document ->
                                        val raw = document.get("disease_id")
                                        when (raw) {
                                            is Number -> raw.toLong()
                                            else -> {
                                                val text = raw?.toString() ?: document.id
                                                Regex("(?i)^disease(\\d+)$").matchEntire(text)
                                                    ?.groupValues?.getOrNull(1)?.toLongOrNull()
                                                    ?: text.toLongOrNull()
                                            }
                                        }
                                    }.maxOrNull() ?: 0L
                                    db.runTransaction { transaction ->
                                        val counterSnapshot = transaction.get(counterRef)
                                        val nextId = maxOf(counterSnapshot.getLong("last_id") ?: 0L, maxExistingId) + 1L
                                        val now = System.currentTimeMillis()
                                        transaction.set(counterRef, mapOf("last_id" to nextId, "updatedAt" to now))
                                        transaction.set(ref, mapOf(
                                            "disease_id" to nextId,
                                            "diseases_name" to cleanName,
                                            "disease_desc" to diseaseDescription.trim(),
                                            "createdAt" to now,
                                            "updatedAt" to now
                                        ))
                                        nextId
                                    }.addOnSuccessListener {
                                        saving = false
                                        showAddDialog = false
                                        diseaseName = ""
                                        diseaseDescription = ""
                                        Toast.makeText(context, "Disease added successfully.", Toast.LENGTH_SHORT).show()
                                    }.addOnFailureListener { error ->
                                        saving = false
                                        Toast.makeText(context, "Could not add disease: ${error.localizedMessage ?: "Please try again."}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener { error ->
                                    saving = false
                                    Toast.makeText(context, "Could not check existing diseases: ${error.localizedMessage ?: "Please try again."}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminInk),
                    shape = RoundedCornerShape(12.dp)
                ) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text(tr("Save disease", LocalAppLanguage.current)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showAddDialog = false }) { Text(tr("Cancel", LocalAppLanguage.current), color = AdminMuted) }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        // Match the Products admin page header: white rounded container, title row,
        // forest-green library card, and search field inside the same header section.
        Column(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(bottom = 14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to dashboard", tint = AdminInk) }
                Column(Modifier.weight(1f)) {
                    Text(tr("Diseases", LocalAppLanguage.current), color = AdminInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${records.size} ${tr("disease catalogue items", LocalAppLanguage.current)}", color = AdminMuted, fontSize = 12.sp)
                }
            }
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(50.dp).background(AdminLightGray, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = AdminInk, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Disease Library", LocalAppLanguage.current), color = AdminInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Manage diseases shown in the customer app", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminSurface, contentColor = AdminInk, disabledContainerColor = AdminBackground.copy(alpha = 0.7f), disabledContentColor = AdminInk)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Add disease", LocalAppLanguage.current), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text("${if (loading) "—" else records.size.toString()} TOTAL", color = AdminMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(60.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp),
                label = { Text(tr("Search diseases", LocalAppLanguage.current), modifier = Modifier.background(Color.Transparent), color = AdminInk, fontSize = 12.sp, maxLines = 1) },
                placeholder = { Text(tr("Disease name or description", LocalAppLanguage.current), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = AdminMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AdminInk) },
                trailingIcon = {
                    if (search.isNotEmpty()) IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, "Clear search")
                    }
                },
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedLabelColor = AdminMuted,
                    unfocusedLabelColor = AdminMuted,
                    cursorColor = AdminInk
                )
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(AdminMuted.copy(alpha = 0.45f)))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                categories.forEach { category ->
                    val count = if (category == "All") records.size else records.count { categoryForDisease(it) == category }
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category },
                        label = { Text("$category ($count)", fontSize = 11.sp) }, shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminInk, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = AdminInk))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading -> AppLoader("Loading diseases…")
            loadError != null -> Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = AdminMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text(tr("Could not load diseases", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold)
                Text(loadError.orEmpty(), color = AdminInk, fontSize = 12.sp)
                Text(tr("Check that the latest Firestore rules are published.", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp)
            }
            filtered.isEmpty() -> EmptyState("🌿", if (search.isBlank()) "No diseases added yet" else "No matching diseases", "Use Add to create your first disease entry.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedDiseases.forEach { (category, diseasesInCategory) ->
                    item(key = "category-$category") {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(category, color = AdminInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Text(diseasesInCategory.size.toString() + " items", color = AdminMuted, fontSize = 10.sp)
                        }
                    }
                    items(diseasesInCategory.chunked(2), key = { row -> "disease-row-" + row.joinToString("-") { it["_documentId"].toString() } }) { rowDiseases ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            rowDiseases.forEach { disease ->
                                val name = disease["diseases_name"]?.toString().orEmpty().ifBlank { "Unnamed disease" }
                                val description = disease["disease_desc"]?.toString().orEmpty().ifBlank { "Information not available." }
                                val diseaseEmoji = when (category) {
                                    "Respiratory" -> "🫁"
                                    "Digestive" -> "◉"
                                    "Skin" -> "✦"
                                    "Bone & Joints" -> "♧"
                                    "Metabolic & Other" -> "⌁"
                                    else -> "✚"
                                }
                                Card(Modifier.weight(1f).defaultMinSize(minHeight = 132.dp), shape = RoundedCornerShape(15.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(0.7.dp, Color(0xFFB5B5B5)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(11.dp)) {
                                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(AdminLightGray), contentAlignment = Alignment.Center) {
                                            Text(diseaseEmoji, color = AdminInk, fontSize = 20.sp)
                                        }
                                        Spacer(Modifier.height(9.dp))
                                        Text(name, color = AdminInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Text(description, color = AdminMuted, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (rowDiseases.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCustomersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var customers by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Latest") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var updatingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var viewingCustomer by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var editingCustomer by remember { mutableStateOf<Map<String, Any?>?>(null) }

    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("customers")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    error = exception.localizedMessage ?: "Unable to load customers."
                    loading = false
                } else {
                    customers = snapshot?.documents?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        if (data["is_admin"] == true) return@mapNotNull null
                        data + mapOf("uid" to doc.id)
                    }?.sortedBy {
                        (it["firstName"]?.toString().orEmpty() + " " + it["lastName"]?.toString().orEmpty()).trim().lowercase()
                    }.orEmpty()
                    error = null
                    loading = false
                }
            }
        onDispose { registration.remove() }
    }

    val enabledCount = remember(customers) { customers.count { (it["is_active"] as? Boolean) ?: (it["enabled"] != false) } }
    val disabledCount = customers.size - enabledCount
    val visibleCustomers = remember(customers, search, statusFilter, sortBy) {
        customers.filter { customer ->
            val matchesSearch = listOf("firstName", "lastName", "nickname", "email", "mobileNumber")
                .mapNotNull { customer[it]?.toString() }.joinToString(" ")
                .contains(search.trim(), ignoreCase = true)
            val active = (customer["is_active"] as? Boolean) ?: (customer["enabled"] != false)
            val matchesStatus = statusFilter == "All" || (statusFilter == "Enabled" && active) || (statusFilter == "Disabled" && !active)
            matchesSearch && matchesStatus
        }.let { result ->
            when (sortBy) {
                "Name A–Z" -> result.sortedBy { (it["firstName"]?.toString().orEmpty() + " " + it["lastName"]?.toString().orEmpty()).trim().lowercase() }
                "Name Z–A" -> result.sortedByDescending { (it["firstName"]?.toString().orEmpty() + " " + it["lastName"]?.toString().orEmpty()).trim().lowercase() }
                else -> result.sortedByDescending { customer ->
                    listOf("createdAt", "created_at", "joinedAt", "registrationDate")
                        .firstNotNullOfOrNull { key ->
                            when (val value = customer[key]) {
                                is com.google.firebase.Timestamp -> value.toDate().time
                                is java.util.Date -> value.time
                                is Number -> value.toLong()
                                else -> null
                            }
                        } ?: 0L
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(AdminBackground).padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AdminInk) }
            Column(Modifier.weight(1f)) {
                Text(tr("Customers", LocalAppLanguage.current), color = AdminInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(tr("Manage customer accounts", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple(customers.size.toString(), "Total", AdminLightGray), Triple(enabledCount.toString(), "Enabled", AdminLightGray), Triple(disabledCount.toString(), "Disabled", AdminLightGray)).forEach { item ->
                Card(Modifier.weight(1f), shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFB5B5B5)), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 9.dp)) {
                        Text(item.first, color = AdminInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(item.second, color = AdminMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                modifier = Modifier.weight(1f).height(54.dp), singleLine = true,
                placeholder = { Text(tr("Search name, email or mobile…", LocalAppLanguage.current), fontSize = 12.sp, color = AdminMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AdminInk) },
                trailingIcon = { if (search.isNotBlank()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, "Clear search") } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFB5B5B5), unfocusedBorderColor = Color(0xFFB5B5B5), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = AdminInk, unfocusedTextColor = AdminInk, cursorColor = AdminInk)
            )
            Spacer(Modifier.width(8.dp))
            Box {
                Surface(onClick = { sortMenuExpanded = true }, shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFB5B5B5))) {
                    Row(Modifier.height(52.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, null, tint = AdminInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null, tint = AdminInk)
                    }
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    listOf("Latest", "Name A–Z", "Name Z–A").forEach { option ->
                        DropdownMenuItem(text = { Text(option, color = AdminInk) }, onClick = { sortBy = option; sortMenuExpanded = false })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("All", "Enabled", "Disabled").forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { statusFilter = status },
                    label = { Text(when (status) { "All" -> "All (${customers.size})"; "Enabled" -> "Enabled ($enabledCount)"; else -> "Disabled ($disabledCount)" }, fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminInk, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = AdminInk)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("SORT", LocalAppLanguage.current) + ": $sortBy", color = AdminMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.weight(1f))
            Text(tr("Showing", LocalAppLanguage.current) + " ${visibleCustomers.size} " + tr("of", LocalAppLanguage.current) + " ${customers.size}", color = AdminMuted, fontSize = 10.sp)
        }
        when {
            loading -> AppLoader("Loading customers…", compact = true, messageColor = AdminInk)
            error != null -> Text(tr("Could not load customers", LocalAppLanguage.current) + ": $error", color = AdminInk, modifier = Modifier.padding(16.dp))
            visibleCustomers.isEmpty() -> EmptyState("👥", if (search.isBlank() && statusFilter == "All") "No customers yet" else "No matching customers", "Try changing your search or filters.")
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleCustomers, key = { it["uid"].toString() }) { customer ->
                    val uid = customer["uid"].toString()
                    val firstName = customer["firstName"]?.toString().orEmpty().trim()
                    val lastName = customer["lastName"]?.toString().orEmpty().trim()
                    val nickname = customer["nickname"]?.toString().orEmpty().trim()
                    val name = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { nickname }.ifBlank { "Customer" }
                    val email = customer["email"]?.toString().orEmpty()
                    val phone = customer["mobileNumber"]?.toString().orEmpty()
                    val enabled = (customer["is_active"] as? Boolean) ?: (customer["enabled"] != false)
                    val isUpdating = uid in updatingIds
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(0.7.dp, Color(0xFFB5B5B5)), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(CircleShape).background(AdminLightGray), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = AdminInk, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = AdminInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(email.ifBlank { "Email not provided" }, color = AdminMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (phone.isNotBlank()) Text(phone, color = AdminMuted, fontSize = 10.sp, maxLines = 1)
                                }
                                Surface(shape = RoundedCornerShape(7.dp), color = AdminLightGray) {
                                    Text(if (enabled) "Enabled" else "Disabled", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = AdminInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFB5B5B5))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("Account status", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                if (isUpdating) CircularProgressIndicator(Modifier.size(17.dp), color = AdminInk, strokeWidth = 2.dp)
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { newValue ->
                                        if (!isUpdating) {
                                            updatingIds = updatingIds + uid
                                            FirebaseFirestore.getInstance().collection("customers").document(uid)
                                                .update(mapOf("is_active" to newValue, "updatedAt" to System.currentTimeMillis()))
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, if (newValue) "Customer enabled" else "Customer disabled", Toast.LENGTH_SHORT).show()
                                                    updatingIds = updatingIds - uid
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(context, "Could not update customer: " + (exception.localizedMessage ?: "permission denied"), Toast.LENGTH_LONG).show()
                                                    updatingIds = updatingIds - uid
                                                }
                                        }
                                    },
                                    enabled = !isUpdating,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2E7D32), checkedBorderColor = Color(0xFF2E7D32), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFC62828), uncheckedBorderColor = Color(0xFFC62828))
                                )
                                IconButton(onClick = { viewingCustomer = customer }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Visibility, "View customer details", tint = Color(0xFFB5B5B5), modifier = Modifier.size(19.dp)) }
                                IconButton(onClick = { editingCustomer = customer }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, "Edit customer details", tint = Color(0xFFB5B5B5), modifier = Modifier.size(19.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    viewingCustomer?.let { customer ->
        val uid = customer["uid"]?.toString().orEmpty()
        val displayName = listOf(customer["firstName"]?.toString().orEmpty(), customer["lastName"]?.toString().orEmpty())
            .map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { customer["nickname"]?.toString().orEmpty() }.ifBlank { "Customer" }
        AlertDialog(
            onDismissRequest = { viewingCustomer = null },
            containerColor = AdminSurface,
            title = { Text(tr("Customer Details", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(displayName, color = AdminInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    customer.entries.filter { it.key != "uid" && it.key != "password" && it.key != "passwordHash" && it.value != null && it.value.toString().isNotBlank() }
                        .sortedBy { it.key }
                        .forEach { entry ->
                            Text(entry.key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() } + ": " + entry.value.toString(), color = AdminMuted, fontSize = 13.sp)
                        }
                    Text(tr("Customer ID: $uid", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { viewingCustomer = null }) { Text(tr("Close", LocalAppLanguage.current), color = AdminInk) } }
        )
    }

    editingCustomer?.let { customer ->
        val uid = customer["uid"]?.toString().orEmpty()
        var firstName by remember(uid) { mutableStateOf(customer["firstName"]?.toString().orEmpty()) }
        var lastName by remember(uid) { mutableStateOf(customer["lastName"]?.toString().orEmpty()) }
        var nickname by remember(uid) { mutableStateOf(customer["nickname"]?.toString().orEmpty()) }
        var email by remember(uid) { mutableStateOf(customer["email"]?.toString().orEmpty()) }
        val adminCountryOptions = remember {
            listOf(
                Triple("IN", "🇮🇳", "+91"), Triple("US", "🇺🇸", "+1"), Triple("CA", "🇨🇦", "+1"),
                Triple("GB", "🇬🇧", "+44"), Triple("AU", "🇦🇺", "+61"), Triple("NZ", "🇳🇿", "+64"),
                Triple("AE", "🇦🇪", "+971"), Triple("SA", "🇸🇦", "+966"), Triple("SG", "🇸🇬", "+65"),
                Triple("MY", "🇲🇾", "+60"), Triple("LK", "🇱🇰", "+94"), Triple("NP", "🇳🇵", "+977"),
                Triple("BD", "🇧🇩", "+880"), Triple("PK", "🇵🇰", "+92"), Triple("ZA", "🇿🇦", "+27"),
                Triple("DE", "🇩🇪", "+49"), Triple("FR", "🇫🇷", "+33"), Triple("IT", "🇮🇹", "+39"),
                Triple("ES", "🇪🇸", "+34"), Triple("NL", "🇳🇱", "+31"), Triple("JP", "🇯🇵", "+81"),
                Triple("CN", "🇨🇳", "+86"), Triple("BR", "🇧🇷", "+55"), Triple("MX", "🇲🇽", "+52"),
                Triple("PH", "🇵🇭", "+63"), Triple("ID", "🇮🇩", "+62"), Triple("TH", "🇹🇭", "+66"),
                Triple("NG", "🇳🇬", "+234"), Triple("KE", "🇰🇪", "+254"), Triple("IE", "🇮🇪", "+353")
            )
        }
        val savedMobile = customer["mobileNumber"]?.toString().orEmpty()
        var adminSelectedCountry by remember(uid) {
            mutableStateOf(
                adminCountryOptions.firstOrNull { it.first == customer["countryIso"]?.toString() }
                    ?: adminCountryOptions.sortedByDescending { it.third.length }.firstOrNull { savedMobile.startsWith(it.third) }
                    ?: adminCountryOptions.first()
            )
        }
        var adminCountryMenuExpanded by remember(uid) { mutableStateOf(false) }
        var mobile by remember(uid) {
            mutableStateOf(
                if (savedMobile.startsWith(adminSelectedCountry.third)) savedMobile.removePrefix(adminSelectedCountry.third).filter(Char::isDigit)
                else savedMobile.filter(Char::isDigit)
            )
        }
        var gender by remember(uid) { mutableStateOf(customer["gender"]?.toString().orEmpty()) }
        var savingCustomer by remember(uid) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!savingCustomer) editingCustomer = null },
            containerColor = AdminSurface,
            title = { Text(tr("Edit Customer", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(firstName, { firstName = it }, label = { Text(tr("First name", LocalAppLanguage.current)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(lastName, { lastName = it }, label = { Text(tr("Last name", LocalAppLanguage.current)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(nickname, { nickname = it }, label = { Text(tr("Nickname", LocalAppLanguage.current)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(email, { email = it }, label = { Text(tr("Email", LocalAppLanguage.current)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(0.42f)) {
                            OutlinedButton(
                                onClick = { adminCountryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("${adminSelectedCountry.second} ${adminSelectedCountry.third} ▾", color = AdminInk, maxLines = 1, fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded = adminCountryMenuExpanded,
                                onDismissRequest = { adminCountryMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 300.dp).background(AdminBackground)
                            ) {
                                adminCountryOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option.second}  ${option.first}  ${option.third}", color = AdminInk) },
                                        onClick = {
                                            adminSelectedCountry = option
                                            adminCountryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it.filter(Char::isDigit).take(15) },
                            label = { Text(tr("Mobile number", LocalAppLanguage.current)) },
                            modifier = Modifier.weight(0.58f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                    OutlinedTextField(gender, { gender = it }, label = { Text(tr("Gender", LocalAppLanguage.current)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = !savingCustomer, onClick = {
                    savingCustomer = true
                    FirebaseFirestore.getInstance().collection("customers").document(uid).update(
                        mapOf("firstName" to firstName.trim(), "lastName" to lastName.trim(), "nickname" to nickname.trim(), "email" to email.trim(), "mobileNumber" to (adminSelectedCountry.third + mobile.trim()), "countryCode" to adminSelectedCountry.third, "countryIso" to adminSelectedCountry.first, "gender" to gender.trim(), "updatedAt" to System.currentTimeMillis())
                    ).addOnSuccessListener {
                        Toast.makeText(context, "Customer updated", Toast.LENGTH_SHORT).show()
                        editingCustomer = null
                    }.addOnFailureListener { exception ->
                        savingCustomer = false
                        Toast.makeText(context, "Could not update customer: " + (exception.localizedMessage ?: "permission denied"), Toast.LENGTH_LONG).show()
                    }
                }) { Text(if (savingCustomer) "Saving…" else "Save", color = AdminInk) }
            },
            dismissButton = { TextButton(enabled = !savingCustomer, onClick = { editingCustomer = null }) { Text(tr("Cancel", LocalAppLanguage.current), color = AdminMuted) } }
        )
    }
}

@Composable
private fun AdminOrdersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var orders by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var expandedOrderId by remember { mutableStateOf<String?>(null) }
    val db = remember { FirebaseFirestore.getInstance() }

    DisposableEffect(Unit) {
        val listener = db.collection("orders").addSnapshotListener { snap, err ->
            if (err != null) {
                error = err.localizedMessage ?: "Unable to load orders"
                loading = false
            } else {
                orders = snap?.documents?.map { it.data.orEmpty() + ("_documentId" to it.id) }
                    ?.sortedByDescending { (it["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L }
                    .orEmpty()
                loading = false
                error = null
            }
        }
        onDispose { listener.remove() }
    }

    val dateFormat = remember { java.text.SimpleDateFormat("EEEE, dd MMM yyyy", java.util.Locale.getDefault()) }
    val query = searchQuery.trim().lowercase()
    val orderStatusForFilter: (Map<String, Any?>) -> String = { order ->
        when (order["orderStatus"]?.toString()?.lowercase()) {
            "accepted" -> "accepted"
            "rejected" -> "rejected"
            else -> "pending"
        }
    }
    val pendingCount = orders.count { orderStatusForFilter(it) == "pending" }
    val acceptedCount = orders.count { orderStatusForFilter(it) == "accepted" }
    val rejectedCount = orders.count { orderStatusForFilter(it) == "rejected" }
    val filteredOrders = remember(orders, query, statusFilter) {
        orders.filter { order ->
            val matchesStatus = statusFilter == "All" || orderStatusForFilter(order) == statusFilter.lowercase()
            val matchesQuery = query.isBlank() ||
                listOf("orderId", "_documentId", "customerName", "phone", "email", "orderStatus")
                    .any { key -> order[key]?.toString()?.lowercase()?.contains(query) == true } ||
                listOf("items", "orderItems", "order_items", "cartItems", "cart_items", "products", "medicines").firstNotNullOfOrNull { key -> order[key] as? List<*> }?.any { raw ->
                    val item = raw as? Map<*, *>
                    item?.values?.any { it?.toString()?.lowercase()?.contains(query) == true } == true
                } == true
            matchesStatus && matchesQuery
        }
    }
    val grouped = filteredOrders.groupBy { row ->
        val ts = row["createdAt"] as? com.google.firebase.Timestamp
        if (ts == null) "Date not available" else dateFormat.format(ts.toDate())
    }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = AdminInk) }
            Column(Modifier.weight(1f)) {
                Text(tr("Manage Orders", LocalAppLanguage.current), color = AdminInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("${filteredOrders.size} " + tr("of", LocalAppLanguage.current) + " ${orders.size} " + tr("orders • newest first", LocalAppLanguage.current), color = AdminInk.copy(alpha = 0.65f), fontSize = 12.sp)
            }
            Icon(Icons.Default.Inventory2, contentDescription = "Orders", tint = AdminInk, modifier = Modifier.size(27.dp))
        }
        Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(AdminLightGray, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AdminInk, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Orders Library", LocalAppLanguage.current), color = AdminInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(tr("Review and manage customer orders", LocalAppLanguage.current), color = AdminMuted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(orders.size.toString(), color = AdminInk, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text(tr("TOTAL", LocalAppLanguage.current), color = AdminMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).height(60.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp),
            label = { Text(tr("Search order", LocalAppLanguage.current), modifier = Modifier.background(Color.Transparent), color = AdminInk, fontSize = 12.sp, maxLines = 1) },
            placeholder = { Text(tr("Order ID, customer or mobile", LocalAppLanguage.current), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = AdminMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search orders", tint = AdminInk) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = tr("Clear search", LocalAppLanguage.current))
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AdminInk,
                unfocusedBorderColor = AdminInk.copy(alpha = 0.45f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = AdminMuted,
                unfocusedLabelColor = AdminMuted,
                cursorColor = AdminInk
            )
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(AdminMuted.copy(alpha = 0.45f)))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AdminSurface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = AdminInk, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(tr("Filter Orders By Status", LocalAppLanguage.current), color = AdminInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = statusFilter == "All",
                        onClick = { statusFilter = "All" },
                        label = { Text("All  ${orders.size}") },
                        leadingIcon = { Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminLightGray, selectedLabelColor = AdminInk, selectedLeadingIconColor = AdminInk)
                    )
                    FilterChip(
                        selected = statusFilter == "Pending",
                        onClick = { statusFilter = "Pending" },
                        label = { Text("Pending  ${pendingCount}") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminInk, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                    FilterChip(
                        selected = statusFilter == "Accepted",
                        onClick = { statusFilter = "Accepted" },
                        label = { Text(tr("Accepted", LocalAppLanguage.current) + "  ${acceptedCount}") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminInk, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                    FilterChip(
                        selected = statusFilter == "Rejected",
                        onClick = { statusFilter = "Rejected" },
                        label = { Text(tr("Rejected", LocalAppLanguage.current) + "  ${rejectedCount}") },
                        leadingIcon = { Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminInk, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                }
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoader("Loading orders…") }
            error != null -> Text(tr("Could not load orders", LocalAppLanguage.current) + ": " + error, color = AdminInk, modifier = Modifier.padding(20.dp))
            orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tr("No orders yet", LocalAppLanguage.current), color = AdminMuted, fontSize = 18.sp) }
            filteredOrders.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AdminMuted, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(tr("No orders match your search", LocalAppLanguage.current), color = AdminMuted, fontSize = 16.sp)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.forEach { (date, rows) ->
                    item(key = "date-" + date) {
                        Text(date + " • " + rows.size + " orders", color = AdminInk, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(rows, key = { it["_documentId"]?.toString().orEmpty() }) { order ->
                        val docId = order["_documentId"]?.toString().orEmpty()
                        val displayId = order["orderId"]?.toString()?.ifBlank { docId } ?: docId
                        val status = order["orderStatus"]?.toString() ?: "pending_verification"
                        val pending = status == "pending_verification" || status == "pending"
                        val expanded = expandedOrderId == docId
                        val total = (order["grandTotal"] as? Number)?.toDouble()
                            ?: (order["subtotal"] as? Number)?.toDouble() ?: 0.0
                        val ts = order["createdAt"] as? com.google.firebase.Timestamp
                        val time = ts?.let { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(it.toDate()) } ?: "Time unavailable"
                        val itemRows = order["items"] as? List<*> ?: emptyList<Any>()
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(0.8.dp, Color(0xFFB5B5B5))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AdminLightGray), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Inventory2, null, tint = AdminInk, modifier = Modifier.size(21.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("#" + displayId.takeLast(12), color = AdminInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                        Text(order["customerName"]?.toString().orEmpty().ifBlank { "Customer" }, color = AdminInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(time + " • " + (order["phone"]?.toString().orEmpty().ifBlank { "No phone" }), color = AdminMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹" + String.format(java.util.Locale.US, "%.2f", total), color = AdminInk, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                        Surface(shape = RoundedCornerShape(7.dp), color = AdminLightGray, border = BorderStroke(0.5.dp, Color(0xFFB5B5B5))) {
                                            Text(tr(status.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }, LocalAppLanguage.current), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = AdminInk, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFB5B5B5).copy(alpha = 0.55f))
                                Text(tr("ORDER ITEMS", LocalAppLanguage.current) + " (" + itemRows.size + ")", color = AdminMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                if (itemRows.isEmpty()) {
                                    Text(tr("Item details are not available for this order.", LocalAppLanguage.current), color = AdminMuted, fontSize = 11.sp)
                                } else {
                                    itemRows.take(3).forEachIndexed { index, raw ->
                                        val item = raw as? Map<*, *>
                                        if (item != null) {
                                            val name = item["productName"]?.toString() ?: item["name"]?.toString() ?: "Item"
                                            val qty = (item["quantity"] as? Number)?.toInt() ?: 0
                                            val price = (item["price"] as? Number)?.toDouble() ?: (item["unitPrice"] as? Number)?.toDouble()
                                            val lineTotal = (item["lineTotal"] as? Number)?.toDouble() ?: (item["total"] as? Number)?.toDouble() ?: price?.times(qty)
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(AdminLightGray), contentAlignment = Alignment.Center) {
                                                    Text("📦", fontSize = 17.sp)
                                                }
                                                Spacer(Modifier.width(9.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(name, color = AdminInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(tr("Qty", LocalAppLanguage.current) + ": " + qty + (price?.let { " • ₹" + String.format(java.util.Locale.US, "%.2f", it) + " " + tr("each", LocalAppLanguage.current) } ?: ""), color = AdminMuted, fontSize = 9.sp)
                                                }
                                                Text(lineTotal?.let { "₹" + String.format(java.util.Locale.US, "%.2f", it) } ?: "—", color = AdminInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (itemRows.size > 3) Text("+ " + (itemRows.size - 3) + " " + tr("more items", LocalAppLanguage.current), color = AdminMuted, fontSize = 10.sp)
                                }
                                HorizontalDivider(color = Color(0xFFB5B5B5).copy(alpha = 0.55f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(tr("Total amount", LocalAppLanguage.current), color = AdminMuted, fontSize = 10.sp)
                                        Text("₹" + String.format(java.util.Locale.US, "%.2f", total), color = AdminInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    OutlinedButton(
                                        onClick = { expandedOrderId = if (expanded) null else docId },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFB5B5B5)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminInk),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (expanded) "Hide details" else "View details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (expanded) {
                                    HorizontalDivider(color = AdminLightGray)
                                    Text(tr("Order ID", LocalAppLanguage.current) + ": " + displayId, color = AdminInk, fontSize = 11.sp)
                                    Text(tr("Email", LocalAppLanguage.current) + ": " + (order["email"]?.toString().orEmpty().ifBlank { tr("Information not available", LocalAppLanguage.current) }), color = AdminMuted, fontSize = 11.sp)
                                    Text(tr("Order time", LocalAppLanguage.current) + ": " + (ts?.let { java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(it.toDate()) } ?: tr("Information not available", LocalAppLanguage.current)), color = AdminMuted, fontSize = 11.sp)
                                    if (pending) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    busy = docId
                                                    db.collection("orders").document(docId).update(mapOf("orderStatus" to "rejected", "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                        .addOnSuccessListener { busy = null; Toast.makeText(context, "Order rejected.", Toast.LENGTH_SHORT).show() }
                                                        .addOnFailureListener { e -> busy = null; Toast.makeText(context, "Reject failed: " + (e.localizedMessage ?: "Try again"), Toast.LENGTH_LONG).show() }
                                                },
                                                enabled = busy == null, modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminInk)
                                            ) {
                                                if (busy == docId) CircularProgressIndicator(Modifier.size(15.dp), color = AdminInk, strokeWidth = 2.dp) else Icon(Icons.Default.Close, null)
                                                Spacer(Modifier.width(5.dp)); Text(tr("Reject", LocalAppLanguage.current))
                                            }
                                            Button(
                                                onClick = {
                                                    busy = docId
                                                    db.collection("orders").document(docId).update(mapOf("orderStatus" to "accepted", "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                        .addOnSuccessListener { busy = null; Toast.makeText(context, "Order approved.", Toast.LENGTH_SHORT).show() }
                                                        .addOnFailureListener { e -> busy = null; Toast.makeText(context, "Approve failed: " + (e.localizedMessage ?: "Try again"), Toast.LENGTH_LONG).show() }
                                                },
                                                enabled = busy == null, modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = AdminInk, contentColor = Color.White)
                                            ) {
                                                if (busy == docId) CircularProgressIndicator(Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Check, null)
                                                Spacer(Modifier.width(5.dp)); Text(tr("Approve", LocalAppLanguage.current))
                                            }
                                        }
                                    } else {
                                        Text(tr("This order has already been", LocalAppLanguage.current) + " " + tr(status.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }, LocalAppLanguage.current) + ".", color = AdminMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSummaryCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, contentDescription = null, tint = AdminInk, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, color = AdminInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = AdminInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AdminMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AdminModuleInfo(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(AdminLightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AdminInk)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AdminInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = AdminMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ShopScreen(
    selectedTab: String, previousTab: String, onTabChange: (String) -> Unit, appLanguage: String, onLanguageChange: (String) -> Unit, category: String, onCategoryChange: (String) -> Unit,
    search: String, onSearchChange: (String) -> Unit, cart: Map<String, Int>, products: List<Product>, productRatings: Map<String, ProductRatingSummary>, productLikes: Map<String, ProductLikeSummary>, onToggleLike: (Product) -> Unit, productsLoading: Boolean, productsError: String?, diseases: List<String>, diseasesLoading: Boolean, diseasesError: String?, latestOrderId: String?, latestOrderSuccess: Boolean?, latestOrderInvoiceUrl: String?, latestOrderMessage: String?, onAdd: (String) -> Unit, onRemove: (String) -> Unit,
    onOrderResult: (String, Boolean, String?, String) -> Unit, onSignOut: () -> Unit, onProductClick: (Product) -> Unit, onClearCart: () -> Unit
) {
    val cartCount = products.sumOf { product -> (cart[product.id] ?: 0).coerceAtLeast(0) }
    val accountsScrollState = rememberScrollState()
    Scaffold(containerColor = if (selectedTab == "Orders" || selectedTab == "MyOrders") Color.White else Cream, topBar = {
        Row(Modifier.fillMaxWidth().shadow(6.dp).background(appThemedColor(Color.White)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.slgan_screen_logo), contentDescription = "SLGAN logo", contentScale = ContentScale.Fit, modifier = Modifier.size(42.dp).clip(CircleShape).background(appThemedColor(Color.White)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.Center) {
                Text(tr("Sri Lakshmi Ganapathi", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        tr("Ayurveda Nilayam", LocalAppLanguage.current),
                        modifier = Modifier.alignByBaseline(),
                        color = Ink,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr("Estd. 1900", LocalAppLanguage.current),
                        modifier = Modifier.alignByBaseline(),
                        color = Muted,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onTabChange("Accounts") }) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(appThemedColor(Color(0xFFE9EDF2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Ink, modifier = Modifier.size(24.dp))
                    }
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 2.dp)
                            .size(12.dp)
                            .background(appThemedColor(Color(0xFF258348)), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                    }
            }
        }
    }, bottomBar = {
        NavigationBar(modifier = Modifier.fillMaxWidth().shadow(8.dp), containerColor = appThemedColor(Color.White), contentColor = Forest) {
            listOf(
                Triple("Home", Icons.Default.Home, "Home"),
                Triple("Diseases", Icons.Default.MedicalInformation, "Diseases"),
                Triple("Categories", Icons.Default.GridView, "Categories"),
                Triple("Orders", Icons.Default.ReceiptLong, "Orders"),
                Triple("Cart", Icons.Default.ShoppingCart, "Cart")
            ).forEach { (label, icon, route) ->
                NavigationBarItem(
                    selected = selectedTab == route,
                    onClick = { onTabChange(route) },
                    icon = {
                        if (route == "Cart") {
                            BadgedBox(badge = { if (cartCount > 0) Badge(containerColor = appThemedColor(Color(0xFF27864A)), contentColor = Color.White) { Text(cartCount.toString()) } }) {
                                Icon(icon, contentDescription = label)
                            }
                        } else {
                            Icon(icon, contentDescription = label)
                        }
                    },
                    label = { Text(tr(label, LocalAppLanguage.current), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Ink, selectedTextColor = Ink, indicatorColor = appThemedColor(Color(0xFFE6E8EC)), unselectedIconColor = Ink, unselectedTextColor = Ink)
                )
            }
        }
    }) { padding ->
        when (selectedTab) {
            "Cart" -> CartPage(Modifier.padding(padding), cart, products, onAdd, onRemove, onOrderResult, onProductClick, onAddAddress = { onTabChange("AddressBook") }, onClearCart = onClearCart) { onTabChange("Home") }
            "Orders" -> OrdersPage(Modifier.padding(padding), latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage, products, onAdd, onHome = { onTabChange(previousTab) }, onCart = { onTabChange("Cart") }, onCategories = { onCategoryChange("All"); onTabChange("Categories") }, onTabChange = onTabChange)
            "MyOrders" -> OrdersPage(Modifier.padding(padding), latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage, products, onAdd, pageTitle = "My Orders", onHome = { onTabChange("Accounts") }, onCart = { onTabChange("Cart") }, onCategories = { onTabChange("Categories") }, onTabChange = onTabChange)
            "Categories" -> CategoriesPage(Modifier.padding(padding), products, cart, productRatings, productLikes, category, onCategoryChange, onToggleLike, onAdd, onRemove, onCategoryChange, onProductClick) { onTabChange("Home") }
            "Diseases" -> DiseasesPage(Modifier.padding(padding), diseases, products, diseasesLoading, diseasesError, onHome = { onTabChange(previousTab) })
            "Accounts" -> AccountsPage(
                modifier = Modifier.padding(padding),
                scrollState = accountsScrollState,
                onTabChange = onTabChange,
                onSignOut = onSignOut,
                appLanguage = appLanguage,
                onLanguageChange = onLanguageChange
            )
            "Help" -> HelpPage(Modifier.padding(padding), onBack = { onTabChange("Accounts") }, appLanguage = appLanguage)
            "Profile" -> ProfilePage(Modifier.padding(padding), onSignOut, appLanguage, onLanguageChange, onBack = { onTabChange("Accounts") })
            "AddressBook" -> AddressBookPage(Modifier.padding(padding), onBack = { onTabChange("Accounts") })
            "Wishlist" -> WishlistPage(Modifier.padding(padding), products, productRatings, productLikes, cart, onToggleLike, onAdd, onProductClick, onBack = { onTabChange("Accounts") }, appLanguage = appLanguage)
            "About" -> AboutPage(Modifier.padding(padding), appLanguage) { onTabChange("Accounts") }
            "SocialMedia" -> SocialMediaPage(Modifier.padding(padding), appLanguage) { onTabChange("Accounts") }
            "ContactSLGAN" -> ContactSLGANPage(Modifier.padding(padding), appLanguage) { onTabChange("Accounts") }
            "AccountPrivacy" -> AccountPrivacyPage(Modifier.padding(padding), appLanguage) { onTabChange("Accounts") }
            else -> HomePage(Modifier.padding(padding), appLanguage, onLanguageChange, category, onCategoryChange, search, onSearchChange, cart, products, productRatings, productLikes, onToggleLike, productsLoading, productsError, onAdd, onProductClick)
        }
    }
}

@Composable
private fun SocialMediaPage(modifier: Modifier, appLanguage: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isEnglish = appLanguage == "English"
    val instagramUrl = "https://www.instagram.com/slganilayam/"
    val facebookUrl = "https://www.facebook.com/slganilayam"
    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF7F7F7)))) {
        Row(
            Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) }
            Text(tr("Social Media", appLanguage), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = appThemedColor(Color(0xFFE4E6EB)), thickness = 1.dp)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                tr("Connect with SLGAN", appLanguage),
                color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                tr("Follow Sri Lakshmi Ganapathi Ayurveda Nilayam on social media for updates and information.", appLanguage),
                color = Muted, fontSize = 14.sp, lineHeight = 21.sp
            )
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).background(
                            appThemedColor(Color(0xFFFCE7F3)),
                            RoundedCornerShape(14.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◎", color = appThemedColor(Color(0xFFE1306C)), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Instagram", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("@slganilayam", color = Muted, fontSize = 13.sp)
                        Text(
                            tr("View live followers on Instagram", appLanguage),
                            color = Muted, fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(instagramUrl))) }
                                .onFailure { Toast.makeText(context, if (isEnglish) "Unable to open Instagram." else "Instagram తెరవడం సాధ్యం కాలేదు.", Toast.LENGTH_SHORT).show() }
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(tr("Follow", appLanguage))
                    }
                }
            }
            Card(
                Modifier.fillMaxWidth().clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl))) }
                        .onFailure { Toast.makeText(context, if (isEnglish) "Unable to open Facebook." else "Facebook తెరవడం సాధ్యం కాలేదు.", Toast.LENGTH_SHORT).show() }
                },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(48.dp).background(appThemedColor(Color(0xFFE8F0FE)), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Facebook, contentDescription = "Facebook", tint = appThemedColor(Color(0xFF1877F2)), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Facebook", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Follow us on Facebook", appLanguage), color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.OpenInNew, contentDescription = if (isEnglish) "Open Facebook" else "Facebook తెరవండి", tint = Ink)
                }
            }
        }
    }
}

@Composable
private fun AccountPrivacyPage(modifier: Modifier, appLanguage: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val isEnglish = appLanguage == "English"
    Column(modifier.fillMaxSize().background(appThemedColor(Color.White))) {
        Row(
            Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) }
            Text(tr("Account Privacy", appLanguage), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = appThemedColor(Color(0xFFE4E6EB)), thickness = 1.dp)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text(tr("Account Privacy And Policy", appLanguage), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text(
                tr("Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN) respects your privacy. We use account information such as your name, contact details, wishlist, and order information to provide app features and support your purchases. We do not ask you to share your password or one-time verification codes with anyone.", appLanguage),
                color = appThemedColor(Color(0xFF667085)), fontSize = 16.sp, lineHeight = 23.sp
            )
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                Text(
                    tr("Your information is used to operate account, address, wishlist, and order features. Access to information should be limited to what is needed to provide these services. Please keep your sign-in credentials private and contact SLGAN support if you have questions or believe your account has been accessed without permission. Do not submit sensitive financial information in chat or support messages.", appLanguage),
                    color = appThemedColor(Color(0xFF667085)), fontSize = 16.sp, lineHeight = 23.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (expanded) tr("Read less", appLanguage) else tr("Read more", appLanguage), color = appThemedColor(Color(0xFF27851F)), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = appThemedColor(Color(0xFF27851F)))
            }
            Spacer(Modifier.height(32.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(appThemedColor(Color(0xFFF6F7FA))).clickable { showDeleteConfirmation = true }.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Ink, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Request To Delete Account", appLanguage), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Request to closure of your account", appLanguage), color = Muted, fontSize = 14.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink)
            }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(tr("Request account deletion", appLanguage)) },
            text = { Text(tr("This will open a request email to SLGAN support. Your account will not be deleted automatically. Do you want to continue?", appLanguage)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_SUBJECT, "SLGAN account deletion request")
                        putExtra(Intent.EXTRA_TEXT, "Please contact me about my SLGAN account deletion request.\nRegistered account email: ${FirebaseAuth.getInstance().currentUser?.email.orEmpty()}")
                    }
                    runCatching { context.startActivity(emailIntent) }.onFailure {
                        Toast.makeText(context, if (isEnglish) "No email app is available." else "ఇమెయిల్ యాప్ అందుబాటులో లేదు.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(tr("Continue", appLanguage)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(tr("Cancel", appLanguage)) }
            }
        )
    }
}

@Composable
private fun AboutPage(modifier: Modifier, appLanguage: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val title = if (appLanguage == "English") "About Us" else "మా గురించి"
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }
    val paragraphs = if (appLanguage == "English") listOf(
        "Sri Lakshmi Ganapathi Ayurveda Nilayam brings traditional Ayurvedic products together in one convenient place. Browse our catalogue, explore product details, and find information about ingredients and usage.",
        "Our aim is to make it easier for customers to explore our products, place orders, and revisit their favourite items. Please read each product's details carefully and consult a qualified healthcare professional for medical advice.",
        "Sri Lakshmi Ganapathi Ayurveda Nilayam is an independent Ayurvedic business. Product information in this app is provided for general information and is not a substitute for professional medical diagnosis or treatment."
    ) else listOf(
        "శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయం సంప్రదాయ ఆయుర్వేద ఉత్పత్తులను ఒకే చోట అందిస్తుంది. మా ఉత్పత్తుల జాబితాను చూడండి, ఉత్పత్తుల వివరాలు, పదార్థాలు మరియు వినియోగ సమాచారాన్ని తెలుసుకోండి.",
        "మా ఉద్దేశ్యం ఉత్పత్తులను సులభంగా చూడటం, డెలివరీ చిరునామాలను నిర్వహించడం, ఆర్డర్లు చేయడం మరియు మీకు ఇష్టమైన ఉత్పత్తులను మళ్లీ చూడటం సులభం చేయడం. వైద్య సలహా కోసం అర్హత కలిగిన వైద్య నిపుణుడిని సంప్రదించండి.",
        "శ్రీ లక్ష్మీ గణపతి ఆయుర్వేద నిలయం స్వతంత్ర ఆయుర్వేద వ్యాపారం. ఈ యాప్‌లోని ఉత్పత్తుల సమాచారం సాధారణ అవగాహన కోసం మాత్రమే; ఇది వైద్య నిర్ధారణ లేదా చికిత్సకు ప్రత్యామ్నాయం కాదు."
    )
    Column(modifier.fillMaxSize().background(appThemedColor(Color.White))) {
        Row(
            Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = appThemedColor(Color(0xFFE4E6EB)), thickness = 1.dp)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Normal)
                Spacer(Modifier.width(12.dp))
                Text("v$versionName", color = Muted, fontSize = 17.sp)
            }
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color(0xFFF7F3EA))),
                border = BorderStroke(1.dp, appThemedColor(Color(0xFFE5D8BF))),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("1900", color = appThemedColor(Color(0xFF8A642D)), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Our Heritage", appLanguage),
                        color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        tr("Sri Lakshmi Ganapathi Ayurveda Nilayam was established in 1900 by Sri Vidya Raju Allamraju Perraju Garu at Uppada Kothapalli.", appLanguage),
                        color = appThemedColor(Color(0xFF514638)),
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            paragraphs.forEachIndexed { index, paragraph ->
                Text(paragraph, color = appThemedColor(Color(0xFF667085)), fontSize = 16.sp, lineHeight = 23.sp)
                if (index != paragraphs.lastIndex) Spacer(Modifier.height(24.dp))
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Divider(color = appThemedColor(Color(0xFFE4E6EB)), thickness = 1.dp)
            Row(
                Modifier.fillMaxWidth().clickable {
                    android.app.AlertDialog.Builder(context)
                        .setTitle(if (appLanguage == "English") "Privacy Policy" else "గోప్యతా విధానం")
                        .setMessage(if (appLanguage == "English") "SLGAN uses account information to provide sign-in, wishlist, and order features. Keep your password and verification codes private. Contact the business for questions about your account data." else "సైన్-ఇన్, చిరునామాలు, విష్‌లిస్ట్ మరియు ఆర్డర్ సేవల కోసం SLGAN ఖాతా సమాచారాన్ని ఉపయోగిస్తుంది. మీ పాస్‌వర్డ్ మరియు OTPలను రహస్యంగా ఉంచండి. ఖాతా సమాచారం గురించి ప్రశ్నల కోసం వ్యాపారాన్ని సంప్రదించండి.")
                        .setPositiveButton("OK", null).show()
                }.padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Text(tr("Privacy Policy", appLanguage), color = Ink, fontSize = 19.sp) }
            Divider(color = appThemedColor(Color(0xFFE4E6EB)), thickness = 1.dp)
            Row(
                Modifier.fillMaxWidth().clickable {
                    android.app.AlertDialog.Builder(context)
                        .setTitle(if (appLanguage == "English") "Terms & Conditions" else "నిబంధనలు")
                        .setMessage(if (appLanguage == "English") "Use SLGAN to browse product information, manage your account, and place orders. Product information is general in nature. Order acceptance and delivery depend on confirmation and availability." else "ఉత్పత్తుల వివరాలు చూడటానికి, ఖాతాను నిర్వహించడానికి మరియు ఆర్డర్లు చేయడానికి SLGANను ఉపయోగించండి. ఉత్పత్తుల సమాచారం సాధారణ అవగాహన కోసం మాత్రమే. ఆర్డర్ అంగీకారం మరియు డెలివరీ నిర్ధారణ, లభ్యతపై ఆధారపడి ఉంటాయి.")
                        .setPositiveButton("OK", null).show()
                }.padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Text(tr("Terms & Conditions", appLanguage), color = Ink, fontSize = 19.sp) }
            Divider(color = appThemedColor(Color(0xFFE4E6EB)), thickness = 1.dp)
        }
    }
}

@Composable
private fun ContactSLGANPage(modifier: Modifier, appLanguage: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isEnglish = appLanguage == "English"
    val businessPhone = "9908355501"
    val businessAddress = "C/O Sri Lakshmi Ganapathi Ayurveda Nilayam, Uppada Kothapalli, Pincode 533447, Kakinada District, Andhra Pradesh"
    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF7F7F7)))) {
        Row(
            Modifier.fillMaxWidth().background(appThemedColor(Color(0xFFF7F7F7))).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) }
            Text(tr("Contact SLGAN", appLanguage), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = appThemedColor(Color(0xFFB5B5B5)), thickness = 1.dp)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(tr("Sri Lakshmi Ganapathi Ayurveda Nilayam", LocalAppLanguage.current), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                tr("We are happy to hear from you. Contact us or visit our location.", appLanguage),
                color = Muted, fontSize = 14.sp, lineHeight = 21.sp
            )
            Card(
                Modifier.fillMaxWidth().border(1.dp, appThemedColor(Color(0xFFB5B5B5)), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Ink, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Business Address", appLanguage), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "C/O Sri Lakshmi Ganapathi Ayurveda Nilayam\nUppada Kothapalli\nPincode: 533447\nKakinada District, Andhra Pradesh",
                        color = Ink, fontSize = 15.sp, lineHeight = 24.sp
                    )
                    Button(
                        onClick = {
                            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(businessAddress))
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, mapUri)) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color(0xFFE2E2E2)), contentColor = Ink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Get directions", appLanguage))
                    }
                }
            }
            Card(
                Modifier.fillMaxWidth().border(1.dp, appThemedColor(Color(0xFFB5B5B5)), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Phone Number", appLanguage), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(businessPhone, color = Ink, fontSize = 18.sp)
                    OutlinedButton(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$businessPhone"))) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Ink)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Call SLGAN", appLanguage), color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistPage(
    modifier: Modifier,
    products: List<Product>,
    productRatings: Map<String, ProductRatingSummary>,
    productLikes: Map<String, ProductLikeSummary>,
    cart: Map<String, Int>,
    onToggleLike: (Product) -> Unit,
    onAdd: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onBack: () -> Unit,
    appLanguage: String
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    var savedProductIds by remember(currentUid) { mutableStateOf<Set<String>>(emptySet()) }
    var wishlistLoading by remember(currentUid) { mutableStateOf(true) }
    var wishlistError by remember(currentUid) { mutableStateOf(false) }
    DisposableEffect(currentUid) {
        if (currentUid.isNullOrBlank()) {
            savedProductIds = emptySet()
            wishlistLoading = false
            wishlistError = false
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance().collection("productLikes")
                .whereEqualTo("userId", currentUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        wishlistError = true
                        wishlistLoading = false
                    } else if (snapshot != null) {
                        savedProductIds = snapshot.documents.mapNotNull { doc ->
                            doc.getString("productId")?.takeIf { it.isNotBlank() }
                        }.toSet()
                        wishlistError = false
                        wishlistLoading = false
                    }
                }
            onDispose { registration.remove() }
        }
    }
    val savedProducts = products.filter { it.id in savedProductIds }
    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF1F2F5)))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, appThemedColor(Color(0xFFE8EAF0))),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Your wishlist", appLanguage), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(tr("${savedProducts.size} saved products", appLanguage), color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.Favorite, contentDescription = null, tint = appThemedColor(Color(0xFFD32F2F)), modifier = Modifier.padding(end = 12.dp).size(24.dp))
        }
        if (wishlistLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppLoader(if (appLanguage == "English") "Loading your wishlist…" else "మీ విష్‌లిస్ట్ లోడ్ అవుతోంది…")
            }
        } else if (wishlistError) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(tr("Couldn't load your wishlist", appLanguage), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tr("Please check your connection and try again.", appLanguage), color = Muted, textAlign = TextAlign.Center)
            }
        } else if (savedProducts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(86.dp).background(appThemedColor(Color(0xFFF5E5C8)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Forest, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(tr("Your wishlist is empty", appLanguage), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Tap the heart on any product to save it here.", appLanguage),
                    color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                    Text(tr("Continue shopping", appLanguage), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
                savedProducts.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { product ->
                            ProductCard(
                                product = product,
                                count = cart[product.id] ?: 0,
                                ratingSummary = productRatings[product.id] ?: ProductRatingSummary(),
                                likeSummary = productLikes[product.id] ?: ProductLikeSummary(),
                                onToggleLike = onToggleLike,
                                onAdd = onAdd,
                                onProductClick = onProductClick,
                                // Keep every wishlist card the same height regardless of cart state or product name.
                                modifier = Modifier.weight(1f).height(290.dp)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun HomePage(modifier: Modifier, appLanguage: String, onLanguageChange: (String) -> Unit, category: String, onCategoryChange: (String) -> Unit, search: String, onSearchChange: (String) -> Unit, cart: Map<String, Int>, products: List<Product>, productRatings: Map<String, ProductRatingSummary>, productLikes: Map<String, ProductLikeSummary>, onToggleLike: (Product) -> Unit, productsLoading: Boolean, productsError: String?, onAdd: (String) -> Unit, onProductClick: (Product) -> Unit) {
    var databaseCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    databaseCategories = snapshot.documents.mapNotNull { doc ->
                        (doc.getString("category_name") ?: doc.getString("name") ?: doc.id)
                            .trim().takeIf { it.isNotBlank() }
                    }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                }
            }
        onDispose { registration.remove() }
    }
    // Merge Firestore category documents with categories found on Firestore-backed products.
    // This keeps categories visible even if a category document is missing or named differently.
    val homeCategories = remember(databaseCategories, products) {
        (listOf("All") + databaseCategories + products.map { it.category.trim() }
            .filter { it.isNotBlank() })
            .distinctBy { it.lowercase() }
    }

    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current
    val voiceSearchLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) onSearchChange(spoken)
        }
    }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var welcomeNickname by remember(currentUser?.uid) { mutableStateOf("") }
    var greetingLoadingLanguage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(greetingLoadingLanguage) {
        if (greetingLoadingLanguage != null) {
            kotlinx.coroutines.delay(350)
            greetingLoadingLanguage = null
        }
    }
    DisposableEffect(currentUser?.uid) {
        var registration: ListenerRegistration? = null
        currentUser?.uid?.let { uid ->
            registration = FirebaseFirestore.getInstance().collection("customers").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    welcomeNickname = snapshot?.getString("nickname").orEmpty().trim()
                }
        }
        onDispose { registration?.remove() }
    }
    val categoryProducts = products.filter { category == "All" || it.category.equals(category, true) }
    val normalizedQuery = search.lowercase().filter { it.isLetterOrDigit() }
    val exactMatches = categoryProducts.filter {
        it.name.contains(search.trim(), true) || it.subtitle.contains(search.trim(), true) ||
            (normalizedQuery.isNotBlank() && it.name.lowercase().filter { ch -> ch.isLetterOrDigit() }.contains(normalizedQuery))
    }
    val visible = if (search.isBlank() || exactMatches.isNotEmpty()) {
        if (search.isBlank()) categoryProducts else exactMatches
    } else {
        fun editDistance(a: String, b: String): Int {
            if (a.isEmpty()) return b.length
            if (b.isEmpty()) return a.length
            var previous = IntArray(b.length + 1) { it }
            for (i in a.indices) {
                val current = IntArray(b.length + 1)
                current[0] = i + 1
                for (j in b.indices) {
                    current[j + 1] = minOf(
                        current[j] + 1,
                        previous[j + 1] + 1,
                        previous[j] + if (a[i] == b[j]) 0 else 1
                    )
                }
                previous = current
            }
            return previous[b.length]
        }
        val queryWords = search.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 1 }.toSet()
        categoryProducts.map { product ->
            val name = product.name.lowercase()
            val normalizedName = name.filter { it.isLetterOrDigit() }
            val nameDistance = if (normalizedQuery.isBlank()) 1.0 else
                1.0 - editDistance(normalizedQuery, normalizedName).toDouble() / maxOf(normalizedQuery.length, normalizedName.length, 1)
            val wordMatches = queryWords.count { word -> name.contains(word) || product.subtitle.contains(word, true) }
            val wordScore = if (queryWords.isEmpty()) 0.0 else wordMatches.toDouble() / queryWords.size
            product to maxOf(nameDistance, wordScore * 0.9)
        }.filter { it.second >= 0.25 }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }
    // Match the Categories page's two-tone styling: a white home content header
    // above the same soft-cream product area.
    LazyColumn(
        modifier = modifier.fillMaxSize().background(appThemedColor(Color(0xFFF3F5FA))),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 18.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val greeting = when (appLanguage) {
                        "Telugu" -> if (welcomeNickname.isNotBlank()) "స్వాగతం, $welcomeNickname" else "SLGAN కు స్వాగతం"
                        "Hindi" -> if (welcomeNickname.isNotBlank()) "स्वागत है, $welcomeNickname" else "SLGAN में आपका स्वागत है"
                        else -> if (welcomeNickname.isNotBlank()) "Welcome, $welcomeNickname" else "Welcome to SLGAN"
                    }
                    Text(
                        greeting,
                        modifier = Modifier.weight(1f),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    listOf("Telugu" to "తె", "English" to "E", "Hindi" to "हि").forEach { (language, initial) ->
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(appThemedColor(Color(0xFFEAF2E9)))
                                .clickable(enabled = greetingLoadingLanguage == null) {
                                    greetingLoadingLanguage = language
                                    onLanguageChange(language)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (greetingLoadingLanguage == language) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = Forest,
                                    strokeWidth = 1.7.dp
                                )
                            } else {
                                Text(initial, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text(tr("Rooted in Tradition. Inspired by Nature.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                HomeBannerSlider(products = products, onViewProduct = { productName ->
                    val normalize: (String) -> String = { value -> value.lowercase().filter { it.isLetterOrDigit() } }
                    val match = products.firstOrNull { normalize(it.name) == normalize(productName) }
                        ?: products.firstOrNull { it.name.contains(productName, ignoreCase = true) || productName.contains(it.name, ignoreCase = true) }
                    if (match != null) {
                        onProductClick(match)
                    } else {
                        Toast.makeText(context, "Product details are not available in the database yet.", Toast.LENGTH_SHORT).show()
                    }
                })
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = tr("Search products", LocalAppLanguage.current),
                            modifier = Modifier.padding(start = 4.dp, end = 10.dp).size(22.dp),
                            tint = Muted
                        )
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (search.isBlank()) {
                                Text(tr("Search products", LocalAppLanguage.current), fontSize = 15.sp, color = Muted, maxLines = 1)
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = search,
                                onValueChange = onSearchChange,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp, color = Ink),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Forest)
                            )
                        }
                        IconButton(onClick = {
                            try {
                                val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (currentLanguage) {
                                        "Telugu" -> "te-IN"
                                        "Hindi" -> "hi-IN"
                                        else -> "en-IN"
                                    })
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, when (currentLanguage) {
                                        "Telugu" -> "te-IN"
                                        "Hindi" -> "hi-IN"
                                        else -> "en-IN"
                                    })
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, when (currentLanguage) {
                                        "Telugu" -> "ఉత్పత్తి పేరు చెప్పండి"
                                        "Hindi" -> "उत्पाद का नाम बोलें"
                                        else -> "Speak a product name"
                                    })
                                }
                                voiceSearchLauncher.launch(voiceIntent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, tr("Voice search is not available on this device.", currentLanguage), Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = tr("Voice search", LocalAppLanguage.current), tint = Muted)
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Muted.copy(alpha = 0.45f))
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = appThemedColor(Color.Black), modifier = Modifier.size(20.dp))
                    Text(tr("Shop By Category", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    homeCategories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = {
                                onSearchChange("")
                                onCategoryChange(item)
                            },
                            label = { Text(tr(item, LocalAppLanguage.current), fontSize = 11.sp) },
                            leadingIcon = {
                                Text(
                                    text = when (item.trim().lowercase()) {
                                        "all" -> "🌿"
                                        "lehyas" -> "🍯"
                                        "grutas", "gruta", "ghrita", "ghritas", "ghruta", "ghrutas", "ghrutaa", "ghrutaas", "ghrutam" -> "🏺"
                                        "pills" -> "💊"
                                        "tailas" -> "🫙"
                                        "churnas" -> "🌾"
                                        "rasayogas", "rasayoga" -> "🪷"
                                        "rasayanas" -> "🪴"
                                        "bhasmas" -> "🪨"
                                        else -> "🌿"
                                    },
                                    fontSize = 16.sp
                                )
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = category == item,
                                borderColor = appThemedColor(Color(0xFFE2E2E2)),
                                selectedBorderColor = appThemedColor(Color(0xFFBDBDBD)),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = appThemedColor(Color(0xFFE2E2E2)),
                                selectedLabelColor = Ink,
                                selectedLeadingIconColor = Ink,
                                containerColor = appThemedColor(Color(0xFFF7F7F7)),
                                labelColor = Ink,
                                iconColor = Ink
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (search.isNotBlank()) Text(tr("Search Results", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                if (productsLoading) {
                    Spacer(Modifier.height(18.dp))
                    AppLoader(message = "Loading products…", compact = true)
                }
                if (productsError != null) { Spacer(Modifier.height(8.dp)); Text(tr("Could not load products: $productsError", LocalAppLanguage.current), color = appThemedColor(Color(0xFFB3261E)), fontSize = 12.sp) }
            }
        }
        if (visible.isEmpty() && !productsLoading) item { Text(if (productsError != null) tr("Products could not be loaded. Check your internet and Firestore rules.", LocalAppLanguage.current) else tr("No products found in this category. Try All or another search.", LocalAppLanguage.current), Modifier.padding(24.dp), color = Muted) }
        else items(visible.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { product -> ProductCard(product, cart[product.id] ?: 0, productRatings[product.id] ?: ProductRatingSummary(), productLikes[product.id] ?: ProductLikeSummary(), onToggleLike, onAdd, onProductClick, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 24.dp)) {
                    val signatureProducts = products.filter { it.signature }
                    if (signatureProducts.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = appThemedColor(Color.Black), modifier = Modifier.size(20.dp))
                                Text(tr("Signature Products", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                            }
                            Text("SLGAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(signatureProducts, key = { it.id }) { product ->
                                Card(
                                    modifier = Modifier.width(198.dp).height(285.dp).clickable { onProductClick(product) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Box(
                                            Modifier.fillMaxWidth().height(80.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(appThemedColor(Color(0xFFF7F7F7))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (product.imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = product.imageUrl,
                                                    contentDescription = product.name,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(product.emoji, fontSize = 43.sp)
                                            }
                                            if (product.signature) {
                                                SignatureBadgeRibbon(Modifier.align(Alignment.TopStart))
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
                                                    .padding(horizontal = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                val signatureLike = productLikes[product.id] ?: ProductLikeSummary()
                                                IconButton(onClick = { onToggleLike(product) }, enabled = product.productStock != 0, modifier = Modifier.size(26.dp)) {
                                                    Icon(
                                                        if (signatureLike.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = if (signatureLike.likedByMe) "Unlike product" else "Like product",
                                                        tint = if (signatureLike.likedByMe) appThemedColor(Color(0xFFD32F2F)) else Ink,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(signatureLike.count.toString(), color = if (signatureLike.likedByMe) appThemedColor(Color(0xFFD32F2F)) else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            localizedProductName(product.name, LocalAppLanguage.current),
                                            fontSize = 13.sp,
                                            color = Ink,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            minLines = 2,
                                            lineHeight = 15.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (product.category.isNotBlank() || product.subtitle.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (product.category.isNotBlank()) {
                                                    Surface(
                                                        modifier = Modifier.heightIn(min = 28.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = appThemedColor(Color(0xFFE2E2E2)),
                                                        contentColor = Ink
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.heightIn(min = 28.dp).padding(horizontal = 7.dp, vertical = 0.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Text(
                                                                when (product.category.trim().lowercase()) {
                                                                    "all" -> "🌿"
                                                                    "lehyas" -> "🫙"
                                                                    "grutas", "ghritas", "ghrutas" -> "🏺"
                                                                    "pills" -> "💊"
                                                                    "tailas" -> "🫙"
                                                                    "churnas" -> "🌾"
                                                                    "rasayogas", "rasayoga" -> "🪷"
                                                                    "rasayanas" -> "🪴"
                                                                    "bhasmas" -> "🪨"
                                                                    else -> "🌿"
                                                                },
                                                                fontSize = 9.sp
                                                            )
                                                            Text(tr(product.category, LocalAppLanguage.current), color = appThemedColor(Color.Black), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                                if (product.subtitle.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = appThemedColor(Color(0xFFE2E2E2)),
                                                        contentColor = Ink
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.heightIn(min = 28.dp).padding(horizontal = 7.dp, vertical = 0.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Text("⚖", color = Muted, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Normal)
                                                            Text(product.subtitle.replace(" ", ""), color = appThemedColor(Color.Black), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ProductRatingLine(product, productRatings[product.id] ?: ProductRatingSummary())
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                tr("₹", LocalAppLanguage.current) + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price),
                                                color = appThemedColor(Color.Black),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            val signatureCartCount = cart[product.id] ?: 0
                                            if (signatureCartCount == 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(appThemedColor(Color(0xFF2E7D32)), CircleShape)
                                                        .then(if (product.price > 0.0 && product.productStock != 0) Modifier.clickable { onAdd(product.id) } else Modifier),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add ${product.name} to cart",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    "${signatureCartCount} in cart",
                                                    color = appThemedColor(Color(0xFF2E7D32)),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    modifier = Modifier.clickable { }
                                                )
                                            }
                                        }

                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
            }
        }
        item { Text(tr("Made with care for your everyday rituals.", LocalAppLanguage.current), Modifier.fillMaxWidth().padding(20.dp), color = Ink, fontSize = 12.sp) }
    }
}

@Composable
private fun ProductLikeControl(product: Product, summary: ProductLikeSummary, onToggleLike: (Product) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        IconButton(onClick = { onToggleLike(product) }, enabled = product.productStock != 0, modifier = Modifier.size(30.dp)) {
            Icon(if (summary.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (summary.likedByMe) "Unlike product" else "Like product",
                tint = if (summary.likedByMe) appThemedColor(Color(0xFFD32F2F)) else Muted, modifier = Modifier.size(19.dp))
        }
        Text(summary.count.toString(), color = if (summary.likedByMe) appThemedColor(Color(0xFFD32F2F)) else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProductRatingLine(product: Product, summary: ProductRatingSummary, showEmptyLabel: Boolean = false, compact: Boolean = false, countBaselineFix: Boolean = false) {
    val context = LocalContext.current
    val ratingUser = FirebaseAuth.getInstance().currentUser
    var savingRating by remember(product.id, ratingUser?.uid) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        (1..5).forEach { star ->
            val starSize = if (compact) 10.dp else 20.dp
            Box(
                modifier = Modifier.requiredSize(starSize).clickable(enabled = !savingRating) {
                    val user = ratingUser
                    if (user == null) {
                        Toast.makeText(context, "Please sign in to rate this product.", Toast.LENGTH_SHORT).show()
                    } else if (!savingRating) {
                        savingRating = true
                        val db = FirebaseFirestore.getInstance()
                        val ratingRef = db.collection("productRatings")
                            .document(product.id.replace("/", "_") + "_" + user.uid)
                        ratingRef.get().addOnSuccessListener { existing ->
                            val payload = hashMapOf<String, Any>(
                                "productId" to product.id,
                                "productName" to product.name,
                                "userId" to user.uid,
                                "userName" to (user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty().substringBefore("@")),
                                "rating" to star,
                                "review" to existing.getString("review").orEmpty(),
                                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                            if (!existing.exists()) payload["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                            ratingRef.set(payload, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    savingRating = false
                                    Toast.makeText(context, "Rating saved", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { error ->
                                    savingRating = false
                                    Toast.makeText(context, error.localizedMessage ?: "Could not save rating.", Toast.LENGTH_SHORT).show()
                                }
                        }.addOnFailureListener { error ->
                            savingRating = false
                            Toast.makeText(context, error.localizedMessage ?: "Could not save rating.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (star <= summary.average) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Rate ${product.name} ${star} out of 5 stars",
                    tint = if (star <= summary.average) Gold else Muted,
                    modifier = Modifier.requiredSize(starSize)
                )
            }
        }
        if (summary.count > 0) {
            Box(
                modifier = Modifier.height(if (compact) 12.dp else 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "(${summary.count})",
                    color = Muted,
                    fontSize = if (compact) 10.sp else 12.sp,
                    maxLines = 1,
                    lineHeight = if (compact) 12.sp else 14.sp,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        } else if (showEmptyLabel) {
            Box(
                modifier = Modifier.height(if (compact) 12.dp else 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No ratings",
                    color = Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    lineHeight = 11.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        }
    }
}

@Composable
private fun SignatureBadgeRibbon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = (-25).dp, y = 15.dp)
            .graphicsLayer { rotationZ = -45f }
            .width(112.dp)
            .height(24.dp)
            .shadow(3.dp, RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF174B2A), Color(0xFF2E7D32), Color(0xFF174B2A))
                ),
                RoundedCornerShape(4.dp)
            )
            .border(1.dp, Color(0xFFD7B65B), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🌿  Signature",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ProductCard(product: Product, count: Int, ratingSummary: ProductRatingSummary, likeSummary: ProductLikeSummary, onToggleLike: (Product) -> Unit, onAdd: (String) -> Unit, onProductClick: (Product) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
    Card(Modifier.fillMaxWidth().clickable { onProductClick(product) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(start = 11.dp, top = 11.dp, end = 11.dp, bottom = 4.dp)) {
            Box(Modifier.fillMaxWidth().height(98.dp).clip(RoundedCornerShape(14.dp)).background(appThemedColor(Color(0xFFF7F7F7))), contentAlignment = Alignment.Center) {
                if (product.imageUrl.isNotBlank()) {
                    var imageLoading by remember(product.imageUrl) { mutableStateOf(true) }
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { imageLoading = true },
                        onSuccess = { imageLoading = false },
                        onError = { imageLoading = false }
                    )
                    if (imageLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Ink)
                } else {
                    Text(product.emoji, fontSize = 42.sp)
                }
                if (product.signature) {
                    SignatureBadgeRibbon(Modifier.align(Alignment.TopStart))
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(onClick = { onToggleLike(product) }, enabled = product.productStock != 0, modifier = Modifier.size(26.dp)) {
                        Icon(
                            if (likeSummary.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (likeSummary.likedByMe) "Unlike product" else "Like product",
                            tint = if (likeSummary.likedByMe) appThemedColor(Color(0xFFD32F2F)) else Ink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        likeSummary.count.toString(),
                        color = if (likeSummary.likedByMe) appThemedColor(Color(0xFFD32F2F)) else Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(localizedProductName(product.name, LocalAppLanguage.current), fontSize = 13.sp, lineHeight = 15.sp, color = Ink, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (product.category.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = appThemedColor(Color(0xFFE2E2E2)), contentColor = Ink) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                when (product.category.trim().lowercase()) {
                                    "lehyas" -> "🫙"
                                    "grutas", "ghritas", "ghrutas" -> "🏺"
                                    "pills" -> "💊"
                                    "tailas" -> "🫙"
                                    "churnas" -> "🌾"
                                    "rasayogas", "rasayoga" -> "🪷"
                                    "rasayanas" -> "🪴"
                                    "bhasmas" -> "🪨"
                                    else -> "🌿"
                                },
                                fontSize = 9.sp
                            )
                            Text(tr(product.category, LocalAppLanguage.current), color = appThemedColor(Color.Black), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (product.subtitle.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = appThemedColor(Color(0xFFE2E2E2)), contentColor = Ink) {
                        Row(
                            modifier = Modifier.heightIn(min = 28.dp).padding(horizontal = 6.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("⚖", color = Muted, fontSize = 9.sp, lineHeight = 12.sp)
                            Text(product.subtitle.replace(" ", ""), color = appThemedColor(Color.Black), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductRatingLine(product, ratingSummary)
            }
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("₹", LocalAppLanguage.current) + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price), color = appThemedColor(Color.Black), fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (count == 0) FilledIconButton(onClick = { onAdd(product.id) }, enabled = product.productStock != 0, modifier = Modifier.size(44.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = appThemedColor(Color(0xFF2E7D32)), contentColor = Color.White)) { Icon(Icons.Default.Add, "Add " + product.name, tint = Color.White, modifier = Modifier.size(24.dp)) }
                else Text(count.toString() + tr(" in cart", LocalAppLanguage.current), fontSize = 10.sp, color = appThemedColor(Color(0xFF2E7D32)), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically).clickable(enabled = false) {})
            }
        }
    }

    if (product.productStock == 0) {
        Box(
            Modifier.matchParentSize()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                color = appThemedColor(Color(0xFFF0F0F0)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                shadowElevation = 2.dp
            ) {
                Text(
                    tr("Out of Stock", LocalAppLanguage.current),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailsPage(
    product: Product,
    ratingSummary: ProductRatingSummary,
    likeSummary: ProductLikeSummary,
    onToggleLike: () -> Unit,
    cartCount: Int,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onNavigateTab: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current
    val ratingUser = FirebaseAuth.getInstance().currentUser
    var selectedRating by remember(product.id, ratingUser?.uid) { mutableIntStateOf(ratingSummary.myRating) }
    var ratingReview by remember(product.id, ratingUser?.uid) { mutableStateOf("") }
    var ratingSaving by remember { mutableStateOf(false) }
    var ratingMessage by remember { mutableStateOf<String?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var customerReviews by remember(product.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(product.id) {
        FirebaseFirestore.getInstance().collection("productRatings")
            .whereEqualTo("productId", product.id)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    customerReviews = snapshot.documents.mapNotNull { doc ->
                        val review = doc.getString("review")?.trim().orEmpty()
                        if (review.isBlank()) null else {
                            val name = doc.getString("userName")?.trim().takeUnless { it.isNullOrBlank() } ?: "Customer"
                            name to review
                        }
                    }
                }
            }
    }
    LaunchedEffect(product.id, ratingUser?.uid) {
        selectedRating = ratingSummary.myRating
        ratingReview = ""
        if (ratingUser != null) {
            FirebaseFirestore.getInstance().collection("productRatings")
                .document(product.id.replace("/", "_") + "_" + ratingUser.uid).get()
                .addOnSuccessListener { doc ->
                    selectedRating = (doc.getLong("rating")?.toInt() ?: ratingSummary.myRating).coerceIn(0, 5)
                    ratingReview = doc.getString("review").orEmpty()
                }
        }
    }
    var selectedDetailsTab by remember(product.id) { mutableStateOf("About") }
    Column(Modifier.fillMaxSize().background(appThemedColor(Color(0xFFF4F5FA)))) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(360.dp).background(appThemedColor(Color(0xFFF7F7F7)))) {
                    if (product.imageUrl.isNotBlank()) {
                        var detailImageLoading by remember(product.imageUrl) { mutableStateOf(true) }
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxWidth(1.3f).fillMaxHeight().align(Alignment.Center),
                            contentScale = ContentScale.Crop,
                            onLoading = { detailImageLoading = true },
                            onSuccess = { detailImageLoading = false },
                            onError = { detailImageLoading = false }
                        )
                        if (detailImageLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Ink)
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(appThemedColor(Color(0xFFF1F2F4))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(product.emoji, fontSize = 112.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 22.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.94f), CircleShape)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back to products", tint = Color.Black) }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onBack() /* product search remains available on the previous page */ }, modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.94f), CircleShape)) { Icon(Icons.Default.Search, contentDescription = "Back to product search", tint = Color.Black) }
                        Spacer(Modifier.width(12.dp))
                        IconButton(onClick = {
                            val productLink = "https://slgan-a364e.web.app/product?id=${Uri.encode(product.id)}"
                            val shareText = buildString {
                                append("Check out this product from Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN)\n\n")
                                append(localizedProductName(product.name, currentLanguage))
                                if (product.subtitle.isNotBlank()) append("\nPack size: ${product.subtitle}")
                                if (product.price > 0.0) append("\nPrice: ₹${String.format("%.2f", product.price)}")
                                append("\n\nView product: $productLink")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                            context.startActivity(Intent.createChooser(shareIntent, tr("Share product", currentLanguage)))
                        }, modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.94f), CircleShape)) { Icon(Icons.Default.Share, contentDescription = tr("Share product", LocalAppLanguage.current), tint = Color.Black) }
                    }
                    IconButton(onClick = { showReviewDialog = true }, modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 22.dp).size(48.dp).background(appThemedColor(Color.White), CircleShape)) {
                        Icon(Icons.Default.Info, contentDescription = tr("Product Information", LocalAppLanguage.current), tint = Ink)
                    }
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(50.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { dot -> Box(Modifier.size(if (dot == 2) 8.dp else 7.dp).background(if (dot == 2) Ink else appThemedColor(Color(0xFFB7BEC9)), CircleShape)) }
                        }
                    }
                    IconButton(onClick = onToggleLike, enabled = product.productStock != 0, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 22.dp).size(48.dp).background(appThemedColor(Color.White), CircleShape)) {
                        Icon(if (likeSummary.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = tr("Save product", LocalAppLanguage.current), tint = if (likeSummary.likedByMe) appThemedColor(Color(0xFFE91E63)) else Ink)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(localizedProductName(product.name, currentLanguage), color = Ink, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (product.subtitle.isNotBlank()) "${tr("Net quantity", currentLanguage)}: ${product.subtitle}" else "${tr("Net quantity", currentLanguage)}: ${tr("See product pack", currentLanguage)}", color = Muted, fontSize = 15.sp)
                            if (product.price > 0.0) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price), color = Ink, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp).background(appThemedColor(Color(0xFFE2E2E2)), RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
                                    Spacer(Modifier.weight(1f))
                                    ProductRatingLine(product, ratingSummary, showEmptyLabel = true)
                                }
                                Text(tr("Price inclusive of applicable taxes", currentLanguage), color = Muted, fontSize = 13.sp)
                            } else Text(tr("Price not available yet.", currentLanguage), color = Muted, fontSize = 15.sp)
                        }
                    }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                        Column(Modifier.fillMaxWidth().padding(0.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val detailTabs = listOf(
                                Triple("About", Icons.Default.Info, "About"),
                                Triple("Cures", Icons.Default.Eco, "Cures"),
                                Triple("Major ingredients", Icons.Default.Spa, "Major ingredients"),
                                Triple("Usage", Icons.Default.Description, "Usage")
                            )
                            val detailContent = when (selectedDetailsTab) {
                                "Cures" -> product.cures
                                "Major ingredients" -> product.majorIngredients
                                "Usage" -> product.usage
                                else -> product.description
                            }
                            Column(Modifier.fillMaxWidth()) {
                                Box {
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                                            .background(appThemedColor(Color(0xFFEAF2E9))).padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        detailTabs.forEach { (tab, icon, label) ->
                                            val selected = selectedDetailsTab == tab
                                            Box(Modifier.weight(1f)) {
                                                Column(
                                                    Modifier.fillMaxWidth().heightIn(min = 58.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(appThemedColor(if (selected) Color(0xFF4F8B45) else Color.Transparent))
                                                        .clickable { selectedDetailsTab = tab }
                                                        .padding(horizontal = 3.dp, vertical = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(icon, contentDescription = label, tint = if (selected) Color.White else Ink, modifier = Modifier.size(19.dp))
                                                    Text(tr(label, currentLanguage), color = if (selected) Color.White else Ink, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 2, lineHeight = 12.sp, textAlign = TextAlign.Center)
                                                }
                                                if (selected) Box(Modifier.align(Alignment.BottomCenter).offset(y = 17.dp).size(30.dp).background(appThemedColor(Color.White), CircleShape))
                                            }
                                        }
                                    }
                                }
                                Column(
                                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 190.dp)
                                        .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 0.dp, topEnd = 0.dp))
                                        .background(appThemedColor(Color.White))
                                        .border(1.dp, appThemedColor(Color(0xFFC9DDC3)), RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 0.dp, topEnd = 0.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(tr(selectedDetailsTab, currentLanguage), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(detailContent.ifBlank { tr("Information not available.", currentLanguage) }, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                                    if (selectedDetailsTab == "About" && product.description.isNotBlank()) Text(tr("Product Information", LocalAppLanguage.current), color = Muted, fontSize = 11.sp)
                                }
                            }
                            Row(Modifier.fillMaxWidth().clickable { showReviewDialog = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(tr("Customer Ratings & Reviews", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                            }
                            if (customerReviews.isNotEmpty()) customerReviews.take(3).forEach { (customerName, reviewText) ->
                                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(customerName, color = appThemedColor(Color(0xFF2E7D32)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(reviewText, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(52.dp).border(1.dp, appThemedColor(Color(0xFFD5DCE7)), RoundedCornerShape(12.dp)).clickable(onClick = onCartClick).padding(6.dp), contentAlignment = Alignment.Center) {
                BadgedBox(badge = { if (cartCount > 0) Badge(containerColor = appThemedColor(Color(0xFF2E7D32)), contentColor = Color.White) { Text(cartCount.toString(), color = Color.White) } }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = tr("View cart", LocalAppLanguage.current), tint = appThemedColor(Color.Black), modifier = Modifier.size(24.dp))
                }
            }
            Button(onClick = { if (product.price > 0.0) onAdd(product.id) }, enabled = product.price > 0.0 && product.productStock != 0, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, appThemedColor(Color(0xFFD5DCE7))), colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color.White), contentColor = Ink, disabledContainerColor = Color.White, disabledContentColor = Ink)) {
                Text(if (product.productStock == 0) tr("Out of stock", LocalAppLanguage.current) else if (cartCount > 0) tr("Add another", LocalAppLanguage.current) else tr("Add to Cart", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        NavigationBar(
            modifier = Modifier.fillMaxWidth().shadow(8.dp),
            containerColor = appThemedColor(Color.White),
            contentColor = Forest
        ) {
            listOf(
                Triple("Home", Icons.Default.Home, "Home"),
                Triple("Diseases", Icons.Default.MedicalInformation, "Diseases"),
                Triple("Categories", Icons.Default.GridView, "Categories"),
                Triple("Orders", Icons.Default.ReceiptLong, "Orders"),
                Triple("Cart", Icons.Default.ShoppingCart, "Cart")
            ).forEach { (label, icon, route) ->
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateTab(route) },
                    icon = {
                        if (route == "Cart") {
                            BadgedBox(badge = {
                                if (cartCount > 0) Badge(
                                    containerColor = appThemedColor(Color(0xFF27864A)),
                                    contentColor = Color.White
                                ) { Text(cartCount.toString()) }
                            }) { Icon(icon, contentDescription = label) }
                        } else {
                            Icon(icon, contentDescription = label)
                        }
                    },
                    label = { Text(tr(label, currentLanguage), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Ink,
                        selectedTextColor = Ink,
                        indicatorColor = appThemedColor(Color(0xFFE6E8EC)),
                        unselectedIconColor = Ink,
                        unselectedTextColor = Ink
                    )
                )
            }
        }
    }
            if (showReviewDialog) {
                ModalBottomSheet(
                    onDismissRequest = { if (!ratingSaving) showReviewDialog = false },
                    containerColor = appThemedColor(Color.White),
                    contentColor = Ink,
                    scrimColor = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 8.dp).size(width = 38.dp, height = 4.dp).background(appThemedColor(Color(0xFFD7D9DE)), CircleShape)) }
                ) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(46.dp), shape = CircleShape, color = appThemedColor(Color(0xFFE7F3E7))) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = appThemedColor(Color(0xFF26743A)), modifier = Modifier.size(25.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tr("Write A Review", LocalAppLanguage.current), color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                                Text(localizedProductName(product.name, currentLanguage), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { if (!ratingSaving) showReviewDialog = false }, modifier = Modifier.size(40.dp).background(appThemedColor(Color(0xFFF0F2F0)), CircleShape)) {
                                Icon(Icons.Default.Close, contentDescription = tr("Close review", LocalAppLanguage.current), tint = Muted)
                            }
                        }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(appThemedColor(Color(0xFFF1F8F1))).padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { star ->
                                    IconButton(onClick = { selectedRating = star; ratingMessage = null }, enabled = !ratingSaving, modifier = Modifier.size(48.dp)) {
                                        Icon(if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = tr("$star stars", LocalAppLanguage.current), tint = if (star <= selectedRating) appThemedColor(Color(0xFFE2A928)) else appThemedColor(Color(0xFF8B948D)), modifier = Modifier.size(34.dp))
                                    }
                                }
                            }
                            Text(if (selectedRating > 0) tr("$selectedRating out of 5", LocalAppLanguage.current) else tr("Tap to rate", LocalAppLanguage.current), color = appThemedColor(Color(0xFF365C40)), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedTextField(
                            value = ratingReview,
                            onValueChange = { if (it.length <= 1000) ratingReview = it; ratingMessage = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Your review", LocalAppLanguage.current) + " *") },
                            placeholder = { Text(tr("Share your experience with this product", LocalAppLanguage.current)) },
                            minLines = 4, maxLines = 6, shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = appThemedColor(Color(0xFF2E7D32)), unfocusedBorderColor = appThemedColor(Color(0xFF9AA99C)), focusedLabelColor = appThemedColor(Color(0xFF26743A)), cursorColor = appThemedColor(Color(0xFF26743A)))
                        )
                        Text(ratingReview.length.toString() + "/1000", modifier = Modifier.fillMaxWidth().padding(end = 4.dp), textAlign = TextAlign.End, color = Muted, fontSize = 11.sp)
                        ratingMessage?.let { Text(it, color = if (it.startsWith("Saved")) Forest else appThemedColor(Color(0xFFB3261E)), fontSize = 12.sp) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { if (!ratingSaving) showReviewDialog = false }, enabled = !ratingSaving, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, appThemedColor(Color(0xFF2E7D32))), colors = ButtonDefaults.outlinedButtonColors(contentColor = appThemedColor(Color(0xFF205C2D)))) {
                                Text(tr("Cancel", LocalAppLanguage.current), fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    val user = ratingUser
                                    if (user == null) ratingMessage = "Please sign in to submit a review."
                                    else if (selectedRating !in 1..5) ratingMessage = "Please select a star rating."
                                    else if (ratingReview.isBlank()) ratingMessage = "Please enter your review."
                                    else {
                                        ratingSaving = true
                                        val db = FirebaseFirestore.getInstance()
                                        val ratingRef = db.collection("productRatings").document(product.id.replace("/", "_") + "_" + user.uid)
                                        ratingRef.get().addOnSuccessListener { existing ->
                                            val payload = hashMapOf<String, Any>(
                                                "productId" to product.id, "productName" to product.name, "userId" to user.uid,
                                                "userName" to (user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty().substringBefore("@")),
                                                "rating" to selectedRating, "review" to ratingReview.trim(),
                                                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                                            if (!existing.exists()) payload["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                                            ratingRef.set(payload, com.google.firebase.firestore.SetOptions.merge())
                                                .addOnSuccessListener { ratingSaving = false; ratingMessage = "Saved! Thank you for your review."; showReviewDialog = false; Toast.makeText(context, "Review saved successfully", Toast.LENGTH_SHORT).show() }
                                                .addOnFailureListener { error -> ratingSaving = false; ratingMessage = error.localizedMessage ?: "Could not save rating. Please try again." }
                                        }.addOnFailureListener { error -> ratingSaving = false; ratingMessage = error.localizedMessage ?: "Could not save rating. Please try again." }
                                    }
                                },
                                enabled = !ratingSaving, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color(0xFF26743A)), contentColor = Color.White)
                            ) {
                                if (ratingSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text(tr("Save", LocalAppLanguage.current), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartPage(modifier: Modifier, cart: Map<String, Int>, products: List<Product>, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onOrderResult: (String, Boolean, String?, String) -> Unit, onProductClick: (Product) -> Unit, onAddAddress: () -> Unit, onClearCart: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val selected = products.filter { (cart[it.id] ?: 0) > 0 }
    val total = selected.sumOf { it.price * (cart[it.id] ?: 0) }
    var showCheckout by remember { mutableStateOf(false) }
    var showClearCartConfirmation by remember { mutableStateOf(false) }
    var savedAddresses by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedDeliveryAddressId by remember { mutableStateOf<String?>(null) }
    var confirmedDeliveryAddress by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showDeliveryAddressDetails by remember { mutableStateOf(false) }
    var loadingDeliveryAddresses by remember { mutableStateOf(false) }
    val signedInUser = FirebaseAuth.getInstance().currentUser
    var customerName by remember(signedInUser?.uid) { mutableStateOf(signedInUser?.displayName.orEmpty()) }
    var email by remember(signedInUser?.uid) { mutableStateOf(signedInUser?.email.orEmpty()) }
    var phone by remember(signedInUser?.uid) { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var orderMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(signedInUser?.uid) {
        val user = signedInUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("customers").document(user.uid).get()
                .addOnSuccessListener { snapshot ->
                    val savedName = listOf(snapshot.getString("firstName"), snapshot.getString("lastName"))
                        .filterNotNull().map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
                    val nickname = snapshot.getString("nickname").orEmpty().trim()
                    val savedPhone = snapshot.getString("mobileNumber").orEmpty().trim()
                    if (customerName.isBlank()) customerName = savedName.ifBlank { nickname }.ifBlank { user.displayName.orEmpty() }
                    if (phone.isBlank()) phone = savedPhone
                    if (email.isBlank()) email = user.email.orEmpty()
                }
        }
    }

    LaunchedEffect(signedInUser?.uid) {
        val user = signedInUser ?: return@LaunchedEffect
        loadingDeliveryAddresses = true
        FirebaseFirestore.getInstance().collection("addresses")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val addresses = snapshot.documents.map { doc -> doc.data.orEmpty() + ("_id" to doc.id) }
                savedAddresses = addresses
                if (confirmedDeliveryAddress == null) {
                    val homeAddress = addresses.firstOrNull { it["label"]?.toString()?.equals("Home", ignoreCase = true) == true }
                    val preferred = homeAddress
                        ?: addresses.firstOrNull { it["isDefault"] == true || it["default"] == true }
                        ?: addresses.firstOrNull()
                    if (homeAddress != null && (homeAddress["isDefault"] != true || homeAddress["default"] != true)) {
                        val batch = FirebaseFirestore.getInstance().batch()
                        addresses.forEach { item ->
                            val id = item["_id"]?.toString().orEmpty()
                            if (id.isNotBlank()) {
                                val makeDefault = id == homeAddress["_id"]?.toString()
                                batch.update(FirebaseFirestore.getInstance().collection("addresses").document(id),
                                    "isDefault", makeDefault, "default", makeDefault)
                            }
                        }
                        batch.commit()
                    }
                    if (preferred != null) {
                        confirmedDeliveryAddress = preferred
                        selectedDeliveryAddressId = preferred["_id"]?.toString()
                        customerName = preferred["name"]?.toString().orEmpty()
                            .ifBlank { preferred["fullName"]?.toString().orEmpty() }
                            .ifBlank { customerName }
                        phone = preferred["phone"]?.toString().orEmpty().ifBlank { phone }
                    }
                }
                loadingDeliveryAddresses = false
            }
            .addOnFailureListener { loadingDeliveryAddresses = false }
    }

    if (showClearCartConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearCartConfirmation = false },
            title = { Text(tr("Clear your cart?", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold) },
            text = { Text(tr("This will remove all ${selected.sumOf { cart[it.id] ?: 0 }} items from your cart. You can add them again later.", LocalAppLanguage.current), color = Muted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCartConfirmation = false
                        onClearCart()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Ink)
                ) { Text(tr("Clear cart", LocalAppLanguage.current), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearCartConfirmation = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Muted)
                ) { Text(tr("Cancel", LocalAppLanguage.current)) }
            },
            containerColor = appThemedColor(Color.White)
        )
    }

    var selectedInstruction by remember { mutableStateOf("") }
    var userInstructions by remember { mutableStateOf("") }
    var userInstructionsEnabled by remember { mutableStateOf(false) }
    val recommendations = remember(products, cart) { products.filter { (cart[it.id] ?: 0) == 0 && it.productStock != 0 }.take(6) }
    val itemCount = selected.sumOf { cart[it.id] ?: 0 }
    val money: (Double) -> String = { amount -> "₹" + String.format(java.util.Locale("en", "IN"), "%.0f", amount) }

    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF3F5FA)))) {
        Row(Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).border(1.dp, appThemedColor(Color(0xFFE8EAF0)), CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBack, contentDescription = tr("Back", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(tr("Checkout", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(24.dp), color = appThemedColor(Color(0xFFF6F7FA))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = appThemedColor(Color(0xFF27864A)), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("{count} items", LocalAppLanguage.current).replace("{count}", itemCount.toString()), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
        if (selected.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(100.dp).background(appThemedColor(Color.White), CircleShape), contentAlignment = Alignment.Center) { Text(tr("🛍️", LocalAppLanguage.current), fontSize = 48.sp) }
                Spacer(Modifier.height(18.dp))
                Text(tr("Your Cart Is Empty", LocalAppLanguage.current), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tr("Explore our Ayurvedic products and add your favourites.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(appThemedColor(Color(0xFFE1E5EC)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(26.dp).background(appThemedColor(Color(0xFF237344)), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tr("Your Ayurvedic Order", LocalAppLanguage.current), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                Text(tr("Review cart before placing order", LocalAppLanguage.current).replace("{count}", selected.size.toString()), color = Muted, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { showClearCartConfirmation = true },
                                modifier = Modifier.size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(appThemedColor(Color(0xFFE2E2E2)))
                                    .border(1.dp, appThemedColor(Color(0xFFB5B5B5)), RoundedCornerShape(14.dp))
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = tr("Clear cart", LocalAppLanguage.current),
                                    tint = Ink,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Divider(color = appThemedColor(Color(0xFFEFF1F5)))
                        selected.forEachIndexed { index, product ->
                            val quantity = cart[product.id] ?: 0
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (product.imageUrl.isNotBlank()) {
                                    AsyncImage(model = product.imageUrl, contentDescription = product.name, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFF6F7FA))), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFF1F2F4))), contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 26.sp) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(localizedProductName(product.name, LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(2.dp))
                                    Text(product.subtitle.ifBlank { product.quantity }, color = Muted, fontSize = 13.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(tr("Save for later", LocalAppLanguage.current), color = Muted, fontSize = 12.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(Modifier.background(appThemedColor(Color(0xFFE1E5EC)), RoundedCornerShape(10.dp)).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onRemove(product.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, contentDescription = tr("Decrease quantity", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(16.dp)) }
                                        Text(quantity.toString(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        IconButton(onClick = { onAdd(product.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, contentDescription = tr("Increase quantity", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(16.dp)) }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(money(product.price * quantity), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index < selected.lastIndex) Divider(color = appThemedColor(Color(0xFFEFF1F5)))
                        }
                    }
                }
                if (recommendations.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                        Column(Modifier.padding(14.dp)) {
                            Text(tr("You Might Also Like", LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(end = 2.dp)
                            ) {
                                items(recommendations.take(10), key = { it.id }) { product ->
                                    Column(Modifier.width(145.dp).clickable { onProductClick(product) }) {
                                        if (product.imageUrl.isNotBlank()) AsyncImage(model = product.imageUrl, contentDescription = product.name, modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFF5F6FA))), contentScale = ContentScale.Crop)
                                        else Box(Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFF1F2F4))), contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 35.sp) }
                                        Spacer(Modifier.height(7.dp))
                                        Text(localizedProductName(product.name, LocalAppLanguage.current), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                                        Text(money(product.price), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        Spacer(Modifier.height(5.dp))
                                        Surface(
                                            onClick = { onAdd(product.id) },
                                            enabled = product.price > 0.0 && product.productStock > 0,
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = appThemedColor(Color(0xFFE1E5EC)),
                                            contentColor = Ink
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(tr("ADD", LocalAppLanguage.current), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(tr("Bill Details", LocalAppLanguage.current), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Items total", LocalAppLanguage.current), color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(money(total), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Delivery charge", LocalAppLanguage.current), color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(tr("Confirmed at order", LocalAppLanguage.current), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Divider(color = appThemedColor(Color(0xFFEFF1F5)))
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Grand Total", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Text(money(total), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(tr("No online payment is collected in this app.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr("Instructions", LocalAppLanguage.current),
                                color = Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { userInstructionsEnabled = !userInstructionsEnabled },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = if (userInstructionsEnabled) "Hide instructions" else "Show instructions",
                                    tint = appThemedColor(if (userInstructionsEnabled) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                                )
                            }
                        }
                        if (userInstructionsEnabled) {
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = userInstructions,
                                onValueChange = { userInstructions = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(tr("Add any special instructions for your order", LocalAppLanguage.current)) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appThemedColor(Color(0xFFB5B5B5)),
                                    unfocusedBorderColor = appThemedColor(Color(0xFFB5B5B5)),
                                    focusedTextColor = Ink,
                                    unfocusedTextColor = Ink,
                                    cursorColor = Ink
                                )
                            )
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(tr("Delivery Instructions", LocalAppLanguage.current), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(10.dp))
                        listOf("Call before delivery", "Avoid calling").forEach { instruction ->
                            Row(Modifier.fillMaxWidth().clickable { selectedInstruction = if (selectedInstruction == instruction) "" else instruction }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (selectedInstruction == instruction) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, contentDescription = null, tint = if (selectedInstruction == instruction) appThemedColor(Color(0xFF7B818A)) else appThemedColor(Color(0xFFD1D5DB)))
                                Spacer(Modifier.width(10.dp))
                                Text(instruction, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            }
                        }
                        if (userInstructions.isNotBlank() && !userInstructionsEnabled) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                tr("Your instruction", LocalAppLanguage.current),
                                color = Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                userInstructions.trim(),
                                color = Ink,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(tr("Ordering For Someone Else?", LocalAppLanguage.current), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Recipient details can be added at checkout.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(tr("Cancellation Policy", LocalAppLanguage.current), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Orders are saved as pending verification. Please contact the shop for changes after placing an order.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(tr("Product Design", LocalAppLanguage.current), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Product images, bottle designs and packaging shown in the app are for illustration and design reference only. The actual product appearance, bottle shape, label, colour and packaging may vary from the images displayed. The contents and product details will correspond to the product supplied.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
            if (confirmedDeliveryAddress != null) {
                val address = confirmedDeliveryAddress!!
                val completeAddress = listOf("completeAddress", "complete_address", "formattedAddress", "formatted_address", "fullAddress", "full_address")
                    .firstNotNullOfOrNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
                val addressKeys = listOf(
                    "address", "addressText", "addressLine1", "addressLine2", "houseNo", "houseNumber",
                    "building", "apartment", "street", "streetName", "road", "landmark", "area", "locality",
                    "village", "town", "city", "district", "state", "pincode", "postalCode", "zipCode", "country"
                )
                val addressParts = addressKeys.mapNotNull { key ->
                    address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
                }.distinctBy { it.lowercase(Locale.ROOT) }
                val confirmedText = (listOf(completeAddress).filter { it.isNotBlank() } + addressParts)
                    .distinctBy { it.lowercase(Locale.ROOT) }
                    .fold(mutableListOf<String>()) { result, part ->
                        if (result.none { it.contains(part, ignoreCase = true) || part.contains(it, ignoreCase = true) }) result.add(part)
                        result
                    }.joinToString(", ")
                val recipientName = address["name"]?.toString().orEmpty()
                    .ifBlank { address["fullName"]?.toString().orEmpty() }
                    .ifBlank { address["receiverName"]?.toString().orEmpty() }
                    .ifBlank { customerName }
                val recipientMobile = address["phone"]?.toString().orEmpty()
                    .ifBlank { address["mobile"]?.toString().orEmpty() }
                    .ifBlank { address["receiverPhone"]?.toString().orEmpty() }
                    .ifBlank { phone }
                val addressLabel = listOf("label", "addressLabel", "addressType", "type", "tag")
                    .firstNotNullOfOrNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                    .orEmpty()
                    .ifBlank { tr("Address", LocalAppLanguage.current) }
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                    border = BorderStroke(1.dp, appThemedColor(Color(0xFFE2E5EA)))
                ) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            buildString {
                                append(tr("Deliver to", LocalAppLanguage.current))
                                append(" ")
                                append(addressLabel)
                            },
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { showDeliveryAddressDetails = !showDeliveryAddressDetails }) {
                            Icon(
                                if (showDeliveryAddressDetails) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (showDeliveryAddressDetails) "Hide delivery address" else "Show delivery address",
                                tint = appThemedColor(Color(0xFF238B3B))
                            )
                        }
                    }
                AnimatedVisibility(
                    visible = showDeliveryAddressDetails,
                    enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                ) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF7FDEB), Color(0xFFE5F7D5), Color(0xFFD5F0BD))))
                        .border(1.dp, appThemedColor(Color(0xFF62A94A)), RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.delivery_address_bg),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).width(170.dp).height(85.dp),
                        contentScale = ContentScale.Fit,
                        alpha = 0.72f
                    )
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = appThemedColor(Color(0xFF245B32)), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(recipientName, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    val user = FirebaseAuth.getInstance().currentUser
                                    if (user == null) {
                                        errorMessage = "Please sign in again before choosing an address."
                                    } else {
                                        errorMessage = null
                                        loadingDeliveryAddresses = true
                                        showCheckout = true
                                        FirebaseFirestore.getInstance().collection("addresses").whereEqualTo("userId", user.uid).get()
                                            .addOnSuccessListener { snapshot ->
                                                savedAddresses = snapshot.documents.map { doc -> doc.data.orEmpty() + ("_id" to doc.id) }
                                                if (selectedDeliveryAddressId == null || savedAddresses.none { it["_id"]?.toString() == selectedDeliveryAddressId }) {
                                                    selectedDeliveryAddressId = (confirmedDeliveryAddress?.get("_id")?.toString()
                                                        ?: savedAddresses.firstOrNull { it["isDefault"] == true || it["default"] == true }?.get("_id")?.toString()
                                                        ?: savedAddresses.firstOrNull()?.get("_id")?.toString())
                                                }
                                                loadingDeliveryAddresses = false
                                            }
                                            .addOnFailureListener { error ->
                                                loadingDeliveryAddresses = false
                                                errorMessage = error.localizedMessage ?: "Could not load saved addresses."
                                            }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 3.dp),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, appThemedColor(Color(0xFF238B3B))),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = appThemedColor(Color(0xFF175F2B)))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(tr("Change", LocalAppLanguage.current), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = appThemedColor(Color(0xFF245B32)), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(recipientMobile, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Normal)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = appThemedColor(Color(0xFF245B32)), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(confirmedText.ifBlank { tr("Complete address not available", LocalAppLanguage.current) }, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
                }

                    }
                }
            } else {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, appThemedColor(Color(0xFFE2E5EA)))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Ink)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("No address added", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = onAddAddress) {
                            Text(tr("Add", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(appThemedColor(Color.White))
                    .border(1.dp, appThemedColor(Color(0xFFE2E2E2)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(0.72f)) {
                    Text(money(total), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(tr("TOTAL", LocalAppLanguage.current), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.width(1.dp).height(42.dp).background(appThemedColor(Color(0xFFB5B5B5))))
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    errorMessage = null
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        errorMessage = "Please sign in again before placing an order."
                    } else if (confirmedDeliveryAddress == null) {
                        errorMessage = "Please choose a delivery address using Change before placing your order."
                    } else if (customerName.isBlank() || !email.contains("@")) {
                        errorMessage = "Enter your name and a valid email address."
                    } else {
                        submitting = true
                        val userForOrder = FirebaseAuth.getInstance().currentUser
                        if (userForOrder == null) {
                            submitting = false
                            errorMessage = "Please sign in again before placing an order."
                        } else {
                            val address = confirmedDeliveryAddress!!
                            val addressText = listOf("addressLine1", "addressLine2", "landmark", "city", "state", "pincode")
                                .mapNotNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                                .joinToString(", ")
                                .ifBlank { (address["address"] ?: address["addressText"] ?: address["fullAddress"] ?: address["completeAddress"] ?: address["formattedAddress"])?.toString().orEmpty() }
                            if (addressText.isBlank()) {
                                submitting = false
                                errorMessage = "The selected address has no address details. Please edit or choose another address."
                            } else {
                                val orderId = "SLGAN-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
                                val itemPayload = selected.map { product ->
                                    mapOf("productId" to product.id, "productName" to product.name, "unitPrice" to product.price, "quantity" to (cart[product.id] ?: 0), "lineTotal" to (product.price * (cart[product.id] ?: 0)))
                                }
                                val now = com.google.firebase.Timestamp.now()
                                val orderFields = mapOf<String, Any>(
                                    "orderId" to orderId,
                                    "customerId" to userForOrder.uid,
                                    "customerName" to customerName.trim(),
                                    "email" to email.trim(),
                                    "phone" to phone.trim().ifBlank { address["phone"]?.toString().orEmpty() },
                                    "deliveryAddressId" to address["_id"]?.toString().orEmpty(),
                                    "deliveryAddress" to addressText,
                                    "shippingAddress" to addressText,
                                    "deliveryAddressLabel" to address["label"]?.toString().orEmpty(),
                                    "deliveryRecipientName" to address["name"]?.toString().orEmpty().ifBlank { address["fullName"]?.toString().orEmpty() },
                                    "deliveryPhone" to address["phone"]?.toString().orEmpty(),
                                    "deliveryInstruction" to selectedInstruction,
                                    "user_instructions" to userInstructions.trim(),
                                    "items" to itemPayload,
                                    "subtotal" to total,
                                    "grandTotal" to total,
                                    "currency" to "INR",
                                    "paymentStatus" to "not_collected",
                                    "orderStatus" to "pending_verification",
                                    "invoiceNumber" to orderId,
                                    "invoiceStatus" to "not_generated",
                                    "createdAt" to now,
                                    "updatedAt" to now
                                )
                                FirebaseFirestore.getInstance().collection("orders").document(orderId).set(orderFields)
                                    .addOnSuccessListener {
                                        orderMessage = "Order saved: $orderId"
                                        submitting = false
                                        confirmedDeliveryAddress = null
                                        onOrderResult(orderId, true, null, "Order saved. Opening secure Cashfree checkout…")
                                        context.startActivity(
                                            android.content.Intent(context, CashfreePaymentActivity::class.java)
                                                .putExtra(CashfreePaymentActivity.EXTRA_DOCUMENT_ID, orderId)
                                        )
                                        userForOrder.getIdToken(false).addOnSuccessListener { tokenResult ->
                                            val idToken = tokenResult.token
                                            if (!idToken.isNullOrBlank()) requestSlganOrderEmail(idToken, orderId) { emailSent, detail ->
                                                orderMessage = if (emailSent) "Order saved: $orderId. Email service confirmed the request." else "Order saved: $orderId. Email failed: $detail"
                                                Toast.makeText(context, if (emailSent) "Order saved. Email service confirmed." else "Order saved, but email failed. " + detail.take(160), Toast.LENGTH_LONG).show()
                                            }
                                        }.addOnFailureListener { error -> orderMessage = "Order saved: $orderId. Email failed: ${error.localizedMessage ?: "Could not get Firebase ID token."}" }
                                    }
                                    .addOnFailureListener { error ->
                                        submitting = false
                                        errorMessage = error.localizedMessage ?: "Could not save order. Please try again."
                                        onOrderResult("", false, null, errorMessage.orEmpty())
                                    }
                            }
                        }
                    }
                }, modifier = Modifier
                    .weight(1.3f)
                    .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appThemedColor(Color(0xFFE2E2E2)),
                        contentColor = Ink,
                        disabledContainerColor = appThemedColor(Color(0xFFE2E2E2)),
                        disabledContentColor = Muted
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 1.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            color = DefaultBtn.content,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(width = 28.dp, height = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = DefaultBtn.content,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Place order", LocalAppLanguage.current),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DefaultBtn.content
                        )
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = DefaultBtn.content,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCheckout) {
        ModalBottomSheet(
            onDismissRequest = { if (!submitting) showCheckout = false },
            containerColor = appThemedColor(Color.White),
            contentColor = Ink,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Choose Delivery Address", LocalAppLanguage.current), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(tr("Select the saved address for this order.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                    if (loadingDeliveryAddresses) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { AppLoader("Loading addresses…", compact = true) }
                    } else if (savedAddresses.isEmpty()) {
                        Text(tr("No saved addresses found. Add a delivery address from your profile before placing an order.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                    } else savedAddresses.forEach { address ->
                        val addressId = address["_id"]?.toString().orEmpty()
                        val label = address["label"]?.toString().orEmpty().ifBlank { "Delivery address" }
                        val completeAddress = listOf("completeAddress", "complete_address", "formattedAddress", "formatted_address", "fullAddress", "full_address")
                            .firstNotNullOfOrNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
                        val addressKeys = listOf(
                            "address", "addressText", "addressLine1", "addressLine2", "houseNo", "houseNumber",
                            "building", "apartment", "street", "streetName", "road", "landmark", "area", "locality",
                            "village", "town", "city", "district", "state", "pincode", "postalCode", "zipCode", "country"
                        )
                        val addressParts = addressKeys.mapNotNull { key ->
                            address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
                        }.distinctBy { it.lowercase(Locale.ROOT) }
                        val addressText = (listOf(completeAddress).filter { it.isNotBlank() } + addressParts)
                            .distinctBy { it.lowercase(Locale.ROOT) }
                            .fold(mutableListOf<String>()) { result, part ->
                                if (result.none { it.contains(part, ignoreCase = true) || part.contains(it, ignoreCase = true) }) result.add(part)
                                result
                            }.joinToString(", ")
                        val addressName = listOf("name", "fullName", "receiverName", "recipientName", "customerName")
                            .firstNotNullOfOrNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                            .orEmpty().ifBlank { FirebaseAuth.getInstance().currentUser?.displayName?.trim().orEmpty() }
                            .ifBlank { "Name not provided" }
                        val addressPhone = listOf("phone", "mobile", "receiverPhone", "mobileNumber", "phoneNumber")
                            .firstNotNullOfOrNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                            .orEmpty().ifBlank { FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty() }
                        val isSelected = selectedDeliveryAddressId == addressId
                        Card(Modifier.fillMaxWidth().clickable { selectedDeliveryAddressId = addressId }, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (isSelected) appThemedColor(Color(0xFFB5B5B5)) else appThemedColor(Color(0xFFE2E5EA))), colors = CardDefaults.cardColors(containerColor = if (isSelected) appThemedColor(Color(0xFFF4F8F2)) else Color.White)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                                Icon(if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isSelected) appThemedColor(Color(0xFF2E7D32)) else Muted)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(label, color = Ink, fontWeight = FontWeight.Bold)
                                    Text(addressName, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    if (addressPhone.isNotBlank()) Text(addressPhone, color = Muted, fontSize = 13.sp)
                                    Text(addressText.ifBlank { tr("Address details unavailable", LocalAppLanguage.current) }, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                                    if (address["isDefault"] == true || address["default"] == true) Text(tr("Default address", LocalAppLanguage.current), color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showCheckout = false
                                onAddAddress()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Ink)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Add New Address", LocalAppLanguage.current), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                        val address = savedAddresses.firstOrNull { it["_id"]?.toString() == selectedDeliveryAddressId }
                        val textAddress = address?.let { listOf("addressLine1", "addressLine2", "landmark", "city", "state", "pincode").mapNotNull { key -> it[key]?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }.joinToString(", ").ifBlank { (it["address"] ?: it["addressText"] ?: it["fullAddress"] ?: it["completeAddress"] ?: it["formattedAddress"])?.toString().orEmpty() } }.orEmpty()
                        if (address == null) errorMessage = if (savedAddresses.isEmpty()) "Please add a delivery address first." else "Please select a delivery address."
                        else if (textAddress.isBlank()) errorMessage = "Please edit this address and add complete address details."
                        else {
                            confirmedDeliveryAddress = address
                            customerName = address["name"]?.toString().orEmpty().ifBlank { address["fullName"]?.toString().orEmpty() }.ifBlank { customerName }
                            phone = address["phone"]?.toString().orEmpty().ifBlank { phone }
                            showCheckout = false
                            errorMessage = null
                        }
                        }, enabled = !loadingDeliveryAddresses && !submitting && savedAddresses.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color(0xFFE2E2E2)), contentColor = Ink)) {
                            Text(tr("Confirm Address", LocalAppLanguage.current), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OrdersPage(
    modifier: Modifier,
    latestOrderId: String?,
    latestOrderSuccess: Boolean?,
    latestInvoiceUrl: String?,
    latestOrderMessage: String?,
    products: List<Product>,
    onAdd: (String) -> Unit,
    pageTitle: String = "Order History",
    onHome: () -> Unit,
    onCart: () -> Unit,
    onCategories: () -> Unit,
    onTabChange: ((String) -> Unit)? = null
) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    var previousOrders by remember(user?.uid) { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var loadingOrders by remember(user?.uid) { mutableStateOf(true) }
    var ordersError by remember(user?.uid) { mutableStateOf<String?>(null) }
    var selectedOrder by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var orderOptions by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var confirmDeleteOrder by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var orderSearch by remember { mutableStateOf("") }
    var rateOrder by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var selectedRating by remember { mutableIntStateOf(5) }

    BackHandler(enabled = selectedOrder != null) {
        when {
            confirmDeleteOrder != null -> confirmDeleteOrder = null
            orderOptions != null -> orderOptions = null
            rateOrder != null -> rateOrder = null
            else -> selectedOrder = null
        }
    }

    DisposableEffect(user?.uid) {
        var registration: ListenerRegistration? = null
        if (user != null) {
            registration = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("customerId", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        ordersError = error.localizedMessage ?: "Could not load previous orders."
                        loadingOrders = false
                    } else {
                        previousOrders = snapshot?.documents?.map { doc ->
                            doc.data.orEmpty() + ("_documentId" to doc.id)
                        }?.sortedByDescending { row ->
                            (row["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                        }.orEmpty()
                        loadingOrders = false
                        ordersError = null
                    }
                }
        } else {
            previousOrders = emptyList()
            loadingOrders = false
        }
        onDispose { registration?.remove() }
    }

    val today = java.time.LocalDate.now()
    fun orderDate(order: Map<String, Any?>): java.time.LocalDate? {
        val timestamp = order["createdAt"] as? com.google.firebase.Timestamp ?: return null
        return timestamp.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }
    val failedOrders = previousOrders.filter { (it["orderStatus"]?.toString()?.lowercase() ?: "").let { status -> status.contains("fail") || status.contains("reject") || status.contains("cancel") } }
    val visibleOrders = previousOrders.filter { order ->
        val q = orderSearch.trim().lowercase()
        q.isBlank() || listOf(order["orderId"], order["_documentId"], order["orderStatus"], order["grandTotal"])
            .any { it?.toString()?.lowercase()?.contains(q) == true } ||
            listOf("items", "orderItems", "order_items", "cartItems", "cart_items", "products", "medicines").firstNotNullOfOrNull { key -> order[key] as? List<*> }?.any { item ->
                val row = item as? Map<*, *>
                row?.values?.any { it?.toString()?.lowercase()?.contains(q) == true } == true
            } == true
    }
    val currentOrders = visibleOrders.filter { orderDate(it) == today }
    val historicalOrders = visibleOrders.filter { orderDate(it)?.isBefore(today) != false }
    // Rebuild the cart using current catalogue IDs. Historical orders may contain
    // legacy product IDs, so fall back to normalized product names before giving up.
    fun addOrderItemsToCart(orderItems: List<Map<*, *>>): Int {
        var addedCount = 0
        val normalizedProducts = products.associateBy { product ->
            product.name.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        }
        fun normalized(value: Any?): String =
            value?.toString()?.lowercase(Locale.ROOT)?.filter { it.isLetterOrDigit() }.orEmpty()

        orderItems.forEach { item ->
            // Older web orders use different field names and may store product details
            // inside a nested product object. Read all known legacy shapes.
            val nestedProduct = (item["product"] as? Map<*, *>)
                ?: (item["productDetails"] as? Map<*, *>)
                ?: (item["medicine"] as? Map<*, *>)
                ?: emptyMap<Any?, Any?>()
            val savedId = listOf("productId", "product_id", "productID", "id", "productDocumentId", "medicineId", "medicine_id")
                .firstNotNullOfOrNull { key -> (item[key] ?: nestedProduct[key])?.toString()?.trim()?.takeIf { it.isNotEmpty() } }.orEmpty()
            val savedName = listOf(
                "productName", "product_name", "name", "title", "medicine_name",
                "medicineName", "productTitle", "product_title", "itemName", "item_name"
            ).firstNotNullOfOrNull { key ->
                (item[key] ?: nestedProduct[key])?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }.orEmpty()
            val normalizedName = normalized(savedName)
            val normalizedId = normalized(savedId)

            val product = products.firstOrNull { it.id == savedId }
                ?: products.firstOrNull { normalized(it.id) == normalizedId && normalizedId.isNotBlank() }
                ?: normalizedProducts[normalizedName]
                ?: products.firstOrNull { candidate ->
                    val candidateName = normalized(candidate.name)
                    normalizedName.isNotBlank() && candidateName.isNotBlank() &&
                        (candidateName.contains(normalizedName) || normalizedName.contains(candidateName))
                }
                // Some older orders stored a product name in the ID field.
                ?: normalizedProducts[normalizedId]
                // Final conservative match for legacy spelling/punctuation differences.
                ?: products.firstOrNull { candidate ->
                    val candidateTokens = normalized(candidate.name).split(Regex("(?<=.{4})")).filter { it.length >= 4 }.toSet()
                    val savedTokens = normalizedName.split(Regex("(?<=.{4})")).filter { it.length >= 4 }.toSet()
                    savedTokens.isNotEmpty() && candidateTokens.intersect(savedTokens).size >= 2
                }

            val quantityValue = item["quantity"] ?: item["qty"] ?: item["count"]
            val quantity = ((quantityValue as? Number)?.toInt()
                ?: quantityValue?.toString()?.toIntOrNull()
                ?: 1).coerceIn(1, 20)
            if (product != null) {
                repeat(quantity) { onAdd(product.id); addedCount++ }
            }
        }
        return addedCount
    }
    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF4F5FA))).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).border(1.dp, appThemedColor(Color(0xFFE8EAF0)), CircleShape).clickable(onClick = onHome), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBack, contentDescription = tr("Back", LocalAppLanguage.current), tint = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Text(tr(pageTitle, LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(24.dp), color = appThemedColor(Color(0xFFF6F7FA))) {
                Text(
                    "${visibleOrders.size} ${if (visibleOrders.size == 1) "order" else "orders"}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = orderSearch,
            onValueChange = { orderSearch = it },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp),
            placeholder = { Text(tr("Search your orders", LocalAppLanguage.current), color = Muted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(22.dp)) },
            singleLine = true,
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = Forest)
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Muted.copy(alpha = 0.45f)))
        Spacer(Modifier.height(18.dp))
        if (failedOrders.isNotEmpty() && orderSearch.isBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = appThemedColor(Color(0xFFE76F35)), modifier = Modifier.size(38.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Your Failed Orders", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(tr("${failedOrders.size} order(s)", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = { selectedOrder = failedOrders.firstOrNull() }, shape = RoundedCornerShape(12.dp)) { Text(tr("View", LocalAppLanguage.current), color = Forest) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (latestOrderSuccess != null) {
            val successGreen = appThemedColor(Color(0xFF237344))
            val successTint = appThemedColor(Color(0xFFF1FAEC))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (latestOrderSuccess) successTint else appThemedColor(Color(0xFFFCE8E6))
                ),
                shape = RoundedCornerShape(24.dp),
                border = if (latestOrderSuccess) BorderStroke(1.dp, appThemedColor(Color(0xFFBFE3AE))) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = if (latestOrderSuccess) 4.dp else 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(if (latestOrderSuccess) 3.dp else 0.dp, CircleShape)
                                .background(if (latestOrderSuccess) successGreen else appThemedColor(Color(0xFFB3261E)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (latestOrderSuccess) Icons.Default.Check else Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (latestOrderSuccess) tr("Order successful", LocalAppLanguage.current) else tr("Order failed", LocalAppLanguage.current),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = if (latestOrderSuccess) successGreen else appThemedColor(Color(0xFFB3261E))
                            )
                            Text(
                                if (latestOrderSuccess) tr("Your order has been saved", LocalAppLanguage.current) else tr("Please review your order", LocalAppLanguage.current),
                                fontSize = 12.sp,
                                color = Muted
                            )
                        }
                        if (latestOrderSuccess) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = appThemedColor(Color(0xFFB8DCA8)), modifier = Modifier.size(22.dp))
                        }
                    }
                    if (latestOrderId != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tr("ORDER ID", LocalAppLanguage.current), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                            Text(
                                latestOrderId!!,
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    HorizontalDivider(color = if (latestOrderSuccess) appThemedColor(Color(0xFFD5E5CF)) else appThemedColor(Color(0xFFE8C9C7)), thickness = 1.dp)
                    Text(
                        latestOrderMessage ?: if (latestOrderSuccess) tr("Your order was saved.", LocalAppLanguage.current) else tr("Your order could not be placed.", LocalAppLanguage.current),
                        color = Ink,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    if (latestOrderSuccess && !latestInvoiceUrl.isNullOrBlank()) {
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestInvoiceUrl))) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("View / Download Invoice", LocalAppLanguage.current), fontWeight = FontWeight.Bold)
                        }
                    } else if (latestOrderSuccess) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(appThemedColor(Color(0xFFF0F4FA)))
                                .border(1.dp, appThemedColor(Color(0xFFDCE5F1)), RoundedCornerShape(16.dp))
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp)
                                    .clip(CircleShape)
                                    .background(appThemedColor(Color(0xFFDDE9F8))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = appThemedColor(Color(0xFF34445D)), modifier = Modifier.size(21.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                tr("Your order is saved. The invoice is not available here yet; please check your email or contact the shop.", LocalAppLanguage.current),
                                color = appThemedColor(Color(0xFF596579)),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Current Orders", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(tr("Placed today", LocalAppLanguage.current) + " · " + today.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")), color = Muted, fontSize = 12.sp)
            }
            Box(
                Modifier.shadow(4.dp, RoundedCornerShape(50.dp))
                    .background(appThemedColor(Color.White), RoundedCornerShape(50.dp))
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text(currentOrders.size.toString(), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loadingOrders -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color(0xFFF1F2F4)))) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Forest, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(tr("Loading your orders…", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                }
            }
            ordersError != null -> Text(ordersError!!, color = MaterialTheme.colorScheme.error)
            currentOrders.isEmpty() -> Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(tr("No orders placed today", LocalAppLanguage.current), color = appThemedColor(Color(0xFF4A4A4A)), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = onCategories,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(tr("Click to order  →", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold) }
                }
            }
            else -> currentOrders.forEach { order ->
                val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
                val status = order["orderStatus"]?.toString() ?: "received"
                val total = (order["grandTotal"] as? Number)?.toDouble() ?: 0.0
                val timestamp = order["createdAt"] as? com.google.firebase.Timestamp
                val invoiceUrl = order["invoiceUrl"]?.toString()
                Card(colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selectedOrder = order }) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).shadow(4.dp, CircleShape).background(appThemedColor(Color(0xFFF1F1F1)), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(orderId, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                                Text(timestamp?.toDate()?.let { java.text.SimpleDateFormat("dd MMM yyyy · h:mm a", java.util.Locale.getDefault()).format(it) } ?: tr("Date not available", LocalAppLanguage.current), color = Muted, fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { orderOptions = order },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = tr("Order options", LocalAppLanguage.current), tint = Ink)
                            }
                        }
                        val orderItems = ((listOf("items", "orderItems", "order_items", "cartItems", "cart_items", "products", "medicines").firstNotNullOfOrNull { key -> order[key] as? List<*> })?.mapNotNull { it as? Map<*, *> }).orEmpty()
                        if (orderItems.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                orderItems.forEach { item ->
                                    val productId = item["productId"]?.toString().orEmpty()
                                    val product = products.firstOrNull { it.id == productId }
                                    Box(Modifier.size(60.dp).shadow(3.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(14.dp)).background(appThemedColor(Color(0xFFF6F7FA))), contentAlignment = Alignment.Center) {
                                        if (!product?.imageUrl.isNullOrBlank()) AsyncImage(model = product?.imageUrl, contentDescription = product?.name ?: item["productName"]?.toString(), modifier = Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                                        else Text(product?.emoji ?: "🌿", fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isFailedStatus = status.contains("reject", ignoreCase = true) || status.contains("cancel", ignoreCase = true) || status.contains("fail", ignoreCase = true)
                            val isPendingStatus = status.contains("pending", ignoreCase = true) || status.contains("verification", ignoreCase = true)
                            val orderStatusTint = when {
                                isFailedStatus -> appThemedColor(Color(0xFFC62828))
                                isPendingStatus -> appThemedColor(Color(0xFFC58B00))
                                else -> appThemedColor(Color(0xFF27864A))
                            }
                            Icon(
                                when {
                                    isFailedStatus -> Icons.Default.Cancel
                                    isPendingStatus -> Icons.Default.Schedule
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = orderStatusTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                tr(if (isPendingStatus) "Pending Verification" else status.replace('_', ' ').split(" ").joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }, LocalAppLanguage.current),
                                color = orderStatusTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    val addedCount = addOrderItemsToCart(orderItems)
                                    Toast.makeText(
                                        context,
                                        if (addedCount > 0) "$addedCount item(s) added to cart" else "No ordered products matched the current catalogue. Please refresh products and try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    if (addedCount > 0) onCart()
                                },
                                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp)
                            ) {
                                Text(tr("Reorder", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Medium)
                            }
                            if (!invoiceUrl.isNullOrBlank()) {
                                IconButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) },
                                    modifier = Modifier.size(38.dp).background(appThemedColor(Color.White), CircleShape)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = tr("Open invoice PDF", LocalAppLanguage.current), tint = Ink)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Previous Orders", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(tr("Your order history", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
            }
            Box(
                Modifier.shadow(4.dp, RoundedCornerShape(50.dp))
                    .background(appThemedColor(Color.White), RoundedCornerShape(50.dp))
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text(historicalOrders.size.toString(), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loadingOrders || ordersError != null -> {}
            historicalOrders.isEmpty() -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color(0xFFF8F9FA)))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tr("No previous orders yet. Orders from earlier days will appear here.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            else -> historicalOrders.forEach { order ->
                val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
                val status = order["orderStatus"]?.toString() ?: "received"
                val total = (order["grandTotal"] as? Number)?.toDouble() ?: 0.0
                val timestamp = order["createdAt"] as? com.google.firebase.Timestamp
                val invoiceUrl = order["invoiceUrl"]?.toString()
                val orderItems = ((listOf("items", "orderItems", "order_items", "cartItems", "cart_items", "products", "medicines").firstNotNullOfOrNull { key -> order[key] as? List<*> })?.mapNotNull { it as? Map<*, *> }).orEmpty()
                Card(colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selectedOrder = order }) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(appThemedColor(Color(0xFFF1F1F1)), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(orderId, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                                Text(timestamp?.toDate()?.let { java.text.SimpleDateFormat("dd MMM yyyy · hh:mm a", java.util.Locale.getDefault()).format(it) } ?: tr("Date not available", LocalAppLanguage.current), color = Muted, fontSize = 11.sp)
                            }
                            IconButton(onClick = { orderOptions = order }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = tr("Order options", LocalAppLanguage.current), tint = Ink)
                            }
                        }
                        if (orderItems.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                orderItems.take(3).forEach { item ->
                                    val productId = item["productId"]?.toString().orEmpty()
                                    val product = products.firstOrNull { it.id == productId }
                                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFF6F7FA))), contentAlignment = Alignment.Center) {
                                        if (!product?.imageUrl.isNullOrBlank()) AsyncImage(model = product?.imageUrl, contentDescription = product?.name ?: item["productName"]?.toString(), modifier = Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
                                        else Text(product?.emoji ?: "🌿", fontSize = 22.sp)
                                    }
                                }
                                if (orderItems.size > 3) Box(Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFE2E2E2))), contentAlignment = Alignment.Center) { Text(tr("+", LocalAppLanguage.current) + (orderItems.size - 3), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isFailedStatus = status.contains("reject", ignoreCase = true) || status.contains("cancel", ignoreCase = true) || status.contains("fail", ignoreCase = true)
                            val isPendingStatus = status.contains("pending", ignoreCase = true) || status.contains("verification", ignoreCase = true)
                            val orderStatusTint = when {
                                isFailedStatus -> appThemedColor(Color(0xFFC62828))
                                isPendingStatus -> appThemedColor(Color(0xFFC58B00))
                                else -> appThemedColor(Color(0xFF27864A))
                            }
                            Icon(
                                when {
                                    isFailedStatus -> Icons.Default.Cancel
                                    isPendingStatus -> Icons.Default.Schedule
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = orderStatusTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(tr(status.replace('_', ' ').split(" ").joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }, LocalAppLanguage.current), color = orderStatusTint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                val addedCount = addOrderItemsToCart(orderItems)
                                Toast.makeText(
                                    context,
                                    if (addedCount > 0) "$addedCount item(s) added to cart" else "No ordered products matched the current catalogue. Please refresh products and try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                if (addedCount > 0) onCart()
                            }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp)) { Text(tr("Reorder", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Medium) }
                            if (!invoiceUrl.isNullOrBlank()) {
                                IconButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) },
                                    modifier = Modifier.size(38.dp).background(appThemedColor(Color.White), CircleShape)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = tr("Open invoice PDF", LocalAppLanguage.current), tint = Ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    }

    rateOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { rateOrder = null },
            title = { Text(tr("Rate Your Order", LocalAppLanguage.current), color = Forest, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("How was your order experience?", LocalAppLanguage.current), color = Muted)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { selectedRating = star }) {
                                Icon(if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = tr("$star stars", LocalAppLanguage.current), tint = Gold, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                    Text(tr("$selectedRating out of 5", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = { TextButton(onClick = { rateOrder = null; Toast.makeText(context, "Thanks for your feedback!", Toast.LENGTH_SHORT).show() }) { Text(tr("Submit", LocalAppLanguage.current)) } },
            dismissButton = { TextButton(onClick = { rateOrder = null }) { Text(tr("Cancel", LocalAppLanguage.current)) } }
        )
    }

    orderOptions?.let { order ->
        ModalBottomSheet(onDismissRequest = { orderOptions = null }, containerColor = appThemedColor(Color.White), contentColor = Ink) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Order Info", LocalAppLanguage.current), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().clickable {
                        selectedOrder = order
                        orderOptions = null
                    }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Ink)
                    Spacer(Modifier.width(12.dp))
                    Text(tr("View Order", LocalAppLanguage.current), color = Ink, fontSize = 16.sp)
                }
                Row(Modifier.fillMaxWidth().clickable {
                    val items = ((listOf("items", "orderItems", "order_items", "cartItems", "cart_items", "products", "medicines").firstNotNullOfOrNull { key -> order[key] as? List<*> })?.mapNotNull { it as? Map<*, *> }).orEmpty()
                    val textItems = items.map { item ->
                        val name = item["productName"]?.toString() ?: item["name"]?.toString() ?: item["title"]?.toString() ?: "Product"
                        val qty = item["quantity"]?.toString() ?: item["qty"]?.toString() ?: "1"
                        "$name × $qty"
                    }.joinToString("\\n").ifBlank { "Order items are not available." }
                    runCatching {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, textItems) }, "Share order items"))
                    }.onFailure { Toast.makeText(context, "Unable to share order items", Toast.LENGTH_SHORT).show() }
                    orderOptions = null
                }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Ink)
                    Spacer(Modifier.width(12.dp)); Text(tr("Share order items", LocalAppLanguage.current), color = Ink, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    confirmDeleteOrder?.let { order ->
        AlertDialog(onDismissRequest = { confirmDeleteOrder = null }, title = { Text(tr("Delete order?", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold) }, text = { Text(tr("This will remove the order from your order history. This action cannot be undone.", LocalAppLanguage.current), color = Muted) }, confirmButton = {
            TextButton(onClick = {
                val id = order["_documentId"]?.toString().orEmpty()
                if (id.isBlank()) Toast.makeText(context, "Could not identify this order.", Toast.LENGTH_LONG).show()
                else FirebaseFirestore.getInstance().collection("orders").document(id).delete()
                    .addOnSuccessListener { Toast.makeText(context, "Order deleted", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(context, e.localizedMessage ?: "Could not delete order", Toast.LENGTH_LONG).show() }
                confirmDeleteOrder = null
            }) { Text(tr("Delete", LocalAppLanguage.current), color = appThemedColor(Color(0xFFC62828))) }
        }, dismissButton = { TextButton(onClick = { confirmDeleteOrder = null }) { Text(tr("Cancel", LocalAppLanguage.current), color = Ink) } })
    }

    selectedOrder?.let { order ->
        val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
        val invoiceNo = order["invoiceNumber"]?.toString() ?: orderId
        fun orderText(vararg keys: String): String =
            keys.firstNotNullOfOrNull { key -> order[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
        val customerName = orderText("customerName", "name", "fullName")
        val customerEmail = orderText("email", "customerEmail", "emailAddress")
        val phone = orderText("phone", "customerPhone", "mobile")
        val deliveryRecipient = orderText("deliveryRecipientName", "recipientName", "receiverName").ifBlank { customerName }
        val deliveryPhone = orderText("deliveryPhone", "recipientPhone", "shippingPhone").ifBlank { phone }
        val deliveryAddress = orderText("deliveryAddress", "shippingAddress", "address", "addressText", "fullAddress")
        val deliveryAddressLabel = orderText("deliveryAddressLabel", "addressLabel")
        val deliveryInstructions = orderText("deliveryInstruction", "deliveryInstructions", "user_instructions", "instructions")
        val status = order["orderStatus"]?.toString() ?: "received"
        val paymentStatus = order["paymentStatus"]?.toString() ?: "not_collected"
        val total = (order["grandTotal"] as? Number)?.toDouble() ?: (order["subtotal"] as? Number)?.toDouble() ?: 0.0
        val subtotal = (order["subtotal"] as? Number)?.toDouble() ?: total
        val invoiceItems = ((listOf("items", "orderItems", "order_items", "cartItems", "cart_items", "products", "medicines").firstNotNullOfOrNull { key -> order[key] as? List<*> })?.mapNotNull { it as? Map<*, *> }).orEmpty()
        val created = (order["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.let { java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(it) } ?: "Date not available"
        val invoiceUrl = order["invoiceUrl"]?.toString()
        // Render order summary as a full-screen in-app page instead of a Dialog.
        // This gives the page its own header, scrollable content, and bottom action area.
        Box(Modifier.fillMaxSize().background(appThemedColor(Color.White))) {
            Column(Modifier.fillMaxSize().background(appThemedColor(Color.White))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedOrder = null }, modifier = Modifier.size(48.dp).border(1.dp, appThemedColor(Color(0xFFE8EAF0)), CircleShape)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = tr("Back to Order History", LocalAppLanguage.current), tint = Ink)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { confirmDeleteOrder = order }, modifier = Modifier.size(48.dp).border(1.dp, appThemedColor(Color(0xFFE8EAF0)), CircleShape)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = tr("Delete order", LocalAppLanguage.current), tint = Ink)
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)) {
                        Text(tr("Order Summary", LocalAppLanguage.current), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        val orderSummaryStatusColor = when {
                            status.contains("reject", ignoreCase = true) || status.contains("cancel", ignoreCase = true) || status.contains("fail", ignoreCase = true) -> Color(0xFFC62828)
                            status.contains("pending", ignoreCase = true) || status.contains("verification", ignoreCase = true) -> Color(0xFFC58B00)
                            else -> Color(0xFF27864A)
                        }
                        Text(if (status.equals("delivered", true) || status.equals("completed", true)) tr("Arrived at $created", LocalAppLanguage.current) else "${tr("Order status", LocalAppLanguage.current)}: ${tr(status.replace('_', ' ').replaceFirstChar { it.uppercase() }, LocalAppLanguage.current)}", color = orderSummaryStatusColor, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        if (!invoiceUrl.isNullOrBlank() && (status.equals("accepted", true) || status.equals("approved", true))) {
                            Row(Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) } }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("Download Invoice", LocalAppLanguage.current), color = Forest, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Forest)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text(tr("${invoiceItems.size} item(s) in this order", LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        invoiceItems.forEach { item ->
                            val productId = item["productId"]?.toString().orEmpty()
                            val product = products.firstOrNull { it.id == productId }
                            val name = item["productName"]?.toString() ?: item["name"]?.toString() ?: product?.name ?: "Product"
                            val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                            val unitPrice = (item["unitPrice"] as? Number)?.toDouble() ?: (item["price"] as? Number)?.toDouble() ?: 0.0
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFF6F7FA))), contentAlignment = Alignment.Center) {
                                    if (!product?.imageUrl.isNullOrBlank()) AsyncImage(model = product?.imageUrl, contentDescription = name, modifier = Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
                                    else Text(product?.emoji ?: "🌿", fontSize = 28.sp)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(localizedProductName(product?.name ?: name, LocalAppLanguage.current), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    val packSize = product?.subtitle?.takeIf { it.isNotBlank() }
                                    Text("${packSize ?: tr("Item", LocalAppLanguage.current)} × $quantity", color = Muted, fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(tr("₹", LocalAppLanguage.current) + String.format("%.0f", (item["lineTotal"] as? Number)?.toDouble() ?: (unitPrice * quantity)), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Divider(color = appThemedColor(Color(0xFFF0F1F4)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                        Text(tr("Bill Details", LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(tr("MRP / subtotal", LocalAppLanguage.current), color = Muted); Text(tr("₹", LocalAppLanguage.current) + String.format("%.0f", subtotal), color = Ink) }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(tr("Delivery charges", LocalAppLanguage.current), color = Muted); Text(tr("FREE", LocalAppLanguage.current), color = Ink) }
                        Spacer(Modifier.height(12.dp))
                        Divider(color = appThemedColor(Color(0xFFE5E7EB)))
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(tr("Bill total", LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(tr("₹", LocalAppLanguage.current) + String.format("%.0f", total), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                    Divider(color = appThemedColor(Color(0xFFE5E7EB)), thickness = 7.dp)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(tr("Order Details", LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Column {
                            Text(tr("Order ID", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(orderId, color = Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Order ID", orderId))
                                    Toast.makeText(context, "Order ID copied", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.ContentCopy, contentDescription = tr("Copy order ID", LocalAppLanguage.current), tint = Muted) }
                            }
                        }
                        Column {
                            Text(tr("Payment", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(paymentStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }, color = Ink, fontSize = 16.sp)
                        }
                        if (phone.isNotBlank()) Column { Text(tr("Phone", LocalAppLanguage.current), color = Muted, fontSize = 14.sp); Text(phone, color = Ink, fontSize = 16.sp) }
                        Column { Text(tr("Order placed", LocalAppLanguage.current), color = Muted, fontSize = 14.sp); Text(created, color = Ink, fontSize = 16.sp) }
                    }
                    Divider(color = appThemedColor(Color(0xFFE5E7EB)), thickness = 7.dp)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(tr("Customer & Delivery Details", LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        if (customerName.isNotBlank()) Column {
                            Text(tr("Customer name", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Text(customerName, color = Ink, fontSize = 16.sp)
                        }
                        if (customerEmail.isNotBlank()) Column {
                            Text(tr("Email", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Text(customerEmail, color = Ink, fontSize = 16.sp)
                        }
                        if (deliveryRecipient.isNotBlank()) Column {
                            Text(tr("Delivery recipient", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Text(deliveryRecipient, color = Ink, fontSize = 16.sp)
                        }
                        if (deliveryPhone.isNotBlank()) Column {
                            Text(tr("Delivery phone", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Text(deliveryPhone, color = Ink, fontSize = 16.sp)
                        }
                        if (deliveryAddress.isNotBlank()) Column {
                            Text(tr(if (deliveryAddressLabel.isNotBlank()) "Delivery address · $deliveryAddressLabel" else "Delivery address", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Text(deliveryAddress, color = Ink, fontSize = 16.sp)
                        }
                        if (deliveryInstructions.isNotBlank()) Column {
                            Text(tr("Delivery instructions", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                            Text(deliveryInstructions, color = Ink, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(tr("Need help with your order?", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(3.dp))
                        Text(tr("About any issues related to your order", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                        TextButton(onClick = { selectedOrder = null; onTabChange?.invoke("Help") }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) {
                            Text(tr("Chat with us", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(appThemedColor(Color.White))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 6.dp, bottom = 8.dp)
                ) {
                    Button(
                        onClick = {
                            // Use the same tolerant catalogue matching as the order-history Reorder action.
                            // Historical order IDs can differ from current Firestore product IDs.
                            val addedCount = addOrderItemsToCart(invoiceItems)
                            selectedOrder = null
                            Toast.makeText(
                                context,
                                if (addedCount > 0) "$addedCount item(s) added to cart"
                                else "No ordered products matched the current catalogue. Please refresh products and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            if (addedCount > 0) onCart()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                        colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color(0xFFE2E2E2)), contentColor = Ink)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tr("Repeat Order", LocalAppLanguage.current), color = appThemedColor(Color(0xFF000000)), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                            Text(tr("VIEW CART ON NEXT STEP", LocalAppLanguage.current), color = appThemedColor(Color(0xFF000000)), fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
                        }
                    }
                }

            }
        }
        }
    }

private fun localizedDiseaseDescription(description: String, language: String): String {
    if (language == "English") return description
    val normalized = description.trim().lowercase(Locale.ROOT)
    if (normalized.startsWith("a disease is a condition that disrupts the normal structure or functioning of the body or mind.")) {
        return when (language) {
            "Telugu" -> "వ్యాధి అనేది శరీరం లేదా మనస్సు యొక్క సాధారణ నిర్మాణం లేదా పనితీరుకు అంతరాయం కలిగించే పరిస్థితి. వ్యాధులు ఇన్ఫెక్షన్లు, వంశపారంపర్య లేదా జన్యు కారణాలు, రోగనిరోధక వ్యవస్థ సమస్యలు, పర్యావరణ ప్రభావాలు, గాయాలు మరియు శరీర అవయవాలు లేదా ప్రక్రియల్లో మార్పులు వంటి అనేక కారణాల వల్ల రావచ్చు. నొప్పి, జ్వరం, అలసట, వాపు లేదా శరీర విధుల్లో మార్పులు వంటి లక్షణాలు కనిపించవచ్చు. కొన్ని వ్యాధులు స్వల్పకాలికమైనవి కాగా, మరికొన్ని దీర్ఘకాలికంగా ఉండి నిరంతర సంరక్షణ అవసరం కావచ్చు. నివారణ, నిర్ధారణ మరియు చికిత్స వ్యాధిని బట్టి మారుతాయి. ఆరోగ్యకరమైన అలవాట్లు, టీకాలు, వైద్య పరీక్షలు, మందులు లేదా అర్హత కలిగిన వైద్య నిపుణుడు సూచించే ఇతర సంరక్షణ ఇందులో భాగం కావచ్చు. ఇది సాధారణ అవగాహన కోసం మాత్రమే; వైద్య నిర్ధారణకు లేదా అర్హత కలిగిన వైద్య నిపుణుడి సలహాకు ప్రత్యామ్నాయం కాదు."
            "Hindi" -> "रोग एक ऐसी स्थिति है जो शरीर या मन की सामान्य संरचना या कार्यप्रणाली में बाधा डालती है। रोग संक्रमण, वंशानुगत या आनुवंशिक कारणों, प्रतिरक्षा प्रणाली की समस्याओं, पर्यावरणीय प्रभावों, चोटों तथा शरीर के अंगों या प्रक्रियाओं में बदलाव सहित कई कारणों से हो सकते हैं। इनके लक्षणों में दर्द, बुखार, थकान, सूजन या शरीर के सामान्य कार्यों में बदलाव शामिल हो सकते हैं। कुछ स्थितियाँ अल्पकालिक होती हैं, जबकि अन्य दीर्घकालिक होती हैं और निरंतर देखभाल की आवश्यकता होती है। रोकथाम, निदान और उपचार संबंधित रोग के अनुसार अलग-अलग होते हैं। इनमें स्वस्थ आदतें, टीकाकरण, चिकित्सकीय जाँच, दवाएँ या योग्य स्वास्थ्य पेशेवर द्वारा सुझाई गई अन्य देखभाल शामिल हो सकती है। यह केवल सामान्य शैक्षिक जानकारी है; यह चिकित्सकीय निदान या योग्य स्वास्थ्य पेशेवर की सलाह का विकल्प नहीं है।"
            else -> description
        }
    }
    return description
}

@Composable
private fun DiseasesPage(modifier: Modifier, diseases: List<String>, products: List<Product>, loading: Boolean, error: String?, onHome: () -> Unit) {
    val scrollState = rememberScrollState()
    var query by remember { mutableStateOf("All Diseases") }
    var selectedDisease by remember { mutableStateOf<String?>("All Diseases") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var detailLoading by remember { mutableStateOf(false) }
    var diseaseDescription by remember { mutableStateOf("") }
    var diseaseImageUrl by remember { mutableStateOf("") }
    var diseasePrecautions by remember { mutableStateOf("") }
    var suggestedMedicineNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    var medicinesExpanded by remember { mutableStateOf(false) }
    var precautionsExpanded by remember { mutableStateOf(false) }
    var selectedDetailTab by remember { mutableStateOf("Description") }

    val relatedMedicines = remember(selectedDisease, products) {
        val chosen = selectedDisease?.takeIf { it != "All Diseases" }
        if (chosen == null) emptyList() else {
            fun normalizeDisease(value: String): String =
                value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim().replace(Regex("\\s+"), " ")
            val chosenKey = normalizeDisease(chosen)
            val diseaseAliases = buildSet {
                add(chosenKey)
                if (chosenKey.contains("tuberculosis") || chosenKey == "tb") {
                    add("tuberculosis")
                    add("tb")
                }
                if (chosenKey.contains("influenza") || chosenKey == "flu") {
                    add("influenza")
                    add("flu")
                }
            }
            products.filter { product ->
                product.isActive && !product.isHidden && product.cures.isNotBlank() &&
                    product.cures.split(",", ";", "|", "\n")
                        .map(::normalizeDisease)
                        .filter { it.isNotBlank() }
                        .any { cure -> diseaseAliases.any { alias -> cure == alias || Regex("(^| )" + Regex.escape(alias) + "( |$)").containsMatchIn(cure) } }
            }.distinctBy { it.id.ifBlank { it.name.trim().lowercase(Locale.ROOT) } }
        }
    }

    val defaultAllDiseasePrecautions = listOf(
        tr("Do not self-diagnose or start medicines based only on general information.", LocalAppLanguage.current),
        tr("Consult a qualified healthcare professional for diagnosis and a treatment plan suited to the specific condition.", LocalAppLanguage.current),
        tr("Use medicines only as prescribed; do not stop prescribed treatment without medical advice.", LocalAppLanguage.current),
        tr("Maintain appropriate hygiene, balanced nutrition, safe physical activity, and recommended vaccinations where applicable.", LocalAppLanguage.current),
        tr("Seek urgent medical care for severe symptoms such as breathing difficulty, chest pain, confusion, severe dehydration, or sudden worsening.", LocalAppLanguage.current)
    )
    
    LaunchedEffect(selectedDisease) {
        diseaseDescription = ""
        diseaseImageUrl = ""
        diseasePrecautions = ""
        suggestedMedicineNames = emptyList()
        if (selectedDisease != null) {
            detailLoading = true
            FirebaseFirestore.getInstance().collection("diseases")
                .whereEqualTo("diseases_name", selectedDisease).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    val data = snapshot.documents.firstOrNull()
                    diseaseDescription = listOf("disease_desc", "disease_description", "description", "disease_details", "details", "about")
                        .firstNotNullOfOrNull { key -> data?.getString(key)?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
                    diseaseImageUrl = listOf("disease_image", "disease_image_url", "image_url", "imageUrl", "image")
                        .firstNotNullOfOrNull { key -> data?.getString(key)?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") } }.orEmpty()
                    diseasePrecautions = listOf("precautions", "disease_precautions", "prevention", "safety_precautions")
                        .firstNotNullOfOrNull { key ->
                            val value = data?.get(key)
                            when (value) {
                                is String -> value.trim().takeIf { it.isNotBlank() }
                                is List<*> -> value.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }.joinToString("\n").takeIf { it.isNotBlank() }
                                else -> null
                            }
                        }.orEmpty()
                    if (selectedDisease == "All Diseases" && diseasePrecautions.isBlank()) {
                        diseasePrecautions = defaultAllDiseasePrecautions.joinToString("\n")
                    }
                    suggestedMedicineNames = listOf("suggested_medicines", "suggestedMedicines", "recommended_medicines", "medicines")
                        .firstNotNullOfOrNull { key ->
                            val value = data?.get(key)
                            when (value) {
                                is String -> value.split(",", ";", "|", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                is List<*> -> value.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                                else -> null
                            }
                        }.orEmpty().distinctBy { it.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) }
                    detailLoading = false
                }
                .addOnFailureListener { detailLoading = false }
        } else {
            detailLoading = false
        }
    }

    val availableDiseases = remember(diseases) {
        (listOf("All Diseases") + diseases.filterNot { it.equals("All Diseases", ignoreCase = true) }).distinct()
    }
    val diseaseLanguage = LocalAppLanguage.current
    val localizedAllDiseases = tr("All Diseases", diseaseLanguage)
    val matches = remember(availableDiseases, query, localizedAllDiseases, diseaseLanguage) {
        if (query.isBlank() || query.equals("All Diseases", ignoreCase = true) || query.equals(localizedAllDiseases, ignoreCase = true)) availableDiseases
        else availableDiseases.filter { disease ->
            disease.contains(query.trim(), ignoreCase = true) ||
                tr(disease, diseaseLanguage).contains(query.trim(), ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(appThemedColor(Color(0xFFF3F5FA))).verticalScroll(scrollState)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
        Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(
                        androidx.compose.foundation.text.selection.LocalTextSelectionColors provides
                            androidx.compose.foundation.text.selection.TextSelectionColors(
                                handleColor = Ink,
                                backgroundColor = Ink.copy(alpha = 0.28f)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tr("Search diseases", LocalAppLanguage.current),
                                color = Muted,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${diseases.size}",
                                color = Ink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(appThemedColor(Color(0xFFE8EAF0)), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        OutlinedTextField(
                            value = when {
                                selectedDisease != null -> tr(selectedDisease.orEmpty(), LocalAppLanguage.current)
                                query.equals("All Diseases", ignoreCase = true) -> localizedAllDiseases
                                else -> query
                            },
                            onValueChange = {
                                query = it
                                selectedDisease = null
                                dropdownExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).defaultMinSize(minHeight = 60.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp),
                            placeholder = { Text(tr("Select disease", LocalAppLanguage.current), fontSize = 14.sp, maxLines = 1, color = Muted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp), tint = Muted) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    // Reopen the complete disease list even when a disease is already selected.
                                    query = ""
                                    dropdownExpanded = true
                                }) {
                                    Icon(
                                        if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = tr("Show all disease options", LocalAppLanguage.current),
                                        tint = Muted
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = Muted,
                                unfocusedLabelColor = Muted,
                                cursorColor = Ink
                            )
                        )
    
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Muted.copy(alpha = 0.45f)))
                }
                DropdownMenu(
                    expanded = dropdownExpanded && matches.isNotEmpty(),
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.94f)
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(appThemedColor(Color.White))
                        .border(1.dp, appThemedColor(Color(0xFFE5E8EF)), RoundedCornerShape(12.dp))
                ) {
                    matches.forEach { disease ->
                        DropdownMenuItem(
                            colors = MenuDefaults.itemColors(textColor = Ink),
                            text = { Text(tr(disease, LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                val normalizedDisease = disease.trim().lowercase(Locale.ROOT)
                                    .replace(Regex("[^a-z0-9 ]"), " ")
                                    .replace(Regex("\\s+"), " ")
                                val diseaseIcon = when {
                                    normalizedDisease.contains("tuberculosis") || normalizedDisease == "tb" || normalizedDisease.contains(" tb ") -> "🫁"
                                    normalizedDisease.contains("fever") || normalizedDisease.contains("pyrexia") -> "🤒"
                                    normalizedDisease.contains("cough") || normalizedDisease.contains("bronchitis") -> "😷"
                                    normalizedDisease.contains("flu") || normalizedDisease.contains("influenza") || normalizedDisease.contains("cold") || normalizedDisease.contains("rhinitis") -> "🤧"
                                    normalizedDisease.contains("obesity") || normalizedDisease.contains("overweight") -> "⚖️"
                                    normalizedDisease.contains("diabetes") || normalizedDisease.contains("blood sugar") -> "🩸"
                                    normalizedDisease.contains("heart") || normalizedDisease.contains("cardiac") || normalizedDisease.contains("hypertension") || normalizedDisease.contains("blood pressure") -> "❤️"
                                    normalizedDisease.contains("skin") || normalizedDisease.contains("rash") || normalizedDisease.contains("eczema") || normalizedDisease.contains("dermatitis") -> "🩹"
                                    normalizedDisease.contains("liver") || normalizedDisease.contains("hepatitis") || normalizedDisease.contains("cirrhosis") -> "🟤"
                                    normalizedDisease.contains("kidney") || normalizedDisease.contains("renal") || normalizedDisease.contains("urinary") -> "🫘"
                                    normalizedDisease.contains("stomach") || normalizedDisease.contains("gastric") || normalizedDisease.contains("digest") || normalizedDisease.contains("acidity") -> "🫃"
                                    normalizedDisease.contains("eye") || normalizedDisease.contains("vision") -> "👁️"
                                    normalizedDisease.contains("bone") || normalizedDisease.contains("joint") || normalizedDisease.contains("arthritis") -> "🦴"
                                    normalizedDisease.contains("headache") || normalizedDisease.contains("migraine") -> "🤕"
                                    normalizedDisease.contains("asthma") || normalizedDisease.contains("lung") || normalizedDisease.contains("respiratory") -> "🫁"
                                    normalizedDisease.contains("anemia") || normalizedDisease.contains("anaemia") -> "🩸"
                                    normalizedDisease.contains("allergy") || normalizedDisease.contains("allergic") -> "🌼"
                                    normalizedDisease.contains("mental") || normalizedDisease.contains("anxiety") || normalizedDisease.contains("depression") -> "🧠"
                                    else -> "🩺"
                                }
                                Text(diseaseIcon, fontSize = 20.sp)
                            },
                            onClick = {
                                selectedDisease = disease
                                query = tr(disease, diseaseLanguage)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (selectedDisease != null) {
            Spacer(Modifier.height(14.dp))
            val detailDiseaseName = selectedDisease.orEmpty()
            val normalizedDetailDisease = detailDiseaseName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim()
            val detailDiseaseEmoji = when {
                normalizedDetailDisease.contains("tuberculosis") || normalizedDetailDisease == "tb" -> "🫁"
                normalizedDetailDisease.contains("fever") -> "🤒"
                normalizedDetailDisease.contains("cough") -> "😷"
                normalizedDetailDisease.contains("flu") || normalizedDetailDisease.contains("influenza") || normalizedDetailDisease.contains("cold") || normalizedDetailDisease.contains("rhinitis") -> "🤧"
                normalizedDetailDisease.contains("obesity") -> "⚖️"
                normalizedDetailDisease.contains("diabetes") || normalizedDetailDisease.contains("blood sugar") -> "🩸"
                normalizedDetailDisease.contains("heart") || normalizedDetailDisease.contains("cardiac") || normalizedDetailDisease.contains("hypertension") || normalizedDetailDisease.contains("blood pressure") -> "❤️"
                normalizedDetailDisease.contains("skin") || normalizedDetailDisease.contains("rash") || normalizedDetailDisease.contains("eczema") || normalizedDetailDisease.contains("dermatitis") -> "🩹"
                normalizedDetailDisease.contains("liver") || normalizedDetailDisease.contains("hepatitis") || normalizedDetailDisease.contains("cirrhosis") -> "🟤"
                normalizedDetailDisease.contains("kidney") || normalizedDetailDisease.contains("renal") || normalizedDetailDisease.contains("urinary") -> "🫘"
                normalizedDetailDisease.contains("stomach") || normalizedDetailDisease.contains("gastric") || normalizedDetailDisease.contains("digest") || normalizedDetailDisease.contains("acidity") -> "🫃"
                normalizedDetailDisease.contains("eye") || normalizedDetailDisease.contains("vision") -> "👁️"
                normalizedDetailDisease.contains("bone") || normalizedDetailDisease.contains("joint") || normalizedDetailDisease.contains("arthritis") -> "🦴"
                normalizedDetailDisease.contains("headache") || normalizedDetailDisease.contains("migraine") -> "🤕"
                normalizedDetailDisease.contains("asthma") || normalizedDetailDisease.contains("lung") || normalizedDetailDisease.contains("respiratory") -> "🫁"
                normalizedDetailDisease.contains("anemia") || normalizedDetailDisease.contains("anaemia") -> "🩸"
                normalizedDetailDisease.contains("allergy") || normalizedDetailDisease.contains("allergic") -> "🌼"
                normalizedDetailDisease.contains("mental") || normalizedDetailDisease.contains("anxiety") || normalizedDetailDisease.contains("depression") -> "🧠"
                else -> "🩺"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detailDiseaseEmoji, fontSize = 30.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("DISEASE DETAILS", LocalAppLanguage.current), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                    Text(tr(detailDiseaseName, LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = {
                    selectedDisease = null
                    query = ""
                    dropdownExpanded = false
                }) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close disease details and clear search", LocalAppLanguage.current), tint = Ink)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(0.dp)) {
                    val detailTabs = listOf(
                        Triple("Description", Icons.Default.Info, "About"),
                        Triple("Suggested Medicines", Icons.Default.MedicalServices, "Medicines"),
                        Triple("Precautions", Icons.Default.HealthAndSafety, "Care")
                    )
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                                .background(appThemedColor(Color(0xFFEAF2E9))).padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            detailTabs.forEach { (tab, icon, shortLabel) ->
                                val selected = selectedDetailTab == tab
                                Box(Modifier.weight(1f)) {
                                    Column(
                                        Modifier.fillMaxWidth().heightIn(min = 58.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(appThemedColor(if (selected) Color(0xFF4F8B45) else Color.Transparent))
                                            .clickable { selectedDetailTab = tab }
                                            .padding(horizontal = 3.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(icon, contentDescription = shortLabel, tint = if (selected) Color.White else Ink, modifier = Modifier.size(19.dp))
                                        Text(tr(shortLabel, LocalAppLanguage.current), color = if (selected) Color.White else Ink, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 2, lineHeight = 12.sp, textAlign = TextAlign.Center)
                                    }
                                    if (selected) Box(Modifier.align(Alignment.BottomCenter).offset(y = 17.dp).size(30.dp).background(appThemedColor(Color.White), CircleShape))
                                }
                            }
                        }
                    AnimatedContent(
                        targetState = selectedDetailTab,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 12 }) togetherWith
                                (fadeOut(animationSpec = tween(150)) + slideOutHorizontally(animationSpec = tween(150)) { -it / 16 })
                        },
                        label = "diseaseDetailTab",
                        modifier = Modifier.fillMaxWidth()
                    ) { activeTab ->
                        Column(
                            Modifier.fillMaxWidth()
                                .defaultMinSize(minHeight = 190.dp)
                                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 0.dp))
                                .background(appThemedColor(Color.White))
                                .border(1.dp, appThemedColor(Color(0xFFC9DDC3)), RoundedCornerShape(topStart = 0.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 0.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (activeTab) {
                                "Description" -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(34.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(appThemedColor(Color(0xFFE3E6EC))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(tr("About This Condition", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(when { detailLoading -> tr("Loading disease information…", LocalAppLanguage.current); diseaseDescription.isNotBlank() -> localizedDiseaseDescription(diseaseDescription, LocalAppLanguage.current); else -> tr("Information not available.", LocalAppLanguage.current) }, color = Muted, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(start = 44.dp))
                                }
                                "Suggested Medicines" -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(34.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(appThemedColor(Color(0xFFE3E6EC))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(tr("Suggested Medicines", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    val isAllDiseases = selectedDisease == "All Diseases"
                                    val displayedSuggestions = when {
                                        isAllDiseases -> products.filter { it.isActive && !it.isHidden }.map { it.name }.distinct()
                                        else -> (suggestedMedicineNames + relatedMedicines.map { it.name })
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                            .distinctBy { it.lowercase(Locale.ROOT) }
                                    }
                                    if (isAllDiseases) {
                                        Text(
                                            tr("SLGAN catalogue products are shown for reference only. There is no single medicine suitable for all diseases. Choose treatment only after a qualified healthcare professional identifies the condition.", LocalAppLanguage.current),
                                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp
                                        )
                                    }
                                    if (detailLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Forest)
                                    else if (displayedSuggestions.isEmpty()) Text(tr("Information not available.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                                    else displayedSuggestions.forEach { medicine ->
                                        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(20.dp).clip(CircleShape).background(Forest), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Text(localizedProductName(medicine, LocalAppLanguage.current), color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                                        }
                                    }
                                }
                                else -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(34.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(appThemedColor(Color(0xFFE3E6EC))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(tr("Precautions & care", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (diseasePrecautions.isBlank()) Text(tr("Information not available.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 44.dp))
                                    else diseasePrecautions.split("\n").filter { it.isNotBlank() }.forEach { precaution ->
                                        Row(Modifier.fillMaxWidth().padding(start = 44.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Forest, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(9.dp))
                                            Text(precaution, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 96.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = appThemedColor(Color(0xFFEAF2E9))),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    tr("Use medicines only with guidance from a qualified healthcare professional.", LocalAppLanguage.current),
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
            }
        }
    }
}

@Composable
private fun DiseaseAccordion(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).border(1.dp, appThemedColor(Color(0xFFE8EAF0)), RoundedCornerShape(15.dp)).background(appThemedColor(Color.White))) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) tr("Collapse $title", LocalAppLanguage.current) else tr("Expand $title", LocalAppLanguage.current), tint = Ink)
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { content() }
    }
}

@Composable
private fun DetailSectionHeading(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun CategoriesPage(modifier: Modifier, products: List<Product>, cart: Map<String, Int>, productRatings: Map<String, ProductRatingSummary>, productLikes: Map<String, ProductLikeSummary>, selectedCategory: String, onSelectedCategoryChange: (String) -> Unit, onToggleLike: (Product) -> Unit, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onCategoryChange: (String) -> Unit, onProductClick: (Product) -> Unit, onHome: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val leftScroll = rememberScrollState()
    val rightScroll = rememberScrollState()
    var backendCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    backendCategories = snapshot.documents.mapNotNull { doc ->
                        (doc.getString("category_name") ?: doc.getString("name") ?: doc.id)
                            .trim().takeIf { it.isNotBlank() }
                    }.distinctBy { it.lowercase() }
                }
            }
        onDispose { registration.remove() }
    }

    val categoryOrder = listOf("All", "Lehyas", "Grutas", "Pills", "Tailas", "Churnas", "Rasayanas", "Bhasmas")
    val categories = remember(backendCategories, products) {
        val available = (backendCategories + products.map { it.category.trim() }.filter { it.isNotBlank() })
            .distinctBy { it.lowercase() }
        val preferred = categoryOrder.filter { name -> name == "All" || available.any { it.equals(name, true) } }
        (preferred + available.filterNot { item -> preferred.any { it.equals(item, true) } }.sortedBy { it.lowercase() })
            .distinctBy { it.lowercase() }
    }
    val visibleCategories = categories
    val selectedProducts = products.filter {
        (selectedCategory.equals("All", true) || it.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
    }
    val categoryEmojis = mapOf(
        "all" to "🧺", "lehyas" to "🍯", "grutas" to "🏺", "gruta" to "🏺",
        "pills" to "💊", "tailas" to "🫙", "churnas" to "🌾",
        "rasayogas" to "🪷", "rasayoga" to "🪷", "rasayanas" to "🪴", "bhasmas" to "🪨",
        "fresh vegetables" to "🥒", "fresh fruits" to "🍌", "exotics" to "🫑",
        "coriander & others" to "🌿", "freshly cut & sprouts" to "🥗",
        "trusted organics" to "🍅", "flowers & leaves" to "💐"
    )

    Row(modifier.fillMaxSize().background(appThemedColor(Color(0xFFFFF8EC)))) {
        Column(
            Modifier.width(94.dp).fillMaxHeight().background(appThemedColor(Color.White)).verticalScroll(leftScroll).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            visibleCategories.forEach { category ->
                val selected = category.equals(selectedCategory, true)
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (selected) appThemedColor(Color(0xFFE2E2E2)) else Color.White)
                        .clickable {
                            onSelectedCategoryChange(category)
                            onCategoryChange(category)
                        }.padding(horizontal = 3.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(54.dp).clip(CircleShape)
                            .background(if (selected) appThemedColor(Color(0xFFE2E2E2)) else appThemedColor(Color(0xFFE2E2E2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(categoryEmojis[category.lowercase()] ?: "🌿", fontSize = 27.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        tr(category, LocalAppLanguage.current),
                        color = if (selected) Ink else Muted,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    if (selected) Spacer(Modifier.height(3.dp))
                }
            }
        }

        Box(Modifier.width(3.dp).fillMaxHeight().background(appThemedColor(Color(0xFFE5E7EB))).shadow(2.dp))
        Column(Modifier.weight(1f).fillMaxHeight().background(appThemedColor(Color(0xFFF3F5FA)))) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 0.dp).height(60.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp),
                placeholder = { Text(tr("Search products", LocalAppLanguage.current), fontSize = 13.sp, maxLines = 1, color = Muted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp), tint = Muted) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = tr("Clear search", LocalAppLanguage.current))
                    }
                },
                singleLine = true,
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    cursorColor = Forest
                )
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Muted.copy(alpha = 0.45f)))
            Row(Modifier.fillMaxWidth().background(appThemedColor(Color(0xFFF3F5FA))).padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(selectedCategory, LocalAppLanguage.current), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text("${selectedProducts.size} ${tr("products", LocalAppLanguage.current)}", color = Muted, fontSize = 11.sp, maxLines = 1)
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 8.dp)
            ) {
                if (selectedProducts.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌿", fontSize = 38.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(tr("No products in this category", LocalAppLanguage.current), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(selectedProducts.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { rowProducts ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowProducts.forEach { product ->
                                val quantityInCart = cart[product.id] ?: 0
                                Card(
                                    modifier = Modifier.weight(1f).height(285.dp).padding(bottom = 6.dp).clickable { onProductClick(product) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier.fillMaxWidth()
                                                .height(if (selectedCategory.equals("Rasayanas", ignoreCase = true)) 145.dp else 128.dp)
                                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                                .background(appThemedColor(Color(0xFFF7F7F7))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (product.imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = product.imageUrl,
                                                    contentDescription = product.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(product.emoji.ifBlank { "🌿" }, fontSize = 38.sp)
                                            }
                                            Box(
                                                Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 7.dp)
                                            ) {
                                                ProductLikeControl(product, productLikes[product.id] ?: ProductLikeSummary(), onToggleLike)
                                            }
                                        }
                                        Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp)) {
                                            Text(
                                                localizedProductName(product.name, LocalAppLanguage.current),
                                                color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                                maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
                                                minLines = 2
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    product.subtitle.takeIf { it.isNotBlank() && it != "Traditional Ayurvedic product" } ?: "Pack size not set",
                                                    color = Muted,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                    softWrap = false
                                                )
                                                Box(
                                                    modifier = Modifier.wrapContentWidth(Alignment.End).align(Alignment.CenterVertically),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    ProductRatingLine(
                                                        product,
                                                        productRatings[product.id] ?: ProductRatingSummary(),
                                                        compact = true,
                                                        countBaselineFix = true
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                if (product.price > 0.0) "₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price) else "Price not set",
                                                color = appThemedColor(Color.Black), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            if (quantityInCart == 0) {
                                                Button(
                                                    onClick = { onAdd(product.id) },
                                                    enabled = product.price > 0.0 && product.productStock > 0,
                                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color(0xFFE2E2E2)), contentColor = appThemedColor(Color.Black), disabledContainerColor = appThemedColor(Color(0xFFE2E2E2)), disabledContentColor = Color.Black),
                                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                                                ) { Text(tr("ADD", LocalAppLanguage.current), color = appThemedColor(Color.Black), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                            } else {
                                                Row(
                                                    Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(12.dp)).background(appThemedColor(Color(0xFFE2E2E2))),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    IconButton(onClick = { onRemove(product.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, contentDescription = tr("Decrease quantity", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(16.dp)) }
                                                    Text(quantityInCart.toString(), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    IconButton(onClick = { onAdd(product.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, contentDescription = tr("Increase quantity", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(16.dp)) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowProducts.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tr(title, LocalAppLanguage.current), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
    }
    HorizontalDivider(color = PaleGreen.copy(alpha = 0.65f), thickness = 0.7.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsPage(
    modifier: Modifier,
    scrollState: androidx.compose.foundation.ScrollState,
    onTabChange: (String) -> Unit,
    onSignOut: () -> Unit,
    appLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email.orEmpty()
    val context = LocalContext.current
    var nickname by remember(user?.uid) { mutableStateOf("") }
    var profilePic by remember(user?.uid) { mutableStateOf("") }
    var profilePicSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var selectedLanguage by remember(appLanguage) { mutableStateOf(appLanguage) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var languageSaving by remember { mutableStateOf(false) }
    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("customers").document(uid).get()
            .addOnSuccessListener { snapshot ->
                profilePic = snapshot.getString("profile_pic").orEmpty()
                nickname = snapshot.getString("nickname")?.trim().orEmpty()
            }
    }
    val profileImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || user == null) return@rememberLauncherForActivityResult
        profilePicSaving = true
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bitmap == null) {
                profilePicSaving = false
                Toast.makeText(context, "Could not open this image. Please choose another.", Toast.LENGTH_SHORT).show()
            } else {
                val side = minOf(bitmap.width, bitmap.height)
                val left = ((bitmap.width - side) / 2).coerceAtLeast(0)
                val top = ((bitmap.height - side) / 2).coerceAtLeast(0)
                val square = Bitmap.createBitmap(bitmap, left, top, side, side)
                val resized = Bitmap.createScaledBitmap(square, 512, 512, true)
                val output = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 78, output)
                val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                if (square !== bitmap) square.recycle()
                if (resized !== square && resized !== bitmap) resized.recycle()
                if (bitmap !== square) bitmap.recycle()
                FirebaseFirestore.getInstance().collection("customers").document(user.uid)
                    .update("profile_pic", encoded)
                    .addOnSuccessListener {
                        profilePic = encoded
                        profilePicSaving = false
                        Toast.makeText(context, "Profile picture updated.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        profilePicSaving = false
                        Toast.makeText(context, "Could not save picture: ${error.localizedMessage ?: "Please try again"}", Toast.LENGTH_LONG).show()
                    }
            }
        } catch (error: Exception) {
            profilePicSaving = false
            Toast.makeText(context, "Could not process image. Please choose another.", Toast.LENGTH_SHORT).show()
        }
    }
    val profileBitmap = remember(profilePic) {
        runCatching {
            if (profilePic.isBlank()) null else {
                val bytes = Base64.decode(profilePic, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Column(modifier = modifier.fillMaxSize().background(appThemedColor(Color(0xFFF3F5FA))).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().background(appThemedColor(Color(0xFFF3F5FA)))
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(78.dp), contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier.fillMaxSize().background(appThemedColor(Color.White), CircleShape)
                        .border(2.dp, Gold.copy(alpha = 0.7f), CircleShape).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(bitmap = profileBitmap!!, contentDescription = tr("Account profile picture", LocalAppLanguage.current), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = tr("Account profile", LocalAppLanguage.current), tint = Muted, modifier = Modifier.size(44.dp))
                    }
                }
                Surface(
                    modifier = Modifier.size(25.dp).clickable(enabled = !profilePicSaving) { profileImagePicker.launch("image/*") },
                    shape = CircleShape,
                    color = appThemedColor(Color(0xFFF1F2F4)),
                    border = BorderStroke(1.dp, Color.White),
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (profilePicSaving) CircularProgressIndicator(modifier = Modifier.size(13.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Edit, contentDescription = tr("Edit profile picture", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (nickname.isNotBlank()) {
                Text(nickname, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            }
            if (email.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(email, color = Ink, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(5.dp))
            Text(tr("Sri Lakshmi Ganapathi Ayurveda Nilayam", LocalAppLanguage.current), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple("My orders", Icons.Default.Inventory2, "MyOrders"),
                Triple("My cart", Icons.Default.ShoppingCart, "Cart"),
                Triple("Need help?", Icons.Default.SupportAgent, "Help")
            ).forEach { (label, icon, route) ->
                Card(
                    modifier = Modifier.weight(1f).height(76.dp).clickable { onTabChange(route) },
                    shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(tr(label, LocalAppLanguage.current), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(appThemedColor(Color(0xFFF1F2F4)), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Spa, contentDescription = null, tint = Ink, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Your wellness, rooted in tradition", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Explore our Ayurvedic collection and updates.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
            Column {
                Text(tr("Your Information", LocalAppLanguage.current), modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 12.dp), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                AccountMenuRow(Icons.Default.PersonOutline, "Personal information") { onTabChange("Profile") }
                AccountMenuRow(Icons.Default.LocationOn, "Address book") { onTabChange("AddressBook") }
                AccountMenuRow(Icons.Default.ReceiptLong, "My orders") { onTabChange("MyOrders") }
                AccountMenuRow(Icons.Default.FavoriteBorder, "My Wishlist") { onTabChange("Wishlist") }
                AccountMenuRow(Icons.Default.MedicalInformation, "Diseases and remedies") { onTabChange("Diseases") }
                AccountMenuRow(Icons.Default.Language, "Language", appLanguage) {
                    selectedLanguage = appLanguage
                    showLanguageSheet = true
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
            Column {
                Text(tr("About And Support", LocalAppLanguage.current), modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 12.dp), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                AccountMenuRow(Icons.Default.HelpOutline, "Help and support") { onTabChange("Help") }
                AccountMenuRow(Icons.Default.Info, "About SLGAN") { onTabChange("About") }
                AccountMenuRow(Icons.Default.Phone, "Contact SLGAN") { onTabChange("ContactSLGAN") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
            Column {
                Text(tr("Other Information", LocalAppLanguage.current), modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 12.dp), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                AccountMenuRow(Icons.Default.Share, "Share the app") {
                    val shareText = "Try Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN). Browse our Ayurvedic product collection."
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Sri Lakshmi Ganapathi Ayurveda Nilayam")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share SLGAN"))
                }
                AccountMenuRow(Icons.Default.Groups, "Social Media", onClick = { onTabChange("SocialMedia") })
                AccountMenuRow(Icons.Default.Lock, "Account Privacy", onClick = { onTabChange("AccountPrivacy") })
                AccountMenuRow(Icons.Default.NotificationsNone, "Notification preferences") {
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Notification preferences")
                        .setMessage("Notification controls are not connected in this version of SLGAN.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                AccountMenuRow(Icons.Default.Logout, "Log out") { showLogoutDialog = true }
            }
        }
        Spacer(Modifier.height(24.dp))
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false; languageDropdownExpanded = false },
            containerColor = Color(0xFFF3F4F5),
            contentColor = Color(0xFF25282D),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    Modifier.padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 5.dp)
                        .background(Color(0xFFB5B5B5), CircleShape)
                )
            }
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(tr("Choose Your Language", LocalAppLanguage.current), color = Color(0xFF25282D), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(tr("Select your preferred language for SLGAN.", LocalAppLanguage.current), color = Color(0xFF707780), fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                listOf("English", "Telugu", "Hindi").forEach { option ->
                    val isSelected = selectedLanguage == option
                    val nativeName = when (option) {
                        "Telugu" -> "తెలుగు"
                        "Hindi" -> "हिन्दी"
                        else -> "English"
                    }
                    val symbol = when (option) {
                        "Telugu" -> "అ"
                        "Hindi" -> "अ"
                        else -> "A"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { selectedLanguage = option },
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color(0xFF25282D) else Color(0xFFB5B5B5)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFE2E2E2) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(46.dp).background(Color(0xFFF3F4F5), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(symbol, color = Color(0xFF25282D), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(nativeName, color = Color(0xFF25282D), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(option, color = Color(0xFF707780), fontSize = 12.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = tr("Selected", LocalAppLanguage.current), tint = Color(0xFF25282D), modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Circle, contentDescription = tr("Not selected", LocalAppLanguage.current), tint = Color(0xFFB5B5B5), modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val uid = user?.uid
                        if (uid == null) {
                            Toast.makeText(context, "Please sign in again to save your language.", Toast.LENGTH_SHORT).show()
                        } else {
                            languageSaving = true
                            FirebaseData.saveCustomerProfile(uid, mapOf(
                                "languagePreference" to selectedLanguage,
                                "updatedAt" to System.currentTimeMillis()
                            )) { error ->
                                languageSaving = false
                                if (error == null) {
                                    onLanguageChange(selectedLanguage)
                                    showLanguageSheet = false
                                    Toast.makeText(context, "Language preference saved.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Couldn't save language. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !languageSaving,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25282D),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB5B5B5),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (languageSaving) CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(tr("Continue", LocalAppLanguage.current), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
    if (showLogoutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFFF3F4F5),
            contentColor = Color(0xFF25282D),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    Modifier.padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 5.dp)
                        .background(Color(0xFFB5B5B5), CircleShape)
                )
            }
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
            ) {
                Text(tr("Log out from?", LocalAppLanguage.current), color = Color(0xFF25282D), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(tr("Choose where you want to end your SLGAN session.", LocalAppLanguage.current), color = Color(0xFF707780), fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable {
                        showLogoutDialog = false
                        onSignOut()
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFB5B5B5)),
                    colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(46.dp).background(Color(0xFFF3F4F5), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = Color(0xFF25282D), modifier = Modifier.size(23.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tr("Current Device", LocalAppLanguage.current), color = Color(0xFF25282D), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(tr("Sign out from this phone", LocalAppLanguage.current), color = Color(0xFF707780), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = tr("Sign out from current device", LocalAppLanguage.current), tint = Color(0xFF25282D))
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable {
                        showLogoutDialog = false
                        Toast.makeText(context, "This app can sign out this device only. Signing out all devices requires secure server support.", Toast.LENGTH_LONG).show()
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFB5B5B5)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E2E2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(46.dp).background(Color(0xFFF3F4F5), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0xFF25282D), modifier = Modifier.size(23.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tr("All Devices", LocalAppLanguage.current), color = Color(0xFF25282D), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(tr("Requires secure server support", LocalAppLanguage.current), color = Color(0xFF707780), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = tr("All devices", LocalAppLanguage.current), tint = Color(0xFF25282D))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color(0xFFB5B5B5)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25282D))
                ) {
                    Text(tr("Cancel", LocalAppLanguage.current), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
    }
}

private data class SupportChatMessage(val text: String, val fromUser: Boolean)

private fun supportFaqReply(question: String, language: String): String {
    val q = question.lowercase(Locale.ROOT)
    val telugu = language != "English"
    return when {
        listOf("hello", "hi", "namaste", "నమస్కారం", "హాయ్").any { q.contains(it) } ->
            if (telugu) "నమస్కారం! నేను SLGAN సహాయకుడిని. ఉత్పత్తులు, ఆర్డర్లు లేదా డెలివరీ గురించి అడగండి." else "Hello! I'm the SLGAN assistant. Ask me about products, orders, or delivery."
        listOf("order", "ఆర్డర్").any { q.contains(it) } ->
            if (telugu) "మీ ఆర్డర్ వివరాల కోసం Accounts → My Orders తెరవండి. అక్కడ వివరాలు కనిపించకపోతే, మీ ఆర్డర్ IDతో మా support‌ను సంప్రదించండి." else "Open Accounts → My Orders to check your order. If details are missing, contact support with your order ID."
        listOf("delivery", "shipping", "డెలివరీ", "చేరడానికి").any { q.contains(it) } ->
            if (telugu) "డెలివరీ వివరాలు ఆర్డర్ నిర్ధారణ తర్వాత అందుబాటులో ఉంటాయి. మీ ఆర్డర్‌ను My Orders‌లో చూడండి." else "Delivery details are available after order confirmation. Check your order in My Orders."
        listOf("invoice", "bill", "ఇన్వాయిస్", "బిల్లు").any { q.contains(it) } ->
            if (telugu) "Accounts → My Orders‌లో మీ ఆర్డర్‌ను తెరిచి invoice లింక్ అందుబాటులో ఉందో చూడండి." else "Open your order in Accounts → My Orders and look for the invoice link."
        listOf("price", "cost", "ధర").any { q.contains(it) } ->
            if (telugu) "ప్రస్తుత ధరను app‌లోని సంబంధిత product page‌లో చూడండి. ధర కనిపించకపోతే support‌ను సంప్రదించండి." else "Check the product page for the current price. If a price is missing, contact support."
        listOf("product", "medicine", "మందు", "ఉత్పత్తి").any { q.contains(it) } ->
            if (telugu) "ఉత్పత్తి వివరాలు, ingredients, usage కోసం product page చూడండి. ఆరోగ్య సమస్యలకు లేదా మందుల మోతాదుకు అర్హత కలిగిన వైద్యుడిని సంప్రదించండి." else "Check the product page for ingredients and usage. For health conditions or dosage, consult a qualified healthcare professional."
        else -> if (telugu) "ఇది సాధారణ FAQ సహాయం. మీ ప్రశ్నకు సమాధానం దొరకకపోతే, AI support‌ను కనెక్ట్ చేసిన తర్వాత మరింత వివరంగా అడగవచ్చు. ఆర్డర్ లేదా ఉత్పత్తి వివరాలు చెప్పండి." else "This is the FAQ helper. Once AI support is connected, it can handle more detailed questions. Please mention the order or product details."
    }
}

@Composable
private fun HelpPage(modifier: Modifier, onBack: () -> Unit, appLanguage: String) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            SupportChatMessage(
                if (appLanguage == "English") "Hi! I'm the SLGAN support assistant. Ask me in Telugu or English about products, orders, invoices, or delivery." else "నమస్కారం! నేను SLGAN support assistant‌ను. Products, orders, invoices లేదా delivery గురించి తెలుగులో లేదా English‌లో అడగండి.",
                false
            )
        )
    }
    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF1F2F6)))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Forest) }
            Column(Modifier.weight(1f)) {
                Text(tr("SLGAN Assistant", LocalAppLanguage.current), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(tr("Help & Support · Telugu + English", appLanguage), color = Muted, fontSize = 12.sp)
            }
            Surface(shape = RoundedCornerShape(14.dp), color = appThemedColor(Color(0xFFF1F1F1)), modifier = Modifier.size(44.dp), border = BorderStroke(1.dp, appThemedColor(Color(0xFFE5E8EF)))) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SupportAgent, contentDescription = tr("SLGAN support assistant", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(27.dp)) } }
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(tr("How Can We Help?", appLanguage), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(tr("Quick answers for products, orders and delivery. AI replies become available when the secure support service is configured.", appLanguage), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.forEach { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (message.fromUser) Forest else Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Text(message.text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = if (message.fromUser) Color.White else Ink, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
            if (sending) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Forest, strokeWidth = 2.dp)
                    Text(tr("Thinking…", appLanguage), color = Muted, fontSize = 12.sp)
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(appThemedColor(Color(0xFFF1F2F6))).padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    if (appLanguage == "English") "Order status" else "ఆర్డర్ వివరాలు",
                    if (appLanguage == "English") "Product details" else "ఉత్పత్తి వివరాలు",
                    if (appLanguage == "English") "Delivery" else "డెలివరీ"
                ).forEach { suggestion ->
                    SuggestionChip(
                        onClick = { input = suggestion },
                        label = { Text(suggestion, fontSize = 11.sp, maxLines = 1, color = Ink) },
                        border = null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = Ink.copy(alpha = 0.10f),
                            spotColor = Ink.copy(alpha = 0.14f)
                        ),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = appThemedColor(Color.White),
                            labelColor = Ink
                        )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(1000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(tr("Type in Telugu or English…", appLanguage), fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4,
                    enabled = !sending
                )
                IconButton(
                    enabled = input.isNotBlank() && !sending,
                    onClick = {
                        val question = input.trim()
                        if (question.isNotBlank()) {
                            messages.add(SupportChatMessage(question, true))
                            input = ""
                            sending = true
                            scope.launch {
                                val answer = withContext(Dispatchers.IO) {
                                    val endpoint = BuildConfig.CHATBOT_API_BASE_URL.trim().trimEnd('/')
                                    if (endpoint.isBlank() || user == null) {
                                        runCatching {
                                            val prompt = """
                                                You are the SLGAN Assistant for Sri Lakshmi Ganapathi Ayurveda Nilayam.
                                                Reply naturally in the user's language (Telugu, English, or mixed Telugu-English).
                                                Help with app navigation, product catalogue information, orders, invoices, and delivery.
                                                Never invent product prices, ingredients, stock, order status, or customer-specific details.
                                                Do not diagnose, prescribe, recommend medicine dosages, claim any Ayurvedic product cures a disease, or advise stopping prescribed treatment.
                                                For health concerns, recommend consulting a qualified healthcare professional.
                                                Never ask for passwords, OTPs, payment details, or unnecessary personal data.
                                                Keep replies concise, polite, and clear.
                                                Preferred language: $appLanguage
                                                User question: $question
                                            """.trimIndent()
                                            val model = Firebase.ai(backend = GenerativeBackend.googleAI(), useLimitedUseAppCheckTokens = true)
                                                .generativeModel("gemini-3.8-flash")
                                            model.generateContent(prompt).text?.takeIf { it.isNotBlank() }
                                                ?: throw IllegalStateException("Gemini returned an empty reply")
                                        }.getOrElse { supportFaqReply(question, appLanguage) }
                                    } else {
                                        runCatching {
                                            val token = Tasks.await(user.getIdToken(false)).token
                                                ?: throw IllegalStateException("Unable to verify sign-in")
                                            val connection = (URL("$endpoint/chat").openConnection() as HttpURLConnection)
                                            try {
                                                connection.requestMethod = "POST"
                                                connection.connectTimeout = 10000
                                                connection.readTimeout = 25000
                                                connection.doOutput = true
                                                connection.setRequestProperty("Content-Type", "application/json")
                                                connection.setRequestProperty("Authorization", "Bearer $token")
                                                val payload = JSONObject().put("message", question).put("language", appLanguage).toString()
                                                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                                                val code = connection.responseCode
                                                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                                                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                                                if (code !in 200..299) throw IllegalStateException("Support service returned $code")
                                                JSONObject(body).optString("reply").takeIf { it.isNotBlank() }
                                                    ?: throw IllegalStateException("Empty reply")
                                            } finally {
                                                connection.disconnect()
                                            }
                                        }.getOrElse { supportFaqReply(question, appLanguage) }
                                    }
                                }
                                messages.add(SupportChatMessage(answer, false))
                                sending = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = tr("Send message", LocalAppLanguage.current), tint = if (input.isNotBlank() && !sending) Forest else Muted)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                if (appLanguage == "English") "AI can make mistakes. For medical advice or dosage, consult a qualified clinician." else "AI సమాధానాల్లో తప్పులు ఉండవచ్చు. వైద్య సలహా లేదా మోతాదు కోసం అర్హత కలిగిన వైద్యుడిని సంప్రదించండి.",
                color = Muted, fontSize = 10.sp, lineHeight = 14.sp
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddressBookPage(modifier: Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val authUser = FirebaseAuth.getInstance().currentUser
    var addresses by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedAddressId by remember(authUser?.uid) { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddAddressPage by remember { mutableStateOf(false) }
    var editAddress by remember { mutableStateOf<Map<String, Any>?>(null) }
    var useCurrentLocationForAddress by remember { mutableStateOf(false) }
    var showAddAddressSheet by remember { mutableStateOf(false) }
    var addressOptions by remember { mutableStateOf<Map<String, Any>?>(null) }
    var confirmDeleteAddress by remember { mutableStateOf<Map<String, Any>?>(null) }
    var currentLocationAddress by remember { mutableStateOf("Detecting your current address…") }
    var locationLookupStarted by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        if (locationLookupStarted) return@LaunchedEffect
        locationLookupStarted = true
        currentLocationAddress = withContext(Dispatchers.IO) {
            try {
                val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!fine && !coarse) return@withContext "Allow location access to detect your address"
                val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                    .asSequence()
                    .filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
                    .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
                    .maxByOrNull { it.time }
                    ?: return@withContext "Turn on device location to detect your address"
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    kotlinx.coroutines.suspendCancellableCoroutine<List<android.location.Address>?> { continuation ->
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            if (continuation.isActive) continuation.resume(addresses)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                }
                val address = results?.firstOrNull()
                address?.getAddressLine(0)?.takeIf { it.isNotBlank() }
                    ?: listOfNotNull(address?.subLocality, address?.locality, address?.adminArea).joinToString(", ")
                        .ifBlank { "Location detected; address unavailable" }
            } catch (_: Exception) {
                "Couldn't detect address. Please enter it manually."
            }
        }
    }

    DisposableEffect(authUser?.uid) {
        val uid = authUser?.uid
        if (uid == null) {
            addresses = emptyList()
            selectedAddressId = null
            loading = false
            onDispose { }
        } else {
            loading = true
            val registration = FirebaseFirestore.getInstance().collection("addresses")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        loading = false
                        Toast.makeText(context, error.localizedMessage ?: "Could not load saved addresses", Toast.LENGTH_LONG).show()
                    } else if (snapshot != null) {
                        addresses = snapshot.documents.map { doc -> doc.data.orEmpty() + ("_id" to doc.id) }
                        val preferred = addresses.firstOrNull { it["label"]?.toString()?.equals("Home", ignoreCase = true) == true }
                            ?: addresses.firstOrNull { it["isDefault"] == true || it["default"] == true }
                            ?: addresses.firstOrNull()
                        selectedAddressId = preferred?.get("_id")?.toString()
                        loading = false
                    }
                }
            onDispose { registration.remove() }
        }
    }

    LaunchedEffect(authUser?.uid) {
        kotlinx.coroutines.delay(10_000)
        if (loading) loading = false
    }

    fun setDefaultAddress(id: String) {
        if (id.isBlank() || id == selectedAddressId) return
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        addresses.forEach { existing ->
            val existingId = existing["_id"]?.toString().orEmpty()
            if (existingId.isNotBlank()) {
                val makeDefault = existingId == id
                batch.update(db.collection("addresses").document(existingId),
                    "isDefault", makeDefault, "default", makeDefault)
            }
        }
        batch.commit().addOnSuccessListener {
            selectedAddressId = id
            addresses = addresses.map { row ->
                val makeDefault = row["_id"]?.toString() == id
                row + ("isDefault" to makeDefault) + ("default" to makeDefault)
            }
            Toast.makeText(context, "Default delivery address updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { error ->
            Toast.makeText(context, error.localizedMessage ?: "Could not update default address", Toast.LENGTH_LONG).show()
        }
    }

    addressOptions?.let { address ->
        ModalBottomSheet(onDismissRequest = { addressOptions = null }, containerColor = appThemedColor(Color.White), contentColor = Ink) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("Address Options", LocalAppLanguage.current), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().clickable { confirmDeleteAddress = address; addressOptions = null }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Ink); Spacer(Modifier.width(12.dp)); Text(tr("Delete address", LocalAppLanguage.current), color = Ink, fontSize = 16.sp)
                }
                Row(Modifier.fillMaxWidth().clickable {
                    editAddress = address
                    useCurrentLocationForAddress = false
                    showAddAddressPage = true
                    addressOptions = null
                }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Ink); Spacer(Modifier.width(12.dp)); Text(tr("Edit address", LocalAppLanguage.current), color = Ink, fontSize = 16.sp)
                }
                Row(Modifier.fillMaxWidth().clickable {
                    setDefaultAddress(address["_id"]?.toString().orEmpty())
                    addressOptions = null
                }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Ink); Spacer(Modifier.width(12.dp)); Text(tr("Set as delivery address", LocalAppLanguage.current), color = Ink, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    confirmDeleteAddress?.let { address ->
        AlertDialog(onDismissRequest = { confirmDeleteAddress = null }, title = { Text(tr("Delete address?", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold) }, text = { Text(tr("Are you sure you want to delete this saved address?", LocalAppLanguage.current), color = Muted) }, confirmButton = {
            TextButton(onClick = {
                val id = address["_id"]?.toString().orEmpty()
                if (id.isNotBlank()) FirebaseFirestore.getInstance().collection("addresses").document(id).delete()
                    .addOnSuccessListener { addresses = addresses.filterNot { it["_id"]?.toString() == id }; if (selectedAddressId == id) selectedAddressId = addresses.firstOrNull()?.get("_id")?.toString(); Toast.makeText(context, "Address deleted", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(context, e.localizedMessage ?: "Could not delete address", Toast.LENGTH_LONG).show() }
                confirmDeleteAddress = null
            }) { Text(tr("Delete", LocalAppLanguage.current), color = appThemedColor(Color(0xFFC62828))) }
        }, dismissButton = { TextButton(onClick = { confirmDeleteAddress = null }) { Text(tr("Cancel", LocalAppLanguage.current), color = Ink) } })
    }

    if (showAddAddressPage) {
        AddAddressDetailsPage(
            useCurrentLocation = useCurrentLocationForAddress,
            editAddress = editAddress,
            onBack = { showAddAddressPage = false; editAddress = null },
            onSaved = { saved ->
                val savedId = saved["_id"]?.toString()
                addresses = if (savedId.isNullOrBlank()) listOf(saved) + addresses else {
                    if (addresses.any { it["_id"]?.toString() == savedId }) {
                        addresses.map { if (it["_id"]?.toString() == savedId) saved else it }
                    } else listOf(saved) + addresses
                }
                selectedAddressId = savedId
                loading = false
                showAddAddressPage = false
                editAddress = null
                useCurrentLocationForAddress = false
                Toast.makeText(context, "Address saved successfully", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
    Column(modifier.fillMaxSize().background(appThemedColor(Color(0xFFF2F4FA)))) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Saved Addresses", LocalAppLanguage.current), modifier = Modifier.weight(1f), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Box(
                    Modifier.size(44.dp)
                        .shadow(5.dp, RoundedCornerShape(DefaultBtn.cornerRadius))
                        .background(appThemedColor(DefaultBtn.background), RoundedCornerShape(DefaultBtn.cornerRadius))
                        .border(1.dp, appThemedColor(DefaultBtn.border), RoundedCornerShape(DefaultBtn.cornerRadius))
                        .clickable { showAddAddressSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = tr("Add address", LocalAppLanguage.current), tint = appThemedColor(DefaultBtn.content), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(tr("Keep delivery locations for future orders.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    AppLoader("Loading addresses…", compact = true)
                }
            } else if (addresses.isEmpty()) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(tr("No saved addresses yet", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Your saved delivery addresses will appear here.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                addresses.forEach { address ->
                    val name = address["name"]?.toString().orEmpty()
                        .ifBlank { address["fullName"]?.toString().orEmpty() }
                        .ifBlank { address["receiverName"]?.toString().orEmpty() }
                        .ifBlank { "Delivery address" }
                    val label = address["label"]?.toString().orEmpty().ifBlank { "HOME" }
                    val phone = address["phone"]?.toString().orEmpty()
                        .ifBlank { address["mobile"]?.toString().orEmpty() }
                        .ifBlank { address["receiverPhone"]?.toString().orEmpty() }
                    val completeAddress = listOf("completeAddress", "complete_address", "formattedAddress", "formatted_address", "fullAddress", "full_address")
                        .firstNotNullOfOrNull { key -> address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
                    val addressKeys = listOf(
                        "address", "addressText", "addressLine1", "addressLine2", "houseNo", "houseNumber",
                        "building", "apartment", "street", "streetName", "road", "landmark", "area", "locality",
                        "village", "town", "city", "district", "state", "pincode", "postalCode", "zipCode", "country"
                    )
                    val addressParts = addressKeys.mapNotNull { key ->
                        address[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
                    }.distinctBy { it.lowercase(Locale.ROOT) }
                    val addressText = (listOf(completeAddress).filter { it.isNotBlank() } + addressParts)
                        .distinctBy { it.lowercase(Locale.ROOT) }
                        .fold(mutableListOf<String>()) { result, part ->
                            if (result.none { it.contains(part, ignoreCase = true) || part.contains(it, ignoreCase = true) }) result.add(part)
                            result
                        }.joinToString(", ")
                    val isDefault = address["isDefault"] == true || address["default"] == true
                    val addressId = address["_id"]?.toString().orEmpty()
                    val isSelected = selectedAddressId == addressId
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                            setDefaultAddress(addressId)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                Modifier.width(66.dp).border(1.dp, appThemedColor(Color(0xFFD9DDE7)), RoundedCornerShape(14.dp))
                                    .background(appThemedColor(Color.White), RoundedCornerShape(14.dp)).padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    when (label.lowercase()) {
                                        "home" -> Icons.Default.Home
                                        "work" -> Icons.Default.Work
                                        "hotel" -> Icons.Default.Business
                                        else -> Icons.Default.People
                                    },
                                    contentDescription = null,
                                    tint = appThemedColor(Color(0xFFB5B5B5)),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    tr(label.trim().ifBlank { "Other" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, LocalAppLanguage.current),
                                    color = Muted,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier.size(34.dp)
                                            .background(
                                                if (isSelected) appThemedColor(Color(0xFF25884B)) else appThemedColor(Color(0xFFE2E2E2)),
                                                CircleShape
                                            )
                                            .clickable { setDefaultAddress(addressId) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = if (isSelected) "Selected address" else "Select address",
                                            tint = if (isSelected) Color.White else appThemedColor(Color(0xFFB5B5B5)),
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                }
                                if (phone.isNotBlank()) {
                                    Text(phone, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                                if (addressText.isNotBlank()) {
                                    Text(addressText, color = Muted, fontSize = 14.sp, lineHeight = 20.sp, softWrap = true, modifier = Modifier.padding(top = 3.dp))
                                }
                                Row(
                                    Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = { addressOptions = address },
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.dp, appThemedColor(Color(0xFFE2E2E2)))
                                    ) {
                                        Icon(
                                            Icons.Default.MoreHoriz,
                                            contentDescription = tr("Address Options", LocalAppLanguage.current),
                                            tint = Ink,
                                            modifier = Modifier.padding(7.dp).size(18.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            val shareText = listOf(label, addressText, phone.takeIf { it.isNotBlank() }?.let { "Phone number: $it" })
                                                .filterNotNull().filter { it.isNotBlank() }.joinToString("\n")
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share address"))
                                        },
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.dp, appThemedColor(Color(0xFFE2E2E2)))
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = tr("Share address", LocalAppLanguage.current),
                                            tint = Ink,
                                            modifier = Modifier.padding(7.dp).size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    if (showAddAddressSheet && !showAddAddressPage) {
        ModalBottomSheet(
            onDismissRequest = { showAddAddressSheet = false },
            containerColor = appThemedColor(Color.White),
            contentColor = Ink,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            dragHandle = {
                Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 38.dp, height = 4.dp).background(appThemedColor(Color(0xFFD7D9DE)), CircleShape))
            }
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
                    .padding(top = 2.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(44.dp).shadow(4.dp, CircleShape)
                        .background(appThemedColor(Color(0xFFB2B2B2)), CircleShape)
                        .clickable { showAddAddressSheet = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Ink, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(112.dp)
                        .background(appThemedColor(Color(0xFFFAFAFA)), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = appThemedColor(Color(0xFF21852A)), modifier = Modifier.size(58.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(tr("DELIVERY LOCATION", LocalAppLanguage.current), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Do you want this order at your current location?",
                    color = Ink,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        showAddAddressSheet = false
                        showAddAddressPage = true
                        useCurrentLocationForAddress = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, appThemedColor(Color(0xFF21852A))),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = appThemedColor(Color(0xFF21852A))),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(tr("Yes, deliver at my current location", LocalAppLanguage.current), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentLocationAddress, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        showAddAddressSheet = false
                        useCurrentLocationForAddress = false
                        showAddAddressPage = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, appThemedColor(Color(0xFF21852A))),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = appThemedColor(Color(0xFF21852A))),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(tr("No, at some other location", LocalAppLanguage.current), color = appThemedColor(Color(0xFF21852A)), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAddressDetailsPage(
    useCurrentLocation: Boolean = false,
    editAddress: Map<String, Any>? = null,
    onBack: () -> Unit,
    onSaved: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    var city by remember(editAddress?.get("_id")) { mutableStateOf(editAddress?.get("city")?.toString().orEmpty()) }
    var area by remember(editAddress?.get("_id")) { mutableStateOf(editAddress?.get("area")?.toString().orEmpty()) }
    var fullAddress by remember(editAddress?.get("_id")) { mutableStateOf(listOf("address", "addressText", "fullAddress", "completeAddress", "complete_address", "formattedAddress", "formatted_address", "complete address").firstNotNullOfOrNull { key -> editAddress?.get(key)?.toString()?.trim()?.takeIf { it.isNotBlank() } } ?: listOf("addressLine1", "addressLine2", "landmark", "street", "area", "city", "state", "pincode", "postalCode").mapNotNull { key -> editAddress?.get(key)?.toString()?.trim()?.takeIf { it.isNotBlank() } }.joinToString(", ")) }
    var mapLink by remember(editAddress?.get("_id")) { mutableStateOf(editAddress?.get("googleMapsLink")?.toString().orEmpty()) }
    var mapLatitude by remember(editAddress?.get("_id")) { mutableStateOf((editAddress?.get("latitude") as? Number)?.toDouble()) }
    var mapLongitude by remember(editAddress?.get("_id")) { mutableStateOf((editAddress?.get("longitude") as? Number)?.toDouble()) }
    var mapLocationResolved by remember(editAddress?.get("_id")) { mutableStateOf(mapLatitude != null && mapLongitude != null) }
    var receiverName by remember(editAddress?.get("_id"), useCurrentLocation) { mutableStateOf(editAddress?.get("name")?.toString() ?: if (useCurrentLocation) user?.displayName.orEmpty() else "") }
    var receiverPhone by remember(editAddress?.get("_id"), useCurrentLocation) { mutableStateOf(editAddress?.get("phone")?.toString() ?: if (useCurrentLocation) user?.phoneNumber.orEmpty() else "") }
    var label by remember(editAddress?.get("_id"), useCurrentLocation) { mutableStateOf(editAddress?.get("label")?.toString() ?: if (useCurrentLocation) "Home" else "Other") }
    var forSomeoneElse by remember(editAddress?.get("_id"), useCurrentLocation) {
        mutableStateOf(
            if (editAddress != null) !editAddress["label"]?.toString().equals("Home", ignoreCase = true)
            else !useCurrentLocation
        )
    }
    var profileName by remember(user?.uid) { mutableStateOf(user?.displayName.orEmpty()) }
    var profilePhone by remember(user?.uid) { mutableStateOf(user?.phoneNumber.orEmpty()) }
    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("customers").document(uid).get().addOnSuccessListener { doc ->
            val savedFirstName = doc.getString("firstName").orEmpty().trim()
            val savedLastName = doc.getString("lastName").orEmpty().trim()
            profileName = listOf(savedFirstName, savedLastName).filter { it.isNotBlank() }.joinToString(" ")
                .ifBlank { (doc.getString("name") ?: doc.getString("fullName") ?: doc.getString("displayName") ?: doc.getString("nickname") ?: user?.displayName).orEmpty() }
            val savedMobile = (doc.getString("mobileNumber") ?: doc.getString("phone") ?: doc.getString("phoneNumber") ?: doc.getString("mobile") ?: user?.phoneNumber).orEmpty().trim()
            val savedCountryCode = doc.getString("countryCode").orEmpty().trim()
            profilePhone = if (savedMobile.isNotBlank() && savedCountryCode.isNotBlank() && !savedMobile.startsWith("+") && savedMobile.startsWith(savedCountryCode).not()) {
                savedCountryCode + savedMobile
            } else savedMobile
            if (!forSomeoneElse) {
                receiverName = profileName
                receiverPhone = profilePhone
            }
        }
    }
    var saving by remember { mutableStateOf(false) }
    var cityError by remember { mutableStateOf(false) }
    var areaError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf(false) }
    var areaMenu by remember { mutableStateOf(false) }
    var citySearch by remember { mutableStateOf("") }
    var areaSearch by remember { mutableStateOf("") }
    val cities = listOf(
        "Abohar", "Adoni", "Agartala", "Agra", "Ahmedabad", "Ahmednagar", "Aizawl", "Ajmer",
        "Akola", "Alappuzha", "Aligarh", "Allahabad", "Alwar", "Amravati", "Amritsar", "Anantapur",
        "Arrah", "Asansol", "Aurangabad", "Bengaluru", "Berhampur", "Bhilai", "Bhopal", "Bhubaneswar",
        "Bikaner", "Bilaspur", "Chandigarh", "Chennai", "Coimbatore", "Cuttack", "Dehradun", "Delhi",
        "Dhanbad", "Dibrugarh", "Durgapur", "Faridabad", "Gandhinagar", "Ghaziabad", "Guntur", "Gurugram",
        "Guwahati", "Gwalior", "Hyderabad", "Imphal", "Indore", "Jabalpur", "Jaipur", "Jalandhar",
        "Jammu", "Jamnagar", "Jamshedpur", "Jhansi", "Jodhpur", "Kakinada", "Kannur", "Kanpur",
        "Kochi", "Kolhapur", "Kolkata", "Kollam", "Kota", "Kozhikode", "Kurnool", "Lucknow",
        "Ludhiana", "Madurai", "Mangaluru", "Meerut", "Mumbai", "Mysuru", "Nagpur", "Nanded",
        "Nashik", "Nellore", "New Delhi", "Noida", "Ongole", "Palakkad", "Panaji", "Patna",
        "Pondicherry", "Prayagraj", "Pune", "Raipur", "Rajahmundry", "Ranchi", "Rourkela", "Salem",
        "Sangli", "Shimla", "Siliguri", "Srinagar", "Surat", "Thane", "Thiruvananthapuram", "Tirupati",
        "Udaipur", "Ujjain", "Vadodara", "Varanasi", "Vellore", "Vijayawada", "Visakhapatnam", "Warangal",
        "Yadadri Bhuvanagiri"
    ).distinct().sorted()

    LaunchedEffect(useCurrentLocation) {
        if (!useCurrentLocation) return@LaunchedEffect
        val detected = withContext(Dispatchers.IO) {
            try {
                val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!fine && !coarse) return@withContext null
                val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                    .asSequence()
                    .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
                    .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                    .maxByOrNull { it.time } ?: return@withContext null
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    kotlinx.coroutines.suspendCancellableCoroutine<List<android.location.Address>?> { continuation ->
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { list ->
                            if (continuation.isActive) continuation.resume(list)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                }
                results?.firstOrNull()
            } catch (_: Exception) { null }
        }
        if (detected != null) {
            mapLatitude = detected.latitude
            mapLongitude = detected.longitude
            mapLocationResolved = true
            city = detected.locality ?: detected.subAdminArea ?: detected.adminArea ?: city
            area = detected.subLocality ?: detected.thoroughfare ?: detected.featureName ?: area
            fullAddress = detected.getAddressLine(0).orEmpty()
                .split(",").drop(2).joinToString(",").trim().ifBlank { detected.getAddressLine(0).orEmpty() }
        } else {
            Toast.makeText(context, "Couldn't detect your location. Please enter the address manually.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(showReview, city, area, fullAddress, editAddress?.get("_id")) {
        if (!showReview || mapLocationResolved) return@LaunchedEffect
        val found = withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val searches = listOf(
                    listOf(fullAddress, area, city).filter { it.isNotBlank() }.joinToString(", "),
                    listOf(area, city).filter { it.isNotBlank() }.joinToString(", "),
                    city.trim()
                ).filter { it.isNotBlank() }.distinct()
                var match: android.location.Address? = null
                for (search in searches) {
                    val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        kotlinx.coroutines.suspendCancellableCoroutine<List<android.location.Address>?> { continuation ->
                            geocoder.getFromLocationName(search, 1) { list ->
                                if (continuation.isActive) continuation.resume(list)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(search, 1)
                    }
                    match = results?.firstOrNull()
                    if (match != null) break
                }
                match
            } catch (_: Exception) { null }
        }
        if (found != null) {
            mapLatitude = found.latitude
            mapLongitude = found.longitude
            mapLocationResolved = true
            if (area.isBlank()) area = found.subLocality ?: found.thoroughfare ?: found.featureName.orEmpty()
            if (city.isBlank()) city = found.locality ?: found.subAdminArea ?: found.adminArea.orEmpty()
        } else {
            Toast.makeText(context, "Address not found. Try entering a nearby area and city to place the pin.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(mapLatitude, mapLongitude) {
        val lat = mapLatitude ?: return@LaunchedEffect
        val lon = mapLongitude ?: return@LaunchedEffect

        // Wait until the user finishes moving the pin so reverse geocoding
        // does not run for every small movement during a drag.
        delay(350)

        val resolved = withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    kotlinx.coroutines.suspendCancellableCoroutine<List<android.location.Address>?> { continuation ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            if (continuation.isActive) continuation.resume(list)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)
                }
                results?.firstOrNull()
            } catch (_: Exception) { null }
        }

        // Ignore a slow response if the pin has moved again while lookup ran.
        if (mapLatitude != lat || mapLongitude != lon) return@LaunchedEffect

        if (resolved != null) {
            resolved.getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { fullAddress = it }
            resolved.subLocality?.takeIf { it.isNotBlank() }?.let { area = it }
            (resolved.locality ?: resolved.subAdminArea ?: resolved.adminArea)
                ?.takeIf { it.isNotBlank() }?.let { city = it }
        }
        mapLink = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
    }
    if (showReview) {
        ReviewAddressPage(
            city = city,
            area = area,
            fullAddress = fullAddress,
            receiverName = receiverName,
            receiverPhone = receiverPhone,
            initialLatitude = mapLatitude,
            initialLongitude = mapLongitude,
            onLocationSelected = { latitude, longitude -> mapLatitude = latitude; mapLongitude = longitude; mapLocationResolved = true },
            onEdit = { showReview = false },
            onSave = {
                if (mapLatitude == null || mapLongitude == null) {
                    Toast.makeText(context, "Please wait for the map or enter a nearby area and city to select your delivery pin.", Toast.LENGTH_LONG).show()
                    return@ReviewAddressPage
                }
                cityError = city.isBlank()
                areaError = area.isBlank()
                addressError = fullAddress.isBlank()
                nameError = receiverName.isBlank()
                phoneError = receiverPhone.filter(Char::isDigit).length < 10
                if (cityError || areaError || addressError || nameError || phoneError) {
                    showReview = false
                    return@ReviewAddressPage
                }
                if (user == null) {
                    Toast.makeText(context, "Please sign in to save an address.", Toast.LENGTH_LONG).show()
                    return@ReviewAddressPage
                }
                saving = true
                val data = hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "city" to city.trim(),
                    "area" to area.trim(),
                    "address" to fullAddress.trim(),
                    "addressText" to fullAddress.trim(),
                    "googleMapsLink" to mapLink.trim(),
                    "latitude" to (mapLatitude ?: 0.0),
                    "longitude" to (mapLongitude ?: 0.0),
                    "name" to receiverName.trim(),
                    "phone" to receiverPhone.trim(),
                    "label" to label.trim().ifBlank { "Other" },
                    "isDefault" to (editAddress?.get("isDefault") as? Boolean ?: false),
                    "createdAt" to (editAddress?.get("createdAt") ?: com.google.firebase.Timestamp.now())
                )
                val addressCollection = FirebaseFirestore.getInstance().collection("addresses")
                val existingId = editAddress?.get("_id")?.toString()
                val normalizedLabel = label.trim().ifBlank { "Other" }
                addressCollection.whereEqualTo("userId", user.uid).get()
                    .addOnSuccessListener { snapshot ->
                        val duplicate = normalizedLabel.equals("Home", ignoreCase = true) ||
                            normalizedLabel.equals("Work", ignoreCase = true)
                        val alreadyUsed = duplicate && snapshot.documents.any { document ->
                            document.id != existingId &&
                                document.getString("label")?.equals(normalizedLabel, ignoreCase = true) == true
                        }
                        if (alreadyUsed) {
                            saving = false
                            Toast.makeText(
                                context,
                                "You can save only one $normalizedLabel address. Please edit or delete the existing one.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }
                        // The first saved address becomes the default automatically.
                        if (existingId.isNullOrBlank() && snapshot.documents.isEmpty()) {
                            data["isDefault"] = true
                            data["default"] = true
                        }
                        val saveTask = if (!existingId.isNullOrBlank()) {
                            addressCollection.document(existingId).set(data, com.google.firebase.firestore.SetOptions.merge())
                        } else {
                            addressCollection.add(data)
                        }
                        saveTask.addOnSuccessListener { result ->
                            saving = false
                            val docId = existingId ?: (result as? com.google.firebase.firestore.DocumentReference)?.id
                            onSaved(data + ("_id" to docId.orEmpty()))
                        }.addOnFailureListener {
                            saving = false
                            Toast.makeText(context, "Couldn't save address. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        saving = false
                        Toast.makeText(context, "Couldn't verify saved addresses. Please try again.", Toast.LENGTH_LONG).show()
                    }
            }
        )
        return
    }

    BackHandler(enabled = true) {
        if (showExitConfirmation) showExitConfirmation = false
        else showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = {
                Text(tr("Are you sure?", LocalAppLanguage.current), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text(
                    tr("Any changes on this screen may be lost if you continue", LocalAppLanguage.current),
                    textAlign = TextAlign.Center,
                    color = Muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appThemedColor(Color(0xFFE2E2E2)), contentColor = Ink),
                    border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(tr("Yes", LocalAppLanguage.current), color = Ink) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitConfirmation = false },
                    border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(tr("No", LocalAppLanguage.current), color = Ink) }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = appThemedColor(Color.White)
        )
    }

    Column(Modifier.fillMaxSize().background(appThemedColor(Color(0xFFF5F5F5)))) {
        Row(
            Modifier.fillMaxWidth().background(appThemedColor(Color.White)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showExitConfirmation = true }, modifier = Modifier.size(40.dp).border(1.dp, appThemedColor(Color(0xFFE2E2E2)), RoundedCornerShape(10.dp))) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text(if (editAddress != null) tr("Edit address details", LocalAppLanguage.current) else tr("Add address details", LocalAppLanguage.current), modifier = Modifier.padding(start = 14.dp), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Address Details", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Box {
                        OutlinedButton(
                            onClick = { citySearch = ""; cityMenu = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (cityError) appThemedColor(Color(0xFFE53945)) else appThemedColor(Color(0xFFB5B5B5))),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.LocationCity, contentDescription = null, tint = Ink)
                            Text(city.ifBlank { "Select a city" }, modifier = Modifier.weight(1f).padding(start = 12.dp), color = Ink, fontSize = 15.sp)
                            Text(tr("Select", LocalAppLanguage.current), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        if (cityMenu) {
                            ModalBottomSheet(
                                onDismissRequest = { cityMenu = false },
                                containerColor = appThemedColor(Color(0xFFF5F5F5)),
                                contentColor = Ink,
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                dragHandle = {
                                    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                                        Box(Modifier.width(44.dp).height(4.dp).background(appThemedColor(Color(0xFFB5B5B5)), CircleShape))
                                    }
                                }
                            ) {
                                Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 16.dp)) {
                                    Text(tr("Select a city", LocalAppLanguage.current), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 10.dp))
                                    OutlinedTextField(
                                        value = citySearch,
                                        onValueChange = { citySearch = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(tr("Search for a city", LocalAppLanguage.current)) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Ink,
                                            unfocusedBorderColor = Ink,
                                            cursorColor = Ink
                                        )
                                    )
                                    Text(tr("Top cities", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
                                    val topCities = listOf("New Delhi", "Mumbai", "Bengaluru", "Kolkata", "Hyderabad", "Ahmedabad")
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        topCities.take(3).forEach { item ->
                                            Surface(
                                                modifier = Modifier.weight(1f).clickable { city = item; cityError = false; cityMenu = false },
                                                shape = RoundedCornerShape(12.dp), color = Color.White
                                            ) {
                                                Column(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.LocationCity, contentDescription = null, tint = Ink, modifier = Modifier.size(32.dp))
                                                    Text(item, color = Ink, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
                                                }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                        topCities.drop(3).forEach { item ->
                                            Surface(
                                                modifier = Modifier.weight(1f).clickable { city = item; cityError = false; cityMenu = false },
                                                shape = RoundedCornerShape(12.dp), color = Color.White
                                            ) {
                                                Column(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.LocationCity, contentDescription = null, tint = Ink, modifier = Modifier.size(32.dp))
                                                    Text(item, color = Ink, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
                                                }
                                            }
                                        }
                                    }
                                    Text(if (citySearch.isBlank()) tr("Other cities", LocalAppLanguage.current) else tr("Search Results", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                        items(cities.filter { it.contains(citySearch.trim(), ignoreCase = true) }) { item ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().clickable { city = item; cityError = false; cityMenu = false },
                                                shape = RoundedCornerShape(10.dp), color = Color.White
                                            ) {
                                                Text(item, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (cityError) Text(tr("Please select a city", LocalAppLanguage.current), color = appThemedColor(Color(0xFFE53945)), fontSize = 13.sp)
                    Box {
                        OutlinedButton(
                            onClick = { areaSearch = ""; areaMenu = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (areaError) appThemedColor(Color(0xFFE53945)) else appThemedColor(Color(0xFFB5B5B5))),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Ink)
                            Text(area.ifBlank { "Select an area, street" }, modifier = Modifier.weight(1f).padding(start = 12.dp), color = Ink, fontSize = 14.sp)
                            Text(tr("Select", LocalAppLanguage.current), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        if (areaMenu) {
                            val localityResults = listOf(
                                "Manikonda Marrichettu" to "Marrichettu, Hyderabad, Telangana, India",
                                "Manikeshwari Nagar" to "Amberpet, Hyderabad, Telangana, India",
                                "Manikonda - Puppalaguda" to "Shirdi Sai Nagar, Manikonda, Telangana, India",
                                "Gachibowli" to "Gachibowli, Hyderabad, Telangana, India",
                                "Kondapur" to "Kondapur, Hyderabad, Telangana, India",
                                "Pappulaguda" to "Pappulaguda, Hyderabad, Telangana, India",
                                "Kukatpally" to "Kukatpally, Hyderabad, Telangana, India",
                                "Malkajgiri" to "Malkajgiri, Hyderabad, Telangana, India",
                                "Miyapur" to "Miyapur, Hyderabad, Telangana, India",
                                "Madhapur" to "Madhapur, Hyderabad, Telangana, India",
                                "Begumpet" to "Begumpet, Hyderabad, Telangana, India",
                                "Banjara Hills" to "Banjara Hills, Hyderabad, Telangana, India",
                                "Secunderabad" to "Secunderabad, Telangana, India",
                                "Kakinada" to "Kakinada, Andhra Pradesh, India",
                                "Sarpavaram" to "Sarpavaram, Kakinada, Andhra Pradesh, India",
                                "Pithapuram" to "Pithapuram, Andhra Pradesh, India",
                                "Mandapeta" to "Mandapeta, Andhra Pradesh, India",
                                "Visakhapatnam" to "Visakhapatnam, Andhra Pradesh, India"
                            )
                            ModalBottomSheet(
                                onDismissRequest = { areaMenu = false },
                                containerColor = appThemedColor(Color(0xFFF5F5F5)),
                                contentColor = Ink,
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                dragHandle = {
                                    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                                        Box(Modifier.width(44.dp).height(4.dp).background(appThemedColor(Color(0xFFB5B5B5)), CircleShape))
                                    }
                                }
                            ) {
                                Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 16.dp)) {
                                    Text(tr("Select an area, street", LocalAppLanguage.current), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 10.dp))
                                    OutlinedTextField(
                                        value = areaSearch,
                                        onValueChange = { areaSearch = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(tr("Search for an area, street", LocalAppLanguage.current)) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink) },
                                        trailingIcon = {
                                            if (areaSearch.isNotEmpty()) IconButton(onClick = { areaSearch = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = tr("Clear search", LocalAppLanguage.current), tint = Muted)
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Ink,
                                            unfocusedBorderColor = Ink,
                                            cursorColor = Ink
                                        )
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        items(localityResults.filter { (name, description) ->
                                            areaSearch.isBlank() || name.contains(areaSearch.trim(), true) || description.contains(areaSearch.trim(), true)
                                        }) { (name, description) ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    area = name
                                                    if (city.isBlank()) {
                                                        city = description.split(",").dropLast(2).lastOrNull()?.trim().orEmpty()
                                                    }
                                                    fullAddress = listOf(fullAddress.trim(), description).filter { it.isNotBlank() }.distinct().joinToString(", ")
                                                    areaError = false
                                                    areaMenu = false
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White
                                            ) {
                                                Row(
                                                    Modifier.fillMaxWidth().padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        Modifier.size(54.dp, 48.dp).clip(RoundedCornerShape(14.dp)).background(appThemedColor(Color(0xFFF5F5F5))),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Place, contentDescription = null, tint = appThemedColor(Color(0xFFB5B5B5)), modifier = Modifier.size(26.dp))
                                                    }
                                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        Text(description, color = Muted, fontSize = 14.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (areaError) Text(tr("Please select an area, street", LocalAppLanguage.current), color = appThemedColor(Color(0xFFE53945)), fontSize = 13.sp)
                    OutlinedTextField(
                        value = fullAddress, onValueChange = { fullAddress = it; addressError = false },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text(tr("Enter complete address*", LocalAppLanguage.current)) },
                        isError = addressError, minLines = 2, shape = RoundedCornerShape(10.dp)
                    )
                    if (addressError) Text(tr("Please enter complete address", LocalAppLanguage.current), color = appThemedColor(Color(0xFFE53945)), fontSize = 13.sp)
                    Text(tr("Example: Flat/house no., building, street, landmark", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = mapLink, onValueChange = { mapLink = it },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text(tr("Add Google Maps link (optional)", LocalAppLanguage.current)) },
                        leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = Ink) },
                        singleLine = true, shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Contact Details", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                            forSomeoneElse = false
                            label = "Home"
                            receiverName = profileName.ifBlank { receiverName }
                            receiverPhone = profilePhone.ifBlank { receiverPhone }
                            nameError = false
                            phoneError = false
                        }) {
                            RadioButton(selected = !forSomeoneElse, onClick = {
                                forSomeoneElse = false
                                label = "Home"
                                receiverName = profileName.ifBlank { user?.displayName.orEmpty() }
                                receiverPhone = profilePhone.ifBlank { user?.phoneNumber.orEmpty() }
                                nameError = false
                                phoneError = false
                            }, colors = RadioButtonDefaults.colors(selectedColor = Ink))
                            Text(tr("Myself", LocalAppLanguage.current), color = Ink)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                            forSomeoneElse = true
                            label = "Other"
                            receiverName = ""
                            receiverPhone = ""
                            nameError = false
                            phoneError = false
                        }) {
                            RadioButton(selected = forSomeoneElse, onClick = {
                                forSomeoneElse = true
                                label = "Other"
                                receiverName = ""
                                receiverPhone = ""
                                nameError = false
                                phoneError = false
                            }, colors = RadioButtonDefaults.colors(selectedColor = Ink))
                            Text(tr("Someone else", LocalAppLanguage.current), color = Ink)
                        }
                    }
                    OutlinedTextField(
                        value = receiverName, onValueChange = { receiverName = it; nameError = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text(if (forSomeoneElse) tr("Receiver's name*", LocalAppLanguage.current) else tr("Your name*", LocalAppLanguage.current)) },
                        isError = nameError, singleLine = true, shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (receiverName.isNotEmpty()) IconButton(onClick = { receiverName = ""; nameError = false }) {
                                Icon(Icons.Default.Cancel, contentDescription = "Clear name", tint = Muted)
                            }
                        }
                    )
                    if (nameError) Text(tr("Please enter receiver's name", LocalAppLanguage.current), color = appThemedColor(Color(0xFFE53945)), fontSize = 13.sp)
                    OutlinedTextField(
                        value = receiverPhone, onValueChange = { receiverPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16); phoneError = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text(if (forSomeoneElse) tr("Receiver's phone number*", LocalAppLanguage.current) else tr("Your phone number*", LocalAppLanguage.current)) },
                        isError = phoneError, singleLine = true, shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Text("+91", color = Ink, fontWeight = FontWeight.SemiBold) },
                        trailingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = "Phone Number", tint = Muted) }
                    )
                    if (phoneError) Text(tr("Please enter a valid phone number", LocalAppLanguage.current), color = appThemedColor(Color(0xFFE53945)), fontSize = 13.sp)
                    Text(tr("Save address as", LocalAppLanguage.current), color = Muted, fontSize = 15.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Home", "Work", "Hotel", "Other").forEach { option ->
                            val selected = label.equals(option, ignoreCase = true)
                            Surface(
                                modifier = Modifier.weight(1f).clickable { label = option },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) appThemedColor(Color(0xFFE2E2E2)) else Color.White,
                                border = BorderStroke(1.dp, if (selected) appThemedColor(Color(0xFFB5B5B5)) else appThemedColor(Color(0xFFE2E2E2)))
                            ) {
                                Row(Modifier.padding(vertical = 8.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (option) {
                                            "Home" -> Icons.Default.Home
                                            "Work" -> Icons.Default.Work
                                            "Hotel" -> Icons.Default.Business
                                            else -> Icons.Default.People
                                        },
                                        contentDescription = null, tint = appThemedColor(Color(0xFFB5B5B5)), modifier = Modifier.size(18.dp)
                                    )
                                    Text(option, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(bottom = 76.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = {
                    cityError = city.isBlank()
                    areaError = area.isBlank()
                    addressError = fullAddress.isBlank()
                    nameError = receiverName.isBlank()
                    phoneError = receiverPhone.filter(Char::isDigit).length < 10
                    if (cityError || areaError || addressError || nameError || phoneError) return@Button
                    showReview = true
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(DefaultBtn.height),
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = appThemedColor(DefaultBtn.background),
                    contentColor = appThemedColor(DefaultBtn.content),
                    disabledContainerColor = appThemedColor(DefaultBtn.background),
                    disabledContentColor = appThemedColor(DefaultBtn.content)
                ),
                border = BorderStroke(1.dp, appThemedColor(DefaultBtn.border)),
                shape = RoundedCornerShape(DefaultBtn.cornerRadius),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 10.dp, focusedElevation = 8.dp, hoveredElevation = 10.dp, disabledElevation = 0.dp)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = appThemedColor(DefaultBtn.content), strokeWidth = 2.dp)
                else Text(tr("Next", LocalAppLanguage.current), color = appThemedColor(DefaultBtn.content), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            }
    }
}


@Composable
private fun ReviewAddressPage(
    city: String,
    area: String,
    fullAddress: String,
    receiverName: String,
    receiverPhone: String,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(appThemedColor(Color.White)).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 72.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp).border(1.dp, appThemedColor(Color(0xFFE1E6EF)), CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to address details", tint = Ink)
            }
            Text(tr("Review Address", LocalAppLanguage.current), modifier = Modifier.padding(start = 14.dp), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (initialLatitude != null && initialLongitude != null) {
            GoogleMapPicker(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(300.dp),
                initialLatitude = initialLatitude,
                initialLongitude = initialLongitude,
                onLocationSelected = onLocationSelected
            )
        } else {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .height(300.dp)
                    .background(appThemedColor(Color(0xFFF1F2F4)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(tr("Finding the nearest area on the map…", LocalAppLanguage.current), color = Muted, textAlign = TextAlign.Center)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tr("Delivery Details", LocalAppLanguage.current), modifier = Modifier.weight(1f), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            TextButton(onClick = onEdit) { Text(tr("Edit", LocalAppLanguage.current), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
        Divider(color = appThemedColor(Color(0xFFE6E8EE)))
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Person, contentDescription = tr("Receiver", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(22.dp))
                Text(tr("  Receiver:  $receiverName", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Phone, contentDescription = "Mobile number", tint = Ink, modifier = Modifier.size(22.dp))
                Text(tr("  Mobile:  $receiverPhone", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Home, contentDescription = "Complete address", tint = Ink, modifier = Modifier.size(22.dp))
                Text(tr("  Complete address:  $fullAddress", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
        }
        val hasCompleteAddress = fullAddress.isNotBlank() &&
            area.isNotBlank() && city.isNotBlank() &&
            receiverName.isNotBlank() && receiverPhone.isNotBlank()
        Button(
            onClick = onSave,
            enabled = hasCompleteAddress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                .height(60.dp)
                .shadow(7.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF7FFF5),
                            Color(0xFFE7F6E2),
                            Color(0xFFD9EFD2),
                            Color(0xFFF9FFF7)
                        )
                    )
                )
                .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.95f)), RoundedCornerShape(50)),
            shape = RoundedCornerShape(50),
            border = null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF174B25),
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color(0xFF78927A)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 1.dp,
                disabledElevation = 0.dp
            )
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                tint = if (hasCompleteAddress) Color(0xFF174B25) else Color(0xFF78927A),
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                tr("Save Address", LocalAppLanguage.current),
                color = if (hasCompleteAddress) Color(0xFF174B25) else Color(0xFF78927A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePage(modifier: Modifier, onSignOut: () -> Unit, appLanguage: String, onLanguageChange: (String) -> Unit, initialSection: String = "Profile", onBack: (() -> Unit)? = null) {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    val context = LocalContext.current
    var firstName by remember(uid) { mutableStateOf("") }
    var lastName by remember(uid) { mutableStateOf("") }
    var nickname by remember(uid) { mutableStateOf("") }
    var gender by remember(uid) { mutableStateOf("Prefer not to say") }
    var mobileNumber by remember(uid) { mutableStateOf("") }
    var dateOfBirth by remember(uid) { mutableStateOf("") }
    var showDateOfBirthPicker by remember { mutableStateOf(false) }
    val countryCodes = remember {
        listOf(
            Triple("IN", "🇮🇳", "+91"), Triple("US", "🇺🇸", "+1"), Triple("CA", "🇨🇦", "+1"),
            Triple("GB", "🇬🇧", "+44"), Triple("AU", "🇦🇺", "+61"), Triple("NZ", "🇳🇿", "+64"),
            Triple("AE", "🇦🇪", "+971"), Triple("SA", "🇸🇦", "+966"), Triple("SG", "🇸🇬", "+65"),
            Triple("MY", "🇲🇾", "+60"), Triple("LK", "🇱🇰", "+94"), Triple("NP", "🇳🇵", "+977"),
            Triple("BD", "🇧🇩", "+880"), Triple("PK", "🇵🇰", "+92"), Triple("ZA", "🇿🇦", "+27"),
            Triple("DE", "🇩🇪", "+49"), Triple("FR", "🇫🇷", "+33"), Triple("IT", "🇮🇹", "+39"),
            Triple("ES", "🇪🇸", "+34"), Triple("NL", "🇳🇱", "+31"), Triple("JP", "🇯🇵", "+81"),
            Triple("CN", "🇨🇳", "+86"), Triple("BR", "🇧🇷", "+55"), Triple("MX", "🇲🇽", "+52"),
            Triple("PH", "🇵🇭", "+63"), Triple("ID", "🇮🇩", "+62"), Triple("TH", "🇹🇭", "+66"),
            Triple("NG", "🇳🇬", "+234"), Triple("KE", "🇰🇪", "+254"), Triple("IE", "🇮🇪", "+353")
        )
    }
    var selectedCountry by remember {
        mutableStateOf(countryCodes.firstOrNull { it.first == Locale.getDefault().country.uppercase(Locale.ROOT) } ?: countryCodes.first())
    }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var genderMenuExpanded by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var languageSaving by remember { mutableStateOf(false) }
    var languageMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var profileSection by remember(initialSection) { mutableStateOf(initialSection) }

    DisposableEffect(uid) {
        var registration: ListenerRegistration? = null
        if (uid != null) {
            registration = FirebaseFirestore.getInstance().collection("customers").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        saveMessage = "Couldn't load profile. Check your connection and try again."
                    } else if (snapshot != null) {
                        firstName = snapshot.getString("firstName").orEmpty()
                        lastName = snapshot.getString("lastName").orEmpty()
                        nickname = snapshot.getString("nickname").orEmpty()
                        gender = snapshot.getString("gender")?.takeIf { it.isNotBlank() } ?: "Prefer not to say"
                        dateOfBirth = snapshot.getString("dateOfBirth").orEmpty()
                        val savedPhone = snapshot.getString("mobileNumber").orEmpty()
                        val savedCountryIso = snapshot.getString("countryIso").orEmpty()
                        val matchingCountry = countryCodes.firstOrNull { it.first == savedCountryIso }
                            ?: countryCodes.sortedByDescending { it.third.length }
                                .firstOrNull { savedPhone.startsWith(it.third) }
                        if (matchingCountry != null) {
                            selectedCountry = matchingCountry
                            mobileNumber = if (savedPhone.startsWith(matchingCountry.third)) {
                                savedPhone.removePrefix(matchingCountry.third).filter(Char::isDigit)
                            } else {
                                savedPhone.filter(Char::isDigit)
                            }
                        } else {
                            mobileNumber = savedPhone.filter(Char::isDigit)
                        }
                        isLoading = false
                    } else {
                        isLoading = false
                    }
                }
        } else {
            isLoading = false
        }
        onDispose { registration?.remove() }
    }
    Column(
        modifier.fillMaxSize()
            .background(appThemedColor(Color(0xFFF3F5FA)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (initialSection == "Profile") 0.dp else 16.dp, vertical = if (initialSection == "Profile") 0.dp else 18.dp)
            .padding(bottom = if (initialSection == "Profile") 28.dp else 0.dp)
    ) {
        if (profileSection == "Profile") {
            Column(Modifier.fillMaxWidth().padding(horizontal = if (initialSection == "Profile") 16.dp else 0.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = appThemedColor(Color.White)), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = appThemedColor(Color(0xFFF1F1F1)),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Ink, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { nickname.ifBlank { "SLGAN Customer" } },
                            color = Ink, fontWeight = FontWeight.Bold
                        )
                        Text(user?.email.orEmpty(), color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = firstName, onValueChange = { firstName = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("First name", LocalAppLanguage.current)) }, singleLine = true, leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = Ink) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Last name", LocalAppLanguage.current)) }, singleLine = true, leadingIcon = { Icon(Icons.Default.Badge, null, tint = Ink) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = nickname, onValueChange = { nickname = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Nickname", LocalAppLanguage.current)) }, singleLine = true, leadingIcon = { Icon(Icons.Default.Face, null, tint = Ink) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedTextField(
                        value = "${selectedCountry.second}  ${selectedCountry.third}",
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.width(140.dp).clickable { countryMenuExpanded = true },
                        label = { Text(tr("Country", LocalAppLanguage.current), maxLines = 1, softWrap = false) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = tr("Select country", LocalAppLanguage.current), tint = Ink, modifier = Modifier.size(18.dp)) },
                        singleLine = true
                    )
                    Box(Modifier.matchParentSize().clickable { countryMenuExpanded = true })
                    DropdownMenu(expanded = countryMenuExpanded, onDismissRequest = { countryMenuExpanded = false }, containerColor = appThemedColor(Color.White)) {
                        countryCodes.forEach { country ->
                            DropdownMenuItem(
                                text = { Text("${country.second}  ${country.first}  ${country.third}") },
                                onClick = {
                                    selectedCountry = country
                                    countryMenuExpanded = false
                                    saveMessage = null
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = mobileNumber,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink),
                    onValueChange = { input -> mobileNumber = input.filter(Char::isDigit).take(15); saveMessage = null },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr("Mobile number", LocalAppLanguage.current), maxLines = 1, softWrap = false) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = Ink) }
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateOfBirth.takeIf { it.isNotBlank() }?.let { saved ->
                        runCatching { LocalDate.parse(saved).format(DateTimeFormatter.ofPattern("dd MMM yyyy")) }.getOrDefault(saved)
                    }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink),
                    label = { Text(tr("Date of birth", LocalAppLanguage.current)) },
                    placeholder = { Text(tr("Select date of birth", LocalAppLanguage.current)) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Ink) },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = tr("Choose date", LocalAppLanguage.current), tint = Ink) }
                )
                Box(Modifier.matchParentSize().clickable { showDateOfBirthPicker = true })
            }
            if (showDateOfBirthPicker) {
                val initialDobMillis = dateOfBirth.takeIf { it.isNotBlank() }?.let { saved ->
                    runCatching { LocalDate.parse(saved).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDobMillis)
                DatePickerDialog(
                    onDismissRequest = { showDateOfBirthPicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    dateOfBirth = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                                    saveMessage = null
                                }
                                showDateOfBirthPicker = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Ink)
                        ) { Text(tr("Select", LocalAppLanguage.current)) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDateOfBirthPicker = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Forest)
                        ) { Text(tr("Cancel", LocalAppLanguage.current)) }
                    },
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            containerColor = appThemedColor(Color.White),
                            titleContentColor = Ink,
                            headlineContentColor = Ink,
                            weekdayContentColor = Muted,
                            subheadContentColor = Ink,
                            navigationContentColor = Ink,
                            yearContentColor = Ink,
                            currentYearContentColor = Ink,
                            selectedYearContainerColor = Ink,
                            selectedYearContentColor = Color.White,
                            dayContentColor = Ink,
                            disabledDayContentColor = Muted,
                            selectedDayContainerColor = Ink,
                            selectedDayContentColor = Color.White,
                            todayContentColor = Ink,
                            todayDateBorderColor = Ink
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = gender, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Gender", LocalAppLanguage.current)) }, placeholder = { Text(tr("Select gender", LocalAppLanguage.current)) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Ink) }, leadingIcon = { Icon(Icons.Default.Wc, null, tint = Ink) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, focusedLabelColor = Ink))
                Box(Modifier.matchParentSize().clickable { genderMenuExpanded = true })
                DropdownMenu(expanded = genderMenuExpanded, onDismissRequest = { genderMenuExpanded = false }, containerColor = appThemedColor(Color.White)) {
                    listOf("Female", "Male", "Non-binary", "Prefer not to say").forEach { option ->
                        DropdownMenuItem(text = { Text(tr(option, LocalAppLanguage.current)) }, onClick = { gender = option; genderMenuExpanded = false; saveMessage = null })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val currentUid = uid
                    val parsedDob = dateOfBirth.takeIf { it.isNotBlank() }?.let {
                        runCatching { LocalDate.parse(it) }.getOrNull()
                    }
                    val mobileDigits = mobileNumber.filter(Char::isDigit)
                    val countryCodeValid = selectedCountry.third.matches(Regex("^\\+[1-9]\\d{0,3}$"))
                    val mobileValid = when (selectedCountry.first) {
                        "IN" -> mobileDigits.length == 10 && mobileDigits.firstOrNull() in '6'..'9'
                        "US", "CA" -> mobileDigits.length == 10
                        else -> mobileDigits.length in 7..15
                    }
                    val today = LocalDate.now()
                    when {
                        firstName.isBlank() && mobileNumber.isBlank() ->
                            saveMessage = "First name and mobile number are required."
                        firstName.isBlank() ->
                            saveMessage = "First name is required."
                        mobileNumber.isBlank() ->
                            saveMessage = "Mobile number is required."
                        !countryCodeValid ->
                            saveMessage = "Please select a valid country calling code."
                        !mobileValid ->
                            saveMessage = if (selectedCountry.first == "IN") "Enter a valid 10-digit Indian mobile number starting with 6–9."
                                else if (selectedCountry.first in listOf("US", "CA")) "Enter a valid 10-digit mobile number."
                                else "Enter a valid mobile number containing 7–15 digits."
                        dateOfBirth.isBlank() ->
                            saveMessage = "Date of birth is required."
                        parsedDob == null ->
                            saveMessage = "Please select a valid date of birth."
                        parsedDob.isAfter(today) ->
                            saveMessage = "Date of birth cannot be in the future."
                        parsedDob.isBefore(today.minusYears(120)) ->
                            saveMessage = "Please enter a valid date of birth."
                        currentUid == null ->
                            saveMessage = "Please sign in again to save your profile."
                        else -> {
                            isSaving = true
                            saveMessage = null
                            FirebaseData.saveCustomerProfile(currentUid, mapOf(
                                "email" to (user.email ?: ""), "firstName" to firstName.trim(),
                                "lastName" to lastName.trim(), "nickname" to nickname.trim(),
                                "gender" to gender,
                                "dateOfBirth" to dateOfBirth,
                                "countryIso" to selectedCountry.first,
                                "countryFlag" to selectedCountry.second,
                                "countryDialCode" to selectedCountry.third,
                                "mobileNumber" to (selectedCountry.third + mobileNumber.filter(Char::isDigit)),
                                "updatedAt" to System.currentTimeMillis()
                            )) { error ->
                                isSaving = false
                                if (error == null) {
                                    saveMessage = null
                                    Toast.makeText(context, "Saved successfully.", Toast.LENGTH_SHORT).show()
                                } else {
                                    saveMessage = "Couldn't save profile. Please try again."
                                    Toast.makeText(context, "Couldn't save profile. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                enabled = !isSaving && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, appThemedColor(Color(0xFFB5B5B5))),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appThemedColor(Color(0xFFE2E2E2)),
                    contentColor = Ink,
                    disabledContainerColor = appThemedColor(Color(0xFFE2E2E2)),
                    disabledContentColor = Muted
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 1.dp,
                    disabledElevation = 0.dp
                )
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = appThemedColor(DefaultBtn.content), strokeWidth = 2.dp) else Icon(Icons.Default.Save, null, tint = appThemedColor(DefaultBtn.content))
                Spacer(Modifier.width(8.dp))
                Text(if (isLoading) tr("Loading profile…", LocalAppLanguage.current) else if (isSaving) tr("Saving…", LocalAppLanguage.current) else tr("Save profile", LocalAppLanguage.current), fontWeight = FontWeight.Bold)
            }
            }
        }
    }
}

@Composable
private fun AppLoader(message: String, compact: Boolean = false, messageColor: Color = Ink) {
    val leafMotion = rememberInfiniteTransition(label = "herbalLeafLoader")
    val leafRotation by leafMotion.animateFloat(
        initialValue = -18f,
        targetValue = 342f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "leafRotation"
    )
    val leafScale by leafMotion.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leafScale"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 8.dp else 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 48.dp else 76.dp)
                .background(appThemedColor(Color(0xFFF1F3F1)), CircleShape)
                .border(2.dp, appThemedColor(Color(0xFFB5B5B5)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌿",
                fontSize = if (compact) 25.sp else 39.sp,
                color = appThemedColor(Color(0xFF2E7D32)),
                modifier = Modifier.graphicsLayer {
                    rotationZ = leafRotation
                    scaleX = leafScale
                    scaleY = leafScale
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(message, color = messageColor, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.SemiBold)
        if (!compact) {
            Spacer(Modifier.height(4.dp))
            Text(tr("Please wait a moment", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 52.sp); Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 13.sp, color = Muted)
    }
}
