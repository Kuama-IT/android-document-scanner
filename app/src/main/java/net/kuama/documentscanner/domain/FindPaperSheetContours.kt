package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import net.kuama.documentscanner.extensions.shape
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class FindPaperSheetContours : UseCase<Pair<Bitmap, Corners?>, FindPaperSheetContours.Params>() {
    class Params(
        val bitmap: Bitmap,
        val returnOriginalMat: Boolean = false
    )

    override suspend fun run(params: Params): Either<Failure, Pair<Bitmap, Corners?>> =
        try {
            val original = Mat()
            val modified = Mat()

            Utils.bitmapToMat(params.bitmap, original)

            // Convert image from RGBA to GrayScale
            Imgproc.cvtColor(original, modified, Imgproc.COLOR_RGBA2GRAY)

            // Strong Gaussian Filter
            Imgproc.GaussianBlur(modified, modified, Size(51.0, 51.0), 0.0)

            // Canny Edge Detection
            Imgproc.Canny(modified, modified, 100.0, 200.0, 5, false)

            // Closing: Dilation followed by Erosion
            Imgproc.dilate(
                modified, modified, Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT, Size(
                        8.0,
                        8.0
                    )
                )
            )
            Imgproc.erode(
                modified, modified, Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    Size(3.0, 3.0)
                )
            )

            var contours: MutableList<MatOfPoint> = ArrayList()
            val hierarchy = Mat()
            Imgproc.findContours(
                modified,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            hierarchy.release()
            contours = contours
                .filter { it.shape.size == 4 }
                .toTypedArray()
                .toMutableList()

            contours.sortWith { lhs, rhs ->
                Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
            }

            if (params.returnOriginalMat) {
                Utils.matToBitmap(original, params.bitmap)
            } else {
                params.bitmap.recycle()
            }

            Right(contours.firstOrNull()?.let {
                Pair(
                    params.bitmap,
                    Corners(
                        it.shape.toList(),
                        original.size()
                    )
                )
            } ?: Pair(params.bitmap, null))
        } catch (throwable: Throwable) {
            Left(Failure(throwable))
        }
}
