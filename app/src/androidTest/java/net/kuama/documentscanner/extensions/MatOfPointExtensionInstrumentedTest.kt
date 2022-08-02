package net.kuama.documentscanner.extensions

import org.junit.Assert.assertEquals

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.kuama.documentscanner.utils.TestUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.core.MatOfPoint
import org.opencv.core.Point

@RunWith(AndroidJUnit4::class)
class MatOfPointExtensionInstrumentedTest {
    @Test
    fun shape_square_shape_should_be_a_square() {
        TestUtils.loadOpenCvLibrary()

        val matOfPoint = MatOfPoint(
            Point(0.0, 0.0),
            Point(0.5, 0.0),
            Point(1.0, 0.0),
            Point(1.0, 0.5),
            Point(1.0, 1.0),
            Point(0.0, 1.0),
            Point(0.0, 0.5)

        )

        val shape = matOfPoint.shape
        val expected = listOf(
            Point(0.0, 0.0),
            Point(1.0, 0.0),
            Point(1.0, 1.0),
            Point(0.0, 1.0))

        assertEquals(expected, shape)
    }
}
