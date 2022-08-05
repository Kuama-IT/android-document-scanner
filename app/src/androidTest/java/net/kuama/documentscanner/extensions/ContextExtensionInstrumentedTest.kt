package net.kuama.documentscanner.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.kuama.documentscanner.R
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContextExtensionInstrumentedTest {

    @Test
    fun outputDirectory_name_should_be_equals_to_app_name() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val outputDir = context.outputDirectory()

        assertEquals(outputDir.name, context.resources.getString(R.string.app_name))
    }
}
