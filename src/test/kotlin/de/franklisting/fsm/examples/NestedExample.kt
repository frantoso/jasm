package de.franklisting.fsm.examples

import de.franklisting.fsm.Event
import de.franklisting.fsm.FinalState
import de.franklisting.fsm.FsmSync
import de.franklisting.fsm.NoEvent
import de.franklisting.fsm.State
import de.franklisting.fsm.fsmOf
import de.franklisting.fsm.testutil.MultipleDiagramGenerator
import de.franklisting.fsm.with
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class NestedExample {
    object Tick : Event()

    data class Parameters(
        var isDayMode: Boolean = true,
    )

    private val parameters = Parameters()

    class TrafficLight {
        val fsmMain: FsmSync<Parameters>
        val fsmDay: FsmSync<Parameters>
        val fsmNight: FsmSync<Parameters>

        init {
            fsmDay = createFsmDayMode()
            fsmNight = createFsmNightMode()

            val controllingDayMode = State("ControllingDayMode")
            val controllingNightMode = State("ControllingNightMode")

            fsmMain =
                fsmOf(
                    "traffic light controller",
                    controllingDayMode
                        .with<Parameters>()
                        .entry { println("starting day mode    $it") }
                        .transition(NoEvent, controllingNightMode)
                        .child(fsmDay),
                    controllingNightMode
                        .with<Parameters>()
                        .entry { println("starting night mode    $it") }
                        .transition(NoEvent, controllingDayMode)
                        .child(fsmNight),
                )
        }

        private fun createFsmDayMode(): FsmSync<Parameters> {
            val showingRed = State("ShowingRed")
            val showingRedYellow = State("ShowingRedYellow")
            val showingYellow = State("ShowingYellow")
            val showingGreen = State("ShowingGreen")

            return fsmOf(
                "traffic light day mode",
                showingRed
                    .with<Parameters>()
                    .entry { println("x--    $it") }
                    .transition(Tick, showingRedYellow),
                showingRedYellow
                    .with<Parameters>()
                    .entry { println("xx-    $it") }
                    .transition(Tick, showingGreen),
                showingGreen
                    .with<Parameters>()
                    .entry { println("--x    $it") }
                    .transition(Tick, showingYellow),
                showingYellow
                    .with<Parameters>()
                    .entry { println("-x-    $it") }
                    .transition(Tick, showingRed) { it.isDayMode }
                    .transition(Tick, FinalState()) { !it.isDayMode },
            )
        }

        private fun createFsmNightMode(): FsmSync<Parameters> {
            val showingNothing = State("ShowingNothing")
            val showingYellow = State("ShowingYellow")

            return fsmOf(
                "traffic light night mode",
                showingYellow
                    .with<Parameters>()
                    .entry { println("x--    $it") }
                    .transition(Tick, showingNothing) { !it.isDayMode }
                    .transition(Tick, FinalState()) { it.isDayMode },
                showingNothing
                    .with<Parameters>()
                    .entry { println("xx-    $it") }
                    .transition(Tick, showingYellow),
            )
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
