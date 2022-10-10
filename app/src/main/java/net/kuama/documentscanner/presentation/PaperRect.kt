package net.kuama.documentscanner.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Magnifier
import androidx.annotation.RequiresApi
import net.kuama.documentscanner.data.Corners
import org.opencv.core.Point
import kotlin.math.abs

class PaperRectangle : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)

    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(context, attributes, defTheme)

    private val pointPaint = Paint()
    private val linePaint = Paint()
    private val fillPaint = Paint()
    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var topLeft: Point = Point()
    private var topRight: Point = Point()
    private var bottomRight: Point = Point()
    private var bottomLeft: Point = Point()
    private val path: Path = Path()
    private var point2Move = Point()
    private var cropMode = false
    private var latestDownX = 0.0F
    private var latestDownY = 0.0F

    private var magnifier: Magnifier? = null

    init {
        pointPaint.color = Color.parseColor("#3454D1")
        pointPaint.isAntiAlias = true
        pointPaint.isDither = true
        pointPaint.strokeWidth = 6F
        pointPaint.style = Paint.Style.FILL
        pointPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        pointPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        pointPaint.pathEffect = CornerPathEffect(10f)

        fillPaint.color = Color.parseColor("#3454D1")
        fillPaint.alpha = 60
        fillPaint.isAntiAlias = true
        fillPaint.isDither = true
        fillPaint.strokeWidth = 6F
        fillPaint.style = Paint.Style.FILL
        fillPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        fillPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        fillPaint.pathEffect = CornerPathEffect(10f)

        linePaint.color = Color.parseColor("#3454D1")
        linePaint.isAntiAlias = true
        linePaint.isDither = true
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 4f

        if (SDK_INT >= Q) {
            magnifier = Magnifier.Builder(this)
                .apply {
                    setSize(300, 300)
                    setCornerRadius(150f)
                    setInitialZoom(1.5f)
                    setDefaultSourceToMagnifierOffset(0, - 300)
                }.build()
        }
    }

    fun onCorners(corners: Corners, width: Int, height: Int) {
        cropMode = true
        ratioX = corners.size.width.div(width)
        ratioY = corners.size.height.div(height)
        topLeft = corners.topLeft
        topRight = corners.topRight
        bottomRight = corners.bottomRight
        bottomLeft = corners.bottomLeft

        resize()
        path.reset()
        path.close()
        invalidate()
    }

    fun onCornersDetected(corners: Corners) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        topLeft = corners.topLeft
        topRight = corners.topRight
        bottomRight = corners.bottomRight
        bottomLeft = corners.bottomLeft

        resize()
        path.reset()

        path.moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
        path.lineTo(topRight.x.toFloat(), topRight.y.toFloat())
        path.lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
        path.lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())

        path.close()
        invalidate()
    }

    fun onCornersNotDetected() {
        path.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawPath(path, fillPaint)
        canvas?.drawPath(path, fillPaint)

        if (cropMode) {


            canvas?.drawLine(
                topLeft.x.toFloat(),
                topLeft.y.toFloat(),
                topRight.x.toFloat(),
                topRight.y.toFloat(),
                linePaint
            )
            canvas?.drawLine(
                topLeft.x.toFloat(),
                topLeft.y.toFloat(),
                bottomLeft.x.toFloat(),
                bottomLeft.y.toFloat(),
                linePaint
            )
            canvas?.drawLine(
                bottomLeft.x.toFloat(), bottomLeft.y.toFloat(),
                bottomRight.x.toFloat(),
                bottomRight.y.toFloat(),
                linePaint
            )
            canvas?.drawLine(
                bottomRight.x.toFloat(),
                bottomRight.y.toFloat(),
                topRight.x.toFloat(),
                topRight.y.toFloat(),
                linePaint
            )

            canvas?.drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 30F, pointPaint)
            canvas?.drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 30F, pointPaint)
            canvas?.drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 30F, pointPaint)
            canvas?.drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 30F, pointPaint)
        }
    }

    fun onTouch(event: MotionEvent?): Boolean {
        if (!cropMode) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                latestDownX = event.x
                latestDownY = event.y
                calculatePoint2Move(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                point2Move.x = (event.x - latestDownX) + point2Move.x
                point2Move.y = (event.y - latestDownY) + point2Move.y
                movePoints()
                latestDownY = event.y
                latestDownX = event.x

                if (SDK_INT >= Q) magnifier?.show(event.rawX, event.rawY)

            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (SDK_INT >= Q) magnifier?.dismiss()
            }
        }
        return true
    }

    private fun calculatePoint2Move(downX: Float, downY: Float) {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        point2Move = points.minByOrNull { abs((it.x - downX).times(it.y - downY)) } ?: topLeft
    }

    private fun movePoints() {
        path.reset()
        path.close()
        invalidate()
    }

    private fun resize() {
        topLeft.x = topLeft.x.div(ratioX)
        topLeft.y = topLeft.y.div(ratioY)
        topRight.x = topRight.x.div(ratioX)
        topRight.y = topRight.y.div(ratioY)
        bottomRight.x = bottomRight.x.div(ratioX)
        bottomRight.y = bottomRight.y.div(ratioY)
        bottomLeft.x = bottomLeft.x.div(ratioX)
        bottomLeft.y = bottomLeft.y.div(ratioY)
    }
}
