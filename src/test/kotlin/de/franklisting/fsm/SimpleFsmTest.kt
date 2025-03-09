package de.franklisting.fsm

import de.franklisting.fsm.testutil.DiagramGenerator
import de.franklisting.fsm.testutil.TestData
import de.franklisting.fsm.testutil.testStateChange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test

class SimpleFsmTest {
    object Tick : Event()

    object OtherEvent : Event()

    private val trafficLight = TrafficLight()

    class TrafficLight {
        val fsm = FsmSync<Int>("simple traffic light")
        val showingRed = State<Int>("ShowingRed")
        val showingRedYellow = State<Int>("ShowingRedYellow")
        val showingYellow = State<Int>("ShowingYellow")
        val showingGreen = State<Int>("ShowingGreen")

        init {

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
        }
    }

    @BeforeEach
    fun setUp() {
        trafficLight.fsm.start(42)
    }

    @Test
    fun `steps through the states`() {
        val fsm = trafficLight.fsm

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

    @TestFactory
    @Suppress("ktlint")
    fun `changes to the right state (0-switch coverage)`() =
        // @formatter:off
            listOf(
                listOf(TestData(startState = trafficLight.showingRed, event = Tick, data = 1, endState = trafficLight.showingRedYellow, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingRedYellow, event = Tick, data = 1, endState = trafficLight.showingGreen, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingGreen, event = Tick, data = 1, endState = trafficLight.showingYellow, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingYellow, event = Tick, data = 1, endState = trafficLight.showingRed, wasHandled = true)),

                listOf(TestData(startState = trafficLight.showingRed, event = OtherEvent, data = 1, endState = trafficLight.showingRed, wasHandled = false)),
                // @formatter:on
            ).mapIndexed { index, data ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - changes from ${data.first().startState} to ${data.last().endState}") {
                    testStateChange(trafficLight.fsm, data)
                }
            }

    @TestFactory
    @Suppress("ktlint")
    fun `changes to the right state (1-switch coverage)`() =
        // @formatter:off
            listOf(
                listOf(TestData(startState = trafficLight.showingRed, event = Tick, data = 1, endState = trafficLight.showingRedYellow, wasHandled = true),
                       TestData(startState = trafficLight.showingRedYellow, event = Tick, data = 1, endState = trafficLight.showingGreen, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingRedYellow, event = Tick, data = 1, endState = trafficLight.showingGreen, wasHandled = true),
                       TestData(startState = trafficLight.showingGreen, event = Tick, data = 1, endState = trafficLight.showingYellow, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingGreen, event = Tick, data = 1, endState = trafficLight.showingYellow, wasHandled = true),
                       TestData(startState = trafficLight.showingYellow, event = Tick, data = 1, endState = trafficLight.showingRed, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingYellow, event = Tick, data = 1, endState = trafficLight.showingRed, wasHandled = true),
                       TestData(startState = trafficLight.showingRed, event = Tick, data = 1, endState = trafficLight.showingRedYellow, wasHandled = true)),
                // @formatter:on
            ).mapIndexed { index, data ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - changes from ${data.first().startState} to ${data.last().endState}") {
                    testStateChange(trafficLight.fsm, data)
                }
            }

    @Test
    fun `generates a graph from the state machine`() {
        val generator = DiagramGenerator(TrafficLight().fsm)

        generator.toSvg("build/reports/simple-traffic-light.svg")
        generator.toPng("build/reports/simple-traffic-light.png")
    }
}
