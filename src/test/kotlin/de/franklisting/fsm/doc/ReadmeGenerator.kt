package de.franklisting.fsm.doc

import com.jillesvangurp.kotlin4example.Page
import com.jillesvangurp.kotlin4example.SourceRepository
import de.franklisting.fsm.Event
import de.franklisting.fsm.Fsm
import de.franklisting.fsm.FsmAsync
import de.franklisting.fsm.FsmSync
import de.franklisting.fsm.SimpleFsmTest.Tick
import de.franklisting.fsm.State
import de.franklisting.fsm.fsmAsyncOf
import de.franklisting.fsm.fsmOf
import de.franklisting.fsm.with
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
        // create the state machine

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
                    .with<Int>()
                    .entry { println("x--    $it") }
                    .transition(Tick, showingRedYellow),
                // define other states with transitions and other parameters...
                showingRedYellow
                    .with<Int>()
                    .entry { println("xx-    $it") }
                    .transition(Tick, showingGreen),
                showingGreen
                    .with<Int>()
                    .entry { println("--x    $it") }
                    .transition(Tick, showingYellow),
                showingYellow
                    .with<Int>()
                    .entry { println("-x-    $it") }
                    .transition(Tick, showingRed),
            )

        // start the state machine
        fsm.start(1)

        assertThat(fsm.isRunning).isTrue

        // trigger an event
        fsm.trigger(Tick, 1)

        assertThat(fsm.currentState.state).isEqualTo(showingRedYellow)
        // END_FSM_CODE_SNIPPET

        // generate diagram picture - only for the README
//        DiagramGenerator(fsm).toSvg(fsmFileName)
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
                    A better solution is to strictly separate the code for the state machines logic from
                    the functional code. An existing state machine component is parametrized to define its behavior.
                    
                    This readme includes the documentation of an implementation of a ready to use FSM (Finite State Machine).
                    Using this state machine is very simple. Just define the states, the transitions and the state actions.
                    """.trimIndent()

                section("Basic steps to create a State Machine")
                +
                    """
                    1. Model your state machine inside a graphical editor e.g. UML tool or any other applicable graphic tool.
                    2. Create an instance of the FSM class in your code.
                    3. Transfer all your transitions from the graphic to the code.
                    4. Register the action handlers for your states.
                    5. Start the state machine.
                    """.trimIndent()

                section("How to ...")
                +
                    """
                    - [Get it.](#gradle)
                    - [Implementing a simple Finite State Machine.](#how-to-create-a-simple-state-machine)
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
                    This topic shows how to implement a simple Finite State Machine using the StateMachine component.
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
                    sourceFileName = "src/test/kotlin/de/franklisting/fsm/doc/ReadmeGenerator.kt",
                    snippetId = "FSM_CODE_SNIPPET",
                )

                section("The classes")
                subSection("FsmSync<T>")
                +
                    """
                    A synchronous (blocking) state machine. The call to trigger is blocking. 
                    
                    The type parameter defines the type of data to send to the machine when it is triggered or started.
                    """.trimIndent()
                example {
                    data class MyFsmData(
                        val x: Int,
                        val y: String,
                    )

                    val state = State("MyState")
                    val fsm =
                        fsmOf(
                            "MyFsm",
                            // add at minimum one state
                            state
                                .with<MyFsmData>()
                                .transitionToFinal(Tick),
                        )

                    fsm.start(MyFsmData(42, "test"))
                }

                subSection("FsmAsync<T>")
                +
                    """
                    An asynchronous (non-blocking) state machine. The call to trigger is non-blocking. The events are 
                    queued and triggered sequentially.
                    
                    The type parameter defines the type of data to send to the machine when it is triggered or started.
                    """.trimIndent()
                example {
                    data class MyFsmData(
                        val x: Int,
                        val y: String,
                    )

                    val state = State("MyState")
                    val fsm =
                        fsmAsyncOf(
                            "MyFsm",
                            // add at minimum one state
                            state
                                .with<MyFsmData>()
                                .transitionToFinal(Tick),
                        )

                    fsm.start(MyFsmData(1, "2"))
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
                example {
                    val output = mutableListOf<String>()
                    val event1 = object : Event() {}
                    val event2 = object : Event() {}
                    val state1 = State("first")
                    val state2 = State("second")

                    fun createFsmSync(): FsmSync<Int> =
                        fsmOf(
                            "MySyncFsm",
                            state1
                                .with<Int>()
                                .transition(event1, state2)
                                .entry {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                },
                            state2
                                .with<Int>()
                                .transition(event1, state2)
                                .entry {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                }.transitionToFinal(event2),
                        )

                    fun createFsmAsync(): FsmAsync<Int> =
                        fsmAsyncOf(
                            "MyAsyncFsm",
                            state1
                                .with<Int>()
                                .transition(event1, state2)
                                .entry {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                },
                            state2
                                .with<Int>()
                                .transition(event1, state2)
                                .entry {
                                    output.addLast("- $it")
                                    Thread.sleep(100)
                                }.transitionToFinal(event2),
                        )

                    fun runFsm(fsm: Fsm<Int>): List<String> {
                        output.clear()
                        fsm.start(1)

                        runBlocking {
                            launch {
                                while (fsm.isRunning) {
                                    delay(100)
                                }
                            }

                            launch {
                                (0..5).forEach {
                                    output.addLast("+ $it")
                                    fsm.trigger(event1, it)
                                    delay(10)
                                }

                                fsm.trigger(event2, -1)
                            }

                            launch {
                                (10..15).forEach {
                                    output.addLast("+ $it")
                                    fsm.trigger(event1, it)
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
                     ||     - 1     |     - 1      |
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
