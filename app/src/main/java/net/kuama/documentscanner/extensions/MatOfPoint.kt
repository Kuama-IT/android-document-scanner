package net.kuama.documentscanner.extensions

import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

/**
 * A list of [MatOfPoint] representing an approximated contour
 */
val MatOfPoint.shape: List<Point>
    get() {
        val inputMatrixFloat = MatOfPoint2f(*this.toArray())
        val resultMatrixFloat = MatOfPoint2f()

        val contourPerimeter = Imgproc.arcLength(inputMatrixFloat, true)

        Imgproc.approxPolyDP(inputMatrixFloat, resultMatrixFloat, 0.02 * contourPerimeter, true)

        return resultMatrixFloat.toList()
    }
