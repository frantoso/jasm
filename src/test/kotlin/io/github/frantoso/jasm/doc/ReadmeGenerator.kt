package io.github.frantoso.jasm.doc

import com.jillesvangurp.kotlin4example.Page
import com.jillesvangurp.kotlin4example.SourceRepository
import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.FsmAsync
import io.github.frantoso.jasm.FsmSync
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.dataEvent
import io.github.frantoso.jasm.examples.SimpleExample.Tick
import io.github.frantoso.jasm.fsmAsyncOf
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.with
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
                    .with() // prepare the state for usage
                    .entry { println("x--") } // add an entry function
                    .transition<Tick>(showingRedYellow), // add one or more transitions
                // define other states with transitions and other parameters...
                showingRedYellow
                    .with()
                    .entry { println("xx-") }
                    .transition<Tick>(showingGreen),
                showingGreen
                    .with()
                    .entry { println("--x") }
                    .transition<Tick>(showingYellow),
                showingYellow
                    .with()
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
        io.github.frantoso.jasm.testutil
            .DiagramGenerator(fsm)
            .toSvg(fsmFileName)
    }

    // BEGIN_EVENT_DEF_CODE_SNIPPET
    object Event1 : Event()

    object Event2 : Event()
    // END_EVENT_DEF_CODE_SNIPPET

    @Test
    fun `synchronous vs asynchronous`() {
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
                    The example shows the modelling of a single traffic light.
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
                                .with()
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
                                .with()
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
                                .with()
                                .transition<Event1>(state2)
                                .entry<Int> {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                },
                            state2
                                .with()
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
                                .with()
                                .transition<Event1>(state2)
                                .entry<Int> {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                },
                            state2
                                .with()
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
                     ||    - 42     |    - 42      |
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
            }

        val readmePage =
            Page(
                title = "Just another State Machine",
                fileName = "README.md",
            )
        readmePage.write(markdown = readmeMarkdown.value)
    }
}
