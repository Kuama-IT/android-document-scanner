package net.kuama.documentscanner.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.kuama.documentscanner.enums.EOpenCvStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class OpenCVLoaderInstrumentedTest {

    @Test
    fun it_should() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val openCVLoader = OpenCVLoader(appContext)

        openCVLoader.load { eOpenCvStatus ->
            assertEquals(eOpenCvStatus, EOpenCvStatus.LOADED)
        }
    }
}
