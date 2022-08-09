package net.kuama.documentscanner.domain

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import org.hamcrest.MatcherAssert.assertThat
import java.lang.NullPointerException

/*
 * NB: InputStream can only be consumed once, after which it will be disposed. If we want to check
 * the use-case against the original Bitmap we have to open two stream, one for the use-case and the
 * other to open the Bitmap.
 */

@RunWith(AndroidJUnit4::class)
class InputStreamToBitmapInstrumentedTest {
    private val uriToBitmap: InputStreamToBitmap = InputStreamToBitmap()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uriToBitmap_valid_input_should_return_correct_result() {
        val inputStream: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("document-picture.jpg")

        val inputStream2: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("document-picture.jpg")

        val originalBitmap = BitmapFactory.decodeStream(inputStream2)

        if (inputStream != null) {
            runTest {
                uriToBitmap(
                    InputStreamToBitmap.Params(
                        inputStream = inputStream,
                        screenOrientationDeg = 0
                    )
                ) { either ->
                    either.fold({
                        fail("Exception thrown")
                    }
                    ) { bitmap ->
                        assertEquals(originalBitmap.width, bitmap.width)
                        assertEquals(originalBitmap.height, bitmap.height)
                    }
                }
            }
        } else {
            fail("File not found")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uriToBitmap_valid_input_without_screen_orientation_should_return_correct_result() {
        val inputStream: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("document-picture.jpg")

        val inputStream2: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("document-picture.jpg")

        val originalBitmap = BitmapFactory.decodeStream(inputStream2)

        if (inputStream != null) {
            runTest {
                uriToBitmap(
                    InputStreamToBitmap.Params(
                        inputStream = inputStream
                    )
                ) { either ->
                    either.fold({
                        fail("Exception thrown")
                    }
                    ) { bitmap ->
                        assertEquals(originalBitmap.width, bitmap.width)
                        assertEquals(originalBitmap.height, bitmap.height)
                    }
                }
            }
        } else {
            fail("File not found")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uriToBitmap_invalid_input_file_should_throw_NullPointerException() {
        val inputStream: InputStream? = this.javaClass.classLoader
            ?.getResourceAsStream("file.txt")

        if (inputStream != null) {
            runTest {
                uriToBitmap(
                    InputStreamToBitmap.Params(
                        inputStream = inputStream
                    )
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
