package com.example.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import java.io.File
import java.net.URLDecoder

object StatusScanner {

    private const val TAG = "StatusScanner"
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val VIDEO_EXTENSIONS = setOf("mp4", "3gp", "mkv", "mov", "webm", "avi", "ts")

    fun detectMediaType(fileName: String, mimeType: String? = null): MediaType? {
        val lowerMime = mimeType?.lowercase() ?: ""
        if (lowerMime.startsWith("image/")) return MediaType.IMAGE
        if (lowerMime.startsWith("video/")) return MediaType.VIDEO

        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
            VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
            else -> null
        }
    }

    /**
     * Builds list of possible direct file system directories for WhatsApp statuses.
     */
    fun buildWhatsAppStatusDirectories(rootDir: File): List<File> {
        val candidates = mutableListOf<File>()
        val possibleRoots = listOf(
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/.statuses"),
            File(rootDir, "Android/media/com.whatsapp/WhatsApp/Media/Statuses"),
            File(rootDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
            File(rootDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.statuses"),
            File(rootDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses"),
            File(rootDir, "Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses"),
            File(rootDir, "Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses"),
            File(rootDir, "WhatsApp/Media/.Statuses"),
            File(rootDir, "WhatsApp/Media/.statuses"),
            File(rootDir, "WhatsApp/Media/Statuses"),
            File(rootDir, "WhatsApp Business/Media/.Statuses"),
            File(rootDir, "WhatsApp Business/Media/.statuses"),
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

    /**
     * Scans real WhatsApp statuses using SAF Tree URIs, Persisted Permissions, and direct File fallback.
     */
    fun scanRealWhatsAppStatuses(context: Context, customTreeUri: String? = null): List<StatusItem> {
        val result = mutableListOf<StatusItem>()
        val seenKeys = mutableSetOf<String>()

        val urisToScan = linkedSetOf<Uri>()

        // 1. Add user selected custom tree URI
        if (!customTreeUri.isNullOrBlank()) {
            try {
                urisToScan.add(Uri.parse(customTreeUri))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing customTreeUri: $customTreeUri", e)
            }
        }

        // 2. Add all persisted URI permissions
        try {
            val persistedPerms = context.contentResolver.persistedUriPermissions
            for (perm in persistedPerms) {
                if (perm.isReadPermission && perm.uri != null) {
                    urisToScan.add(perm.uri)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading persistedUriPermissions", e)
        }

        // 3. Scan all SAF Tree URIs
        for (treeUri in urisToScan) {
            try {
                scanSafTree(context, treeUri, result, seenKeys)
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning treeUri: $treeUri", e)
            }
        }

        // 4. Scan direct file system paths for older Android or accessible paths
        try {
            val extStorage = Environment.getExternalStorageDirectory()
            val possibleDirs = buildWhatsAppStatusDirectories(extStorage)

            for (dir in possibleDirs) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (!file.isFile || file.name.startsWith(".nomedia")) continue
                    val mediaType = detectMediaType(file.name) ?: continue
                    val key = file.name
                    if (seenKeys.contains(key) || seenKeys.contains(file.absolutePath)) continue

                    val isBusiness = dir.absolutePath.contains("w4b", ignoreCase = true)
                        || dir.absolutePath.contains("Business", ignoreCase = true)

                    val item = StatusItem(
                        id = file.absolutePath,
                        title = file.name,
                        uriString = Uri.fromFile(file).toString(),
                        filePath = file.absolutePath,
                        mediaType = mediaType,
                        fileSize = file.length().coerceAtLeast(0L),
                        durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                        dateModified = if (file.lastModified() > 0) file.lastModified() else System.currentTimeMillis(),
                        isSaved = false,
                        isFavorite = false,
                        isNew = true,
                        source = if (isBusiness) "WHATSAPP_BUSINESS" else "WHATSAPP"
                    )
                    result.add(item)
                    seenKeys.add(key)
                    seenKeys.add(file.absolutePath)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning direct file paths", e)
        }

        return result.sortedByDescending { it.dateModified }
    }

    private fun scanSafTree(
        context: Context,
        treeUri: Uri,
        result: MutableList<StatusItem>,
        seenKeys: MutableSet<String>
    ) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val rootDocId = try {
            if (DocumentsContract.isDocumentUri(context, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }
        } catch (e: Exception) {
            null
        }

        val queriedDocIds = mutableSetOf<String>()

        // 1. Build all candidate document IDs based on the granted tree root
        val candidateDocIds = linkedSetOf<String>()

        if (!rootDocId.isNullOrBlank()) {
            val cleanDocId = rootDocId.trimEnd('/')
            val decodedDocId = try { URLDecoder.decode(cleanDocId, "UTF-8") } catch (e: Exception) { cleanDocId }

            candidateDocIds.add(cleanDocId)
            candidateDocIds.add(decodedDocId)

            val relativeSubPaths = listOf(
                ".Statuses", ".statuses", "Statuses", "statuses",
                "Media/.Statuses", "Media/.statuses", "Media/Statuses", "Media/statuses",
                "WhatsApp/Media/.Statuses", "WhatsApp/Media/.statuses", "WhatsApp/Media/Statuses",
                "WhatsApp Business/Media/.Statuses", "WhatsApp Business/Media/.statuses",
                "com.whatsapp/WhatsApp/Media/.Statuses", "com.whatsapp/WhatsApp/Media/.statuses",
                "com.whatsapp.w4b/WhatsApp Business/Media/.Statuses", "com.whatsapp.w4b/WhatsApp Business/Media/.statuses",
                "com.whatsapp.clone/WhatsApp/Media/.Statuses", "com.whatsapp.dual/WhatsApp/Media/.Statuses",
                "com.gbwhatsapp/GBWhatsApp/Media/.Statuses", "com.fmwhatsapp/FMWhatsApp/Media/.Statuses",
                "com.yowhatsapp/YoWhatsApp/Media/.Statuses"
            )

            for (sp in relativeSubPaths) {
                candidateDocIds.add("$cleanDocId/$sp")
                candidateDocIds.add("$decodedDocId/$sp")
            }

            // Extract volume prefix (e.g. primary, 0, etc.)
            val volume = if (cleanDocId.contains(":")) cleanDocId.substringBefore(":") else "primary"
            val volumes = listOf(volume, "primary", "0", "1").distinct()

            for (v in volumes) {
                candidateDocIds.add("$v:Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
                candidateDocIds.add("$v:Android/media/com.whatsapp/WhatsApp/Media/.statuses")
                candidateDocIds.add("$v:Android/media/com.whatsapp/WhatsApp/Media/Statuses")
                candidateDocIds.add("$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses")
                candidateDocIds.add("$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.statuses")
                candidateDocIds.add("$v:Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses")
                candidateDocIds.add("$v:Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses")
                candidateDocIds.add("$v:Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses")
                candidateDocIds.add("$v:WhatsApp/Media/.Statuses")
                candidateDocIds.add("$v:WhatsApp/Media/.statuses")
                candidateDocIds.add("$v:WhatsApp Business/Media/.Statuses")
                candidateDocIds.add("$v:GBWhatsApp/Media/.Statuses")
            }
        }

        // Query all candidate .Statuses docIds directly
        for (targetDocId in candidateDocIds) {
            queryDocIdChildren(context, treeUri, targetDocId, projection, result, seenKeys, queriedDocIds)
        }

        // 2. Perform BFS directory traversal to discover any hidden .Statuses directory
        if (!rootDocId.isNullOrBlank()) {
            val queue = ArrayDeque<String>()
            queue.add(rootDocId)
            var visitedCount = 0

            while (queue.isNotEmpty() && visitedCount < 150) {
                val currentDocId = queue.removeFirst()
                visitedCount++

                // Proactively probe hidden .Statuses under this directory
                queryDocIdChildren(context, treeUri, "$currentDocId/.Statuses", projection, result, seenKeys, queriedDocIds)
                queryDocIdChildren(context, treeUri, "$currentDocId/.statuses", projection, result, seenKeys, queriedDocIds)

                var cursor: Cursor? = null
                try {
                    val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
                    cursor = context.contentResolver.query(childUri, projection, null, null, null)
                    if (cursor != null) {
                        val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                        val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                        while (cursor.moveToNext()) {
                            val childId = if (idIdx >= 0) cursor.getString(idIdx) else null ?: continue
                            val childName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                            val childMime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                            val childSize = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                            val childMod = if (modIdx >= 0) cursor.getLong(modIdx) else System.currentTimeMillis()

                            if (childMime == DocumentsContract.Document.MIME_TYPE_DIR) {
                                val lower = childName.lowercase()
                                if (lower == ".statuses" || lower == "statuses") {
                                    queryDocIdChildren(context, treeUri, childId, projection, result, seenKeys, queriedDocIds)
                                } else if (lower != "cache" && lower != "thumbnails" && lower != ".trash" && lower != "databases") {
                                    queue.add(childId)
                                }
                            } else if (!childName.startsWith(".nomedia")) {
                                val mediaType = detectMediaType(childName, childMime)
                                if (mediaType != null) {
                                    addStatusFromCursor(treeUri, childId, childName, childMime, childSize, childMod, mediaType, result, seenKeys)
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {
                } finally {
                    try { cursor?.close() } catch (ignored: Exception) {}
                }
            }
        }
    }

    private fun queryDocIdChildren(
        context: Context,
        treeUri: Uri,
        targetDocId: String,
        projection: Array<String>,
        result: MutableList<StatusItem>,
        seenKeys: MutableSet<String>,
        queriedDocIds: MutableSet<String>
    ) {
        if (queriedDocIds.contains(targetDocId)) return
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
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else null ?: continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val mod = if (modIdx >= 0) cursor.getLong(modIdx) else System.currentTimeMillis()

                    if (mime != DocumentsContract.Document.MIME_TYPE_DIR && !name.startsWith(".nomedia")) {
                        val mediaType = detectMediaType(name, mime)
                        if (mediaType != null) {
                            addStatusFromCursor(treeUri, docId, name, mime, size, mod, mediaType, result, seenKeys)
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        } finally {
            try { cursor?.close() } catch (ignored: Exception) {}
        }
    }

    private fun addStatusFromCursor(
        treeUri: Uri,
        docId: String,
        name: String,
        mime: String,
        size: Long,
        mod: Long,
        mediaType: MediaType,
        result: MutableList<StatusItem>,
        seenKeys: MutableSet<String>
    ) {
        val displayName = if (name.isNotBlank()) name else "status_${System.currentTimeMillis()}"
        val childDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val uriStr = childDocUri.toString()

        if (seenKeys.contains(displayName) || seenKeys.contains(uriStr)) return

        val isBusiness = docId.contains("w4b", ignoreCase = true) || docId.contains("Business", ignoreCase = true)
        val item = StatusItem(
            id = uriStr,
            title = displayName,
            uriString = uriStr,
            filePath = null,
            mediaType = mediaType,
            fileSize = size.coerceAtLeast(0L),
            durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
            dateModified = if (mod > 0) mod else System.currentTimeMillis(),
            isSaved = false,
            isFavorite = false,
            isNew = true,
            source = if (isBusiness) "WHATSAPP_BUSINESS" else "WHATSAPP"
        )
        result.add(item)
        seenKeys.add(displayName)
        seenKeys.add(uriStr)
    }

    /**
     * Scans all statuses saved into phone gallery/storage by StatusVault.
     */
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
                            val mediaType = detectMediaType(file.name)
                            if (mediaType != null) {
                                val item = StatusItem(
                                    id = file.absolutePath,
                                    title = file.name,
                                    uriString = Uri.fromFile(file).toString(),
                                    filePath = file.absolutePath,
                                    mediaType = mediaType,
                                    fileSize = file.length(),
                                    durationMs = if (mediaType == MediaType.VIDEO) 15000L else 0L,
                                    dateModified = if (file.lastModified() > 0) file.lastModified() else System.currentTimeMillis(),
                                    isSaved = true,
                                    isFavorite = false,
                                    isNew = false,
                                    savedDate = file.lastModified(),
                                    savedFilePath = file.absolutePath,
                                    source = "SAVED"
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

        return result.sortedByDescending { it.savedDate ?: it.dateModified }
    }
}
