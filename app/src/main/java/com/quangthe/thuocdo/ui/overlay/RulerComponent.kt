package com.quangthe.thuocdo.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthe.thuocdo.model.RulerState
import kotlin.math.*

@Composable
fun RulerComponent(
    state: RulerState,
    onUpdateState: (RulerState) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val xdpi = context.resources.displayMetrics.xdpi

    // Flag để biết người dùng đang chạm, tránh bị state từ DataStore ghi đè gây giật
    var isInteracting by remember { mutableStateOf(false) }

    // Trạng thái tạm thời để tương tác mượt mà
    var localState by remember { mutableStateOf(normalizeState(state)) }
    
    // Cập nhật localState khi state từ DataStore thay đổi, NHƯNG chỉ khi không tương tác
    LaunchedEffect(state) {
        if (!isInteracting) {
            localState = normalizeState(state)
        }
    }

    val barThick = with(density) { 48.dp.toPx() }
    val majTickH = with(density) { 20.dp.toPx() }
    val minTickH = with(density) { 12.dp.toPx() }

    val labelTextSize = with(density) { 14.sp.toPx() }
    val measureTextSize = with(density) { 18.sp.toPx() }

    val touchSlop = 60f // Vùng nhận diện tay cầm
    val resizeSlop = 80f // Vùng nhận diện đầu thước (kéo dài)

    val textPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = labelTextSize
        color = android.graphics.Color.BLACK
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val measurePaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = measureTextSize
        color = android.graphics.Color.parseColor("#1B5E20")
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val anglePaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = with(density) { 13.sp.toPx() }
        color = android.graphics.Color.parseColor("#E91E63")
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(
                localState.numRulers, localState.isCoupled,
                localState.isZoomEnabled, localState.fixedOrientation
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isInteracting = true
                    val densityVal = density.density
                    var mode = detectMode(down.position, localState, barThick, densityVal, touchSlop, resizeSlop)
                    // Anchor để bám tay: thước đi theo ngón tay 1:1, không bị rung/tụt
                    var startPos = down.position
                    var startHX = if (mode == MODE_DRAG_V && !localState.isCoupled) localState.verticalX else localState.horizontalX
                    var startHY = if (mode == MODE_DRAG_V && !localState.isCoupled) localState.verticalY else localState.horizontalY
                    var pinchDist = 0f
                    var lastAngle = 0f
                    var wasPinch = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        when {
                            pressed.isEmpty() -> break

                            pressed.size >= 2 -> {
                                // Pinch: zoom + xoay
                                val dist = spacing(pressed)
                                val ang = rotation(pressed)
                                if (pinchDist > 0f) {
                                    var ns = localState
                                    if (localState.isZoomEnabled && dist > 10f && pinchDist > 10f) {
                                        ns = ns.copy(scale = (ns.scale * dist / pinchDist).coerceIn(0.25f, 4.0f))
                                    }
                                    val canRotate = localState.numRulers == 2 || localState.fixedOrientation == 0
                                    val da = ang - lastAngle
                                    if (canRotate && da != 0f) {
                                        ns = rotateForMode(ns, mode, da)
                                    }
                                    localState = ns
                                }
                                pinchDist = dist
                                lastAngle = ang
                                wasPinch = true
                            }

                            else -> {
                                // 1 ngón: kéo theo mode đã nhận diện
                                val p = pressed[0]
                                if (wasPinch) {
                                    mode = detectMode(p.position, localState, barThick, densityVal, touchSlop, resizeSlop)
                                    startPos = p.position
                                    startHX = if (mode == MODE_DRAG_V && !localState.isCoupled) localState.verticalX else localState.horizontalX
                                    startHY = if (mode == MODE_DRAG_V && !localState.isCoupled) localState.verticalY else localState.horizontalY
                                    wasPinch = false
                                }
                                when (mode) {
                                    MODE_NONE, MODE_DRAG_H, MODE_DRAG_V -> {
                                        // Dùng anchor tuyệt đối: không tích luỹ delta -> trượt mượt 1:1
                                        val nx = startHX + (p.position.x - startPos.x)
                                        val ny = startHY + (p.position.y - startPos.y)
                                        localState = if (mode == MODE_DRAG_V && !localState.isCoupled) {
                                            localState.copy(verticalX = nx, verticalY = ny)
                                        } else {
                                            val ns = localState.copy(horizontalX = nx, horizontalY = ny)
                                            if (localState.numRulers == 2 && localState.isCoupled) {
                                                ns.copy(verticalX = ns.horizontalX, verticalY = ns.horizontalY)
                                            } else {
                                                ns
                                            }
                                        }
                                    }
                                    else -> {
                                        val dx = p.position.x - p.previousPosition.x
                                        val dy = p.position.y - p.previousPosition.y
                                        if (dx != 0f || dy != 0f) {
                                            localState = handleRulerDrag(localState, mode, Offset(dx, dy), densityVal)
                                        }
                                    }
                                }
                            }
                        }

                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                    isInteracting = false
                    onUpdateState(normalizeState(localState))
                }
            }
    ) {
        val len = localState.barLength * localState.scale * density.density

        // Draw Corner if coupled
        if (localState.numRulers == 2 && localState.isCoupled) {
            rotate(localState.horizontalRotation, pivot = Offset(localState.horizontalX, localState.horizontalY)) {
                translate(localState.horizontalX, localState.horizontalY) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(-barThick, -barThick),
                        size = Size(barThick + 16f, barThick + 16f),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                }
            }
        }

        // Horizontal Ruler
        if (localState.numRulers >= 1) {
            drawRulerBar(
                localState.horizontalX, localState.horizontalY, localState.horizontalRotation,
                len, localState.horizontalStart, localState.horizontalEnd,
                localState.unit, localState.scale, barThick, majTickH, minTickH,
                textPaint, measurePaint, anglePaint, density.density, xdpi, false
            )
        }

        // Vertical Ruler
        if (localState.numRulers == 2) {
            if (localState.isCoupled) {
                drawRulerBar(
                    localState.horizontalX, localState.horizontalY, localState.horizontalRotation + 90f,
                    len, localState.verticalStart, localState.verticalEnd,
                    localState.unit, localState.scale, barThick, majTickH, minTickH,
                    textPaint, measurePaint, anglePaint, density.density, xdpi, true
                )
            } else {
                drawRulerBar(
                    localState.verticalX, localState.verticalY, localState.verticalRotation,
                    len, localState.verticalStart, localState.verticalEnd,
                    localState.unit, localState.scale, barThick, majTickH, minTickH,
                    textPaint, measurePaint, anglePaint, density.density, xdpi, false
                )
            }
        }
    }
}

private fun DrawScope.drawRulerBar(
    x: Float, y: Float, rot: Float, len: Float,
    start: Float, end: Float, unit: Int, scale: Float,
    barThick: Float, majTickH: Float, minTickH: Float,
    textPaint: android.graphics.Paint, measurePaint: android.graphics.Paint, anglePaint: android.graphics.Paint,
    density: Float, xdpi: Float, flipScale: Boolean
) {
    rotate(rot, pivot = Offset(x, y)) {
        translate(x, y) {
            val rect = if (!flipScale) {
                Rect(0f, -barThick, len, 0f)
            } else {
                Rect(0f, 0f, len, barThick)
            }

            drawRoundRect(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color.LightGray,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 2f)
            )

            // Số đo góc (màu đỏ) hiển thị trên thước
            val angleNorm = ((rot % 360f) + 360f) % 360f
            val angleTxt = "${angleNorm.toInt()}°"
            val angleY = if (!flipScale) -barThick - 10f else barThick + 28f * density
            
            // Vẽ nền cho số đo góc
            val angleBounds = android.graphics.Rect()
            anglePaint.getTextBounds(angleTxt, 0, angleTxt.length, angleBounds)
            val bgPadding = 4f * density
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(14f - bgPadding, angleY - angleBounds.height() - bgPadding),
                size = Size(angleBounds.width() + bgPadding * 2, angleBounds.height() + bgPadding * 2),
                cornerRadius = CornerRadius(4f * density, 4f * density)
            )
            drawContext.canvas.nativeCanvas.drawText(angleTxt, 14f, angleY, anglePaint)

            val unitsPerMaj = when (unit) {
                1 -> 1f
                2 -> 50f
                else -> 10f
            }
            val pixelsPerUnit = when (unit) {
                1 -> xdpi / 2.54f
                2 -> 1f
                else -> density
            }

            val majStep = pixelsPerUnit * unitsPerMaj * scale
            val minStep = majStep / 10f
            val nTicks = (len / majStep).toInt() + 1
            val labelStep = if (unit == 1) 1 else 5

            for (i in 0 until nTicks) {
                val tx = i * majStep
                val ty = if (!flipScale) -majTickH else majTickH
                drawLine(Color.Black, Offset(tx, 0f), Offset(tx, ty), strokeWidth = 3f)

                if (i % labelStep == 0) {
                    val lbl = "${i / labelStep * labelStep}"
                    val lblY = if (!flipScale) -majTickH - 10f else majTickH + 25f
                    drawContext.canvas.nativeCanvas.drawText(lbl, tx, lblY, textPaint)
                }

                for (j in 1..9) {
                    val mx = tx + j * minStep
                    if (mx < len) {
                        val h = if (j == 5) minTickH * 1.5f else minTickH
                        val mty = if (!flipScale) -h else h
                        drawLine(Color.Gray, Offset(mx, 0f), Offset(mx, mty), strokeWidth = 2f)
                    }
                }
            }

            // Measurement selection highlight (vùng tô xanh dùng để đo)
            val maxVal = len / (scale * density)
            val hs = start.coerceIn(0f, maxVal)
            val he = end.coerceIn(hs, maxVal)
            val hbL = hs * scale * density
            val hbR = he * scale * density
            if (hbR > hbL) {
                val hndlY1 = if (!flipScale) -barThick + 6f else 6f
                val hndlY2 = if (!flipScale) -6f else barThick - 6f

                drawRoundRect(
                    color = Color(0x804CAF50),
                    topLeft = Offset(hbL, hndlY1),
                    size = Size(hbR - hbL, hndlY2 - hndlY1),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                val unitStr = when (unit) { 0 -> "dp"; 1 -> "cm"; 2 -> "px"; 3 -> "sp"; else -> "" }
                val value = he - hs
                val formatted = when (unit) {
                    1 -> "%.2f".format(value * density / (xdpi / 2.54f))
                    2 -> "%.0f".format(value * density)
                    else -> "%.1f".format(value)
                }
                val fullText = "$formatted $unitStr"

                val mTextY = if (!flipScale) 40f * density else -20f * density
                val centerX = (hbL + hbR) / 2
                
                // Vẽ nền cho số đo độ dài
                val textBounds = android.graphics.Rect()
                measurePaint.getTextBounds(fullText, 0, fullText.length, textBounds)
                val mBgPadding = 6f * density
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(centerX - (textBounds.width() / 2) - mBgPadding, mTextY - textBounds.height() - mBgPadding),
                    size = Size(textBounds.width() + mBgPadding * 2, textBounds.height() + mBgPadding * 2),
                    cornerRadius = CornerRadius(6f * density, 6f * density)
                )
                
                drawContext.canvas.nativeCanvas.drawText(fullText, centerX, mTextY, measurePaint)

                // Chấm di chuyển giữa vùng xanh (kéo để di chuyển cả vùng đo)
                val cX = (hbL + hbR) / 2
                val hndlY = (hndlY1 + hndlY2) / 2
                drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 14f * density, center = Offset(cX, hndlY))
                drawCircle(color = Color(0xFF4CAF50), radius = 14f * density, center = Offset(cX, hndlY), style = Stroke(width = 2f * density))

                // 2 nút tròn ở 2 đầu vùng xanh để co dãn
                drawCircle(color = Color(0xFF4CAF50), radius = 16f * density, center = Offset(hbL, hndlY))
                drawCircle(color = Color.White, radius = 16f * density, center = Offset(hbL, hndlY), style = Stroke(width = 2f * density))

                drawCircle(color = Color(0xFF4CAF50), radius = 16f * density, center = Offset(hbR, hndlY))
                drawCircle(color = Color.White, radius = 16f * density, center = Offset(hbR, hndlY), style = Stroke(width = 2f * density))
            }

            // Chỉ báo ở cuối thước: kéo để làm dài/ngắn thước
            val endX = len
            val endY = if (!flipScale) -barThick / 2 else barThick / 2
            drawCircle(color = Color(0xFF4CAF50).copy(alpha = 0.35f), radius = 12f * density, center = Offset(endX, endY))
            drawCircle(color = Color(0xFF4CAF50), radius = 12f * density, center = Offset(endX, endY), style = Stroke(width = 2f * density))
            drawLine(color = Color.White, start = Offset(endX - 5f * density, endY - 5f * density), end = Offset(endX + 5f * density, endY + 5f * density), strokeWidth = 2f * density)
            drawLine(color = Color.White, start = Offset(endX - 5f * density, endY + 5f * density), end = Offset(endX + 5f * density, endY - 5f * density), strokeWidth = 2f * density)
        }
    }
}

private const val MODE_NONE = 0
private const val MODE_DRAG_H = 1
private const val MODE_DRAG_V = 11
private const val MODE_HS = 2
private const val MODE_HE = 3
private const val MODE_VS = 4
private const val MODE_VE = 5
private const val MODE_RESIZE_H = 6
private const val MODE_RESIZE_V = 7
private const val MODE_MOVE_H = 8
private const val MODE_MOVE_V = 9

private fun detectMode(offset: Offset, state: RulerState, barThick: Float, density: Float, touchSlop: Float, resizeSlop: Float): Int {
    val x = offset.x
    val y = offset.y
    val len = state.barLength * state.scale * density
    val yTol = 30f
    val slop = touchSlop

    // Horizontal Ruler
    val hp = transformPoint(x, y, state.horizontalX, state.horizontalY, state.horizontalRotation)
    val hInY = hp.y > -barThick - yTol && hp.y < yTol
    if (hInY) {
        if (hp.x > len - resizeSlop && hp.x < len + resizeSlop) return MODE_RESIZE_H
        val hL = state.horizontalStart * state.scale * density
        val hR = state.horizontalEnd * state.scale * density
        if (abs(hp.x - hL) < slop) return MODE_HS
        if (abs(hp.x - hR) < slop) return MODE_HE
        if (hR - hL > slop && hp.x >= hL + slop && hp.x <= hR - slop) return MODE_MOVE_H
        if (hp.x > 0f && hp.x < len) return MODE_DRAG_H
    }

    if (state.numRulers == 2) {
        if (state.isCoupled) {
            val vp = transformPoint(x, y, state.horizontalX, state.horizontalY, state.horizontalRotation + 90f)
            val vInY = vp.y > -yTol && vp.y < barThick + yTol
            if (vInY) {
                if (vp.x > len - resizeSlop && vp.x < len + resizeSlop) return MODE_RESIZE_V
                val vL = state.verticalStart * state.scale * density
                val vR = state.verticalEnd * state.scale * density
                if (abs(vp.x - vL) < slop) return MODE_VS
                if (abs(vp.x - vR) < slop) return MODE_VE
                if (vR - vL > slop && vp.x >= vL + slop && vp.x <= vR - slop) return MODE_MOVE_V
                if (vp.x > 0f && vp.x < len) return MODE_DRAG_V
            }
        } else {
            val vp = transformPoint(x, y, state.verticalX, state.verticalY, state.verticalRotation)
            val vInY = vp.y > -barThick - yTol && vp.y < yTol
            if (vInY) {
                if (vp.x > len - resizeSlop && vp.x < len + resizeSlop) return MODE_RESIZE_V
                val vL = state.verticalStart * state.scale * density
                val vR = state.verticalEnd * state.scale * density
                if (abs(vp.x - vL) < slop) return MODE_VS
                if (abs(vp.x - vR) < slop) return MODE_VE
                if (vR - vL > slop && vp.x >= vL + slop && vp.x <= vR - slop) return MODE_MOVE_V
                if (vp.x > 0f && vp.x < len) return MODE_DRAG_V
            }
        }
    }
    return MODE_NONE
}

private fun transformPoint(x: Float, y: Float, px: Float, py: Float, rot: Float): Offset {
    val dx = x - px; val dy = y - py
    val rad = -rot * PI.toFloat() / 180f
    val rx = dx * cos(rad) - dy * sin(rad)
    val ry = dx * sin(rad) + dy * cos(rad)
    return Offset(rx, ry)
}

private fun handleRulerDrag(state: RulerState, mode: Int, dragAmount: Offset, density: Float): RulerState {
    val dx = dragAmount.x; val dy = dragAmount.y
    val perUnit = state.scale * density
    return when (mode) {
        MODE_DRAG_H -> {
            val ns = state.copy(horizontalX = state.horizontalX + dx, horizontalY = state.horizontalY + dy)
            if (state.numRulers == 2 && state.isCoupled) ns.copy(verticalX = ns.horizontalX, verticalY = ns.horizontalY) else ns
        }
        MODE_DRAG_V -> {
            if (state.isCoupled) {
                state.copy(horizontalX = state.horizontalX + dx, horizontalY = state.horizontalY + dy)
            } else {
                state.copy(verticalX = state.verticalX + dx, verticalY = state.verticalY + dy)
            }
        }
        MODE_HS -> state.copy(horizontalStart = (state.horizontalStart + transformDist(dx, dy, state.horizontalRotation) / perUnit).coerceIn(0f, state.horizontalEnd - 5f))
        MODE_HE -> state.copy(horizontalEnd = (state.horizontalEnd + transformDist(dx, dy, state.horizontalRotation) / perUnit).coerceIn(state.horizontalStart + 5f, state.barLength))
        MODE_MOVE_H -> {
            val d = transformDist(dx, dy, state.horizontalRotation) / perUnit
            val w = (state.horizontalEnd - state.horizontalStart).coerceAtLeast(5f)
            val s = (state.horizontalStart + d).coerceIn(0f, state.barLength - w)
            state.copy(horizontalStart = s, horizontalEnd = s + w)
        }
        MODE_VS -> {
            val rot = if (state.isCoupled) state.horizontalRotation + 90f else state.verticalRotation
            state.copy(verticalStart = (state.verticalStart + transformDist(dx, dy, rot) / perUnit).coerceIn(0f, state.verticalEnd - 5f))
        }
        MODE_VE -> {
            val rot = if (state.isCoupled) state.horizontalRotation + 90f else state.verticalRotation
            state.copy(verticalEnd = (state.verticalEnd + transformDist(dx, dy, rot) / perUnit).coerceIn(state.verticalStart + 5f, state.barLength))
        }
        MODE_MOVE_V -> {
            val rot = if (state.isCoupled) state.horizontalRotation + 90f else state.verticalRotation
            val d = transformDist(dx, dy, rot) / perUnit
            val w = (state.verticalEnd - state.verticalStart).coerceAtLeast(5f)
            val s = (state.verticalStart + d).coerceIn(0f, state.barLength - w)
            state.copy(verticalStart = s, verticalEnd = s + w)
        }
        MODE_RESIZE_H -> {
            val delta = transformDist(dx, dy, state.horizontalRotation) / perUnit
            val minLen = maxOf(100f, state.horizontalEnd, state.verticalEnd)
            state.copy(barLength = (state.barLength + delta).coerceAtLeast(minLen))
        }
        MODE_RESIZE_V -> {
            val rot = if (state.isCoupled) state.horizontalRotation + 90f else state.verticalRotation
            val delta = transformDist(dx, dy, rot) / perUnit
            val minLen = maxOf(100f, state.horizontalEnd, state.verticalEnd)
            state.copy(barLength = (state.barLength + delta).coerceAtLeast(minLen))
        }
        else -> state
    }
}

private fun transformDist(dx: Float, dy: Float, rot: Float): Float {
    val rad = rot * PI.toFloat() / 180f
    return dx * cos(rad) + dy * sin(rad)
}

private fun rotateForMode(state: RulerState, mode: Int, deltaDeg: Float): RulerState {
    if (state.fixedOrientation != 0) return state // Không cho xoay nếu đang ở bất kỳ chế độ khóa nào
    
    return if (state.numRulers == 2 && state.isCoupled) {
        state.copy(horizontalRotation = state.horizontalRotation + deltaDeg)
    } else if (mode == MODE_DRAG_V || mode == MODE_VS || mode == MODE_VE || mode == MODE_RESIZE_V || mode == MODE_MOVE_V) {
        state.copy(verticalRotation = state.verticalRotation + deltaDeg)
    } else {
        state.copy(horizontalRotation = state.horizontalRotation + deltaDeg)
    }
}

private fun normalizeState(state: RulerState): RulerState {
    val bl = state.barLength.coerceAtLeast(100f)
    val hStart = state.horizontalStart.coerceIn(0f, bl - 5f)
    val vStart = state.verticalStart.coerceIn(0f, bl - 5f)
    var s = state.copy(
        barLength = bl,
        horizontalStart = hStart,
        horizontalEnd = state.horizontalEnd.coerceIn(hStart + 5f, bl),
        verticalStart = vStart,
        verticalEnd = state.verticalEnd.coerceIn(vStart + 5f, bl)
    )
    
    // Logic khóa hướng thông minh
    s = when {
        s.numRulers == 1 -> {
            when (s.fixedOrientation) {
                1 -> s.copy(horizontalRotation = 0f)
                2 -> s.copy(horizontalRotation = 90f)
                else -> s
            }
        }
        s.numRulers == 2 -> {
            if (s.fixedOrientation == 1) {
                // Khóa 2 thước vuông góc chuẩn khung màn hình
                if (s.isCoupled) {
                    s.copy(horizontalRotation = 0f, verticalX = s.horizontalX, verticalY = s.horizontalY)
                } else {
                    s.copy(horizontalRotation = 0f, verticalRotation = 90f)
                }
            } else s
        }
        else -> s
    }
    return s
}

private fun spacing(pointers: List<PointerInputChange>): Float {
    val a = pointers[0].position
    val b = pointers[1].position
    return hypot(a.x - b.x, a.y - b.y)
}

private fun rotation(pointers: List<PointerInputChange>): Float {
    val a = pointers[0].position
    val b = pointers[1].position
    return atan2(b.y - a.y, b.x - a.x) * 180f / PI.toFloat()
}
