package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.viewmodel.CartItem
import com.example.viewmodel.PaymentUiState
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.LocalAppThemeProperties

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CheckoutCartSheet(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier,
    onNavigateToScan: () -> Unit
) {
    val themeProps = LocalAppThemeProperties.current
    val cartItems by viewModel.cart.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val tax by viewModel.cartTax.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(themeProps.containerPadding)
    ) {
        // Ticket Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "CHECKOUT RECEIPT",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = themeProps.headerFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Confirm items in cart and process payment",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = { viewModel.clearCart() },
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.testTag("clear_cart_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear all",
                    tint = if (cartItems.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cart items or Empty State
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (cartItems.isEmpty()) {
                EmptyCartState(
                    onScanClick = onNavigateToScan
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            cartItem = item,
                            onQtyChange = { delta ->
                                viewModel.modifyCartQuantity(item.product.barcode, delta)
                            },
                            onRemove = {
                                viewModel.removeFromCart(item.product.barcode)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Billing Summary Panel
        if (cartItems.isNotEmpty()) {
            BillingSummaryCard(
                subtotal = subtotal,
                tax = tax,
                total = total,
                onProceedToPayment = { viewModel.initiatePayment() }
            )
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onQtyChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val themeProps = LocalAppThemeProperties.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = themeProps.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = themeProps.cardElevation / 2),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (themeProps.borderWidth > 0.dp) {
                    Modifier.border(themeProps.borderWidth, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), themeProps.cardShape)
                } else Modifier
            )
            .testTag("cart_item_${cartItem.product.barcode}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Category Icon or Marker
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = getCategoryColor(cartItem.product.category).copy(alpha = 0.15f),
                        shape = themeProps.buttonShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(cartItem.product.category),
                    contentDescription = cartItem.product.category,
                    tint = getCategoryColor(cartItem.product.category),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info (Name & Price)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.product.name,
                    fontFamily = themeProps.headerFontFamily,
                    fontWeight = themeProps.labelWeight,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format("%.2f", cartItem.product.price)} each • Barcode: ${cartItem.product.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Quantity Control Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQtyChange(-1) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("cart_qty_minus_${cartItem.product.barcode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "decrease quantity",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${cartItem.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeProps.headerFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .widthIn(min = 20.dp)
                        .testTag("cart_qty_value_${cartItem.product.barcode}"),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { onQtyChange(1) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("cart_qty_plus_${cartItem.product.barcode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "increase quantity",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("cart_remove_item_${cartItem.product.barcode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "remove item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCartState(
    onScanClick: () -> Unit
) {
    val themeProps = LocalAppThemeProperties.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Empty",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "YOUR CART IS EMPTY",
            fontFamily = themeProps.headerFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use the Barcode Scanner tab to read codes or select simulation pad elements.",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = themeProps.bodyFontFamily,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onScanClick,
            shape = themeProps.buttonShape,
            modifier = Modifier.testTag("empty_cart_scan_btn")
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Go to Scanner",
                fontFamily = themeProps.headerFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BillingSummaryCard(
    subtotal: Double,
    tax: Double,
    total: Double,
    onProceedToPayment: () -> Unit
) {
    val themeProps = LocalAppThemeProperties.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = themeProps.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = themeProps.cardElevation),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (themeProps.borderWidth > 0.dp) {
                    Modifier.border(themeProps.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), themeProps.cardShape)
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Charges breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "$${String.format("%.2f", subtotal)}",
                    fontWeight = FontWeight.Medium,
                    fontFamily = themeProps.headerFontFamily,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sales Tax (8.0%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "$${String.format("%.2f", tax)}",
                    fontWeight = FontWeight.Medium,
                    fontFamily = themeProps.headerFontFamily,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Divider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grand Total",
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeProps.headerFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$${String.format("%.2f", total)}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeProps.headerFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("checkout_grand_total")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proceed to Payment Action button
            Button(
                onClick = onProceedToPayment,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = themeProps.buttonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("checkout_payment_action_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = "NFC checkout"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TAP TO DEBIT (NFC PAY)",
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeProps.headerFontFamily,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// Utility maps to draw beautiful custom icons and indicators per category
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "beverages" -> Color(0xFF00FFCC)
        "snacks" -> Color(0xFFFF9900)
        "dairy" -> Color(0xFF33CCFF)
        "produce" -> Color(0xFFFF3366)
        "stationery" -> Color(0xFFCC66FF)
        else -> Color(0xFF90A4AE)
    }
}

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "beverages" -> Icons.Default.LocalCafe
        "snacks" -> Icons.Default.Cookie
        "dairy" -> Icons.Default.Icecream
        "produce" -> Icons.Default.Eco
        "stationery" -> Icons.Default.Create
        else -> Icons.Default.Category
    }
}
