package net.kuama.documentscanner.extensions

import android.graphics.Bitmap
import org.junit.Test
import org.mockito.kotlin.*
import java.io.ByteArrayOutputStream

class BitmapExtensionTest {

    @Test
    fun it_returns_original_bitmap_byte_array() {
        val outputStream = mock<ByteArrayOutputStream>()
        val bitmapMock = mock<Bitmap>()

        val byteArr = "test-byte-array".toByteArray()
        whenever(outputStream.toByteArray()).thenReturn(byteArr)
        whenever(bitmapMock.compress(any(), any(), any())).thenReturn(true)

        bitmapMock.toByteArray(outputStream)

        verify(bitmapMock, times(1)).compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        verify(outputStream, times(1)).toByteArray()
    }
}
