package com.quangthe.thuocdo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quangthe.thuocdo.service.OverlayService
import com.quangthe.thuocdo.ui.theme.ThuocDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThuocDoTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val rulerPrefs = remember { context.getSharedPreferences("ruler_prefs", Context.MODE_PRIVATE) }
    
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isServiceRunning by remember { mutableStateOf(false) }
    
    // Đồng bộ các giá trị từ SharedPreferences khi chúng thay đổi từ phía Overlay
    var rulerScale by remember { mutableFloatStateOf(rulerPrefs.getFloat("sc", 1.0f)) }
    
    DisposableEffect(rulerPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "sc") {
                rulerScale = prefs.getFloat("sc", 1.0f)
            }
        }
        rulerPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            rulerPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Thước đo Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Service Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.BubbleChart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Column {
                                Text("Dịch vụ Bong bóng nổi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(if (isServiceRunning) "Dịch vụ đang Chạy" else "Dịch vụ đã Dừng", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = if (isServiceRunning) Color.Gray else Color.Red)
                            }
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = {
                                if (it) {
                                    if (hasOverlayPermission) {
                                        context.startService(Intent(context, OverlayService::class.java))
                                        isServiceRunning = true
                                    } else {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        context.startActivity(intent)
                                    }
                                } else {
                                    context.stopService(Intent(context, OverlayService::class.java))
                                    isServiceRunning = false
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("Nhấn: Bật/Tắt Thước | Giữ: Cài đặt", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            Text("CẤU HÌNH THƯỚC", style = MaterialTheme.typography.labelLarge, color = Color.Gray, letterSpacing = 1.sp)

            var numRulers by remember { mutableIntStateOf(rulerPrefs.getInt("num_rulers", 2)) }
            var isCoupled by remember { mutableStateOf(rulerPrefs.getBoolean("is_coupled", true)) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Số lượng thước", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            FilterChip(
                                selected = numRulers == 1,
                                onClick = { numRulers = 1; rulerPrefs.edit { putInt("num_rulers", 1) } },
                                label = { Text("1 cây") }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = numRulers == 2,
                                onClick = { numRulers = 2; rulerPrefs.edit { putInt("num_rulers", 2) } },
                                label = { Text("2 cây") }
                            )
                        }
                    }

                    if (numRulers == 2) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.LightGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Ghép 2 thước", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = isCoupled,
                                onCheckedChange = {
                                    isCoupled = it
                                    rulerPrefs.edit { putBoolean("is_coupled", it) }
                                }
                            )
                        }
                    }
                }
            }

            // Ruler Unit Selector
            var rulerUnit by remember { mutableIntStateOf(rulerPrefs.getInt("un", 0)) }
            var isZoomEnabled by remember { mutableStateOf(rulerPrefs.getBoolean("is_zoom_enabled", true)) }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Đơn vị đo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val units = listOf("dp", "cm", "px", "sp")
                        units.forEachIndexed { i, label ->
                            FilterChip(
                                selected = rulerUnit == i,
                                onClick = {
                                    rulerUnit = i
                                    rulerPrefs.edit { putInt("un", i) }
                                },
                                label = { Text(label, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Thu phóng: ${String.format("%.1f", rulerScale)}x", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Bật/Tắt", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Switch(
                                checked = isZoomEnabled,
                                onCheckedChange = {
                                    isZoomEnabled = it
                                    rulerPrefs.edit { 
                                        putBoolean("is_zoom_enabled", it)
                                        if (!it) {
                                            // Khi tắt: lưu lại tỷ lệ hiện tại và đưa về 1.0x
                                            putFloat("saved_sc", rulerScale)
                                            putFloat("sc", 1.0f)
                                        } else {
                                            // Khi bật: khôi phục lại tỷ lệ đã lưu
                                            val saved = rulerPrefs.getFloat("saved_sc", 1.0f)
                                            putFloat("sc", saved)
                                        }
                                        apply()
                                    }
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                    
                    if (isZoomEnabled) {
                        Slider(
                            value = rulerScale,
                            onValueChange = { 
                                rulerScale = it
                                rulerPrefs.edit { putFloat("sc", it) } 
                            },
                            valueRange = 0.25f..4.0f,
                            steps = 14
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
                    Button(
                        onClick = { 
                            rulerPrefs.edit {
                                putFloat("hx", 150f); putFloat("hy", 150f); putFloat("hrot", 0f)
                                putFloat("vx", 150f); putFloat("vy", 300f); putFloat("vrot", 90f)
                                putFloat("sc", 1.0f); putFloat("saved_sc", 1.0f)
                                putFloat("bl", 600 * context.resources.displayMetrics.density)
                                apply()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Đặt lại vị trí & Kích thước")
                    }

                    if (numRulers == 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
                        Text("CỐ ĐỊNH HƯỚNG", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        
                        var fixedOrientation by remember { mutableIntStateOf(rulerPrefs.getInt("fixed_orientation", 0)) }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = fixedOrientation == 1,
                                onClick = {
                                    fixedOrientation = if (fixedOrientation == 1) 0 else 1
                                    rulerPrefs.edit { putInt("fixed_orientation", fixedOrientation) }
                                },
                                label = { Text("Nằm ngang (0°)") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (fixedOrientation == 1) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                            )
                            FilterChip(
                                selected = fixedOrientation == 2,
                                onClick = {
                                    fixedOrientation = if (fixedOrientation == 2) 0 else 2
                                    rulerPrefs.edit { putInt("fixed_orientation", fixedOrientation) }
                                },
                                label = { Text("Thẳng đứng (90°)") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (fixedOrientation == 2) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Quyền hiển thị trên ứng dụng khác", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (hasOverlayPermission) Color(0xFF4CAF50) else Color.Red, modifier = Modifier.size(18.dp))
                        Text(if (hasOverlayPermission) "Đã cấp" else "Chưa cấp", style = MaterialTheme.typography.labelMedium, color = if (hasOverlayPermission) Color(0xFF8D6E63) else Color.Red)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Phiên bản 2.5.0 (Pro Edition)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}


