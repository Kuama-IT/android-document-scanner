package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_scanner.*
import net.kuama.documentscanner.R
import net.kuama.documentscanner.data.Loader
import net.kuama.documentscanner.domain.Failure
import net.kuama.documentscanner.domain.PerspectiveTransform
import java.io.File

@androidx.camera.core.ExperimentalGetImage
abstract class BaseScannerActivity : AppCompatActivity() {
    private lateinit var viewModel: ScannerViewModel
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private var thresholdValue = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        setContentView(R.layout.activity_scanner)
        val viewModel: ScannerViewModel by viewModels()
        viewModel.isBusy.observe(this, Observer { isBusy ->
            if (isBusy) {
                progress.visibility = View.VISIBLE
            } else {
                progress.visibility = View.INVISIBLE
            }
        })

        viewModel.errors.observe(this, Observer {
            onError(it)
            Log.e(ScannerActivity::class.java.simpleName, it.message, it)
        })

        viewModel.corners.observe(this, Observer {
            it?.let { corners ->
                hud.onCornersDetected(corners)
            } ?: run {
                hud.onCornersNotDetected()
            }
        })

        viewModel.documentPreview.observe(this, Observer {
            documentPreview.setImageBitmap(it)
            previewWrap.visibility = View.VISIBLE
        })

        viewModel.flashStatus.observe(this, Observer { status ->
            flashToggle.setImageResource(
                when (status) {
                    FlashStatus.ON -> R.drawable.ic_flash_on
                    FlashStatus.OFF -> R.drawable.ic_flash_off
                    else -> R.drawable.ic_flash_off
                }
            )
        })

        threshold.max = 15
        threshold.progress = thresholdValue

        threshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.onThresholdChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        flashToggle.setOnClickListener {
            viewModel.onFlashToggle()
        }

        shutter.setOnClickListener {
            viewModel.onTakePicture(getOutputDirectory(), this)
        }

        closePreview.setOnClickListener {
            closePreview()
        }

        confirmDocument.setOnClickListener {
            previewWrap.visibility = View.GONE
            onDocumentAccepted(documentPreview.drawToBitmap())
        }

        closeScanner.setOnClickListener {
            onClose()
        }
        this.viewModel = viewModel
    }

    /** Use external media if it is available, our app's file directory otherwise */
    private fun getOutputDirectory(): File {
        val appContext = applicationContext
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    private fun handleFailure(failure: Failure) {
        viewModel.errors.value = failure.origin
        viewModel.isBusy.value = false
    }

    override fun onResume() {
        super.onResume()
        viewModel.onViewCreated(Loader(this), this, viewFinder)
    }

    fun closePreview() {
        super.onResume()
        previewWrap.visibility = View.GONE
        viewModel.onViewCreated(Loader(this), this, viewFinder)

    }

    abstract fun onError(throwable: Throwable)
    abstract fun onDocumentAccepted(bitmap: Bitmap)
    abstract fun onClose()
}
