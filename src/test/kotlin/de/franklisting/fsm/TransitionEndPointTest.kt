package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class TransitionEndPointTest {
    @Test
    fun getState1Parameter() {
        val endPoint = TransitionEndPoint(State<Int>("xyz"))
        assertThat(endPoint.state.name).isEqualTo("xyz")
        assertThat(endPoint.history).isSameAs(History.None)
    }

    @Test
    fun getState2Parameter() {
        val endPoint = TransitionEndPoint(State<Int>("xyz"), History.H)
        assertThat(endPoint.state.name).isEqualTo("xyz")
        assertThat(endPoint.history).isSameAs(History.H)
    }
}
