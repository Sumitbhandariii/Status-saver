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

        // 1. Scan SAF TreeUri if user granted permission to Android/media or any subfolder
        if (!customTreeUri.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customTreeUri)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                if (rootDoc != null) {
                    val allDocFolders = mutableListOf<DocumentFile>()
                    if (rootDoc.isDirectory) {
                        allDocFolders.add(rootDoc)
                        collectAllFolders(rootDoc, allDocFolders, currentDepth = 0, maxDepth = 10)
                    }

                    for (folder in allDocFolders.distinctBy { it.uri.toString() }) {
                        try {
                            val folderName = folder.name ?: ""
                            val files = folder.listFiles()
                            for (doc in files) {
                                if (doc.isFile && doc.name != null && !doc.name!!.startsWith(".nomedia") && doc.length() > 0) {
                                    val name = doc.name!!
                                    val ext = name.substringAfterLast('.', "").lowercase()
                                    val mediaType = when {
                                        IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                        VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                        else -> null
                                    }
                                    if (mediaType != null && !seenPaths.contains(doc.uri.toString())) {
                                        val uriStr = doc.uri.toString()
                                        val isBusiness = uriStr.contains("w4b", ignoreCase = true) || uriStr.contains("Business", ignoreCase = true)
                                        val item = StatusItem(
                                            id = uriStr,
                                            title = name,
                                            uriString = uriStr,
                                            filePath = null,
                                            mediaType = mediaType,
                                            fileSize = doc.length(),
                                            durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                                            dateModified = if (doc.lastModified() > 0) doc.lastModified() else System.currentTimeMillis(),
                                            isSaved = false,
                                            isFavorite = false,
                                            isNew = true,
                                            source = if (isBusiness) "WHATSAPP_BUSINESS" else "WHATSAPP"
                                        )
                                        result.add(item)
                                        seenPaths.add(uriStr)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Scan standard WhatsApp direct file paths (covers Android 9, 10, 11, 12, 13, 14, 15, Dual WhatsApp)
        val extStorage = Environment.getExternalStorageDirectory()
        val possibleDirs = listOf(
            // Modern WhatsApp in Android/media (Android 11+)
            File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media/Statuses"),
            File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media"),
            // WhatsApp Business Modern in Android/media
            File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
            File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses"),
            File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"),
            // Cloned / Dual WhatsApp
            File(extStorage, "Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses"),
            File(extStorage, "Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses"),
            File(extStorage, "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses"),
            File(extStorage, "Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses"),
            File(extStorage, "Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses"),
            // Legacy WhatsApp root paths (Android 10 and below)
            File(extStorage, "WhatsApp/Media/.Statuses"),
            File(extStorage, "WhatsApp/Media/Statuses"),
            File(extStorage, "WhatsApp Business/Media/.Statuses"),
            File(extStorage, "WhatsApp Business/Media/Statuses"),
            File(extStorage, "GBWhatsApp/Media/.Statuses"),
            File(extStorage, "DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(extStorage, "ParallelApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(extStorage, "999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
        )

        for (dir in possibleDirs) {
            try {
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
                                        dateModified = if (file.lastModified() > 0) file.lastModified() else System.currentTimeMillis(),
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
            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun collectAllFolders(
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
                    outFolders.add(sub)
                    collectAllFolders(sub, outFolders, currentDepth + 1, maxDepth)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
