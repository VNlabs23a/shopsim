package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.viewmodel.ShopViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InventorySheet(
    viewModel: ShopViewModel,
    prefilledBarcode: String = "",
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(prefilledBarcode.isNotEmpty()) }

    // Form inputs
    var barcodeInput by remember { mutableStateOf(prefilledBarcode) }
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var stockInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Beverages") }

    val categories = listOf("Beverages", "Snacks", "Dairy", "Produce", "Stationery")

    // Update barcode input if parent changes it
    LaunchedEffect(prefilledBarcode) {
        if (prefilledBarcode.isNotEmpty()) {
            barcodeInput = prefilledBarcode
            showAddForm = true
        }
    }

    // Filter products
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Calculations
    val totalSkus = filteredProducts.size
    val totalStockUnits = filteredProducts.sumOf { it.stock }
    val totalValue = filteredProducts.sumOf { it.price * it.stock }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Upper Title / Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "INVENTORY CONTROLS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Monitor SKU registers, stock quotas, and values",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = {
                    showAddForm = !showAddForm
                    if (!showAddForm) {
                        // clear inputs on hide
                        barcodeInput = ""
                        nameInput = ""
                        priceInput = ""
                        stockInput = ""
                    }
                },
                modifier = Modifier.testTag("toggle_add_item_form")
            ) {
                Icon(
                    imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.LibraryAdd,
                    contentDescription = "Toggle Add SKU",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slide-down interactive Add / Edit SKU Form
        AnimatedVisibility(
            visible = showAddForm,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .testTag("add_item_form_container")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (products.any { it.barcode == barcodeInput }) "EDIT EXISTING STOCK SKU" else "REGISTER NEW STOCK SKU",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { input ->
                                barcodeInput = input
                                // autofill if already exists
                                val match = products.find { it.barcode == input.trim() }
                                if (match != null) {
                                    nameInput = match.name
                                    priceInput = match.price.toString()
                                    stockInput = match.stock.toString()
                                    categoryInput = match.category
                                }
                            },
                            label = { Text("Barcode") },
                            placeholder = { Text("e.g. 012000000133") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("form_barcode_input")
                        )

                        // Quick grab info button
                        IconButton(
                            onClick = {
                                // generate random 8 digit barcode
                                if (barcodeInput.isEmpty()) {
                                    barcodeInput = (10000000..99999999).random().toString()
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Casino, contentDescription = "Auto Barcode")
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Product Name") },
                        placeholder = { Text("e.g. Organic Milk bottle") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_name_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("Price ($)") },
                            placeholder = { Text("e.g. 2.99") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("form_price_input")
                        )

                        OutlinedTextField(
                            value = stockInput,
                            onValueChange = { stockInput = it },
                            label = { Text("Stock Qty") },
                            placeholder = { Text("e.g. 25") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("form_stock_input")
                        )
                    }

                    // Category selection list
                    Column {
                        Text(text = "Category Filter Tag", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { cat ->
                                val isSelected = cat == categoryInput
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { categoryInput = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showAddForm = false
                                barcodeInput = ""
                                nameInput = ""
                                priceInput = ""
                                stockInput = ""
                            }
                        ) {
                            Text("Clear")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val price = priceInput.toDoubleOrNull() ?: 0.0
                                val stock = stockInput.toIntOrNull() ?: 0
                                if (barcodeInput.isNotBlank() && nameInput.isNotBlank()) {
                                    viewModel.addOrUpdateProduct(barcodeInput, nameInput, price, stock, categoryInput)
                                    showAddForm = false
                                    // clear
                                    barcodeInput = ""
                                    nameInput = ""
                                    priceInput = ""
                                    stockInput = ""
                                }
                            },
                            modifier = Modifier.testTag("submit_item_btn")
                        ) {
                            Text("Save Product SKU")
                        }
                    }
                }
            }
        }

        // Summary Metric Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadge(title = "SKUs Registered", value = "$totalSkus", modifier = Modifier.weight(1f))
            MetricBadge(title = "Total Units", value = "$totalStockUnits", modifier = Modifier.weight(1f))
            MetricBadge(title = "Total Value", value = "$${String.format("%.2f", totalValue)}", modifier = Modifier.weight(1.2f))
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, category, or barcode...") },
            prefix = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(end = 4.dp).size(18.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_bar")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Product stock list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.ProductionQuantityLimits, contentDescription = "Empty stock", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No stock registers found. Register new SKUs using the Add form above.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryProductRow(
                            product = product,
                            onReplenish = {
                                viewModel.addOrUpdateProduct(
                                    barcode = product.barcode,
                                    name = product.name,
                                    price = product.price,
                                    stock = product.stock + 5,
                                    category = product.category
                                )
                            },
                            onDelete = {
                                viewModel.deleteProduct(product)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadge(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title.uppercase(), fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun InventoryProductRow(
    product: ProductEntity,
    onReplenish: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = if (product.stock == 0) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("inventory_item_${product.barcode}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category tag indicator color
            Box(
                modifier = Modifier
                    .size(8.dp, 40.dp)
                    .background(getCategoryColor(product.category), RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = product.category.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = getCategoryColor(product.category),
                        modifier = Modifier
                            .background(getCategoryColor(product.category).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, getCategoryColor(product.category).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Code: " + product.barcode,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.testTag("inventory_barcode_label_${product.barcode}")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "$${String.format("%.2f", product.price)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("inventory_price_label_${product.barcode}")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Stock: ${product.stock}",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stock == 0) MaterialTheme.colorScheme.error else Color.Gray,
                        modifier = Modifier.testTag("inventory_stock_label_${product.barcode}")
                    )
                }
            }

            // Inventory Actions: Replenish (+5) & Delete SKU
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onReplenish,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .testTag("inventory_replenish_btn_${product.barcode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Replenish stock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .testTag("inventory_delete_btn_${product.barcode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete SKU",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
