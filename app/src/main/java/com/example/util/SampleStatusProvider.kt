package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import java.io.File
import java.io.FileOutputStream

object SampleStatusProvider {
    fun generateSampleStatuses(context: Context): List<StatusItem> {
        val sampleDir = File(context.filesDir, "sample_statuses")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }

        val items = mutableListOf<StatusItem>()

        val samples = listOf(
            Triple("Sample_Sunset_View.jpg", "Golden Hour Sunset #Nature", 0xFFE65100.toInt()),
            Triple("Sample_Good_Morning.jpg", "Good Morning ☀️ Have a Great Day!", 0xFF1976D2.toInt()),
            Triple("Sample_Motivation_Quote.jpg", "Believe in Yourself & Never Give Up 🚀", 0xFF388E3C.toInt()),
            Triple("Sample_Festival_Vibes.jpg", "Happy Celebrations with Friends 🎉", 0xFF7B1FA2.toInt())
        )

        for ((index, sample) in samples.withIndex()) {
            val (fileName, text, bgColor) = sample
            val file = File(sampleDir, fileName)
            if (!file.exists() || file.length() == 0L) {
                try {
                    val width = 720
                    val height = 1280
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(bgColor)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 40f
                        textAlign = Paint.Align.CENTER
                    }

                    canvas.drawText("WhatsApp Status Demo", width / 2f, height / 3f, paint)
                    paint.textSize = 30f
                    canvas.drawText(text, width / 2f, height / 2f, paint)

                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    bitmap.recycle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (file.exists() && file.length() > 0L) {
                items.add(
                    StatusItem(
                        id = "sample_${file.name}",
                        title = file.name,
                        uriString = Uri.fromFile(file).toString(),
                        filePath = file.absolutePath,
                        mediaType = MediaType.IMAGE,
                        fileSize = file.length(),
                        durationMs = 0L,
                        dateModified = System.currentTimeMillis() - (index * 3600000L),
                        isSaved = false,
                        isFavorite = false,
                        isNew = true,
                        source = "SAMPLE_DEMO"
                    )
                )
            }
        }

        return items
    }
}


