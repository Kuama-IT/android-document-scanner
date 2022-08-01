package net.kuama.documentscanner.utils

import net.kuama.documentscanner.data.Corners
import org.opencv.core.Point
import org.opencv.core.Size
import java.util.*
import kotlin.Comparator

object PointUtils {
    fun getSortedCorners(cornersList: List<Point>, originalSize: Size): Corners {
        val sumComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                (lhs.y + lhs.x).compareTo(rhs.y + rhs.x)
            }
        val diffComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                (lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
            }

        return Corners(
            topLeft = Collections.min(cornersList, sumComparator),
            topRight = Collections.min(cornersList, diffComparator),
            bottomRight = Collections.max(cornersList, sumComparator),
            bottomLeft = Collections.max(cornersList, diffComparator),
            size = originalSize
        )
    }
}
