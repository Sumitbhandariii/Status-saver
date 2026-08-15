package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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

        val samples = listOf(
            SampleDef(
                title = "status_sunset_vibes.jpg",
                mediaType = MediaType.IMAGE,
                caption = "Golden Hour Magic ✨",
                subtext = "Enjoy every moment 🌅",
                startColor = Color.parseColor("#FF512F"),
                endColor = Color.parseColor("#DD2476"),
                iconSymbol = "☀️"
            ),
            SampleDef(
                title = "status_morning_coffee.jpg",
                mediaType = MediaType.IMAGE,
                caption = "Good Morning ☕",
                subtext = "Start your day with a positive mindset",
                startColor = Color.parseColor("#4A00E0"),
                endColor = Color.parseColor("#8E2DE2"),
                iconSymbol = "☕"
            ),
            SampleDef(
                title = "status_mountain_travel.jpg",
                mediaType = MediaType.IMAGE,
                caption = "Wanderlust Adventure 🏔️",
                subtext = "The mountains are calling",
                startColor = Color.parseColor("#00C9FF"),
                endColor = Color.parseColor("#92FE9D"),
                iconSymbol = "⛰️"
            ),
            SampleDef(
                title = "status_motivational_quote.jpg",
                mediaType = MediaType.IMAGE,
                caption = "Never Stop Dreaming 🚀",
                subtext = "Success is not final, failure is not fatal",
                startColor = Color.parseColor("#11998E"),
                endColor = Color.parseColor("#38EF7D"),
                iconSymbol = "🌟"
            ),
            SampleDef(
                title = "status_city_lights.jpg",
                mediaType = MediaType.IMAGE,
                caption = "Night City Glow 🌃",
                subtext = "Lost in the rhythm of the city lights",
                startColor = Color.parseColor("#0F2027"),
                endColor = Color.parseColor("#2C5364"),
                iconSymbol = "✨"
            ),
            SampleDef(
                title = "status_beach_workout.mp4",
                mediaType = MediaType.VIDEO,
                caption = "Morning Cardio & Flow 🏃‍♂️",
                subtext = "15-minute quick full body burn",
                startColor = Color.parseColor("#FC466B"),
                endColor = Color.parseColor("#3F5EFB"),
                iconSymbol = "🔥",
                durationMs = 28000L
            ),
            SampleDef(
                title = "status_celebration_vibes.mp4",
                mediaType = MediaType.VIDEO,
                caption = "Weekend Celebration 🎉",
                subtext = "Happy times with best friends",
                startColor = Color.parseColor("#654ea3"),
                endColor = Color.parseColor("#eaafc8"),
                iconSymbol = "🎊",
                durationMs = 15000L
            ),
            SampleDef(
                title = "status_peaceful_rain.mp4",
                mediaType = MediaType.VIDEO,
                caption = "Rainy Day Chill 🌧️",
                subtext = "Lo-fi beats & cozy vibes",
                startColor = Color.parseColor("#3a7bd5"),
                endColor = Color.parseColor("#3a6073"),
                iconSymbol = "💧",
                durationMs = 45000L
            )
        )

        val result = mutableListOf<StatusItem>()
        val baseTime = System.currentTimeMillis()

        samples.forEachIndexed { index, def ->
            val file = File(sampleDir, def.title)
            if (!file.exists() || file.length() == 0L) {
                createSampleMediaFile(file, def)
            }

            val statusItem = StatusItem(
                id = "sample_${def.title}",
                title = def.title,
                uriString = Uri.fromFile(file).toString(),
                filePath = file.absolutePath,
                mediaType = def.mediaType,
                fileSize = if (file.exists()) file.length() else 1024L * (150 + index * 40),
                durationMs = def.durationMs,
                dateModified = baseTime - (index * 3600000L),
                isSaved = false,
                isFavorite = (index == 0),
                isNew = (index < 3),
                source = "SAMPLE"
            )
            result.add(statusItem)
        }

        return result
    }

    private fun createSampleMediaFile(file: File, def: SampleDef) {
        try {
            val width = 480
            val height = 854
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)

            // Background Gradient
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                def.startColor, def.endColor, Shader.TileMode.CLAMP
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = gradient
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Decorative Geometric Shapes
            val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(40, 255, 255, 255)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(width * 0.85f, height * 0.15f, 180f, shapePaint)
            canvas.drawCircle(width * 0.15f, height * 0.85f, 200f, shapePaint)

            // Card Container
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(70, 0, 0, 0)
                style = Paint.Style.FILL
            }
            val cardRect = RectF(60f, height * 0.35f, width - 60f, height * 0.65f)
            canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

            // Center Symbol / Emoji
            val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 90f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(def.iconSymbol, width / 2f, height * 0.45f, symbolPaint)

            // Title / Caption
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 34f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(def.caption, width / 2f, height * 0.53f, textPaint)

            // Subtitle
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(def.subtext, width / 2f, height * 0.59f, subPaint)

            // Watermark / Brand
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(160, 255, 255, 255)
                textSize = 20f
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.2f
            }
            val typeText = if (def.mediaType == MediaType.VIDEO) "▶ WHATSAPP VIDEO STATUS" else "📷 WHATSAPP STATUS PHOTO"
            canvas.drawText("STATUSVAULT • $typeText", width / 2f, height * 0.92f, brandPaint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class SampleDef(
        val title: String,
        val mediaType: MediaType,
        val caption: String,
        val subtext: String,
        val startColor: Int,
        val endColor: Int,
        val iconSymbol: String,
        val durationMs: Long = 0L
    )
}
