package com.example.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PhotoEntity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SamsungCameraScreen(
    viewModel: CameraViewModel,
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(cameraPermissionState.status) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Samsung Camera Dark Theme Layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
                // Viewfinder Preview Container with Samsung One UI rounded corners
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 70.dp, bottom = 150.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF111111))
                ) {
                    // CameraX Preview
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { pView ->
                                pView.scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewView = pView
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { pView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(pView.surfaceProvider)
                                }

                                val imgCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = imgCapture

                                val cameraSelector = if (uiState.isFrontCamera) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imgCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("SamsungCamera", "Camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )

                    // Shutter Flash Animation Overlay
                    if (uiState.showShutterFlash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        )
                    }

                    // Samsung Focus Ring Animation (Tap to focus)
                    uiState.focusPoint?.let { (x, y) ->
                        SamsungFocusRing(x = x, y = y)
                    }

                    // Floating AI Scene Optimizer Badge (Samsung One UI 6 Bottom-Right Corner inside Viewfinder)
                    if (uiState.isAiSceneOptimizerEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 16.dp)
                        ) {
                            Surface(
                                onClick = { viewModel.toggleAiSceneOptimizer() },
                                color = Color(0xCC1A1A1A),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD800)),
                                modifier = Modifier.testTag("floating_ai_scene_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Scene",
                                        tint = Color(0xFFFFD800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.detectedScene,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Zoom Selector Pill (Floating lower center inside viewfinder)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        SamsungZoomPill(
                            currentZoom = uiState.zoomLevel,
                            onZoomSelected = { viewModel.setZoomLevel(it) }
                        )
                    }
                }
        } else {
            // Permission Grant Request UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFFFC107)
                    ) {
                        Text(
                            text = "Grant Permission",
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Top Quick Settings Bar (Samsung One UI Header)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0x66000000))
                .padding(top = 36.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings Icon
                IconButton(
                    onClick = { viewModel.toggleSettings(true) },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }

                // Flash Option
                IconButton(
                    onClick = { viewModel.setActiveQuickSetting(QuickSettingType.FLASH) },
                    modifier = Modifier.testTag("flash_button")
                ) {
                    Icon(
                        imageVector = when (uiState.flashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                            FlashMode.ON -> Icons.Default.FlashOn
                        },
                        contentDescription = "Flash",
                        tint = if (uiState.flashMode != FlashMode.OFF) Color(0xFFFFC107) else Color.White
                    )
                }

                // Timer Option
                IconButton(
                    onClick = { viewModel.setActiveQuickSetting(QuickSettingType.TIMER) },
                    modifier = Modifier.testTag("timer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (uiState.timerSeconds > 0) Color(0xFFFFC107) else Color.White
                        )
                        if (uiState.timerSeconds > 0) {
                            Text(
                                text = "${uiState.timerSeconds}s",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0xFFFFC107), CircleShape)
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                // Aspect Ratio Option
                IconButton(
                    onClick = { viewModel.setActiveQuickSetting(QuickSettingType.ASPECT_RATIO) },
                    modifier = Modifier.testTag("aspect_ratio_button")
                ) {
                    Text(
                        text = uiState.aspectRatio.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // AI Scene Optimizer Toggle Sparkle Icon
                IconButton(
                    onClick = { viewModel.toggleAiSceneOptimizer() },
                    modifier = Modifier.testTag("ai_optimizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Scene Optimizer",
                        tint = if (uiState.isAiSceneOptimizerEnabled) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.scale(if (uiState.isAiSceneOptimizerEnabled) 1.1f else 1.0f)
                    )
                }
            }
        }

        // Quick Settings Dropdown Overlay Bar
        uiState.activeQuickSetting?.let { setting ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
                    .background(Color(0xCC1E1E1E), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (setting) {
                        QuickSettingType.FLASH -> {
                            FlashMode.values().forEach { mode ->
                                Text(
                                    text = mode.name,
                                    color = if (uiState.flashMode == mode) Color(0xFFFFC107) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.setFlashMode(mode) }
                                        .padding(8.dp)
                                )
                            }
                        }
                        QuickSettingType.TIMER -> {
                            listOf(0, 2, 5, 10).forEach { sec ->
                                Text(
                                    text = if (sec == 0) "OFF" else "${sec}s",
                                    color = if (uiState.timerSeconds == sec) Color(0xFFFFC107) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.setTimerSeconds(sec) }
                                        .padding(8.dp)
                                )
                            }
                        }
                        QuickSettingType.ASPECT_RATIO -> {
                            AspectRatioMode.values().forEach { ratio ->
                                Text(
                                    text = ratio.label,
                                    color = if (uiState.aspectRatio == ratio) Color(0xFFFFC107) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.setAspectRatio(ratio) }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls Container (Samsung One UI Bottom Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(bottom = 24.dp, top = 8.dp)
        ) {
            // Camera Mode Carousel (MORE, PORTRAIT, PHOTO, VIDEO, PRO, NIGHT, SINGLE TAKE)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(CameraMode.values()) { mode ->
                    val isSelected = uiState.currentMode == mode
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable { viewModel.setCameraMode(mode) }
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(Color(0x33FFC107), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                } else Modifier
                            )
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) Color(0xFFFFC107) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Shutter Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Gallery Thumbnail Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222222))
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable { viewModel.openGallery() }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val latest = uiState.latestPhoto
                    if (latest != null) {
                        AsyncImage(
                            model = latest.displayPath,
                            contentDescription = "Gallery Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Small yellow sparkle indicator if silent AI enhancement complete!
                        if (latest.enhancementStatus == PhotoEntity.STATUS_COMPLETED) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(14.dp)
                                    .background(Color(0xFFFFC107), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Enhanced",
                                    tint = Color.Black,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        } else if (latest.enhancementStatus == PhotoEntity.STATUS_PROCESSING) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp),
                                color = Color(0xFFFFC107),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White
                        )
                    }
                }

                // Center: Samsung White Shutter Ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clickable {
                            viewModel.capturePhoto(context, imageCapture)
                        }
                        .testTag("shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (uiState.isCapturing) Color(0xFFFFC107) else Color.White)
                    )
                }

                // Right: Front/Rear Camera Flip Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                        .clickable { viewModel.toggleFrontRearCamera() }
                        .testTag("camera_switch_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SamsungFocusRing(x: Float, y: Float) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFFFC107),
                radius = 42.dp.toPx() * scale,
                center = Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun SamsungZoomPill(
    currentZoom: Float,
    onZoomSelected: (Float) -> Unit
) {
    Surface(
        color = Color(0x99000000),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(0.6f, 1.0f, 2.0f, 3.0f).forEach { zoom ->
                val isSelected = Math.abs(currentZoom - zoom) < 0.1f
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFFC107) else Color.Transparent)
                        .clickable { onZoomSelected(zoom) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (zoom == 0.6f) "0.6" else "${zoom.toInt()}x",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
