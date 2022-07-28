package net.kuama.documentscanner.presentation

import android.graphics.Bitmap
import android.widget.Toast
import net.kuama.documentscanner.R
import net.kuama.documentscanner.exceptions.MissingSquareException

class ScannerActivity : BaseScannerActivity() {
    override fun onError(throwable: Throwable) {
        when (throwable) {
            is MissingSquareException -> Toast.makeText(
                this,
                R.string.null_corners, Toast.LENGTH_LONG
            )
                .show()
            else -> Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDocumentAccepted(bitmap: Bitmap) {
    }

    override fun onClose() {
        finish()
    }
}
