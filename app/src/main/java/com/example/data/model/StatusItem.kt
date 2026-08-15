package com.example.data.model

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO
}

data class StatusItem(
    val id: String,
    val title: String,
    val uriString: String,
    val filePath: String? = null,
    val mediaType: MediaType,
    val fileSize: Long = 0L,
    val durationMs: Long = 0L,
    val dateModified: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val isFavorite: Boolean = false,
    val isNew: Boolean = true,
    val savedFilePath: String? = null,
    val savedDate: Long? = null,
    val source: String = "WHATSAPP" // "WHATSAPP", "WHATSAPP_BUSINESS", "SAMPLE"
) {
    val uri: Uri
        get() = Uri.parse(uriString)

    val formattedSize: String
        get() {
            if (fileSize <= 0) return "0 KB"
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "00:00"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
