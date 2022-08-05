package net.kuama.documentscanner.extensions

import android.content.Context
import net.kuama.documentscanner.R
import java.io.File

/** Use external media if it is available, our app's file directory otherwise */

fun Context.outputDirectory(): File {
    val mediaDir =
        File(filesDir, applicationContext.resources.getString(R.string.app_name)).apply {
            mkdirs()
        }

    return if (mediaDir.exists())
        mediaDir else applicationContext.filesDir
}
