package com.example.ui

import com.example.data.PhotoEntity

enum class CameraMode(val displayName: String) {
    MORE("MORE"),
    PORTRAIT("PORTRAIT"),
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
    PRO("PRO"),
    NIGHT("NIGHT"),
    SINGLE_TAKE("SINGLE TAKE")
}

enum class FlashMode {
    OFF, AUTO, ON
}

enum class AspectRatioMode(val label: String, val ratioNumerator: Int, val ratioDenominator: Int) {
    RATIO_3_4("3:4", 3, 4),
    RATIO_9_16("9:16", 9, 16),
    RATIO_1_1("1:1", 1, 1),
    RATIO_FULL("FULL", 9, 19)
}

data class CameraUiState(
    val currentMode: CameraMode = CameraMode.PHOTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_3_4,
    val timerSeconds: Int = 0,
    val zoomLevel: Float = 1.0f,
    val isAiSceneOptimizerEnabled: Boolean = true,
    val detectedScene: String = "Sunset",
    val latestPhoto: PhotoEntity? = null,
    val allPhotos: List<PhotoEntity> = emptyList(),
    val isCapturing: Boolean = false,
    val isFrontCamera: Boolean = false,
    val selectedPhotoForGallery: PhotoEntity? = null,
    val isGalleryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val showShutterFlash: Boolean = false,
    val showCompareBeforeAfter: Boolean = false,
    val focusPoint: Pair<Float, Float>? = null,
    val activeQuickSetting: QuickSettingType? = null
)

enum class QuickSettingType {
    FLASH, ASPECT_RATIO, TIMER
}
