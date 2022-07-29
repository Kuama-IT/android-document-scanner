package net.kuama.documentscanner.extensions

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

internal fun Bitmap.toByteArray(outputStream: ByteArrayOutputStream): ByteArray {
    compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    return outputStream.toByteArray()
}
