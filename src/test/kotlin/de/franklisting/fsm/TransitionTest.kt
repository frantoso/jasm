package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class TransitionTest {
    @Test
    fun getTrigger() {
        val transition = Transition<Int>(StartEvent, TransitionEndPoint(State("abc"))) { i -> i < 20 }
        assertThat(transition.trigger).isEqualTo(StartEvent)
        assertThat(transition.isToFinal).isFalse
    }

    @Test
    fun isToFinal() {
        val transition = Transition<Int>(StartEvent, TransitionEndPoint(FinalState())) { i -> i < 20 }
        assertThat(transition.isToFinal).isTrue
    }

    @Test
    fun getCondition() {
        val transition = Transition<Int>(StartEvent, TransitionEndPoint(State("abc"))) { i -> i < 20 }
        assertThat(transition.guard(20)).isFalse
        assertThat(transition.guard(19)).isTrue
    }

    @Test
    fun getEndPoint() {
        val transition = Transition<Int>(StartEvent, TransitionEndPoint(State("abc"))) { i -> i < 20 }
        assertThat(transition.endPoint.state.name).isEqualTo("abc")
        assertThat(transition.endPoint.history.isHistory).isFalse
        assertThat(transition.endPoint.history.isDeepHistory).isFalse
    }
}
