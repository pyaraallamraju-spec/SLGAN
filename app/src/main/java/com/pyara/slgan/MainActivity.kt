package com.pyara.slgan

import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent { AyurvedicApp() }
    }
}

private data class Product(val id: Int, val name: String, val subtitle: String, val price: Int, val category: String, val emoji: String)
private val products = listOf(
    Product(1, "Ashwagandha Capsules", "Herbal wellness · 60 caps", 249, "Supplements", "🌿"),
    Product(2, "Amla Powder", "Traditional herb · 100 g", 149, "Powders", "🍃"),
    Product(3, "Neem Hair Oil", "Herbal hair care · 100 ml", 199, "Oils", "🫒"),
    Product(4, "Triphala Churna", "Herbal powder · 100 g", 129, "Powders", "🌱"),
    Product(5, "Aloe Vera Gel", "Skin care · 150 g", 179, "Skin Care", "🌵"),
    Product(6, "Brahmi Herbal Oil", "Traditional oil · 100 ml", 229, "Oils", "🪴"),
    Product(7, "Tulsi Herbal Drops", "Herbal extract · 30 ml", 99, "Supplements", "🌿"),
    Product(8, "Herbal Bath Powder", "Daily care · 100 g", 159, "Skin Care", "🌼")
)
private val categories = listOf("All", "Supplements", "Powders", "Oils", "Skin Care")

@Composable
private fun AyurvedicApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }
    var authResolved by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(5000)
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
    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    val cart = remember { mutableStateMapOf<Int, Int>() }
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
            else ShopScreen(selectedTab, { selectedTab = it }, selectedCategory, { selectedCategory = it }, search, { search = it }, cart,
                { id -> cart[id] = (cart[id] ?: 0) + 1 },
                { id -> val n = cart[id] ?: 0; if (n <= 1) cart.remove(id) else cart[id] = n - 1 },
                { auth.signOut(); signedIn = false; password = "" })
        }
    }
}
@Composable
private fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splashGoldCircle")
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringRotation"
    )
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "goldGlow"
    )
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(650))
        logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color(0xFF260806), Color(0xFF651B17), Color(0xFF210605))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(Gold.copy(alpha = 0.9f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(194.dp)
                        .graphicsLayer { rotationZ = ringRotation; alpha = glow }
                        .border(2.dp, Gold.copy(alpha = glow), CircleShape)
                )
                Box(
                    Modifier.size(174.dp)
                        .graphicsLayer { rotationZ = -ringRotation * 0.55f; alpha = glow * 0.7f }
                        .border(1.dp, Color(0xFFFFE6A3).copy(alpha = glow), CircleShape)
                )
                Image(
                    painter = painterResource(id = R.drawable.logo2),
                    contentDescription = "Sri Lakshmi Ganapathi Ayurveda Nilayam logo",
                    modifier = Modifier.size(150.dp).graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                )
            }
            Spacer(Modifier.height(22.dp))
            Text("Sri Lakshmi Ganapathi", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFE6A3), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Ayurveda Nilayam", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("Estd. 1900 by Perraju Sharma", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFD783), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Gold,
                strokeWidth = 2.dp
            )
        }
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(Gold.copy(alpha = 0.9f)))
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
            Text("  ✦  🌿  ✦  ", color = Gold, fontSize = 16.sp)
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
            label = { Text("Email address") },
            placeholder = { Text("you@example.com") },
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
            label = { Text("Password (at least 6 characters)") },
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
        Text("Secure email and password sign-in", fontSize = 13.sp, color = Color(0xFF56534D))
        Spacer(Modifier.height(30.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF8B211B))) {
            Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopCenter).background(Gold))
        }
    }
}

@Composable
private fun ShopScreen(
    selectedTab: String, onTabChange: (String) -> Unit, category: String, onCategoryChange: (String) -> Unit,
    search: String, onSearchChange: (String) -> Unit, cart: Map<Int, Int>, onAdd: (Int) -> Unit, onRemove: (Int) -> Unit, onSignOut: () -> Unit
) {
    val cartCount = cart.values.sum()
    Scaffold(containerColor = Cream, topBar = {
        Row(Modifier.fillMaxWidth().background(Cream).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.logo2), contentDescription = "SLGAN logo", modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text("Sri Lakshmi Ganapathi", color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text("Ayurveda Nilayam", color = Muted, fontSize = 11.sp) }
            IconButton(onClick = { onTabChange("Cart") }) {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) { Icon(Icons.Default.ShoppingCart, "Cart", tint = Forest) }
            }
        }
    }, bottomBar = {
        NavigationBar(containerColor = Color.White, contentColor = Forest) {
            listOf(Triple("Home", Icons.Default.Home, "Home"), Triple("Categories", Icons.Default.GridView, "Categories"), Triple("Cart", Icons.Default.ShoppingCart, "Cart"), Triple("Profile", Icons.Default.Person, "Profile")).forEach { (label, icon, route) ->
                NavigationBarItem(selected = selectedTab == route, onClick = { onTabChange(route) }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = PaleGreen, unselectedIconColor = Muted))
            }
        }
    }) { padding ->
        when (selectedTab) {
            "Cart" -> CartPage(Modifier.padding(padding), cart, onAdd, onRemove)
            "Categories" -> CategoriesPage(Modifier.padding(padding), onCategoryChange) { onTabChange("Home") }
            "Profile" -> ProfilePage(Modifier.padding(padding), onSignOut)
            else -> HomePage(Modifier.padding(padding), category, onCategoryChange, search, onSearchChange, cart, onAdd)
        }
    }
}

@Composable
private fun HomePage(modifier: Modifier, category: String, onCategoryChange: (String) -> Unit, search: String, onSearchChange: (String) -> Unit, cart: Map<Int, Int>, onAdd: (Int) -> Unit) {
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
    val visible = products.filter { (category == "All" || it.category == category) && (it.name.contains(search, true) || it.subtitle.contains(search, true)) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(4.dp))
                Text(if (welcomeNickname.isNotBlank()) "Welcome, $welcomeNickname" else "Welcome to SLGAN", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Explore thoughtful herbal essentials.", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(22.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ROOTED IN NATURE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(7.dp))
                        Text("Everyday wellness,\nwith a herbal touch.", color = Color.White, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("Explore collection  →", color = Color(0xFFFFE7B3), fontSize = 12.sp)
                    }
                    Text("🌿", fontSize = 48.sp)
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(search, onSearchChange, Modifier.fillMaxWidth(), placeholder = { Text("Search products") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(20.dp))
                Text("Shop by category", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(4).forEach { item -> FilterChip(selected = category == item, onClick = { onCategoryChange(item) }, label = { Text(item, fontSize = 10.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Forest, selectedLabelColor = Color.White, containerColor = Color.White)) }
                }
                Spacer(Modifier.height(18.dp))
                Text(if (search.isBlank()) "Popular essentials" else "Search results", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            }
        }
        if (visible.isEmpty()) item { Text("No products found. Try another search.", Modifier.padding(24.dp), color = Muted) }
        else items(visible.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { product -> ProductCard(product, cart[product.id] ?: 0, onAdd, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { Text("Made with care for your everyday rituals.", Modifier.fillMaxWidth().padding(20.dp), color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun ProductCard(product: Product, count: Int, onAdd: (Int) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(11.dp)) {
            Box(Modifier.fillMaxWidth().height(98.dp).background(PaleGreen, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(product.emoji, fontSize = 42.sp)
                Text("HERBAL", Modifier.align(Alignment.BottomEnd).padding(6.dp), color = Leaf, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(product.name, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Text(product.subtitle, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹" + product.price, color = Forest, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (count == 0) FilledIconButton(onClick = { onAdd(product.id) }, modifier = Modifier.size(34.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Forest)) { Icon(Icons.Default.Add, "Add " + product.name, modifier = Modifier.size(18.dp)) }
                else Text(count.toString() + " in cart", fontSize = 10.sp, color = Forest, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartPage(modifier: Modifier, cart: Map<Int, Int>, onAdd: (Int) -> Unit, onRemove: (Int) -> Unit) {
    val selected = products.filter { (cart[it.id] ?: 0) > 0 }
    val total = selected.sumOf { it.price * (cart[it.id] ?: 0) }
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("Your cart", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(cart.values.sum().toString() + " items selected", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        if (selected.isEmpty()) EmptyState("🛍️", "Your cart is waiting", "Add herbal essentials from the Home page.")
        else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(selected) { product ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).background(PaleGreen, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 28.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp); Text("₹" + product.price + " each", color = Muted, fontSize = 12.sp) }
                            IconButton(onClick = { onRemove(product.id) }) { Icon(Icons.Default.Remove, "Remove one") }
                            Text((cart[product.id] ?: 0).toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onAdd(product.id) }) { Icon(Icons.Default.Add, "Add one") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Subtotal", color = Muted, fontSize = 13.sp); Text("₹" + total, color = Forest, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
                Button(onClick = { }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text("Checkout") }
            }
            Text("Demo only: checkout and payments are not connected.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CategoriesPage(modifier: Modifier, onCategoryChange: (String) -> Unit, onHome: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("Categories", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Find the collection for your routine.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        categories.drop(1).forEach { item ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onCategoryChange(item); onHome() }, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) { Text(when (item) { "Oils" -> "🫒"; "Powders" -> "🌾"; "Skin Care" -> "🌼"; else -> "🌿" }, fontSize = 24.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(item, color = Ink, fontWeight = FontWeight.Bold); Text(products.count { it.category == item }.toString() + " products", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Default.ChevronRight, null, tint = Forest)
                }
            }
        }
    }
}

@Composable
private fun ProfilePage(modifier: Modifier, onSignOut: () -> Unit) {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    var firstName by remember(uid) { mutableStateOf("") }
    var lastName by remember(uid) { mutableStateOf("") }
    var nickname by remember(uid) { mutableStateOf("") }
    var gender by remember(uid) { mutableStateOf("") }
    var mobileNumber by remember(uid) { mutableStateOf("") }
    var genderMenuExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uid) {
        var registration: ListenerRegistration? = null
        if (uid != null) {
            registration = FirebaseFirestore.getInstance().collection("customers").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        saveMessage = "Couldn't load profile. Check your connection and try again."
                    } else if (snapshot != null && snapshot.exists()) {
                        firstName = snapshot.getString("firstName").orEmpty()
                        lastName = snapshot.getString("lastName").orEmpty()
                        nickname = snapshot.getString("nickname").orEmpty()
                        gender = snapshot.getString("gender").orEmpty()
                        mobileNumber = snapshot.getString("mobileNumber").orEmpty()
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

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Your profile", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text("Update your personal details. Changes sync to your SLGAN account.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Forest, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOf(nickname, firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "SLGAN Customer" }, color = Ink, fontWeight = FontWeight.Bold)
                    Text(mobileNumber.ifBlank { user?.email.orEmpty() }, color = Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = firstName, onValueChange = { firstName = it; saveMessage = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("First name") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = Forest) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = lastName, onValueChange = { lastName = it; saveMessage = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("Last name") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Forest) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = nickname, onValueChange = { nickname = it; saveMessage = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("Nickname") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Face, null, tint = Forest) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { input -> mobileNumber = input.filter { it.isDigit() || it == '+' }.take(15); saveMessage = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mobile number") },
            placeholder = { Text("Enter mobile number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Forest) }
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = gender, onValueChange = {}, readOnly = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("Gender") },
                placeholder = { Text("Select gender") },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                leadingIcon = { Icon(Icons.Default.Wc, null, tint = Forest) }
            )
            Box(Modifier.matchParentSize().clickable { genderMenuExpanded = true })
            DropdownMenu(expanded = genderMenuExpanded, onDismissRequest = { genderMenuExpanded = false }) {
                listOf("Female", "Male", "Non-binary", "Prefer not to say").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        gender = option
                        genderMenuExpanded = false
                        saveMessage = null
                    })
                }
            }
        }
        if (saveMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(saveMessage!!, color = if (saveMessage!!.startsWith("Saved")) Forest else Color(0xFFB3261E), fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                val currentUid = uid
                if (currentUid == null) {
                    saveMessage = "Please sign in again to save your profile."
                } else {
                    isSaving = true
                    saveMessage = null
                    FirebaseData.saveCustomerProfile(
                        currentUid,
                        mapOf(
                            "email" to (user.email ?: ""),
                            "firstName" to firstName.trim(),
                            "lastName" to lastName.trim(),
                            "nickname" to nickname.trim(),
                            "gender" to gender,
                            "mobileNumber" to mobileNumber.trim(),
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ) { error ->
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
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isLoading) "Loading profile…" else if (isSaving) "Saving…" else "Save profile", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
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
