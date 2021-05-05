package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.Comparator
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Given a set of corners, and a source image,
 * crops the corners from the image and transforms the shape represented by the corners
 * into a rectangle
 */
class PerspectiveTransform : UseCase<Bitmap, PerspectiveTransform.Params>() {

    class Params(val bitmap: Bitmap, val corners: Corners)

    override suspend fun run(params: Params): Either<Failure, Bitmap> = try {
        val src = Mat()

        Utils.bitmapToMat(
            params.bitmap, src
        )

        val orderedCorners = sortPoints(
            arrayOf(
                params.corners.tl,
                params.corners.tr,
                params.corners.br,
                params.corners.bl
            )
        )

        val tl = orderedCorners[0]
        val tr = orderedCorners[1]
        val br = orderedCorners[2]
        val bl = orderedCorners[3]

        val widthA = sqrt(
            (br.x - bl.x).pow(2.0) + (br.y - bl.y).pow(2.0)
        )
        val widthB = sqrt(
            (tr.x - tl.x).pow(2.0) + (tr.y - tl.y).pow(2.0)
        )
        val dw = max(widthA, widthB)
        val maxWidth = dw.toInt()
        val heightA = sqrt(
            (tr.x - br.x).pow(2.0) + (tr.y - br.y).pow(2.0)
        )
        val heightB = sqrt(
            (tl.x - bl.x).pow(2.0) + (tl.y - bl.y).pow(2.0)
        )
        val dh = max(heightA, heightB)
        val maxHeight = java.lang.Double.valueOf(dh).toInt()
        val doc = Mat(maxHeight, maxWidth, CvType.CV_8UC4)
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        val dstMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
        dstMat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh)
        val m = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        Imgproc.warpPerspective(src, doc, m, doc.size())
        val bitmap = Bitmap.createBitmap(doc.cols(), doc.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(doc, bitmap)
        srcMat.release()
        dstMat.release()
        m.release()
        doc.release()
        Right(bitmap)
    } catch (throwable: Throwable) {
        Left(Failure(throwable))
    }
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
