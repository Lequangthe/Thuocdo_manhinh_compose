package com.quangthe.thuocdo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthe.thuocdo.service.OverlayService
import com.quangthe.thuocdo.ui.RulerViewModel
import com.quangthe.thuocdo.ui.theme.ThuocDoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: RulerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThuocDoTheme {
                MainScreen(viewModel)
            }
        }
    }
}

private fun isServiceRunning(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (OverlayService::class.java.name == service.service.className) {
            return true
        }
    }
    return false
}

@Composable
fun MainScreen(viewModel: RulerViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isServiceActive by remember { mutableStateOf(isServiceRunning(context)) }
    
    // Đồng bộ lại trạng thái quyền và service khi quay lại app
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                isServiceActive = isServiceRunning(context)
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
                                Text(if (isServiceActive) "Dịch vụ đang Chạy" else "Dịch vụ đã Dừng", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = if (isServiceActive) Color(0xFF4CAF50) else Color.Red)
                            }
                        }
                        Switch(
                            checked = isServiceActive,
                            onCheckedChange = {
                                if (it) {
                                    if (hasOverlayPermission) {
                                        context.startService(Intent(context, OverlayService::class.java))
                                        isServiceActive = true
                                    } else {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        context.startActivity(intent)
                                    }
                                } else {
                                    context.stopService(Intent(context, OverlayService::class.java))
                                    isServiceActive = false
                                }
                            }
                        )
                    }
                }
            }

            Text("CẤU HÌNH THƯỚC", style = MaterialTheme.typography.labelLarge, color = Color.Gray, letterSpacing = 1.sp)

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
                                selected = uiState.numRulers == 1,
                                onClick = { viewModel.updateNumRulers(1) },
                                label = { Text("1 cây") }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = uiState.numRulers == 2,
                                onClick = { viewModel.updateNumRulers(2) },
                                label = { Text("2 cây") }
                            )
                        }
                    }

                    if (uiState.numRulers == 2) {
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
                                checked = uiState.isCoupled,
                                onCheckedChange = { viewModel.toggleCoupled(it) }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Định hướng thước", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val orientations = listOf("Tự do" to 0, "Nằm ngang" to 1, "Thẳng đứng" to 2)
                        orientations.forEach { (label, value) ->
                            FilterChip(
                                selected = uiState.fixedOrientation == value,
                                onClick = { viewModel.updateFixedOrientation(value) },
                                label = { Text(label, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (uiState.numRulers == 2) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Định hướng chỉ áp dụng khi dùng 1 thước (khoá góc xoay)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

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
                                selected = uiState.unit == i,
                                onClick = { viewModel.updateUnit(i) },
                                label = { Text(label, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Thu phóng: ${String.format("%.1f", uiState.scale)}x", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = uiState.isZoomEnabled,
                            onCheckedChange = { viewModel.toggleZoom(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    
                    if (uiState.isZoomEnabled) {
                        Slider(
                            value = uiState.scale,
                            onValueChange = { viewModel.updateScale(it) },
                            valueRange = 0.25f..4.0f,
                            steps = 14
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
                    Text("ĐỊNH HƯỚNG THƯỚC", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.numRulers == 1) {
                            // Chế độ 1 thước
                            FilterChip(
                                selected = uiState.fixedOrientation == 0,
                                onClick = { viewModel.updateFixedOrientation(0) },
                                label = { Text("Xoay tự do", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.fixedOrientation == 1,
                                onClick = { viewModel.updateFixedOrientation(1) },
                                label = { Text("Ngang (0°)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.fixedOrientation == 2,
                                onClick = { viewModel.updateFixedOrientation(2) },
                                label = { Text("Dọc (90°)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Chế độ 2 thước
                            FilterChip(
                                selected = uiState.fixedOrientation == 0,
                                onClick = { viewModel.updateFixedOrientation(0) },
                                label = { Text("Xoay tự do", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (uiState.fixedOrientation == 0) { { Icon(Icons.Default.ScreenRotation, null, Modifier.size(16.dp)) } } else null
                            )
                            FilterChip(
                                selected = uiState.fixedOrientation == 1,
                                onClick = { viewModel.updateFixedOrientation(1) },
                                label = { Text("Khóa góc (0° & 90°)", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (uiState.fixedOrientation == 1) { { Icon(Icons.Default.Lock, null, Modifier.size(16.dp)) } } else null
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
                    Button(
                        onClick = { viewModel.resetSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Đặt lại toàn bộ")
                    }
                }
            }
        }
    }
}
