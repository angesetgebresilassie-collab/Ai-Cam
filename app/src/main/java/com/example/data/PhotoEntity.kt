package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val enhancedPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val enhancementStatus: String = STATUS_PROCESSING, // PROCESSING, COMPLETED, RAW, FAILED
    val sceneCategory: String = "Auto Scene", // Sunset, Portrait, Landscape, Food, Night, Document, General
    val width: Int = 0,
    val height: Int = 0,
    val enhancementSummary: String? = null
) {
    companion object {
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_RAW = "RAW"
        const val STATUS_FAILED = "FAILED"
    }

    val displayPath: String
        get() = if (enhancementStatus == STATUS_COMPLETED && !enhancedPath.isNullOrEmpty()) {
            enhancedPath
        } else {
            originalPath
        }
}
