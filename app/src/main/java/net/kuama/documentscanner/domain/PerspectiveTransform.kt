package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.scanner.data.Corners
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Given a set of corners (see [FindPaperSheet]), and a source image,
 * crops the corners from the image and transform the shape represented by the corners
 * into a rectangle
 */
class PerspectiveTransform : UseCase<Bitmap, PerspectiveTransform.Params>() {
    class Params(val bitmap: Bitmap, val corners: Corners)

    override suspend fun run(params: Params): Either<Failure, Bitmap> = try {
        val src = Mat()
        Utils.bitmapToMat(params.bitmap, src)

        val tl = params.corners.corners[0] ?: error("Invalid corners")
        val tr = params.corners.corners[1] ?: error("Invalid corners")
        val br = params.corners.corners[2] ?: error("Invalid corners")
        val bl = params.corners.corners[3] ?: error("Invalid corners")
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
