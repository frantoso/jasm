package io.github.frantoso.jasm.doc

import com.jillesvangurp.kotlin4example.Page
import com.jillesvangurp.kotlin4example.SourceRepository
import io.github.frantoso.jasm.CompositeState
import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.FsmAsync
import io.github.frantoso.jasm.FsmSync
import io.github.frantoso.jasm.NoEvent
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.child
import io.github.frantoso.jasm.dataEvent
import io.github.frantoso.jasm.entry
import io.github.frantoso.jasm.fsmAsyncOf
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.testutil.DiagramGenerator
import io.github.frantoso.jasm.testutil.MultipleDiagramGenerator
import io.github.frantoso.jasm.transition
import io.github.frantoso.jasm.transitionToFinal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

/**
 * The README.md is generated when the tests run.
 */
@Suppress("ktlint:standard:unary-op-spacing")
class ReadmeGenerator {
    private val fsmFileName = "images/traffic_light_simple.svg"
    private val nestedFsmFileName = "images/traffic_light_nested.svg"

    @Test
    fun `test a simple state machine`() {
        // BEGIN_FSM_CODE_SNIPPET

        // create the states...
        val showingRed = State("ShowingRed")
        val showingRedYellow = State("ShowingRedYellow")
        val showingYellow = State("ShowingYellow")
        val showingGreen = State("ShowingGreen")

        // create the state machine...
        val fsm =
            fsmOf(
                "simple traffic light",
                // define initial state with transitions and other parameters...
                showingRed
                    .entry { println("x--") } // add an entry function
                    .transition<Tick>(showingRedYellow), // add one or more transitions
                // define other states with transitions and other parameters...
                showingRedYellow
                    .entry { println("xx-") }
                    .transition<Tick>(showingGreen),
                showingGreen
                    .entry { println("--x") }
                    .transition<Tick>(showingYellow),
                showingYellow
                    .entry { println("-x-") }
                    .transition<Tick>(showingRed),
            )

        // start the state machine
        fsm.start()

        assertThat(fsm.isRunning).isTrue

        // trigger an event
        fsm.trigger(Tick)

        assertThat(fsm.currentState).isEqualTo(showingRedYellow)
        // END_FSM_CODE_SNIPPET

        // generate diagram picture - only for the README
        DiagramGenerator(fsm).toSvg(fsmFileName)
    }

    // BEGIN_EVENT_DEF_CODE_SNIPPET
    object Event1 : Event()

    object Event2 : Event()
    // END_EVENT_DEF_CODE_SNIPPET

    object Tick : Event()

    // BEGIN_COMPOSITE_STATE_DEF_CODE_SNIPPET
    class ControllingDayMode : CompositeState() {
        private val showingRed = State("ShowingRed")
        private val showingRedYellow = State("ShowingRedYellow")
        private val showingYellow = State("ShowingYellow")
        private val showingGreen = State("ShowingGreen")

        override val subMachines =
            listOf(
                fsmOf(
                    name,
                    showingRed
                        .transition<Tick>(showingRedYellow),
                    showingRedYellow
                        .transition<Tick>(showingGreen),
                    showingGreen
                        .transition<Tick>(showingYellow),
                    showingYellow
                        .transition<Tick, Boolean>(showingRed) { it!! }
                        .transition<Tick, Boolean>(FinalState()) { !it!! },
                ),
            )
    }
    // END_COMPOSITE_STATE_DEF_CODE_SNIPPET

    @Suppress("KotlinConstantConditions")
    @Test
    fun `composite states`() {
        // BEGIN_COMPOSITE_STATE_MANUALLY_CODE_SNIPPET
        val showingNothing = State("ShowingNothing")
        val showingYellow = State("ShowingYellow")

        val fsmNight =
            fsmOf(
                "ControllingNightMode",
                showingYellow
                    .transition<Tick, Boolean>(showingNothing) { !it!! }
                    .transition<Tick, Boolean>(FinalState()) { it!! },
                showingNothing
                    .transition<Tick>(showingYellow),
            )
        // END_COMPOSITE_STATE_MANUALLY_CODE_SNIPPET

        // BEGIN_USE_COMPOSITE_STATE_CODE_SNIPPET
        val controllingDayMode = ControllingDayMode()
        val controllingNightMode = State("ControllingNightMode")

        val trafficLight =
            fsmOf(
                "TrafficLight",
                controllingDayMode // is a composite state - child is added automatically
                    .transition<NoEvent>(controllingNightMode),
                controllingNightMode // normal state to use as a composite state
                    .child(fsmNight) // child must be added manually
                    .transition<NoEvent>(controllingDayMode),
            )
        // END_USE_COMPOSITE_STATE_CODE_SNIPPET

        var isDayMode = true

        trafficLight.start()
        assertThat(trafficLight.currentState.name).isEqualTo("ControllingDayMode")
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingRed")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingRedYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingGreen")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingRed")

        isDayMode = false
        trafficLight.trigger(Tick, isDayMode)
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingRedYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingGreen")
        assertThat(controllingDayMode.subMachines[0].isRunning).isTrue
        assertThat(fsmNight.isRunning).isFalse

        trafficLight.trigger(Tick, isDayMode)
        assertThat(trafficLight.currentState.name).isEqualTo("ControllingDayMode")
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(trafficLight.currentState.name).isEqualTo("ControllingNightMode")
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("Final")
        assertThat(fsmNight.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(fsmNight.currentState.name).isEqualTo("ShowingNothing")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(fsmNight.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(fsmNight.currentState.name).isEqualTo("ShowingNothing")

        isDayMode = true
        trafficLight.trigger(Tick, isDayMode)
        assertThat(trafficLight.currentState.name).isEqualTo("ControllingNightMode")
        assertThat(fsmNight.currentState.name).isEqualTo("ShowingYellow")

        trafficLight.trigger(Tick, isDayMode)
        assertThat(trafficLight.currentState.name).isEqualTo("ControllingDayMode")
        assertThat(controllingDayMode.subMachines[0].currentState.name).isEqualTo("ShowingRed")
        assertThat(fsmNight.currentState.name).isEqualTo("Final")

        // generate diagram picture - only for the README
        MultipleDiagramGenerator(trafficLight).toSvg(nestedFsmFileName, 1000)
    }

    @Test
    fun `generate readme for this project`() {
        val k4ERepo =
            SourceRepository(
                repoUrl = "https://github.com/frantoso/jasm",
                branch = "main",
                sourcePaths =
                    setOf(
                        "src/main/kotlin",
                        "src/test/kotlin",
                    ),
            )

        val readmeMarkdown =
            k4ERepo.md {
                section("Introduction")
                +
                    """
                    This is an easy-to-use state machine implementation for Kotlin.

                    There are a lot of variants known to implement state machines.
                    Most of them merge the code of the state machines behavior together with the functional code.
                    A better solution is to strictly separate the code of the state machines logic from
                    the functional code. An existing state machine component is parametrized to define its behavior.

                    This readme includes the documentation of an implementation of a ready to use FSM (Finite State Machine).
                    Using this state machine is very simple. Just define the states, the transitions and the state actions.
                    """.trimIndent()

                section("Basic steps to create a State Machine")
                +
                    """
                    1. Model your state machine inside a graphical editor e.g. UML tool or any other applicable graphic tool.
                    2. Create all states in your code.
                    3. Transfer all your transitions from the graphic to the code.
                    4. Register the action handlers for your states.
                    5. Start the state machine.
                    """.trimIndent()

                section("How to ...")
                +
                    """
                    - [Get it.](#gradle)
                    - [Implementing a simple Finite State Machine.](#how-to-create-a-simple-state-machine)
                    - [The Classes.](#the-classes)
                    - [Synchronous vs Asynchronous.](#synchronous-vs-asynchronous)
                    - [Composite States.](#composite-states)
                    """.trimIndent()

                subSection("Gradle")
                +
                    """
                    The library jasm is distributed via MavenCentral.

                    **build.gradle.kts**
                    """.trimIndent()
                mdCodeBlock(
                    code =
                        """
                        repositories {
                            mavenCentral()
                        }

                        dependencies {
                            implementation("io.github.frantoso:jasm:<version>")
                        }
                        """.trimIndent(),
                    type = "kotlin",
                )

                section("How to: Create a simple State Machine")
                +
                    """
                    This topic shows how to implement a simple Finite State Machine using the jasm component.
                    The example shows the modeling of a single traffic light.
                    """.trimIndent()
                subSection("Start with the model of the state machine")
                +
                    """
                    ![Simple state machine]($fsmFileName)
                    
                    *A simple traffic light with four states, starting with showing the red light.*
                    """.trimIndent()
                subSection("Create the state machine and the states")
                exampleFromSnippet(
                    sourceFileName = "src/test/kotlin/io/github/frantoso/jasm/doc/ReadmeGenerator.kt",
                    snippetId = "FSM_CODE_SNIPPET",
                )

                section("The classes")
                subSection("FsmSync")
                +
                    """
                    A synchronous (blocking) state machine. The call to trigger is blocking.
                    """.trimIndent()
                example {
                    val state = State("MyState")
                    val fsm =
                        fsmOf(
                            "MyFsm",
                            // add at minimum one state
                            state
                                .transitionToFinal<Tick>(),
                        )

                    fsm.start()
                }

                subSection("FsmAsync")
                +
                    """
                    An asynchronous (non-blocking) state machine. The call to trigger is non-blocking. The events are
                    queued and triggered sequentially.
                    """.trimIndent()
                example {
                    val state = State("MyState")
                    val fsm =
                        fsmAsyncOf(
                            "MyFsm",
                            // add at minimum one state
                            state
                                .transitionToFinal<Tick>(),
                        )

                    fsm.start()
                }

                section("Synchronous vs Asynchronous")
                +
                    """
                    A function calling trigger() on a synchronous state machine waits until all entry and exit functions
                    are executed and the transition table was processed. After the trigger() function is returned,
                    the next function can call trigger().

                    At an asynchronous state machine the call to trigger only blocks until the event is queued. All
                    the processing will be executed non-blocking.

                    Following example shows the difference. The code is identically, only the type of state machine is
                     different.
                    """.trimIndent()
                exampleFromSnippet(
                    sourceFileName = "src/test/kotlin/io/github/frantoso/jasm/doc/ReadmeGenerator.kt",
                    snippetId = "EVENT_DEF_CODE_SNIPPET",
                )
                example {
                    val output = mutableListOf<String>()
                    val state1 = State("first")
                    val state2 = State("second")

                    fun createFsmSync(): FsmSync =
                        fsmOf(
                            "MySyncFsm",
                            state1
                                .transition<Event1>(state2)
                                .entry<Int> {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                },
                            state2
                                .transition<Event1>(state2)
                                .entry<Int> {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                }.transitionToFinal<Event2>(),
                        )

                    fun createFsmAsync(): FsmAsync =
                        fsmAsyncOf(
                            "MyAsyncFsm",
                            state1
                                .transition<Event1>(state2)
                                .entry<Int> {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                },
                            state2
                                .transition<Event1>(state2)
                                .entry<Int> {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                }.transitionToFinal<Event2>(),
                        )

                    fun runFsm(fsm: Fsm): List<String> {
                        output.clear()

                        fsm.start(42)

                        runBlocking {
                            launch {
                                while (fsm.isRunning) {
                                    delay(100)
                                }
                            }

                            launch {
                                (0..5).forEach {
                                    output.addLast("+ $it")
                                    fsm.trigger(dataEvent<Event1, Int>(it))
                                    delay(10)
                                }

                                fsm.trigger(dataEvent<Event2, Int>(-1))
                            }

                            launch {
                                (10..15).forEach {
                                    output.addLast("+ $it")
                                    fsm.trigger(dataEvent<Event1, Int>(it))
                                    delay(1)
                                }
                            }
                        }

                        output.forEach {
                            println(it)
                        }

                        return output.toList()
                    }

                    val outputAsync = runFsm(createFsmAsync())
                    assertThat(outputAsync.takeLast(10).filter { it.startsWith("+") }).hasSize(0)

                    val outputSync = runFsm(createFsmSync())
                    assertThat(outputSync.takeLast(5).filter { it.startsWith("+") }.size)
                        .isGreaterThanOrEqualTo(2)
                }
                +
                    """
                    The output produced by both calls to `runFsm()`:

                     || synchronous | asynchronous |
                     ||:-----------:|:------------:|
                     ||    - 42     |     - 42     |
                     ||     + 0     |     + 0      |
                     ||     - 0     |     - 0      |
                     ||    + 10     |     + 10     |
                     ||    - 10     |     + 11     |
                     ||     + 1     |     + 1      |
                     ||     - 1     |     + 12     |
                     ||    + 11     |     + 2      |
                     ||    - 11     |     + 13     |
                     ||     + 2     |     + 3      |
                     ||     - 2     |     + 14     |
                     ||    + 12     |     + 4      |
                     ||    - 12     |     + 15     |
                     ||     + 3     |     + 5      |
                     ||     - 3     |     - 10     |
                     ||    + 13     |     - 11     |
                     ||    - 13     |     - 1      |
                     ||     + 4     |     - 12     |
                     ||     - 4     |     - 2      |
                     ||    + 14     |     - 13     |
                     ||    - 14     |     - 3      |
                     ||     + 5     |     - 14     |
                     ||     - 5     |     - 4      |
                     ||    + 15     |     - 15     |
                     ||    - 15     |     - 5      |
                    """.trimIndent()

                section("Composite States")
                +
                    """
                    This library also supports nested state machines through composite states.

                    A composite state can be built from the scratch or encapsulated in a class derived from `CompositeState`.  
                    To see how composite states work together with the history states, look at this example:
                    [How History States work](https://frantoso.github.io/jasmsharp/).
                    """.trimIndent()

                subSection("The diagram of the nested state machine")
                +
                    """
                    ![Simple state machine]($nestedFsmFileName)
                    
                    *A traffic light with normal operation over the day and flashing yellow in the night.*
                    """.trimIndent()

                subSection("Nested State Machine as Composite State")
                +
                    """
                    When deriving from the `CompositeState` class, the sub state-machine must be part of the state and
                    will be added automatically to the parent state machine when used.
                    """.trimIndent()
                exampleFromSnippet(
                    sourceFileName = "src/test/kotlin/io/github/frantoso/jasm/doc/ReadmeGenerator.kt",
                    snippetId = "COMPOSITE_STATE_DEF_CODE_SNIPPET",
                )

                subSection("Nested State Machine - manually created")
                +
                    """
                    A composite state can be also created by using a normal state as base and adding one or more child
                    machines when creating the parent state machine.
                    """.trimIndent()
                exampleFromSnippet(
                    sourceFileName = "src/test/kotlin/io/github/frantoso/jasm/doc/ReadmeGenerator.kt",
                    snippetId = "COMPOSITE_STATE_MANUALLY_CODE_SNIPPET",
                )

                subSection("Putting all together")
                +
                    """
                    The parent machine with two composite states.
                    """.trimIndent()
                exampleFromSnippet(
                    sourceFileName = "src/test/kotlin/io/github/frantoso/jasm/doc/ReadmeGenerator.kt",
                    snippetId = "USE_COMPOSITE_STATE_CODE_SNIPPET",
                )
            }

        val readmePage =
            Page(
                title = "Just another State Machine",
                fileName = "../README.md",
            )
        readmePage.write(markdown = readmeMarkdown.value)
    }
}
