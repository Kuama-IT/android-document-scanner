package net.kuama.documentscanner.data

import EOpenCvStatus
import android.content.Context
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.lang.ref.WeakReference

class OpenCVLoader(context: Context) {
    private val reference = WeakReference(context)

    private var onLoad: ((EOpenCvStatus) -> Unit)? = null

    private val mLoaderCallback = object : BaseLoaderCallback(context.applicationContext) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    onLoad?.invoke(EOpenCvStatus.LOADED)
                }
                else -> {
                    super.onManagerConnected(status)
                    onLoad?.invoke(EOpenCvStatus.ERROR)
                }
            }
        }
    }

    fun load(callback: (EOpenCvStatus) -> Unit) = reference.get()?.let {
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
