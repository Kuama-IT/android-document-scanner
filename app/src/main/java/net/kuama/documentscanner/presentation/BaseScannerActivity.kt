package net.kuama.documentscanner.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_scanner.*
import net.kuama.documentscanner.R
import net.kuama.documentscanner.data.Loader
import java.io.File

abstract class BaseScannerActivity : AppCompatActivity() {
    private lateinit var viewModel: ScannerViewModel

    private var thresholdValue = 48

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
            } ?: {
                hud.onCornersNotDetected()
            }()
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

        threshold.max = 255
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
            previewWrap.visibility = View.GONE
            viewModel.onClosePreview()
        }

        confirmDocument.setOnClickListener {
            onDocumentAccepted(viewModel.documentPath!!)
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

    override fun onResume() {
        super.onResume()
        viewModel.onViewCreated(Loader(this), this, viewFinder)
    }

    abstract fun onError(throwable: Throwable)
    abstract fun onDocumentAccepted(path: String)
    abstract fun onClose()
}
