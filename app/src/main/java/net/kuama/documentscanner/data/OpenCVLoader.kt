package net.kuama.documentscanner.data

import android.content.Context
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.lang.ref.WeakReference

enum class OpenCvStatus {
    LOADED, ERROR
}

class OpenCVLoader(context: Context) {
    private val reference = WeakReference(context)

    private var onLoad: ((OpenCvStatus) -> Unit)? = null
    private val mLoaderCallback = object : BaseLoaderCallback(context.applicationContext) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    onLoad?.invoke(OpenCvStatus.LOADED)
                }
                else -> {
                    super.onManagerConnected(status)
                    onLoad?.invoke(OpenCvStatus.ERROR)
                }
            }
        }
    }

    fun load(callback: (OpenCvStatus) -> Unit) = reference.get()?.let {
        onLoad = callback
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(
                OpenCVLoader.OPENCV_VERSION,
                it.applicationContext,
                mLoaderCallback
            )
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
}
