package net.kuama.documentscanner.extensions

import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

/**
 * A list of [MatOfPoint] representing an approximated contour
 */
val MatOfPoint.shape: Array<Point>
    get() {
        val c2f = MatOfPoint2f(*toArray())
        val peri = Imgproc.arcLength(c2f, true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
        return approx.toArray()
    }
