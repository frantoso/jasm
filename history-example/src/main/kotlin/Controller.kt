package io.github.frantoso

import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.IState

class Controller {
    /**
     * Initializes a new instance of the Controller class.
     *
     * This constructor creates a new MainMachine instance and registers the necessary handlers for the controller.
     */
    constructor() {
        this.addHandlers(this.mainFsm.machine)
    }

    /**
     * Gets the mapping of event key characters to their corresponding event instances.
     */
    val eventMappings =
        mapOf(
            "n" to Events.Next,
            "b" to Events.Break,
            "c" to Events.Continue,
            "d" to Events.ContinueDeep,
            "r" to Events.Restart,
        )

    /**
     * Gets the main finite state machine (FSM) that controls the primary workflow of the system.
     */
    val mainFsm: MainMachine = MainMachine()

    /**
     * Runs the main interactive loop, processing user input to control the finite state machine.
     *
     * Pressing 's' starts the main finite state machine. Pressing 'q' exits the loop. Any other key
     * triggers an event mapped to that key, or a default event if no mapping exists. This method blocks until the user
     * chooses to exit.
     */
    fun run() {
        showHelp()

        while (true) {
            val input = (readln().trim().firstOrNull() ?: 'n').lowercase()
            println()
            when (input) {
                "h" -> {
                    showHelp()
                }

                "q" -> {
                    return
                }

                "s" -> {
                    this.mainFsm.start()
                }

                else -> {
                    this.mainFsm.trigger(
                        this.eventMappings.entries
                            .firstOrNull { m -> m.key == input }
                            ?.value ?: Events.Next,
                    )
                }
            }
        }
    }

    /**
     * Displays a list of available keyboard commands and their descriptions to the console.
     */
    private fun showHelp() {
        println("Press a key to control the state machine.")
        println("q: Quit the application")
        println("h: Shows help (this text)")
        println("s: Start the main state machine")
        println("n: Trigger NextEvent")
        println("b: Trigger BreakEvent")
        println("c: Trigger ContinueEvent")
        println("d: Trigger ContinueDeepEvent")
        println("r: Trigger RestartEvent")
        println("Any other key: Trigger NextEvent")
        println()
    }

    /**
     * Handles the event that is raised when the state of a finite state machine changes.
     *
     * @param sender The finite state machine instance whose state has changed.
     * @param from An object that contains the event data, including the previous state.
     * @param to An object that contains the event data, including the new state.
     */
    private fun onStateChanged(
        sender: Fsm,
        from: IState,
        to: IState,
    ) {
        println("${(sender).name}: $from ==> $to")
    }

    /**
     * Attaches event handlers to the specified finite state machine (FSM) and all of its child states recursively.
     *
     * @param fsm The finite state machine to which event handlers are added. Cannot be null.
     */
    private fun addHandlers(fsm: Fsm) {
        fsm.stateChanged += this::onStateChanged
        fsm.debugInterface.stateDump
            .map { container -> container.children }
            .forEach { children -> children.forEach(this::addHandlers) }
    }
}
