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

        // Method 1: Comprehensive Direct Doc ID querying for all known WhatsApp and WhatsApp Business .Statuses folders
        val candidateDocIds = linkedSetOf<String>()

        if (!rootDocId.isNullOrBlank()) {
            candidateDocIds.add(rootDocId)
            // If rootDocId is Media
            candidateDocIds.add("$rootDocId/.Statuses")
            candidateDocIds.add("$rootDocId/.statuses")
            candidateDocIds.add("$rootDocId/Statuses")
            candidateDocIds.add("$rootDocId/statuses")
            // If rootDocId is WhatsApp or WhatsApp Business
            candidateDocIds.add("$rootDocId/Media/.Statuses")
            candidateDocIds.add("$rootDocId/Media/.statuses")
            candidateDocIds.add("$rootDocId/Media/Statuses")
            candidateDocIds.add("$rootDocId/Media/statuses")
            // If rootDocId is com.whatsapp or com.whatsapp.w4b
            candidateDocIds.add("$rootDocId/WhatsApp/Media/.Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp/Media/.statuses")
            candidateDocIds.add("$rootDocId/WhatsApp/Media/Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp Business/Media/.Statuses")
            candidateDocIds.add("$rootDocId/WhatsApp Business/Media/.statuses")
            candidateDocIds.add("$rootDocId/WhatsApp Business/Media/Statuses")
            // If rootDocId is Android/media
            candidateDocIds.add("$rootDocId/com.whatsapp/WhatsApp/Media/.Statuses")
            candidateDocIds.add("$rootDocId/com.whatsapp/WhatsApp/Media/.statuses")
            candidateDocIds.add("$rootDocId/com.whatsapp/WhatsApp/Media/Statuses")
            candidateDocIds.add("$rootDocId/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses")
            candidateDocIds.add("$rootDocId/com.whatsapp.w4b/WhatsApp Business/Media/.statuses")
            candidateDocIds.add("$rootDocId/com.whatsapp.w4b/WhatsApp Business/Media/Statuses")
            candidateDocIds.add("$rootDocId/com.gbwhatsapp/GBWhatsApp/Media/.Statuses")
            candidateDocIds.add("$rootDocId/com.fmwhatsapp/FMWhatsApp/Media/.Statuses")
            candidateDocIds.add("$rootDocId/com.yowhatsapp/YoWhatsApp/Media/.Statuses")
        }

        // Standard known document ID paths across all volumes
        val volumePrefixes = listOf(volume, "primary", "0", "1").distinct()
        for (v in volumePrefixes) {
            candidateDocIds.addAll(
                listOf(
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/.statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/Statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/statuses",
                    "$v:Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses",
                    "$v:Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses",
                    "$v:Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
                    "$v:Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses",
                    "$v:Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses",
                    "$v:WhatsApp/Media/.Statuses",
                    "$v:WhatsApp/Media/.statuses",
                    "$v:WhatsApp/Media/Statuses",
                    "$v:WhatsApp/Media/statuses",
                    "$v:WhatsApp Business/Media/.Statuses",
                    "$v:WhatsApp Business/Media/.statuses",
                    "$v:WhatsApp Business/Media/Statuses",
                    "$v:WhatsApp Business/Media/statuses",
                    "$v:GBWhatsApp/Media/.Statuses",
                    "$v:DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    "$v:ParallelApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    "$v:999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
                )
            )
        }

        val queriedDocIds = mutableSetOf<String>()

        for (targetDocId in candidateDocIds) {
            if (queriedDocIds.contains(targetDocId)) continue
            queriedDocIds.add(targetDocId)

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
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        if (docId.isNullOrBlank()) continue
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
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
                // Target docId might not exist or outside tree, continue
            } finally {
                try { cursor?.close() } catch (ignored: Exception) {}
            }
        }

        // Method 2: Recursive DocumentsContract tree walk with proactive .Statuses probe
        if (!rootDocId.isNullOrBlank()) {
            val queue = ArrayDeque<String>()
            queue.add(rootDocId)
            var count = 0

            while (queue.isNotEmpty() && count < 200) {
                val currentId = queue.removeFirst()
                count++

                // Proactively probe hidden .Statuses inside this directory
                val hiddenProbes = listOf("$currentId/.Statuses", "$currentId/.statuses", "$currentId/Statuses")
                for (probeId in hiddenProbes) {
                    if (!queriedDocIds.contains(probeId)) {
                        queriedDocIds.add(probeId)
                        var probeCursor: Cursor? = null
                        try {
                            val probeUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, probeId)
                            probeCursor = context.contentResolver.query(probeUri, projection, null, null, null)
                            if (probeCursor != null) {
                                val idIdx = probeCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                                val nameIdx = probeCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                                val mimeIdx = probeCursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                                val sizeIdx = probeCursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                                val modIdx = probeCursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                                while (probeCursor.moveToNext()) {
                                    val docId = if (idIdx >= 0) probeCursor.getString(idIdx) else null
                                    if (docId.isNullOrBlank()) continue
                                    val name = if (nameIdx >= 0) probeCursor.getString(nameIdx) ?: "" else ""
                                    val mime = if (mimeIdx >= 0) probeCursor.getString(mimeIdx) ?: "" else ""
                                    val size = if (sizeIdx >= 0) probeCursor.getLong(sizeIdx) else 0L
                                    val mod = if (modIdx >= 0) probeCursor.getLong(modIdx) else System.currentTimeMillis()

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
                                                val isBusiness = probeId.contains("w4b", ignoreCase = true) || probeId.contains("Business", ignoreCase = true)
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
                        } catch (ignored: Exception) {
                        } finally {
                            try { probeCursor?.close() } catch (ignored: Exception) {}
                        }
                    }
                }

                // Query visible children of current directory
                var curCursor: Cursor? = null
                try {
                    val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentId)
                    curCursor = context.contentResolver.query(childUri, projection, null, null, null)
                    if (curCursor != null) {
                        val idIdx = curCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameIdx = curCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeIdx = curCursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeIdx = curCursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                        val modIdx = curCursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                        while (curCursor.moveToNext()) {
                            val docId = if (idIdx >= 0) curCursor.getString(idIdx) else null
                            if (docId.isNullOrBlank()) continue
                            val name = if (nameIdx >= 0) curCursor.getString(nameIdx) ?: "" else ""
                            val mime = if (mimeIdx >= 0) curCursor.getString(mimeIdx) ?: "" else ""
                            val size = if (sizeIdx >= 0) curCursor.getLong(sizeIdx) else 0L
                            val mod = if (modIdx >= 0) curCursor.getLong(modIdx) else System.currentTimeMillis()

                            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                                val lowerName = name.lowercase()
                                if (lowerName != "cache" && lowerName != "thumbnails" && lowerName != ".trash") {
                                    queue.add(docId)
                                }
                            } else if (size > 0 && !name.startsWith(".nomedia")) {
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
                                        val isBusiness = docId.contains("w4b", ignoreCase = true) || docId.contains("Business", ignoreCase = true)
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
                } catch (ignored: Exception) {
                } finally {
                    try { curCursor?.close() } catch (ignored: Exception) {}
                }
            }
        }

        // Method 3: DocumentFile tree crawler as additional fallback
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
