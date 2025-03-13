package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ChangeStateDataTest {
    @Test
    fun getHandled1() {
        val data = ChangeStateData(true)
        assertThat(data.handled).isTrue
        assertThat(data.endPoint).isNull()
    }

    @Test
    fun getHandled2() {
        val data = ChangeStateData(false)
        assertThat(data.handled).isFalse
        assertThat(data.endPoint).isNull()
    }

    @Test
    fun getHandled3() {
        val endPoint = TransitionEndPoint(FinalState())
        val data = ChangeStateData(true, endPoint)
        assertThat(data.handled).isTrue
        assertThat(data.endPoint).isSameAs(endPoint)
    }
}
