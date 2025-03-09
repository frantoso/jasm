package de.franklisting.fsm

import de.franklisting.fsm.testutil.MultipleDiagramGenerator
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class NestedFsmTest {
    object Tick : Event()

    data class Parameters(
        var isDayMode: Boolean = true,
    )

    private val parameters = Parameters()

    class TrafficLight {
        val fsmMain = FsmSync<Parameters>("traffic light controller")
        val fsmDay = FsmSync<Parameters>("traffic light day mode")
        val fsmNight = FsmSync<Parameters>("traffic light night mode")

        init {
            setupFsmDayMode()
            setupFsmNightMode()

            val controllingDayMode = State<Parameters>("ControllingDayMode")
            val controllingNightMode = State<Parameters>("ControllingNightMode")

            fsmMain.initialTransition(controllingDayMode)
            controllingDayMode
                .entry { println("starting day mode    $it") }
                .transition(NoEvent, controllingNightMode)
                .child(fsmDay)
            controllingNightMode
                .entry { println("starting night mode    $it") }
                .transition(NoEvent, controllingDayMode)
                .child(fsmNight)
        }

        private fun setupFsmDayMode() {
            val showingRed = State<Parameters>("ShowingRed")
            val showingRedYellow = State<Parameters>("ShowingRedYellow")
            val showingYellow = State<Parameters>("ShowingYellow")
            val showingGreen = State<Parameters>("ShowingGreen")

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
            val showingNothing = State<Parameters>("ShowingNothing")
            val showingYellow = State<Parameters>("ShowingYellow")

            fsmNight.initialTransition(showingYellow)
            showingYellow
                .entry { println("x--    $it") }
                .transition(Tick, showingNothing) { !it.isDayMode }
                .transition(Tick, fsmNight.final) { it.isDayMode }
            showingNothing
                .entry { println("xx-    $it") }
                .transition(Tick, showingYellow)
        }
    }

    @Test
    fun `steps through the states`() {
        val trafficLight = TrafficLight()

        trafficLight.fsmMain.start(parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingRed")
        assertThat(trafficLight.fsmDay.isRunning).isTrue
        assertThat(trafficLight.fsmNight.isRunning).isFalse

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingRedYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingGreen")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingRed")

        parameters.isDayMode = false
        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingRedYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingGreen")
        assertThat(trafficLight.fsmDay.isRunning).isTrue
        assertThat(trafficLight.fsmNight.isRunning).isFalse

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmDay.currentState.name).isEqualTo("Final")
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("ShowingYellow")
        assertThat(trafficLight.fsmDay.isRunning).isFalse
        assertThat(trafficLight.fsmNight.isRunning).isTrue
    }

    @Test
    fun `generates a graph from the state machine`() {
        val generator = MultipleDiagramGenerator(TrafficLight().fsmMain)

        generator.toSvg("build/reports/traffic-light.svg")
        generator.toPng("build/reports/traffic-light.png")
    }
}
