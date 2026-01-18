package io.github.frantoso.jasm

import io.github.frantoso.jasm.model.StateInfo
import io.github.frantoso.jasm.model.TransitionInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class ExtractorTests {
    private val owner = "the-owner"

    // --------------------------------------------------------------------
    // TransitionConvertTestData
    // --------------------------------------------------------------------
    @TestFactory
    fun convertTransitionTests() =
        listOf(
            Triple(TransitionEndPoint(State("State")), Triple(false, false, false), "Normal"),
            Triple(State("State").history, Triple(true, false, false), "History"),
            Triple(State("State").deepHistory, Triple(false, true, false), "DeepHistory"),
            Triple(TransitionEndPoint(FinalState()), Triple(false, false, true), "Final"),
        ).map { (state, expected, name) ->

            DynamicTest.dynamicTest("ConvertTransitionTest - $name") {
                val (isHistory, isDeepHistory, isFinal) = expected

                val transition = Transition(TestEvent::class, state) { true }
                val info = transition.convert(owner)

                assertThat(info.endPointId).isEqualTo(state.state.normalizedId(owner))
                assertThat(info.isHistory).isEqualTo(isHistory)
                assertThat(info.isDeepHistory).isEqualTo(isDeepHistory)
                assertThat(info.isToFinal).isEqualTo(isFinal)
            }
        }

    data class TestData(
        val init: List<Pair<Boolean, Boolean>>,
        val expected: Pair<Boolean, Boolean>,
    )

    @TestFactory
    fun updateStateInfoTests() =
        listOf(
            TestData(listOf(false to false), false to false),
            TestData(listOf(true to false), true to false),
            TestData(listOf(false to true), false to true),
            TestData(listOf(false to false, false to false), false to false),
            TestData(listOf(true to false, false to false), true to false),
            TestData(listOf(false to false, true to false), true to false),
            TestData(listOf(false to true, false to false), false to true),
            TestData(listOf(false to false, false to true), false to true),
            TestData(listOf(true to false, true to false), true to false),
            TestData(listOf(true to false, true to false), true to false),
            TestData(listOf(true to false, false to true), true to true),
            TestData(listOf(false to true, true to false), true to true),
            TestData(listOf(true to true, true to true), true to true),
        ).mapIndexed { index, (initData, expected) ->

            DynamicTest.dynamicTest("UpdateStateInfoTest #$index") {
                val (expectedHistory, expectedDeepHistory) = expected

                val stateId = "State_01"
                val state =
                    StateInfo(
                        id = stateId,
                        name = "State",
                        isInitial = false,
                        isFinal = false,
                        transitions = mutableListOf(),
                        children = mutableListOf(),
                        hasHistory = false,
                        hasDeepHistory = false,
                    )

                val transitions =
                    mutableListOf(
                        TransitionInfo("other", isHistory = false, isDeepHistory = true, isToFinal = false),
                    )

                initData.forEach { (h, dh) ->
                    transitions += TransitionInfo(stateId, h, dh, false)
                }

                val updated = state.update(transitions)

                assertThat(updated.hasHistory).isEqualTo(expectedHistory)
                assertThat(updated.hasDeepHistory).isEqualTo(expectedDeepHistory)
            }
        }

    // --------------------------------------------------------------------
    // NormalizedId Tests
    // --------------------------------------------------------------------
    @Test
    fun normalizedIdTestForNormalState() {
        val state = State("state")
        val normalized = state.normalizedId("owner")

        assertThat(normalized).isEqualTo(state.id)
    }

    @Test
    fun normalizedIdForFinalState() {
        val final = FinalState()
        val normalized = final.normalizedId(owner)

        assertThat(normalized).isEqualTo("$owner-${FINAL_STATE_ID}")
    }

    // --------------------------------------------------------------------
    // Convert State Tests
    // --------------------------------------------------------------------
    @Test
    fun convertStateForNormalState() {
        val container =
            State("S1")
                .transition<TestEvent>(FinalState())
                .transition<TestEvent>(State("S2"))

        val info = container.convert(owner)

        assertAll(
            { assertThat(info.id).isEqualTo(container.state.normalizedId(owner)) },
            { assertThat(info.name).isEqualTo(container.state.name) },
            { assertThat(info.isInitial).isFalse() },
            { assertThat(info.isFinal).isFalse() },
            { assertThat(info.transitions).hasSize(2) },
            { assertThat(info.transitions[0].isToFinal).isTrue() },
            { assertThat(info.transitions[1].isToFinal).isFalse() },
        )
    }

    @Test
    fun convertStateForInitialState() {
        val container = InitialStateContainer(InitialState(), emptyList())
        val info = container.convert(owner)

        assertThat(info.isInitial).isTrue()
        assertThat(info.isFinal).isFalse()
    }

    @Test
    fun convertStateForFinalState() {
        val container = FinalStateContainer(FinalState())
        val info = container.convert(owner)

        assertThat(info.isInitial).isFalse()
        assertThat(info.isFinal).isTrue()
    }

    // --------------------------------------------------------------------
    // Convert FSM
    // --------------------------------------------------------------------
    @Test
    fun convertFsmTest() {
        val start = State("Start").transitionToFinal<TestEvent>()
        val fsm = fsmOf("MyFsm", start)

        val info = fsm.convert()

        assertAll(
            { assertThat(info.name).isEqualTo("MyFsm") },
            { assertThat(info.states.any { it.isInitial }).isTrue() },
            { assertThat(info.states.flatMap { it.transitions }.any { it.isToFinal }).isTrue() },
        )
    }

    // --------------------------------------------------------------------
    // AllMachines Test
    // --------------------------------------------------------------------
    @Test
    fun allMachinesTest() {
        val child = fsmOf("ChildFsm", State("CStart").toContainer())
        val parent =
            fsmOf(
                "ParentFsm",
                State("PStart").child(child),
            )

        val all = parent.allMachines()

        assertAll(
            { assertThat(all.any { it.name == "ParentFsm" }).isTrue() },
            { assertThat(all.any { it.name == "ChildFsm" }).isTrue() },
            { assertThat(all.size).isGreaterThanOrEqualTo(2) },
        )
    }

    // --------------------------------------------------------------------
    // TestEvent
    // --------------------------------------------------------------------
    private class TestEvent : Event()
}
