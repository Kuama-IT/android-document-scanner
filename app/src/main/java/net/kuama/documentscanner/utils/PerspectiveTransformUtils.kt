package net.kuama.documentscanner.utils

import net.kuama.documentscanner.data.Corners
import org.opencv.core.Point
import java.util.*
import kotlin.Comparator

object PerspectiveTransformUtils {
    fun sortPoints(sourceCorners: Corners): Corners {
        val sourcePointList = arrayListOf(sourceCorners.bottomLeft, sourceCorners.bottomRight,
            sourceCorners.topLeft, sourceCorners.topRight)

        val sumComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x)
            }
        val diffComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
            }

        // top-left corner = minimal sum
        val topLeft = Collections.min(sourcePointList, sumComparator)

        // bottom-right corner = maximal sum
        val bottomRight = Collections.max(sourcePointList, sumComparator)

        // top-right corner = minimal difference
        val topRight = Collections.min(sourcePointList, diffComparator)

        // bottom-left corner = maximal difference
        val bottomLeft = Collections.max(sourcePointList, diffComparator)

        val orderedCornerList = listOf<Point>(topLeft, topRight, bottomRight, bottomLeft)

        return Corners(orderedCornerList, sourceCorners.size)
    }
}
