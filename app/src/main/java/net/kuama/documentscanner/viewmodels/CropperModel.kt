package net.kuama.documentscanner.viewmodels

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.support.Failure
import net.kuama.documentscanner.domain.FindPaperSheetContours
import net.kuama.documentscanner.domain.PerspectiveTransform
import net.kuama.documentscanner.domain.UriToBitmap

class CropperModel : ViewModel() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()
    private val uriToBitmap: UriToBitmap = UriToBitmap()

    val corners = MutableLiveData<Corners>()
    val originalBitmap = MutableLiveData<Bitmap>()
    val bitmapToCrop = MutableLiveData<Bitmap>()
    val errors = MutableLiveData<Throwable>()

    fun onViewCreated(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            uriToBitmap(
                UriToBitmap.Params(
                    uri = uri,
                    contentResolver = contentResolver
                )
            ) { either ->
                either.fold(::handleFailure) { preview ->
                    originalBitmap.value = preview
                    val resultingCorners = analyze(preview)
                    resultingCorners?.let {
                        corners.value = it
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
        bitmap: Bitmap
    ): Corners? {
        var result: Corners? = null
        viewModelScope.launch {
            findPaperSheetUseCase(
                FindPaperSheetContours.Params(bitmap)
            ) {
                it.fold(::handleFailure) { paperSheetContoursResult: Corners? ->
                    result = paperSheetContoursResult
                }
            }
        }
        return result
    }

    private fun handleFailure(failure: Failure) {
        errors.value = failure.origin
    }
}
