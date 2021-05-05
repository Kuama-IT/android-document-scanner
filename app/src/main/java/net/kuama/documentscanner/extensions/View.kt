package net.kuama.documentscanner.extensions

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener

internal inline fun View.waitForLayout(crossinline yourAction: () -> Unit) {
    val vto = viewTreeObserver
    vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
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
