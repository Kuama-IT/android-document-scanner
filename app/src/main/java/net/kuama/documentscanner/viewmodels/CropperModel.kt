package net.kuama.documentscanner.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.data.CornersFactory
import net.kuama.documentscanner.support.Failure
import net.kuama.documentscanner.domain.FindPaperSheetContours
import net.kuama.documentscanner.domain.PerspectiveTransform
import net.kuama.documentscanner.domain.InputStreamToBitmap

class CropperModel : ViewModel() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()
    private val uriToBitmap: InputStreamToBitmap = InputStreamToBitmap()

    val corners = MutableLiveData<Corners>()
    val originalBitmap = MutableLiveData<Bitmap>()
    val bitmapToCrop = MutableLiveData<Bitmap>()
    val errors = MutableLiveData<Throwable>()

    fun onViewCreated(uri: Uri, screenOrientationDeg: Int) {
        viewModelScope.launch {
            uriToBitmap(
                InputStreamToBitmap.Params(
                    inputStream = uri.toFile().inputStream(),
                    screenOrientationDeg = screenOrientationDeg
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
                        corners = CornersFactory.create(
                            acceptedAndOrderedCorners,
                            acceptedCorners.size
                        )
                    )
                ) { bitmap ->
                    bitmapToCrop.value = bitmap
                }
            }
        }
    }

    private fun analyze(bitmap: Bitmap): Corners? {
        var result: Corners? = null
        viewModelScope.launch {
            findPaperSheetUseCase(
                FindPaperSheetContours.Params(bitmap)
            ) { corners: Corners? ->
                result = corners
            }
        }
        return result
    }

    private fun handleFailure(failure: Failure) {
        errors.value = failure.origin
    }
}
