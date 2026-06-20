package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppThemeProperties
import com.example.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val themeProps = LocalAppThemeProperties.current
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isBeepEnabled by viewModel.isBeepEnabled.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsState()

    val isScanTabVisible by viewModel.isScanTabVisible.collectAsState()
    val isCheckoutTabVisible by viewModel.isCheckoutTabVisible.collectAsState()
    val isNfcTabVisible by viewModel.isNfcTabVisible.collectAsState()
    val isInventoryTabVisible by viewModel.isInventoryTabVisible.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
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
                    text = "APPLICATION SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = themeProps.headerFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Configure visual styles, hardware options, and tabs",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = themeProps.bodyFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 1: VISUAL THEMES
        SettingsSectionHeader(title = "DESIGN & COLOR SCHEMES", icon = Icons.Default.Palette)
        Spacer(modifier = Modifier.height(12.dp))

        // Theme cards choice
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ThemeChoiceItem(
                title = "Cyberpunk Default",
                description = "Vibrant neon mint, cyan, & sleek luxury carbon black",
                theme = AppTheme.CYBERPUNK_DARK,
                colors = listOf(Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFF1B2026)),
                selected = currentTheme == AppTheme.CYBERPUNK_DARK,
                onClick = { viewModel.updateTheme(AppTheme.CYBERPUNK_DARK) },
                modifier = Modifier.testTag("theme_cyberpunk_btn")
            )

            ThemeChoiceItem(
                title = "Material 3 Expressive",
                description = "Rich aesthetic terracotta, warm energetic velvet & high contrast",
                theme = AppTheme.M3_EXPRESSIVE,
                colors = listOf(Color(0xFFE040FB), Color(0xFFFF6D00), Color(0xFF1D1433)),
                selected = currentTheme == AppTheme.M3_EXPRESSIVE,
                onClick = { viewModel.updateTheme(AppTheme.M3_EXPRESSIVE) },
                modifier = Modifier.testTag("theme_expressive_btn")
            )

            ThemeChoiceItem(
                title = "Ocean Breeze",
                description = "Refreshing dark teal, seaside foam & crystal blue",
                theme = AppTheme.OCEAN_BREEZE,
                colors = listOf(Color(0xFF26A69A), Color(0xFF4DB6AC), Color(0xFF002F37)),
                selected = currentTheme == AppTheme.OCEAN_BREEZE,
                onClick = { viewModel.updateTheme(AppTheme.OCEAN_BREEZE) },
                modifier = Modifier.testTag("theme_ocean_btn")
            )

            ThemeChoiceItem(
                title = "Sunset Ember",
                description = "Cozy sunset terracotta, warm gold embers & wine wine twilight",
                theme = AppTheme.SUNSET_EMBER,
                colors = listOf(Color(0xFFFF7043), Color(0xFFFFD54F), Color(0xFF2D181C)),
                selected = currentTheme == AppTheme.SUNSET_EMBER,
                onClick = { viewModel.updateTheme(AppTheme.SUNSET_EMBER) },
                modifier = Modifier.testTag("theme_sunset_btn")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dark / Light Theme toggle
        SettingsToggleRow(
            title = "Dark Aesthetic Scheme",
            subtitle = "Enable gorgeous atmospheric dark backgrounds",
            checked = isDarkTheme,
            onCheckedChange = { viewModel.updateDarkTheme(it) },
            modifier = Modifier.testTag("toggle_dark_theme")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: BARCODE & HARDWARE
        SettingsSectionHeader(title = "HARDWARE & SOUNDS", icon = Icons.Default.CameraAlt)
        Spacer(modifier = Modifier.height(12.dp))

        // Scanner Camera Feed toggle
        SettingsToggleRow(
            title = "Live Camera Preview Feed",
            subtitle = "Enable camera viewfinder for physical barcodes (Turn off on emulators / virtual platforms)",
            checked = isCameraEnabled,
            onCheckedChange = { viewModel.updateCameraEnabled(it) },
            modifier = Modifier.testTag("toggle_camera_enabled")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scanner Beep toggle
        SettingsToggleRow(
            title = "Scanning Bleep Notification",
            subtitle = "Emit high-tempo audio alert upon successful barcode scan",
            checked = isBeepEnabled,
            onCheckedChange = { viewModel.updateBeepEnabled(it) },
            modifier = Modifier.testTag("toggle_beep_sound")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Camera facing selector
        SettingsToggleRow(
            title = "Use Front Camera",
            subtitle = "Utilize front camera lens instead of back lens for scans",
            checked = isFrontCamera,
            onCheckedChange = { viewModel.updateFrontCamera(it) },
            modifier = Modifier.testTag("toggle_camera_facing")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: NAVIGATION TABS CONFIG
        SettingsSectionHeader(title = "NAVIGATION BAR TABS", icon = Icons.Default.ViewAgenda)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Customize bottom menu tabs. At least one tab must remain active.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabVisibilityRow(
                    title = "Scan Register",
                    visible = isScanTabVisible,
                    onToggle = { viewModel.updateTabVisibility("SCAN", it) },
                    modifier = Modifier.testTag("toggle_tab_scan")
                )
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                TabVisibilityRow(
                    title = "Checkout Cart",
                    visible = isCheckoutTabVisible,
                    onToggle = { viewModel.updateTabVisibility("CHECKOUT", it) },
                    modifier = Modifier.testTag("toggle_tab_checkout")
                )
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                TabVisibilityRow(
                    title = "NFC Prepaid Payment",
                    visible = isNfcTabVisible,
                    onToggle = { viewModel.updateTabVisibility("NFC", it) },
                    modifier = Modifier.testTag("toggle_tab_nfc")
                )
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                TabVisibilityRow(
                    title = "Stock Inventory",
                    visible = isInventoryTabVisible,
                    onToggle = { viewModel.updateTabVisibility("INVENTORY", it) },
                    modifier = Modifier.testTag("toggle_tab_inventory")
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ThemeChoiceItem(
    title: String,
    description: String,
    theme: AppTheme,
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Palette preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun TabVisibilityRow(
    title: String,
    visible: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Checkbox(
            checked = visible,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
    }
}
