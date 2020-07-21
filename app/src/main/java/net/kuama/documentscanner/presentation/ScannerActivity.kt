package net.kuama.documentscanner.presentation

import android.widget.Toast
import net.kuama.documentscanner.R
import net.kuama.documentscanner.exceptions.NullCorners

class ScannerActivity : BaseScannerActivity() {
    override fun onError(throwable: Throwable) {
        when (throwable) {
            is NullCorners -> Toast.makeText(
                this,
                R.string.null_corners, Toast.LENGTH_LONG
            )
                .show()
            else -> Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDocumentAccepted(path: String) {
        Toast.makeText(this, path, Toast.LENGTH_LONG).show()
    }

    override fun onClose() {
        finish()
    }
}
