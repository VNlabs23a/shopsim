package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.ProductEntity
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.LocalAppThemeProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerSheet(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier,
    onNavigateToInventory: (String) -> Unit // if barcode not found, direct them to register it!
) {
    val themeProps = LocalAppThemeProperties.current
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val scannedProduct by viewModel.scannedProductResult.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    val isBeepEnabled by viewModel.isBeepEnabled.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsState()

    var customBarcode by remember { mutableStateOf("") }
    
    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Play a scanning beep音
    val playBeep = {
        if (isBeepEnabled) {
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(scannedProduct) {
        if (scannedProduct != null) {
            playBeep()
            kotlinx.coroutines.delay(1800L) // Show the success card for 1.8 seconds then auto dismiss
            viewModel.dismissScannedResult()
        }
    }

    LaunchedEffect(scanError) {
        if (scanError != null) {
            kotlinx.coroutines.delay(3000L) // Show error details for 3 seconds then auto dismiss
            viewModel.dismissScanError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(themeProps.containerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Title / Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "BARCODE SCANNER",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = themeProps.headerFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Scan products or enter codes to add to ticket",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scanner",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Surface Box (Real camera + Red Laser overlay + Sim option)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!isCameraEnabled) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Virtual Simulator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "VIRTUAL EMULATOR MODE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Live camera feed is turned off. Use simulator cards or type barcodes manually.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else if (hasCameraPermission) {
                CameraPreviewView(
                    isFrontCamera = isFrontCamera,
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeScanned = { barcode ->
                        if (scannedProduct == null && scanError == null) {
                            viewModel.addToCartByBarcode(barcode)
                        }
                    }
                )
                
                // Red Laser Sweep Animation
                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                val laserPercent by infiniteTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sweep"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val y = size.height * laserPercent
                            drawLine(
                                color = Color.Red,
                                start = Offset(20f, y),
                                end = Offset(size.width - 20f, y),
                                strokeWidth = 6f
                            )
                        }
                )

                // Corner brackets for scan viewfinder
                Box(
                    modifier = Modifier
                        .size(160.dp, 100.dp)
                        .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                )
            } else {
                // Request Permission UI
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Required",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera Access Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Grant camera privileges to simulate scanner via camera preview.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onSetTag = "grant_camera_btn",
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Enable Camera Feed")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Barcode Input
        OutlinedTextField(
            value = customBarcode,
            onValueChange = { customBarcode = it },
            label = { Text("Enter Barcode Digits Manually") },
            placeholder = { Text("e.g. 8801097250041") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (customBarcode.isNotBlank()) {
                        viewModel.addToCartByBarcode(customBarcode)
                        customBarcode = ""
                    }
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (customBarcode.isNotBlank()) {
                            viewModel.addToCartByBarcode(customBarcode)
                            customBarcode = ""
                        }
                    },
                    modifier = Modifier.testTag("manual_scan_submit")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item"
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_barcode_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scan Quick Simulator Panel
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "BARCODE SIMULATION PAD",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (products.isEmpty()) {
                Text(
                    text = "Populate inventory to see simulation buttons",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(products) { item ->
                        SimulationBarcodeCard(
                            product = item,
                            onClick = {
                                viewModel.addToCartByBarcode(item.barcode)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result / Feedback Message Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (scannedProduct != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SCANNED SUCCESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = scannedProduct!!.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Code: ${scannedProduct!!.barcode} • Price: $${String.format("%.2f", scannedProduct!!.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = { viewModel.dismissScannedResult() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("OK")
                        }
                    }
                }
            } else if (scanError != null) {
                val errorMsg = scanError!!
                val isNotFound = "not found" in errorMsg.lowercase()
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCAN STATE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.dismissScanError() }
                            ) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            if (isNotFound) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val extractedBarcode = errorMsg.split("'").getOrNull(1) ?: ""
                                Button(
                                    onClick = {
                                        viewModel.dismissScanError()
                                        onNavigateToInventory(extractedBarcode)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Add to Stock")
                                }
                            }
                        }
                    }
                }
            } else {
                // Default Scanner Help Screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "READY TO SCAN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Items added appear automatically in the checkout cart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Customized Button extensions for test tagging compliance
@Composable
fun Button(
    onSetTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        colors = colors,
        modifier = modifier.testTag(onSetTag),
        content = content
    )
}

@Composable
fun SimulationBarcodeCard(
    product: ProductEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .wrapContentWidth()
            .border(
                1.dp,
                if (product.stock == 0) Color.Gray.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .testTag("scan_sim_card_${product.barcode}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Render barcode visual bars
            Row(
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                BarcodeVisualRepresentation()
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${String.format("%.2f", product.price)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Stock: ${product.stock}",
                style = MaterialTheme.typography.labelSmall,
                color = if (product.stock == 0) Color.Red else Color.Gray
            )
        }
    }
}

@Composable
fun BarcodeVisualRepresentation() {
    val barWeights = listOf(2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2)
    Row(modifier = Modifier.height(20.dp)) {
        barWeights.forEachIndexed { idx, weight ->
            Spacer(
                modifier = Modifier
                    .width(weight.dp)
                    .fillMaxHeight()
                    .background(if (idx % 2 == 0) Color.Black else Color.White)
            )
        }
    }
}

@Composable
fun CameraPreviewView(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var resolvedCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var lastScannedBarcode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                resolvedCameraProvider?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    resolvedCameraProvider = cameraProvider
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    val barcodeScanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue ?: barcode.displayValue
                                        if (rawValue != null) {
                                            val currentTime = System.currentTimeMillis()
                                            if (rawValue != lastScannedBarcode || currentTime - lastScannedTime > 2000L) {
                                                lastScannedBarcode = rawValue
                                                lastScannedTime = currentTime
                                                onBarcodeScanned(rawValue)
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    it.printStackTrace()
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    
                    if (cameraProvider.hasCamera(cameraSelector)) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analysis
                        )
                    } else {
                        val fallbackSelector = if (isFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                        if (cameraProvider.hasCamera(fallbackSelector)) {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                fallbackSelector,
                                preview,
                                analysis
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}
