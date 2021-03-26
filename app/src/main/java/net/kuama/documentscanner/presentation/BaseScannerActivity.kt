package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_scanner.*
import net.kuama.documentscanner.R
import net.kuama.documentscanner.data.Loader
import net.kuama.documentscanner.domain.Failure
import net.kuama.documentscanner.domain.PerspectiveTransform
import java.io.File
import android.graphics.BitmapFactory

@androidx.camera.core.ExperimentalGetImage
abstract class BaseScannerActivity : AppCompatActivity() {
    private lateinit var viewModel: ScannerViewModel
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()

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

        viewModel.lastUri.observe(this, Observer {
            val intent = Intent(this, CropperActivity::class.java)
            intent.putExtra("lastUri", it.toString())
            this.startActivityForResult(intent, 0)
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

        viewModel.flashStatus.observe(this, Observer { status ->
            flashToggle.setImageResource(
                when (status) {
                    FlashStatus.ON -> R.drawable.ic_flash_on
                    FlashStatus.OFF -> R.drawable.ic_flash_off
                    else -> R.drawable.ic_flash_off
                }
            )
        })

        flashToggle.setOnClickListener {
            viewModel.onFlashToggle()
        }

        shutter.setOnClickListener {
            viewModel.onTakePicture(getOutputDirectory(), this)
        }

        closeScanner.setOnClickListener {
            closePreview()
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

    private fun closePreview() {
        root_view.visibility = View.GONE
        viewModel.onClosePreview()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val bitmapUri = data?.extras?.getString("croppedPath") ?: error("invalid path")

            val image: File = File(bitmapUri)
            val bmOptions = BitmapFactory.Options()
            val bitmap = BitmapFactory.decodeFile(image.absolutePath, bmOptions)
            onDocumentAccepted(bitmap)

            image.delete()
        } else {
            viewModel.onViewCreated(Loader(this), this, viewFinder)
        }
    }

    abstract fun onError(throwable: Throwable)
    abstract fun onDocumentAccepted(bitmap: Bitmap)
    abstract fun onClose()
}
