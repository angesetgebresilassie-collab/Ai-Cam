package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.R
import com.example.ai.AiEnhancer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val aiEnhancer: AiEnhancer
) {
    val allPhotos: Flow<List<PhotoEntity>> = photoDao.getAllPhotos()
    val latestPhoto: Flow<PhotoEntity?> = photoDao.getLatestPhoto()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    suspend fun initializeSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = photoDao.getAllPhotos().firstOrNull()
        if (existing.isNullOrEmpty()) {
            try {
                val sampleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_sample_sunset)
                if (sampleBitmap != null) {
                    val sampleFile = File(context.filesDir, "sample_sunset.jpg")
                    FileOutputStream(sampleFile).use { out ->
                        sampleBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    val entity = PhotoEntity(
                        originalPath = sampleFile.absolutePath,
                        enhancementStatus = PhotoEntity.STATUS_PROCESSING,
                        sceneCategory = "Sunset",
                        width = sampleBitmap.width,
                        height = sampleBitmap.height
                    )
                    val id = photoDao.insertPhoto(entity)

                    // Run silent AI enhancement on sample
                    repositoryScope.launch {
                        try {
                            val result = aiEnhancer.enhancePhotoSilently(sampleFile)
                            val updated = entity.copy(
                                id = id,
                                enhancedPath = result.enhancedFilePath,
                                enhancementStatus = PhotoEntity.STATUS_COMPLETED,
                                sceneCategory = result.sceneCategory,
                                enhancementSummary = result.enhancementSummary
                            )
                            photoDao.updatePhoto(updated)
                        } catch (e: Exception) {
                            photoDao.updatePhoto(entity.copy(id = id, enhancementStatus = PhotoEntity.STATUS_RAW))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveCapturedPhoto(photoFile: File): PhotoEntity = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.absolutePath, options)

        val initialEntity = PhotoEntity(
            originalPath = photoFile.absolutePath,
            enhancementStatus = PhotoEntity.STATUS_PROCESSING,
            sceneCategory = "Auto Scene",
            width = options.outWidth,
            height = options.outHeight
        )

        val photoId = photoDao.insertPhoto(initialEntity)
        val savedEntity = initialEntity.copy(id = photoId)

        // Silent AI Enhancement in background
        repositoryScope.launch {
            try {
                val result = aiEnhancer.enhancePhotoSilently(photoFile)
                val updatedEntity = savedEntity.copy(
                    enhancedPath = result.enhancedFilePath,
                    enhancementStatus = PhotoEntity.STATUS_COMPLETED,
                    sceneCategory = result.sceneCategory,
                    enhancementSummary = result.enhancementSummary
                )
                photoDao.updatePhoto(updatedEntity)
            } catch (e: Exception) {
                e.printStackTrace()
                photoDao.updatePhoto(savedEntity.copy(enhancementStatus = PhotoEntity.STATUS_RAW))
            }
        }

        savedEntity
    }

    suspend fun reEnhancePhoto(photo: PhotoEntity): PhotoEntity = withContext(Dispatchers.IO) {
        val originalFile = File(photo.originalPath)
        if (originalFile.exists()) {
            photoDao.updatePhoto(photo.copy(enhancementStatus = PhotoEntity.STATUS_PROCESSING))
            val result = aiEnhancer.enhancePhotoSilently(originalFile)
            val updated = photo.copy(
                enhancedPath = result.enhancedFilePath,
                enhancementStatus = PhotoEntity.STATUS_COMPLETED,
                sceneCategory = result.sceneCategory,
                enhancementSummary = result.enhancementSummary
            )
            photoDao.updatePhoto(updated)
            updated
        } else {
            photo
        }
    }

    suspend fun cloudUpscalePhoto(photo: PhotoEntity): PhotoEntity = withContext(Dispatchers.IO) {
        val originalFile = File(photo.originalPath)
        if (originalFile.exists()) {
            photoDao.updatePhoto(photo.copy(enhancementStatus = PhotoEntity.STATUS_PROCESSING))
            val result = aiEnhancer.upscalePhotoCloud(originalFile)
            val updated = photo.copy(
                enhancedPath = result.enhancedFilePath,
                enhancementStatus = PhotoEntity.STATUS_COMPLETED,
                sceneCategory = result.sceneCategory,
                enhancementSummary = result.enhancementSummary,
                width = photo.width * 2,
                height = photo.height * 2
            )
            photoDao.updatePhoto(updated)
            updated
        } else {
            photo
        }
    }

    suspend fun deletePhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        try {
            File(photo.originalPath).delete()
            photo.enhancedPath?.let { File(it).delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        photoDao.deletePhotoById(photo.id)
    }
}
