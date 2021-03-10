package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_cropper.*
import net.kuama.documentscanner.R
import net.kuama.documentscanner.domain.Failure
import net.kuama.documentscanner.domain.FindPaperSheetContours
import net.kuama.documentscanner.domain.PerspectiveTransform
import net.kuama.documentscanner.domain.UriToBitmap
import net.kuama.scanner.data.Corners

class CropperActivity: AppCompatActivity() {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()
    private val uriToBitmap: UriToBitmap = UriToBitmap()

    private val corners = MutableLiveData<Corners?>()
    private val bitmap = MutableLiveData<Bitmap>()
    private val finalDocument = MutableLiveData<Bitmap>()

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
        var value = ""
        if (extras != null) {
            value = intent.extras?.getString("lastUri")!!
        }

        uriToBitmap(
            UriToBitmap.Params(
                uri = value.toUri(),
                contentResolver = contentResolver
            )
        ) {
            it.fold(::handleFailure) { preview ->
                analyze(preview, Matrix(), returnOriginalMat = true) { pair ->
                    pair.second?.let {
                        cropPreview.setImageBitmap(preview)
                        cropWrap.visibility = View.VISIBLE
                    }

                    cropWrap.waitForLayout {
                        corners.value = pair.second!!
                        bitmap.value = pair.first
                    }
                }
            }
        }

        acceptFinalResult.setOnClickListener {
            finish()
        }

        closeResultPreview.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            this.startActivity(intent)
        }

        closeCropPreview.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            this.startActivity(intent)
        }

        confirmCropResult.setOnClickListener {
            cropResultWrap.visibility = View.GONE
            finalResult.setImageBitmap(finalDocument.value)
            finalResultWrap.visibility = View.VISIBLE
        }

        confirmCropPreview.setOnClickListener {
            val sel = loadBitmapFromView(cropPreview)
            perspectiveTransform(
                PerspectiveTransform.Params(
                    bitmap = sel!!,
                    corners = corners.value!!
                )
            ) { result ->
                result.fold(::handleFailure) { bitmap ->
                    cropWrap.visibility = View.GONE
                    cropHud.visibility = View.INVISIBLE

                    finalDocument.value = bitmap

                    cropResultPreview.setImageBitmap(bitmap)
                    cropResultWrap.visibility = View.VISIBLE

                }
            }
        }

        cropPreview.setOnTouchListener { _, motionEvent ->
            cropHud.onTouch(motionEvent)
        }

        corners.observe(this, Observer {
            it?.let { corners ->
                cropHud.onCorners(corners, cropWrap.measuredWidth, cropPreview.measuredHeight)
            }
        })
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

    private fun handleFailure(failure: Failure) {

    }

    private fun analyze(
        bitmap: Bitmap,
        matrix: Matrix,
        onSuccess: (() -> Unit)? = null,
        returnOriginalMat: Boolean = false,
        callback: ((Pair<Bitmap, Corners?>) -> Unit)? = null
    ) {
        findPaperSheetUseCase(
            FindPaperSheetContours.Params(
                bitmap,
                matrix,
                0.0,
                returnOriginalMat
            )
        ) {
            it.fold(::handleFailure) { pair: Pair<Bitmap, Corners?> ->
                callback?.invoke(pair) ?: run { }
                onSuccess?.invoke()
            }
        }
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

