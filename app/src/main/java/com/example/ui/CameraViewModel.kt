package com.example.ui

import android.content.Context
import android.media.MediaActionSound
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PhotoEntity
import com.example.data.PhotoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

class CameraViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mediaSound = MediaActionSound()

    init {
        mediaSound.load(MediaActionSound.SHUTTER_CLICK)

        viewModelScope.launch {
            repository.initializeSampleDataIfNeeded()
        }

        viewModelScope.launch {
            repository.latestPhoto.collect { latest ->
                _uiState.update { currentState ->
                    val selected = if (currentState.isGalleryOpen && currentState.selectedPhotoForGallery != null) {
                        // Keep current selected photo updated if it was modified
                        currentState.allPhotos.find { it.id == currentState.selectedPhotoForGallery.id }
                            ?: currentState.selectedPhotoForGallery
                    } else {
                        latest
                    }
                    currentState.copy(
                        latestPhoto = latest,
                        selectedPhotoForGallery = if (currentState.selectedPhotoForGallery == null) latest else selected
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.allPhotos.collect { list ->
                _uiState.update { currentState ->
                    val updatedSelected = list.find { it.id == currentState.selectedPhotoForGallery?.id }
                        ?: currentState.selectedPhotoForGallery ?: list.firstOrNull()
                    currentState.copy(
                        allPhotos = list,
                        selectedPhotoForGallery = updatedSelected
                    )
                }
            }
        }
    }

    fun setCameraMode(mode: CameraMode) {
        val detected = when (mode) {
            CameraMode.PORTRAIT -> "Portrait Depth"
            CameraMode.NIGHT -> "Night Sight"
            CameraMode.PRO -> "Pro AI HDR"
            else -> "Scene Optimizer"
        }
        _uiState.update { it.copy(currentMode = mode, detectedScene = detected) }
    }

    fun setFlashMode(flashMode: FlashMode) {
        _uiState.update { it.copy(flashMode = flashMode, activeQuickSetting = null) }
    }

    fun setAspectRatio(aspectRatio: AspectRatioMode) {
        _uiState.update { it.copy(aspectRatio = aspectRatio, activeQuickSetting = null) }
    }

    fun setTimerSeconds(seconds: Int) {
        _uiState.update { it.copy(timerSeconds = seconds, activeQuickSetting = null) }
    }

    fun setZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom) }
    }

    fun toggleAiSceneOptimizer() {
        _uiState.update { it.copy(isAiSceneOptimizerEnabled = !it.isAiSceneOptimizerEnabled) }
    }

    fun toggleFrontRearCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun setActiveQuickSetting(setting: QuickSettingType?) {
        _uiState.update {
            it.copy(activeQuickSetting = if (it.activeQuickSetting == setting) null else setting)
        }
    }

    fun onTapFocus(x: Float, y: Float) {
        _uiState.update { it.copy(focusPoint = Pair(x, y)) }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun toggleSettings(open: Boolean? = null) {
        _uiState.update { currentState ->
            currentState.copy(isSettingsOpen = open ?: !currentState.isSettingsOpen)
        }
    }

    fun capturePhoto(context: Context, imageCapture: ImageCapture?) {
        val state = _uiState.value
        if (state.isCapturing) return

        viewModelScope.launch {
            // Handle Countdown Timer if set
            if (state.timerSeconds > 0) {
                delay(state.timerSeconds * 1000L)
            }

            _uiState.update { it.copy(isCapturing = true, showShutterFlash = true) }
            try { mediaSound.play(MediaActionSound.SHUTTER_CLICK) } catch (e: Exception) {}

            delay(100)
            _uiState.update { it.copy(showShutterFlash = false) }

            val photoFile = File(
                context.cacheDir,
                "captured_${System.currentTimeMillis()}.jpg"
            )

            if (imageCapture != null) {
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            viewModelScope.launch {
                                // Save photo to repository (triggers silent background AI enhancement)
                                repository.saveCapturedPhoto(photoFile)
                                _uiState.update { it.copy(isCapturing = false) }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraViewModel", "Capture error: ${exception.message}", exception)
                            // Fallback to saving placeholder / existing image if capture fails in preview container
                            viewModelScope.launch {
                                repository.saveCapturedPhoto(photoFile)
                                _uiState.update { it.copy(isCapturing = false) }
                            }
                        }
                    }
                )
            } else {
                // If Camera Hardware is non-functional or emulator preview, generate fallback photo file
                viewModelScope.launch {
                    repository.saveCapturedPhoto(photoFile)
                    _uiState.update { it.copy(isCapturing = false) }
                }
            }
        }
    }

    fun openGallery(photo: PhotoEntity? = null) {
        _uiState.update { currentState ->
            currentState.copy(
                isGalleryOpen = true,
                selectedPhotoForGallery = photo ?: currentState.latestPhoto ?: currentState.allPhotos.firstOrNull()
            )
        }
    }

    fun closeGallery() {
        _uiState.update { it.copy(isGalleryOpen = false, showCompareBeforeAfter = false) }
    }

    fun selectGalleryPhoto(photo: PhotoEntity) {
        _uiState.update { it.copy(selectedPhotoForGallery = photo, showCompareBeforeAfter = false) }
    }

    fun toggleCompareBeforeAfter() {
        _uiState.update { it.copy(showCompareBeforeAfter = !it.showCompareBeforeAfter) }
    }

    fun reEnhanceSelectedPhoto() {
        val photo = _uiState.value.selectedPhotoForGallery ?: return
        viewModelScope.launch {
            val updated = repository.reEnhancePhoto(photo)
            _uiState.update { it.copy(selectedPhotoForGallery = updated) }
        }
    }

    fun upscaleSelectedPhotoCloud() {
        val photo = _uiState.value.selectedPhotoForGallery ?: return
        viewModelScope.launch {
            val updated = repository.cloudUpscalePhoto(photo)
            _uiState.update { it.copy(selectedPhotoForGallery = updated) }
        }
    }

    fun deleteSelectedPhoto() {
        val photo = _uiState.value.selectedPhotoForGallery ?: return
        viewModelScope.launch {
            repository.deletePhoto(photo)
            val remaining = _uiState.value.allPhotos.filter { it.id != photo.id }
            _uiState.update {
                it.copy(
                    selectedPhotoForGallery = remaining.firstOrNull(),
                    isGalleryOpen = remaining.isNotEmpty()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        mediaSound.release()
    }
}

class CameraViewModelFactory(
    private val repository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
