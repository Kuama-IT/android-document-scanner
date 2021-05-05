package net.kuama.documentscanner.extensions

import android.content.Context
import net.kuama.documentscanner.R
import java.io.File

/** Use external media if it is available, our app's file directory otherwise */
internal val Context.outputDirectory: File
    get() {
        val appContext = applicationContext
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }
