package com.example

import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.ShopRepository
import com.example.ui.components.BarcodeScannerSheet
import com.example.ui.components.CheckoutCartSheet
import com.example.ui.components.NfcPaymentSheet
import com.example.ui.components.InventorySheet
import com.example.ui.components.SettingsSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.LocalAppThemeProperties
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import com.example.viewmodel.PaymentUiState
import com.example.viewmodel.ShopViewModel
import com.example.viewmodel.ShopViewModelFactory

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // Initialize Database & Repository safely
    private val database by lazy { AppDatabase.getDatabase(this, lifecycleScope) }
    private val repository by lazy { ShopRepository(database.shopDao()) }

    // Initialize ViewModel using Factory
    private val viewModel: ShopViewModel by viewModels {
        ShopViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Capture NFC hardware adapter safely
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(themePreset = currentTheme, darkTheme = isDarkTheme) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    // Modern reader mode for real physical NFC tag taps
    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            val options = Bundle()
            adapter.enableReaderMode(
                this,
                { tag ->
                    // Convert card UID bytes to HEX string (e.g. BF:28:92:76)
                    val rawId = tag.id
                    val hexId = rawId.joinToString(separator = ":") { eachByte ->
                        String.format("%02X", eachByte)
                    }
                    runOnUiThread {
                        viewModel.processNfcPayment(hexId)
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
                options
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}

enum class ShopTab {
    SCANNER,
    CHECKOUT,
    NFC_PAY,
    INVENTORY,
    SETTINGS
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppScreen(viewModel: ShopViewModel) {
    var currentTab by remember { mutableStateOf(ShopTab.SCANNER) }
    val cartItems by viewModel.cart.collectAsStateWithLifecycle()
    val toastMessage by viewModel.showToast.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()

    val isScanTabVisible by viewModel.isScanTabVisible.collectAsStateWithLifecycle()
    val isCheckoutTabVisible by viewModel.isCheckoutTabVisible.collectAsStateWithLifecycle()
    val isNfcTabVisible by viewModel.isNfcTabVisible.collectAsStateWithLifecycle()
    val isInventoryTabVisible by viewModel.isInventoryTabVisible.collectAsStateWithLifecycle()

    var prefilledInventoryBarcode by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Observe payment state to dynamically switch tabs to the NFC module
    LaunchedEffect(paymentState) {
        if (paymentState == PaymentUiState.WaitingForTap) {
            currentTab = ShopTab.NFC_PAY
        }
    }

    // Redirect active view if the selected tab becomes toggled off in settings
    LaunchedEffect(isScanTabVisible, isCheckoutTabVisible, isNfcTabVisible, isInventoryTabVisible) {
        val activeValue = when (currentTab) {
            ShopTab.SCANNER -> isScanTabVisible
            ShopTab.CHECKOUT -> isCheckoutTabVisible
            ShopTab.NFC_PAY -> isNfcTabVisible
            ShopTab.INVENTORY -> isInventoryTabVisible
            ShopTab.SETTINGS -> true
        }
        if (!activeValue) {
            currentTab = ShopTab.SETTINGS
        }
    }

    // Launch snackbar when Toast alerts trigger from ViewModel
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToast()
        }
    }

    val themeProps = LocalAppThemeProperties.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 12.dp)
            ) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSecondary,
                    snackbarData = data
                )
            }
        },
        bottomBar = {
            if (themeProps.navigationLayout != "none") {
                val totalQty = cartItems.sumOf { it.quantity }
                ThemeCustomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    isScanVisible = isScanTabVisible,
                    isCheckoutVisible = isCheckoutTabVisible,
                    isNfcVisible = isNfcTabVisible,
                    isInventoryVisible = isInventoryTabVisible,
                    cartCount = totalQty
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (themeProps.navigationLayout == "none") {
                CyberpunkConsoleDashboard(
                    viewModel = viewModel,
                    prefilledBarcode = prefilledInventoryBarcode,
                    onPrefillChange = { prefilledInventoryBarcode = it }
                )
            } else {
                // Keep state or animate transitions cleanly between sub-views
                when (currentTab) {
                    ShopTab.SCANNER -> {
                        BarcodeScannerSheet(
                            viewModel = viewModel,
                            onNavigateToInventory = { barcode ->
                                prefilledInventoryBarcode = barcode
                                currentTab = ShopTab.INVENTORY
                            }
                        )
                    }
                    ShopTab.CHECKOUT -> {
                        CheckoutCartSheet(
                            viewModel = viewModel,
                            onNavigateToScan = {
                                currentTab = ShopTab.SCANNER
                            }
                        )
                    }
                    ShopTab.NFC_PAY -> {
                        NfcPaymentSheet(
                            viewModel = viewModel
                        )
                    }
                    ShopTab.INVENTORY -> {
                        InventorySheet(
                            viewModel = viewModel,
                            prefilledBarcode = prefilledInventoryBarcode
                        )
                    }
                    ShopTab.SETTINGS -> {
                        SettingsSheet(
                            viewModel = viewModel
                        )
                    }
                }
            }

            // High-fidelity scanline CRT overlay for Cyberpunk dark theme
            if (themeProps.showScanlineOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val lineSpacing = 6.dp.toPx()
                            val count = (size.height / lineSpacing).toInt()
                            for (i in 0 until count) {
                                val y = i * lineSpacing
                                drawLine(
                                    color = Color.Black.copy(alpha = 0.08f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                )
            }
        }
    }
}

// Radical structural navigation bar switcher containing styled layouts for each theme
@Composable
fun ThemeCustomNavigationBar(
    currentTab: ShopTab,
    onTabSelected: (ShopTab) -> Unit,
    isScanVisible: Boolean,
    isCheckoutVisible: Boolean,
    isNfcVisible: Boolean,
    isInventoryVisible: Boolean,
    cartCount: Int
) {
    val themeProps = LocalAppThemeProperties.current
    
    when (themeProps.navigationLayout) {
        "floating_bubble" -> {
            // Suspended dynamic dynamic island pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color.Transparent)
            ) {
                Card(
                    shape = themeProps.cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_navigation_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabItemBubble(
                            selected = currentTab == ShopTab.SCANNER,
                            onClick = { onTabSelected(ShopTab.SCANNER) },
                            icon = Icons.Default.QrCodeScanner,
                            label = "Scan",
                            visible = isScanVisible
                        )
                        TabItemBubble(
                            selected = currentTab == ShopTab.CHECKOUT,
                            onClick = { onTabSelected(ShopTab.CHECKOUT) },
                            icon = Icons.Default.ShoppingCart,
                            label = "Checkout",
                            isCart = true,
                            cartCount = cartCount,
                            visible = isCheckoutVisible
                        )
                        TabItemBubble(
                            selected = currentTab == ShopTab.NFC_PAY,
                            onClick = { onTabSelected(ShopTab.NFC_PAY) },
                            icon = Icons.Default.Nfc,
                            label = "NFC Pay",
                            visible = isNfcVisible
                        )
                        TabItemBubble(
                            selected = currentTab == ShopTab.INVENTORY,
                            onClick = { onTabSelected(ShopTab.INVENTORY) },
                            icon = Icons.Default.Inventory,
                            label = "Stock",
                            visible = isInventoryVisible
                        )
                        TabItemBubble(
                            selected = currentTab == ShopTab.SETTINGS,
                            onClick = { onTabSelected(ShopTab.SETTINGS) },
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            visible = true
                        )
                    }
                }
            }
        }
        "hardware_dock" -> {
            // Command-center console keypad with green glow indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    .testTag("app_navigation_bar"),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabItemHardwareKey(
                    selected = currentTab == ShopTab.SCANNER,
                    onClick = { onTabSelected(ShopTab.SCANNER) },
                    icon = Icons.Default.QrCodeScanner,
                    label = "SYS.SCAN",
                    visible = isScanVisible
                )
                TabItemHardwareKey(
                    selected = currentTab == ShopTab.CHECKOUT,
                    onClick = { onTabSelected(ShopTab.CHECKOUT) },
                    icon = Icons.Default.ShoppingCart,
                    label = "CHECKOUT",
                    isCart = true,
                    cartCount = cartCount,
                    visible = isCheckoutVisible
                )
                TabItemHardwareKey(
                    selected = currentTab == ShopTab.NFC_PAY,
                    onClick = { onTabSelected(ShopTab.NFC_PAY) },
                    icon = Icons.Default.Nfc,
                    label = "NFC.NET",
                    visible = isNfcVisible
                )
                TabItemHardwareKey(
                    selected = currentTab == ShopTab.INVENTORY,
                    onClick = { onTabSelected(ShopTab.INVENTORY) },
                    icon = Icons.Default.Inventory,
                    label = "STK.LEDG",
                    visible = isInventoryVisible
                )
                TabItemHardwareKey(
                    selected = currentTab == ShopTab.SETTINGS,
                    onClick = { onTabSelected(ShopTab.SETTINGS) },
                    icon = Icons.Default.Settings,
                    label = "CFG.OPTS",
                    visible = true
                )
            }
        }
        "minimal_foam" -> {
            // Calm minimalist seascape navigation indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(width = 0.5.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(vertical = 10.dp)
                    .testTag("app_navigation_bar"),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItemMinimalFoam(
                    selected = currentTab == ShopTab.SCANNER,
                    onClick = { onTabSelected(ShopTab.SCANNER) },
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan",
                    visible = isScanVisible
                )
                TabItemMinimalFoam(
                    selected = currentTab == ShopTab.CHECKOUT,
                    onClick = { onTabSelected(ShopTab.CHECKOUT) },
                    icon = Icons.Default.ShoppingCart,
                    label = "Cart",
                    isCart = true,
                    cartCount = cartCount,
                    visible = isCheckoutVisible
                )
                TabItemMinimalFoam(
                    selected = currentTab == ShopTab.NFC_PAY,
                    onClick = { onTabSelected(ShopTab.NFC_PAY) },
                    icon = Icons.Default.Nfc,
                    label = "Pay",
                    visible = isNfcVisible
                )
                TabItemMinimalFoam(
                    selected = currentTab == ShopTab.INVENTORY,
                    onClick = { onTabSelected(ShopTab.INVENTORY) },
                    icon = Icons.Default.Inventory,
                    label = "Store",
                    visible = isInventoryVisible
                )
                TabItemMinimalFoam(
                    selected = currentTab == ShopTab.SETTINGS,
                    onClick = { onTabSelected(ShopTab.SETTINGS) },
                    icon = Icons.Default.Settings,
                    label = "Prefs",
                    visible = true
                )
            }
        }
        else -> {
            // Sunset Ember: Slanted dynamic geometric chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .testTag("app_navigation_bar"),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItemSlanted(
                    selected = currentTab == ShopTab.SCANNER,
                    onClick = { onTabSelected(ShopTab.SCANNER) },
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan",
                    isAlternate = false,
                    visible = isScanVisible
                )
                TabItemSlanted(
                    selected = currentTab == ShopTab.CHECKOUT,
                    onClick = { onTabSelected(ShopTab.CHECKOUT) },
                    icon = Icons.Default.ShoppingCart,
                    label = "Cart",
                    isAlternate = true,
                    isCart = true,
                    cartCount = cartCount,
                    visible = isCheckoutVisible
                )
                TabItemSlanted(
                    selected = currentTab == ShopTab.NFC_PAY,
                    onClick = { onTabSelected(ShopTab.NFC_PAY) },
                    icon = Icons.Default.Nfc,
                    label = "Pay",
                    isAlternate = false,
                    visible = isNfcVisible
                )
                TabItemSlanted(
                    selected = currentTab == ShopTab.INVENTORY,
                    onClick = { onTabSelected(ShopTab.INVENTORY) },
                    icon = Icons.Default.Inventory,
                    label = "LEDG",
                    isAlternate = true,
                    visible = isInventoryVisible
                )
                TabItemSlanted(
                    selected = currentTab == ShopTab.SETTINGS,
                    onClick = { onTabSelected(ShopTab.SETTINGS) },
                    icon = Icons.Default.Settings,
                    label = "Prefs",
                    isAlternate = false,
                    visible = true
                )
            }
        }
    }
}

// M3 Expressive: Bubble-pill nav item with bouncy touch scales
@Composable
fun TabItemBubble(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    visible: Boolean,
    isCart: Boolean = false,
    cartCount: Int = 0
) {
    if (!visible) return
    
    val themeProps = LocalAppThemeProperties.current
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val labelColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
    val iconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            if (isCart && cartCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("$cartCount", fontSize = 9.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = themeProps.bodyFontFamily,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}

// Cyberpunk Dark: Industrial key pad button with electronic indicators
@Composable
fun TabItemHardwareKey(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    visible: Boolean,
    isCart: Boolean = false,
    cartCount: Int = 0
) {
    if (!visible) return
    
    val themeProps = LocalAppThemeProperties.current
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
    val ledColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val rawBg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent

    Column(
        modifier = Modifier
            .width(66.dp)
            .height(58.dp)
            .clickable { onClick() }
            .background(rawBg)
            .border(1.dp, borderColor, RoundedCornerShape(0.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Digital LED Indicator top line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ledColor)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            if (isCart && cartCount > 0) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text("$cartCount", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = themeProps.bodyFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

// Ocean Breeze: High minimalist foam tab deck
@Composable
fun TabItemMinimalFoam(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    visible: Boolean,
    isCart: Boolean = false,
    cartCount: Int = 0
) {
    if (!visible) return
    
    val themeProps = LocalAppThemeProperties.current
    val colorAccent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorAccent,
                modifier = Modifier.size(22.dp)
            )
            if (isCart && cartCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("$cartCount", fontSize = 9.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = themeProps.bodyFontFamily,
            color = colorAccent
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Seaside wave underline trace
        if (selected) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        } else {
            Box(modifier = Modifier.height(2.dp))
        }
    }
}

// Sunset Ember: Skewed artistic tab with custom crop angles
@Composable
fun TabItemSlanted(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isAlternate: Boolean,
    visible: Boolean,
    isCart: Boolean = false,
    cartCount: Int = 0
) {
    if (!visible) return
    
    val themeProps = LocalAppThemeProperties.current
    val shape = if (isAlternate) {
        RoundedCornerShape(topStart = 0.dp, bottomEnd = 0.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 0.dp, bottomStart = 0.dp)
    }
    val background = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f) else Color.Transparent
    val accentColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray

    Column(
        modifier = Modifier
            .clip(shape)
            .clickable { onClick() }
            .background(background)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = shape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            if (isCart && cartCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("$cartCount", fontSize = 9.sp)
                }
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = themeProps.bodyFontFamily,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
fun CyberpunkConsoleDashboard(
    viewModel: ShopViewModel,
    prefilledBarcode: String,
    onPrefillChange: (String) -> Unit
) {
    var expandedModule by remember { mutableStateOf("scanner") } // "scanner", "cart", "nfc", "ledger", "settings"
    val themeProps = LocalAppThemeProperties.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dashboard Header
        Card(
            shape = themeProps.cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), themeProps.cardShape)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = ">>> INDUSTRIAL COCKPIT TERMINAL v5.2",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SECURE LOCAL STORAGE REGISTRY // OFFLINE MODE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        // Module 1: Scanner
        ConsoleModuleCard(
            title = "SYS.CAMERA_SCANNER",
            isExpanded = expandedModule == "scanner",
            onToggle = { expandedModule = if (expandedModule == "scanner") "" else "scanner" },
            accentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(modifier = Modifier.height(380.dp)) {
                BarcodeScannerSheet(
                    viewModel = viewModel,
                    onNavigateToInventory = { barcode ->
                        onPrefillChange(barcode)
                        expandedModule = "ledger"
                    }
                )
            }
        }

        // Module 2: Cart Checkout
        ConsoleModuleCard(
            title = "TICKET.RECEIPT_REGISTER",
            isExpanded = expandedModule == "cart",
            onToggle = { expandedModule = if (expandedModule == "cart") "" else "cart" },
            accentColor = MaterialTheme.colorScheme.secondary
        ) {
            Box(modifier = Modifier.height(440.dp)) {
                CheckoutCartSheet(
                    viewModel = viewModel,
                    onNavigateToScan = {
                        expandedModule = "scanner"
                    }
                )
            }
        }

        // Module 3: NFC Debit
        ConsoleModuleCard(
            title = "NFC.DEBIT_TERMINAL",
            isExpanded = expandedModule == "nfc",
            onToggle = { expandedModule = if (expandedModule == "nfc") "" else "nfc" },
            accentColor = MaterialTheme.colorScheme.tertiary
        ) {
            Box(modifier = Modifier.height(380.dp)) {
                NfcPaymentSheet(
                    viewModel = viewModel
                )
            }
        }

        // Module 4: Ledger Inventory
        ConsoleModuleCard(
            title = "LOGISTICS.STOCK_LEDGER",
            isExpanded = expandedModule == "ledger",
            onToggle = { expandedModule = if (expandedModule == "ledger") "" else "ledger" },
            accentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(modifier = Modifier.height(480.dp)) {
                InventorySheet(
                    viewModel = viewModel,
                    prefilledBarcode = prefilledBarcode
                )
            }
        }

        // Module 5: Config Options
        ConsoleModuleCard(
            title = "SYSTEM.CONFIG_OPTS",
            isExpanded = expandedModule == "settings",
            onToggle = { expandedModule = if (expandedModule == "settings") "" else "settings" },
            accentColor = Color.Gray
        ) {
            Box(modifier = Modifier.height(400.dp)) {
                SettingsSheet(
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun ConsoleModuleCard(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    val themeProps = LocalAppThemeProperties.current
    Card(
        shape = themeProps.cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isExpanded) 1.5.dp else 1.dp,
                color = if (isExpanded) accentColor else accentColor.copy(alpha = 0.4f),
                shape = themeProps.cardShape
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .background(if (isExpanded) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isExpanded) accentColor else Color.DarkGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) accentColor else Color.Gray
                    )
                }
                Text(
                    text = if (isExpanded) "[-] COLLAPSE" else "[+] ENGAGE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpanded) accentColor else Color.Gray
                )
            }
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}
