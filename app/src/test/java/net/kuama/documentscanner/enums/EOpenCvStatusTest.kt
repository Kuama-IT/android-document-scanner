package net.kuama.documentscanner.enums

import org.junit.Assert
import org.junit.Test

class EOpenCvStatusTest {

    @Test
    fun empty_test_for_EOpenCvStatus() {
        val loaded = enumValueOf<EOpenCvStatus>("LOADED")
        val error = enumValueOf<EOpenCvStatus>("ERROR")

        Assert.assertEquals(loaded.ordinal, 0)
        Assert.assertEquals(error.ordinal, 1)
    }
}
