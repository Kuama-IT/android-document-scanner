package net.kuama.documentscanner.utils

import androidx.test.platform.app.InstrumentationRegistry
import net.kuama.documentscanner.data.OpenCVLoader as KOpenCVLoader

object TestUtils {
    fun loadOpenCvLibrary() {
        System.loadLibrary("opencv_java4")
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        KOpenCVLoader(appContext)
    }
}
