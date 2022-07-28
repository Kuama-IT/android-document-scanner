package net.kuama.documentscanner.extensions

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener

internal inline fun View.waitForLayout(crossinline yourAction: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            when {
                viewTreeObserver.isAlive -> {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    yourAction()
                }
                else -> viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
    })
}
