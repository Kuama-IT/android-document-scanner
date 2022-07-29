package net.kuama.documentscanner.extensions

import org.opencv.core.Point
import kotlin.math.pow
import kotlin.math.sqrt

fun Point.distanceTo(point: Point): Double {
    return sqrt((this.x - point.x).pow(2) + (this.y - point.y).pow(2))
}
