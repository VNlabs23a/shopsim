package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NfcCardEntity
import com.example.data.ProductEntity
import com.example.data.ShopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
)

data class Receipt(
    val transactionId: String,
    val cardholderName: String,
    val cardUid: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val initialBalance: Double,
    val remainingBalance: Double,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface PaymentUiState {
    object Idle : PaymentUiState
    object WaitingForTap : PaymentUiState
    object Processing : PaymentUiState
    data class Success(val receipt: Receipt) : PaymentUiState
    data class Error(val message: String) : PaymentUiState
}

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    // Products from Database
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Registered NFC Cards from Database
    val cards: StateFlow<List<NfcCardEntity>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Shopping Cart State
    private val _cart = MutableStateFlow<Map<String, CartItem>>(emptyMap()) // barcode -> CartItem
    val cart: StateFlow<List<CartItem>> = _cart
        .map { it.values.toList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Calculations
    val cartSubtotal: StateFlow<Double> = cart.map { list ->
        list.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val cartTax: StateFlow<Double> = cartSubtotal.map { subtotal ->
        subtotal * 0.08 // 8% sales tax
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val cartTotal: StateFlow<Double> = combine(cartSubtotal, cartTax) { sub, tax ->
        sub + tax
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Barcode Scanner helper states
    private val _scannedProductResult = MutableStateFlow<ProductEntity?>(null)
    val scannedProductResult: StateFlow<ProductEntity?> = _scannedProductResult.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // NFC Payment UI State
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()

    // Interactive message / status info
    private val _showToast = MutableStateFlow<String?>(null)
    val showToast: StateFlow<String?> = _showToast.asStateFlow()

    // Add product to cart by searching its barcode
    fun addToCartByBarcode(barcode: String) {
        viewModelScope.launch {
            _scannedProductResult.value = null
            _scanError.value = null
            
            val trimmed = barcode.trim()
            if (trimmed.isEmpty()) return@launch

            val product = repository.getProductByBarcode(trimmed)
            if (product != null) {
                if (product.stock > 0) {
                    val currentMap = _cart.value.toMutableMap()
                    val existing = currentMap[trimmed]
                    val currentQtyInCart = existing?.quantity ?: 0

                    if (currentQtyInCart < product.stock) {
                        currentMap[trimmed] = CartItem(product, currentQtyInCart + 1)
                        _cart.value = currentMap
                        _scannedProductResult.value = product
                        _showToast.value = "Scanned: ${product.name} (Qty: ${currentQtyInCart + 1})"
                    } else {
                        _scanError.value = "Cannot add more. Stock limit: ${product.stock}"
                        _showToast.value = "Out of Stock limit for ${product.name}"
                    }
                } else {
                    _scanError.value = "Product '${product.name}' is out of stock!"
                    _showToast.value = "Out of stock!"
                }
            } else {
                _scanError.value = "Barcode '$trimmed' not found in store inventory!"
                _showToast.value = "Unknown barcode scanned: $trimmed"
            }
        }
    }

    fun modifyCartQuantity(barcode: String, delta: Int) {
        val currentMap = _cart.value.toMutableMap()
        val existing = currentMap[barcode] ?: return
        val newQty = existing.quantity + delta
        if (newQty <= 0) {
            currentMap.remove(barcode)
        } else {
            // check stock
            if (newQty <= existing.product.stock) {
                currentMap[barcode] = existing.copy(quantity = newQty)
            } else {
                _showToast.value = "Stock limit reached for ${existing.product.name}"
                return
            }
        }
        _cart.value = currentMap
    }

    fun removeFromCart(barcode: String) {
        val currentMap = _cart.value.toMutableMap()
        currentMap.remove(barcode)
        _cart.value = currentMap
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    // Begin payment process
    fun initiatePayment() {
        if (_cart.value.isEmpty()) {
            _showToast.value = "Your cart is empty!"
            return
        }
        _paymentState.value = PaymentUiState.WaitingForTap
    }

    fun cancelPayment() {
        _paymentState.value = PaymentUiState.Idle
    }

    // Core Payment Processing following an NFC Tap
    fun processNfcPayment(cardUid: String) {
        if (_paymentState.value != PaymentUiState.WaitingForTap && _paymentState.value !is PaymentUiState.Error) {
            return // not in paying mode
        }
        
        _paymentState.value = PaymentUiState.Processing
        viewModelScope.launch {
            val formattedUid = cardUid.trim().uppercase()
            val card = repository.getCardByUid(formattedUid)
            val totalCost = cartTotal.value

            if (card == null) {
                _paymentState.value = PaymentUiState.Error("Unrecognized NFC card ($formattedUid). Tap a registered card or add it as a new payment profile first.")
                _showToast.value = "NFC checkout failed: Card unrecognized"
                return@launch
            }

            if (card.balance >= totalCost) {
                // Deduct balance
                val newBalance = card.balance - totalCost
                val updatedCard = card.copy(balance = newBalance)
                repository.updateCard(updatedCard)

                // Deduct product inventory stock
                _cart.value.forEach { (_, cartItem) ->
                    val product = cartItem.product
                    val newStock = (product.stock - cartItem.quantity).coerceAtLeast(0)
                    repository.insertProduct(product.copy(stock = newStock))
                }

                // Generate receipt
                val receiptObj = Receipt(
                    transactionId = UUID.randomUUID().toString().take(12).uppercase(),
                    cardholderName = card.cardholderName,
                    cardUid = updatedCard.uid,
                    items = _cart.value.values.toList(),
                    subtotal = cartSubtotal.value,
                    tax = cartTax.value,
                    total = totalCost,
                    initialBalance = card.balance,
                    remainingBalance = newBalance
                )

                _paymentState.value = PaymentUiState.Success(receiptObj)
                _showToast.value = "Payment Successful with card: ${card.cardholderName}!"
                // Cart clearing is handled AFTER user dismisses / closes receipt or we can clear right away
                _cart.value = emptyMap()
            } else {
                _paymentState.value = PaymentUiState.Error(
                    "Insufficient funds on card ${card.cardholderName}.\n" +
                    "Price: $${String.format("%.2f", totalCost)}\n" +
                    "Balance: $${String.format("%.2f", card.balance)}"
                )
                _showToast.value = "NFC checkout failed: Insufficient funds"
            }
        }
    }

    // NFC Admin/Simulation controls
    fun saveNfcCard(uid: String, name: String, initialBalance: Double) {
        viewModelScope.launch {
            val formattedUid = uid.trim().uppercase()
            if (formattedUid.isEmpty() || name.trim().isEmpty()) {
                _showToast.value = "UID and Cardholder name cannot be empty!"
                return@launch
            }
            val card = NfcCardEntity(uid = formattedUid, cardholderName = name.trim(), balance = initialBalance)
            repository.insertCard(card)
            _showToast.value = "Card profile saved: ${card.cardholderName}!"
        }
    }

    fun reloadCardBalance(uid: String, amount: Double) {
        viewModelScope.launch {
            val formattedUid = uid.trim().uppercase()
            val existing = repository.getCardByUid(formattedUid)
            if (existing != null) {
                val newCard = existing.copy(balance = existing.balance + amount)
                repository.insertCard(newCard)
                _showToast.value = "Reloaded $${String.format("%.2f", amount)} successfully."
            } else {
                _showToast.value = "Card not found: $formattedUid"
            }
        }
    }

    // Inventory Controls
    fun addOrUpdateProduct(barcode: String, name: String, price: Double, stock: Int, category: String) {
        viewModelScope.launch {
            val trimmedBarcode = barcode.trim()
            val trimmedName = name.trim()
            val trimmedCategory = category.trim()

            if (trimmedBarcode.isEmpty() || trimmedName.isEmpty()) {
                _showToast.value = "Barcode and Name are required!"
                return@launch
            }
            if (price < 0.0 || stock < 0) {
                _showToast.value = "Ensure price and stock are 0 or greater!"
                return@launch
            }

            val existingProduct = repository.getProductByBarcode(trimmedBarcode)
            val updatedProduct = if (existingProduct != null) {
                existingProduct.copy(
                    name = trimmedName,
                    price = price,
                    stock = stock,
                    category = trimmedCategory
                )
            } else {
                ProductEntity(
                    barcode = trimmedBarcode,
                    name = trimmedName,
                    price = price,
                    stock = stock,
                    category = trimmedCategory
                )
            }

            repository.insertProduct(updatedProduct)
            _showToast.value = "Product saved successfully!"
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _showToast.value = "Deleted product: ${product.name}"
        }
    }

    fun dismissScannedResult() {
        _scannedProductResult.value = null
    }

    fun dismissScanError() {
        _scanError.value = null
    }

    fun clearToast() {
        _showToast.value = null
    }

    fun finishPaymentSession() {
        _paymentState.value = PaymentUiState.Idle
    }

    // --- SETTINGS STATES ---
    private val _currentTheme = MutableStateFlow(com.example.ui.theme.AppTheme.CYBERPUNK_DARK)
    val currentTheme: StateFlow<com.example.ui.theme.AppTheme> = _currentTheme.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isBeepEnabled = MutableStateFlow(true)
    val isBeepEnabled: StateFlow<Boolean> = _isBeepEnabled.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isScanTabVisible = MutableStateFlow(true)
    val isScanTabVisible: StateFlow<Boolean> = _isScanTabVisible.asStateFlow()

    private val _isCheckoutTabVisible = MutableStateFlow(true)
    val isCheckoutTabVisible: StateFlow<Boolean> = _isCheckoutTabVisible.asStateFlow()

    private val _isNfcTabVisible = MutableStateFlow(true)
    val isNfcTabVisible: StateFlow<Boolean> = _isNfcTabVisible.asStateFlow()

    private val _isInventoryTabVisible = MutableStateFlow(true)
    val isInventoryTabVisible: StateFlow<Boolean> = _isInventoryTabVisible.asStateFlow()

    fun updateTheme(theme: com.example.ui.theme.AppTheme) {
        _currentTheme.value = theme
    }

    fun updateDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    fun updateBeepEnabled(enabled: Boolean) {
        _isBeepEnabled.value = enabled
    }

    fun updateFrontCamera(enabled: Boolean) {
        _isFrontCamera.value = enabled
    }

    fun updateCameraEnabled(enabled: Boolean) {
        _isCameraEnabled.value = enabled
    }

    fun updateTabVisibility(tab: String, visible: Boolean) {
        viewModelScope.launch {
            val activeCount = listOf(
                _isScanTabVisible.value,
                _isCheckoutTabVisible.value,
                _isNfcTabVisible.value,
                _isInventoryTabVisible.value
            ).count { it }

            if (!visible && activeCount <= 1) {
                _showToast.value = "At least one navigation tab must remain active!"
                return@launch
            }

            when (tab) {
                "SCAN" -> _isScanTabVisible.value = visible
                "CHECKOUT" -> _isCheckoutTabVisible.value = visible
                "NFC" -> _isNfcTabVisible.value = visible
                "INVENTORY" -> _isInventoryTabVisible.value = visible
            }
        }
    }
}

class ShopViewModelFactory(private val repository: ShopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
