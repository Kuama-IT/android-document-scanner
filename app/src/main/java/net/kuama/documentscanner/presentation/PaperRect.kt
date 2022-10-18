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
import net.kuama.documentscanner.data.Corners
import org.opencv.core.Point
import kotlin.math.abs

class PaperRectangle : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)

    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(context, attributes, defTheme)

    private val strokePaint = Paint()
    private val pointPaint = Paint()
    private val linePaint = Paint()
    private val bluePaint = Paint()
    private val whitePaint = Paint()
    private var ratioX: Double = 1.0
    private var ratioY: Double = 1.0
    private var topLeft: Point = Point()
    private var topRight: Point = Point()
    private var bottomRight: Point = Point()
    private var bottomLeft: Point = Point()
    private val bluePath: Path = Path()
    private val whitePath = Path()
    private var point2Move = Point()
    private var cropMode = false
    private var latestDownX = 0.0F
    private var latestDownY = 0.0F

    private var magnifier: Magnifier? = null

    init {

        strokePaint.color = Color.parseColor("#FFFFFF")
        strokePaint.isAntiAlias = true
        strokePaint.isDither = true
        strokePaint.strokeWidth = 6F
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.pathEffect = CornerPathEffect(10f)

        pointPaint.color = Color.parseColor("#FFFFFF")
        pointPaint.alpha = 50
        pointPaint.isAntiAlias = true
        pointPaint.isDither = true
        pointPaint.strokeWidth = 6F
        pointPaint.style = Paint.Style.FILL
        pointPaint.strokeJoin = Paint.Join.ROUND
        pointPaint.strokeCap = Paint.Cap.ROUND
        pointPaint.pathEffect = CornerPathEffect(10f)

        bluePaint.color = Color.parseColor("#3454D1")
        bluePaint.alpha = 60
        bluePaint.isAntiAlias = true
        bluePaint.isDither = true
        bluePaint.strokeWidth = 6F
        bluePaint.style = Paint.Style.FILL
        bluePaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        bluePaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        bluePaint.pathEffect = CornerPathEffect(10f)

        whitePaint.color = Color.parseColor("#FFFFFF")
        whitePaint.alpha = 50
        whitePaint.isAntiAlias = true
        whitePaint.isDither = true
        whitePaint.strokeWidth = 6F
        whitePaint.style = Paint.Style.FILL
        whitePaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        whitePaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        whitePaint.pathEffect = CornerPathEffect(10f)

        linePaint.color = Color.parseColor("#FFFFFF")
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
        bluePath.reset()
        bluePath.close()
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
        bluePath.reset()

        bluePath.moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
        bluePath.lineTo(topRight.x.toFloat(), topRight.y.toFloat())
        bluePath.lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
        bluePath.lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())

        bluePath.close()
        invalidate()
    }

    fun onCornersNotDetected() {
        bluePath.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawPath(bluePath, bluePaint)
        canvas?.drawPath(bluePath, bluePaint)

        if (cropMode) {

            canvas?.drawLine(topLeft.x.toFloat(), topLeft.y.toFloat(), topRight.x.toFloat(), topRight.y.toFloat(), linePaint)
            canvas?.drawLine(topLeft.x.toFloat(), topLeft.y.toFloat(), bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), linePaint)
            canvas?.drawLine(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), bottomRight.x.toFloat(), bottomRight.y.toFloat(), linePaint)
            canvas?.drawLine(bottomRight.x.toFloat(), bottomRight.y.toFloat(), topRight.x.toFloat(), topRight.y.toFloat(), linePaint)

            canvas?.drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 35F, strokePaint)
            canvas?.drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 35F, strokePaint)
            canvas?.drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 35F, strokePaint)
            canvas?.drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 35F, strokePaint)

            canvas?.drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 30F, pointPaint)
            canvas?.drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 30F, pointPaint)
            canvas?.drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 30F, pointPaint)
            canvas?.drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 30F, pointPaint)

            whitePath.reset()

            whitePath.moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
            whitePath.lineTo(topRight.x.toFloat(), topRight.y.toFloat())
            whitePath.lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
            whitePath.lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())

            canvas?.drawPath(whitePath, whitePaint)
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

                if (SDK_INT >= Q) magnifier?.show(point2Move.x.toFloat(), point2Move.y.toFloat())

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
        bluePath.reset()
        bluePath.close()
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
