package net.kuama.documentscanner.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.ByteArrayOutputStream

internal fun Bitmap.toByteArray(outputStream: ByteArrayOutputStream): ByteArray {
    compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    return outputStream.toByteArray()
}

fun loadBitmapFromView(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(
        view.measuredWidth,
        view.measuredHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    view.layout(view.left, view.top, view.right, view.bottom)
    view.draw(canvas)
    return bitmap
}
