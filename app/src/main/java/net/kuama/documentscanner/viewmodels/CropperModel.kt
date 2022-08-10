package net.kuama.documentscanner.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.data.CornersFactory
import net.kuama.documentscanner.support.Failure
import net.kuama.documentscanner.domain.FindPaperSheetContours
import net.kuama.documentscanner.domain.PerspectiveTransform

class CropperModel : ViewModel() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()

    val corners = MutableLiveData<Corners>()
    val originalBitmap = MutableLiveData<Bitmap>()
    val bitmapToCrop = MutableLiveData<Bitmap>()
    val errors = MutableLiveData<Throwable>()

    fun onViewCreated(uri: Uri, screenOrientationDeg: Int) {
        val image = BitmapFactory.decodeStream(uri.toFile().inputStream())
        val exif = ExifInterface(uri.toFile().inputStream())

        val pictureOrientation =
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        val matrix = Matrix()

        when (pictureOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        }

        when (screenOrientationDeg) {
            90 -> matrix.postRotate(90F)
            180 -> matrix.postRotate(180F)
            270 -> matrix.postRotate(270F)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            image, 0, 0, image.width, image.height, matrix, true
        )
        originalBitmap.value = rotatedBitmap
        analyze(rotatedBitmap)
    }

    fun onCornersAccepted(bitmap: Bitmap) {
        corners.value?.let { acceptedCorners ->
            val acceptedAndOrderedCorners = listOf(
                acceptedCorners.topLeft,
                acceptedCorners.topRight,
                acceptedCorners.bottomRight,
                acceptedCorners.bottomLeft
            )

            viewModelScope.launch {
                perspectiveTransform(
                    PerspectiveTransform.Params(
                        bitmap = bitmap,
                        corners = CornersFactory.create(acceptedAndOrderedCorners, acceptedCorners.size)
                    )
                ) { it ->
                    it.fold(::handleFailure) { bitmap ->
                        bitmapToCrop.value = bitmap
                    }
                }
            }
        }
    }

    private fun analyze(bitmap: Bitmap) {
        viewModelScope.launch {
            findPaperSheetUseCase(
                FindPaperSheetContours.Params(bitmap)
            ) {
                it.fold(::handleFailure) { foundCorners: Corners? ->
                    corners.value = foundCorners
                }
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        errors.value = failure.origin
    }
}
