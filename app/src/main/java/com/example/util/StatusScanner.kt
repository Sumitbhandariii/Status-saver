package com.example.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import java.io.File

object StatusScanner {

    private const val TAG = "StatusScanner"
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "3gp", "mkv", "mov", "webm")

    fun scanRealWhatsAppStatuses(context: Context, customTreeUri: String? = null): List<StatusItem> {
        val result = mutableListOf<StatusItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. Scan SAF TreeUri if user granted permission
        if (!customTreeUri.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customTreeUri)
                Log.d(TAG, "Scanning granted SAF TreeUri: $treeUri")

                // Fast ContentResolver SAF DocumentsContract query & recursive DocumentFile fallback
                scanTreeUriFast(context, treeUri, result, seenPaths)

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning customTreeUri: $customTreeUri", e)
            }
        }

        // 2. Scan direct file system paths (works on Android 10 and below, dual/cloned WhatsApp, and public dirs)
        try {
            val extStorage = Environment.getExternalStorageDirectory()
            val possibleDirs = listOf(
                // Modern WhatsApp in Android/media
                File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media/Statuses"),
                File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media"),
                // WhatsApp Business Modern in Android/media
                File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses"),
                File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"),
                // Dual / Cloned / Modded WhatsApp paths
                File(extStorage, "Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses"),
                File(extStorage, "Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses"),
                File(extStorage, "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses"),
                File(extStorage, "Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses"),
                File(extStorage, "Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses"),
                // Legacy WhatsApp root paths
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
                                if (mediaType != null && !seenPaths.contains(file.absolutePath) && !seenPaths.contains(file.name)) {
                                    val isBusiness = dir.absolutePath.contains("w4b", ignoreCase = true) || dir.absolutePath.contains("Business", ignoreCase = true)
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
                                    seenPaths.add(file.name)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning direct file paths", e)
        }

        // Sort newly detected statuses by date
        return result.sortedByDescending { it.dateModified }
    }

    private fun scanTreeUriFast(
        context: Context,
        treeUri: Uri,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>
    ) {
        // Approach A: High-performance DocumentFile traversal
        val rootDoc = try {
            DocumentFile.fromTreeUri(context, treeUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed fromTreeUri", e)
            null
        }

        if (rootDoc != null) {
            val foldersToVisit = ArrayDeque<DocumentFile>()
            foldersToVisit.add(rootDoc)
            var visitedCount = 0

            while (foldersToVisit.isNotEmpty() && visitedCount < 100) {
                val currentFolder = foldersToVisit.removeFirst()
                visitedCount++

                try {
                    val files = currentFolder.listFiles()
                    for (doc in files) {
                        if (doc.isDirectory) {
                            val dirName = doc.name?.lowercase() ?: ""
                            // Follow relevant WhatsApp/Media/Statuses folders
                            if (dirName != "cache" && !dirName.startsWith(".")) {
                                foldersToVisit.add(doc)
                            } else if (dirName == ".statuses" || dirName == "statuses") {
                                foldersToVisit.add(doc)
                            }
                        } else if (doc.isFile && doc.length() > 0) {
                            val name = doc.name ?: continue
                            if (name.startsWith(".nomedia")) continue

                            val ext = name.substringAfterLast('.', "").lowercase()
                            val mediaType = when {
                                IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                else -> null
                            }

                            val uriStr = doc.uri.toString()
                            if (mediaType != null && !seenPaths.contains(uriStr) && !seenPaths.contains(name)) {
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
                                seenPaths.add(name)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error listing files in folder: ${currentFolder.name}", e)
                }
            }
        }

        // Approach B: Query DocumentsContract Child Documents directly if tree URI has documentId
        try {
            val docId = if (DocumentsContract.isDocumentUri(context, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }
            if (!docId.isNullOrBlank()) {
                queryDocumentsContractChildren(context, treeUri, docId, result, seenPaths, depth = 0, maxDepth = 6)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DocumentsContract query error", e)
        }
    }

    private fun queryDocumentsContractChildren(
        context: Context,
        treeUri: Uri,
        parentId: String,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return
        var cursor: Cursor? = null
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
            if (cursor != null) {
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val childDocId = if (idIdx >= 0) cursor.getString(idIdx) else null ?: continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else ""
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val mod = if (modIdx >= 0) cursor.getLong(modIdx) else System.currentTimeMillis()

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val lowerName = name.lowercase()
                        if (lowerName != "cache" && !lowerName.startsWith(".")) {
                            queryDocumentsContractChildren(context, treeUri, childDocId, result, seenPaths, depth + 1, maxDepth)
                        } else if (lowerName == ".statuses" || lowerName == "statuses") {
                            queryDocumentsContractChildren(context, treeUri, childDocId, result, seenPaths, depth + 1, maxDepth)
                        }
                    } else if (size > 0 && !name.startsWith(".nomedia")) {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val mediaType = when {
                            mime.startsWith("image/") || IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                            mime.startsWith("video/") || VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                            else -> null
                        }

                        if (mediaType != null) {
                            val childDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                            val uriStr = childDocUri.toString()
                            if (!seenPaths.contains(uriStr) && !seenPaths.contains(name)) {
                                val isBusiness = childDocId.contains("w4b", ignoreCase = true) || childDocId.contains("Business", ignoreCase = true)
                                val item = StatusItem(
                                    id = uriStr,
                                    title = name,
                                    uriString = uriStr,
                                    filePath = null,
                                    mediaType = mediaType,
                                    fileSize = size,
                                    durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                                    dateModified = if (mod > 0) mod else System.currentTimeMillis(),
                                    isSaved = false,
                                    isFavorite = false,
                                    isNew = true,
                                    source = if (isBusiness) "WHATSAPP_BUSINESS" else "WHATSAPP"
                                )
                                result.add(item)
                                seenPaths.add(uriStr)
                                seenPaths.add(name)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in queryDocumentsContractChildren for parent: $parentId", e)
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
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
}
