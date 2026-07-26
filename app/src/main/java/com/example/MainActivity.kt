package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AiEnhancer
import com.example.data.CameraDatabase
import com.example.data.PhotoRepository
import com.example.ui.CameraViewModel
import com.example.ui.CameraViewModelFactory
import com.example.ui.SamsungCameraScreen
import com.example.ui.SamsungGalleryScreen
import com.example.ui.SamsungSettingsSheet
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = CameraDatabase.getDatabase(applicationContext)
        val aiEnhancer = AiEnhancer(applicationContext)
        val repository = PhotoRepository(applicationContext, db.photoDao(), aiEnhancer)
        val viewModelFactory = CameraViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val viewModel: CameraViewModel = viewModel(factory = viewModelFactory)
                    val uiState by viewModel.uiState.collectAsState()

                    Crossfade(
                        targetState = when {
                            uiState.isSettingsOpen -> "SETTINGS"
                            uiState.isGalleryOpen -> "GALLERY"
                            else -> "CAMERA"
                        },
                        label = "MainScreenNavigation"
                    ) { screen ->
                        when (screen) {
                            "SETTINGS" -> SamsungSettingsSheet(viewModel = viewModel, uiState = uiState)
                            "GALLERY" -> SamsungGalleryScreen(viewModel = viewModel, uiState = uiState)
                            else -> SamsungCameraScreen(viewModel = viewModel, uiState = uiState)
                        }
                    }
                }
            }
        }
    }
}
