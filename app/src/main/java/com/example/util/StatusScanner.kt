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

    fun detectMediaType(fileName: String): MediaType? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
            VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
            else -> null
        }
    }

    fun buildWhatsAppStatusDirectories(rootDir: File): List<File> {
        val candidates = mutableListOf<File>()
        val possibleRoots = listOf(
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/Statuses"),
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media"),
            File(rootDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
            File(rootDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses"),
            File(rootDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"),
            File(rootDir, "Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses"),
            File(rootDir, "WhatsApp/Media/.Statuses"),
            File(rootDir, "WhatsApp/Media/Statuses"),
            File(rootDir, "WhatsApp Business/Media/.Statuses"),
            File(rootDir, "WhatsApp Business/Media/Statuses"),
            File(rootDir, "GBWhatsApp/Media/.Statuses"),
            File(rootDir, "DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(rootDir, "ParallelApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(rootDir, "999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
        )

        for (path in possibleRoots) {
            if (path.exists() && path.isDirectory) {
                candidates.add(path)
            }
        }

        return candidates.distinctBy { it.absolutePath }
    }

    fun scanRealWhatsAppStatuses(context: Context, customTreeUri: String? = null): List<StatusItem> {
        val result = mutableListOf<StatusItem>()
        val seenPaths = mutableSetOf<String>()

        if (!customTreeUri.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(customTreeUri)
                Log.d(TAG, "Scanning granted SAF TreeUri: $treeUri")
                scanTreeUriFast(context, treeUri, result, seenPaths)
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning customTreeUri: $customTreeUri", e)
            }
        }

        try {
            val extStorage = Environment.getExternalStorageDirectory()
            val possibleDirs = buildWhatsAppStatusDirectories(extStorage)

            for (dir in possibleDirs) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (!file.isFile || file.name.startsWith(".nomedia") || file.length() <= 0) continue
                    val mediaType = detectMediaType(file.name)
                    if (mediaType == null) continue
                    val key = file.absolutePath
                    if (seenPaths.contains(key) || seenPaths.contains(file.name)) continue

                    val isBusiness = dir.absolutePath.contains("w4b", ignoreCase = true)
                        || dir.absolutePath.contains("Business", ignoreCase = true)

                    val item = StatusItem(
                        id = key,
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
                    seenPaths.add(key)
                    seenPaths.add(file.name)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning direct file paths", e)
        }

        return result.sortedByDescending { it.dateModified }
    }

    private fun scanTreeUriFast(
        context: Context,
        treeUri: Uri,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>
    ) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        // Determine volume and root document ID
        val rootDocId = try {
            if (DocumentsContract.isDocumentUri(context, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }
        } catch (e: Exception) {
            null
        }

        val volume = if (!rootDocId.isNullOrBlank() && rootDocId.contains(":")) {
            rootDocId.substringBefore(":")
        } else {
            "primary"
        }

        // Method 1: Direct Doc ID querying for all known WhatsApp and WhatsApp Business .Statuses folders
        // Querying the folder's doc ID directly returns all media files inside it even if the folder is hidden from directory pickers!
        val candidateDocIds = linkedSetOf<String>()

        if (!rootDocId.isNullOrBlank()) {
            candidateDocIds.add(rootDocId)
            candidateDocIds.add("$rootDocId/.Statuses")
            candidateDocIds.add("$rootDocId/Statuses")
            candidateDocIds.add("$rootDocId/Media/.Statuses")
            candidateDocIds.add("$rootDocId/Media/Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp/Media/.Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp/Media/Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp Business/Media/.Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp Business/Media/Statuses")
        }

        // Standard known document ID paths
        candidateDocIds.addAll(
            listOf(
                "$volume:Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                "$volume:Android/media/com.whatsapp/WhatsApp/Media/Statuses",
                "$volume:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                "$volume:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses",
                "$volume:Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses",
                "$volume:Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses",
                "$volume:Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
                "$volume:Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses",
                "$volume:Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses",
                "$volume:WhatsApp/Media/.Statuses",
                "$volume:WhatsApp/Media/Statuses",
                "$volume:WhatsApp Business/Media/.Statuses",
                "$volume:WhatsApp Business/Media/Statuses",
                "$volume:GBWhatsApp/Media/.Statuses",
                "$volume:DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                "$volume:ParallelApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                "$volume:999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
            )
        )

        for (targetDocId in candidateDocIds) {
            var cursor: Cursor? = null
            try {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, targetDocId)
                cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor != null) {
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else null ?: continue
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else ""
                        val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                        val mod = if (modIdx >= 0) cursor.getLong(modIdx) else System.currentTimeMillis()

                        if (mime != DocumentsContract.Document.MIME_TYPE_DIR && size > 0 && !name.startsWith(".nomedia")) {
                            val ext = name.substringAfterLast('.', "").lowercase()
                            val mediaType = when {
                                mime.startsWith("image/") || IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                mime.startsWith("video/") || VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                else -> null
                            }

                            if (mediaType != null) {
                                val childDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                val uriStr = childDocUri.toString()
                                if (!seenPaths.contains(uriStr) && !seenPaths.contains(name)) {
                                    val isBusiness = targetDocId.contains("w4b", ignoreCase = true) || targetDocId.contains("Business", ignoreCase = true)
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
                // Target docId might not exist on this specific device, continue to next
            } finally {
                try { cursor?.close() } catch (ignored: Exception) {}
            }
        }

        // Method 2: DocumentFile tree crawler as additional fallback
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null) {
                val queue = ArrayDeque<DocumentFile>()
                queue.add(rootDoc)
                var count = 0
                while (queue.isNotEmpty() && count < 300) {
                    val folder = queue.removeFirst()
                    count++
                    val files = folder.listFiles()
                    for (file in files) {
                        if (file.isDirectory) {
                            val dirName = file.name?.lowercase() ?: ""
                            if (dirName != "cache" && dirName != "thumbnails" && dirName != ".trash") {
                                queue.add(file)
                            }
                        } else if (file.isFile && file.length() > 0) {
                            val name = file.name ?: ""
                            if (!name.startsWith(".nomedia")) {
                                val ext = name.substringAfterLast('.', "").lowercase()
                                val mediaType = when {
                                    IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
                                    VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
                                    else -> null
                                }
                                val uriString = file.uri.toString()
                                if (mediaType != null && !seenPaths.contains(uriString) && !seenPaths.contains(name)) {
                                    val isBusiness = uriString.contains("w4b", ignoreCase = true) || uriString.contains("Business", ignoreCase = true)
                                    val item = StatusItem(
                                        id = uriString,
                                        title = name,
                                        uriString = uriString,
                                        filePath = null,
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
                                    seenPaths.add(uriString)
                                    seenPaths.add(name)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during DocumentFile crawl", e)
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
