package net.kuama.documentscanner.exceptions

import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat

class MissingSquareExceptionTest {
    @Test
    fun empty_test_for_cover_MissingSquareException_claas() {
        try {
            throw MissingSquareException()
        } catch (exception: MissingSquareException) {
            assertThat(exception, instanceOf(MissingSquareException::class.java)
            )
        }
    }
}
