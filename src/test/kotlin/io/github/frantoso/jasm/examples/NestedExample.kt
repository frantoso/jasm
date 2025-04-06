package io.github.frantoso.jasm.examples

import io.github.frantoso.jasm.CompositeState
import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.FsmSync
import io.github.frantoso.jasm.NoEvent
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.entry
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.testutil.MultipleDiagramGenerator
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class NestedExample {
    object Tick : Event()

    data class Parameters(
        var isDayMode: Boolean = true,
    )

    private val parameters = Parameters()

    class FsmDayMode : CompositeState("ControllingDayMode") {
        private val showingRed = State("ShowingRed")
        private val showingRedYellow = State("ShowingRedYellow")
        private val showingYellow = State("ShowingYellow")
        private val showingGreen = State("ShowingGreen")

        override val subMachine =
            fsmOf(
                "traffic light day mode",
                showingRed
                    .entry<Parameters> { println("x--    $it") }
                    .transition<Tick>(showingRedYellow),
                showingRedYellow
                    .entry<Parameters> { println("xx-    $it") }
                    .transition<Tick>(showingGreen),
                showingGreen
                    .entry<Parameters> { println("--x    $it") }
                    .transition<Tick>(showingYellow),
                showingYellow
                    .entry<Parameters> { println("-x-    $it") }
                    .transition<Tick, Parameters>(showingRed) { it!!.isDayMode }
                    .transition<Tick, Parameters>(FinalState()) { !it!!.isDayMode },
            )
    }

    class TrafficLight {
        val fsmMain: FsmSync
        val fsmNight: FsmSync
        val controllingDayMode = FsmDayMode()

        init {
            fsmNight = createFsmNightMode()

            val controllingNightMode = State("ControllingNightMode")

            fsmMain =
                fsmOf(
                    "traffic light controller",
                    controllingDayMode
                        .entry<Parameters> { println("starting day mode    $it") }
                        .transition<NoEvent>(controllingNightMode),
                    controllingNightMode
                        .entry<Parameters> { println("starting night mode    $it") }
                        .transition<NoEvent>(controllingDayMode)
                        .child(fsmNight),
                )
        }

        private fun createFsmNightMode(): FsmSync {
            val showingNothing = State("ShowingNothing")
            val showingYellow = State("ShowingYellow")

            return fsmOf(
                "traffic light night mode",
                showingYellow
                    .entry<Parameters> { println("x--    $it") }
                    .transition<Tick, Parameters>(showingNothing) { !it!!.isDayMode }
                    .transition<Tick, Parameters>(FinalState()) { it!!.isDayMode },
                showingNothing
                    .entry<Parameters> { println("xx-    $it") }
                    .transition<Tick>(showingYellow),
            )
        }
    }

    @Test
    fun `steps through the states`() {
        val trafficLight = TrafficLight()

        trafficLight.fsmMain.start()
        assertThat(trafficLight.fsmMain.currentState.name).isEqualTo("ControllingDayMode")
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingRed")
        assertThat(trafficLight.controllingDayMode.subMachine.isRunning).isTrue
        assertThat(trafficLight.fsmNight.isRunning).isFalse

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingRedYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingGreen")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingRed")

        parameters.isDayMode = false
        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingRedYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingGreen")
        assertThat(trafficLight.controllingDayMode.subMachine.isRunning).isTrue
        assertThat(trafficLight.fsmNight.isRunning).isFalse

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmMain.currentState.name).isEqualTo("ControllingDayMode")
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmMain.currentState.name).isEqualTo("ControllingNightMode")
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("Final")
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("ShowingYellow")
        assertThat(trafficLight.controllingDayMode.subMachine.isRunning).isFalse
        assertThat(trafficLight.fsmNight.isRunning).isTrue

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("ShowingNothing")
        assertThat(trafficLight.controllingDayMode.subMachine.isRunning).isFalse
        assertThat(trafficLight.fsmNight.isRunning).isTrue

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("ShowingNothing")

        parameters.isDayMode = true
        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmMain.currentState.name).isEqualTo("ControllingNightMode")
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("ShowingYellow")
        assertThat(trafficLight.controllingDayMode.subMachine.isRunning).isFalse
        assertThat(trafficLight.fsmNight.isRunning).isTrue

        trafficLight.fsmMain.trigger(Tick, parameters)
        assertThat(trafficLight.fsmMain.currentState.name).isEqualTo("ControllingDayMode")
        assertThat(trafficLight.controllingDayMode.subMachine.currentState.name).isEqualTo("ShowingRed")
        assertThat(trafficLight.fsmNight.currentState.name).isEqualTo("Final")
        assertThat(trafficLight.controllingDayMode.subMachine.isRunning).isTrue
        assertThat(trafficLight.fsmNight.isRunning).isFalse
    }

    @Test
    fun `generates a graph from the state machine`() {
        val generator = MultipleDiagramGenerator(TrafficLight().fsmMain)

        generator.toSvg("build/reports/traffic-light.svg", 1000)
        generator.toPng("build/reports/traffic-light.png", 1000)
    }
}
