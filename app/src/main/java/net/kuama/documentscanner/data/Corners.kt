package net.kuama.documentscanner.data

import android.util.Log
import org.opencv.core.Point
import org.opencv.core.Size

data class Corners(val points: List<Point>, val size: Size) {
    fun log() {

        Log.d(
            javaClass.simpleName,
            "size: ${size.width}x${size.height} - tl: ${tl.x}, ${tl.y} - tr: ${tr.x}, ${tr.y} - br: ${br.x}, ${br.y} - bl: ${bl.x}, ${bl.y}"
        )
    }

    val tl: Point = points[0]
    val tr: Point = points[1]
    val br: Point = points[2]
    val bl: Point = points[3]
}
