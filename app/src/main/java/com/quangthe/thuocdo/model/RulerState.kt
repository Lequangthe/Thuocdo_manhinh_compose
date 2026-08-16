package com.quangthe.thuocdo.model

data class RulerState(
    val horizontalX: Float = 150f,
    val horizontalY: Float = 150f,
    val horizontalRotation: Float = 0f,
    val horizontalStart: Float = 0f,
    val horizontalEnd: Float = 300f,
    
    val verticalX: Float = 150f,
    val verticalY: Float = 300f,
    val verticalRotation: Float = 90f,
    val verticalStart: Float = 0f,
    val verticalEnd: Float = 300f,
    
    val barLength: Float = 320f,
    val scale: Float = 1.0f,
    val unit: Int = 0, // 0: dp, 1: cm, 2: px, 3: sp
    val numRulers: Int = 2,
    val isCoupled: Boolean = true,
    val isZoomEnabled: Boolean = true,
    val fixedOrientation: Int = 0, // 0: Tự do, 1: Cố định (0°/90° hoặc L-Shape Fixed)
    
    val bubbleX: Int = 100,
    val bubbleY: Int = 300,
    val isRulerVisible: Boolean = false
)
