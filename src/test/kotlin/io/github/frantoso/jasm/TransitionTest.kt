package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class TransitionTest {
    @Test
    fun `initialization via standard constructor`() {
        val transition = Transition<Int>(StartEvent, TransitionEndPoint(State("abc"))) { i -> i < 20 }
        assertThat(transition.trigger).isEqualTo(StartEvent)
        assertThat(transition.endPoint.state.name).isEqualTo("abc")
    }

    @Test
    fun `initialization via alternative constructor`() {
        val transition = Transition<Int>(StartEvent, State("abc")) { i -> i < 20 }
        assertThat(transition.trigger).isEqualTo(StartEvent)
        assertThat(transition.endPoint.state.name).isEqualTo("abc")
    }

    @Test
    fun getTrigger() {
        val transition = Transition<Int>(StartEvent, State("abc")) { i -> i < 20 }
        assertThat(transition.trigger).isEqualTo(StartEvent)
        assertThat(transition.isToFinal).isFalse
    }

    @Test
    fun isToFinal() {
        val transition = Transition<Int>(StartEvent, FinalState()) { i -> i < 20 }
        assertThat(transition.isToFinal).isTrue
    }

    @Test
    fun getCondition() {
        val transition = Transition<Int>(StartEvent, State("abc")) { i -> i < 20 }
        assertThat(transition.guard(20)).isFalse
        assertThat(transition.guard(19)).isTrue
    }

    @Test
    fun getEndPoint() {
        val transition = Transition<Int>(StartEvent, State("abc")) { i -> i < 20 }
        assertThat(transition.endPoint.state.name).isEqualTo("abc")
        assertThat(transition.endPoint.history.isHistory).isFalse
        assertThat(transition.endPoint.history.isDeepHistory).isFalse
    }

    @Test
    fun `uses default construction with history`() {
        val transition = Transition<Int>(StartEvent, State("abc").history) { i -> i < 20 }
        assertThat(transition.endPoint.state.name).isEqualTo("abc")
        assertThat(transition.endPoint.history.isHistory).isTrue
        assertThat(transition.endPoint.history.isDeepHistory).isFalse
    }
}
