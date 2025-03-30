package io.github.frantoso.jasm.examples

import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FsmSync
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.dataEvent
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.testutil.DiagramGenerator
import io.github.frantoso.jasm.testutil.TestData
import io.github.frantoso.jasm.testutil.testStateChange
import io.github.frantoso.jasm.with
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test

class SimpleExample {
    object Tick : Event()

    object OtherEvent : Event()

    private val trafficLight = TrafficLight()

    class TrafficLight {
        val fsm: FsmSync
        val showingRed = State("ShowingRed")
        val showingRedYellow = State("ShowingRedYellow")
        val showingYellow = State("ShowingYellow")
        val showingGreen = State("ShowingGreen")

        init {

            fsm =
                fsmOf(
                    "simple traffic light",
                    showingRed
                        .with()
                        .entry<Int> { println("x--    $it") }
                        .transition<Tick>(showingRedYellow),
                    showingRedYellow
                        .with()
                        .entry<Int> { println("xx-    $it") }
                        .transition<Tick>(showingGreen),
                    showingGreen
                        .with()
                        .entry<Int> { println("--x    $it") }
                        .transition<Tick>(showingYellow),
                    showingYellow
                        .with()
                        .entry { println("-x-    ry") }
                        .transition<Tick>(showingRed),
                )
        }
    }

    @BeforeEach
    fun setUp() {
        trafficLight.fsm.start(42)
    }

    @Test
    fun `steps through the states`() {
        val fsm = trafficLight.fsm

        fsm.start()
        assertThat(fsm.currentState.name).isEqualTo("ShowingRed")

        fsm.trigger(dataEvent<Tick, Int>(1))
        assertThat(fsm.currentState.name).isEqualTo("ShowingRedYellow")

        fsm.trigger(Tick)
        assertThat(fsm.currentState.name).isEqualTo("ShowingGreen")

        fsm.trigger(dataEvent<Tick, Int>(3))
        assertThat(fsm.currentState.name).isEqualTo("ShowingYellow")

        fsm.trigger(Tick)
        assertThat(fsm.currentState.name).isEqualTo("ShowingRed")

        fsm.trigger(Tick)
        assertThat(fsm.currentState.name).isEqualTo("ShowingRedYellow")

        fsm.trigger(Tick)
        assertThat(fsm.currentState.name).isEqualTo("ShowingGreen")

        fsm.trigger(Tick)
        assertThat(fsm.currentState.name).isEqualTo("ShowingYellow")
    }

    @TestFactory
    @Suppress("ktlint")
    fun `changes to the right state (0-switch coverage)`() =
        // @formatter:off
            listOf(
                listOf(TestData(startState = trafficLight.showingRed, event = Tick,  endState = trafficLight.showingRedYellow, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingRedYellow, event = Tick, endState = trafficLight.showingGreen, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingGreen, event = Tick, endState = trafficLight.showingYellow, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingYellow, event = Tick, endState = trafficLight.showingRed, wasHandled = true)),

                listOf(TestData(startState = trafficLight.showingRed, event = OtherEvent, endState = trafficLight.showingRed, wasHandled = false)),
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
                listOf(TestData(startState = trafficLight.showingRed, event = Tick, endState = trafficLight.showingRedYellow, wasHandled = true),
                       TestData(startState = trafficLight.showingRedYellow, event = Tick, endState = trafficLight.showingGreen, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingRedYellow, event = Tick, endState = trafficLight.showingGreen, wasHandled = true),
                       TestData(startState = trafficLight.showingGreen, event = Tick, endState = trafficLight.showingYellow, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingGreen, event = Tick, endState = trafficLight.showingYellow, wasHandled = true),
                       TestData(startState = trafficLight.showingYellow, event = Tick, endState = trafficLight.showingRed, wasHandled = true)),
                listOf(TestData(startState = trafficLight.showingYellow, event = Tick, endState = trafficLight.showingRed, wasHandled = true),
                       TestData(startState = trafficLight.showingRed, event = Tick, endState = trafficLight.showingRedYellow, wasHandled = true)),
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
