package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NfcCardEntity
import com.example.viewmodel.PaymentUiState
import com.example.viewmodel.Receipt
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.LocalAppThemeProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NfcPaymentSheet(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val themeProps = LocalAppThemeProperties.current
    val cards by viewModel.cards.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val totalCost by viewModel.cartTotal.collectAsState()

    var showAddCardDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(themeProps.containerPadding)
    ) {
        // Upper Title / Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "NFC PAYMENT TERMINAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = themeProps.headerFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Tap physical NFC key card or simulate default tag",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = { showAddCardDialog = true },
                modifier = Modifier.testTag("admin_register_card_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddCard,
                    contentDescription = "Add card template",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Tap Payment UI Centerpiece when checkout is waiting!
        if (paymentState == PaymentUiState.WaitingForTap || paymentState is PaymentUiState.Error || paymentState == PaymentUiState.Processing) {
            NfcTerminalTapActive(
                viewModel = viewModel,
                totalCost = totalCost,
                paymentState = paymentState
            )
        } else if (paymentState is PaymentUiState.Success) {
            // Receipt view
            ReceiptSummaryView(
                receipt = (paymentState as PaymentUiState.Success).receipt,
                onClose = { viewModel.finishPaymentSession() }
            )
        } else {
            // Idle screen: show active cards & simulated reload controls
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "REGISTERED CHECKOUT CARDS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Render Vince's Card and others in list
                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading registered cards...",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(cards, key = { it.uid }) { card ->
                            NfcCreditCardItem(
                                card = card,
                                isDefault = card.uid == "BF:28:92:76",
                                onReloadClick = { amount ->
                                    viewModel.reloadCardBalance(card.uid, amount)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Physical NFC Info guide
                NfcHardwareDetectorFooter()
            }
        }
    }

    if (showAddCardDialog) {
        RegisterCardDialog(
            onDismiss = { showAddCardDialog = false },
            onSave = { uid, name, bal ->
                viewModel.saveNfcCard(uid, name, bal)
                showAddCardDialog = false
            }
        )
    }
}

@Composable
fun NfcTerminalTapActive(
    viewModel: ShopViewModel,
    totalCost: Double,
    paymentState: PaymentUiState
) {
    val themeProps = LocalAppThemeProperties.current
    var selectedSimCardUid by remember { mutableStateOf("BF:28:92:76") }
    val cards by viewModel.cards.collectAsState()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = themeProps.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = themeProps.cardElevation),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(
                if (themeProps.borderWidth > 0.dp) {
                    Modifier.border(themeProps.borderWidth, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), themeProps.cardShape)
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NFC TERMINAL ACTIVE",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = themeProps.headerFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Wave Radar Ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = "Waves",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total amount due:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = "$${String.format("%.2f", totalCost)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("checkout_payment_amount_due")
            )

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 16.dp))

            // State Messages
            when (paymentState) {
                is PaymentUiState.WaitingForTap -> {
                    Text(
                        text = "Hold physical card to device NFC receiver...",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "— OR SIMULATE TAP BELOW —",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    // Card selector dropdown or buttons for simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            // Render quick simulation chip/buttons for any registered cards
                            LazyRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(cards) { card ->
                                    val isSelected = card.uid == selectedSimCardUid
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSimCardUid = card.uid },
                                        label = {
                                            Text("${card.cardholderName} (${card.uid})")
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.processNfcPayment(selectedSimCardUid) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_nfc_tap_btn")
                    ) {
                        Icon(imageVector = Icons.Default.TapAndPlay, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMULATE NFC TAP")
                    }
                }
                is PaymentUiState.Processing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Deducting keys & updating inventory stock ledger...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                is PaymentUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NFC DISMISS / ERROR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = paymentState.message,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.processNfcPayment(selectedSimCardUid) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Retry Transaction")
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.cancelPayment() },
                modifier = Modifier.testTag("cancel_payment_btn")
            ) {
                Text("Cancel & Modify Cart", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun NfcCreditCardItem(
    card: NfcCardEntity,
    isDefault: Boolean,
    onReloadClick: (Double) -> Unit
) {
    val themeProps = LocalAppThemeProperties.current
    // Elegant luxury metallic look gradient
    val cardGradient = if (isDefault) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1F2937), // Charcoal
                Color(0xFF111827), // Deep Obsidian
                Color(0xFF030712)  // Matte Black
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0369A1), // Ocean Cyan
                Color(0xFF075985), // Emerald Blue
                Color(0xFF1E3A8A)  // Deep Blue
            )
        )
    }

    Card(
        shape = themeProps.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = themeProps.cardElevation),
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .then(
                if (themeProps.borderWidth > 0.dp) {
                    Modifier.border(
                        width = themeProps.borderWidth,
                        color = if (isDefault) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                        shape = themeProps.cardShape
                    )
                } else Modifier
            )
            .testTag("nfc_card_${card.uid}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardGradient)
                .padding(16.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "DEBIT PREPAID",
                        fontSize = 11.sp,
                        fontFamily = themeProps.bodyFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = card.cardholderName.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontFamily = themeProps.headerFontFamily,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = if (isDefault) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Chip and serial number middle
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brass chip look
                Box(
                    modifier = Modifier
                        .size(36.dp, 26.dp)
                        .background(Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Card UID representation
                Text(
                    text = "UID:  " + card.uid,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Bottom row: balance and Quick Recharge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "AVAILABLE BALANCE",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$${String.format("%.2f", card.balance)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (card.balance < 5.0) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("nfc_card_balance_${card.uid}")
                    )
                }

                // Quick reload actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onReloadClick(10.00) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("card_reload_10_btn_${card.uid}")
                    ) {
                        Text("+$10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(
                        onClick = { onReloadClick(50.00) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("card_reload_50_btn_${card.uid}")
                    ) {
                        Text("+$50", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (isDefault) {
                // Default tag marker
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DEVICES NFC KEY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptSummaryView(
    receipt: Receipt,
    onClose: () -> Unit
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateStr = formatter.format(Date(receipt.timestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .testTag("payment_receipt_popup")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "APPROVED PAYMENT RECEIPT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "TxID: ${receipt.transactionId}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable ticket items
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(receipt.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.product.name}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$${String.format("%.2f", item.product.price * item.quantity)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing breakdowns
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Subtotal:", style = MaterialTheme.typography.bodySmall)
                Text(text = "$${String.format("%.2f", receipt.subtotal)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Tax (8.0%):", style = MaterialTheme.typography.bodySmall)
                Text(text = "$${String.format("%.2f", receipt.tax)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Total Paid:", fontWeight = FontWeight.Bold)
                Text(text = "$${String.format("%.2f", receipt.total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            // Card details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Paid with card:", style = MaterialTheme.typography.bodySmall)
                Text(text = receipt.cardholderName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Remaining balance:", style = MaterialTheme.typography.bodySmall)
                Text(text = "$${String.format("%.2f", receipt.remainingBalance)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dismiss_receipt_btn")
            ) {
                Text("Print & Exit Ticket")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCardDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit
) {
    var cardUid by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("50.00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Register NFC Payment Tag", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Convert any office key, bus pass, or credit card tag to a store balance key. Simply read its hex ID first or input it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = cardUid,
                    onValueChange = { cardUid = it },
                    label = { Text("Card UID (Hex)") },
                    placeholder = { Text("e.g. AA:BB:CC:DD") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Cardholder Name") },
                    placeholder = { Text("e.g. Vince S") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = initialBalance.toDoubleOrNull() ?: 0.0
                    if (cardUid.isNotBlank() && cardholderName.isNotBlank()) {
                        onSave(cardUid, cardholderName, bal)
                    }
                }
            ) {
                Text("Register Profile")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NfcHardwareDetectorFooter() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Physical NFC tag scanning uses foreground dispatch. Simply hold any ISO 14443 standard tag to the back of the device while the app is active.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
