package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ChangeStateDataTest {
    @Test
    fun getHandled1() {
        val data = ChangeStateData<Int>(true)
        assertThat(data.handled).isTrue
        assertThat(data.endPoint.state.isInvalid).isTrue
    }

    @Test
    fun getHandled2() {
        val data = ChangeStateData<Int>(false)
        assertThat(data.handled).isFalse
        assertThat(data.endPoint.state.isInvalid).isTrue
    }

    @Test
    fun getHandled3() {
        val endPoint = TransitionEndPoint<Int>(FinalState())
        val data = ChangeStateData(true, endPoint)
        assertThat(data.handled).isTrue
        assertThat(data.endPoint).isSameAs(endPoint)
    }
}
