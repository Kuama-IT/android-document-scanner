package net.kuama.documentscanner.presentation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import net.kuama.scanner.data.Corners
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
    private var tl: Point = Point()
    private var tr: Point = Point()
    private var br: Point = Point()
    private var bl: Point = Point()
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
        tl = corners.corners[0] ?: Point()
        tr = corners.corners[1] ?: Point()
        br = corners.corners[2] ?: Point()
        bl = corners.corners[3] ?: Point()
        printLog(corners)
        resize()
        path.reset()
        path.close()
        invalidate()
    }

    fun onCornersDetected(corners: Corners) {
        ratioX = corners.size.width.div(measuredWidth)
        ratioY = corners.size.height.div(measuredHeight)
        tl = corners.corners[0] ?: Point()
        tr = corners.corners[1] ?: Point()
        br = corners.corners[2] ?: Point()
        bl = corners.corners[3] ?: Point()
        printLog(corners)
        resize()
        path.reset()

        path.moveTo(tl.x.toFloat(), tl.y.toFloat())
        path.lineTo(tr.x.toFloat(), tr.y.toFloat())
        path.lineTo(br.x.toFloat(), br.y.toFloat())
        path.lineTo(bl.x.toFloat(), bl.y.toFloat())

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
            canvas?.drawCircle(tl.x.toFloat(), tl.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(tr.x.toFloat(), tr.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(bl.x.toFloat(), bl.y.toFloat(), 40F, extCirclePaint)
            canvas?.drawCircle(br.x.toFloat(), br.y.toFloat(), 40F, extCirclePaint)

            canvas?.drawCircle(tl.x.toFloat(), tl.y.toFloat(), 35F, intCirclePaint)
            canvas?.drawCircle(tr.x.toFloat(), tr.y.toFloat(), 35F, intCirclePaint)
            canvas?.drawCircle(bl.x.toFloat(), bl.y.toFloat(), 35F, intCirclePaint)
            canvas?.drawCircle(br.x.toFloat(), br.y.toFloat(), 35F, intCirclePaint)
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
        val points = listOf(tl, tr, br, bl)
        point2Move = points.minBy { abs((it.x - downX).times(it.y - downY)) } ?: tl
    }

    private fun movePoints() {
        path.reset()
        path.close()
        invalidate()
    }

    private fun resize() {
        tl.x = tl.x.div(ratioX)
        tl.y = tl.y.div(ratioY)
        tr.x = tr.x.div(ratioX)
        tr.y = tr.y.div(ratioY)
        br.x = br.x.div(ratioX)
        br.y = br.y.div(ratioY)
        bl.x = bl.x.div(ratioX)
        bl.y = bl.y.div(ratioY)
    }

    private fun printLog(corners: Corners) {
        Log.d(javaClass.simpleName, "size: ${corners.size.width}x${corners.size.height} - tl: ${tl.x}, ${tl.y} - tr: ${tr.x}, ${tr.y} - br: ${br.x}, ${br.y} - bl: ${bl.x}, ${bl.y}")
    }
}
