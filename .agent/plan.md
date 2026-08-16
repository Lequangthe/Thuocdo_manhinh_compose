# Project Plan

Tạo ứng dụng Android đo kích thước widget trên màn hình chính, KHÔNG dùng camera. Yêu cầu:
1. Tính năng chính: Kéo thả khung hình chữ nhật để đo widget, hiển thị Grid + dp + px.
2. Bong bóng nổi (floating bubble): Toggle chế độ đo, di chuyển được.
3. Công thức: Tính toán dựa trên màn hình và grid do người dùng nhập.
4. Kỹ thuật: Kotlin, TYPE_APPLICATION_OVERLAY, Service, Canvas custom view.

## Project Brief

# Project Brief: Widget Size Meter (KICHTHUOC_WIDGET)

A specialized Android utility designed for developers and power users to accurately measure home screen widgets and icons without needing the camera. The app provides a non-intrusive overlay system to calculate precise dimensions in multiple units.


## Features
*   **Floating Controller Bubble**: A persistent, draggable overlay icon (inspired by Messenger) that allows users to toggle measurement mode instantly from the home screen.
*   **Interactive Measuring Frame**: A semi-transparent Canvas-based overlay with corner drag handles, enabling users to "frame" any widget or icon for measurement.
*   **Multi-Unit Dimension Feedback**: Real-time display of widget dimensions in **Grid Cells** (e.g., 4x2), **Density-independent Pixels (dp)**, and **Pixels (px)** via an attached tooltip.
*   **Customizable Grid Calibration**: Allows users to input their specific launcher grid size (e.g., 5x4 or 6x5) to ensure perfectly accurate cell calculations based on the device's screen resolution.

## High-Level Technical Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose with **Material Design 3**
*   **Navigation**: **Jetpack Navigation 3** (State-driven architecture)
*   **Adaptive Strategy**: **Compose Material Adaptive** library for a responsive settings interface.
*   **Core Graphics**: Custom View with **Canvas API** for overlay rendering and touch-event handling.
*   **System Integration**: Android **Service** with `TYPE_APPLICATION_OVERLAY` for cross-app measurement capabilities.
*   **Asynchrony**: **Kotlin Coroutines** for background dimension calculations and state management.

## Implementation Steps
**Total Duration:** 50m 20s

### Task_1_OverlayService_Permissions: Setup SYSTEM_ALERT_WINDOW permission handling in MainActivity and create the base OverlayService to manage TYPE_APPLICATION_OVERLAY windows.
- **Status:** COMPLETED
- **Updates:** Implemented SYSTEM_ALERT_WINDOW and foreground service permissions. Created MainActivity with Compose UI for permission handling and service control. Implemented base OverlayService as a foreground service with notification. Updated project to SDK 37 for compatibility.
- **Acceptance Criteria:**
  - Permission request flow works
  - Service starts and remains active in background
  - Overlay permission is granted by user
- **Duration:** 15m 16s

### Task_2_DraggableBubble: Implement a draggable floating bubble icon that persists on top of other apps and toggles the visibility of the measuring frame.
- **Status:** COMPLETED
- **Updates:** Implemented a draggable floating bubble in OverlayService. The bubble uses a custom ruler icon and supports drag-to-move with a 'snap to edge' animation. Tapping the bubble toggles measurement mode, with visual feedback (icon color change) and a Toast message. Used WindowManager with TYPE_APPLICATION_OVERLAY and a custom OnTouchListener.
- **Acceptance Criteria:**
  - Bubble is draggable across the screen
  - Bubble stays visible over home screen
  - Tapping bubble toggles measurement overlay
- **Duration:** 1m 39s

### Task_3_MeasuringCanvas: Create a custom Canvas-based overlay for the measuring frame. Implement 4-corner drag handles for resizing and a center area for moving the frame.
- **Status:** COMPLETED
- **Updates:** Created MeasuringOverlayView with custom Canvas drawing. Implemented a resizable and draggable semi-transparent frame with corner handles and a 4x4 internal grid. Integrated the view into OverlayService to toggle with the floating bubble. Used Material 3 styling and handled touch events for real-time resizing and moving.
- **Acceptance Criteria:**
  - Frame is resizable via corner handles
  - Frame is draggable as a whole
  - Canvas correctly renders the frame and grid lines
- **Duration:** 1m 28s

### Task_4_DimensionLogic: Implement calculation logic to convert frame coordinates into Pixels (px), Density-independent Pixels (dp), and Grid Cells (based on user settings). Display these in a tooltip.
- **Status:** COMPLETED
- **Updates:** Implemented calculation logic for Px, Dp, and Grid cells in MeasuringOverlayView. Added a dynamic tooltip to the canvas to display measurements in real-time. Updated MainActivity to allow users to configure launcher grid dimensions (columns/rows), persisting settings via SharedPreferences. Calculations round grid cells to the nearest 0.5. Tooltip intelligently stays within screen bounds.
- **Acceptance Criteria:**
  - Dimensions update in real-time while resizing
  - Calculations are accurate for the device's screen density
  - Grid cell calculation respects user-defined grid size (e.g., 4x4)
- **Duration:** 3m 58s

### Task_5_Settings_Verify: Create the main Settings UI using Jetpack Compose and Material 3 for grid configuration. Implement adaptive icon and verify application stability.
- **Status:** COMPLETED
- **Updates:** Created the main Settings UI using Jetpack Compose and Material 3 for grid configuration. Implemented an adaptive icon with a ruler-and-grid motif. Verified application stability and functionality through comprehensive testing. The app handles permissions, service lifecycle, and interactive measurements seamlessly. Verified Edge-to-Edge and Material 3 compliance.
- **Acceptance Criteria:**
  - Settings screen allows entering grid rows/cols
  - App follows Material 3 guidelines and Edge-to-Edge
  - Adaptive icon is present
  - Build pass and app does not crash during use
  - Measurement flow works on home screen
- **Duration:** 27m 59s

### Hoàn thiện tương tác Thước đo – Kéo dài thước & di chuyển vùng đo – 16/08/2026
- File thay đổi: `app/src/main/java/com/quangthe/thuocdo/ui/overlay/RulerComponent.kt`, `model/RulerState.kt`, `data/RulerRepository.kt`
- Chi tiết:
  - Thay thế `detectDragGestures` + `detectTransformGestures` (xung đột pointer) bằng 1 gesture thống nhất `awaitEachGesture`: 1 ngón kéo theo mode, 2 ngón pinch-zoom/xoay không bị chặn khi chạm vào thước.
  - Thêm mode `MODE_MOVE_H/MODE_MOVE_V`: kéo giữa vùng tô xanh để di chuyển cả vùng đo (trước đây kéo xanh chỉ di chuyển toàn bộ thước).
  - Sửa `MODE_RESIZE_H/V` (kéo dài thước): giảm mặc định `barLength` 600→320 (đầu thước trước đây nằm NGOÀI màn hình nên không kéo được), tăng độ ưu tiên vùng chạm đầu thước, chặn `barLength < selection`.
  - Thêm `normalizeState` để chuẩn hoá bounds khi load.
  - Vẽ thêm chấm giữa vùng xanh (chỉ dẫn kéo di chuyển) + làm rõ tay cầm đầu thước.
- Build: `:app:assembleDebug` SUCCESS.

### Cải thiện cảm giác kéo & Định hướng thước + hiện số đo góc – 16/08/2026
- File thay đổi: `app/src/main/java/com/quangthe/thuocdo/ui/overlay/RulerComponent.kt`, `app/src/main/java/com/quangthe/thuocdo/MainActivity.kt`
- Chi tiết:
  - **Cảm giác di chuyển**: chuyển từ cộng dồn delta (`lastX/lastY`) sang **anchor tuyệt đối** (`startPos` + `startHX/HY`) cho các mode di chuyển (MODE_NONE/DRAG_H/DRAG_V) → thước bám ngón tay 1:1, không rung/tụt khi frame rớt. Các mode co dãn/handles dùng delta `position - previousPosition` cho chính xác.
  - **Cài đặt "Định hướng thước"**: thêm Card trong `MainActivity` (Tự do / Nằm ngang / Thẳng đứng) → `viewModel.updateFixedOrientation` (0/1/2). Khi `numRulers == 1`: thước bị khoá 0° hoặc 90° (không xoay được, tự snap khi đổi setting — bổ sung `fixedOrientation` vào key `remember` + snap trong `normalizeState`).
  - **Hiện số đo góc màu đỏ**: vẽ góc thực tế của từng cây thước (như `0°`, `90°`) bằng chữ đỏ `#E91E63` in nghiêng trên thước (giống bản Custom View cũ).
- Build: `:app:assembleDebug` SUCCESS.

