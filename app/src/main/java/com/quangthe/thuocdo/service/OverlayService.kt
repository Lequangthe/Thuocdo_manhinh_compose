package com.quangthe.thuocdo.service

import android.annotation.SuppressLint
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
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.quangthe.thuocdo.MainActivity
import com.quangthe.thuocdo.R
import com.quangthe.thuocdo.ui.overlay.RulerOverlayView
import kotlin.math.abs
import kotlin.math.hypot

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var closeTargetView: View? = null
    private var rulerView: RulerOverlayView? = null
    private var isRulerMode = false

    // Thông số cho Bong bóng
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

    // Thông số cho biểu tượng đóng (X)
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
        private const val CLOSE_TARGET_Y_OFFSET = 150
        private const val CLOSE_TARGET_HALF_HEIGHT_DP = 40
        private const val CLOSE_PROXIMITY_THRESHOLD = 300.0
        private const val CLOSE_ACTIVATE_THRESHOLD = 200.0
        private const val EDGE_SNAP = 30
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        createNotificationChannel()
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        setupBubble()
        setupCloseTarget()
    }

    @SuppressLint("InflateParams")
    private fun setupCloseTarget() {
        closeTargetView = LayoutInflater.from(this).inflate(R.layout.layout_close_target, null)
        closeTargetView?.visibility = View.GONE
        windowManager.addView(closeTargetView, closeTargetParams)
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_bubble, null)
        
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleRulerMode()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleRulerMode()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                openMainApp()
            }
        })

        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                // Ưu tiên xử lý Tap và Double Tap
                if (gestureDetector.onTouchEvent(event)) return true

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        
                        // Hiện 'X' khi đang kéo
                        closeTargetView?.visibility = View.VISIBLE
                        checkProximityToClose(event.rawX, event.rawY)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        closeTargetView?.visibility = View.GONE
                        
                        if (isOverCloseTarget(event.rawX, event.rawY)) {
                            stopSelf()
                        } else {
                            clampBubblePosition()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
        Toast.makeText(this, "Opening app settings...", Toast.LENGTH_SHORT).show()
    }

    private fun checkProximityToClose(touchX: Float, touchY: Float) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val targetX = screenWidth / 2f
        val targetY = screenHeight - CLOSE_TARGET_Y_OFFSET.toFloat() - CLOSE_TARGET_HALF_HEIGHT_DP.dpToPx()
        
        val dist = hypot((touchX - targetX).toDouble(), (touchY - targetY).toDouble())
        if (dist < CLOSE_PROXIMITY_THRESHOLD) {
            closeTargetView?.scaleX = 1.6f
            closeTargetView?.scaleY = 1.6f
            closeTargetView?.alpha = 1.0f
        } else {
            closeTargetView?.scaleX = 1.0f
            closeTargetView?.scaleY = 1.0f
            closeTargetView?.alpha = 0.6f
        }
    }

    private fun isOverCloseTarget(touchX: Float, touchY: Float): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val targetX = screenWidth / 2f
        val targetY = screenHeight - CLOSE_TARGET_Y_OFFSET.toFloat() - CLOSE_TARGET_HALF_HEIGHT_DP.dpToPx()
        
        val dist = hypot((touchX - targetX).toDouble(), (touchY - targetY).toDouble())
        return dist < CLOSE_ACTIVATE_THRESHOLD
    }

    private fun clampBubblePosition() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val bubbleWidth = bubbleView?.width ?: 0
        bubbleParams.x = bubbleParams.x.coerceIn(-(bubbleWidth - EDGE_SNAP), screenWidth - EDGE_SNAP)
        bubbleParams.y = bubbleParams.y.coerceIn(0, displayMetrics.heightPixels - EDGE_SNAP)
        windowManager.updateViewLayout(bubbleView, bubbleParams)
    }

    private val rulerParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    )

    private fun toggleRulerMode() {
        isRulerMode = !isRulerMode
        
        if (isRulerMode) {
            if (rulerView == null) {
                rulerView = RulerOverlayView(this)
                windowManager.addView(rulerView, rulerParams)
                bubbleView?.let {
                    windowManager.removeView(it)
                    windowManager.addView(it, bubbleParams)
                }
            }
            Toast.makeText(this, "Ruler ON", Toast.LENGTH_SHORT).show()
        } else {
            hideRuler()
            Toast.makeText(this, "Ruler OFF", Toast.LENGTH_SHORT).show()
        }
        
        updateBubbleIcon()
    }

    private fun updateBubbleIcon() {
        val icon = bubbleView?.findViewById<ImageView>(R.id.bubble_icon)
        if (isRulerMode) {
            icon?.setColorFilter(resources.getColor(android.R.color.holo_green_light, theme))
        } else {
            icon?.clearColorFilter()
        }
    }

    private fun hideRuler() {
        rulerView?.let {
            windowManager.removeView(it)
            rulerView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager.removeView(it) }
        closeTargetView?.let { windowManager.removeView(it) }
        hideRuler()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Thước đo màn hình Active")
            .setContentText("Nhấn: Bật/Tắt Thước | Giữ: Cài đặt")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
