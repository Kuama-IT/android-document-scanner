package net.kuama.documentscanner.presentation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import net.kuama.documentscanner.data.Corners
import org.opencv.core.Point
import kotlin.math.abs

class PaperRectangle : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attributes: AttributeSet) : super(context, attributes)

    constructor(context: Context, attributes: AttributeSet, defTheme: Int) : super(context, attributes, defTheme)

    private val rectPaint = Paint()
    private val extCirclePaint = Paint()
    private val intCirclePaint = Paint()
    private val intCirclePaintR = Paint()
    private val extCirclePaintR = Paint()
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

    init {
        rectPaint.color = Color.parseColor("#3454D1")
        rectPaint.isAntiAlias = true
        rectPaint.isDither = true
        rectPaint.strokeWidth = 6F
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        rectPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        rectPaint.pathEffect = CornerPathEffect(10f)

        fillPaint.color = Color.parseColor("#3454D1")
        fillPaint.alpha = 60
        fillPaint.isAntiAlias = true
        fillPaint.isDither = true
        fillPaint.strokeWidth = 6F
        fillPaint.style = Paint.Style.FILL
        fillPaint.strokeJoin = Paint.Join.ROUND // set the join to round you want
        fillPaint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        fillPaint.pathEffect = CornerPathEffect(10f)

        extCirclePaint.color = Color.parseColor("#3454D1")
        extCirclePaint.isDither = true
        extCirclePaint.isAntiAlias = true
        extCirclePaint.strokeWidth = 8F
        extCirclePaint.style = Paint.Style.STROKE

        intCirclePaint.color = Color.DKGRAY
        intCirclePaint.isDither = true
        intCirclePaint.isAntiAlias = true
        intCirclePaint.strokeWidth = 10F
        intCirclePaint.style = Paint.Style.FILL

        intCirclePaintR.color = Color.RED
        intCirclePaintR.isDither = true
        intCirclePaintR.isAntiAlias = true
        intCirclePaintR.strokeWidth = 10F
        intCirclePaintR.style = Paint.Style.FILL

        extCirclePaintR.color = Color.RED
        extCirclePaintR.isDither = true
        extCirclePaintR.isAntiAlias = true
        extCirclePaintR.strokeWidth = 8F
        extCirclePaintR.style = Paint.Style.STROKE
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
        canvas?.drawPath(path, rectPaint)

        if (cropMode) {
            canvas?.drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 40F, extCirclePaint)

            canvas?.drawCircle(topLeft.x.toFloat(), topLeft.y.toFloat(), 35F, intCirclePaint)
            canvas?.drawCircle(topRight.x.toFloat(), topRight.y.toFloat(), 35F, intCirclePaint)
            canvas?.drawCircle(bottomLeft.x.toFloat(), bottomLeft.y.toFloat(), 35F, intCirclePaint)
            canvas?.drawCircle(bottomRight.x.toFloat(), bottomRight.y.toFloat(), 35F, intCirclePaint)
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
