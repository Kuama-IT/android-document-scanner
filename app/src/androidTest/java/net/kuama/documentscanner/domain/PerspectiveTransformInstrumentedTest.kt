package net.kuama.documentscanner.domain

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.kuama.documentscanner.utils.TestUtils
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class PerspectiveTransformInstrumentedTest {
    private val perspectiveTransform: PerspectiveTransform = PerspectiveTransform()
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

                    runBlocking {
                        perspectiveTransform(
                            PerspectiveTransform.Params(
                                bitmap,
                                resultingCorners
                            )
                        ) { bitmap ->
                            // The resulting document should be a square, so height and width are
                            // comparable in size

                            assertEquals(1418.0, bitmap.height.toDouble(), 5.0)
                            assertEquals(1448.0, bitmap.width.toDouble(), 5.0)
                        }
                    }
                } else {
                    Assert.fail("Corners were not found")
                }
            }
        }
    }
}
