package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.extensions.distanceTo
import net.kuama.documentscanner.support.*
import net.kuama.documentscanner.utils.PerspectiveTransformUtils
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.max

/**
 * Given a set of corners, and a source image,
 * crops the corners from the image and transforms the shape represented by the corners
 * into a rectangle
 */
class PerspectiveTransform : UseCase<Bitmap, PerspectiveTransform.Params>() {

    class Params(val bitmap: Bitmap, val corners: Corners)

    override suspend fun run(params: Params): Either<Failure, Bitmap> = try {
        val sourceBitmapMatrix = Mat()
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        val dstMat = Mat(4, 1, CvType.CV_32FC2)

        Utils.bitmapToMat(params.bitmap, sourceBitmapMatrix)

        val orderedCorners = PerspectiveTransformUtils.sortPoints(params.corners)

        val bottomWidth = orderedCorners.bottomRight.distanceTo(orderedCorners.bottomLeft)

        val topWidth = orderedCorners.topRight.distanceTo(orderedCorners.topLeft)

        val maxWidth = max(bottomWidth, topWidth)

        val rightHeight = orderedCorners.topRight.distanceTo(orderedCorners.bottomRight)

        val leftHeight = orderedCorners.topLeft.distanceTo(orderedCorners.bottomLeft)

        val maxHeight = max(rightHeight, leftHeight)

        val transformedDocumentMatrix = Mat(maxHeight.toInt(), maxWidth.toInt(), CvType.CV_8UC4)

        srcMat.put(0, 0, orderedCorners.topLeft.x, orderedCorners.topLeft.y,
            orderedCorners.topRight.x, orderedCorners.topRight.y, orderedCorners.bottomRight.x,
            orderedCorners.bottomRight.y, orderedCorners.bottomLeft.x, orderedCorners.bottomLeft.y
        )

        dstMat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth,
            maxHeight, 0.0, maxHeight)

        val perspectiveTransformMatrix = Imgproc.getPerspectiveTransform(srcMat, dstMat)

        Imgproc.warpPerspective(
            sourceBitmapMatrix, transformedDocumentMatrix,
            perspectiveTransformMatrix, transformedDocumentMatrix.size()
        )

        val transformedBitmap = Bitmap
            .createBitmap(
                transformedDocumentMatrix.cols(),
                transformedDocumentMatrix.rows(),
                Bitmap.Config.ARGB_8888
            )

        Utils.matToBitmap(transformedDocumentMatrix, transformedBitmap)

        srcMat.release()
        dstMat.release()
        perspectiveTransformMatrix.release()
        transformedDocumentMatrix.release()

        Right(transformedBitmap)
    } catch (throwable: Throwable) {
        Left(Failure(throwable))
    }
}
