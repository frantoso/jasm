package io.github.frantoso.jasm.examples

import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.FsmSync
import io.github.frantoso.jasm.NoEvent
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.testutil.MultipleDiagramGenerator
import io.github.frantoso.jasm.with
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class NestedExample {
    object Tick : Event()

    data class Parameters(
        var isDayMode: Boolean = true,
    )

    private val parameters = Parameters()

    class TrafficLight {
        val fsmMain: FsmSync
        val fsmDay: FsmSync
        val fsmNight: FsmSync

        init {
            fsmDay = createFsmDayMode()
            fsmNight = createFsmNightMode()

            val controllingDayMode = State("ControllingDayMode")
            val controllingNightMode = State("ControllingNightMode")

            fsmMain =
                fsmOf(
                    "traffic light controller",
                    controllingDayMode
                        .with()
                        .entry<Parameters> { println("starting day mode    $it") }
                        .transition<NoEvent, Parameters>(controllingNightMode)
                        .child(fsmDay),
                    controllingNightMode
                        .with()
                        .entry<Parameters> { println("starting night mode    $it") }
                        .transition<NoEvent, Parameters>(controllingDayMode)
                        .child(fsmNight),
                )
        }

        private fun createFsmDayMode(): FsmSync {
            val showingRed = State("ShowingRed")
            val showingRedYellow = State("ShowingRedYellow")
            val showingYellow = State("ShowingYellow")
            val showingGreen = State("ShowingGreen")

            return fsmOf(
                "traffic light day mode",
                showingRed
                    .with()
                    .entry<Parameters> { println("x--    $it") }
                    .transition<Tick, Parameters>(showingRedYellow),
                showingRedYellow
                    .with()
                    .entry<Parameters> { println("xx-    $it") }
                    .transition<Tick>(showingGreen),
                showingGreen
                    .with()
                    .entry<Parameters> { println("--x    $it") }
                    .transition<Tick>(showingYellow),
                showingYellow
                    .with()
                    .entry<Parameters> { println("-x-    $it") }
                    .transition<Tick, Parameters>(showingRed) { it!!.isDayMode }
                    .transition<Tick, Parameters>(FinalState()) { !it!!.isDayMode },
            )
        }

        private fun createFsmNightMode(): FsmSync {
            val showingNothing = State("ShowingNothing")
            val showingYellow = State("ShowingYellow")

            return fsmOf(
                "traffic light night mode",
                showingYellow
                    .with()
                    .entry<Parameters> { println("x--    $it") }
                    .transition<Tick, Parameters>(showingNothing) { !it!!.isDayMode }
                    .transition<Tick, Parameters>(FinalState()) { it!!.isDayMode },
                showingNothing
                    .with()
                    .entry<Parameters> { println("xx-    $it") }
                    .transition<Tick>(showingYellow),
            )
        }
    }

    @Test
    fun `steps through the states`() {
        val trafficLight = TrafficLight()

        trafficLight.fsmMain.start()
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

        generator.toSvg("build/reports/traffic-light.svg", 1000)
        generator.toPng("build/reports/traffic-light.png", 1000)
    }
}
