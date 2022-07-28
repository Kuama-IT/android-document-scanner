package net.kuama.documentscanner.viewmodels

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.data.PaperSheetContoursResult
import net.kuama.documentscanner.support.Failure
import net.kuama.documentscanner.domain.FindPaperSheetContours
import net.kuama.documentscanner.domain.PerspectiveTransform
import net.kuama.documentscanner.domain.UriToBitmap

class CropperModel : ViewModel() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()
    private val uriToBitmap: UriToBitmap = UriToBitmap()

    val corners = MutableLiveData<Corners?>()
    val originalBitmap = MutableLiveData<Bitmap>()
    val bitmapToCrop = MutableLiveData<Bitmap>()

    fun onViewCreated(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            uriToBitmap(
                UriToBitmap.Params(
                    uri = uri,
                    contentResolver = contentResolver
                )
            ) { either ->
                either.fold(::handleFailure) { preview ->
                    analyze(preview, returnOriginalMat = true) { pair ->
                        pair.corners?.let {
                            originalBitmap.value = pair.bitmap
                            corners.value = it
                        }
                    }
                }
            }
        }
    }

    fun onCornersAccepted(bitmap: Bitmap) {
        viewModelScope.launch {
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
    }

    private fun analyze(
        bitmap: Bitmap,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((PaperSheetContoursResult) -> Unit)? = null
    ) {

        viewModelScope.launch {
            findPaperSheetUseCase(
                FindPaperSheetContours.Params(
                    bitmap,
                    returnOriginalMat
                )
            ) {
                it.fold(::handleFailure) { paperSheetContoursResult: PaperSheetContoursResult ->
                    callback?.invoke(paperSheetContoursResult) ?: run { }
                    onSuccess?.invoke()
                }
            }
        }

    }

    // TODO: Handle Failure
    private fun handleFailure(failure: Failure) {
    }
}
