package com.pyara.slgan

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.Manifest
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private var Forest by mutableStateOf(Color(0xFF762A24))
private var Leaf by mutableStateOf(Color(0xFF8A4B2A))
private var Cream by mutableStateOf(Color(0xFFFFF7E8))
private var PaleGreen by mutableStateOf(Color(0xFFF5E5C8))
private var Ink by mutableStateOf(Color(0xFF38251F))
private var Muted by mutableStateOf(Color(0xFF806D60))
private var Gold by mutableStateOf(Color(0xFFD9A441))

private fun applyAppPalette(themeId: Int) {
    if (themeId == 2) {
        Forest = Color(0xFF214B35); Leaf = Color(0xFF557A45); Cream = Color(0xFFFFF9E9)
        PaleGreen = Color(0xFFE7EBD4); Ink = Color(0xFF20372A); Muted = Color(0xFF687663); Gold = Color(0xFFC9A34A)
    } else {
        Forest = Color(0xFF762A24); Leaf = Color(0xFF8A4B2A); Cream = Color(0xFFFFF7E8)
        PaleGreen = Color(0xFFF5E5C8); Ink = Color(0xFF38251F); Muted = Color(0xFF806D60); Gold = Color(0xFFD9A441)
    }
}

private val LocalAppLanguage = staticCompositionLocalOf { "English" }

private fun tr(text: String, language: String): String {
    if (language == "English") return text
    val te = mapOf(
        "Home" to "హోమ్", "Categories" to "వర్గాలు", "Cart" to "కార్ట్", "Profile" to "ప్రొఫైల్",
        "Your account" to "మీ ఖాతా", "Manage your personal details and delivery addresses." to "మీ వ్యక్తిగత వివరాలు, డెలివరీ చిరునామాలను నిర్వహించండి.",
        "Profile" to "ప్రొఫైల్", "Addresses" to "చిరునామాలు", "First name" to "పేరు", "Last name" to "ఇంటి పేరు",
        "Nickname" to "ముద్దుపేరు", "Mobile number" to "మొబైల్ నంబర్", "Enter mobile number" to "మొబైల్ నంబర్ నమోదు చేయండి",
        "Gender" to "లింగం", "Select gender" to "లింగాన్ని ఎంచుకోండి", "Female" to "స్త్రీ", "Male" to "పురుషుడు",
        "Non-binary" to "నాన్-బైనరీ", "Prefer not to say" to "చెప్పదలచుకోలేదు", "Save profile" to "ప్రొఫైల్ సేవ్ చేయండి",
        "Saved successfully." to "విజయవంతంగా సేవ్ అయింది.", "Saving…" to "సేవ్ అవుతోంది…", "Loading profile…" to "ప్రొఫైల్ లోడ్ అవుతోంది…",
        "Sign out" to "సైన్ అవుట్", "Add address" to "చిరునామా జోడించండి", "Enter delivery details below." to "డెలివరీ వివరాలను క్రింద నమోదు చేయండి.",
        "Choose / check location on Google Maps" to "Google Maps లో లొకేషన్ ఎంచుకోండి / చూడండి",
        "Google Maps opens separately. Please copy the address details into the fields below." to "Google Maps వేరుగా తెరుచుకుంటుంది. చిరునామా వివరాలను క్రింది ఫీల్డ్స్‌లో నమోదు చేయండి.",
        "Address type" to "చిరునామా రకం", "Home" to "ఇల్లు", "Work" to "పని", "Other" to "ఇతర",
        "Recipient name *" to "పొందేవారి పేరు *", "House / flat, building, street *" to "ఇల్లు / ఫ్లాట్, భవనం, వీధి *",
        "Area / village / locality" to "ప్రాంతం / గ్రామం / కాలనీ", "Landmark (optional)" to "గుర్తింపు స్థలం (ఐచ్ఛికం)",
        "City / town *" to "నగరం / పట్టణం *", "State *" to "రాష్ట్రం *", "PIN code *" to "పిన్ కోడ్ *",
        "Save address" to "చిరునామా సేవ్ చేయండి", "Saving address…" to "చిరునామా సేవ్ అవుతోంది…",
        "Saved addresses" to "సేవ్ చేసిన చిరునామాలు", "Keep delivery locations for future orders." to "భవిష్యత్ ఆర్డర్ల కోసం డెలివరీ చిరునామాలను ఉంచండి.",
        "No addresses yet" to "ఇంకా చిరునామాలు లేవు", "Tap Add address to save your first delivery location." to "మొదటి డెలివరీ చిరునామాను సేవ్ చేయడానికి చిరునామా జోడించండి నొక్కండి.",
        "All" to "అన్నీ", "Lehyas" to "లేహ్యాలు", "Ghrutas" to "ఘృతాలు", "Pills" to "మాత్రలు",
        "Tailas" to "తైలాలు", "Churnas" to "చూర్ణాలు", "Rasayanas" to "రసాయనాలు", "Bhasmas" to "భస్మాలు",
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
        "No online payment. Place your order and we will confirm delivery details." to "ఆన్‌లైన్ చెల్లింపు లేదు. ఆర్డర్ చేయండి; డెలివరీ వివరాలను మేము నిర్ధారిస్తాము.",
        "Place order" to "ఆర్డర్ చేయండి",
        "Customer name *" to "కస్టమర్ పేరు *",
        "Email for invoice *" to "ఇన్‌వాయిస్ కోసం ఇమెయిల్ *",
        "Phone number" to "ఫోన్ నంబర్",
        "Full delivery address *" to "పూర్తి డెలివరీ చిరునామా *",
        "Payment: none. This order request does not collect payment." to "చెల్లింపు లేదు. ఈ ఆర్డర్ అభ్యర్థనలో డబ్బు వసూలు చేయబడదు.",
        "Cancel" to "రద్దు చేయండి",
        "My Orders" to "నా ఆర్డర్లు",
        "You have no previous orders yet." to "మీకు ఇంకా పాత ఆర్డర్లు లేవు.",
        "View / Download Invoice" to "ఇన్‌వాయిస్ చూడండి / డౌన్‌లోడ్ చేయండి",
        "Previous orders" to "మునుపటి ఆర్డర్లు",
        "View complete invoice" to "పూర్తి ఇన్‌వాయిస్ చూడండి",
        "Open invoice PDF" to "ఇన్‌వాయిస్ PDF తెరవండి",
        "ORDER INVOICE" to "ఆర్డర్ ఇన్‌వాయిస్",
        "Invoice No: ${'$' }invoiceNo" to "ఇన్‌వాయిస్ నం.: ${'$' }invoiceNo",
        "Order ID: ${'$' }orderId" to "ఆర్డర్ ID: ${'$' }orderId",
        "Order date: ${'$' }created" to "ఆర్డర్ తేదీ: ${'$' }created",
        "BILL TO" to "బిల్ వివరాలు",
        "ITEM DETAILS" to "వస్తువు వివరాలు",
        "No item details were saved with this order." to "ఈ ఆర్డర్‌తో వస్తువుల వివరాలు సేవ్ కాలేదు.",
        "GRAND TOTAL" to "మొత్తం",
        "Payment has not been collected unless the status above says otherwise." to "పై స్థితిలో వేరుగా చూపకపోతే చెల్లింపు స్వీకరించలేదు.",
        "Close" to "మూసివేయండి",
        "Open PDF" to "PDF తెరవండి",
        "Diseases" to "వ్యాధులు",
        "Select a disease to view." to "అందుబాటులో ఉన్న జాబితా నుంచి వ్యాధిని ఎంచుకోండి.",
        "Select disease" to "వ్యాధిని ఎంచుకోండి",
        "Loading diseases from database…" to "డేటాబేస్ నుంచి వ్యాధులు లోడ్ అవుతున్నాయి…",
        "Could not load diseases: ${'$' }error" to "వ్యాధులను లోడ్ చేయలేకపోయాం: ${'$' }error",
        "No matching diseases found." to "సరిపోలే వ్యాధులు కనబడలేదు.",
        "DISEASE OVERVIEW" to "వ్యాధి వివరాలు",
        "What is this disease?" to "ఈ వ్యాధి అంటే ఏమిటి?",
        "Medicines available" to "అందుబాటులో ఉన్న మందులు",
        "Choose a specific disease to see matching medicines." to "సంబంధిత మందులను చూడటానికి ఒక నిర్దిష్ట వ్యాధిని ఎంచుకోండి.",
        "Use medicines only with guidance from a qualified healthcare professional." to "అర్హత కలిగిన వైద్య నిపుణుల సూచనతో మాత్రమే మందులను వాడండి.",
        "Information not available." to "సమాచారం అందుబాటులో లేదు.",
        "Pack size: ${'$' }{product.subtitle}" to "ప్యాక్ పరిమాణం: ${'$' }{product.subtitle}",
        "Category: ${'$' }{product.category}" to "వర్గం: ${'$' }{product.category}",
        "Quantity / stock: ${'$' }{product.quantity}" to "పరిమాణం / నిల్వ: ${'$' }{product.quantity}",
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
        "Search products" to "ఉత్పత్తులను వెతకండి", "Shop by category" to "వర్గాల వారీగా కొనండి", "Could not load products: ${'$' }productsError" to "ఉత్పత్తులను లోడ్ చేయలేకపోయాం: ${'$' }productsError",
        "Made with care for your everyday rituals." to "మీ రోజువారీ అవసరాల కోసం శ్రద్ధగా తయారు చేయబడింది.", "Your cart" to "మీ కార్ట్", "Subtotal" to "మొత్తం", "Checkout" to "చెక్‌అవుట్", "Demo only: checkout and payments are not connected." to "ఇది డెమో మాత్రమే: చెక్‌అవుట్ మరియు చెల్లింపులు ఇంకా కనెక్ట్ కాలేదు.", "Find the collection for your routine." to "మీ అవసరానికి తగిన ఉత్పత్తుల సమాహారాన్ని కనుగొనండి.",
        "Language preference saved." to "భాష ప్రాధాన్యత సేవ్ అయింది.", "Couldn't save language. Please try again." to "భాషను సేవ్ చేయలేకపోయాం. మళ్లీ ప్రయత్నించండి.",
        "English" to "English", "Telugu" to "తెలుగు", "Hindi" to "హిందీ"
    )
    val hi = mapOf(
        "Home" to "होम", "Categories" to "श्रेणियाँ", "Cart" to "कार्ट", "Profile" to "प्रोफ़ाइल",
        "Your account" to "आपका खाता", "Manage your personal details and delivery addresses." to "अपनी व्यक्तिगत जानकारी और डिलीवरी पते प्रबंधित करें।",
        "First name" to "पहला नाम", "Last name" to "उपनाम", "Nickname" to "उपनाम", "Mobile number" to "मोबाइल नंबर",
        "Enter mobile number" to "मोबाइल नंबर दर्ज करें", "Gender" to "लिंग", "Select gender" to "लिंग चुनें",
        "Female" to "महिला", "Male" to "पुरुष", "Non-binary" to "नॉन-बाइनरी", "Prefer not to say" to "बताना नहीं चाहते",
        "Save profile" to "प्रोफ़ाइल सहेजें", "Saved successfully." to "सफलतापूर्वक सहेजा गया।",
        "Saving…" to "सहेज रहे हैं…", "Loading profile…" to "प्रोफ़ाइल लोड हो रही है…", "Sign out" to "साइन आउट",
        "Add address" to "पता जोड़ें", "Enter delivery details below." to "नीचे डिलीवरी विवरण दर्ज करें।",
        "Address type" to "पते का प्रकार", "Home" to "घर", "Work" to "काम", "Other" to "अन्य",
        "Recipient name *" to "प्राप्तकर्ता का नाम *", "House / flat, building, street *" to "मकान / फ्लैट, भवन, सड़क *",
        "Area / village / locality" to "क्षेत्र / गाँव / इलाका", "Landmark (optional)" to "नज़दीकी पहचान (वैकल्पिक)",
        "City / town *" to "शहर / कस्बा *", "State *" to "राज्य *", "PIN code *" to "पिन कोड *",
        "Save address" to "पता सहेजें", "Saved addresses" to "सहेजे गए पते", "Keep delivery locations for future orders." to "भविष्य के ऑर्डर के लिए डिलीवरी पते रखें।",
        "No addresses yet" to "अभी कोई पता नहीं", "All" to "सभी", "Lehyas" to "लेह्य", "Ghrutas" to "घृत",
        "Pills" to "गोलियाँ", "Tailas" to "तेल", "Churnas" to "चूर्ण", "Rasayanas" to "रसायन", "Bhasmas" to "भस्म",
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
        "Major ingredients" to "मुख्य सामग्री",
        "No online payment. Place your order and we will confirm delivery details." to "ऑनलाइन भुगतान उपलब्ध नहीं है। ऑर्डर दें; हम डिलीवरी विवरण की पुष्टि करेंगे।",
        "Place order" to "ऑर्डर करें",
        "Customer name *" to "ग्राहक का नाम *",
        "Email for invoice *" to "इनवॉइस के लिए ईमेल *",
        "Phone number" to "फ़ोन नंबर",
        "Full delivery address *" to "पूरा डिलीवरी पता *",
        "Payment: none. This order request does not collect payment." to "कोई भुगतान नहीं। इस ऑर्डर अनुरोध में पैसे नहीं लिए जाते।",
        "Cancel" to "रद्द करें",
        "My Orders" to "मेरे ऑर्डर",
        "You have no previous orders yet." to "आपके अभी तक कोई पुराने ऑर्डर नहीं हैं।",
        "View / Download Invoice" to "इनवॉइस देखें / डाउनलोड करें",
        "Previous orders" to "पिछले ऑर्डर",
        "View complete invoice" to "पूरा इनवॉइस देखें",
        "Open invoice PDF" to "इनवॉइस PDF खोलें",
        "ORDER INVOICE" to "ऑर्डर इनवॉइस",
        "Invoice No: ${'$' }invoiceNo" to "इनवॉइस नंबर: ${'$' }invoiceNo",
        "Order ID: ${'$' }orderId" to "ऑर्डर ID: ${'$' }orderId",
        "Order date: ${'$' }created" to "ऑर्डर की तारीख: ${'$' }created",
        "BILL TO" to "बिल प्राप्तकर्ता",
        "ITEM DETAILS" to "वस्तु विवरण",
        "No item details were saved with this order." to "इस ऑर्डर के साथ वस्तु विवरण सेव नहीं हुए।",
        "GRAND TOTAL" to "कुल योग",
        "Payment has not been collected unless the status above says otherwise." to "ऊपर अलग स्थिति न हो तो भुगतान प्राप्त नहीं हुआ है।",
        "Close" to "बंद करें",
        "Open PDF" to "PDF खोलें",
        "Diseases" to "रोग",
        "Find a disease from the available list." to "सूची में से रोग खोजें।",
        "Select disease" to "रोग चुनें",
        "Loading diseases from database…" to "डेटाबेस से रोग लोड हो रहे हैं…",
        "Could not load diseases: ${'$' }error" to "रोग लोड नहीं हो सके: ${'$' }error",
        "No matching diseases found." to "कोई मेल खाता रोग नहीं मिला।",
        "DISEASE OVERVIEW" to "रोग का विवरण",
        "What is this disease?" to "यह रोग क्या है?",
        "Medicines available" to "उपलब्ध दवाएँ",
        "Choose a specific disease to see matching medicines." to "संबंधित दवाएँ देखने के लिए कोई विशिष्ट रोग चुनें।",
        "Use medicines only with guidance from a qualified healthcare professional." to "दवाओं का उपयोग केवल योग्य स्वास्थ्य विशेषज्ञ की सलाह से करें।",
        "Information not available." to "जानकारी उपलब्ध नहीं है।",
        "Pack size: ${'$' }{product.subtitle}" to "पैक का आकार: ${'$' }{product.subtitle}",
        "Category: ${'$' }{product.category}" to "श्रेणी: ${'$' }{product.category}",
        "Quantity / stock: ${'$' }{product.quantity}" to "मात्रा / स्टॉक: ${'$' }{product.quantity}",
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
        "Search products" to "उत्पाद खोजें", "Shop by category" to "श्रेणी के अनुसार खरीदें", "Could not load products: ${'$' }productsError" to "उत्पाद लोड नहीं हो सके: ${'$' }productsError",
        "Made with care for your everyday rituals." to "आपकी रोज़मर्रा की ज़रूरतों के लिए सावधानी से बनाया गया।", "Your cart" to "आपकी कार्ट", "Subtotal" to "उप-योग", "Checkout" to "चेकआउट", "Demo only: checkout and payments are not connected." to "यह केवल डेमो है: चेकआउट और भुगतान जुड़े नहीं हैं।", "Find the collection for your routine." to "अपनी दिनचर्या के लिए उत्पाद खोजें।",
        "Language preference saved." to "भाषा प्राथमिकता सहेजी गई।", "Couldn't save language. Please try again." to "भाषा सहेजी नहीं जा सकी। फिर प्रयास करें।",
        "English" to "English", "Telugu" to "तेलुगु", "Hindi" to "हिन्दी"
    )
    return when (language) { "Telugu" -> te[text] ?: text; "Hindi" -> hi[text] ?: text; else -> text }
}

class MainActivity : ComponentActivity() {
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private lateinit var mapLocationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
            pendingGeoCallback = null
            pendingGeoOrigin = null
        }
        FirebaseApp.initializeApp(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) { AyurvedicApp() } }

        // Ask for location access on first launch so map location features can work.
        val fineLocationGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineLocationGranted && !coarseLocationGranted) {
            mapLocationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun requestMapLocationPermission(origin: String, callback: GeolocationPermissions.Callback) {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) callback.invoke(origin, true, false)
        else {
            pendingGeoOrigin = origin
            pendingGeoCallback = callback
            mapLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}


private fun localizedProductName(name: String, language: String): String {
    if (language == "English") return name
    val telugu = mapOf(
        "Mahakanakasindhura Rasam" to "మహాకనకసింధూర రసం",
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
        "Siddha Makaradhwajam" to "సిద్ధ మకరధ్వజం"
    )
    val hindi = mapOf(
        "Mahakanakasindhura Rasam" to "महाकनकसिंधूर रस",
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
        "Siddha Makaradhwajam" to "सिद्ध मकरध्वज"
    )
    return when (language) { "Telugu" -> telugu[name] ?: name; "Hindi" -> hindi[name] ?: name; else -> name }
}

private data class Product(val id: String, val name: String, val subtitle: String, val price: Double, val category: String, val emoji: String, val description: String, val imageUrl: String, val quantity: String, val usage: String, val cures: String, val majorIngredients: String, val signature: Boolean = false, val productStock: Int = 1)
private data class ProductRatingSummary(val average: Double = 0.0, val count: Int = 0, val myRating: Int = 0)
private data class ProductLikeSummary(val count: Int = 0, val likedByMe: Boolean = false)
private val categories = listOf("All", "Lehyas", "Ghrutas", "Pills", "Tailas", "Churnas", "Rasayanas", "Bhasmas")

// Offline fallback keeps the disease selector usable until Firestore is populated.
private val fallbackDiseases = listOf("All Diseases", "Cough", "Fever", "Obesity", "Tuberculosis (TB)")

private fun productFromFirestore(data: Map<String, Any?>): Product? {
    val name = data["product_name"]?.toString()?.trim().orEmpty()
    if (name.isBlank()) return null
    // Show only the pack weight beneath the product name, e.g. "5g".
    // Quantity is intentionally hidden; avoid separators such as "1 · 5 · g".
    val weight = data["product_weight"]?.toString()?.trim().orEmpty()
    val units = data["product_units"]?.toString()?.trim().orEmpty()
    val cleanWeight = weight.removeSuffix(units).trim().removeSuffix(".").trim()
    val details = if (cleanWeight.isNotBlank() && units.isNotBlank()) "$cleanWeight$units" else cleanWeight.ifBlank { units }
    val price = data["product_price"]?.toString()?.replace(",", "")?.toDoubleOrNull() ?: 0.0
    val rawCategory = data["product_category"]?.toString()?.trim().orEmpty()
    val category = when (rawCategory.lowercase()) {
        "rasayoga", "rasayogas" -> "Rasayogas"
        "rasayanas", "rasayana" -> "Rasayanas"
        "lehya", "lehyam", "lehyas" -> "Lehyas"
        "grutha", "ghrita", "grutas", "gruthas" -> "Ghrutas"
        "pill", "pills", "vati" -> "Pills"
        "taila", "tailam", "tailas" -> "Tailas"
        "churna", "churnas" -> "Churnas"
        "bhasma", "bhasmas" -> "Bhasmas"
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
    val imageUrl = listOf("product_image", "image_url", "imageUrl", "image", "product_image_url").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") } }.orEmpty()
    val quantity = listOf("product_quantity", "quantity", "stock").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val usage = listOf("product_usage", "usage", "how_to_use", "directions").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val cures = listOf("product_cures", "cures", "benefits", "indications").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val majorIngredients = listOf("product_major_ingredients", "major_ingredients", "ingredients", "product_ingredients").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    return Product(data["id"]?.toString() ?: name.hashCode().toString(), name, details.ifBlank { "Traditional Ayurvedic product" }, price, category, emoji, description, imageUrl, quantity, usage, cures, majorIngredients, signature = data["signature"]?.toString()?.toIntOrNull() == 1 || data["signature"]?.toString()?.equals("true", ignoreCase = true) == true, productStock = data["product_stock"]?.toString()?.toIntOrNull()?.coerceIn(0, 1) ?: 1)
}
@Composable
private fun AyurvedicApp() {
    val appContext = LocalContext.current
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
        delay(10000)
        showSplash = false
    }
    DisposableEffect(Unit) {
        val themeRegistration = FirebaseFirestore.getInstance().collection("appSettings").document("appTheme")
            .addSnapshotListener { snapshot, error -> if (error == null) applyAppPalette((snapshot?.getLong("activeTheme") ?: 1L).toInt().coerceIn(1, 2)) }
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
            authError = signInFailure ?: "Google sign-in was cancelled (result code ${result.resultCode})."
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
    var latestOrderId by remember { mutableStateOf<String?>(null) }
    var latestOrderSuccess by remember { mutableStateOf<Boolean?>(null) }
    var latestOrderInvoiceUrl by remember { mutableStateOf<String?>(null) }
    var latestOrderMessage by remember { mutableStateOf<String?>(null) }
    var appLanguage by remember { mutableStateOf("English") }
    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var liveProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
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
                            productFromFirestore(record)
                        } catch (error: Exception) {
                            android.util.Log.w("SLGAN_PRODUCTS", "Skipping an invalid product record", error)
                            null
                        }
                    }
                    liveProducts = parsedProducts
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
            signedIn && selectedTab != "Home" -> selectedTab = "Home"
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
        Surface(Modifier.fillMaxSize(), color = Cream) {
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
                onContinue = {
                    val email = userInput.trim()
                    if (email.isBlank() || password.length < 6) {
                        authError = "Enter an email and a password with at least 6 characters."
                    } else if (isCreatingAccount && password != confirmPassword) {
                        authError = "Password and confirm password do not match."
                    } else {
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
                                authError = result.exception?.localizedMessage ?: "Sign-in failed. Please try again."
                            }
                        }
                    }
                }
            )
            else if (!adminRoleResolved) AppLoader("Checking account permissions…")
            else if (isAdmin && adminCustomersOpen) AdminCustomersScreen(onBack = { adminCustomersOpen = false })
            else if (isAdmin && adminDiseasesOpen) AdminDiseasesScreen(onBack = { adminDiseasesOpen = false })
            else if (isAdmin && adminBannersOpen) BannerManagementScreen(onBack = { adminBannersOpen = false })
            else if (isAdmin && adminThemesOpen) ThemeManagementScreen(onBack = { adminThemesOpen = false })
            else if (isAdmin && adminOrdersOpen) AdminOrdersScreen(onBack = { adminOrdersOpen = false })
            else if (isAdmin && adminProductsOpen) AdminProductsScreen(
                products = liveProducts,
                loading = productsLoading,
                loadError = productsError,
                onBack = { adminProductsOpen = false }
            )
            else if (isAdmin) AdminDashboardScreen(
                email = auth.currentUser?.email.orEmpty(),
                productCount = liveProducts.size,
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
                onAdd = { id -> cart[id] = (cart[id] ?: 0) + 1 },
                onRemove = { id -> val n = cart[id] ?: 0; if (n <= 1) cart.remove(id) else cart[id] = n - 1 }
            )
            else ShopScreen(selectedTab, { selectedTab = it }, appLanguage, { appLanguage = it }, selectedCategory, { selectedCategory = it }, search, { search = it }, cart, liveProducts, productRatings, productLikes, toggleProductLike, productsLoading, productsError, liveDiseases.ifEmpty { fallbackDiseases }, diseasesLoading, if (liveDiseases.isEmpty()) null else diseasesError, latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage,
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
                { product -> selectedProduct = product })
        }
    }
    }
}
@Composable
private fun SplashScreen() {
    // Keep splash artwork in sync with the currently selected app palette.
    val botanicalTheme = Forest == Color(0xFF214B35)
    val splashBackground = if (botanicalTheme) Color(0xFFFFF9E9) else Color(0xFFFFF7E8)
    val splashTint = if (botanicalTheme) Color(0xFF557A45) else Color(0xFF8A4B2A)
    val splashHalo = if (botanicalTheme) Color(0xFFB9D5A6) else Color(0xFFE8C7A0)
    val splashRing = if (botanicalTheme) Color(0xFF9EBB86) else Color(0xFFD9A441)
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
        modifier = Modifier.fillMaxSize().background(splashBackground),
        contentAlignment = Alignment.TopStart
    ) {
        // Soft botanical background accents and gently falling leaves.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    if (botanicalTheme) listOf(splashBackground, Color(0xFFE7EBD4), splashBackground) else listOf(splashBackground, Color(0xFFFFEBD0), splashBackground)
                )
            )
        )
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
                color = Forest,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                tr("AYURVEDA NILAYAM", LocalAppLanguage.current),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Leaf,
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
    onContinue: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Traditional maroon-and-gold header band
        Box(Modifier.fillMaxWidth().height(18.dp).background(Color(0xFF6B1825))) {
            Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter).background(Gold))
        }
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
            tr("Sri Lakshmi Ganapathi", LocalAppLanguage.current),
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B1825),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            tr("Ayurveda Nilayam", LocalAppLanguage.current),
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6B1825),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.width(54.dp).height(1.dp).background(Gold))
            Text(tr("  ✦  🌿  ✦  ", LocalAppLanguage.current), color = Gold, fontSize = 13.sp)
            Box(Modifier.width(54.dp).height(1.dp).background(Gold))
        }
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
            color = Color(0xFF2E7D32),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            label = { Text(tr("Email address", LocalAppLanguage.current)) },
            placeholder = { Text(tr("you@example.com", LocalAppLanguage.current)) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Forest) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8A5360),
                unfocusedBorderColor = Gold.copy(alpha = 0.55f),
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                focusedLabelColor = Forest,
                cursorColor = Forest
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            label = { Text(tr("Password", LocalAppLanguage.current)) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Forest) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = Forest
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold.copy(alpha = 0.65f),
                unfocusedBorderColor = Gold.copy(alpha = 0.65f),
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                focusedLabelColor = Forest,
                cursorColor = Forest
            )
        )
        if (isCreatingAccount) {
            Spacer(Modifier.height(12.dp))
            var showConfirmPassword by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                label = { Text("Confirm password") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Forest) },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide confirm password" else "Show confirm password",
                            tint = Forest
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showConfirmPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold.copy(alpha = 0.65f),
                    unfocusedBorderColor = Gold.copy(alpha = 0.65f),
                    focusedContainerColor = Cream,
                    unfocusedContainerColor = Cream,
                    focusedLabelColor = Forest,
                    cursorColor = Forest
                )
            )
        }
        if (errorMessage != null) Text(errorMessage, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFFB3261E), fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onContinue,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B1825))
        ) {
            Text(if (loading) "Please wait…" else if (isCreatingAccount) "Create account" else "Sign in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.ArrowForward, null)
        }
        TextButton(onClick = onToggleMode, modifier = Modifier.padding(top = 2.dp), enabled = !loading) {
            Text(if (isCreatingAccount) "Already have an account? Sign in" else "New customer? Create account", color = Color(0xFF6B1825))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(Gold.copy(alpha = 0.55f)))
            Text("  OR  ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
            Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(12.dp))
            Text(if (loading) "Please wait…" else "Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("Sign in with your registered email and password.", LocalAppLanguage.current), fontSize = 13.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(tr("Mobile-number login is not available yet.", LocalAppLanguage.current), fontSize = 12.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.fillMaxWidth().height(18.dp).background(Color(0xFF6B1825))) {
            Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopCenter).background(Gold))
        }
    }
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
        listOf(
            HomeBannerSlide(
                productName = "Sri Lakshmi Ganapathi Ayurveda Nilayam",
                tagline = "Estd. 1900 by Sri Vidyaraju Allamraju Perraju Sharma",
                fallbackImageUrl = "",
                isCompanySlide = true
            )
        ) + signatureSlides
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
                .border(1.dp, Gold.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                .background(if (selectedDesign == 4) Forest else when (selectedDesign) { 2 -> Color(0xFF173D2B); 3 -> Color(0xFFF8F0DC); else -> Color(0xFFF5F0E6) })
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
                    (fadeIn(animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing))
                },
                label = "homeBannerSlide"
            ) { index ->
                val slide = slides[index]
                val productImage = products.firstOrNull { it.name.trim().equals(slide.productName, ignoreCase = true) }?.imageUrl.orEmpty()
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = if (selectedDesign == 4) {
                            when (index % 5) {
                                0 -> "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=1400&q=85"
                                1 -> "https://images.unsplash.com/photo-1515586000433-45406d8e6662?auto=format&fit=crop&w=1400&q=85"
                                2 -> "https://images.unsplash.com/photo-1501004318641-b39e6451bec6?auto=format&fit=crop&w=1400&q=85"
                                3 -> "https://images.unsplash.com/photo-1470058869958-2a77ade41c02?auto=format&fit=crop&w=1400&q=85"
                                else -> "https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=1400&q=85"
                            }
                        } else slide.drawableResId ?: productImage.ifBlank { slide.fallbackImageUrl },
                        contentDescription = if (selectedDesign == 4) "Botanical herbal leaves background" else slide.productName,
                        contentScale = if (selectedDesign == 4) ContentScale.Crop else if (slide.isCompanySlide) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!slide.isCompanySlide) {
                        // Consistent dark botanical scrim keeps every signature-product slide readable,
                        // even when the product image has a bright or busy background.
                        Box(Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xF0183528),
                                    Color(0xD91D402E),
                                    Color(0x80244835),
                                    Color.Transparent
                                )
                            )
                        ))
                    } else if (selectedDesign == 3) {
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFF8F0DC).copy(alpha = 0.97f), Color(0xFFE7EBCB).copy(alpha = 0.86f), Color(0xFFC6D6A3).copy(alpha = 0.42f), Color.Transparent))))
                    } else if (selectedDesign == 2) {
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF102D20).copy(alpha = 0.98f), Color(0xFF173D2B).copy(alpha = 0.88f), Color(0xFF24523A).copy(alpha = 0.55f), Color.Transparent))))
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Forest.copy(alpha = 0.24f)))))
                    } else if (selectedDesign != 4 && !slide.isCompanySlide) {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF183D2D).copy(alpha = 0.96f),
                                        Color(0xFF24523A).copy(alpha = 0.82f),
                                        Color(0xFF3F6A4A).copy(alpha = 0.36f),
                                        Color.Transparent
                                    )
                                )
                            )
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Forest.copy(alpha = 0.30f))
                                )
                            )
                        )
                    }
                    // Slider 4's decorative frame appears on every slide while design 4 is active.
                    if (selectedDesign == 4) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(7.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF762A24), Gold, Color(0xFF762A24)))))
                            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(7.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF762A24), Gold, Color(0xFF762A24)))))
                            Row(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("🌿", fontSize = 20.sp); Text("🍃", fontSize = 17.sp)
                            }
                            Row(Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("🌱", fontSize = 18.sp); Text("🌿", fontSize = 20.sp)
                            }
                            Row(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("🪴", fontSize = 18.sp); Text("🌿", fontSize = 20.sp)
                            }
                            Row(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("🍃", fontSize = 18.sp); Text("🌼", fontSize = 19.sp)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (slide.isCompanySlide) {
                            Text(
                                tr("TRUSTED SINCE 1990", LocalAppLanguage.current),
                                color = Gold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            modifier = Modifier.animateEnterExit(
                                enter = fadeIn(animationSpec = tween(950, delayMillis = 220, easing = FastOutSlowInEasing)) +
                                    slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(950, delayMillis = 220, easing = FastOutSlowInEasing))
                            ),
                            text = if (slide.isCompanySlide) tr("Authentic Ayurvedic Formulations", LocalAppLanguage.current)
                            else localizedProductName(slide.productName, LocalAppLanguage.current),
                            color = if (selectedDesign == 3) Color(0xFF23452C) else if (selectedDesign == 2) Color(0xFFFFE8AE) else if (selectedDesign == 4) Color(0xFFFFF8E8) else Color.White,
                            fontSize = if (slide.isCompanySlide) 22.sp else 21.sp,
                            lineHeight = if (slide.isCompanySlide) 26.sp else 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = if (slide.isCompanySlide) 3 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = tr(slide.tagline, LocalAppLanguage.current),
                            modifier = Modifier.animateEnterExit(
                                enter = fadeIn(animationSpec = tween(1000, delayMillis = 480, easing = FastOutSlowInEasing)) +
                                    slideInVertically(initialOffsetY = { it / 7 }, animationSpec = tween(1000, delayMillis = 480, easing = FastOutSlowInEasing))
                            ),
                            color = if (selectedDesign == 3) Color(0xFF5B3428) else if (selectedDesign == 2) Color(0xFFE8F0D8) else if (selectedDesign == 4) Color(0xFFFFE8C2) else Color(0xFFFFE9B2),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = if (slide.isCompanySlide) 3 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!slide.isCompanySlide) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFFFF7E8), Color(0xFFFFE6A3))
                                        )
                                    )
                                    .border(1.dp, Gold, RoundedCornerShape(12.dp))
                                    .clickable { openProduct(slide.productName) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    "View product",
                                    color = Forest,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Forest,
                                    modifier = Modifier.size(13.dp)
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
                        .background(if (selected) Forest else Gold.copy(alpha = 0.65f))
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
        listOf("All") + products.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
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
        products.filter { product ->
            (selectedAdminCategory == "All" || product.category.equals(selectedAdminCategory, ignoreCase = true)) &&
            (searchText.isBlank() ||
                product.name.contains(searchText, ignoreCase = true) ||
                product.category.contains(searchText, ignoreCase = true) ||
                product.id.contains(searchText, ignoreCase = true))
        }.sortedBy { it.name.lowercase() }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(bottom = 14.dp)
        ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to dashboard", tint = Forest) }
            Column(Modifier.weight(1f)) {
                Text("Products", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("${products.size} catalogue items", color = Muted, fontSize = 12.sp)
            }
        }
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Forest)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(Cream, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = Forest, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Product library", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Manage your Ayurvedic catalogue", color = Cream.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { addingProduct = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Forest, disabledContainerColor = Cream.copy(alpha = 0.7f), disabledContentColor = Forest)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add product", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("${products.size} TOTAL", color = Cream.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(72.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 18.sp),
            label = { Text("Search product", modifier = Modifier.background(Color.Transparent), color = Forest, fontSize = 12.sp, maxLines = 1) },
            placeholder = { Text("Name, category or product ID", fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search products", tint = Forest) },
            trailingIcon = {
                if (searchText.isNotEmpty()) IconButton(onClick = { searchText = "" }) {
                    Icon(Icons.Default.Close, "Clear search")
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Forest,
                unfocusedBorderColor = Gold,
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                cursorColor = Gold
            )
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            adminCategories.forEach { item ->
                FilterChip(
                    selected = selectedAdminCategory == item,
                    onClick = { selectedAdminCategory = item },
                    label = { Text("${categoryEmoji(item)} $item", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Forest,
                        selectedLabelColor = Color.White,
                        containerColor = Cream
                    )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (selectedAdminCategory == "All") "PRODUCT CATALOGUE" else selectedAdminCategory.uppercase(),
                    color = Leaf, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                Text("${filtered.size} products", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (searchText.isNotBlank() || selectedAdminCategory != "All") {
                TextButton(onClick = { searchText = ""; selectedAdminCategory = "All" }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
        if (loading) {
            AppLoader("Loading products…", compact = true)
        } else if (loadError != null && products.isEmpty()) {
            Text("Could not load products: $loadError", Modifier.padding(18.dp), color = Color(0xFFB3261E))
        } else if (filtered.isEmpty()) {
            EmptyState("📦", "No products found", if (searchText.isBlank()) "Tap Add product to create your first product." else "Try another product name or category.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filtered, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.48f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            Box(
                                Modifier.fillMaxWidth().height(5.dp)
                                    .background(Brush.horizontalGradient(listOf(Forest, Gold, Forest)))
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(104.dp, 112.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(PaleGreen),
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
                                        Text(product.emoji, fontSize = 42.sp)
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        product.category.uppercase(),
                                        color = Leaf,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        product.name,
                                        color = Ink,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (product.subtitle.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFFEBC2),
                                                contentColor = Ink
                                            ) {
                                                Text(
                                                    "Pack size: " + product.subtitle,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (product.productStock == 1) Color(0xFFE1F3E4) else Color(0xFFFFE3DF)
                                        ) {
                                            Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(6.dp).clip(CircleShape).background(if (product.productStock == 1) Color(0xFF258348) else Color(0xFFC5392F)))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    if (product.productStock == 1) "In stock" else "Out of stock",
                                                    color = if (product.productStock == 1) Color(0xFF176B37) else Color(0xFFAE2924),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        if (product.price > 0) "₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price) else "Price not set",
                                        color = Forest,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            HorizontalDivider(color = Gold.copy(alpha = 0.28f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PRODUCT DETAILS", Modifier.weight(1f), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                TextButton(
                                    onClick = { viewingProduct = product },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View ${product.name}", modifier = Modifier.size(16.dp), tint = Forest)
                                    Spacer(Modifier.width(4.dp))
                                    Text("View", color = Forest, fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = { editingProduct = product },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit ${product.name}", modifier = Modifier.size(16.dp), tint = Forest)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit", color = Forest, fontSize = 11.sp)
                                }
                                IconButton(onClick = { deletingProduct = product }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${product.name}", tint = Color(0xFFB3261E), modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            containerColor = Cream,
            title = { Text("Delete product?", color = Forest, fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${product.name}\" from the catalogue? This cannot be undone.", color = Ink) },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseFirestore.getInstance().collection("products").document(product.id).delete()
                        .addOnSuccessListener { Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show() }
                    deletingProduct = null
                }) { Text("Delete", color = Color(0xFFB3261E)) }
            },
            dismissButton = { TextButton(onClick = { deletingProduct = null }) { Text("Cancel", color = Muted) } }
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

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to products", tint = Forest) }
            Column(Modifier.weight(1f)) {
                Text("View Product", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Complete catalogue information", color = Muted, fontSize = 12.sp)
            }
            TextButton(onClick = { onEdit(product) }) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Forest, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Edit", color = Forest)
            }
        }
        if (loadingDetails) {
            AppLoader("Loading product details…", compact = true)
        } else if (detailsError != null && data.isEmpty()) {
            Text(detailsError.orEmpty(), Modifier.padding(20.dp), color = Color(0xFFB3261E))
            TextButton(onClick = onBack) { Text("Back to products", color = Forest) }
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.45f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val image = listOf("product_image_url", "image_url", "imageUrl", "image", "productImage").firstNotNullOfOrNull { key -> data[key]?.toString()?.takeIf { it.startsWith("http") } }.orEmpty()
                        if (image.isNotBlank()) {
                            AsyncImage(model = image, contentDescription = product.name, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                                Text(product.emoji, fontSize = 48.sp)
                            }
                        }
                        Text(data["product_name"]?.toString() ?: data["name"]?.toString() ?: product.name, color = Forest, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (product.price > 0) "₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price) else "Price not set",
                            color = Forest, fontSize = 21.sp, fontWeight = FontWeight.Bold
                        )
                        Text(if (product.productStock == 1) "IN STOCK" else "OUT OF STOCK", color = if (product.productStock == 1) Color(0xFF258348) else Color(0xFFC5392F), fontWeight = FontWeight.Bold)
                    }
                }
                Text("PRODUCT INFORMATION", color = Leaf, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                if (data.isEmpty()) {
                    Text("No additional fields are saved for this product.", color = Muted)
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
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.22f))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(displayLabel(key), color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(valueText.ifBlank { "—" }, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
                if (detailsError != null) Text("Latest details could not be refreshed: ${detailsError}", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

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
    var categoryOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var categorySeedStarted by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val registration = db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
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
                    categoryOptions = snapshot.documents.mapNotNull { doc ->
                        val label = (doc.getString("category_name") ?: doc.getString("name") ?: doc.id).trim()
                        if (label.isBlank()) null else label to doc.getString("emoji").orEmpty()
                    }.distinctBy { it.first.lowercase() }.sortedBy { it.first.lowercase() }
                    if (category.isNotBlank() && categoryOptions.none { it.first.equals(category, ignoreCase = true) }) {
                        categoryOptions = (categoryOptions + (category.trim() to "")).distinctBy { it.first.lowercase() }.sortedBy { it.first.lowercase() }
                    }
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
    var quantity by remember(product?.id) { mutableStateOf(product?.quantity.orEmpty()) }
    var imageUrl by remember(product?.id) { mutableStateOf(product?.imageUrl.orEmpty()) }
    var description by remember(product?.id) { mutableStateOf(product?.description.orEmpty()) }
    var usage by remember(product?.id) { mutableStateOf(product?.usage.orEmpty()) }
    var cures by remember(product?.id) { mutableStateOf(product?.cures.orEmpty()) }
    var ingredients by remember(product?.id) { mutableStateOf(product?.majorIngredients.orEmpty()) }
    var signature by remember(product?.id) { mutableStateOf(product?.signature ?: false) }
    var productStock by remember(product?.id) { mutableStateOf(product?.productStock ?: 1) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !saving) { Icon(Icons.Default.ArrowBack, "Back", tint = Forest) }
            Text(if (isNew) "Add product" else "Edit product", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Product information", color = Forest, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(name, { name = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Product name *") }, singleLine = true)
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    enabled = true,
                    label = { Text("Category *") },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { categoryMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select category")
                        }
                    }
                )
                Box(
                    modifier = Modifier.matchParentSize().clickable { categoryMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 320.dp).background(Cream)
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(listOf(option.second, option.first).filter { it.isNotBlank() }.joinToString(" "), color = Ink) },
                            onClick = { category = option.first; message = null; categoryMenuExpanded = false }
                        )
                    }
                }
            }
            OutlinedTextField(price, { price = it.filter { ch -> ch.isDigit() || ch == '.' }; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Price (₹) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' }; message = null }, modifier = Modifier.weight(1f), label = { Text("Weight *") }, singleLine = true)
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { unitMenuExpanded = true },
                        label = { Text("Units *") },
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
                        modifier = Modifier.heightIn(max = 280.dp).background(Cream)
                    ) {
                        if (unitOptions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No units found in product database", color = Muted) },
                                onClick = { unitMenuExpanded = false },
                                enabled = false
                            )
                        } else {
                            unitOptions.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, color = Ink) },
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
                    label = { Text("Qty *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Product availability", color = Forest, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (productStock == 1) "IN" else "OUT", color = if (productStock == 1) Forest else Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                    Switch(
                        checked = productStock == 1,
                        onCheckedChange = { productStock = if (it) 1 else 0 },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF258348),
                            checkedBorderColor = Color(0xFF258348),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFB3261E)
                        )
                    )
                }
            }
            OutlinedTextField(imageUrl, { imageUrl = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Product image URL") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), minLines = 1)
            OutlinedTextField(description, { description = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 3)
            OutlinedTextField(usage, { usage = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Usage / directions") }, minLines = 2)
            OutlinedTextField(cures, { cures = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Cures / indications") }, minLines = 2)
            OutlinedTextField(ingredients, { ingredients = it; message = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Major ingredients") }, minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = signature, onCheckedChange = { signature = it }, colors = CheckboxDefaults.colors(checkedColor = Forest))
                Text("Show as signature product", color = Ink)
            }
            if (message != null) Text(message!!, color = if (message!!.startsWith("Saved")) Forest else Color(0xFFB3261E), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = {
                if (name.isBlank() || category.isBlank() || price.toDoubleOrNull() == null || weight.toDoubleOrNull() == null || weight.toDoubleOrNull()!! <= 0 || units.isBlank() || quantity.toIntOrNull() == null || quantity.toIntOrNull()!! <= 0) {
                    message = "Enter product name, category, valid price, weight, weight unit and quantity."
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp)); Text(if (saving) "Saving…" else "Save product", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminDashboardScreen(
    email: String,
    productCount: Int,
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
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val ordersListener = db.collection("orders").addSnapshotListener { snapshot, error ->
            if (error == null) orderCount = snapshot?.size() ?: 0
        }
        val diseasesListener = db.collection("diseases").addSnapshotListener { snapshot, error ->
            if (error == null) diseaseCount = snapshot?.size() ?: 0
        }
        val customersListener = db.collection("customers").addSnapshotListener { snapshot, error ->
            if (error == null) customerCount = snapshot?.documents?.count { it.getBoolean("is_admin") != true } ?: 0
        }
        onDispose {
            ordersListener.remove()
            diseasesListener.remove()
            customersListener.remove()
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("SLGAN ADMIN", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("Dashboard", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = onSignOut) {
                Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = Forest)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Welcome back", color = Muted, fontSize = 15.sp)
        Text(email.ifBlank { "Administrator" }, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(22.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Forest)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Sri Lakshmi Ganapathi", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
                Text("Ayurvedic Nilayam", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                Text("Manage your store from one place.", color = Cream, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("OVERVIEW", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminSummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Inventory2,
                title = "Products",
                value = productCount.toString(),
                subtitle = "Catalogue items",
                onClick = onProductsClick
            )
            AdminSummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ReceiptLong,
                title = "Orders",
                value = orderCount.toString(),
                subtitle = "Customer orders",
                onClick = onOrdersClick
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminSummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MedicalServices,
                title = "Diseases",
                value = diseaseCount.toString(),
                subtitle = "Disease records",
                onClick = onDiseasesClick
            )
            AdminSummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PeopleAlt,
                title = "Customers",
                value = customerCount.toString(),
                subtitle = "Registered customers",
                onClick = onCustomersClick
            )
        }
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth().clickable(onClick = onBannersClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Cream), border = BorderStroke(1.dp, Gold.copy(alpha = 0.65f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PaleGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.ViewCarousel, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("Home banner sliders", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Choose Slider 1, 2 or 3 for the home page", color = Muted, fontSize = 12.sp) }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Forest)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth().clickable(onClick = onThemesClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Cream), border = BorderStroke(1.dp, Gold.copy(alpha = 0.65f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PaleGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("App themes", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Apply Default or Theme 1 across the app", color = Muted, fontSize = 12.sp) }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Forest)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }
    }
}



@Composable
private fun ThemeManagementScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var selectedTheme by remember { mutableIntStateOf(1) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val listener = db.collection("appSettings").document("appTheme").addSnapshotListener { snap, error ->
            if (error == null) selectedTheme = (snap?.getLong("activeTheme") ?: 1L).toInt().coerceIn(1, 2)
        }
        onDispose { listener.remove() }
    }
    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Forest) }
            Column { Text("APP APPEARANCE", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold); Text("Choose app theme", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Choose a theme and tap Apply. Colors across the app update together, including buttons, cards, backgrounds, icons, emojis accents and text.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        (1..2).forEach { theme ->
            val botanical = theme == 2
            val primary = if (botanical) Color(0xFF214B35) else Color(0xFF762A24)
            val accent = if (botanical) Color(0xFFC9A34A) else Color(0xFFD9A441)
            val background = if (botanical) Color(0xFFFFF9E9) else Color(0xFFFFF7E8)
            val textColor = if (botanical) Color(0xFF20372A) else Color(0xFF38251F)
            Card(Modifier.fillMaxWidth().padding(bottom = 14.dp).clickable { selectedTheme = theme; message = null },
                shape = RoundedCornerShape(22.dp), border = BorderStroke(if (selectedTheme == theme) 2.dp else 1.dp, if (selectedTheme == theme) accent else Color(0xFFE8DCC7)),
                colors = CardDefaults.cardColors(containerColor = Cream)) {
                Column(Modifier.padding(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(if (botanical) listOf(Color(0xFF173D2B), Color(0xFF557A45), Color(0xFFC9A34A)) else listOf(Color(0xFF762A24), Color(0xFF8A4B2A), Color(0xFFD9A441)))).padding(16.dp)) {
                        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.78f)) {
                            Text(if (botanical) "🌿  BOTANICAL HERITAGE" else "🌺  SLGAN CLASSIC", color = Color(0xFFFFE8AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(if (botanical) "Nature’s Wisdom" else "Tradition & Trust", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text(if (botanical) "Forest green · antique gold · ivory" else "Maroon · gold · warm cream", color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp)
                        }
                        if (selectedTheme == theme) Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.align(Alignment.TopEnd).size(26.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(if (botanical) "Theme 1 · Botanical Heritage" else "Default · SLGAN Classic", color = textColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (botanical) "Logo-inspired forest green and antique gold with ivory surfaces and botanical accents." else "The app’s current maroon, terracotta, gold and cream appearance. No visual change.", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(primary, accent, background, textColor).forEach { swatch -> Box(Modifier.size(24.dp).clip(CircleShape).background(swatch).border(1.dp, Color.LightGray, CircleShape)) }
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = { selectedTheme = theme; saving = true; message = null
                            db.collection("appSettings").document("appTheme").set(mapOf("activeTheme" to theme, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                .addOnSuccessListener {
                                    applyAppPalette(theme)
                                    val matchingSlider = if (theme == 1) 1 else 2
                                    db.collection("appSettings").document("homeBanner")
                                        .set(mapOf("activeDesign" to matchingSlider, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                        .addOnSuccessListener {
                                            saving = false
                                            message = if (theme == 1) "Default theme and Slider 1 applied across the app." else "Theme 1 and Slider 2 applied across the app."
                                        }
                                        .addOnFailureListener { e ->
                                            saving = false
                                            message = "Theme applied, but could not update its slider: ${e.localizedMessage ?: "Check Firestore permissions."}"
                                        }
                                }
                                .addOnFailureListener { e -> saving = false; message = "Could not apply theme: ${e.localizedMessage ?: "Check Firestore permissions."}" }
                        }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = primary), shape = RoundedCornerShape(12.dp)) {
                            if (saving && selectedTheme == theme) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(6.dp)); Text(if (theme == 1) "Apply Default" else "Apply Theme 1")
                        }
                    }
                }
            }
        }
        message?.let { Text(it, color = if (it.startsWith("Could not")) Color(0xFFB3261E) else Forest, fontSize = 13.sp) }
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
    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Forest) }
            Column { Text("HOME PAGE SLIDERS", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold); Text("Choose active design", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Gold.copy(alpha = 0.55f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show slider on Home", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (sliderEnabled) "Slider is visible on the home page" else "Slider is hidden from the home page", color = Muted, fontSize = 12.sp)
                }
                Switch(checked = sliderEnabled, onCheckedChange = { enabled ->
                    sliderEnabled = enabled
                    message = null
                    db.collection("appSettings").document("homeBanner")
                        .set(mapOf("enabled" to enabled, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                        .addOnFailureListener { e ->
                            sliderEnabled = !enabled
                            message = "Could not update slider visibility: ${e.localizedMessage ?: "Check Firestore permissions."}"
                        }
                        .addOnSuccessListener { message = if (enabled) "Home slider enabled." else "Home slider disabled." }
                })
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Choose a slider design and tap Apply. Slider 4 automatically follows the selected app theme.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        (1..4).forEach { design ->
            val colors = when (design) { 4 -> listOf(Forest, Forest.copy(alpha = 0.92f), Gold, Forest); 3 -> listOf(Color(0xFFF8F0DC), Color(0xFFE6EBCB), Color(0xFF6B8B52)); 2 -> listOf(Color(0xFF173D2B), Color(0xFF24523A), Color(0xFF762A24)); else -> listOf(Color(0xFF762A24), Color(0xFF8A3D2B), Gold) }
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { activeDesign = design; message = null }, shape = RoundedCornerShape(20.dp), border = BorderStroke(if (activeDesign == design) 2.dp else 1.dp, if (activeDesign == design) Gold else Color(0xFFE8DCC7)), colors = CardDefaults.cardColors(containerColor = Cream)) {
                Column(Modifier.padding(12.dp)) {
                    Box(Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(14.dp)).background(Color.Transparent).padding(0.dp)) {
                        if (design == 4) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&w=1000&q=85",
                                contentDescription = "Botanical background preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(Modifier.fillMaxSize().background(Forest.copy(alpha = 0.68f)))
                        } else {
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)))
                        }
                        Box(Modifier.fillMaxSize().padding(14.dp)) {
                        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.82f)) {
                            Text(when (design) { 4 -> "✦  THEME-ADAPTIVE SIGNATURE"; 3 -> "PURE AYURVEDA"; 2 -> "BOTANICAL WELLNESS"; else -> "SLGAN · TRUSTED SINCE 1990" }, color = if (design == 3) Color(0xFF23452C) else Color(0xFFFFDFA0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(when (design) { 4 -> "Wellness, Inspired by Nature"; 3 -> "Pure Ayurveda for Better Living"; 2 -> "Nature’s Goodness"; else -> "Authentic Ayurvedic Formulations" }, color = if (design == 3) Color(0xFF23452C) else if (design == 2) Color(0xFFFFE8AE) else Color.White, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(when (design) { 4 -> "Traditional wisdom · Trusted since 1990"; 3 -> "Traditional wisdom · Natural wellness"; 2 -> "Herbs · Roots · Everyday wellness"; else -> "Traditional wisdom · Natural wellness" }, color = if (design == 3) Color(0xFF5B3428) else if (design == 2) Color(0xFFE8F0D8) else Color(0xFFFFE9B2), fontSize = 10.sp)
                        }
                        Icon(if (activeDesign == design) Icons.Default.CheckCircle else Icons.Default.Circle, contentDescription = null, tint = if (design == 3) Color(0xFF35552F) else Gold, modifier = Modifier.align(Alignment.TopEnd).size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(when (design) { 1 -> "Slider 1 · Current banner"; 2 -> "Slider 2 · Nature’s Goodness"; 3 -> "Slider 3 · Cream & Green"; else -> "Slider 4 · Theme-Adaptive Signature" }, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(when (design) { 1 -> "Keep the existing look"; 2 -> "Deep botanical green, herbal imagery and gold accents"; 3 -> "Fresh botanical, natural look"; else -> "Uses the selected app theme’s Forest and Gold palette, with high-contrast text and button styling." }, color = Muted, fontSize = 12.sp)
                    if (activeDesign == design) Text("SELECTED", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Button(onClick = {
            saving = true; message = null
            db.collection("appSettings").document("homeBanner")
                .set(mapOf("activeDesign" to activeDesign, "enabled" to sliderEnabled, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { saving = false; message = "Slider $activeDesign applied to the home page." }
                .addOnFailureListener { e -> saving = false; message = "Could not apply slider: ${e.localizedMessage ?: "Check Firestore permissions."}" }
        }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp)); Text(if (saving) "Applying…" else "Apply Slider $activeDesign", fontWeight = FontWeight.Bold)
        }
        message?.let { Spacer(Modifier.height(10.dp)); Text(it, color = if (it.startsWith("Could not")) Color(0xFFB3261E) else Forest, fontSize = 13.sp) }
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

    val filtered = records.filter {
        (it["diseases_name"]?.toString().orEmpty() + " " + it["disease_desc"]?.toString().orEmpty())
            .contains(search.trim(), ignoreCase = true)
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!saving) showAddDialog = false },
            containerColor = Cream,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(Modifier.size(54.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Forest, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Add disease", color = Ink, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add a disease to the app catalogue.", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = diseaseName,
                        onValueChange = { diseaseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Disease name *") },
                        placeholder = { Text("e.g. Cough") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = diseaseDescription,
                        onValueChange = { diseaseDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                        placeholder = { Text("Enter details (optional)") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                    shape = RoundedCornerShape(12.dp)
                ) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Save disease") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showAddDialog = false }) { Text("Cancel", color = Muted) }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
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
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to dashboard", tint = Forest) }
                Column(Modifier.weight(1f)) {
                    Text("Diseases", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${records.size} disease catalogue items", color = Muted, fontSize = 12.sp)
                }
            }
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Forest)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(50.dp).background(Cream, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Forest, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Disease library", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Manage diseases shown in the customer app", color = Cream.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Forest, disabledContainerColor = Cream.copy(alpha = 0.7f), disabledContentColor = Forest)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add disease", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text("${if (loading) "—" else records.size.toString()} TOTAL", color = Cream.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 18.sp),
                label = { Text("Search diseases", modifier = Modifier.background(Color.Transparent), color = Forest, fontSize = 12.sp, maxLines = 1) },
                placeholder = { Text("Disease name or description", fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = Muted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Forest) },
                trailingIcon = {
                    if (search.isNotEmpty()) IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, "Clear search")
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Forest,
                    unfocusedBorderColor = Forest.copy(alpha = 0.75f),
                    focusedContainerColor = Cream,
                    unfocusedContainerColor = Cream,
                    cursorColor = Gold
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading -> AppLoader("Loading diseases…")
            loadError != null -> Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Muted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("Could not load diseases", color = Ink, fontWeight = FontWeight.Bold)
                Text(loadError.orEmpty(), color = Color(0xFFB3261E), fontSize = 12.sp)
                Text("Check that the latest Firestore rules are published.", color = Muted, fontSize = 12.sp)
            }
            filtered.isEmpty() -> EmptyState("🌿", if (search.isBlank()) "No diseases added yet" else "No matching diseases", "Use Add to create your first disease entry.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it["_documentId"].toString() }) { disease ->
                    val name = disease["diseases_name"]?.toString().orEmpty().ifBlank { "Unnamed disease" }
                    val description = disease["disease_desc"]?.toString().orEmpty().ifBlank { "Information not available." }
                    val diseaseEmoji = when (name.trim().lowercase()) {
                        "fever" -> "🤒"
                        "cough" -> "🦠"
                        "flu" -> "🫁"
                        "obesity" -> "⚖️"
                        "tuberculosis", "tuberculosis (tb)", "tb" -> "🫁"
                        else -> "🌿"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(44.dp).background(PaleGreen, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Text(diseaseEmoji, fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text(description, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
                            }
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

    val visibleCustomers = customers.filter { customer ->
        listOf("firstName", "lastName", "nickname", "email", "mobileNumber")
            .mapNotNull { customer[it]?.toString() }.joinToString(" ")
            .contains(search.trim(), ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().background(Color.White).padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Forest) }
            Column(Modifier.weight(1f)) {
                Text("Customers", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("${customers.size} customer accounts", color = Muted, fontSize = 12.sp)
            }
        }
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Forest)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(Cream, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Forest, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Customer library", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Registered customer accounts", color = Cream.copy(alpha = 0.88f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(customers.size.toString(), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("TOTAL", color = Cream.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 18.sp),
            label = { Text("Search name, email or mobile", modifier = Modifier.background(Color.Transparent), color = Forest, fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Forest) },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Forest,
                unfocusedBorderColor = Gold,
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                cursorColor = Gold
            )
        )
        Spacer(Modifier.height(10.dp))
        when {
            loading -> AppLoader("Loading customers…")
            error != null -> Text("Could not load customers: ${error}", color = Color(0xFFB3261E), modifier = Modifier.padding(16.dp))
            visibleCustomers.isEmpty() -> EmptyState("👥", if (search.isBlank()) "No customers yet" else "No matching customers", "Registered customer accounts will appear here.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleCustomers, key = { it["uid"].toString() }) { customer ->
                    val uid = customer["uid"].toString()
                    val firstName = customer["firstName"]?.toString().orEmpty().trim()
                    val lastName = customer["lastName"]?.toString().orEmpty().trim()
                    val nickname = customer["nickname"]?.toString().orEmpty().trim()
                    val name = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { nickname }.ifBlank { "Customer" }
                    val email = customer["email"]?.toString().orEmpty()
                    val phone = customer["mobileNumber"]?.toString().orEmpty()
                    val gender = customer["gender"]?.toString().orEmpty()
                    val enabled = (customer["is_active"] as? Boolean) ?: (customer["enabled"] != false)
                    val isUpdating = uid in updatingIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Forest)
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(email.ifBlank { "Email not provided" }, color = Muted, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Forest)
                                    )
                                    Text(if (enabled) "Enabled" else "Disabled", color = if (enabled) Forest else Color(0xFFB3261E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = PaleGreen)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewingCustomer = customer }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View customer details", tint = Forest)
                                }
                                IconButton(onClick = { editingCustomer = customer }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit customer details", tint = Forest)
                                }
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
            containerColor = Cream,
            title = { Text("Customer details", color = Forest, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(displayName, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    customer.entries.filter { it.key != "uid" && it.key != "password" && it.key != "passwordHash" && it.value != null && it.value.toString().isNotBlank() }
                        .sortedBy { it.key }
                        .forEach { entry ->
                            Text(entry.key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() } + ": " + entry.value.toString(), color = Muted, fontSize = 13.sp)
                        }
                    Text("Customer ID: $uid", color = Muted, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { viewingCustomer = null }) { Text("Close", color = Forest) } }
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
            containerColor = Cream,
            title = { Text("Edit customer", color = Forest, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(firstName, { firstName = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(lastName, { lastName = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(nickname, { nickname = it }, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(0.42f)) {
                            OutlinedButton(
                                onClick = { adminCountryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Gold),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("${adminSelectedCountry.second} ${adminSelectedCountry.third} ▾", color = Forest, maxLines = 1, fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded = adminCountryMenuExpanded,
                                onDismissRequest = { adminCountryMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 300.dp).background(Cream)
                            ) {
                                adminCountryOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option.second}  ${option.first}  ${option.third}", color = Ink) },
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
                            label = { Text("Mobile number") },
                            modifier = Modifier.weight(0.58f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                    OutlinedTextField(gender, { gender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                }) { Text(if (savingCustomer) "Saving…" else "Save", color = Forest) }
            },
            dismissButton = { TextButton(enabled = !savingCustomer, onClick = { editingCustomer = null }) { Text("Cancel", color = Muted) } }
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
                listOf("orderId", "_documentId", "customerName", "phone", "email", "shippingAddress", "orderStatus")
                    .any { key -> order[key]?.toString()?.lowercase()?.contains(query) == true } ||
                ((order["items"] as? List<*>)?.any { raw ->
                    val item = raw as? Map<*, *>
                    item?.values?.any { it?.toString()?.lowercase()?.contains(query) == true } == true
                } == true)
            matchesStatus && matchesQuery
        }
    }
    val grouped = filteredOrders.groupBy { row ->
        val ts = row["createdAt"] as? com.google.firebase.Timestamp
        if (ts == null) "Date not available" else dateFormat.format(ts.toDate())
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Forest) }
            Column(Modifier.weight(1f)) {
                Text("Manage Orders", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("${filteredOrders.size} of ${orders.size} orders • newest first", color = Ink.copy(alpha = 0.65f), fontSize = 12.sp)
            }
            Icon(Icons.Default.Inventory2, contentDescription = "Orders", tint = Forest, modifier = Modifier.size(27.dp))
        }
        Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Forest)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Cream, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Orders library", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Review and manage customer orders", color = Cream.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(orders.size.toString(), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("TOTAL", color = Cream.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).height(72.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 18.sp),
            label = { Text("Search order", modifier = Modifier.background(Color.Transparent), color = Forest, fontSize = 12.sp, maxLines = 1) },
            placeholder = { Text("Order ID, customer or mobile", fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search orders", tint = Forest) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Forest,
                unfocusedBorderColor = Gold,
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                cursorColor = Gold
            )
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Cream),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Filter orders by status", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Forest, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                    FilterChip(
                        selected = statusFilter == "Pending",
                        onClick = { statusFilter = "Pending" },
                        label = { Text("Pending  ${pendingCount}") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF8A5A12), selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                    FilterChip(
                        selected = statusFilter == "Accepted",
                        onClick = { statusFilter = "Accepted" },
                        label = { Text("Accepted  ${acceptedCount}") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF206B3A), selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                    FilterChip(
                        selected = statusFilter == "Rejected",
                        onClick = { statusFilter = "Rejected" },
                        label = { Text("Rejected  ${rejectedCount}") },
                        leadingIcon = { Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFB3261E), selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                    )
                }
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) }
            error != null -> Text("Could not load orders: " + error, color = Color(0xFFB3261E), modifier = Modifier.padding(20.dp))
            orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No orders yet", color = Muted, fontSize = 18.sp) }
            filteredOrders.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No orders match your search", color = Muted, fontSize = 16.sp)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.forEach { (date, rows) ->
                    item(key = "date-" + date) {
                        Text(date + " • " + rows.size + " orders", color = Forest, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
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
                            Modifier.fillMaxWidth().clickable { expandedOrderId = if (expanded) null else docId },
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Cream)
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(order["customerName"]?.toString().orEmpty().ifBlank { "Customer" }, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        Text("Order #" + displayId.takeLast(12) + " • " + time, color = Muted, fontSize = 11.sp)
                                        Text(order["phone"]?.toString().orEmpty().ifBlank { "Phone not provided" }, color = Muted, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹" + String.format(java.util.Locale.US, "%.2f", total), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                        Text(status.replace("_", " ").replaceFirstChar { it.uppercase() }, color = if (status == "accepted") Color(0xFF206B3A) else if (status == "rejected") Color(0xFFB3261E) else Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse order" else "Expand order", tint = Forest)
                                    }
                                }
                                if (expanded) {
                                    HorizontalDivider(color = PaleGreen)
                                    Text("Order details", color = Forest, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Order ID: " + displayId, color = Ink, fontSize = 12.sp)
                                    Text("Customer: " + (order["customerName"]?.toString().orEmpty().ifBlank { "Information not available" }), color = Ink, fontSize = 12.sp)
                                    Text("Email: " + (order["email"]?.toString().orEmpty().ifBlank { "Information not available" }), color = Ink, fontSize = 12.sp)
                                    Text("Phone: " + (order["phone"]?.toString().orEmpty().ifBlank { "Information not available" }), color = Ink, fontSize = 12.sp)
                                    Text("Delivery address: " + (order["shippingAddress"]?.toString().orEmpty().ifBlank { "Information not available" }), color = Ink, fontSize = 12.sp)
                                    Text("Order time: " + (ts?.let { java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(it.toDate()) } ?: "Information not available"), color = Muted, fontSize = 12.sp)
                                    Text("Items", color = Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (itemRows.isEmpty()) {
                                        Text("No item details were saved with this order.", color = Muted, fontSize = 12.sp)
                                    } else {
                                        itemRows.forEachIndexed { index, raw ->
                                            val item = raw as? Map<*, *>
                                            if (item != null) {
                                                val name = item["productName"]?.toString() ?: item["name"]?.toString() ?: "Item"
                                                val qty = (item["quantity"] as? Number)?.toInt() ?: 0
                                                val price = (item["price"] as? Number)?.toDouble() ?: (item["unitPrice"] as? Number)?.toDouble()
                                                val lineTotal = (item["lineTotal"] as? Number)?.toDouble() ?: (item["total"] as? Number)?.toDouble()
                                                Text(
                                                    "${index + 1}. ${name} × ${qty}" +
                                                        (if (price != null) " • ₹" + String.format(java.util.Locale.US, "%.2f", price) + " each" else "") +
                                                        (if (lineTotal != null) " • ₹" + String.format(java.util.Locale.US, "%.2f", lineTotal) else ""),
                                                    color = Muted, fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = PaleGreen)
                                    Text("Order total: ₹" + String.format(java.util.Locale.US, "%.2f", total), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    if (pending) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    busy = docId
                                                    db.collection("orders").document(docId)
                                                        .update(mapOf("orderStatus" to "rejected", "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                        .addOnSuccessListener { busy = null; Toast.makeText(context, "Order rejected.", Toast.LENGTH_SHORT).show() }
                                                        .addOnFailureListener { e -> busy = null; Toast.makeText(context, "Reject failed: " + (e.localizedMessage ?: "Try again"), Toast.LENGTH_LONG).show() }
                                                },
                                                enabled = busy == null,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E))
                                            ) {
                                                Icon(Icons.Default.Close, null); Spacer(Modifier.width(5.dp)); Text(if (busy == docId) "Updating…" else "Reject")
                                            }
                                            Button(
                                                onClick = {
                                                    busy = docId
                                                    db.collection("orders").document(docId)
                                                        .update(mapOf("orderStatus" to "accepted", "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                        .addOnSuccessListener { busy = null; Toast.makeText(context, "Order approved.", Toast.LENGTH_SHORT).show() }
                                                        .addOnFailureListener { e -> busy = null; Toast.makeText(context, "Approve failed: " + (e.localizedMessage ?: "Try again"), Toast.LENGTH_LONG).show() }
                                                },
                                                enabled = busy == null,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Forest)
                                            ) {
                                                Icon(Icons.Default.Check, null); Spacer(Modifier.width(5.dp)); Text(if (busy == docId) "Updating…" else "Approve")
                                            }
                                        }
                                    } else {
                                        Text("This order has already been ${status.replace("_", " ")}.", color = Muted, fontSize = 12.sp)
                                    }
                                } else {
                                    Text("Tap to view order details", color = Muted, fontSize = 11.sp)
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
        colors = CardDefaults.cardColors(containerColor = Cream)
    ) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
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
        colors = CardDefaults.cardColors(containerColor = Cream)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(PaleGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Forest)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ShopScreen(
    selectedTab: String, onTabChange: (String) -> Unit, appLanguage: String, onLanguageChange: (String) -> Unit, category: String, onCategoryChange: (String) -> Unit,
    search: String, onSearchChange: (String) -> Unit, cart: Map<String, Int>, products: List<Product>, productRatings: Map<String, ProductRatingSummary>, productLikes: Map<String, ProductLikeSummary>, onToggleLike: (Product) -> Unit, productsLoading: Boolean, productsError: String?, diseases: List<String>, diseasesLoading: Boolean, diseasesError: String?, latestOrderId: String?, latestOrderSuccess: Boolean?, latestOrderInvoiceUrl: String?, latestOrderMessage: String?, onAdd: (String) -> Unit, onRemove: (String) -> Unit,
    onOrderResult: (String, Boolean, String?, String) -> Unit, onSignOut: () -> Unit, onProductClick: (Product) -> Unit
) {
    val cartCount = cart.values.sum()
    Scaffold(containerColor = Cream, topBar = {
        Row(Modifier.fillMaxWidth().background(Cream).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.slgan_screen_logo), contentDescription = "SLGAN logo", contentScale = ContentScale.Fit, modifier = Modifier.size(42.dp).clip(CircleShape).background(Cream))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(tr("Sri Lakshmi Ganapathi", LocalAppLanguage.current), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tr("Ayurveda Nilayam", LocalAppLanguage.current), color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onTabChange("Cart") }) {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart", tint = Forest) }
            }
        }
    }, bottomBar = {
        NavigationBar(containerColor = Cream, contentColor = Forest) {
            listOf(Triple("Home", Icons.Default.Home, "Home"), Triple("Categories", Icons.Default.GridView, "Categories"), Triple("Diseases", Icons.Default.MedicalInformation, "Diseases"), Triple("Orders", Icons.Default.ReceiptLong, "Orders"), Triple("Profile", Icons.Default.Person, "Profile")).forEach { (label, icon, route) ->
                NavigationBarItem(selected = selectedTab == route, onClick = { onTabChange(route) }, icon = { Icon(icon, label) }, label = { Text(tr(label, LocalAppLanguage.current), fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = PaleGreen, unselectedIconColor = Muted))
            }
        }
    }) { padding ->
        when (selectedTab) {
            "Cart" -> CartPage(Modifier.padding(padding), cart, products, onAdd, onRemove, onOrderResult)
            "Orders" -> OrdersPage(Modifier.padding(padding), latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage) { onTabChange("Home") }
            "Categories" -> CategoriesPage(Modifier.padding(padding), products, cart, productRatings, productLikes, onToggleLike, onAdd, onRemove, onCategoryChange) { onTabChange("Home") }
            "Diseases" -> DiseasesPage(Modifier.padding(padding), diseases, products, diseasesLoading, diseasesError)
            "Profile" -> ProfilePage(Modifier.padding(padding), onSignOut, appLanguage, onLanguageChange)
            else -> HomePage(Modifier.padding(padding), category, onCategoryChange, search, onSearchChange, cart, products, productRatings, productLikes, onToggleLike, productsLoading, productsError, onAdd, onProductClick)
        }
    }
}

@Composable
private fun HomePage(modifier: Modifier, category: String, onCategoryChange: (String) -> Unit, search: String, onSearchChange: (String) -> Unit, cart: Map<String, Int>, products: List<Product>, productRatings: Map<String, ProductRatingSummary>, productLikes: Map<String, ProductLikeSummary>, onToggleLike: (Product) -> Unit, productsLoading: Boolean, productsError: String?, onAdd: (String) -> Unit, onProductClick: (Product) -> Unit) {
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
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(4.dp))
                Text(if (welcomeNickname.isNotBlank()) "Welcome, $welcomeNickname" else "Welcome to SLGAN", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(tr("Rooted in Tradition. Inspired by Nature.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
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
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.95f), Forest.copy(alpha = 0.72f), Gold.copy(alpha = 0.95f))),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    placeholder = { Text(tr("Search products", LocalAppLanguage.current), fontSize = 14.sp, lineHeight = 18.sp, color = Muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier.size(30.dp).background(
                                Brush.linearGradient(listOf(Forest, Leaf)),
                                CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = tr("Search products", LocalAppLanguage.current), modifier = Modifier.size(19.dp), tint = Cream)
                        }
                    },
                    trailingIcon = {
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
                            Icon(Icons.Default.Mic, contentDescription = tr("Voice search", LocalAppLanguage.current), tint = Forest)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        cursorColor = Forest
                    )
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                    Text(tr("Shop by category", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
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
                            label = { Text(item, fontSize = 11.sp) },
                            leadingIcon = {
                                Text(
                                    text = when (item.trim().lowercase()) {
                                        "all" -> "🌿"
                                        "lehyas" -> "🫙"
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
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Forest, selectedLabelColor = Color.White, containerColor = Cream)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (search.isNotBlank()) Text("Search results", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                if (productsLoading) {
                    Spacer(Modifier.height(18.dp))
                    AppLoader(message = "Loading products…", compact = true)
                }
                if (productsError != null) { Spacer(Modifier.height(8.dp)); Text(tr("Could not load products: $productsError", LocalAppLanguage.current), color = Color(0xFFB3261E), fontSize = 12.sp) }
            }
        }
        if (visible.isEmpty() && !productsLoading) item { Text(if (productsError != null) "Products could not be loaded. Check your internet and Firestore rules." else "No products found in this category. Try All or another search.", Modifier.padding(24.dp), color = Muted) }
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
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                                Text("Signature Products", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                            }
                            Text("SLGAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Leaf)
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(signatureProducts, key = { it.id }) { product ->
                                Card(
                                    modifier = Modifier.width(198.dp).height(265.dp).clickable { onProductClick(product) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Cream),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Box(
                                            Modifier.fillMaxWidth().height(112.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(PaleGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (product.imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = product.imageUrl,
                                                    contentDescription = product.name,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(product.emoji, fontSize = 43.sp)
                                            }
                                            Box(
                                                modifier = Modifier.align(Alignment.TopStart).padding(5.dp).size(42.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(Modifier.fillMaxSize()) {
                                                    val center = Offset(size.width / 2f, size.height / 2f)
                                                    val outerRadius = size.minDimension * 0.47f
                                                    for (i in 0 until 16) {
                                                        val angle = (i * 22.5f - 90f) * (Math.PI.toFloat() / 180f)
                                                        val inner = outerRadius * 0.72f
                                                        val outer = outerRadius
                                                        drawLine(
                                                            color = Color(0xFFE4AC37),
                                                            start = Offset(center.x + kotlin.math.cos(angle) * inner, center.y + kotlin.math.sin(angle) * inner),
                                                            end = Offset(center.x + kotlin.math.cos(angle) * outer, center.y + kotlin.math.sin(angle) * outer),
                                                            strokeWidth = 1.8.dp.toPx(),
                                                            cap = StrokeCap.Round
                                                        )
                                                    }
                                                    drawCircle(color = Color(0xFFE4AC37), radius = outerRadius * 0.72f, center = center)
                                                    drawCircle(color = Color(0xFF762A24), radius = outerRadius * 0.61f, center = center)
                                                }
                                                Text(
                                                    "SLG",
                                                    color = Color(0xFFFFE6A3),
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            localizedProductName(product.name, LocalAppLanguage.current),
                                            fontSize = 13.sp,
                                            color = Ink,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            minLines = 1,
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
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF762A24),
                                                        contentColor = Color(0xFFFFF7E8)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
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
                                                            Text(product.category, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                                if (product.subtitle.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFF5E5C8),
                                                        contentColor = Color(0xFF762A24)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Text("⚖", fontSize = 9.sp)
                                                            Text(product.subtitle.replace(" ", ""), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                            ProductRatingLine(product, productRatings[product.id] ?: ProductRatingSummary(), compact = true)
                                            Spacer(Modifier.width(6.dp))
                                            ProductLikeControl(product, productLikes[product.id] ?: ProductLikeSummary(), onToggleLike)
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                tr("₹", LocalAppLanguage.current) + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price),
                                                color = Forest,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                maxLines = 1
                                            )
                                            val signatureCartCount = cart[product.id] ?: 0
                                            if (signatureCartCount == 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(
                                                            Brush.linearGradient(listOf(Forest, Leaf)),
                                                            CircleShape
                                                        )
                                                        .then(if (product.price > 0.0) Modifier.clickable { onAdd(product.id) } else Modifier),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add ${product.name} to cart",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Cream,
                                                    contentColor = Forest
                                                ) {
                                                    Text(
                                                        "${signatureCartCount} in cart",
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
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
        item { Text(tr("Made with care for your everyday rituals.", LocalAppLanguage.current), Modifier.fillMaxWidth().padding(20.dp), color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun ProductLikeControl(product: Product, summary: ProductLikeSummary, onToggleLike: (Product) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        IconButton(onClick = { onToggleLike(product) }, modifier = Modifier.size(30.dp)) {
            Icon(if (summary.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (summary.likedByMe) "Unlike product" else "Like product",
                tint = if (summary.likedByMe) Color(0xFFD32F2F) else Muted, modifier = Modifier.size(19.dp))
        }
        Text(summary.count.toString(), color = if (summary.likedByMe) Color(0xFFD32F2F) else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProductRatingLine(product: Product, summary: ProductRatingSummary, showEmptyLabel: Boolean = false, compact: Boolean = false) {
    val context = LocalContext.current
    val ratingUser = FirebaseAuth.getInstance().currentUser
    var savingRating by remember(product.id, ratingUser?.uid) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        (1..5).forEach { star ->
            val starSize = if (compact) 15.dp else 20.dp
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
            Text(String.format(java.util.Locale.US, "%.1f", summary.average), color = Ink, fontWeight = FontWeight.Bold, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
            Text("(${summary.count})", color = Muted, fontSize = if (compact) 8.sp else 9.sp, maxLines = 1)
        } else if (showEmptyLabel) {
            Text("No ratings", color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProductCard(product: Product, count: Int, ratingSummary: ProductRatingSummary, likeSummary: ProductLikeSummary, onToggleLike: (Product) -> Unit, onAdd: (String) -> Unit, onProductClick: (Product) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable { onProductClick(product) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Cream), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(11.dp)) {
            Box(Modifier.fillMaxWidth().height(98.dp).background(PaleGreen, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(product.emoji, fontSize = 42.sp)
                Text(tr("SLGAN", LocalAppLanguage.current), Modifier.align(Alignment.BottomEnd).padding(6.dp), color = Leaf, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                if (product.signature) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(5.dp).size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val outerRadius = size.minDimension * 0.47f
                            for (i in 0 until 16) {
                                val angle = (i * 22.5f - 90f) * (Math.PI.toFloat() / 180f)
                                val inner = outerRadius * 0.72f
                                drawLine(
                                    color = Color(0xFFE4AC37),
                                    start = Offset(center.x + kotlin.math.cos(angle) * inner, center.y + kotlin.math.sin(angle) * inner),
                                    end = Offset(center.x + kotlin.math.cos(angle) * outerRadius, center.y + kotlin.math.sin(angle) * outerRadius),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            drawCircle(color = Color(0xFFE4AC37), radius = outerRadius * 0.72f, center = center)
                            drawCircle(color = Color(0xFF762A24), radius = outerRadius * 0.61f, center = center)
                        }
                        Text("SLG", color = Color(0xFFFFE6A3), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(localizedProductName(product.name, LocalAppLanguage.current), fontSize = 13.sp, lineHeight = 15.sp, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            if (product.category.isNotBlank() || product.subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF762A24),
                            contentColor = Color(0xFFFFF7E8)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
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
                                        "rasayanas" -> "🪴"
                                        "bhasmas" -> "🪨"
                                        else -> "🌿"
                                    },
                                    fontSize = 9.sp
                                )
                                Text(product.category, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (product.subtitle.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF5E5C8),
                            contentColor = Color(0xFF762A24)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("⚖", fontSize = 9.sp)
                                Text(product.subtitle.replace(" ", ""), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))

                }
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    ProductRatingLine(product, ratingSummary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    IconButton(onClick = { onToggleLike(product) }, modifier = Modifier.size(24.dp)) {
                        Icon(if (likeSummary.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (likeSummary.likedByMe) "Unlike product" else "Like product", tint = if (likeSummary.likedByMe) Color(0xFFD32F2F) else Muted, modifier = Modifier.size(15.dp))
                    }
                    Text(likeSummary.count.toString(), color = if (likeSummary.likedByMe) Color(0xFFD32F2F) else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("₹", LocalAppLanguage.current) + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (count == 0) FilledIconButton(onClick = { onAdd(product.id) }, modifier = Modifier.size(28.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Forest)) { Icon(Icons.Default.Add, "Add " + product.name, modifier = Modifier.size(15.dp)) }
                else Text(count.toString() + " in cart", fontSize = 10.sp, color = Forest, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun ProductDetailsPage(
    product: Product,
    ratingSummary: ProductRatingSummary,
    likeSummary: ProductLikeSummary,
    onToggleLike: () -> Unit,
    cartCount: Int,
    onBack: () -> Unit,
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
    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back to products", tint = Forest) }
            Text(tr("View product", LocalAppLanguage.current), modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            IconButton(onClick = {
                val productLink = "https://slgan-a364e.web.app/product?id=${Uri.encode(product.id)}"
                val shareText = buildString {
                    append("Check out this product from Sri Lakshmi Ganapathi Ayurveda Nilayam (SLGAN)\n\n")
                    append(localizedProductName(product.name, currentLanguage))
                    if (product.subtitle.isNotBlank()) append("\nPack size: ${product.subtitle}")
                    if (product.category.isNotBlank()) append("\nCategory: ${product.category}")
                    if (product.price > 0.0) {
                        val priceText = if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price)
                        append("\nPrice: ₹$priceText")
                    } else append("\nPrice: Not available yet")
                    if (product.description.isNotBlank()) append("\n\nDescription: ${product.description}")
                    if (product.usage.isNotBlank()) append("\nUsage: ${product.usage}")
                    if (product.majorIngredients.isNotBlank()) append("\nMajor ingredients: ${product.majorIngredients}")
                    append("\n\nView product: $productLink")
                    append("\n\nShared from SLGAN.")
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "SLGAN - ${product.name}")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(shareIntent, tr("Share product", currentLanguage)))
            }) {
                Icon(Icons.Default.Share, contentDescription = tr("Share product", LocalAppLanguage.current), tint = Forest)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                if (product.imageUrl.isNotBlank()) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                webViewClient = WebViewClient()
                                val safeUrl = product.imageUrl.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
                                loadDataWithBaseURL(null, "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'></head><body style='margin:0;background:#fff;height:100%;display:flex;align-items:center;justify-content:center'><img src=\"$safeUrl\" style='max-width:100%;max-height:100%;object-fit:contain'/></body></html>", "text/html", "UTF-8", null)
                            }
                        }
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(210.dp).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Text(product.emoji, fontSize = 82.sp)
                    }
                }
            }
            Text(
                localizedProductName(product.name, LocalAppLanguage.current),
                color = Ink,
                fontSize = 25.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PaleGreen,
                        contentColor = Forest
                    ) {
                        Row(
                            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text(product.category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (product.subtitle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFEBC2),
                        contentColor = Ink
                    ) {
                        Row(
                            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text(product.subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                ProductLikeControl(product, likeSummary, { onToggleLike() })
            }
            if (product.price > 0.0) {
                Text(tr("₹", LocalAppLanguage.current) + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price), color = Forest, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                Text(tr("Price not available yet.", LocalAppLanguage.current), color = Muted, fontSize = 15.sp)
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Customer rating", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        ProductRatingLine(product, ratingSummary, showEmptyLabel = true)
                    }
                    TextButton(onClick = { showReviewDialog = true; ratingMessage = null }) {
                        Text("⭐", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Add review", color = Forest, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (customerReviews.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer reviews", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    customerReviews.forEach { (customerName, reviewText) ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(customerName, color = Forest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(reviewText, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
            if (showReviewDialog) {
                AlertDialog(
                    onDismissRequest = { if (!ratingSaving) showReviewDialog = false },
                    title = { Text("Write a review", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(localizedProductName(product.name, currentLanguage), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = ratingReview,
                                onValueChange = { if (it.length <= 1000) ratingReview = it; ratingMessage = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Your review (optional)") },
                                placeholder = { Text("Share your experience with this product") },
                                minLines = 3, maxLines = 5, shape = RoundedCornerShape(12.dp)
                            )
                            ratingMessage?.let {
                                Text(it, color = if (it.startsWith("Saved")) Forest else Color(0xFFB3261E), fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val user = ratingUser
                                if (user == null) ratingMessage = "Please sign in to submit a review."
                                else if (ratingReview.trim().isBlank()) ratingMessage = "Please enter your review before saving."
                                else {
                                    ratingSaving = true
                                    val db = FirebaseFirestore.getInstance()
                                    val ratingRef = db.collection("productRatings").document(product.id.replace("/", "_") + "_" + user.uid)
                                    ratingRef.get().addOnSuccessListener { existing ->
                                        val payload = hashMapOf<String, Any>(
                                            "productId" to product.id, "productName" to product.name, "userId" to user.uid,
                                            "userName" to (user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty().substringBefore("@")),
                                            "rating" to (existing.getLong("rating")?.toInt() ?: 0), "review" to ratingReview.trim(),
                                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                                        if (!existing.exists()) payload["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        ratingRef.set(payload, com.google.firebase.firestore.SetOptions.merge())
                                            .addOnSuccessListener {
                                                ratingSaving = false
                                                ratingMessage = "Saved! Thank you for your review."
                                                showReviewDialog = false
                                                Toast.makeText(context, "Review saved successfully", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { error -> ratingSaving = false; ratingMessage = error.localizedMessage ?: "Could not save rating. Please try again." }
                                    }.addOnFailureListener { error -> ratingSaving = false; ratingMessage = error.localizedMessage ?: "Could not save rating. Please try again." }
                                }
                            },
                            enabled = !ratingSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            if (ratingSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReviewDialog = false }, enabled = !ratingSaving) { Text("Cancel", color = Muted) }
                    },
                    shape = RoundedCornerShape(22.dp),
                    containerColor = Cream
                )
            }
            HorizontalDivider(color = Gold.copy(alpha = 0.45f))
            Text(tr("Description", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                product.description.ifBlank { "Information not available." },
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            Text(tr("Cures", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(product.cures.ifBlank { "Information not available." }, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
            Text(tr("Usage", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(product.usage.ifBlank { "Information not available." }, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
            Text(tr("Major ingredients", LocalAppLanguage.current), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(product.majorIngredients.ifBlank { "Information not available." }, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
        }
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (cartCount > 0) {
                IconButton(onClick = { onRemove(product.id) }) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove one", tint = Forest) }
                Text(cartCount.toString(), color = Ink, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { if (product.price > 0.0) onAdd(product.id) },
                enabled = product.price > 0.0,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                Text(if (cartCount > 0) "Add another" else "Add to cart", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartPage(modifier: Modifier, cart: Map<String, Int>, products: List<Product>, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onOrderResult: (String, Boolean, String?, String) -> Unit) {
    val selected = products.filter { (cart[it.id] ?: 0) > 0 }
    val total = selected.sumOf { it.price * (cart[it.id] ?: 0) }
    var showCheckout by remember { mutableStateOf(false) }
    val signedInUser = FirebaseAuth.getInstance().currentUser
    var customerName by remember(signedInUser?.uid) { mutableStateOf(signedInUser?.displayName.orEmpty()) }
    var email by remember(signedInUser?.uid) { mutableStateOf(signedInUser?.email.orEmpty()) }
    var phone by remember(signedInUser?.uid) { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
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

    Column(modifier.fillMaxSize().background(Cream).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Forest),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("SRI LAKSHMI GANAPATHI", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Your shopping bag", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(5.dp))
                        Text("Review medicines before placing order.", fontSize = 13.sp, color = PaleGreen, lineHeight = 19.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(48.dp).background(Color(0x26FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("🛍️", fontSize = 27.sp)
                    }
                }

            }
        }
        Spacer(Modifier.height(18.dp))
        if (selected.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Cream)
            ) {
                Column(
                    Modifier.fillMaxWidth().fillMaxHeight().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.size(104.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Text("🛍️", fontSize = 52.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Your cart is waiting", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Discover herbal favourites and add something special to your bag.", fontSize = 14.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR SELECTION", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Leaf, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text("${selected.size} products", fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(selected) { product ->
                    val quantity = cart[product.id] ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(66.dp).background(PaleGreen, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text(product.emoji, fontSize = 32.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PaleGreen,
                                        contentColor = Forest
                                    ) {
                                        Text(
                                            product.subtitle,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFEBC2),
                                        contentColor = Color(0xFF762A24)
                                    ) {
                                        Text(
                                            "₹" + String.format("%.2f", product.price) + " each",
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("₹" + String.format("%.2f", product.price * quantity), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .border(1.dp, Gold, RoundedCornerShape(14.dp))
                                        .background(Cream, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 2.dp)
                                ) {
                                    IconButton(onClick = { onRemove(product.id) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = "Remove one", tint = Forest, modifier = Modifier.size(17.dp))
                                    }
                                    Text(quantity.toString(), fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 14.sp)
                                    IconButton(onClick = { onAdd(product.id) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = "Add one", tint = Forest, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Cream),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ORDER SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Leaf, letterSpacing = 1.3.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Subtotal · ${cart.values.sum()} items", color = Muted, fontSize = 13.sp)
                        }
                        Text("₹" + String.format("%.2f", total), color = Forest, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showCheckout = true; errorMessage = null },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Forest),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(tr("Checkout", LocalAppLanguage.current) + "  →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Leaf, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("No online payment is collected in the app.", color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    if (showCheckout) {
        AlertDialog(
            onDismissRequest = { if (!submitting) showCheckout = false },
            title = { Text(tr("Place order", LocalAppLanguage.current)) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Total: ₹", LocalAppLanguage.current) + String.format("%.2f", total), fontWeight = FontWeight.Bold, color = Forest)
                    OutlinedTextField(customerName, { customerName = it }, label = { Text(tr("Customer name *", LocalAppLanguage.current)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text(tr("Email for invoice *", LocalAppLanguage.current)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(phone, { phone = it }, label = { Text(tr("Phone number", LocalAppLanguage.current)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(address, { address = it }, label = { Text(tr("Full delivery address *", LocalAppLanguage.current)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Text(tr("Payment: none. This order request does not collect payment.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                    if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    if (orderMessage != null) Text(orderMessage!!, color = Forest)
                }
            },
            confirmButton = {
                Button(enabled = !submitting && orderMessage == null, onClick = {
                    if (customerName.isBlank() || !email.contains("@") || address.isBlank()) {
                        errorMessage = "Enter your name, valid email and full delivery address."
                    } else {
                        submitting = true
                        errorMessage = null
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            errorMessage = "Please sign in again before placing an order."
                            submitting = false
                        } else {
                            val orderId = "SLGAN-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
                            val itemPayload = selected.map { product ->
                                mapOf(
                                    "productId" to product.id,
                                    "productName" to product.name,
                                    "unitPrice" to product.price,
                                    "quantity" to (cart[product.id] ?: 0),
                                    "lineTotal" to (product.price * (cart[product.id] ?: 0))
                                )
                            }
                            val now = com.google.firebase.Timestamp.now()
                            val orderFields = mapOf<String, Any>(
                                "orderId" to orderId,
                                "customerId" to user.uid,
                                "customerName" to customerName.trim(),
                                "email" to email.trim(),
                                "phone" to phone.trim(),
                                "shippingAddress" to address.trim(),
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
                            FirebaseFirestore.getInstance().collection("orders").document(orderId)
                                .set(orderFields)
                                .addOnSuccessListener {
                                    orderMessage = "Order saved: " + orderId
                                    submitting = false
                                    showCheckout = false
                                    onOrderResult(orderId, true, null, "Order saved. Payment has not been collected; the shop must verify the order.")
                                }
                                .addOnFailureListener { err ->
                                    val message = err.localizedMessage ?: "Could not save order. Please try again."
                                    errorMessage = message
                                    submitting = false
                                    onOrderResult("", false, null, message)
                                }
                        }
                    }
                }) { Text(if (submitting) "Placing order…" else "Place order") }
            },
            dismissButton = { TextButton(enabled = !submitting, onClick = { showCheckout = false }) { Text(tr("Cancel", LocalAppLanguage.current)) } }
        )
    }
}

@Composable
private fun OrdersPage(
    modifier: Modifier,
    latestOrderId: String?,
    latestOrderSuccess: Boolean?,
    latestInvoiceUrl: String?,
    latestOrderMessage: String?,
    onHome: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    var previousOrders by remember(user?.uid) { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var loadingOrders by remember(user?.uid) { mutableStateOf(true) }
    var ordersError by remember(user?.uid) { mutableStateOf<String?>(null) }
    var selectedOrder by remember { mutableStateOf<Map<String, Any?>?>(null) }

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
    val currentOrders = previousOrders.filter { orderDate(it) == today }
    val historicalOrders = previousOrders.filter { orderDate(it)?.isBefore(today) != false }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Forest),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("SRI LAKSHMI GANAPATHI", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(tr("My Orders", LocalAppLanguage.current), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color(0xFFFFE7A3)
                    ) {
                        Text(
                            "Total: ${previousOrders.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Forest,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("Track orders and revisit.", color = PaleGreen, fontSize = 13.sp, lineHeight = 19.sp)

            }
        }

        Spacer(Modifier.height(18.dp))
        if (latestOrderSuccess != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (latestOrderSuccess) Color(0xFFE5F4E8) else Color(0xFFFCE8E6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (latestOrderSuccess) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = null, tint = if (latestOrderSuccess) Color(0xFF237344) else Color(0xFFB3261E))
                        Spacer(Modifier.width(8.dp))
                        Text(if (latestOrderSuccess) "Order successful" else "Order failed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (latestOrderSuccess) Color(0xFF237344) else Color(0xFFB3261E))
                    }
                    latestOrderId?.let { Text(tr("Order ID: ", LocalAppLanguage.current) + it, color = Ink, fontWeight = FontWeight.SemiBold) }
                    Text(latestOrderMessage ?: if (latestOrderSuccess) "Your order was saved." else "Your order could not be placed.", color = Ink)
                    if (latestOrderSuccess && !latestInvoiceUrl.isNullOrBlank()) {
                        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestInvoiceUrl))) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("View / Download Invoice", LocalAppLanguage.current))
                        }
                    } else if (latestOrderSuccess) {
                        Text(tr("Your order is saved. The invoice is not available here yet; please check your email or contact the shop.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Current orders", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Placed today · ${today.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))}", color = Muted, fontSize = 12.sp)
            }
            Box(Modifier.background(PaleGreen, RoundedCornerShape(50.dp)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                Text(currentOrders.size.toString(), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loadingOrders -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Forest, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Loading your orders…", color = Muted, fontSize = 13.sp)
                }
            }
            ordersError != null -> Text(ordersError!!, color = MaterialTheme.colorScheme.error)
            currentOrders.isEmpty() -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Cream), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(56.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("No orders placed today", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Ready to discover your Ayurvedic essentials?", color = Muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 19.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onHome) { Text("Click here to order  →", color = Forest, fontWeight = FontWeight.Bold) }

                }
            }
            else -> currentOrders.forEach { order ->
                val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
                val status = order["orderStatus"]?.toString() ?: "received"
                val total = (order["grandTotal"] as? Number)?.toDouble() ?: 0.0
                val timestamp = order["createdAt"] as? com.google.firebase.Timestamp
                val invoiceUrl = order["invoiceUrl"]?.toString()
                Card(colors = CardDefaults.cardColors(containerColor = Cream), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selectedOrder = order }) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Forest, modifier = Modifier.size(22.dp)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(orderId, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 14.sp)
                                Text(timestamp?.toDate()?.let { java.text.SimpleDateFormat("dd MMM yyyy · h:mm a", java.util.Locale.getDefault()).format(it) } ?: "Date not available", color = Muted, fontSize = 11.sp)
                            }
                            Text("₹" + String.format("%.2f", total), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.background(PaleGreen, RoundedCornerShape(50.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(status.replace('_', ' ').replaceFirstChar { it.uppercase() }, color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { selectedOrder = order }) {
                                Text("View details", color = Forest, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Forest)
                            }
                            if (!invoiceUrl.isNullOrBlank()) {
                                IconButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) },
                                    modifier = Modifier.size(38.dp).background(PaleGreen, CircleShape)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = tr("Open invoice PDF", LocalAppLanguage.current), tint = Forest)
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
                Text("Previous orders", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Your order history", color = Muted, fontSize = 12.sp)
            }
            Box(Modifier.background(Color(0xFFE8EDE4), RoundedCornerShape(50.dp)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                Text(historicalOrders.size.toString(), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loadingOrders || ordersError != null -> {}
            historicalOrders.isEmpty() -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F4ED))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("No previous orders yet. Orders from earlier days will appear here.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            else -> historicalOrders.forEach { order ->
                val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
                val status = order["orderStatus"]?.toString() ?: "received"
                val total = (order["grandTotal"] as? Number)?.toDouble() ?: 0.0
                val timestamp = order["createdAt"] as? com.google.firebase.Timestamp
                val invoiceUrl = order["invoiceUrl"]?.toString()
                Card(colors = CardDefaults.cardColors(containerColor = Cream), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { selectedOrder = order }) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(Color(0xFFE8EDE4), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Forest, modifier = Modifier.size(22.dp)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(orderId, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 14.sp)
                                Text(timestamp?.toDate()?.let { java.text.SimpleDateFormat("dd MMM yyyy · hh:mm a", java.util.Locale.getDefault()).format(it) } ?: "Date not available", color = Muted, fontSize = 11.sp)
                            }
                            Text("₹" + String.format("%.2f", total), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.background(Color(0xFFE8EDE4), RoundedCornerShape(50.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(status.replace('_', ' ').replaceFirstChar { it.uppercase() }, color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { selectedOrder = order }) {
                                Text("View details", color = Forest, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Forest)
                            }
                            if (!invoiceUrl.isNullOrBlank()) {
                                IconButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) },
                                    modifier = Modifier.size(38.dp).background(PaleGreen, CircleShape)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = tr("Open invoice PDF", LocalAppLanguage.current), tint = Forest)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
        val invoiceNo = order["invoiceNumber"]?.toString() ?: orderId
        val customerName = order["customerName"]?.toString().orEmpty()
        val email = order["email"]?.toString().orEmpty()
        val phone = order["phone"]?.toString().orEmpty()
        val address = order["shippingAddress"]?.toString().orEmpty()
        val status = order["orderStatus"]?.toString() ?: "received"
        val paymentStatus = order["paymentStatus"]?.toString() ?: "not_collected"
        val subtotal = (order["subtotal"] as? Number)?.toDouble() ?: (order["grandTotal"] as? Number)?.toDouble() ?: 0.0
        val grandTotal = (order["grandTotal"] as? Number)?.toDouble() ?: subtotal
        val invoiceItems = (order["items"] as? List<*>)?.mapNotNull { it as? Map<*, *> }.orEmpty()
        val created = (order["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.let { java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(it) } ?: "Not available"

        AlertDialog(
            onDismissRequest = { selectedOrder = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(tr("ORDER INVOICE", LocalAppLanguage.current), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Forest)
                    Text(tr("Sri Lakshmi Ganapathi Ayurveda Nilayam", LocalAppLanguage.current), fontSize = 13.sp, color = Muted)
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Gold)
                    Text("Invoice No: ${'$' }invoiceNo", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Order ID: ${'$' }orderId", color = Ink, fontSize = 12.sp)
                    Text("Order date: ${'$' }created", color = Muted, fontSize = 12.sp)
                    Text(tr("Order status: ", LocalAppLanguage.current) + status.replaceFirstChar { it.uppercase() }, color = Ink)
                    Text(tr("Payment status: ", LocalAppLanguage.current) + paymentStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }, color = Ink)
                    HorizontalDivider(color = Gold.copy(alpha = 0.7f))
                    Text(tr("BILL TO", LocalAppLanguage.current), fontWeight = FontWeight.Bold, color = Forest)
                    Text(customerName.ifBlank { "Customer name not provided" }, color = Ink)
                    if (phone.isNotBlank()) Text("Phone: $phone", color = Ink)
                    if (email.isNotBlank()) Text("Email: $email", color = Ink)
                    if (address.isNotBlank()) Text("Delivery address: $address", color = Ink)
                    HorizontalDivider(color = Gold.copy(alpha = 0.7f))
                    Text(tr("ITEM DETAILS", LocalAppLanguage.current), fontWeight = FontWeight.Bold, color = Forest)
                    if (invoiceItems.isEmpty()) {
                        Text(tr("No item details were saved with this order.", LocalAppLanguage.current), color = Muted)
                    } else {
                        invoiceItems.forEachIndexed { index, item ->
                            val name = item["productName"]?.toString() ?: item["name"]?.toString() ?: "Item \${index + 1}"
                            val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                            val unitPrice = (item["unitPrice"] as? Number)?.toDouble() ?: (item["price"] as? Number)?.toDouble() ?: 0.0
                            val lineTotal = (item["lineTotal"] as? Number)?.toDouble() ?: (unitPrice * quantity)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Cream),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, color = Ink)
                                    Text("$quantity × ₹" + String.format("%.2f", unitPrice) + "     Line total: ₹" + String.format("%.2f", lineTotal), color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Gold.copy(alpha = 0.7f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Subtotal", LocalAppLanguage.current), color = Ink)
                        Text(tr("₹", LocalAppLanguage.current) + String.format("%.2f", subtotal), color = Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Delivery / tax", LocalAppLanguage.current), color = Ink)
                        Text(tr("₹0.00", LocalAppLanguage.current), color = Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("GRAND TOTAL", LocalAppLanguage.current), fontWeight = FontWeight.ExtraBold, color = Forest)
                        Text(tr("₹", LocalAppLanguage.current) + String.format("%.2f", grandTotal), fontWeight = FontWeight.ExtraBold, color = Forest)
                    }
                    Text(tr("Payment has not been collected unless the status above says otherwise.", LocalAppLanguage.current), color = Muted, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { selectedOrder = null }) { Text(tr("Close", LocalAppLanguage.current)) } },
            dismissButton = {
                val invoiceUrl = order["invoiceUrl"]?.toString()
                if (!invoiceUrl.isNullOrBlank()) {
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) }) {
                        Text(tr("Open PDF", LocalAppLanguage.current))
                    }
                }
            },
            containerColor = Cream
        )
    }

}

@Composable
private fun DiseasesPage(modifier: Modifier, diseases: List<String>, products: List<Product>, loading: Boolean, error: String?) {
    var query by remember { mutableStateOf("") }
    var selectedDisease by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var detailLoading by remember { mutableStateOf(false) }
    var diseaseDescription by remember { mutableStateOf("") }
    var diseaseImageUrl by remember { mutableStateOf("") }
    var diseasePrecautions by remember { mutableStateOf("") }
    var suggestedMedicineNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val relatedMedicines = remember(selectedDisease, products) {
        val chosen = selectedDisease?.takeIf { it != "All Diseases" }
        if (chosen == null) emptyList() else products.filter { product ->
            product.cures.split(",", ";", "|", "\n").any { it.trim().equals(chosen, ignoreCase = true) } ||
                product.cures.contains(chosen, ignoreCase = true)
        }
    }

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
                    suggestedMedicineNames = listOf("suggested_medicines", "suggestedMedicines", "recommended_medicines", "medicines")
                        .firstNotNullOfOrNull { key ->
                            val value = data?.get(key)
                            when (value) {
                                is String -> value.split(",", ";", "|", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                is List<*> -> value.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                                else -> null
                            }
                        }.orEmpty()
                    detailLoading = false
                }
                .addOnFailureListener { detailLoading = false }
        } else {
            detailLoading = false
        }
    }

    val matches = remember(diseases, query) {
        if (query.isBlank()) diseases else diseases.filter { it.contains(query.trim(), ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp)) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Forest),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("SRI LAKSHMI GANAPATHI", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tr("Diseases", LocalAppLanguage.current), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFFFFE7A3)
                        ) {
                            Text(
                                "${diseases.size} available",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color(0xFF762A24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(tr("Find a disease from the available list.", LocalAppLanguage.current), color = PaleGreen, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selectedDisease = null
                        dropdownExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, Brush.horizontalGradient(listOf(Gold, Forest.copy(alpha = 0.78f), Gold)), RoundedCornerShape(18.dp)),
                    placeholder = { Text(tr("Select disease", LocalAppLanguage.current), fontSize = 18.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Forest) },
                    trailingIcon = {
                        IconButton(onClick = {
                            // Reopen the complete disease list even when a disease is already selected.
                            query = ""
                            dropdownExpanded = true
                        }) {
                            Icon(
                                if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Show all disease options",
                                tint = Forest
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent,
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream
                    )
                )
                DropdownMenu(
                    expanded = dropdownExpanded && matches.isNotEmpty(),
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.94f)
                        .heightIn(max = 280.dp)
                        .background(Cream, RoundedCornerShape(14.dp))
                        .border(1.dp, Gold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                ) {
                    matches.forEach { disease ->
                        DropdownMenuItem(
                            colors = MenuDefaults.itemColors(textColor = Ink),
                            text = { Text(disease, color = Ink, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Text(when (disease.trim().lowercase()) { "fever" -> "🤒"; "cough" -> "🦠"; "flu" -> "🫁"; "obesity" -> "⚖️"; "tuberculosis", "tuberculosis (tb)", "tb" -> "🫁"; else -> "🌿" }, fontSize = 20.sp) },
                            onClick = {
                                selectedDisease = disease
                                query = disease
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (selectedDisease != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DISEASE DETAILS", color = Leaf, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                    Text(selectedDisease!!, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = {
                    selectedDisease = null
                    query = ""
                    dropdownExpanded = false
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close disease details and clear search", tint = Forest)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Cream),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (diseaseImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = diseaseImageUrl,
                            contentDescription = selectedDisease,
                            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(PaleGreen, Cream)), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Forest, modifier = Modifier.size(76.dp))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    DetailSectionHeading(Icons.Default.Info, "Description")
                    Spacer(Modifier.height(7.dp))
                    Text(
                        when {
                            detailLoading -> "Loading disease information…"
                            diseaseDescription.isNotBlank() -> diseaseDescription
                            else -> tr("Information not available.", LocalAppLanguage.current)
                        },
                        color = Muted, fontSize = 14.sp, lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    DetailSectionHeading(Icons.Default.MedicalServices, "Suggested medicines")
                    Spacer(Modifier.height(8.dp))
                    val displayedSuggestions = if (suggestedMedicineNames.isNotEmpty()) suggestedMedicineNames else relatedMedicines.map { it.name }
                    if (detailLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Forest)
                    } else if (displayedSuggestions.isEmpty()) {
                        Text(tr("Information not available.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                    } else {
                        displayedSuggestions.forEach { medicine ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(medicine, color = Ink, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    DetailSectionHeading(Icons.Default.HealthAndSafety, "Precautions")
                    Spacer(Modifier.height(7.dp))
                    if (diseasePrecautions.isBlank()) {
                        Text(tr("Information not available.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                    } else {
                        diseasePrecautions.split("\n").filter { it.isNotBlank() }.forEach { precaution ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("•", color = Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(precaution, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(tr("Use medicines only with guidance from a qualified healthcare professional.", LocalAppLanguage.current), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            }
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
private fun CategoriesPage(modifier: Modifier, products: List<Product>, cart: Map<String, Int>, productRatings: Map<String, ProductRatingSummary>, productLikes: Map<String, ProductLikeSummary>, onToggleLike: (Product) -> Unit, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onCategoryChange: (String) -> Unit, onHome: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val selectedProducts = products.filter { selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true) }
    // Category tiles are sourced directly from the Firestore categories collection.
    var backendCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    backendCategories = snapshot.documents.mapNotNull { doc ->
                        (doc.getString("category_name") ?: doc.getString("name") ?: doc.id)
                            .trim().takeIf { it.isNotBlank() }
                    }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                }
            }
        onDispose { registration.remove() }
    }
    // Merge category documents with categories present on database-backed products.
    // Product categories such as "Rasayogas" remain available even if the categories
    // collection does not contain a matching document.
    val categoriesFromBackend = remember(backendCategories, products) {
        (listOf("All") + backendCategories + products.map { it.category.trim() }
            .filter { it.isNotBlank() })
            .distinctBy { it.lowercase() }
    }
    val categoryItems = categoriesFromBackend.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    val categoryRows = categoryItems.chunked(2)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        AnimatedVisibility(
            visible = selectedCategory == null,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Forest)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("SRI LAKSHMI GANAPATHI", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(7.dp))
                Text(tr("Explore Categories", LocalAppLanguage.current), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr("Discover Ayurvedic products by collection.", LocalAppLanguage.current),
                    color = PaleGreen,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("${products.size} products", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(1.dp).height(16.dp).background(Color(0x66FFFFFF)))
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Default.Category, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("${categoriesFromBackend.size} collections", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Brush.horizontalGradient(listOf(Gold, Forest.copy(alpha = 0.78f), Gold)), RoundedCornerShape(18.dp)),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text(tr("Search categories...", LocalAppLanguage.current)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Forest) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = tr("Clear search", LocalAppLanguage.current))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Forest,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                cursorColor = Gold
            )
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.GridView, contentDescription = null, tint = Forest, modifier = Modifier.size(22.dp))
            Text(tr("Shop by category", LocalAppLanguage.current), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("${categoryItems.size} shown", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        if (categoryItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Cream)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔎", fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("No categories found", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(tr("Try another search term.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
                }
            }
        } else {
            categoryRows.forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { item ->
                        val emoji = when (item.trim().lowercase()) {
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
                        }
                        val count = if (item.equals("All", ignoreCase = true)) products.size else products.count { it.category.equals(item, ignoreCase = true) }
                        Card(
                            modifier = Modifier.weight(1f).padding(bottom = 2.dp).clickable {
                                selectedCategory = item
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Cream),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Box(
                                        Modifier.size(48.dp).background(
                                            if (item == "All") Color(0xFFF0DFC0) else PaleGreen,
                                            CircleShape
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) { Text(emoji, fontSize = 24.sp) }
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9A7845), modifier = Modifier.size(21.dp))
                                }
                                Spacer(Modifier.height(13.dp))
                                Text(tr(item, LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "$count " + tr("products", LocalAppLanguage.current),
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(if (item == "All") Gold else Color(0xFFDCE8D7)))
                            }
                        }
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
            }
        }

        AnimatedVisibility(
            visible = selectedCategory != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(Modifier.fillMaxWidth()) {
        if (selectedCategory != null) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(selectedCategory!!, LocalAppLanguage.current), color = Forest, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.background(Color(0xFFFFE7A3), RoundedCornerShape(50))
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text("${selectedProducts.size}", color = Color(0xFF762A24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { selectedCategory = null }) { Text("← Categories", color = Forest, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            if (selectedProducts.isEmpty()) {
                Text("No products found in this category.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                selectedProducts.forEach { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (product.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)).background(PaleGreen),
                                    contentAlignment = Alignment.Center
                                ) { Text(product.emoji.ifBlank { "🌿" }, fontSize = 27.sp) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    localizedProductName(product.name, LocalAppLanguage.current),
                                    color = Ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val packSize = product.subtitle.ifBlank { product.quantity }
                                val cartQuantity = cart[product.id] ?: 0
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (packSize.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFFEBC2),
                                            contentColor = Ink
                                        ) {
                                            Row(
                                                Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Inventory2, contentDescription = "Pack size", modifier = Modifier.size(14.dp))
                                                Text(packSize.replace(" ", ""), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    ProductRatingLine(product, productRatings[product.id] ?: ProductRatingSummary(), compact = true)
                                    Box(
                                        modifier = Modifier.width(54.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        ProductLikeControl(product, productLikes[product.id] ?: ProductLikeSummary(), onToggleLike)
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        if (product.price > 0.0) "₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price) else "Price not set",
                                        color = Forest,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier.width(116.dp).height(38.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (cartQuantity == 0) {
                                            Button(
                                                onClick = { onAdd(product.id) },
                                                enabled = product.price > 0.0,
                                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Forest),
                                                shape = RoundedCornerShape(9.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Add to cart", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxSize(),
                                                shape = RoundedCornerShape(10.dp),
                                                color = Cream,
                                                border = BorderStroke(1.dp, Gold)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    IconButton(
                                                        onClick = { onRemove(product.id) },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", tint = Forest, modifier = Modifier.size(16.dp))
                                                    }
                                                    Text(
                                                        cartQuantity.toString(),
                                                        color = Ink,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.widthIn(min = 16.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    IconButton(
                                                        onClick = { onAdd(product.id) },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Increase quantity", tint = Forest, modifier = Modifier.size(16.dp))
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
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tr("Select a category to view its products.", LocalAppLanguage.current),
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
    }            }
        }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePage(modifier: Modifier, onSignOut: () -> Unit, appLanguage: String, onLanguageChange: (String) -> Unit) {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    val context = LocalContext.current
    var firstName by remember(uid) { mutableStateOf("") }
    var lastName by remember(uid) { mutableStateOf("") }
    var nickname by remember(uid) { mutableStateOf("") }
    var gender by remember(uid) { mutableStateOf("") }
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
    var profileSection by remember { mutableStateOf("Profile") }
    var showAddressForm by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var addresses by remember(uid) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var addressLabel by remember { mutableStateOf("Home") }
    var recipientName by remember { mutableStateOf("") }
    var recipientCountry by remember { mutableStateOf(countryCodes.first { it.first == "IN" }) }
    var recipientCountryMenuExpanded by remember { mutableStateOf(false) }
    var recipientCountryCode by remember { mutableStateOf("+91") }
    var recipientPhone by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var addressSaving by remember { mutableStateOf(false) }
    var addressMessage by remember { mutableStateOf<String?>(null) }

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
                        gender = snapshot.getString("gender").orEmpty()
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
                        snapshot.getString("languagePreference")?.let(onLanguageChange)
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
    DisposableEffect(uid) {
        var addressRegistration: ListenerRegistration? = null
        if (uid != null) {
            addressRegistration = FirebaseFirestore.getInstance().collection("addresses")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, _ ->
                    addresses = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.plus("id" to doc.id)
                    }.orEmpty()
                }
        }
        onDispose { addressRegistration?.remove() }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Forest),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("SRI LAKSHMI GANAPATHI", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Your account", LocalAppLanguage.current), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            tr(
                                if (profileSection == "Addresses") "Manage your delivery addresses." else "Manage your personal details.",
                                LocalAppLanguage.current
                            ),
                            color = PaleGreen,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(48.dp).background(Color(0x26FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().background(Color(0xFFF1E5D0), RoundedCornerShape(14.dp)).padding(4.dp)) {
            listOf("Profile", "Addresses").forEach { section ->
                val selected = profileSection == section
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Forest else Color.Transparent)
                        .clickable { profileSection = section; showAddressForm = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tr(section, LocalAppLanguage.current), color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        if (profileSection == "Profile") {
            Card(colors = CardDefaults.cardColors(containerColor = Cream), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Forest, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { nickname.ifBlank { "SLGAN Customer" } },
                            color = Ink, fontWeight = FontWeight.Bold
                        )
                        Text(mobileNumber.ifBlank { user?.email.orEmpty() }, color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = firstName, onValueChange = { firstName = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("First name", LocalAppLanguage.current)) }, singleLine = true, leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = Forest) })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Last name", LocalAppLanguage.current)) }, singleLine = true, leadingIcon = { Icon(Icons.Default.Badge, null, tint = Forest) })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = nickname, onValueChange = { nickname = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Nickname", LocalAppLanguage.current)) }, singleLine = true, leadingIcon = { Icon(Icons.Default.Face, null, tint = Forest) })
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedTextField(
                        value = "${selectedCountry.second}  ${selectedCountry.third}",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.width(140.dp).clickable { countryMenuExpanded = true },
                        label = { Text("Country", maxLines = 1, softWrap = false) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country", tint = Forest, modifier = Modifier.size(18.dp)) },
                        singleLine = true
                    )
                    Box(Modifier.matchParentSize().clickable { countryMenuExpanded = true })
                    DropdownMenu(expanded = countryMenuExpanded, onDismissRequest = { countryMenuExpanded = false }) {
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
                    onValueChange = { input -> mobileNumber = input.filter(Char::isDigit).take(15); saveMessage = null },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr("Mobile number", LocalAppLanguage.current), maxLines = 1, softWrap = false) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = Forest) }
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
                    label = { Text("Date of birth") },
                    placeholder = { Text("Select date of birth") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Forest) },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Choose date", tint = Forest) }
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
                            colors = ButtonDefaults.textButtonColors(contentColor = Forest)
                        ) { Text("Select") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDateOfBirthPicker = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Forest)
                        ) { Text("Cancel") }
                    },
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            containerColor = Cream,
                            titleContentColor = Forest,
                            headlineContentColor = Forest,
                            weekdayContentColor = Muted,
                            subheadContentColor = Forest,
                            navigationContentColor = Forest,
                            yearContentColor = Ink,
                            currentYearContentColor = Forest,
                            selectedYearContainerColor = Forest,
                            selectedYearContentColor = Color.White,
                            dayContentColor = Ink,
                            disabledDayContentColor = Muted,
                            selectedDayContainerColor = Forest,
                            selectedDayContentColor = Color.White,
                            todayContentColor = Forest,
                            todayDateBorderColor = Gold
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = gender, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Gender", LocalAppLanguage.current)) }, placeholder = { Text(tr("Select gender", LocalAppLanguage.current)) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, leadingIcon = { Icon(Icons.Default.Wc, null, tint = Forest) })
                Box(Modifier.matchParentSize().clickable { genderMenuExpanded = true })
                DropdownMenu(expanded = genderMenuExpanded, onDismissRequest = { genderMenuExpanded = false }) {
                    listOf("Female", "Male", "Non-binary", "Prefer not to say").forEach { option ->
                        DropdownMenuItem(text = { Text(tr(option, LocalAppLanguage.current)) }, onClick = { gender = option; genderMenuExpanded = false; saveMessage = null })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(tr("Language", appLanguage), fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tr(appLanguage, appLanguage),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Select language", appLanguage)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    leadingIcon = { Icon(Icons.Default.Language, null, tint = Forest) }
                )
                Box(Modifier.matchParentSize().clickable { languageMenuExpanded = true })
                DropdownMenu(expanded = languageMenuExpanded, onDismissRequest = { languageMenuExpanded = false }) {
                    listOf("English", "Telugu", "Hindi").forEach { option ->
                        DropdownMenuItem(text = { Text(tr(option, appLanguage)) }, onClick = {
                            onLanguageChange(option)
                            languageMenuExpanded = false
                            languageMessage = null
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (uid == null) Toast.makeText(context, "Please sign in again to save your profile.", Toast.LENGTH_SHORT).show()
                    else {
                        languageSaving = true
                        languageMessage = null
                        FirebaseData.saveCustomerProfile(uid, mapOf(
                            "languagePreference" to appLanguage,
                            "updatedAt" to System.currentTimeMillis()
                        )) { error ->
                            languageSaving = false
                            Toast.makeText(context, if (error == null) "Language preference saved." else "Couldn't save language. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !languageSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (languageSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Save language preference", appLanguage))
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val currentUid = uid
                    when {
                        firstName.isBlank() && mobileNumber.isBlank() ->
                            saveMessage = "First name and mobile number are required."
                        firstName.isBlank() ->
                            saveMessage = "First name is required."
                        mobileNumber.isBlank() ->
                            saveMessage = "Mobile number is required."
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isLoading) "Loading profile…" else if (isSaving) "Saving…" else "Save profile", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text(tr("Sign out", LocalAppLanguage.current))
            }
        } else if (showAddressForm) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAddressForm = false; addressMessage = null }) { Icon(Icons.Default.ArrowBack, "Back", tint = Forest) }
                Column(Modifier.weight(1f)) {
                    Text(tr("Add address", LocalAppLanguage.current), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(tr("Enter delivery details below.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showMapPicker = true },
                    modifier = Modifier.widthIn(min = 96.dp, max = 112.dp).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Gold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Forest, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Show map", fontSize = 12.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (showMapPicker) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showMapPicker = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.94f),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                }
                                IconButton(onClick = { showMapPicker = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close map", tint = Forest)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            AndroidView(
                    modifier = Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(14.dp)),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?,
                                    callback: GeolocationPermissions.Callback?
                                ) {
                                    val hasFine = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    callback?.invoke(origin, hasFine || hasCoarse, false)
                                }
                            }
                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun selectAddress(line1: String, line2: String, landmarkValue: String, cityValue: String, stateValue: String, pinValue: String) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        addressLine1 = line1
                                        addressLine2 = line2
                                        landmark = landmarkValue
                                        city = cityValue
                                        state = stateValue
                                        pincode = pinValue
                                        addressMessage = "Address filled from map. Please verify before saving."
                                    }
                                }
                            }, "AddressBridge")
                            loadDataWithBaseURL("https://www.openstreetmap.org/", """
                                <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
                                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                                <style>
                                *{box-sizing:border-box}html,body{width:100%;margin:0;padding:0;font-family:Arial,sans-serif;background:#fbf6eb;color:#30251d}
                                body{padding:10px}
                                .search-panel{padding:14px;background:linear-gradient(145deg,#fffdf7,#f3e8d5);border:1px solid #dfc58e;border-radius:18px;box-shadow:0 5px 16px #4b2b1718}
                                .heading{display:flex;align-items:center;gap:7px;font-size:11px;font-weight:800;letter-spacing:1.25px;color:#762b35;margin:0 0 10px}
                                .heading:before{content:'📍';font-size:14px}
                                #locate{display:block;width:100%;margin:0 0 8px;padding:11px 12px;border:0;border-radius:12px;background:#762b35;color:#fff8e9;font-size:14px;font-weight:700;text-align:left}#locate:active{opacity:.8}
                                 #search{display:block;width:100%;padding:13px 14px;border:1px solid #d5c5a6;border-radius:12px;background:#fffefa;color:#30251d;font-size:15px;outline:none;box-shadow:inset 0 1px 2px #3e28100a}
                                #search::placeholder{color:#8d8170}#search:focus{border-color:#9a6b2e;box-shadow:0 0 0 3px #c69a4b25}
                                #results{max-height:100px;overflow:auto;background:#fffefa;border:1px solid #e7dcc7;border-radius:12px;margin-top:8px}
                                .result{padding:12px;border-bottom:1px solid #eee4d2;font-size:13px;line-height:1.45;color:#30251d;cursor:pointer}
                                .result:last-child{border-bottom:0}.result:active{background:#f5e8ce}
                                #map-frame{position:relative;margin-top:12px;padding:4px;border:1px solid #b88b43;border-radius:18px;background:linear-gradient(135deg,#f0d18a,#8c542d);box-shadow:0 7px 18px #4b2b1724;overflow:hidden}
                                #map{height:220px;width:100%;border-radius:14px;overflow:hidden;background:#e9e3d6}
                                .map-caption{display:flex;align-items:center;justify-content:space-between;padding:9px 5px 3px;font-size:11px;color:#5b4939}
                                .map-caption strong{color:#762b35;font-size:11px}
                                .hint{font-size:11px;line-height:1.5;color:#786957;padding:9px 3px 1px}
                                .leaflet-control-zoom{border:1px solid #b88b43!important;border-radius:10px!important;overflow:hidden}
                                .leaflet-control-zoom a{background:#fffaf0!important;color:#762b35!important;border-color:#e4d2ad!important}
                                .leaflet-control-attribution{background:#fffaf0e8!important;color:#665747!important;font-size:9px!important}
                                .leaflet-control-attribution a{color:#762b35!important}
                                .leaflet-container{font:12px Arial,sans-serif}
                                </style></head>
                                <body><div class="search-panel"><div class="heading">FIND DELIVERY LOCATION</div><button id="locate" type="button" onclick="locateMe()">◎ Use my current location</button><input id="search" autocomplete="off" placeholder="Search locality, street, landmark or PIN code"><div id="results"></div></div><div id="map-frame"><div id="map"></div></div>
                                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script><script>
                                const map=L.map('map',{zoomControl:true,scrollWheelZoom:true}).setView([17.385,78.4867],12);
                                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap contributors',maxZoom:19}).addTo(map);
                                const pinIcon=L.divIcon({className:'slgan-pin',html:'<div style="width:24px;height:24px;background:#762b35;border:3px solid #f0c56a;border-radius:50% 50% 50% 0;transform:rotate(-45deg);box-shadow:0 3px 8px #0005;position:relative"><span style="position:absolute;width:6px;height:6px;background:#fff2d0;border-radius:50%;left:6px;top:6px"></span></div>',iconSize:[28,34],iconAnchor:[12,30]});
                                let marker=null;let currentMarker=null;let currentCircle=null;
                                 function locateMe(){const btn=document.getElementById('locate');if(!navigator.geolocation){btn.textContent='Location is not supported on this device';return}btn.textContent='Finding your current location…';navigator.geolocation.getCurrentPosition(async pos=>{const lat=pos.coords.latitude,lon=pos.coords.longitude,ll=[lat,lon];map.setView(ll,17);if(currentMarker)map.removeLayer(currentMarker);if(currentCircle)map.removeLayer(currentCircle);currentMarker=L.circleMarker(ll,{radius:9,color:'#ffffff',weight:3,fillColor:'#1687ff',fillOpacity:1}).addTo(map).bindPopup('Your current location').openPopup();currentCircle=L.circle(ll,{radius:Math.max(pos.coords.accuracy||30,25),color:'#1687ff',weight:1,fillColor:'#1687ff',fillOpacity:.12}).addTo(map);if(marker)map.removeLayer(marker);marker=L.marker(ll,{icon:pinIcon,draggable:true}).addTo(map);btn.textContent='✓ Current location found';try{const r=await fetch('https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat='+lat+'&lon='+lon);if(r.ok){const d=await r.json();applyAddress(d.address||{});}}catch(e){}marker.on('dragend',async()=>{const p=marker.getLatLng();map.setView(p,Math.max(map.getZoom(),16));try{const r=await fetch('https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat='+p.lat+'&lon='+p.lng);if(r.ok){const d=await r.json();applyAddress(d.address||{});}}catch(e){}})},err=>{btn.textContent=err.code===1?'Allow location permission to find you':'Could not get location — tap to retry';},{enableHighAccuracy:true,timeout:15000,maximumAge:60000})}
                                async function searchPlaces(){const q=document.getElementById('search').value.trim();if(q.length<3){document.getElementById('results').innerHTML='';return}const box=document.getElementById('results');box.innerHTML='<div class="result">Searching…</div>';try{const query=/\\b(india|telangana|andhra pradesh|hyderabad)\\b/i.test(q)?q:(q+', India');const response=await fetch('https://photon.komoot.io/api/?limit=8&lat=17.385&lon=78.4867&q='+encodeURIComponent(query));if(!response.ok)throw new Error('Search unavailable');const data=await response.json();box.innerHTML='';data.features.forEach(p=>{const row=document.createElement('div');row.className='result';row.textContent=[p.properties.name,p.properties.street,p.properties.city||p.properties.town||p.properties.village,p.properties.state,p.properties.postcode].filter(Boolean).join(', ')||'Location result';row.onclick=()=>choose(p);box.appendChild(row)});if(!data.features.length)box.innerHTML='<div class="result">No results found. Try a locality or PIN code.</div>'}catch(e){box.innerHTML='<div class="result">Search unavailable. Check your internet and try again.</div>'}}
                                let searchTimer;document.getElementById('search').addEventListener('input',()=>{clearTimeout(searchTimer);searchTimer=setTimeout(searchPlaces,700)});
                                function choose(p){document.getElementById('results').innerHTML='';const lon=Number(p.geometry.coordinates[0]),lat=Number(p.geometry.coordinates[1]);map.setView([lat,lon],16);if(marker)map.removeLayer(marker);marker=L.marker([lat,lon],{icon:pinIcon,draggable:true}).addTo(map);applyAddress(p.properties||{});marker.on('dragend',()=>{const ll=marker.getLatLng();updateAddress(ll.lat,ll.lng)});}
                                async function updateAddress(lat,lon){try{const r=await fetch('https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat='+lat+'&lon='+lon);if(r.ok){const d=await r.json();applyAddress(d.address||{});}}catch(e){}}
                                map.on('click',e=>{const ll=e.latlng;if(marker)map.removeLayer(marker);marker=L.marker(ll,{icon:pinIcon,draggable:true}).addTo(map);updateAddress(ll.lat,ll.lng);marker.on('dragend',()=>{const p=marker.getLatLng();updateAddress(p.lat,p.lng)});});
                                function applyAddress(a){const line1=[a.house_number||a.housenumber,a.road||a.street].filter(Boolean).join(' ');const city=a.city||a.town||a.village||a.municipality||'';const locality=a.neighbourhood||a.suburb||a.quarter||a.residential||a.hamlet||'';const district=a.district||a.county||a.state_district||'';const line2=locality&&locality.toLowerCase()!==city.toLowerCase()?locality:(district&&district.toLowerCase()!==city.toLowerCase()?district:'');const st=a.state||'';const pin=a.postcode||'';const lm=a.amenity||a.name||'';AddressBridge.selectAddress(line1||a.name||'',line2,lm,city,st,pin) }
                                function rowLabel(a){return [a.name,a.street,a.city||a.town||a.village,a.state,a.postcode].filter(Boolean).join(', ')}
                                 window.addEventListener('load',()=>setTimeout(locateMe,500));
                                </script></body></html>
                            """.trimIndent(), "text/html", "UTF-8", null)
                        }
                    }
                )
                            Spacer(Modifier.height(8.dp))
                            Text("Drag the pin to fine-tune the location. The address fields will update.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { showMapPicker = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                                Text("Use this location")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            } else {
                Spacer(Modifier.height(14.dp))
            }
            Text(tr("Address type", LocalAppLanguage.current), color = Ink, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Home", "Work", "Other").forEach { label ->
                    FilterChip(selected = addressLabel == label, onClick = { addressLabel = label }, label = { Text(tr(label, LocalAppLanguage.current)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Forest, selectedLabelColor = Color.White))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = recipientName, onValueChange = { recipientName = it; addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Recipient name *", LocalAppLanguage.current)) }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = recipientCountryMenuExpanded,
                    onExpandedChange = { recipientCountryMenuExpanded = !recipientCountryMenuExpanded },
                    modifier = Modifier.weight(0.38f)
                ) {
                    OutlinedTextField(
                        value = "${recipientCountry.second} ${recipientCountry.third}",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(),
                        label = { Text("Country code") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipientCountryMenuExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = recipientCountryMenuExpanded,
                        onDismissRequest = { recipientCountryMenuExpanded = false }
                    ) {
                        countryCodes.forEach { country ->
                            DropdownMenuItem(
                                text = { Text("${country.second}  ${country.first}  ${country.third}") },
                                onClick = {
                                    recipientCountry = country
                                    recipientCountryCode = country.third
                                    recipientCountryMenuExpanded = false
                                    addressMessage = null
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = recipientPhone,
                    onValueChange = { recipientPhone = it.filter(Char::isDigit).take(15); addressMessage = null },
                    modifier = Modifier.weight(0.62f),
                    label = { Text(tr("Mobile number *", LocalAppLanguage.current)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = addressLine1, onValueChange = { addressLine1 = it; addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("House / flat, building, street *", LocalAppLanguage.current)) })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = addressLine2, onValueChange = { addressLine2 = it; addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Area / village / locality", LocalAppLanguage.current)) })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = landmark, onValueChange = { landmark = it; addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Landmark (optional)", LocalAppLanguage.current)) })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = city, onValueChange = { city = it; addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("City / town *", LocalAppLanguage.current)) }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = state, onValueChange = { state = it; addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("State *", LocalAppLanguage.current)) }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = pincode, onValueChange = { pincode = it.filter(Char::isDigit).take(6); addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("PIN code *", LocalAppLanguage.current)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            if (addressMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(addressMessage!!, color = if (addressMessage!!.startsWith("Address saved")) Forest else Color(0xFFB3261E), fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (uid == null) addressMessage = "Please sign in again."
                    else if (recipientName.isBlank() || recipientPhone.isBlank() || addressLine1.isBlank() || city.isBlank() || state.isBlank() || pincode.length != 6) {
                        addressMessage = "Please complete all required fields and enter a 6-digit PIN code."
                    } else {
                        addressSaving = true
                        addressMessage = null
                        val newAddress = mapOf<String, Any>(
                            "id" to System.currentTimeMillis().toString(), "label" to addressLabel,
                            "recipientName" to recipientName.trim(), "phone" to (recipientCountryCode.trim() + recipientPhone.trim()), "countryCode" to recipientCountryCode.trim(),
                            "addressLine1" to addressLine1.trim(), "addressLine2" to addressLine2.trim(),
                            "landmark" to landmark.trim(), "city" to city.trim(), "state" to state.trim(),
                            "pincode" to pincode.trim(), "createdAt" to System.currentTimeMillis()
                        )
                        val addressId = newAddress["id"].toString()
                        val addressRecord = newAddress + ("userId" to uid)
                        FirebaseFirestore.getInstance().collection("addresses").document(addressId)
                            .set(addressRecord)
                            .addOnSuccessListener {
                                addressSaving = false
                                addressMessage = "Address saved successfully."
                                showAddressForm = false
                                recipientName = ""; recipientCountry = countryCodes.first { it.first == "IN" }; recipientCountryCode = "+91"; recipientPhone = ""; addressLine1 = ""; addressLine2 = ""
                                landmark = ""; city = ""; state = ""; pincode = ""; addressLabel = "Home"
                                profileSection = "Addresses"
                            }
                            .addOnFailureListener {
                                addressSaving = false
                                addressMessage = "Couldn't save address. Please check your connection and try again."
                            }
                    }
                },
                enabled = !addressSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                if (addressSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp)); Text(if (addressSaving) "Saving address…" else "Save address", fontWeight = FontWeight.Bold)
            }
        } else {
            if (addresses.isEmpty()) {
                Spacer(Modifier.height(44.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(PaleGreen, CircleShape)
                            .border(1.dp, Gold.copy(alpha = 0.75f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Forest, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("No addresses yet", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap Add address to save your first delivery location.",
                        color = Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { showAddressForm = true; addressMessage = null },
                        modifier = Modifier.fillMaxWidth(0.78f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Forest),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("+ Address", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr("Saved addresses", LocalAppLanguage.current),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            tr("Keep delivery locations for future orders.", LocalAppLanguage.current),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { showAddressForm = true; addressMessage = null },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Forest),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add address", modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                addresses.forEach { address ->
                    val addressLabelText = address["label"]?.toString()?.takeIf(String::isNotBlank) ?: "Address"
                    val fullAddress = listOf(
                        address["addressLine1"], address["addressLine2"],
                        address["landmark"], address["city"], address["state"], address["pincode"]
                    ).mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }.joinToString(", ")
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.38f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(PaleGreen, RoundedCornerShape(14.dp))
                                        .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = Forest, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        addressLabelText.uppercase(Locale.getDefault()),
                                        color = Forest,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        address["recipientName"]?.toString().orEmpty(),
                                        color = Ink,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(Icons.Default.CheckCircle, null, tint = Gold, modifier = Modifier.size(21.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Gold.copy(alpha = 0.28f), thickness = 1.dp)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Call, null, tint = Muted, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    address["phone"]?.toString().orEmpty(),
                                    color = Muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(9.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Home, null, tint = Forest, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    fullAddress,
                                    color = Ink,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
            if (addressMessage != null) {
                Spacer(Modifier.height(8.dp)); Text(addressMessage!!, color = Forest, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AppLoader(message: String, compact: Boolean = false) {
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
                .background(Cream, CircleShape)
                .border(2.dp, Gold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌿",
                fontSize = if (compact) 25.sp else 39.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = leafRotation
                    scaleX = leafScale
                    scaleY = leafScale
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(message, color = Forest, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.SemiBold)
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
