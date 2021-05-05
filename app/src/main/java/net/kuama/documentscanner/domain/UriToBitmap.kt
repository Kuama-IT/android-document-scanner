package net.kuama.documentscanner.domain

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import java.io.FileDescriptor

/**
 * Given a image URI, tries to load the image into a bitmap, checking also the image rotation
 */
class UriToBitmap : UseCase<Bitmap, UriToBitmap.Params>() {

    class Params(val uri: Uri, val contentResolver: ContentResolver)

    override suspend fun run(params: Params): Either<Failure, Bitmap> = try {
        val parcelFileDescriptor: ParcelFileDescriptor =
            params.contentResolver.openFileDescriptor(params.uri, "r")!!
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        val exif = ExifInterface(params.uri.path.toString())
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        }

        val rotatedBitmap =
            Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)

        Right(rotatedBitmap)
    } catch (throwable: Throwable) {
        Left(Failure(throwable))
    }
}
