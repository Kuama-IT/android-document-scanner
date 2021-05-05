package net.kuama.documentscanner.presentation

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.domain.Failure
import net.kuama.documentscanner.domain.FindPaperSheetContours
import net.kuama.documentscanner.domain.PerspectiveTransform
import net.kuama.documentscanner.domain.UriToBitmap

class CropperModel : ViewModel() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()
    private val uriToBitmap: UriToBitmap = UriToBitmap()

    val corners = MutableLiveData<Corners?>()
    val original = MutableLiveData<Bitmap>()
    val bitmapToCrop = MutableLiveData<Bitmap>()

    fun onViewCreated(uri: Uri, contentResolver: ContentResolver) {
        uriToBitmap(
            UriToBitmap.Params(
                uri = uri,
                contentResolver = contentResolver
            )
        ) {
            it.fold(::handleFailure) { preview ->
                analyze(preview, returnOriginalMat = true) { pair ->
                    pair.second?.let {
                        original.value = pair.first
                        corners.value = pair.second
                    }
                }
            }
        }
    }

    fun onCornersAccepted(bitmap: Bitmap) {
        perspectiveTransform(
                PerspectiveTransform.Params(
                    bitmap = bitmap,
                    corners = corners.value!!
                )
            ) { result ->
                result.fold(::handleFailure) { bitmap ->
                    bitmapToCrop.value = bitmap
                }
            }
    }

    private fun analyze(
        bitmap: Bitmap,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((Pair<Bitmap, Corners?>) -> Unit)? = null
    ) {
        findPaperSheetUseCase(
            FindPaperSheetContours.Params(
                bitmap,
                returnOriginalMat
            )
        ) {
            it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                callback?.invoke(pair) ?: run { }
                onSuccess?.invoke()
            }
        }
    }

    private fun handleFailure(failure: Failure) { }
}
