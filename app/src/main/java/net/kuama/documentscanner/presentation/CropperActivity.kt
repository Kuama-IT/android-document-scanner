package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_cropper.*
import net.kuama.documentscanner.R

class CropperActivity : AppCompatActivity() {
    private lateinit var cropModel: CropperModel
    private lateinit var bitmapUri: Uri

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
        setContentView(R.layout.activity_cropper)

        val extras = intent.extras
        if (extras != null) {
            bitmapUri = intent.extras?.getString("lastUri")?.toUri() ?: error("invalid uri")
        }

        val cropModel: CropperModel by viewModels()

        // Picture taken from User
        cropModel.original.observe(this, Observer {
            cropPreview.setImageBitmap(cropModel.original.value)
            cropWrap.visibility = View.VISIBLE

            // Wait for bitmap to be loaded on view, then draw corners
            cropWrap.waitForLayout {
                cropHud.onCorners(corners = cropModel.corners.value ?: error("invalic Corners"), height = cropPreview.measuredHeight, width = cropPreview.measuredWidth)
            }
        })

        cropModel.finalDocument.observe(this, Observer {
            finalResult.setImageBitmap(cropModel.finalDocument.value)
        })

        cropModel.bitmapToCrop.observe(this, Observer {
            cropResultPreview.setImageBitmap(cropModel.bitmapToCrop.value)
        })

        acceptFinalResult.setOnClickListener {
            finish()
        }

        closeResultPreview.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            finish()
            this.startActivity(intent)
        }

        closeCropPreview.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            finish()
            this.startActivity(intent)
        }

        confirmCropPreview.setOnClickListener {
            cropWrap.visibility = View.GONE
            cropHud.visibility = View.GONE
            loadBitmapFromView(cropPreview)?.let { bitmapToCrop -> cropModel.onCornersAccepted(bitmapToCrop) }
            cropResultWrap.visibility = View.VISIBLE
        }

        confirmCropResult.setOnClickListener {
            cropResultWrap.visibility = View.GONE
            cropModel.onAcceptResult()
            finalResultWrap.visibility = View.VISIBLE
        }

        cropPreview.setOnTouchListener { _, motionEvent ->
            cropHud.onTouch(motionEvent)
        }

        this.cropModel = cropModel
    }

    override fun onResume() {
        super.onResume()
        cropModel.onViewCreated(bitmapUri, contentResolver)
    }

    private fun loadBitmapFromView(v: View): Bitmap? {
        val b = Bitmap.createBitmap(
            v.measuredWidth,
            v.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }
}

private inline fun View.waitForLayout(crossinline yourAction: () -> Unit) {
    val vto = viewTreeObserver
    vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            when {
                vto.isAlive -> {
                    vto.removeOnGlobalLayoutListener(this)
                    yourAction()
                }
                else -> viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
    })
}
