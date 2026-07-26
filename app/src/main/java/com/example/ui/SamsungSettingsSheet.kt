package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SamsungSettingsSheet(
    viewModel: CameraViewModel,
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    var shotSuggestions by remember { mutableStateOf(true) }
    var autoHdr by remember { mutableStateOf(true) }
    var gridLines by remember { mutableStateOf(true) }
    var watermark by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleSettings(false) },
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Camera settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scrollable Settings Items in One UI Card Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section 1: Intelligent Features
                Text(
                    text = "INTELLIGENT FEATURES",
                    color = Color(0xFFFFD800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 8.dp)
                )

                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingSwitchItem(
                            title = "Scene Optimizer",
                            subtitle = "Automatically optimize colors, contrast & clarity with seamless offline AI or online Gemini",
                            checked = uiState.isAiSceneOptimizerEnabled,
                            onCheckedChange = { viewModel.toggleAiSceneOptimizer() },
                            icon = true
                        )

                        Divider(color = Color(0xFF2A2A2E))

                        SettingSwitchItem(
                            title = "Shot suggestions",
                            subtitle = "Get on-screen guides to help align great shots",
                            checked = shotSuggestions,
                            onCheckedChange = { shotSuggestions = it }
                        )

                        Divider(color = Color(0xFF2A2A2E))

                        SettingSwitchItem(
                            title = "Auto HDR",
                            subtitle = "Capture more detail in bright and shadow areas",
                            checked = autoHdr,
                            onCheckedChange = { autoHdr = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: General Settings
                Text(
                    text = "GENERAL",
                    color = Color(0xFFFFD800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingSwitchItem(
                            title = "Grid lines",
                            subtitle = "3x3 alignment grid on viewfinder",
                            checked = gridLines,
                            onCheckedChange = { gridLines = it }
                        )

                        Divider(color = Color(0xFF2A2A2E))

                        SettingSwitchItem(
                            title = "Watermark",
                            subtitle = "Add Galaxy AI Remaster stamp to photos",
                            checked = watermark,
                            onCheckedChange = { watermark = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: About
                Text(
                    text = "ABOUT",
                    color = Color(0xFFFFD800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Galaxy Camera One UI",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Version 1.0.0 (Seamless Gemini & Offline Engine)",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFD800)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFFFC107),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
