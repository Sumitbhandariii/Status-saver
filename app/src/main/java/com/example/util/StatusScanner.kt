package com.example.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import java.io.File

object StatusScanner {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "3gp", "mkv", "mov", "webm")

    fun scanRealWhatsAppStatuses(context: Context, customTreeUri: String? = null): List<StatusItem> {
        val result = mutableListOf<StatusItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. Scan SAF TreeUri if provided
        if (!customTreeUri.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customTreeUri)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                if (rootDoc != null && rootDoc.isDirectory) {
                    val statusFolders = mutableListOf<DocumentFile>()
                    
                    // Check if root itself is .Statuses
                    if (rootDoc.name.equals(".Statuses", ignoreCase = true)) {
                        statusFolders.add(rootDoc)
                    } else {
                        // Scan for .Statuses subfolder or media folders
                        findStatusDirectories(rootDoc, statusFolders, currentDepth = 0, maxDepth = 4)
                        if (statusFolders.isEmpty()) {
                            // If no explicitly named .Statuses folder found, scan root files directly
                            statusFolders.add(rootDoc)
                        }
                    }

                    for (folder in statusFolders) {
                        val files = folder.listFiles()
                        for (doc in files) {
                            if (doc.isFile && doc.name != null && !doc.name!!.startsWith(".nomedia")) {
                                val name = doc.name!!
                                val ext = name.substringAfterLast('.', "").lowercase()
                                val mediaType = when {
                                    IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                    VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                    else -> null
                                }
                                if (mediaType != null && !seenPaths.contains(doc.uri.toString())) {
                                    val isBusiness = doc.uri.toString().contains("w4b") || doc.uri.toString().contains("Business")
                                    val item = StatusItem(
                                        id = doc.uri.toString(),
                                        title = name,
                                        uriString = doc.uri.toString(),
                                        filePath = null,
                                        mediaType = mediaType,
                                        fileSize = doc.length(),
                                        durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                                        dateModified = doc.lastModified(),
                                        isSaved = false,
                                        isFavorite = false,
                                        isNew = true,
                                        source = if (isBusiness) "WHATSAPP_BUSINESS" else "WHATSAPP"
                                    )
                                    result.add(item)
                                    seenPaths.add(doc.uri.toString())
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Scan standard WhatsApp direct file paths
        val possibleDirs = listOf(
            // Modern WhatsApp (Android 11+)
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            // WhatsApp Business Modern
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
            // Legacy WhatsApp
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
            // Legacy WhatsApp Business
            File(Environment.getExternalStorageDirectory(), "WhatsApp Business/Media/.Statuses")
        )

        for (dir in possibleDirs) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isFile && !file.name.startsWith(".nomedia") && file.length() > 0) {
                            val ext = file.extension.lowercase()
                            val mediaType = when {
                                IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                else -> null
                            }
                            if (mediaType != null && !seenPaths.contains(file.absolutePath)) {
                                val isBusiness = dir.absolutePath.contains("w4b") || dir.absolutePath.contains("Business")
                                val item = StatusItem(
                                    id = file.absolutePath,
                                    title = file.name,
                                    uriString = Uri.fromFile(file).toString(),
                                    filePath = file.absolutePath,
                                    mediaType = mediaType,
                                    fileSize = file.length(),
                                    durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                                    dateModified = file.lastModified(),
                                    isSaved = false,
                                    isFavorite = false,
                                    isNew = true,
                                    source = if (isBusiness) "WHATSAPP_BUSINESS" else "WHATSAPP"
                                )
                                result.add(item)
                                seenPaths.add(file.absolutePath)
                            }
                        }
                    }
                }
            }
        }

        // Sort newly detected statuses first
        return result.sortedByDescending { it.dateModified }
    }

    fun scanSavedStatuses(context: Context): List<StatusItem> {
        val result = mutableListOf<StatusItem>()
        val seenPaths = mutableSetOf<String>()

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
                    for (file in files) {
                        if (file.isFile && file.length() > 0 && !seenPaths.contains(file.absolutePath)) {
                            val ext = file.extension.lowercase()
                            val mediaType = when {
                                IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                else -> null
                            }
                            if (mediaType != null) {
                                val item = StatusItem(
                                    id = file.absolutePath,
                                    title = file.name,
                                    uriString = Uri.fromFile(file).toString(),
                                    filePath = file.absolutePath,
                                    mediaType = mediaType,
                                    fileSize = file.length(),
                                    durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                                    dateModified = file.lastModified(),
                                    isSaved = true,
                                    isFavorite = false,
                                    isNew = false,
                                    savedFilePath = file.absolutePath,
                                    savedDate = file.lastModified(),
                                    source = "SAVED"
                                )
                                result.add(item)
                                seenPaths.add(file.absolutePath)
                            }
                        }
                    }
                }
            }
        }

        return result.sortedByDescending { it.savedDate ?: it.dateModified }
    }

    private fun findStatusDirectories(
        currentDir: DocumentFile,
        outFolders: MutableList<DocumentFile>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth >= maxDepth) return
        try {
            val subFiles = currentDir.listFiles()
            for (sub in subFiles) {
                if (sub.isDirectory) {
                    val dirName = sub.name ?: ""
                    if (dirName.equals(".Statuses", ignoreCase = true) || dirName.equals("Statuses", ignoreCase = true)) {
                        outFolders.add(sub)
                    } else if (!dirName.startsWith(".") || dirName.equals(".Statuses", ignoreCase = true)) {
                        // Recurse down
                        findStatusDirectories(sub, outFolders, currentDepth + 1, maxDepth)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
