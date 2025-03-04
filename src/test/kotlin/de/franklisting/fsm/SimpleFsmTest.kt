package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class SimpleFsmTest {
    object Tick : Event()

    private fun create(): Fsm<Int> {
        val fsm = FsmSync<Int>("simple traffic light")
        val showingRed = State<Int>("ShowingRed")
        val showingRedYellow = State<Int>("ShowingRedYellow")
        val showingYellow = State<Int>("ShowingYellow")
        val showingGreen = State<Int>("ShowingGreen")

        fsm.initialTransition(showingRed)
        showingRed
            .entry { println("x--    $it") }
            .transition(Tick, showingRedYellow)
        showingRedYellow
            .entry { println("xx-    $it") }
            .transition(Tick, showingGreen)
        showingGreen
            .entry { println("--x    $it") }
            .transition(Tick, showingYellow)
        showingYellow
            .entry { println("-x-    $it") }
            .transition(Tick, showingRed)

        return fsm
    }

    @Test
    fun `steps through the states`() {
        val fsm = create()

        fsm.start(1)
        assertThat(fsm.currentState.name).isEqualTo("ShowingRed")

        fsm.trigger(Tick, 1)
        assertThat(fsm.currentState.name).isEqualTo("ShowingRedYellow")

        fsm.trigger(Tick, 1)
        assertThat(fsm.currentState.name).isEqualTo("ShowingGreen")

        fsm.trigger(Tick, 1)
        assertThat(fsm.currentState.name).isEqualTo("ShowingYellow")

        fsm.trigger(Tick, 2)
        assertThat(fsm.currentState.name).isEqualTo("ShowingRed")

        fsm.trigger(Tick, 2)
        assertThat(fsm.currentState.name).isEqualTo("ShowingRedYellow")

        fsm.trigger(Tick, 2)
        assertThat(fsm.currentState.name).isEqualTo("ShowingGreen")

        fsm.trigger(Tick, 2)
        assertThat(fsm.currentState.name).isEqualTo("ShowingYellow")
    }
}
