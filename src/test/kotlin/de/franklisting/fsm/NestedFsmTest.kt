package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

data class TrafficLightParameters(
    var isDayMode: Boolean = true,
)

class NestedFsmTest {
    object Tick : Event()

    private val fsmMain = FsmSync<TrafficLightParameters>("traffic light controller")
    private val fsmDay = FsmSync<TrafficLightParameters>("traffic light day mode")
    private val fsmNight = FsmSync<TrafficLightParameters>("traffic light night mode")

    private val parameters = TrafficLightParameters()

    private fun setupFsmDayMode() {
        val showingRed = State<TrafficLightParameters>("ShowingRed")
        val showingRedYellow = State<TrafficLightParameters>("ShowingRedYellow")
        val showingYellow = State<TrafficLightParameters>("ShowingYellow")
        val showingGreen = State<TrafficLightParameters>("ShowingGreen")

        fsmDay.initialTransition(showingRed)
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
            .transition(Tick, showingRed) { it.isDayMode }
            .transition(Tick, fsmDay.final) { !it.isDayMode }
    }

    private fun setupFsmNightMode() {
        val showingNothing = State<TrafficLightParameters>("ShowingNothing")
        val showingYellow = State<TrafficLightParameters>("ShowingYellow")

        fsmNight.initialTransition(showingYellow)
        showingYellow
            .entry { println("x--    $it") }
            .transition(Tick, showingNothing) { !it.isDayMode }
            .transition(Tick, fsmNight.final) { it.isDayMode }
        showingNothing
            .entry { println("xx-    $it") }
            .transition(Tick, showingYellow)
    }

    private fun setupFsmController(): Fsm<TrafficLightParameters> {
        setupFsmDayMode()
        setupFsmNightMode()

        val controllingDayMode = State<TrafficLightParameters>("ControllingDayMode")
        val controllingNightMode = State<TrafficLightParameters>("ControllingNightMode")

        fsmMain.initialTransition(controllingDayMode)
        controllingDayMode
            .entry { println("starting day mode    $it") }
            .transition(NoEvent, controllingNightMode)
            .child(fsmDay)
        controllingNightMode
            .entry { println("starting night mode    $it") }
            .transition(NoEvent, controllingDayMode)
            .child(fsmNight)

        return fsmMain
    }

    @Test
    fun `steps through the states`() {
        setupFsmController()

        fsmMain.start(parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingRed")
        assertThat(fsmDay.isRunning).isTrue
        assertThat(fsmNight.isRunning).isFalse

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingRedYellow")

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingGreen")

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingYellow")

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingRed")

        parameters.isDayMode = false
        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingRedYellow")

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingGreen")
        assertThat(fsmDay.isRunning).isTrue
        assertThat(fsmNight.isRunning).isFalse

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("ShowingYellow")

        fsmMain.trigger(Tick, parameters)
        assertThat(fsmDay.currentState.name).isEqualTo("Final")
        assertThat(fsmNight.currentState.name).isEqualTo("ShowingYellow")
        assertThat(fsmDay.isRunning).isFalse
        assertThat(fsmNight.isRunning).isTrue
    }
}
