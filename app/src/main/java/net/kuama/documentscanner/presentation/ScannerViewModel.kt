package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.graphics.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures.addCallback
import androidx.camera.view.CameraController.IMAGE_ANALYSIS
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.kuama.documentscanner.data.Loader
import net.kuama.documentscanner.data.OpenCvStatus
import net.kuama.documentscanner.domain.*
import net.kuama.scanner.data.Corners
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

enum class FlashStatus {
    ON, OFF
}

class ScannerViewModel : ViewModel() {
    private lateinit var controller: LifecycleCameraController

    /**
     * Observable data
     */
    val isBusy = MutableLiveData<Boolean>()
    val openCv = MutableLiveData<OpenCvStatus>()
    val corners = MutableLiveData<Corners?>()
    val errors = MutableLiveData<Throwable>()
    val flashStatus = MutableLiveData<FlashStatus>()
    val documentPreview = MutableLiveData<Bitmap>()
    private var didLoadOpenCv = false

    /**
     * Use cases
     */
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()

    /**
     * Tries to load OpenCv native libraries
     */
    fun onViewCreated(
        loader: Loader,
        scannerActivity: AppCompatActivity,
        viewFinder: PreviewView
    ) {
        isBusy.value = true
        setupCamera(scannerActivity, viewFinder) {
            if (!didLoadOpenCv) {
                loader.load {
                    isBusy.value = false
                    openCv.value = it
                    didLoadOpenCv = true
                }
            } else {
                isBusy.value = false
            }
        }
    }

    fun onFlashToggle() {
        flashStatus.value?.let { currentValue ->
            flashStatus.value = when (currentValue) {
                FlashStatus.ON -> FlashStatus.OFF
                FlashStatus.OFF -> FlashStatus.ON
            }
        } ?: // default flash status is off
        run {
            // default flash status is off
            // default flash status is off
            flashStatus.value = FlashStatus.ON
        }
        when (flashStatus.value) {
            FlashStatus.ON -> controller.enableTorch(true)
            FlashStatus.OFF -> controller.enableTorch(false)
            null -> controller.enableTorch(false)
        }
    }
    fun onTakePicture(outputDirectory: File, context: Context) {
        isBusy.value = true
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        controller.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    errors.value = exc
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lastUri = Uri.fromFile(photoFile)

                    val intent = Intent(context, CropperActivity::class.java)
                    intent.putExtra("lastUri", lastUri.toString())
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            })
    }

    // CameraX setup
    private var lastUri: Uri? = null

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun setupCamera(
        lifecycleOwner: AppCompatActivity,
        viewFinder: PreviewView,
        then: () -> Unit
    ) {
        isBusy.value = true

        val executor: Executor = ContextCompat.getMainExecutor(lifecycleOwner)
        controller = LifecycleCameraController(lifecycleOwner)
        controller.setImageAnalysisAnalyzer(executor, ImageAnalysis.Analyzer { proxy: ImageProxy ->
            // could not find a performing way to transform
            // the proxy to a bitmap, so we are reading
            // the bitmap directly from the preview view

            viewFinder.bitmap?.let {
                analyze(it, onSuccess = {
                    proxy.close()
                })
            } ?: run {
                corners.value = null
                proxy.close()
            }
        })
        controller.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        controller.setEnabledUseCases(IMAGE_CAPTURE or IMAGE_ANALYSIS)

        controller.bindToLifecycle(lifecycleOwner)
        viewFinder.controller = controller
        addCallback(
            controller.initializationFuture,
            object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                    then()
                }
                override fun onFailure(t: Throwable?) {
                    errors.value = t
                }
            },
            executor
        )
        then.invoke()
    }
    private fun analyze(
        bitmap: Bitmap,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((Pair<Bitmap, Corners?>) -> Unit)? = null
    ) {
        findPaperSheetUseCase(FindPaperSheetContours.Params(bitmap, returnOriginalMat)) {
            it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                callback?.invoke(pair) ?: run {
                    corners.value = pair.second
                }
                onSuccess?.invoke()
            }
        }
    }
    private fun handleFailure(failure: Failure) {
        errors.value = failure.origin
        isBusy.value = false
    }

    fun onClosePreview() {
        lastUri?.let {
            val file = File(it.path!!)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
