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

        val queriedDocIds = mutableSetOf<String>()

        // 1. If user selected .Statuses folder directly or rootDocId is already .Statuses
        if (!rootDocId.isNullOrBlank()) {
            val isDirectStatuses = rootDocId.endsWith(".Statuses", ignoreCase = true) ||
                rootDocId.endsWith("Statuses", ignoreCase = true)

            if (isDirectStatuses) {
                queryDocIdChildren(context, treeUri, rootDocId, projection, result, seenPaths, queriedDocIds)
            }
        }

        // 2. Comprehensive Direct Doc ID candidates across common prefixes, volumes, and case variations
        val candidateDocIds = linkedSetOf<String>()

        if (!rootDocId.isNullOrBlank()) {
            val cleanRoot = rootDocId.trimEnd('/')
            candidateDocIds.add(cleanRoot)

            // Direct relative subpaths to hidden .Statuses from whatever folder was selected
            val subPaths = listOf(
                ".Statuses", ".statuses", "Statuses", "statuses",
                "Media/.Statuses", "Media/.statuses", "Media/Statuses", "Media/statuses",
                "media/.Statuses", "media/.statuses", "media/Statuses", "media/statuses",
                "WhatsApp/Media/.Statuses", "WhatsApp/Media/.statuses", "WhatsApp/Media/Statuses", "WhatsApp/Media/statuses",
                "WhatsApp/media/.Statuses", "WhatsApp/media/.statuses", "WhatsApp/media/Statuses",
                "whatsapp/media/.Statuses", "whatsapp/media/.statuses",
                "WhatsApp Business/Media/.Statuses", "WhatsApp Business/Media/.statuses", "WhatsApp Business/Media/Statuses", "WhatsApp Business/Media/statuses",
                "WhatsApp Business/media/.Statuses", "WhatsApp Business/media/.statuses",
                "com.whatsapp/WhatsApp/Media/.Statuses", "com.whatsapp/WhatsApp/Media/.statuses",
                "com.whatsapp/WhatsApp/Media/Statuses", "com.whatsapp/WhatsApp/Media/statuses",
                "com.whatsapp/WhatsApp/media/.Statuses", "com.whatsapp/WhatsApp/media/.statuses",
                "com.whatsapp.w4b/WhatsApp Business/Media/.Statuses", "com.whatsapp.w4b/WhatsApp Business/Media/.statuses",
                "com.whatsapp.w4b/WhatsApp Business/Media/Statuses", "com.whatsapp.w4b/WhatsApp Business/media/.Statuses",
                "com.whatsapp.clone/WhatsApp/Media/.Statuses", "com.whatsapp.clone/WhatsApp/Media/.statuses",
                "com.whatsapp.dual/WhatsApp/Media/.Statuses", "com.whatsapp.dual/WhatsApp/Media/.statuses",
                "com.gbwhatsapp/GBWhatsApp/Media/.Statuses", "com.gbwhatsapp/GBWhatsApp/Media/.statuses",
                "com.fmwhatsapp/FMWhatsApp/Media/.Statuses", "com.fmwhatsapp/FMWhatsApp/Media/.statuses",
                "com.yowhatsapp/YoWhatsApp/Media/.Statuses", "com.yowhatsapp/YoWhatsApp/Media/.statuses"
            )
            for (sp in subPaths) {
                candidateDocIds.add("$cleanRoot/$sp")
            }
        }

        // Standard known document ID paths across all storage volumes
        val volumePrefixes = listOf(volume, "primary", "0", "1", "sdcard").distinct()
        for (v in volumePrefixes) {
            candidateDocIds.addAll(
                listOf(
                    // Android 11+ Scoped Storage standard WhatsApp path
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/.statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/Statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/Media/statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/media/.Statuses",
                    "$v:Android/media/com.whatsapp/WhatsApp/media/.statuses",
                    "$v:Android/media/com.whatsapp/whatsapp/media/.Statuses",
                    "$v:Android/media/com.whatsapp/whatsapp/media/.statuses",
                    // Android 11+ WhatsApp Business path
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/media/.Statuses",
                    "$v:Android/media/com.whatsapp.w4b/WhatsApp Business/media/.statuses",
                    // Dual / Clone WhatsApp paths
                    "$v:Android/media/com.whatsapp.clone/WhatsApp/Media/.Statuses",
                    "$v:Android/media/com.whatsapp.clone/WhatsApp/Media/.statuses",
                    "$v:Android/media/com.whatsapp.dual/WhatsApp/Media/.Statuses",
                    "$v:Android/media/com.whatsapp.dual/WhatsApp/Media/.statuses",
                    "$v:Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
                    "$v:Android/media/com.gbwhatsapp/GBWhatsApp/Media/.statuses",
                    "$v:Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses",
                    "$v:Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses",
                    // Android 10 and older root WhatsApp paths
                    "$v:WhatsApp/Media/.Statuses",
                    "$v:WhatsApp/Media/.statuses",
                    "$v:WhatsApp/Media/Statuses",
                    "$v:WhatsApp/Media/statuses",
                    "$v:WhatsApp/media/.Statuses",
                    "$v:WhatsApp/media/.statuses",
                    "$v:whatsapp/media/.statuses",
                    "$v:WhatsApp Business/Media/.Statuses",
                    "$v:WhatsApp Business/Media/.statuses",
                    "$v:WhatsApp Business/Media/Statuses",
                    "$v:WhatsApp Business/media/.Statuses",
                    "$v:GBWhatsApp/Media/.Statuses",
                    "$v:DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    "$v:ParallelApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                    "$v:999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
                )
            )
        }

        for (targetDocId in candidateDocIds) {
            queryDocIdChildren(context, treeUri, targetDocId, projection, result, seenPaths, queriedDocIds)
        }

        // 3. DocumentFile Hierarchy Probing (solves hidden .Statuses resolution when parent is selected)
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null) {
                probeDocumentFileHierarchies(treeUri, rootDoc, result, seenPaths)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error probing DocumentFile hierarchies", e)
        }

        // 4. Recursive DocumentsContract tree walk with proactive .Statuses probe
        if (!rootDocId.isNullOrBlank()) {
            val queue = ArrayDeque<String>()
            queue.add(rootDocId)
            var count = 0

            while (queue.isNotEmpty() && count < 250) {
                val currentId = queue.removeFirst()
                count++

                // Proactively probe hidden .Statuses inside this directory
                val hiddenProbes = listOf("$currentId/.Statuses", "$currentId/.statuses", "$currentId/Statuses", "$currentId/statuses")
                for (probeId in hiddenProbes) {
                    queryDocIdChildren(context, treeUri, probeId, projection, result, seenPaths, queriedDocIds)
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
                                if (lowerName != "cache" && lowerName != "thumbnails" && lowerName != ".trash" && lowerName != "databases") {
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
    }

    private fun queryDocIdChildren(
        context: Context,
        treeUri: Uri,
        targetDocId: String,
        projection: Array<String>,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>,
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
        } catch (ignored: Exception) {
        } finally {
            try { cursor?.close() } catch (ignored: Exception) {}
        }
    }

    private fun probeDocumentFileHierarchies(
        treeUri: Uri,
        rootDoc: DocumentFile,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>
    ) {
        val queue = ArrayDeque<DocumentFile>()
        queue.add(rootDoc)
        var count = 0

        while (queue.isNotEmpty() && count < 200) {
            val current = queue.removeFirst()
            count++

            // Check if current is already .Statuses or Statuses
            val curName = current.name ?: ""
            if (curName.equals(".Statuses", ignoreCase = true) || curName.equals("Statuses", ignoreCase = true)) {
                collectFilesFromDocFolder(current, result, seenPaths)
                continue
            }

            // Proactively probe .Statuses, .statuses, Statuses inside this folder
            val statusesChild = current.findFile(".Statuses") ?: current.findFile(".statuses") ?: current.findFile("Statuses")
            if (statusesChild != null && statusesChild.isDirectory) {
                collectFilesFromDocFolder(statusesChild, result, seenPaths)
            }

            // Step-by-step well-known path checks
            if (curName.equals("media", ignoreCase = true)) {
                // If this is Android/media: check com.whatsapp -> WhatsApp -> Media -> .Statuses
                val comWhatsapp = current.findFile("com.whatsapp")
                val waStatuses = comWhatsapp?.findFile("WhatsApp")?.findFile("Media")?.let {
                    it.findFile(".Statuses") ?: it.findFile(".statuses") ?: it.findFile("Statuses")
                }
                if (waStatuses != null) {
                    collectFilesFromDocFolder(waStatuses, result, seenPaths)
                }

                // Check com.whatsapp.w4b -> WhatsApp Business -> Media -> .Statuses
                val comW4b = current.findFile("com.whatsapp.w4b")
                val w4bStatuses = comW4b?.findFile("WhatsApp Business")?.findFile("Media")?.let {
                    it.findFile(".Statuses") ?: it.findFile(".statuses") ?: it.findFile("Statuses")
                }
                if (w4bStatuses != null) {
                    collectFilesFromDocFolder(w4bStatuses, result, seenPaths)
                }
            } else if (curName.equals("com.whatsapp", ignoreCase = true)) {
                val waStatuses = current.findFile("WhatsApp")?.findFile("Media")?.let {
                    it.findFile(".Statuses") ?: it.findFile(".statuses") ?: it.findFile("Statuses")
                }
                if (waStatuses != null) {
                    collectFilesFromDocFolder(waStatuses, result, seenPaths)
                }
            } else if (curName.equals("WhatsApp", ignoreCase = true)) {
                val waStatuses = current.findFile("Media")?.let {
                    it.findFile(".Statuses") ?: it.findFile(".statuses") ?: it.findFile("Statuses")
                }
                if (waStatuses != null) {
                    collectFilesFromDocFolder(waStatuses, result, seenPaths)
                }
            }

            // List regular files and subfolders
            try {
                val children = current.listFiles()
                for (child in children) {
                    if (child.isDirectory) {
                        val childName = child.name?.lowercase() ?: ""
                        if (childName != "cache" && childName != "thumbnails" && childName != ".trash" && childName != "databases") {
                            queue.add(child)
                        }
                    } else if (child.isFile && child.length() > 0) {
                        addDocFileStatusItem(child, result, seenPaths)
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun collectFilesFromDocFolder(
        folder: DocumentFile,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>
    ) {
        try {
            val files = folder.listFiles()
            for (file in files) {
                if (file.isFile && file.length() > 0) {
                    addDocFileStatusItem(file, result, seenPaths)
                }
            }
        } catch (ignored: Exception) {}
    }

    private fun addDocFileStatusItem(
        file: DocumentFile,
        result: MutableList<StatusItem>,
        seenPaths: MutableSet<String>
    ) {
        val name = file.name ?: return
        if (name.startsWith(".nomedia") || file.length() <= 0) return
        val ext = name.substringAfterLast('.', "").lowercase()
        val mediaType = when {
            IMAGE_EXTENSIONS.contains(ext) -> MediaType.IMAGE
            VIDEO_EXTENSIONS.contains(ext) -> MediaType.VIDEO
            else -> null
        } ?: return

        val uriStr = file.uri.toString()
        if (seenPaths.contains(uriStr) || seenPaths.contains(name)) return

        val isBusiness = uriStr.contains("w4b", ignoreCase = true) || uriStr.contains("Business", ignoreCase = true)
        val item = StatusItem(
            id = uriStr,
            title = name,
            uriString = uriStr,
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
        seenPaths.add(uriStr)
        seenPaths.add(name)
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
