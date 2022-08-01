package net.kuama.documentscanner.data

import net.kuama.documentscanner.utils.PointUtils
import org.opencv.core.Point
import org.opencv.core.Size

data class Corners(
    val topLeft: Point,
    val topRight: Point,
    val bottomRight: Point,
    val bottomLeft: Point,
    val size: Size
)

object CornersFactory {
    fun create(points: List<Point>, size: Size): Corners {
        return PointUtils.getSortedCorners(points, size)
    }
}
