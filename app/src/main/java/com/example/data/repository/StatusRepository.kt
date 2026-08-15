package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.StatusDao
import com.example.data.local.StatusEntity
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import com.example.util.SampleStatusProvider
import com.example.util.StatusScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

data class CleanerStats(
    val totalSavedCount: Int = 0,
    val imagesCount: Int = 0,
    val videosCount: Int = 0,
    val imagesSizeBytes: Long = 0L,
    val videosSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val totalSizeBytes: Long = 0L
) {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}

class StatusRepository(
    private val context: Context,
    private val statusDao: StatusDao = AppDatabase.getInstance(context).statusDao()
) {
    private val prefs = context.getSharedPreferences("status_vault_prefs", Context.MODE_PRIVATE)

    val allStatuses: Flow<List<StatusItem>> = statusDao.getAllStatuses().map { list ->
        list.map { it.toModel() }
    }

    val savedStatuses: Flow<List<StatusItem>> = statusDao.getSavedStatuses().map { list ->
        list.map { it.toModel() }
    }

    val favoriteStatuses: Flow<List<StatusItem>> = statusDao.getFavoriteStatuses().map { list ->
        list.map { it.toModel() }
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun getCustomTreeUri(): String? {
        return prefs.getString("custom_tree_uri", null)
    }

    fun setCustomTreeUri(uriString: String?) {
        prefs.edit().putString("custom_tree_uri", uriString).apply()
    }

    fun isSampleModeEnabled(): Boolean {
        return prefs.getBoolean("sample_mode_enabled", true)
    }

    fun setSampleModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sample_mode_enabled", enabled).apply()
    }

    suspend fun refreshStatuses(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val customUri = getCustomTreeUri()
            val realStatuses = StatusScanner.scanRealWhatsAppStatuses(context, customUri)
            val savedFiles = StatusScanner.scanSavedStatuses(context)

            val detectedItems = mutableListOf<StatusItem>()
            detectedItems.addAll(realStatuses)

            // If no real statuses found or user enabled sample mode on emulator/device, load sample pack
            if (realStatuses.isEmpty() || isSampleModeEnabled()) {
                val samples = SampleStatusProvider.generateSampleStatuses(context)
                // Add samples without duplicating existing ids
                for (s in samples) {
                    if (detectedItems.none { it.id == s.id }) {
                        detectedItems.add(s)
                    }
                }
            }

            // Sync with existing database records to preserve user preferences (favorites, saved flags)
            val entitiesToUpsert = mutableListOf<StatusEntity>()

            for (item in detectedItems) {
                val existing = statusDao.getStatusById(item.id)
                if (existing != null) {
                    // Retain user-set state
                    entitiesToUpsert.add(
                        existing.copy(
                            title = item.title,
                            uriString = item.uriString,
                            filePath = item.filePath,
                            fileSize = item.fileSize,
                            dateModified = item.dateModified
                        )
                    )
                } else {
                    // New item
                    entitiesToUpsert.add(StatusEntity.fromModel(item))
                }
            }

            // Also ensure saved media from external directory is indexed
            for (saved in savedFiles) {
                val existing = statusDao.getStatusById(saved.id)
                if (existing == null) {
                    entitiesToUpsert.add(StatusEntity.fromModel(saved))
                } else if (!existing.isSaved) {
                    statusDao.markAsSaved(saved.id, saved.filePath ?: saved.uriString, saved.savedDate ?: System.currentTimeMillis())
                }
            }

            if (entitiesToUpsert.isNotEmpty()) {
                statusDao.insertOrUpdateAll(entitiesToUpsert)
            }

            Result.success(detectedItems.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun saveStatus(status: StatusItem): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isVideo = status.mediaType == MediaType.VIDEO
            val subFolder = "StatusVault"
            val fileName = if (status.title.startsWith("status_") || status.title.startsWith("SV_")) {
                status.title
            } else {
                "SV_${System.currentTimeMillis()}_${status.title}"
            }

            val savedFile: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // MediaStore approach
                val collection = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                val relativePath = if (isVideo) "${Environment.DIRECTORY_MOVIES}/$subFolder" else "${Environment.DIRECTORY_PICTURES}/$subFolder"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(collection, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry")

                openInputStream(status).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                // Fallback local file reference
                val targetDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    subFolder
                )
                targetDir.mkdirs()
                File(targetDir, fileName)
            } else {
                // Legacy file storage
                val targetDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    subFolder
                )
                targetDir.mkdirs()
                val destFile = File(targetDir, fileName)

                openInputStream(status).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Scan with MediaScannerConnection
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(if (isVideo) "video/mp4" else "image/jpeg"),
                    null
                )
                destFile
            }

            // Also copy to App internal/external files dir as guaranteed backup
            val appSavedDir = File(
                context.getExternalFilesDir(if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES),
                subFolder
            )
            appSavedDir.mkdirs()
            val backupFile = File(appSavedDir, fileName)
            if (!backupFile.exists() && savedFile.exists()) {
                try {
                    savedFile.copyTo(backupFile, overwrite = true)
                } catch (e: Exception) {
                    // Ignore backup error
                }
            }

            val savedPath = savedFile.absolutePath
            val now = System.currentTimeMillis()
            statusDao.markAsSaved(status.id, savedPath, now)

            Result.success(savedPath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun saveAllStatuses(statuses: List<StatusItem>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        val unsaved = statuses.filter { !it.isSaved }
        for (item in unsaved) {
            val res = saveStatus(item)
            if (res.isSuccess) {
                count++
            }
        }
        Result.success(count)
    }

    suspend fun toggleFavorite(status: StatusItem) = withContext(Dispatchers.IO) {
        val newFav = !status.isFavorite
        statusDao.updateFavorite(status.id, newFav)
    }

    suspend fun markAsViewed(statusId: String) = withContext(Dispatchers.IO) {
        statusDao.markAsViewed(statusId)
    }

    suspend fun markAllAsViewed() = withContext(Dispatchers.IO) {
        statusDao.markAllAsViewed()
    }

    suspend fun deleteSavedStatus(status: StatusItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // NEVER delete original WhatsApp files. Only delete saved copies!
            if (status.savedFilePath != null) {
                val file = File(status.savedFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            if (status.filePath != null && (status.filePath.contains("StatusVault") || status.source == "SAVED")) {
                val file = File(status.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }

            // Update in Room
            if (status.source == "SAVED") {
                statusDao.deleteById(status.id)
            } else {
                statusDao.unmarkSaved(status.id)
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteSelectedSaved(statuses: List<StatusItem>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        for (s in statuses) {
            val res = deleteSavedStatus(s)
            if (res.isSuccess) count++
        }
        Result.success(count)
    }

    suspend fun deleteAllSavedMedia(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            val savedDirs = listOf(
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "StatusVault"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "StatusVault"),
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "StatusVault"),
                File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "StatusVault")
            )

            for (dir in savedDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()
                    if (files != null) {
                        for (f in files) {
                            if (f.isFile) {
                                f.delete()
                                count++
                            }
                        }
                    }
                }
            }

            statusDao.unmarkAllSaved()
            statusDao.clearUnsavedCache()
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun cleanCache(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L
            val cacheDir = context.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (f in files) {
                        freedBytes += f.length()
                        f.deleteRecursively()
                    }
                }
            }
            Result.success(freedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun computeCleanerStats(savedList: List<StatusItem>): CleanerStats = withContext(Dispatchers.IO) {
        var imgCount = 0
        var vidCount = 0
        var imgBytes = 0L
        var vidBytes = 0L

        for (item in savedList) {
            if (item.mediaType == MediaType.VIDEO) {
                vidCount++
                vidBytes += item.fileSize
            } else {
                imgCount++
                imgBytes += item.fileSize
            }
        }

        var cacheBytes = 0L
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().forEach {
                if (it.isFile) cacheBytes += it.length()
            }
        }

        CleanerStats(
            totalSavedCount = savedList.size,
            imagesCount = imgCount,
            videosCount = vidCount,
            imagesSizeBytes = imgBytes,
            videosSizeBytes = vidBytes,
            cacheSizeBytes = cacheBytes,
            totalSizeBytes = imgBytes + vidBytes + cacheBytes
        )
    }

    fun shareMedia(status: StatusItem): Intent? {
        try {
            val shareUri = getShareableUri(status) ?: return null
            val mimeType = if (status.mediaType == MediaType.VIDEO) "video/*" else "image/*"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return Intent.createChooser(intent, "Share Status via StatusVault")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareMultipleMedia(statuses: List<StatusItem>): Intent? {
        try {
            val uris = ArrayList<Uri>()
            var hasVideo = false
            var hasImage = false

            for (status in statuses) {
                val uri = getShareableUri(status)
                if (uri != null) {
                    uris.add(uri)
                    if (status.mediaType == MediaType.VIDEO) hasVideo = true else hasImage = true
                }
            }

            if (uris.isEmpty()) return null

            val mimeType = when {
                hasVideo && hasImage -> "*/*"
                hasVideo -> "video/*"
                else -> "image/*"
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return Intent.createChooser(intent, "Share ${statuses.size} Statuses via StatusVault")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getShareableUri(status: StatusItem): Uri? {
        return try {
            val filePath = status.savedFilePath ?: status.filePath
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    return FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }
            }
            Uri.parse(status.uriString)
        } catch (e: Exception) {
            e.printStackTrace()
            Uri.parse(status.uriString)
        }
    }

    private fun openInputStream(status: StatusItem): InputStream {
        val filePath = status.filePath
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                return FileInputStream(file)
            }
        }
        val uri = Uri.parse(status.uriString)
        return context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open stream for uri: $uri")
    }
}
