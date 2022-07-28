package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.support.*
import net.kuama.documentscanner.utils.PerspectiveTransformUtils
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
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

        Utils.bitmapToMat(params.bitmap, src)

        val orderedCorners = PerspectiveTransformUtils.sortPoints(params.corners)

        val widthA = sqrt(
            (orderedCorners.bottomRight.x - orderedCorners.bottomLeft.x).pow(2.0) + (orderedCorners.bottomRight.y - orderedCorners.bottomLeft.y).pow(2.0)
        )
        val widthB = sqrt(
            (orderedCorners.topRight.x - orderedCorners.topLeft.x).pow(2.0) + (orderedCorners.topRight.y - orderedCorners.topLeft.y).pow(2.0)
        )
        val dw = max(widthA, widthB)
        val maxWidth = dw.toInt()
        val heightA = sqrt(
            (orderedCorners.topRight.x - orderedCorners.bottomRight.x).pow(2.0) + (orderedCorners.topRight.y - orderedCorners.bottomRight.y).pow(2.0)
        )
        val heightB = sqrt(
            (orderedCorners.topLeft.x - orderedCorners.bottomLeft.x).pow(2.0) + (orderedCorners.topLeft.y - orderedCorners.bottomLeft.y).pow(2.0)
        )

        val dh = max(heightA, heightB)
        val maxHeight = java.lang.Double.valueOf(dh).toInt()
        val doc = Mat(maxHeight, maxWidth, CvType.CV_8UC4)
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        val dstMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(0, 0, orderedCorners.topLeft.x, orderedCorners.topLeft.y, orderedCorners.topRight.x, orderedCorners.topRight.y, orderedCorners.bottomRight.x, orderedCorners.bottomRight.y, orderedCorners.bottomLeft.x, orderedCorners.bottomLeft.y)
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
