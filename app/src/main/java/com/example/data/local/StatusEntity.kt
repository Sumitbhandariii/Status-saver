package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaType
import com.example.data.model.StatusItem

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val filePath: String?,
    val mediaType: String, // "IMAGE" or "VIDEO"
    val fileSize: Long,
    val durationMs: Long,
    val dateModified: Long,
    val isSaved: Boolean,
    val isFavorite: Boolean,
    val isNew: Boolean,
    val savedFilePath: String?,
    val savedDate: Long?,
    val source: String
) {
    fun toModel(): StatusItem {
        return StatusItem(
            id = id,
            title = title,
            uriString = uriString,
            filePath = filePath,
            mediaType = if (mediaType == "VIDEO") MediaType.VIDEO else MediaType.IMAGE,
            fileSize = fileSize,
            durationMs = durationMs,
            dateModified = dateModified,
            isSaved = isSaved,
            isFavorite = isFavorite,
            isNew = isNew,
            savedFilePath = savedFilePath,
            savedDate = savedDate,
            source = source
        )
    }

    companion object {
        fun fromModel(model: StatusItem): StatusEntity {
            return StatusEntity(
                id = model.id,
                title = model.title,
                uriString = model.uriString,
                filePath = model.filePath,
                mediaType = model.mediaType.name,
                fileSize = model.fileSize,
                durationMs = model.durationMs,
                dateModified = model.dateModified,
                isSaved = model.isSaved,
                isFavorite = model.isFavorite,
                isNew = model.isNew,
                savedFilePath = model.savedFilePath,
                savedDate = model.savedDate,
                source = model.source
            )
        }
    }
}
