package com.quangthe.thuocdo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.quangthe.thuocdo.MainActivity
import com.quangthe.thuocdo.data.RulerRepository
import com.quangthe.thuocdo.model.RulerState
import com.quangthe.thuocdo.ui.overlay.BubbleComponent
import com.quangthe.thuocdo.ui.overlay.RulerComponent
import com.quangthe.thuocdo.ui.theme.ThuocDoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.hypot

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    @Inject
    lateinit var repository: RulerRepository

    private lateinit var windowManager: WindowManager
    private var bubbleComposeView: ComposeView? = null
    private var rulerComposeView: ComposeView? = null
    private var closeTargetComposeView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performAttach()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val bubbleParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 300
    }

    private val rulerParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    )

    private val closeTargetParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 150
    }

    companion object {
        private const val CHANNEL_ID = "OverlayServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("OverlayService", "onCreate called")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                createNotification(), 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        setupBubble()
        setupRuler()
        setupCloseTarget()
        
        observeState()
    }

    private fun observeState() {
        serviceScope.launch {
            repository.rulerStateFlow.collect { state ->
                Log.d("OverlayService", "State updated: visible=${state.isRulerVisible}")
                bubbleParams.x = state.bubbleX
                bubbleParams.y = state.bubbleY
                bubbleComposeView?.let {
                    if (it.isAttachedToWindow) {
                        windowManager.updateViewLayout(it, bubbleParams)
                    }
                }
                
                if (state.isRulerVisible) {
                    if (rulerComposeView?.parent == null) {
                        Log.d("OverlayService", "Adding ruler view")
                        rulerComposeView?.let { windowManager.addView(it, rulerParams) }
                        bubbleComposeView?.let {
                            windowManager.removeView(it)
                            windowManager.addView(it, bubbleParams)
                        }
                    }
                } else {
                    if (rulerComposeView?.parent != null) {
                        Log.d("OverlayService", "Removing ruler view")
                        windowManager.removeView(rulerComposeView)
                    }
                }
            }
        }
    }

    private fun setupBubble() {
        Log.d("OverlayService", "setupBubble")
        bubbleComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            
            setContent {
                val state by repository.rulerStateFlow.collectAsState(initial = RulerState())
                
                ThuocDoTheme {
                    BubbleComponent(
                        isRulerActive = state.isRulerVisible,
                        onTap = { 
                            Log.d("OverlayService", "Bubble tapped")
                            serviceScope.launch { repository.toggleRulerVisibility() } 
                        },
                        onLongPress = { openMainApp() },
                        onDrag = { dx, dy ->
                            bubbleParams.x += dx.toInt()
                            bubbleParams.y += dy.toInt()
                            windowManager.updateViewLayout(this, bubbleParams)
                            
                            updateCloseTargetProximity(bubbleParams.x.toFloat(), bubbleParams.y.toFloat())
                            closeTargetComposeView?.visibility = View.VISIBLE
                        },
                        onDragEnd = {
                            closeTargetComposeView?.visibility = View.GONE
                            
                            if (isOverCloseTarget(bubbleParams.x.toFloat(), bubbleParams.y.toFloat())) {
                                stopSelf()
                            } else {
                                val screenWidth = resources.displayMetrics.widthPixels
                                val bubbleWidth = (56 * resources.displayMetrics.density).toInt()
                                if (bubbleParams.x < screenWidth / 2) {
                                    bubbleParams.x = 0
                                } else {
                                    bubbleParams.x = screenWidth - bubbleWidth
                                }
                                windowManager.updateViewLayout(this, bubbleParams)
                                
                                serviceScope.launch { 
                                    repository.updateBubblePosition(bubbleParams.x, bubbleParams.y)
                                }
                            }
                        }
                    )
                }
            }
        }
        windowManager.addView(bubbleComposeView, bubbleParams)
    }

    private val closeProximityScale = mutableFloatStateOf(1f)
    private val closeProximityAlpha = mutableFloatStateOf(0.6f)

    private fun setupCloseTarget() {
        closeTargetComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            visibility = View.GONE
            
            setContent {
                val scale by closeProximityScale
                val alpha by closeProximityAlpha
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(60.dp)) {
                        drawCircle(color = Color.Black, alpha = alpha * 0.5f)
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = alpha),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        windowManager.addView(closeTargetComposeView, closeTargetParams)
    }

    private fun updateCloseTargetProximity(x: Float, y: Float) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val targetX = screenWidth / 2f
        val targetY = screenHeight - 150f - (40 * resources.displayMetrics.density)
        
        val dist = hypot((x - targetX).toDouble(), (y - targetY).toDouble())
        if (dist < 300.0) {
            closeProximityScale.floatValue = 1.6f
            closeProximityAlpha.floatValue = 1.0f
        } else {
            closeProximityScale.floatValue = 1.0f
            closeProximityAlpha.floatValue = 0.6f
        }
    }

    private fun isOverCloseTarget(x: Float, y: Float): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val targetX = screenWidth / 2f
        val targetY = screenHeight - 150f - (40 * resources.displayMetrics.density)
        
        val dist = hypot((x - targetX).toDouble(), (y - targetY).toDouble())
        return dist < 200.0
    }

    private fun setupRuler() {
        Log.d("OverlayService", "setupRuler")
        rulerComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            
            setContent {
                val state by repository.rulerStateFlow.collectAsState(initial = RulerState())
                ThuocDoTheme {
                    RulerComponent(
                        state = state,
                        onUpdateState = { newState ->
                            serviceScope.launch { repository.saveAll(newState) }
                        }
                    )
                }
            }
        }
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        serviceScope.cancel()
        bubbleComposeView?.let { if (it.parent != null) windowManager.removeView(it) }
        rulerComposeView?.let { if (it.parent != null) windowManager.removeView(it) }
        closeTargetComposeView?.let { if (it.parent != null) windowManager.removeView(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Thước đo Pro")
            .setContentText("Dịch vụ bong bóng đang hoạt động")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
