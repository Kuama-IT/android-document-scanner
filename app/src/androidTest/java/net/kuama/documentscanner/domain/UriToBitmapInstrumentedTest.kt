package net.kuama.documentscanner.domain

import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream

/*
 * NB: InputStream can only be consumed once, after which it will be disposed. If we want to check
 * the use-case against the original Bitmap we have to open two stream, one for the use-case and the
 * other to open the Bitmap.
 */

@RunWith(AndroidJUnit4::class)
class UriToBitmapInstrumentedTest {
    private val uriToBitmap: UriToBitmap = UriToBitmap()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uriToBitmap_valid_input_should_return_correct_result() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val inputStream: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("document-picture.jpg")

        val inputStream2: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("document-picture.jpg")

        val originalBitmap = BitmapFactory.decodeStream(inputStream2)

        val tempFile = File(appContext.filesDir, "temp-test.jpg")

        if (inputStream != null) {
            tempFile.writeBytes(inputStream.readBytes())
            runTest {
                uriToBitmap(
                    UriToBitmap.Params(
                        uri = tempFile.toUri(),
                        screenOrientationDeg = 0
                    )
                ) { either ->
                    either.fold({
                        fail("Exception thrown") }
                    ) { bitmap ->
                        assertEquals(originalBitmap.width, bitmap.width)
                        assertEquals(originalBitmap.height, bitmap.height)
                    }
                }
            }
        } else {
            fail("File not found")
        }

        tempFile.delete()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uriToBitmap_invalid_input_file_should_throw_NullPointerException() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val inputStream: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("file.txt")

        val tempFile = File(appContext.filesDir, "temp-test.jpg")

        if (inputStream != null) {
            tempFile.writeBytes(inputStream.readBytes())

            runTest {
                uriToBitmap(
                    UriToBitmap.Params(
                        tempFile.toUri(), 0)
                ) { either ->
                    either.fold({
                        assertThat(it.origin, instanceOf(NullPointerException::class.java))
                    }) { fail("Should throw exception") }
                }
            }
        } else {
            fail("File not found")
        }
    }
}
