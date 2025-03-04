package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import javax.naming.InvalidNameException

class FsmExceptionTest {
    private val testException = InvalidNameException()

    class Expected(
        val message: String?,
        val stateName: String,
        val cause: Throwable?,
    )

    @TestFactory
    fun `initialization of NoSuchPrefixException`() =
        listOf(
            FsmException() to
                Expected(null, "", null),
            FsmException(message = "test") to
                Expected("test", "", null),
            FsmException(cause = testException) to
                Expected(null, "", testException),
            FsmException(message = "test", cause = testException) to
                Expected("test", "", testException),
            FsmException(message = "test", stateName = "doing", cause = testException) to
                Expected("test", "doing", testException),
        ).mapIndexed { index, (input, expected) ->
            DynamicTest.dynamicTest("${"%02d".format(index)} expected result: $expected") {
                assertThat(input.message).isEqualTo(expected.message)
                assertThat(input.stateName).isEqualTo(expected.stateName)
                assertThat(input.cause).isEqualTo(expected.cause)
            }
        }
}
