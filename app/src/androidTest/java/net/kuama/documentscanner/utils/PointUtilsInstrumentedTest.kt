package net.kuama.documentscanner.utils

import org.junit.Assert.assertEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.core.Point
import org.opencv.core.Size

@RunWith(AndroidJUnit4::class)
class PointUtilsInstrumentedTest {

    @Test
    fun getSortedCorners_valid_input_should_return_correct_results() {
        TestUtils.loadOpenCvLibrary()

        val points = listOf(
            Point(5.0, 5.0),
            Point(2.0, 5.0),
            Point(5.0, 2.0),
            Point(2.0, 2.0)
        )

        val size = Size(Point(3.0, 3.0))

        val result = PointUtils.getSortedCorners(points, size)

        assertEquals(Point(2.0, 5.0), result.topLeft)
        assertEquals(Point(5.0, 5.0), result.topRight)
        assertEquals(Point(2.0, 2.0), result.bottomLeft)
        assertEquals(Point(5.0, 2.0), result.bottomRight)

        assertEquals(size, result.size)
    }
}
