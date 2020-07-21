package net.kuama.scanner.data

import org.opencv.core.Point
import org.opencv.core.Size

data class Corners(val corners: List<Point?>, val size: Size)
