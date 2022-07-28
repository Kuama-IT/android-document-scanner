package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import net.kuama.documentscanner.databinding.ActivityCropperBinding
import net.kuama.documentscanner.extensions.outputDirectory
import net.kuama.documentscanner.extensions.toByteArray
import net.kuama.documentscanner.extensions.waitForLayout
import net.kuama.documentscanner.viewmodels.CropperModel
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CropperActivity : AppCompatActivity() {
    private lateinit var cropModel: CropperModel
    private lateinit var bitmapUri: Uri
    private lateinit var binding: ActivityCropperBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        if (extras != null) {
            bitmapUri = intent.extras?.getString("lastUri")?.toUri() ?: error("invalid uri")
        }

        val cropModel: CropperModel by viewModels()

        // Picture taken from User
        cropModel.originalBitmap.observe(this) {
            binding.cropPreview.setImageBitmap(cropModel.originalBitmap.value)
            binding.cropWrap.visibility = View.VISIBLE

            // Wait for bitmap to be loaded on view, then draw corners
            binding.cropWrap.waitForLayout {
                binding.cropHud.onCorners(
                    corners = cropModel.corners.value ?: error("invalid Corners"),
                    height = binding.cropPreview.measuredHeight,
                    width = binding.cropPreview.measuredWidth
                )
            }
        }

        cropModel.bitmapToCrop.observe(this) {
            binding.cropResultPreview.setImageBitmap(cropModel.bitmapToCrop.value)
        }

        binding.closeResultPreview.setOnClickListener {
            closeActivity()
        }

        binding.closeCropPreview.setOnClickListener {
            closeActivity()
        }

        binding.confirmCropPreview.setOnClickListener {
            binding.cropWrap.visibility = View.GONE
            binding.cropHud.visibility = View.GONE
            loadBitmapFromView(binding.cropPreview)?.let { bitmapToCrop ->
                cropModel.onCornersAccepted(
                    bitmapToCrop
                )
            }
            binding.cropResultWrap.visibility = View.VISIBLE
        }

        binding.confirmCropResult.setOnClickListener {

            val file = File(outputDirectory, "${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            outputStream.write(cropModel.bitmapToCrop.value?.toByteArray())
            outputStream.close()

            val resultIntent = Intent()
            resultIntent.putExtra("croppedPath", file.absolutePath)
            setResult(RESULT_OK, resultIntent)

            finish()
        }

        binding.cropPreview.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            view.performClick()
            binding.cropHud.onTouch(motionEvent)
        }

        this.cropModel = cropModel
    }

    override fun onResume() {
        super.onResume()
        cropModel.onViewCreated(bitmapUri, contentResolver)
    }

    private fun closeActivity() {
        this.setResult(Activity.RESULT_CANCELED)
        finish()
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
