package net.kuama.documentscanner.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class EFlashStatusTest {

    @Test
    fun empty_test_for_EFlashStatus() {
        val flashOn = enumValueOf<EFlashStatus>("ON")
        val flashOff = enumValueOf<EFlashStatus>("OFF")

        assertEquals(flashOn.ordinal, 0)
        assertEquals(flashOff.ordinal, 1)
    }
}
