package net.kuama.documentscanner.domain

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.kuama.documentscanner.utils.TestUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class FindPaperSheetContoursInstrumentedTest {

    private val findPaperSheetUseCase: FindPaperSheetContours = FindPaperSheetContours()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findPaperSheetUseCase_valid_input_return_correct_result() {
        TestUtils.loadOpenCvLibrary()

        val inputStream: InputStream? =
            this.javaClass.classLoader?.getResourceAsStream("document-picture.jpg")

        val bitmap = BitmapFactory.decodeStream(inputStream)

        runTest {
            findPaperSheetUseCase(FindPaperSheetContours.Params(bitmap)) { resultingCorners ->
                if (resultingCorners != null) {

                    // TopLeft
                    assertEquals(770.0, resultingCorners.bottomLeft.x, 10.0)
                    assertEquals(2013.0, resultingCorners.bottomLeft.y, 10.0)

                    // TopRight
                    assertEquals(2093.0, resultingCorners.bottomRight.x, 10.0)
                    assertEquals(2015.0, resultingCorners.bottomRight.y, 10.0)

                    // BottomLeft
                    assertEquals(659.0, resultingCorners.topLeft.x, 10.0)
                    assertEquals(3427.0, resultingCorners.topLeft.y, 10.0)

                    // BottomRight
                    assertEquals(2107.0, resultingCorners.topRight.x, 10.0)
                    assertEquals(3414.0, resultingCorners.topRight.y, 10.0)
                } else {
                    fail("Corners were not found")
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findPaperSheetUseCase_empty_image_should_not_find_corners() {
        TestUtils.loadOpenCvLibrary()

        val inputStream: InputStream? =
            this.javaClass.classLoader?.getResourceAsStream("blank.png")

        val bitmap = BitmapFactory.decodeStream(inputStream)

        runTest {
            findPaperSheetUseCase(FindPaperSheetContours.Params(bitmap)) { resultingCorners ->
                assert(resultingCorners == null)
            }
        }
    }
}
