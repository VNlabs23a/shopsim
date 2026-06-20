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
import com.example.ui.theme.MyApplicationTheme
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
            MyApplicationTheme {
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
    INVENTORY
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppScreen(viewModel: ShopViewModel) {
    var currentTab by remember { mutableStateOf(ShopTab.SCANNER) }
    val cartItems by viewModel.cart.collectAsStateWithLifecycle()
    val toastMessage by viewModel.showToast.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()

    var prefilledInventoryBarcode by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Observe payment state to dynamically switch tabs to the NFC module
    LaunchedEffect(paymentState) {
        if (paymentState == PaymentUiState.WaitingForTap) {
            currentTab = ShopTab.NFC_PAY
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == ShopTab.SCANNER,
                    onClick = { currentTab = ShopTab.SCANNER },
                    label = { Text("Scan") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan item"
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_scan")
                )

                NavigationBarItem(
                    selected = currentTab == ShopTab.CHECKOUT,
                    onClick = { currentTab = ShopTab.CHECKOUT },
                    label = { Text("Checkout") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartItems.isNotEmpty()) {
                                    val count = cartItems.sumOf { it.quantity }
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("$count", modifier = Modifier.testTag("cart_badge_count"))
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Active checkout cart"
                            )
                        }
                    },
                    modifier = Modifier.testTag("nav_tab_checkout")
                )

                NavigationBarItem(
                    selected = currentTab == ShopTab.NFC_PAY,
                    onClick = { currentTab = ShopTab.NFC_PAY },
                    label = { Text("NFC Pay") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "Contactless prepaid terminal"
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_nfc")
                )

                NavigationBarItem(
                    selected = currentTab == ShopTab.INVENTORY,
                    onClick = {
                        // clear any prefill states on tab clicks
                        prefilledInventoryBarcode = ""
                        currentTab = ShopTab.INVENTORY
                    },
                    label = { Text("Inventory") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Ledger list"
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_inventory")
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
            }
        }
    }
}
