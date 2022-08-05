package net.kuama.documentscanner.domain

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import net.kuama.documentscanner.support.*
import java.io.FileDescriptor

/**
 * Given a image URI, tries to load the image into a bitmap, checking also the image rotation
 *
 * NB: Inappropriate blocking method call cannot be suppressed since it will still block the thread
 * on which is running. The only way to bypass the problem is to use the IO thread to offload the
 * main thread. All Kotlin I/O class are blocking (since they are inherited from Java)
 */
class UriToBitmap : UseCase<Bitmap, UriToBitmap.Params>() {

    class Params(val uri: Uri, val screenOrientationDeg: Int? = null, val contentResolver: ContentResolver)

    override suspend fun run(params: Params): Either<Failure, Bitmap> = withContext(Dispatchers.IO) {
        try {
            // Thread Blocking call
            val parcelFileDescriptor: ParcelFileDescriptor =
                params.contentResolver.openFileDescriptor(params.uri, "r") ?: throw NullPointerException("Null Content Resolver")

            val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)

            // Thread Blocking call
            parcelFileDescriptor.close()

            // Thread Blocking call
            val exif = ExifInterface(params.uri.path.toString())

            val pictureOrientation =
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()

            when (pictureOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
            }

            when (params.screenOrientationDeg) {
                90 -> matrix.postRotate(90F)
                180 -> matrix.postRotate(180F)
                270 -> matrix.postRotate(270F)
            }

            Right(
                Bitmap.createBitmap(
                    image, 0, 0, image.width, image.height, matrix, true)
            )
        } catch (throwable: Throwable) {
            Left(Failure(throwable))
        }
    }
}
