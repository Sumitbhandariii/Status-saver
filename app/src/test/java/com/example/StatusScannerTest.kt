package com.example

import com.example.util.StatusScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class StatusScannerTest {
    @Test
    fun `detectMediaType recognizes image and video files`() {
        assertEquals(com.example.data.model.MediaType.IMAGE, StatusScanner.detectMediaType("IMG-123.jpg"))
        assertEquals(com.example.data.model.MediaType.VIDEO, StatusScanner.detectMediaType("VID-456.mp4"))
        assertEquals(null, StatusScanner.detectMediaType("notes.txt"))
    }

    @Test
    fun `buildWhatsAppStatusDirectories finds Android media WhatsApp folders`() {
        val root = File("/tmp/statusscanner_test")
        val whatsappDir = File(root, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
        whatsappDir.mkdirs()

        val dirs = StatusScanner.buildWhatsAppStatusDirectories(root)
        assertNotNull(dirs)
        assert(dirs.any { it.absolutePath == whatsappDir.absolutePath })
    }
}
