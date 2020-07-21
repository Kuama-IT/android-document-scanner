package net.kuama.documentscanner.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListenableFuture
import net.kuama.documentscanner.data.Loader
import net.kuama.documentscanner.data.OpenCvStatus
import net.kuama.documentscanner.domain.*
import net.kuama.documentscanner.exceptions.NullCorners
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
    private val findPaperSheetUseCase: FindPaperSheet = FindPaperSheet()
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val uriToBitmap: UriToBitmap = UriToBitmap()

    /**
     * See [THRESHOLD_BASE]
     */
    private var threshold = THRESHOLD_BASE

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

    fun onThresholdChange(threshold: Int) {
        this.threshold = threshold.toDouble()
    }

    fun onFlashToggle() {
        flashStatus.value?.let { currentValue ->
            flashStatus.value = when (currentValue) {
                FlashStatus.ON -> FlashStatus.OFF
                FlashStatus.OFF -> FlashStatus.ON
            }
        } ?: {
            // default flash status is off
            flashStatus.value = FlashStatus.ON
        }()

        when (flashStatus.value) {
            FlashStatus.ON -> camera?.cameraControl?.enableTorch(true)
            FlashStatus.OFF -> camera?.cameraControl?.enableTorch(false)
            null -> camera?.cameraControl?.enableTorch(false)
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

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    errors.value = exc
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lastUri = Uri.fromFile(photoFile)

                    uriToBitmap(
                        UriToBitmap.Params(
                            uri = lastUri!!,
                            contentResolver = context.contentResolver
                        )
                    ) {
                        it.fold(::handleFailure) { preview ->
                            analyze(preview, returnOriginalMat = true) { pair ->
                                pair.second?.let {
                                    perspectiveTransform(
                                        PerspectiveTransform.Params(
                                            bitmap = pair.first,
                                            corners = pair.second!!
                                        )
                                    ) { result ->
                                        isBusy.value = false
                                        result.fold(::handleFailure) { documentPreview ->
                                            this@ScannerViewModel.documentPreview.value =
                                                documentPreview
                                        }
                                    }
                                } ?: {
                                    errors.value = NullCorners()
                                    isBusy.value = false
                                }()
                            }
                        }
                    }
                }
            })
    }

    fun onClosePreview() {
        lastUri?.let {
            val file = File(it.path!!)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    val documentPath: String?
        get() = lastUri?.path

// CameraX setup

    private var lastUri: Uri? = null

    // Preview
    private val preview: Preview by lazy {
        Preview.Builder().build()
    }

    // Select back camera
    private val cameraSelector: CameraSelector =
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

    private val imageAnalysis: ImageAnalysis by lazy {
        ImageAnalysis.Builder().apply {
            setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        }.build()
    }

    private val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .build()
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var executor: Executor? = null

    private fun setupCamera(
        lifecycleOwner: AppCompatActivity,
        viewFinder: PreviewView,
        then: () -> Unit
    ) {
        isBusy.value = true
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(lifecycleOwner)
        executor = ContextCompat.getMainExecutor(lifecycleOwner)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
            try {
                // Unbind use cases before possible rebinding
                cameraProvider!!.unbindAll()

                imageAnalysis.setAnalyzer(executor!!, ImageAnalysis.Analyzer { proxy ->
                    // could not find a performing way to transform
                    // the proxy to a bitmap, so we are reading
                    // the bitmap directly from the preview view
                    viewFinder.bitmap?.let {
                        analyze(it, onSuccess = {
                            proxy.close()
                        })
                    } ?: {
                        corners.value = null
                        proxy.close()
                    }()
                })

                // Bind use cases to camera
                camera = cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
                preview.setSurfaceProvider(viewFinder.createSurfaceProvider())
            } catch (exc: Exception) {
                errors.value = exc
            }
        }, executor!!)
        then.invoke()
    }

    private fun analyze(
        bitmap: Bitmap,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((Pair<Bitmap, Corners?>) -> Unit)? = null
    ) {
        findPaperSheetUseCase(FindPaperSheet.Params(bitmap, threshold, returnOriginalMat)) {
            it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                callback?.invoke(pair) ?: {
                    corners.value = pair.second
                }()
                onSuccess?.invoke()
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        errors.value = failure.origin
        isBusy.value = false
    }
}
