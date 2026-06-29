package com.quangthe.thuocdo.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.*

class RulerOverlayView(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val screenW = context.resources.displayMetrics.widthPixels
    private val screenH = context.resources.displayMetrics.heightPixels
    private val xdpi = context.resources.displayMetrics.xdpi

    private val barThick = (48 * density)
    private val hndlR = (16 * density)
    private val majTickH = (20 * density)
    private val minTickH = (12 * density)

    private var hX = 150f; private var hY = 150f; private var hRot = 0f
    private var vX = 150f; private var vY = 300f; private var vRot = 90f
    private var barLen = 600 * density
    private var scale = 1.0f
    private var unit = 0
    private var isCoupled = true
    private var numRulers = 2
    private var isZoomEnabled = true
    private var fixedOrientation = 0 // 0: none, 1: horizontal (0deg), 2: vertical (90deg)

    private var hS = 0f; private var hE = 300 * density
    private var vS = 0f; private var vE = 300 * density

    private var mode = MODE_NONE
    private var lastX = 0f; private var lastY = 0f
    private var pinchDist = 0f; private var lastAngle = 0f

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG_H = 1; private const val MODE_DRAG_V = 11
        private const val MODE_HS = 2; private const val MODE_HE = 3
        private const val MODE_VS = 4; private const val MODE_VE = 5
        private const val MODE_RESIZE_H = 6; private const val MODE_RESIZE_V = 7
        private const val MODE_PINCH = 10
    }

    private val bg = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; setShadowLayer(16f, 0f, 4f, Color.parseColor("#60000000")) }
    private val brd = Paint().apply { color = Color.parseColor("#BDBDBD"); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val tMaj = Paint().apply { color = Color.BLACK; strokeWidth = 3f; isAntiAlias = true }
    private val tMin = Paint().apply { color = Color.parseColor("#616161"); strokeWidth = 2f; isAntiAlias = true }
    private val tLbl = Paint().apply { color = Color.BLACK; textSize = 24 * density; isAntiAlias = true }
    private val hBg = Paint().apply { color = Color.parseColor("#804CAF50"); style = Paint.Style.FILL }
    private val hBrd = Paint().apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val vBg = Paint().apply { color = Color.parseColor("#804CAF50"); style = Paint.Style.FILL }
    private val vBrd = Paint().apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val hndl = Paint().apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.FILL }
    private val hndlS = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val mTxt = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 28 * density; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val anglePaint = Paint().apply { color = Color.parseColor("#E91E63"); textSize = 20 * density; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }

    private val prefs = context.getSharedPreferences("ruler_prefs", Context.MODE_PRIVATE)
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> loadPrefs(); invalidate() }

    private val touchSlop: Float
    private val resizeSlop: Float

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val config = ViewConfiguration.get(context)
        touchSlop = config.scaledTouchSlop.toFloat() * 5f
        resizeSlop = config.scaledTouchSlop.toFloat() * 8f
        loadPrefs()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadPrefs() {
        numRulers = prefs.getInt("num_rulers", 2)
        isCoupled = prefs.getBoolean("is_coupled", true)
        isZoomEnabled = prefs.getBoolean("is_zoom_enabled", true)
        fixedOrientation = prefs.getInt("fixed_orientation", 0)
        hX = prefs.getFloat("hx", 150f); hY = prefs.getFloat("hy", 150f); hRot = prefs.getFloat("hrot", 0f)
        vX = prefs.getFloat("vx", 150f); vY = prefs.getFloat("vy", 300f); vRot = prefs.getFloat("vrot", 90f)
        
        if (numRulers == 1) {
            if (fixedOrientation == 1) hRot = 0f
            else if (fixedOrientation == 2) hRot = 90f
        }
        scale = prefs.getFloat("sc", 1.0f); unit = prefs.getInt("un", 0)
        hE = prefs.getFloat("he", 300 * density); vE = prefs.getFloat("ve", 300 * density)
        barLen = prefs.getFloat("bl", 600 * density)
        hS = prefs.getFloat("hs", 0f); vS = prefs.getFloat("vs", 0f)
        
        if (numRulers == 2 && isCoupled) {
            vX = hX; vY = hY; vRot = hRot + 90f
        }
    }

    private fun savePrefs() {
        prefs.edit().apply {
            putFloat("hx", hX); putFloat("hy", hY); putFloat("hrot", hRot)
            putFloat("vx", vX); putFloat("vy", vY); putFloat("vrot", vRot)
            putFloat("sc", scale); putInt("un", unit)
            putFloat("he", hE); putFloat("ve", vE)
            putFloat("hs", hS); putFloat("vs", vS); putFloat("bl", barLen)
            putInt("fixed_orientation", fixedOrientation)
            apply()
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val len = barLen * scale

        // Vẽ miếng đệm góc (corner meat) khi ghép thước
        if (numRulers == 2 && isCoupled) {
            canvas.save()
            canvas.translate(hX, hY)
            canvas.rotate(hRot)
            val cornerSize = barThick
            val cornerRect = RectF(-cornerSize, -cornerSize, 16f, 16f)
            canvas.drawRoundRect(cornerRect, 24f, 24f, bg)
            canvas.drawRoundRect(cornerRect, 24f, 24f, brd)
            canvas.restore()
        }

        if (numRulers >= 1) {
            drawRulerBar(canvas, hX, hY, hRot, len, hS, hE, hBg, hBrd, false)
        }
        if (numRulers == 2) {
            if (isCoupled) {
                drawRulerBar(canvas, hX, hY, hRot + 90f, len, vS, vE, vBg, vBrd, true)
            } else {
                drawRulerBar(canvas, vX, vY, vRot, len, vS, vE, vBg, vBrd, false)
            }
        }
    }

    private fun drawRulerBar(c: Canvas, x: Float, y: Float, rot: Float, len: Float, s: Float, e: Float, mBg: Paint, mBrd: Paint, flipScale: Boolean) {
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        
        val barRect = if (!flipScale) RectF(0f, -barThick, len, 0f) else RectF(0f, 0f, len, barThick)
        c.drawRoundRect(barRect, 16f, 16f, bg)
        c.drawRoundRect(barRect, 16f, 16f, brd)

        // Vẽ chỉ số góc nghiêng (Angle)
        val angleTxt = "${rot.toInt() % 360}°"
        val angleY = if (!flipScale) -barThick - 10f else barThick + 30f * density
        c.drawText(angleTxt, 10f, angleY, anglePaint)

        val unitsPerMaj = when (unit) {
            1 -> 1f      // 1 cm mỗi số
            2 -> 50f     // 50 px mỗi số (cho thoáng hơn 100)
            else -> 10f   // 10 dp/sp mỗi số (0 10 20... đúng kiểu học sinh)
        }

        val pixelsPerUnit = when (unit) {
            1 -> xdpi / 2.54f
            2 -> 1f
            else -> density
        }

        val majStep = pixelsPerUnit * unitsPerMaj * scale
        val minStep = majStep / 10f
        val nTicks = (len / majStep).toInt() + 1
        
        val labelStep = if (unit == 1) 1 else 5 // cm: mọi vạch, khác: mỗi 5 vạch
        for (i in 0 until nTicks) {
            val tx = i * majStep
            val tickY = if (!flipScale) -majTickH else majTickH
            c.drawLine(tx, 0f, tx, tickY, tMaj)
            if (i % labelStep == 0) {
                val lbl = "${i / labelStep * labelStep}"
                val b = Rect(); tLbl.getTextBounds(lbl, 0, lbl.length, b)
                val lblY = if (!flipScale) -majTickH - 6f else majTickH + b.height() + 6f
                c.drawText(lbl, tx - b.width() / 2f, lblY, tLbl)
            }
            for (j in 1..9) {
                val mx = tx + j * minStep
                if (mx < len) {
                    val h = if (j == 5) minTickH * 1.5f else minTickH
                    val mTickY = if (!flipScale) -h else h
                    c.drawLine(mx, 0f, mx, mTickY, tMin)
                }
            }
        }

        val hbL = s * scale; val hbR = e * scale
        if (hbR > hbL) {
            val hndlY1 = if (!flipScale) -barThick + 6f else 6f
            val hndlY2 = if (!flipScale) -6f else barThick - 6f
            val r = RectF(hbL, hndlY1, hbR, hndlY2)
            c.drawRoundRect(r, 6f, 6f, mBg); c.drawRoundRect(r, 6f, 6f, mBrd)
            val my = (hndlY1 + hndlY2) / 2
            c.drawCircle(hbL, my, hndlR, hndl); c.drawCircle(hbL, my, hndlR, hndlS)
            c.drawCircle(hbR, my, hndlR, hndl); c.drawCircle(hbR, my, hndlR, hndlS)
            val dp = (e - s) / density; val txt = fmt(dp); val uni = unitStr(); val full = "$txt $uni"
            val mTextY = if (!flipScale) 32f * density else -16f * density
            c.drawText(full, (hbL + hbR) / 2 - mTxt.measureText(full) / 2, mTextY, mTxt)
        }
        c.restore()
    }

    private fun fmt(dp: Float): String = when (unit) {
        0 -> String.format("%.1f", dp); 1 -> String.format("%.2f", dp * density / (xdpi / 2.54f))
        2 -> String.format("%.0f", dp * density); 3 -> String.format("%.1f", dp); else -> ""
    }
    private fun unitStr(): String = when (unit) { 0 -> "dp"; 1 -> "cm"; 2 -> "px"; 3 -> "sp"; else -> "" }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) { mode = detect(event.x, event.y); lastX = event.x; lastY = event.y }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) { pinchDist = spacing(event); lastAngle = rotation(event); mode = MODE_PINCH }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_PINCH && event.pointerCount >= 2) {
                    if (isZoomEnabled) {
                        val d = spacing(event); if (d > 10f && pinchDist > 10f) { scale = (scale * d / pinchDist).coerceIn(0.25f, 4.0f); pinchDist = d }
                    }
                    val a = rotation(event); val da = a - lastAngle
                    
                    val canRotate = if (numRulers == 1) fixedOrientation == 0 else true
                    
                    if (canRotate) {
                        if (numRulers == 2 && isCoupled) hRot += da else {
                            val currentMode = detect(event.x, event.y)
                            if (currentMode == MODE_DRAG_V || currentMode == MODE_VS || currentMode == MODE_VE || currentMode == MODE_RESIZE_V) vRot += da else hRot += da
                        }
                    }
                    lastAngle = a; invalidate()
                } else if (mode != MODE_NONE && event.pointerCount == 1) {
                    handleDrag(event.x - lastX, event.y - lastY); lastX = event.x; lastY = event.y; invalidate()
                }
            }
            MotionEvent.ACTION_UP -> { savePrefs(); mode = MODE_NONE; invalidate() }
        }
        return true
    }

    private fun detect(x: Float, y: Float): Int {
        val th = touchSlop; val rth = resizeSlop
        
        val p = transform(x, y, hX, hY, hRot)
        if (p.x > barLen * scale - rth && p.x < barLen * scale + rth && p.y > -barThick && p.y < 0) return MODE_RESIZE_H
        if (abs(p.x - hS * scale) < th && p.y > -barThick && p.y < 0) return MODE_HS
        if (abs(p.x - hE * scale) < th && p.y > -barThick && p.y < 0) return MODE_HE
        if (p.x > 0 && p.x < barLen * scale && p.y > -barThick && p.y < 0) return MODE_DRAG_H

        if (numRulers == 2) {
            if (isCoupled) {
                val vp = transform(x, y, hX, hY, hRot + 90f)
                if (vp.x > barLen * scale - rth && vp.x < barLen * scale + rth && vp.y > 0 && vp.y < barThick) return MODE_RESIZE_V
                if (abs(vp.x - vS * scale) < th && vp.y > 0 && vp.y < barThick) return MODE_VS
                if (abs(vp.x - vE * scale) < th && vp.y > 0 && vp.y < barThick) return MODE_VE
                if (vp.x > 0 && vp.x < barLen * scale && vp.y > 0 && vp.y < barThick) return MODE_DRAG_V
            } else {
                val vp = transform(x, y, vX, vY, vRot)
                if (vp.x > barLen * scale - rth && vp.x < barLen * scale + rth && vp.y > -barThick && vp.y < 0) return MODE_RESIZE_V
                if (abs(vp.x - vS * scale) < th && vp.y > -barThick && vp.y < 0) return MODE_VS
                if (abs(vp.x - vE * scale) < th && vp.y > -barThick && vp.y < 0) return MODE_VE
                if (vp.x > 0 && vp.x < barLen * scale && vp.y > -barThick && vp.y < 0) return MODE_DRAG_V
            }
        }
        return MODE_NONE
    }

    private fun transform(x: Float, y: Float, px: Float, py: Float, rot: Float): PointF {
        val dx = x - px; val dy = y - py
        val rad = -rot * PI.toFloat() / 180f
        val rx = dx * cos(rad) - dy * sin(rad)
        val ry = dx * sin(rad) + dy * cos(rad)
        return PointF(rx, ry)
    }

    private fun handleDrag(dx: Float, dy: Float) {
        when (mode) {
            MODE_DRAG_H -> { hX += dx; hY += dy; if (numRulers == 2 && isCoupled) { vX = hX; vY = hY } }
            MODE_DRAG_V -> { if (isCoupled) { hX += dx; hY += dy; vX = hX; vY = hY } else { vX += dx; vY += dy } }
            MODE_RESIZE_H -> barLen = (barLen + transformDist(dx, dy, hRot) / scale).coerceAtLeast(100 * density)
            MODE_RESIZE_V -> {
                val rot = if (isCoupled) hRot + 90f else vRot
                barLen = (barLen + transformDist(dx, dy, rot) / scale).coerceAtLeast(100 * density)
            }
            MODE_HS -> hS = ((hS * scale + transformDist(dx, dy, hRot)) / scale).coerceIn(0f, hE - 5 * density)
            MODE_HE -> hE = ((hE * scale + transformDist(dx, dy, hRot)) / scale).coerceIn(hS + 5 * density, barLen)
            MODE_VS -> {
                val rot = if (isCoupled) hRot + 90f else vRot
                vS = ((vS * scale + transformDist(dx, dy, rot)) / scale).coerceIn(0f, vE - 5 * density)
            }
            MODE_VE -> {
                val rot = if (isCoupled) hRot + 90f else vRot
                vE = ((vE * scale + transformDist(dx, dy, rot)) / scale).coerceIn(vS + 5 * density, barLen)
            }
        }
    }

    private fun transformDist(dx: Float, dy: Float, rot: Float): Float {
        val rad = rot * PI.toFloat() / 180f
        return dx * cos(rad) + dy * sin(rad)
    }

    private fun spacing(e: MotionEvent): Float {
        val dx = e.getX(0) - e.getX(1); val dy = e.getY(0) - e.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
    private fun rotation(e: MotionEvent): Float {
        val dx = e.getX(0) - e.getX(1); val dy = e.getY(0) - e.getY(1)
        return atan2(dy, dx) * 180f / PI.toFloat()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    fun setUnit(u: Int) { unit = u.coerceIn(0, 3); invalidate(); savePrefs() }
    fun getUnit(): Int = unit
    fun getScale(): Float = scale
    fun setBarLengthDp(l: Float) { barLen = (l * density).coerceAtLeast(100 * density); invalidate(); savePrefs() }
    fun getBarLengthDp(): Float = barLen / density
    fun resetPosition() { hX = 150f; hY = 150f; vX = 150f; vY = 300f; hRot = 0f; vRot = 90f; scale = 1.0f; invalidate(); savePrefs() }
}
