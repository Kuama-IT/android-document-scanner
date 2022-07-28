package net.kuama.documentscanner.data

import org.opencv.core.Point
import org.opencv.core.Size

// TODO: move from List of point to named Parameters
data class Corners(val points: List<Point>, val size: Size) {
    var topLeft: Point = points[0]
    var topRight: Point = points[1]
    var bottomRight: Point = points[2]
    var bottomLeft: Point = points[3]
}
