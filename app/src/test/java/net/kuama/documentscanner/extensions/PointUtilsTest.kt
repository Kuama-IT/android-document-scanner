package net.kuama.documentscanner.extensions

import net.kuama.documentscanner.utils.PointUtils
import org.junit.Assert
import org.junit.Test
import org.opencv.core.Point
import org.opencv.core.Size

class PointUtilsTest {
    @Test
    fun valid_input_returns_correct_result() {
        val points = listOf(
            Point(5.0, 5.0),
            Point(2.0, 5.0),
            Point(5.0, 2.0),
            Point(2.0, 2.0)
        )

        val size = Size(Point(3.0, 3.0))

        val result = PointUtils.getSortedCorners(points, size)

        Assert.assertEquals(Point(2.0, 5.0), result.topLeft)
        Assert.assertEquals(Point(5.0, 5.0), result.topRight)
        Assert.assertEquals(Point(2.0, 2.0), result.bottomLeft)
        Assert.assertEquals(Point(5.0, 2.0), result.bottomRight)

        Assert.assertEquals(size, result.size)
    }
}
