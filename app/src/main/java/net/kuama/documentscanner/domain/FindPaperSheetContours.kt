package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.data.CornersFactory
import net.kuama.documentscanner.extensions.shape
import net.kuama.documentscanner.support.InfallibleUseCase
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class FindPaperSheetContours : InfallibleUseCase<Corners?, FindPaperSheetContours.Params>() {

    class Params(val bitmap: Bitmap)

    override suspend fun run(params: Params): Corners? {
        val original = Mat()
        val modified = Mat()

        Utils.bitmapToMat(params.bitmap, original)

        // Convert image from RGBA to GrayScale
        Imgproc.cvtColor(original, modified, Imgproc.COLOR_RGBA2GRAY)

        // Strong Gaussian Filter
        Imgproc.GaussianBlur(modified, modified, Size(25.0, 25.0), 5.0)

        // Canny Edge Detection
        Imgproc.Canny(modified, modified, 50.0, 200.0, 5, false)

        // Closing: Dilation followed by Erosion
        Imgproc.dilate(
            modified, modified, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(8.0, 8.0))
        )

        var contours: MutableList<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            modified, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE
        )

        hierarchy.release()
        contours = contours
            .filter { point -> point.shape.size == 4 }
            .toTypedArray()
            .toMutableList()

        contours.sortWith { lhs, rhs ->
            Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
        }

        var result: Corners? = null

        contours.firstOrNull()?.let { contour ->

            CornersFactory.create(contour.shape, original.size()).run {
                with(this) {

                    val topLine = topRight.x - topLeft.x
                    val bottomLine = bottomRight.x - bottomLeft.x
                    val leftLine = topLeft.y - bottomLeft.y
                    val rightLine = topRight.y - bottomRight.y

                    val isApproximateQuadrilateral =
                        topLine > MIN_VALUE_HORIZONTAL_LINES && bottomLine > MIN_VALUE_HORIZONTAL_LINES
                                && leftLine > MIN_VALUE_VERTICAL_LINES && rightLine > MIN_VALUE_VERTICAL_LINES

                    result = if (isApproximateQuadrilateral) this else null

                }
            }
        }

        return result
    }

    private companion object {
        private const val MIN_VALUE_VERTICAL_LINES = 130.0
        private const val MIN_VALUE_HORIZONTAL_LINES = 320.0
    }
}
