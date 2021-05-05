package net.kuama.documentscanner.extensions

import android.view.View
import androidx.appcompat.app.AppCompatActivity

private const val FULLSCREEN_FLAGS = (View.SYSTEM_UI_FLAG_IMMERSIVE
        // Set the content to appear under the system bars so that the
        // content doesn't resize when the system bars hide and show.
        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        // Hide the nav bar and status bar
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_FULLSCREEN)

internal fun AppCompatActivity.triggerFullscreen() {
    window.decorView.systemUiVisibility = FULLSCREEN_FLAGS
}
