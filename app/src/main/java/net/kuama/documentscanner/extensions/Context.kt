package net.kuama.documentscanner.extensions

import android.content.Context
import android.content.res.Resources
import net.kuama.documentscanner.R
import java.io.File

/** Use external media if it is available, our app's file directory otherwise */

fun Context.outputDirectory(): File {
    return File(filesDir, applicationContext.resources.getString(R.string.app_name)).apply {
            mkdirs()
        }
}

val Context.screenWidth get() = Resources.getSystem().displayMetrics.widthPixels

val Context.screenHeight get() = Resources.getSystem().displayMetrics.heightPixels
