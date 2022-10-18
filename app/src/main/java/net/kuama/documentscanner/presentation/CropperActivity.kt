package net.kuama.documentscanner.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.doOnNextLayout
import net.kuama.documentscanner.R
import net.kuama.documentscanner.data.Corners
import net.kuama.documentscanner.databinding.ActivityCropperBinding
import net.kuama.documentscanner.extensions.loadBitmapFromView
import net.kuama.documentscanner.extensions.outputDirectory
import net.kuama.documentscanner.extensions.percentOf
import net.kuama.documentscanner.extensions.screenHeight
import net.kuama.documentscanner.extensions.screenWidth
import net.kuama.documentscanner.extensions.toByteArray
import net.kuama.documentscanner.viewmodels.CropperModel
import org.opencv.core.Point
import org.opencv.core.Size
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

@SuppressLint("ClickableViewAccessibility")
class CropperActivity : AppCompatActivity() {
    private lateinit var cropModel: CropperModel
    private lateinit var bitmapUri: Uri
    private var screenOrientationDeg: Int = 0
    private lateinit var binding: ActivityCropperBinding
    private val defaultCorners = Corners(
        topLeft = Point(screenWidth.percentOf(25), screenHeight.percentOf(25)),
        topRight = Point(screenWidth.percentOf(80), screenHeight.percentOf(25)),
        bottomRight = Point(screenWidth.percentOf(80), screenHeight.percentOf(70)),
        bottomLeft = Point(screenWidth.percentOf(25), screenHeight.percentOf(70)),
        size = Size(screenWidth.toDouble(), screenHeight.toDouble())
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropperBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val extras = intent.extras
        if (extras != null) {
            bitmapUri = intent.extras?.getString("lastUri")?.toUri() ?: error("invalid uri")
            screenOrientationDeg = if (intent.extras?.getInt("screenOrientationDeg") != null) intent.extras!!.getInt("screenOrientationDeg") else 0
        }

        val cropModel: CropperModel by viewModels()

        // Picture taken from User
        cropModel.originalBitmap.observe(this) {
            binding.cropPreview.setImageBitmap(cropModel.originalBitmap.value)
            binding.cropPreview.visibility = View.VISIBLE

            // Wait for bitmap to be loaded on view, then draw corners
            binding.cropPreview.doOnNextLayout {
                binding.cropHud.onCorners(
                    corners = cropModel.corners.value ?: error("invalid Corners"),
                    height = binding.cropPreview.measuredHeight,
                    width = binding.cropPreview.measuredWidth
                )
            }
        }

        cropModel.errors.observe(this) {
            Toast.makeText(this, this.resources.getText(R.string.crop_error), Toast.LENGTH_SHORT).show()
        }

        binding.closeCropPreview.setOnClickListener {
            closeActivity()
        }

        binding.rescan.setOnClickListener {
            closeActivity()
        }

        binding.confirmCrop.setOnClickListener {
            binding.cropPreview.visibility = View.GONE
            binding.cropHud.visibility = View.GONE
            val bitmapToCrop = loadBitmapFromView(binding.cropPreview)

            cropModel.onCornersAccepted(bitmapToCrop)

            val file = File(this.outputDirectory(), "${UUID.randomUUID()}.jpg")

            val outputStream = FileOutputStream(file)
            val byteArrayOutputStream = ByteArrayOutputStream()
            outputStream.write(cropModel.bitmapToCrop.value?.toByteArray(byteArrayOutputStream))
            outputStream.close()
            byteArrayOutputStream.close()

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
        cropModel.onViewCreated(bitmapUri, screenOrientationDeg, defaultCorners)
    }

    private fun closeActivity() {
        this.setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
