package net.kuama.documentscanner.extensions

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

internal fun Bitmap.toByteArray(): ByteArray {
    ByteArrayOutputStream().apply {
        compress(Bitmap.CompressFormat.JPEG, 100, this)
        return toByteArray()
    }
}
