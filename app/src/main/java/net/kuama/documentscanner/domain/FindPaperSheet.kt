package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.scanner.data.Corners
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import net.kuama.documentscanner.support.shape
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * A good initial number to use during the white-color filter operation
 * see [FindPaperSheet.run]
 */
internal const val THRESHOLD_BASE = 48.0

/**
 * Tries to find an A4 document inside the provided image.
 * Returns the corners of the A4 document, if found
 */
class FindPaperSheet : UseCase<Pair<Bitmap, Corners?>, FindPaperSheet.Params>() {

    class Params(
        val bitmap: Bitmap,
        val sensitivity: Double = THRESHOLD_BASE,
        val returnOriginalMat: Boolean = false
    )

    override suspend fun run(params: Params): Either<Failure, Pair<Bitmap, Corners?>> =
        try {
            val mat = Mat()
            // move the bitmap to a mat
            Utils.bitmapToMat(params.bitmap, mat)

            val hsv = Mat()
            Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

            // keep only white-ish colors
            val mask = Mat()

            Core.inRange(
                hsv,
                Scalar(0.0, 0.0, 255 - params.sensitivity),
                Scalar(255.0, params.sensitivity, 255.0),
                mask
            )

            var contours: MutableList<MatOfPoint> = ArrayList()
            val hierarchy = Mat()
            Imgproc.findContours(
                mask,
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

            contours.sortWith(Comparator { lhs, rhs ->
                Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
            })

            if (params.returnOriginalMat) {
                Utils.matToBitmap(mat, params.bitmap)
            } else {
                params.bitmap.recycle()
            }

            Right(contours.firstOrNull()?.let {
                val foundPoints: Array<Point> = sortPoints(it.shape)
                Pair(
                    params.bitmap,
                    Corners(
                        foundPoints.toList(),
                        mat.size()
                    )
                )
            } ?: Pair(params.bitmap, null))
        } catch (throwable: Throwable) {
            Left(Failure(throwable))
        }

    private fun sortPoints(src: Array<Point>): Array<Point> {
        val srcPoints = src.toList()
        val result = arrayOf<Point?>(null, null, null, null)
        val sumComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x)
            }
        val diffComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
            }

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator)

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator)

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator)

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator)
        return result.filterNotNull().toTypedArray()
    }
}
