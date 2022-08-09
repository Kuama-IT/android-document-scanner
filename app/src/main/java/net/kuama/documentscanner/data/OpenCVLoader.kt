package net.kuama.documentscanner.data

import net.kuama.documentscanner.enums.EOpenCvStatus
import android.content.Context
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

class OpenCVLoader(context: Context) {
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

    fun load(callback: (EOpenCvStatus) -> Unit) {
        onLoad = callback

        OpenCVLoader.initDebug()
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }
}
