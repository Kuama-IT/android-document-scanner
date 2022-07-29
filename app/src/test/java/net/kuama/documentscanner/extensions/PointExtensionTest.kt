package net.kuama.documentscanner.extensions

import org.junit.Test
import org.opencv.core.Point

class PointExtensionTest {
    @Test
    fun it_calculate_distance_between_two_points() {
        val point1 = Point(-1.0, -3.0)
        val point2 = Point(5.0, -3.0)
        val expectedResult = 6.0

        val result = point1.distanceTo(point2)

        assert(result.equals(expectedResult))
    }
}
