package com.pyara.slgan

import android.os.Bundle
import android.view.WindowManager
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Forest = Color(0xFF762A24)
private val Leaf = Color(0xFF8A4B2A)
private val Cream = Color(0xFFFFF7E8)
private val PaleGreen = Color(0xFFF5E5C8)
private val Ink = Color(0xFF38251F)
private val Muted = Color(0xFF806D60)
private val Gold = Color(0xFFD9A441)

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
        "All" to "అన్నీ", "Lehyas" to "లేహ్యాలు", "Grutas" to "ఘృతాలు", "Pills" to "మాత్రలు",
        "Tailas" to "తైలాలు", "Churnas" to "చూర్ణాలు", "Rasayanas" to "రసాయనాలు", "Bhasmas" to "భస్మాలు",
        "Language" to "భాష", "Select language" to "భాషను ఎంచుకోండి", "Save language preference" to "భాష ప్రాధాన్యతను సేవ్ చేయండి",
        "Email address" to "ఇమెయిల్ చిరునామా", "Password (at least 6 characters)" to "పాస్‌వర్డ్ (కనీసం 6 అక్షరాలు)", "Secure email and password sign-in" to "ఇమెయిల్ మరియు పాస్‌వర్డ్‌తో సురక్షిత లాగిన్",
        "Explore thoughtful herbal essentials." to "సహజ మూలికా ఉత్పత్తులను అన్వేషించండి.", "ROOTED IN NATURE" to "ప్రకృతిలో వేళ్లూనుకున్నది", "Everyday wellness,\\nwith a herbal touch." to "ప్రతిరోజూ ఆరోగ్యం,\\nమూలికలతో.", "Explore collection  →" to "ఉత్పత్తులను చూడండి  →",
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
        "No addresses yet" to "अभी कोई पता नहीं", "All" to "सभी", "Lehyas" to "लेह्य", "Grutas" to "घृत",
        "Pills" to "गोलियाँ", "Tailas" to "तेल", "Churnas" to "चूर्ण", "Rasayanas" to "रसायन", "Bhasmas" to "भस्म",
        "Language" to "भाषा", "Select language" to "भाषा चुनें", "Save language preference" to "भाषा प्राथमिकता सहेजें",
        "Email address" to "ईमेल पता", "Password (at least 6 characters)" to "पासवर्ड (कम से कम 6 अक्षर)", "Secure email and password sign-in" to "ईमेल और पासवर्ड से सुरक्षित साइन-इन",
        "Explore thoughtful herbal essentials." to "चुनिंदा हर्बल उत्पाद देखें।", "ROOTED IN NATURE" to "प्रकृति से जुड़ा", "Everyday wellness,\\nwith a herbal touch." to "हर दिन स्वास्थ्य,\\nजड़ी-बूटियों के साथ।", "Explore collection  →" to "उत्पाद देखें  →",
        "Search products" to "उत्पाद खोजें", "Shop by category" to "श्रेणी के अनुसार खरीदें", "Could not load products: ${'$' }productsError" to "उत्पाद लोड नहीं हो सके: ${'$' }productsError",
        "Made with care for your everyday rituals." to "आपकी रोज़मर्रा की ज़रूरतों के लिए सावधानी से बनाया गया।", "Your cart" to "आपकी कार्ट", "Subtotal" to "उप-योग", "Checkout" to "चेकआउट", "Demo only: checkout and payments are not connected." to "यह केवल डेमो है: चेकआउट और भुगतान जुड़े नहीं हैं।", "Find the collection for your routine." to "अपनी दिनचर्या के लिए उत्पाद खोजें।",
        "Language preference saved." to "भाषा प्राथमिकता सहेजी गई।", "Couldn't save language. Please try again." to "भाषा सहेजी नहीं जा सकी। फिर प्रयास करें।",
        "English" to "English", "Telugu" to "तेलुगु", "Hindi" to "हिन्दी"
    )
    return when (language) { "Telugu" -> te[text] ?: text; "Hindi" -> hi[text] ?: text; else -> text }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        // Deter screenshots and screen recording on supported Android surfaces.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent { AyurvedicApp() }
    }
}

private data class Product(val id: String, val name: String, val subtitle: String, val price: Double, val category: String, val emoji: String, val description: String, val imageUrl: String, val quantity: String)
private val categories = listOf("All", "Lehyas", "Grutas", "Pills", "Tailas", "Churnas", "Rasayanas", "Bhasmas")

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
        "rasayoga", "rasayanas", "rasayana" -> "Rasayanas"
        "lehya", "lehyam", "lehyas" -> "Lehyas"
        "grutha", "ghrita", "grutas", "gruthas" -> "Grutas"
        "pill", "pills", "vati" -> "Pills"
        "taila", "tailam", "tailas" -> "Tailas"
        "churna", "churnas" -> "Churnas"
        "bhasma", "bhasmas" -> "Bhasmas"
        else -> rawCategory
    }
    val emoji = when (category) {
        "Lehyas" -> "🫙"
        "Grutas" -> "🥣"
        "Pills" -> "💊"
        "Tailas" -> "🪔"
        "Churnas" -> "🌰"
        "Rasayanas" -> "🪷"
        "Bhasmas" -> "⚱️"
        else -> "🌿"
    }
    val description = listOf("product_description", "description", "product_details", "details").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    val imageUrl = listOf("product_image", "image_url", "imageUrl", "image", "product_image_url").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") } }.orEmpty()
    val quantity = listOf("product_quantity", "quantity", "stock").firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
    return Product(data["id"]?.toString() ?: name.hashCode().toString(), name, details.ifBlank { "Traditional Ayurvedic product" }, price, category, emoji, description, imageUrl, quantity)
}
@Composable
private fun AyurvedicApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }
    var authResolved by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(10000)
        showSplash = false
    }
    DisposableEffect(auth) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            signedIn = firebaseAuth.currentUser != null
            authResolved = true
        }
        auth.addAuthStateListener(authStateListener)
        onDispose { auth.removeAuthStateListener(authStateListener) }
    }
    var userInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
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
    DisposableEffect(signedIn) {
        if (signedIn) {
            val registration = FirebaseData.observeProducts(
                onProducts = { records ->
                    liveProducts = records.mapNotNull { record -> productFromFirestore(record) }
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
    val cart = remember { mutableStateMapOf<String, Int>() }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, secondary = Leaf, background = Cream, surface = Color.White, onSurface = Ink)) {
        Surface(Modifier.fillMaxSize(), color = Cream) {
            if (showSplash || !authResolved) {
                SplashScreen()
            } else if (!signedIn) LoginScreen(
                value = userInput,
                onValueChange = { userInput = it },
                password = password,
                onPasswordChange = { password = it },
                isCreatingAccount = isCreatingAccount,
                onToggleMode = { isCreatingAccount = !isCreatingAccount; authError = null },
                errorMessage = authError,
                loading = isLoading,
                onContinue = {
                    val email = userInput.trim()
                    if (email.isBlank() || password.length < 6) {
                        authError = "Enter an email and a password with at least 6 characters."
                    } else {
                        isLoading = true
                        authError = null
                        val task = if (isCreatingAccount) auth.createUserWithEmailAndPassword(email, password)
                                   else auth.signInWithEmailAndPassword(email, password)
                        task.addOnCompleteListener { result ->
                            isLoading = false
                            if (result.isSuccessful) {
                                signedIn = true
                                auth.currentUser?.uid?.let { uid ->
                                    FirebaseData.saveCustomerProfile(uid, mapOf("email" to email, "updatedAt" to System.currentTimeMillis())) { }
                                }
                            } else {
                                authError = result.exception?.localizedMessage ?: "Sign-in failed. Please try again."
                            }
                        }
                    }
                }
            )
            else if (selectedProduct != null) ProductDetailsPage(
                product = selectedProduct!!,
                cartCount = cart[selectedProduct!!.id] ?: 0,
                onBack = { selectedProduct = null },
                onAdd = { id -> cart[id] = (cart[id] ?: 0) + 1 },
                onRemove = { id -> val n = cart[id] ?: 0; if (n <= 1) cart.remove(id) else cart[id] = n - 1 }
            )
            else ShopScreen(selectedTab, { selectedTab = it }, appLanguage, { appLanguage = it }, selectedCategory, { selectedCategory = it }, search, { search = it }, cart, liveProducts, productsLoading, productsError, latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage,
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
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.86f) }
    val textAlpha = remember { Animatable(0f) }
    val breathe = rememberInfiniteTransition(label = "premiumSplash")
    val halo by breathe.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        delay(120)
        textAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.40f).align(Alignment.TopCenter)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF4D100E), Color(0xFF762A24))
                ))
        )
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(Gold))
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(Gold))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(214.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer { alpha = halo }
                        .border(1.dp, Gold, CircleShape)
                )
                Box(
                    Modifier.size(192.dp).border(1.dp, Gold.copy(alpha = 0.65f), CircleShape)
                )
                Box(
                    Modifier.size(164.dp).background(Color.White, CircleShape)
                        .border(3.dp, Gold, CircleShape).padding(11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo2),
                        contentDescription = "Sri Lakshmi Ganapathi Ayurveda Nilayam logo",
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = logoAlpha.value
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SRI LAKSHMI GANAPATHI",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Forest,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    letterSpacing = 1.1.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "AYURVEDA NILAYAM",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Leaf,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    letterSpacing = 2.0.sp
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.width(42.dp).height(1.dp).background(Gold))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(6.dp).background(Gold, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(42.dp).height(1.dp).background(Gold))
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    "TRADITION • TRUST • WELLNESS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    letterSpacing = 1.1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Serving with care since 1990",
                    fontSize = 12.sp,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Forest,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Preparing your wellness experience",
                    fontSize = 11.sp,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
@Composable
private fun LoginScreen(
    value: String,
    onValueChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isCreatingAccount: Boolean,
    onToggleMode: () -> Unit,
    errorMessage: String?,
    loading: Boolean,
    onContinue: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Traditional maroon-and-gold header band
        Box(Modifier.fillMaxWidth().height(48.dp).background(Forest.copy(red = 0.47f, green = 0.12f, blue = 0.10f))) {
            Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter).background(Gold))
        }
        Spacer(Modifier.height(24.dp))
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
            Modifier.size(168.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(154.dp)
                    .graphicsLayer { alpha = haloAlpha }
                    .border(2.dp, Gold.copy(alpha = haloAlpha), CircleShape)
            )
            Text(
                "🍃",
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer { translationY = leafDrift; rotationZ = -25f },
                fontSize = 25.sp
            )
            Text(
                "🍃",
                modifier = Modifier.align(Alignment.BottomEnd).graphicsLayer { translationY = -leafDrift; rotationZ = 25f },
                fontSize = 25.sp
            )
            Image(
                painter = painterResource(id = R.drawable.logo2),
                contentDescription = "Sri Lakshmi Ganapathi Ayurveda Nilayam logo",
                modifier = Modifier.size(142.dp).graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    rotationZ = logoRotation.value
                }
            )
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
        Spacer(Modifier.height(12.dp))
        Text(
            "Sri Lakshmi Ganapathi",
            fontSize = 29.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF8B211B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "Ayurveda Nilayam",
            fontSize = 27.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF245D32),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.width(54.dp).height(1.dp).background(Gold))
            Text(tr("  ✦  🌿  ✦  ", LocalAppLanguage.current), color = Gold, fontSize = 16.sp)
            Box(Modifier.width(54.dp).height(1.dp).background(Gold))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Estd. 1900 by Perraju Sharma",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF245D32),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "Nature's care,\nrooted in tradition.",
            fontSize = 29.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B211B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Sign in to explore your herbal wellness products.",
            color = Color(0xFF56534D),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            label = { Text(tr("Email address", LocalAppLanguage.current)) },
            placeholder = { Text(tr("you@example.com", LocalAppLanguage.current)) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF8B211B)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD2BE9C),
                unfocusedBorderColor = Color(0xFFD2BE9C),
                focusedContainerColor = Color(0xFFFFFCF5),
                unfocusedContainerColor = Color(0xFFFFFCF5),
                focusedLabelColor = Color(0xFF8B211B),
                cursorColor = Color(0xFF8B211B)
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            label = { Text(tr("Password (at least 6 characters)", LocalAppLanguage.current)) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF8B211B)) },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD2BE9C),
                unfocusedBorderColor = Color(0xFFD2BE9C),
                focusedContainerColor = Color(0xFFFFFCF5),
                unfocusedContainerColor = Color(0xFFFFFCF5),
                focusedLabelColor = Color(0xFF8B211B),
                cursorColor = Color(0xFF8B211B)
            )
        )
        if (errorMessage != null) Text(errorMessage, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFFB3261E), fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onContinue,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(62.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B211B))
        ) {
            Text(if (loading) "Please wait…" else if (isCreatingAccount) "Create account" else "Sign in", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.ArrowForward, null)
        }
        TextButton(onClick = onToggleMode, modifier = Modifier.padding(top = 2.dp)) {
            Text(if (isCreatingAccount) "Already have an account? Sign in" else "New customer? Create account", color = Forest)
        }
        Spacer(Modifier.height(8.dp))
        Text("Sign in with your registered email and password.", fontSize = 13.sp, color = Color(0xFF56534D), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Mobile-number login is not available yet.", fontSize = 12.sp, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF8B211B))) {
            Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopCenter).background(Gold))
        }
    }
}

@Composable
private fun ShopScreen(
    selectedTab: String, onTabChange: (String) -> Unit, appLanguage: String, onLanguageChange: (String) -> Unit, category: String, onCategoryChange: (String) -> Unit,
    search: String, onSearchChange: (String) -> Unit, cart: Map<String, Int>, products: List<Product>, productsLoading: Boolean, productsError: String?, latestOrderId: String?, latestOrderSuccess: Boolean?, latestOrderInvoiceUrl: String?, latestOrderMessage: String?, onAdd: (String) -> Unit, onRemove: (String) -> Unit,
    onOrderResult: (String, Boolean, String?, String) -> Unit, onSignOut: () -> Unit, onProductClick: (Product) -> Unit
) {
    val cartCount = cart.values.sum()
    Scaffold(containerColor = Cream, topBar = {
        Row(Modifier.fillMaxWidth().background(Cream).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.logo2), contentDescription = "SLGAN logo", modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(tr("Sri Lakshmi Ganapathi", LocalAppLanguage.current), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text(tr("Ayurveda Nilayam", LocalAppLanguage.current), color = Muted, fontSize = 11.sp) }
            IconButton(onClick = { onTabChange("Cart") }) {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart", tint = Forest) }
            }
        }
    }, bottomBar = {
        NavigationBar(containerColor = Color.White, contentColor = Forest) {
            listOf(Triple("Home", Icons.Default.Home, "Home"), Triple("Categories", Icons.Default.GridView, "Categories"), Triple("Cart", Icons.Default.ShoppingCart, "Cart"), Triple("Orders", Icons.Default.ReceiptLong, "Orders"), Triple("Profile", Icons.Default.Person, "Profile")).forEach { (label, icon, route) ->
                NavigationBarItem(selected = selectedTab == route, onClick = { onTabChange(route) }, icon = { Icon(icon, label) }, label = { Text(tr(label, LocalAppLanguage.current), fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = PaleGreen, unselectedIconColor = Muted))
            }
        }
    }) { padding ->
        when (selectedTab) {
            "Cart" -> CartPage(Modifier.padding(padding), cart, products, onAdd, onRemove, onOrderResult)
            "Orders" -> OrdersPage(Modifier.padding(padding), latestOrderId, latestOrderSuccess, latestOrderInvoiceUrl, latestOrderMessage)
            "Categories" -> CategoriesPage(Modifier.padding(padding), products, onCategoryChange) { onTabChange("Home") }
            "Profile" -> ProfilePage(Modifier.padding(padding), onSignOut, appLanguage, onLanguageChange)
            else -> HomePage(Modifier.padding(padding), category, onCategoryChange, search, onSearchChange, cart, products, productsLoading, productsError, onAdd, onProductClick)
        }
    }
}

@Composable
private fun HomePage(modifier: Modifier, category: String, onCategoryChange: (String) -> Unit, search: String, onSearchChange: (String) -> Unit, cart: Map<String, Int>, products: List<Product>, productsLoading: Boolean, productsError: String?, onAdd: (String) -> Unit, onProductClick: (Product) -> Unit) {
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
    val visible = products.filter { (category == "All" || it.category.equals(category, true)) && (it.name.contains(search, true) || it.subtitle.contains(search, true)) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(4.dp))
                Text(if (welcomeNickname.isNotBlank()) "Welcome, $welcomeNickname" else "Welcome to SLGAN", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(tr("Explore thoughtful herbal essentials.", LocalAppLanguage.current), color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(22.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("ROOTED IN NATURE", LocalAppLanguage.current), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(7.dp))
                        Text(tr("Everyday wellness,\nwith a herbal touch.", LocalAppLanguage.current), color = Color.White, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text(tr("Explore collection  →", LocalAppLanguage.current), color = Color(0xFFFFE7B3), fontSize = 12.sp)
                    }
                    Text(tr("🌿", LocalAppLanguage.current), fontSize = 48.sp)
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(search, onSearchChange, Modifier.fillMaxWidth(), placeholder = { Text(tr("Search products", LocalAppLanguage.current)) }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(20.dp))
                Text(tr("Shop by category", LocalAppLanguage.current), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { item -> FilterChip(selected = category == item, onClick = { onCategoryChange(item) }, label = { Text((when (item) { "All" -> "✨"; "Lehyas" -> "🫙"; "Grutas" -> "🥣"; "Pills" -> "💊"; "Tailas" -> "🪔"; "Churnas" -> "🌰"; "Rasayanas" -> "🪷"; "Bhasmas" -> "⚱️"; else -> "🌿" }) + " " + item, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Forest, selectedLabelColor = Color.White, containerColor = Color.White)) }
                }
                Spacer(Modifier.height(18.dp))
                Text(if (search.isBlank()) "Our Ayurvedic products" else "Search results", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                if (productsLoading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest) }
                if (productsError != null) { Spacer(Modifier.height(8.dp)); Text(tr("Could not load products: $productsError", LocalAppLanguage.current), color = Color(0xFFB3261E), fontSize = 12.sp) }
            }
        }
        if (visible.isEmpty()) item { Text(if (productsLoading) "Loading products…" else if (productsError != null) "Products could not be loaded. Check your internet and Firestore rules." else "No products found in this category. Try All or another search.", Modifier.padding(24.dp), color = Muted) }
        else items(visible.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { product -> ProductCard(product, cart[product.id] ?: 0, onAdd, onProductClick, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { Text(tr("Made with care for your everyday rituals.", LocalAppLanguage.current), Modifier.fillMaxWidth().padding(20.dp), color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun ProductCard(product: Product, count: Int, onAdd: (String) -> Unit, onProductClick: (Product) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable { onProductClick(product) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(11.dp)) {
            Box(Modifier.fillMaxWidth().height(98.dp).background(PaleGreen, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(product.emoji, fontSize = 42.sp)
                Text(tr("HERBAL", LocalAppLanguage.current), Modifier.align(Alignment.BottomEnd).padding(6.dp), color = Leaf, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(product.name, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Text(product.subtitle, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("₹", LocalAppLanguage.current) + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price), color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (count == 0) FilledIconButton(onClick = { onAdd(product.id) }, modifier = Modifier.size(34.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Forest)) { Icon(Icons.Default.Add, "Add " + product.name, modifier = Modifier.size(18.dp)) }
                else Text(count.toString() + " in cart", fontSize = 10.sp, color = Forest, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun ProductDetailsPage(
    product: Product,
    cartCount: Int,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back to products", tint = Forest) }
            Text("View product", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
            Text(product.category, color = Leaf, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(product.name, color = Ink, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text(product.subtitle, color = Muted, fontSize = 15.sp)
            Text("₹" + if (product.price % 1.0 == 0.0) product.price.toLong().toString() else String.format("%.2f", product.price), color = Forest, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            if (product.quantity.isNotBlank()) {
                Text("Quantity / pack: " + product.quantity, color = Ink, fontSize = 14.sp)
            }
            HorizontalDivider(color = Gold.copy(alpha = 0.45f))
            Text("Product details", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                product.description.ifBlank { "Traditional Ayurvedic product. Please contact Sri Lakshmi Ganapathi Ayurveda Nilayam for more information about ingredients, usage and suitability." },
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            Text("Category: " + product.category, color = Ink, fontSize = 13.sp)
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
                onClick = { onAdd(product.id) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
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

    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text(tr("Your cart", LocalAppLanguage.current), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(cart.values.sum().toString() + " items selected", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        if (selected.isEmpty()) EmptyState("🛍️", "Your cart is waiting", "Add herbal essentials from the Home page.")
        else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(selected) { product ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(product.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, color = Ink)
                                Text(product.subtitle, color = Muted, fontSize = 12.sp)
                                Text("₹" + String.format("%.2f", product.price), color = Forest, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { onRemove(product.id) }) { Icon(Icons.Default.Remove, "Remove one") }
                            Text((cart[product.id] ?: 0).toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onAdd(product.id) }) { Icon(Icons.Default.Add, "Add one") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Subtotal", LocalAppLanguage.current), color = Muted, modifier = Modifier.weight(1f))
                        Text("₹" + String.format("%.2f", total), color = Forest, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(onClick = { showCheckout = true; errorMessage = null }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest), modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Checkout", LocalAppLanguage.current))
                    }
                }
            }
            Text("No online payment. Place your order and we will confirm delivery details.", color = Muted, fontSize = 11.sp)
        }
    }

    if (showCheckout) {
        AlertDialog(
            onDismissRequest = { if (!submitting) showCheckout = false },
            title = { Text("Place order") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total: ₹" + String.format("%.2f", total), fontWeight = FontWeight.Bold, color = Forest)
                    OutlinedTextField(customerName, { customerName = it }, label = { Text("Customer name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Email for invoice *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(address, { address = it }, label = { Text("Full delivery address *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Text("Payment: none. This order request does not collect payment.", color = Muted, fontSize = 12.sp)
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
            dismissButton = { TextButton(enabled = !submitting, onClick = { showCheckout = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OrdersPage(
    modifier: Modifier,
    latestOrderId: String?,
    latestOrderSuccess: Boolean?,
    latestInvoiceUrl: String?,
    latestOrderMessage: String?
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

    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("My Orders", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(12.dp))
        if (latestOrderSuccess != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (latestOrderSuccess) Color(0xFFE5F4E8) else Color(0xFFFCE8E6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (latestOrderSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (latestOrderSuccess) Color(0xFF237344) else Color(0xFFB3261E)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (latestOrderSuccess) "Order successful" else "Order failed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (latestOrderSuccess) Color(0xFF237344) else Color(0xFFB3261E)
                        )
                    }
                    latestOrderId?.let { Text("Order ID: " + it, color = Ink, fontWeight = FontWeight.SemiBold) }
                    Text(latestOrderMessage ?: if (latestOrderSuccess) "Your order was saved." else "Your order could not be placed.", color = Ink)
                    if (latestOrderSuccess && !latestInvoiceUrl.isNullOrBlank()) {
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestInvoiceUrl))) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View / Download Invoice")
                        }
                    } else if (latestOrderSuccess) {
                        Text("Your order is saved. The invoice is not available here yet; please check your email or contact the shop.", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        Text("Previous orders", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        when {
            loadingOrders -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) }
            ordersError != null -> Text(ordersError!!, color = MaterialTheme.colorScheme.error)
            previousOrders.isEmpty() -> Text("You have no previous orders yet.", color = Muted)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(previousOrders) { order ->
                    val orderId = order["orderId"]?.toString() ?: order["_documentId"]?.toString().orEmpty()
                    val status = order["orderStatus"]?.toString() ?: "received"
                    val total = (order["grandTotal"] as? Number)?.toDouble() ?: 0.0
                    val invoiceUrl = order["invoiceUrl"]?.toString()
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { selectedOrder = order }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(orderId, fontWeight = FontWeight.Bold, color = Forest)
                            Text("Status: " + status.replaceFirstChar { it.uppercase() }, color = Ink)
                            Text("Product total: ₹" + String.format("%.2f", total), color = Ink)
                            val timestamp = order["createdAt"] as? com.google.firebase.Timestamp
                            if (timestamp != null) Text(timestamp.toDate().toString(), color = Muted, fontSize = 11.sp)
                            TextButton(onClick = { selectedOrder = order }) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("View complete invoice")
                            }
                            if (!invoiceUrl.isNullOrBlank()) {
                                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open invoice PDF")
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
        val created = (order["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.toString() ?: "Not available"

        AlertDialog(
            onDismissRequest = { selectedOrder = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("ORDER INVOICE", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Forest)
                    Text("Sri Lakshmi Ganapathi Ayurveda Nilayam", fontSize = 13.sp, color = Muted)
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Gold)
                    Text("Invoice No: $invoiceNo", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Order ID: $orderId", color = Ink, fontSize = 12.sp)
                    Text("Order date: $created", color = Muted, fontSize = 12.sp)
                    Text("Order status: " + status.replaceFirstChar { it.uppercase() }, color = Ink)
                    Text("Payment status: " + paymentStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }, color = Ink)
                    HorizontalDivider(color = Gold.copy(alpha = 0.7f))
                    Text("BILL TO", fontWeight = FontWeight.Bold, color = Forest)
                    Text(customerName.ifBlank { "Customer name not provided" }, color = Ink)
                    if (phone.isNotBlank()) Text("Phone: $phone", color = Ink)
                    if (email.isNotBlank()) Text("Email: $email", color = Ink)
                    if (address.isNotBlank()) Text("Delivery address: $address", color = Ink)
                    HorizontalDivider(color = Gold.copy(alpha = 0.7f))
                    Text("ITEM DETAILS", fontWeight = FontWeight.Bold, color = Forest)
                    if (invoiceItems.isEmpty()) {
                        Text("No item details were saved with this order.", color = Muted)
                    } else {
                        invoiceItems.forEachIndexed { index, item ->
                            val name = item["productName"]?.toString() ?: item["name"]?.toString() ?: "Item \${index + 1}"
                            val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                            val unitPrice = (item["unitPrice"] as? Number)?.toDouble() ?: (item["price"] as? Number)?.toDouble() ?: 0.0
                            val lineTotal = (item["lineTotal"] as? Number)?.toDouble() ?: (unitPrice * quantity)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)),
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
                        Text("Subtotal", color = Ink)
                        Text("₹" + String.format("%.2f", subtotal), color = Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery / tax", color = Ink)
                        Text("₹0.00", color = Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GRAND TOTAL", fontWeight = FontWeight.ExtraBold, color = Forest)
                        Text("₹" + String.format("%.2f", grandTotal), fontWeight = FontWeight.ExtraBold, color = Forest)
                    }
                    Text("Payment has not been collected unless the status above says otherwise.", color = Muted, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { selectedOrder = null }) { Text("Close") } },
            dismissButton = {
                val invoiceUrl = order["invoiceUrl"]?.toString()
                if (!invoiceUrl.isNullOrBlank()) {
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invoiceUrl))) }) {
                        Text("Open PDF")
                    }
                }
            },
            containerColor = Cream
        )
    }

}

@Composable
private fun CategoriesPage(modifier: Modifier, products: List<Product>, onCategoryChange: (String) -> Unit, onHome: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text(tr("Categories", LocalAppLanguage.current), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(tr("Find the collection for your routine.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        categories.drop(1).forEach { item ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onCategoryChange(item); onHome() }, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) { Text(when (item) { "Lehyas" -> "🫙"; "Grutas" -> "🥣"; "Pills" -> "💊"; "Tailas" -> "🪔"; "Churnas" -> "🌰"; "Rasayanas" -> "🪷"; "Bhasmas" -> "⚱️"; else -> "🌿" }, fontSize = 24.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(item, color = Ink, fontWeight = FontWeight.Bold); Text(products.count { it.category.equals(item, true) }.toString() + " products", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Default.ChevronRight, null, tint = Forest)
                }
            }
        }
    }
}

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
                        mobileNumber = snapshot.getString("mobileNumber").orEmpty()
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

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(tr("Your account", LocalAppLanguage.current), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(tr("Manage your personal details and delivery addresses.", LocalAppLanguage.current), color = Muted, fontSize = 13.sp)
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
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(20.dp)) {
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
            OutlinedTextField(value = mobileNumber, onValueChange = { input -> mobileNumber = input.filter { it.isDigit() || it == '+' }.take(15); saveMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Mobile number", LocalAppLanguage.current)) }, placeholder = { Text(tr("Enter mobile number", LocalAppLanguage.current)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), leadingIcon = { Icon(Icons.Default.Phone, null, tint = Forest) })
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
                    if (uid == null) languageMessage = "Please sign in again to save your profile."
                    else {
                        languageSaving = true
                        languageMessage = null
                        FirebaseData.saveCustomerProfile(uid, mapOf(
                            "languagePreference" to appLanguage,
                            "updatedAt" to System.currentTimeMillis()
                        )) { error ->
                            languageSaving = false
                            languageMessage = if (error == null) "Language preference saved." else "Couldn't save language. Please try again."
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
            if (languageMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(tr(languageMessage!!, appLanguage), color = if (languageMessage!!.contains("saved")) Forest else Color(0xFFB3261E), fontSize = 13.sp)
            }
            if (saveMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(saveMessage!!, color = if (saveMessage!!.startsWith("Saved")) Forest else Color(0xFFB3261E), fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val currentUid = uid
                    if (currentUid == null) saveMessage = "Please sign in again to save your profile."
                    else {
                        isSaving = true
                        saveMessage = null
                        FirebaseData.saveCustomerProfile(currentUid, mapOf(
                            "email" to (user.email ?: ""), "firstName" to firstName.trim(),
                            "lastName" to lastName.trim(), "nickname" to nickname.trim(),
                            "gender" to gender, "mobileNumber" to mobileNumber.trim(),
                            "updatedAt" to System.currentTimeMillis()
                        )) { error ->
                            isSaving = false
                            saveMessage = if (error == null) "Saved successfully." else "Couldn't save profile. Please try again."
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showAddressForm = false; addressMessage = null }) { Icon(Icons.Default.ArrowBack, "Back", tint = Forest) }
                Column {
                    Text(tr("Add address", LocalAppLanguage.current), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(tr("Enter delivery details below.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { showMapPicker = !showMapPicker },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Map, null, tint = Forest)
                Spacer(Modifier.width(8.dp))
                Text(if (showMapPicker) "Hide map" else "Search and select address on map")
            }
            if (showMapPicker) {
                Text("Search a place or address below, then tap a result to fill the address fields.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(14.dp)),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
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
                                <style>body{font-family:Arial;margin:0;background:#fff7e8;color:#38251f}#search{box-sizing:border-box;width:100%;padding:12px;border:1px solid #d9a441;border-radius:8px;font-size:16px}#results{max-height:115px;overflow:auto;background:white}.result{padding:10px;border-bottom:1px solid #eee;font-size:14px}#map{height:220px;width:100%}.hint{font-size:12px;padding:7px}</style></head>
                                <body><div style="padding:9px"><input id="search" placeholder="Search address, area, village or PIN code"><div id="results"></div></div><div id="map"></div><div class="hint">Choose a search result to fill the address form.</div>
                                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script><script>
                                const map=L.map('map').setView([17.385,78.4867],5);
                                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap contributors',maxZoom:19}).addTo(map);
                                let marker=null;
                                async function searchPlaces(){const q=document.getElementById('search').value.trim();if(q.length<3)return;const box=document.getElementById('results');box.innerHTML='<div class="result">Searching…</div>';try{const response=await fetch('https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit=6&q='+encodeURIComponent(q));const data=await response.json();box.innerHTML='';data.forEach(p=>{const row=document.createElement('div');row.className='result';row.textContent=p.display_name;row.onclick=()=>choose(p);box.appendChild(row)});if(!data.length)box.innerHTML='<div class="result">No results. Try a more specific search.</div>'}catch(e){box.innerHTML='<div class="result">Search unavailable. Check internet and try again.</div>'}}
                                let timer;document.getElementById('search').addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(searchPlaces,650)});
                                function choose(p){document.getElementById('results').innerHTML='';map.setView([Number(p.lat),Number(p.lon)],16);if(marker)map.removeLayer(marker);marker=L.marker([Number(p.lat),Number(p.lon)]).addTo(map);const a=p.address||{};const line1=[a.house_number,a.road||a.pedestrian||a.footway].filter(Boolean).join(' ');const line2=[a.suburb||a.neighbourhood||a.quarter||a.village||a.hamlet,a.county].filter(Boolean).join(', ');const city=a.city||a.town||a.village||a.municipality||a.county||'';const st=a.state||'';const pin=a.postcode||'';const lm=a.amenity||a.shop||a.building||'';AddressBridge.selectAddress(line1||p.name||'',line2,lm,city,st,pin);document.getElementById('search').value=p.display_name}
                                </script></body></html>
                            """.trimIndent(), "text/html", "UTF-8", null)
                        }
                    }
                )
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
            OutlinedTextField(value = recipientPhone, onValueChange = { recipientPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15); addressMessage = null }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Mobile number *", LocalAppLanguage.current)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
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
                            "recipientName" to recipientName.trim(), "phone" to recipientPhone.trim(),
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
                                recipientName = ""; recipientPhone = ""; addressLine1 = ""; addressLine2 = ""
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Saved addresses", LocalAppLanguage.current), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(tr("Keep delivery locations for future orders.", LocalAppLanguage.current), color = Muted, fontSize = 12.sp)
                }
                Button(onClick = { showAddressForm = true; addressMessage = null }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(tr("Add address", LocalAppLanguage.current))
                }
            }
            Spacer(Modifier.height(14.dp))
            if (addresses.isEmpty()) {
                EmptyState("📍", "No addresses yet", "Tap Add address to save your first delivery location.")
            } else {
                addresses.forEach { address ->
                    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Forest)
                                Spacer(Modifier.width(8.dp))
                                Text(address["label"]?.toString() ?: "Address", color = Ink, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(address["recipientName"]?.toString().orEmpty(), color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(address["phone"]?.toString().orEmpty(), color = Muted, fontSize = 12.sp)
                            Text(listOf(address["addressLine1"], address["addressLine2"], address["landmark"], address["city"], address["state"], address["pincode"]).mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }.joinToString(", "), color = Ink, fontSize = 13.sp)
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
private fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 52.sp); Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 13.sp, color = Muted)
    }
}
