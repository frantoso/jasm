package io.github.frantoso.jasm

import io.github.frantoso.jasm.model.FsmInfo
import io.github.frantoso.jasm.model.StateChangedInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 *     Provides debugging support for a finite state machine (FSM) by enabling state inspection and synchronization over a
 *     TCP connection.
 *
 *     The DebugAdapter class registers commands with a TCP adapter to allow external tools to query FSM
 *     information and receive state updates in real time. It is typically used to facilitate integration with debugging
 *     or visualization tools that require insight into the FSM's structure and current state. Thread safety and
 *     responsiveness are managed internally; consumers do not need to handle synchronization when using this
 *     class.
 */
class DebugAdapter {
    /**
     * Scope to execute async operations.
     */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Initializes a new instance of the DebugAdapter class and registers command handlers
     * for the specified finite state machine (FSM).
     *
     * @param fsm The finite state machine to be debugged.
     */
    private constructor(fsm: Fsm) {
        fsmInfo = fsm.convert()
        allMachines = fsm.allMachines()

        TcpAdapter.addCommand(fsm.name, GET_STATES_COMMAND, this::onGetStates)
        TcpAdapter.addCommand(fsm.name, RECEIVED_FSM_COMMAND, this::onFsmReceived)
        addStateChangedHandlers(fsm)

        scope.launch(Dispatchers.Default) { sendFsmInfo() }
    }

    /**
     * Gets the FSM information.
     */
    private val fsmInfo: FsmInfo

    /**
     * Gets a list of all machines, used to send the currently active states.
     */
    private val allMachines: List<Fsm>

    /**
     * A value indicating whether the server has received the FSM information.
     */
    private var fsmInfoAcknowledged: Boolean = false

    /**
     * Helper to send the data to the server.
     *
     * @param T The type of the data to send.
     * @param command The command.
     * @param data The data.
     */
    private fun <T> sendAsync(
        command: String,
        data: T,
    ) = TcpAdapter.sendAsync(fsmInfo.name, command, data.serialize())

    /**
     * Handles the acknowledgement of the server. Stops periodically sending the FSM info.
     *
     * @param ignored The command data (ignored).
     */
    private fun onFsmReceived(
        @Suppress("unused") ignored: String,
    ) {
        fsmInfoAcknowledged = true
    }

    /**
     * Sends the FSM information until the server sent an acknowledgement.
     */
    private suspend fun sendFsmInfo() {
        while (!fsmInfoAcknowledged) {
            sendAsync(SET_FSM_COMMAND, fsmInfo)
            delay(NextSendWaitingTime)
        }
    }

    /**
     * Sends the info about the current active of all state machines.
     *
     * @param ignored The command data (ignored).
     */
    private fun onGetStates(
        @Suppress("unused") ignored: String,
    ) = allMachines.forEach { fsm ->
        onStateChanged(fsm, fsm.debugInterface.initialState.state, fsm.currentState)
    }

    /**
     * Attaches event handlers to the specified finite state machine (FSM) and all of its child states recursively.
     *
     * @param fsm The finite state machine to which event handlers are added. Cannot be null.
     */
    private fun addStateChangedHandlers(fsm: Fsm) {
        fsm.stateChanged += this::onStateChanged
        fsm.debugInterface.stateDump
            .map { container -> container.children }
            .forEach { children -> children.forEach(this::addStateChangedHandlers) }
    }

    /**
     * Called when the state of the FSM has changed.
     *
     * @param sender The sending state machine.
     * @param oldState The state before the state change.
     * @param newState The state after the state change.
     */
    private fun onStateChanged(
        sender: Fsm?,
        oldState: IState,
        newState: IState,
    ) {
        val stateChangedInfo = StateChangedInfo(sender?.name ?: fsmInfo.name, oldState, newState)
        sendAsync(UPDATE_STATE_COMMAND, stateChangedInfo)
    }

    companion object {
        private const val GET_STATES_COMMAND = "get-states"
        private const val SET_FSM_COMMAND = "set-fsm"
        private const val UPDATE_STATE_COMMAND = "update-state"
        private const val RECEIVED_FSM_COMMAND = "received-fsm"
        private val NextSendWaitingTime = 3.seconds

        /**
         * Static collection of all registered debug adapters. Currently only used to prevent garbage collection.
         */
        private val Adapters = mutableMapOf<String, DebugAdapter>()

        /**
         * Registers the specified finite state machine (FSM) at the DebugAdapter.
         *
         * @param fsm The finite state machine to be debugged.
         * @return Returns the specified fsm to support chaining.
         */
        @JvmStatic
        fun of(fsm: Fsm): Fsm {
            val adapter = DebugAdapter(fsm)
            Adapters[fsm.name] = adapter
            return fsm
        }
    }
}
