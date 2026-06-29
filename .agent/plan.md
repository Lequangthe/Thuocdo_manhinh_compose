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

