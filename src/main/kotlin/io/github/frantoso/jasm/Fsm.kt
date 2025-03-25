package io.github.frantoso.jasm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * Class managing the states of an FSM (finite state machine).
 * Initializes a new instance of the Fsm class.
 *
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param onTriggered Callback to be informed about a trigger of an event. This event is fired before a state
 * is changed. It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
abstract class Fsm(
    val name: String,
    private val onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    private val onTriggered: ((sender: Fsm, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    otherStates: List<StateContainerBase<out IState>>,
) {
    /**
     * Gets the initial state.
     */
    private val initial: InitialStateContainer = InitialState().use().transition(startState.state)

    /**
     * The start state in a list for later use.
     */
    private val startStateAsList = listOf(startState)

    /**
     * A list of all states excluding the initial state.
     */
    private val states: List<StateContainerBase<out IState>> =
        startStateAsList + otherStates + destinationOnlyStates(startState, otherStates) + finalStateOrNot(otherStates)

    /**
     * Gets the final state (as list) or an empty list if there is no final state used in the transitions.
     */
    private fun finalStateOrNot(otherStates: List<StateContainerBase<out IState>>): List<StateContainerBase<out IState>> =
        if ((otherStates + startStateAsList).flatMap { it.debugInterface.transitionDump }.none { it.isToFinal }) {
            emptyList()
        } else {
            listOf(FinalState().use())
        }

    /**
     * Gets a list with states used only as destination in a transition.
     * It's not a normal use case, but it may happen.
     */
    private fun destinationOnlyStates(
        startState: StateContainerBase<out EndState>,
        otherStates: List<StateContainerBase<out IState>>,
    ): List<StateContainerBase<out IState>> =
        (listOf(startState) + otherStates).let { knownStates ->
            knownStates
                .asSequence()
                .flatMap { st ->
                    st.debugInterface.transitionDump.map { it.endPoint.state }
                }.distinct()
                .filterIsInstance<State>()
                .filterNot { state -> knownStates.map { it.state }.contains(state) }
                .map { it.use() }
                .toList()
        }

    init {
        val isNoEventMisused =
            states
                .flatMap { state -> state.debugInterface.transitionDump.map { state to it } }
                .any { !it.first.hasChildren && (it.second::class == NoEvent::class || it.second::class == DataNoEvent::class) }

        if (isNoEventMisused) {
            throw FsmException("A transition without event can only be used for nested states!")
        }
    }

    /**
     * Gets the name of the currently active state.
     */
    var currentState: StateContainerBase<out IState> = initial
        private set

    /**
     * Gets a value indicating whether the automaton is started and has not reached the final state.
     */
    val isRunning get() = currentState.state !is InitialState && !hasFinished

    /**
     * Gets a value indicating whether the automaton has reached the final state.
     */
    val hasFinished get() = currentState.state is FinalState

    /**
     * Starts the behavior of the Fsm class. Executes the transition from the start state to the first user defined state.
     * This method calls the initial states OnEntry method.
     */
    fun start() {
        currentState = initial
        triggerEvent(StartEvent)
        onStart()
    }

    /**
     * Called when the FSM starts. Allows a derived class to execute additional startup code.
     */
    protected open fun onStart() {
    }

    /**
     * Returns a string representation of the FSM - it's name.
     */
    override fun toString(): String = name

    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @return Returns true if the event was handled; false otherwise. In case of
     * asynchronous processing it returns null.
     */
    abstract fun trigger(trigger: Event): Boolean

    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @return Returns true if the event was handled; false otherwise. In case of
     * asynchronous processing it returns null.
     */
    protected fun triggerEvent(trigger: Event): Boolean {
        synchronized(this) {
            checkParameter(trigger)

            val changeStateData = currentState.trigger(trigger)
            raiseTriggered(trigger, changeStateData.handled)
            activateState(trigger, changeStateData)

            return changeStateData.handled
        }
    }

    /**
     * Activates the new state.
     *
     * @param changeStateData The data needed to activate the next state.
     */
    private fun activateState(
        trigger: Event,
        changeStateData: ChangeStateData,
    ) {
        if (changeStateData.endPoint == null) {
            return
        }

        val oldState = currentState

        currentState = changeStateData.endPoint.state.container

        raiseStateChanged(oldState, currentState)

        currentState.start(trigger, changeStateData.endPoint.history)
    }

    /**
     * Gets the state container of the provided state. Will crash if the state is not in the list of states.
     */
    val EndState.container get() = states.first { it.state == this }

    /**
     * Raises the state changed event.
     *
     * @param oldState The old state.
     * @param newState The new state.
     */
    private fun raiseStateChanged(
        oldState: StateContainerBase<out IState>,
        newState: StateContainerBase<out IState>,
    ) {
        try {
            onStateChanged(this, oldState.state, newState.state)
        } catch (ex: Throwable) {
            throw FsmException("Error calling onStateChanged on machine $name.", "", ex)
        }
    }

    /**
     * Raises the triggered event.
     *
     * @param event The event which was processed.
     * @param handled A value indicating whether the event was handled (true) or not (false).
     */
    private fun raiseTriggered(
        event: Event,
        handled: Boolean,
    ) {
        try {
            onTriggered(this, currentState.state, event, handled)
        } catch (ex: Throwable) {
            throw FsmException("Error calling onTriggered on machine $name.", "", ex)
        }
    }

    /**
     * This interface provides methods which are not intended to use for normal operation.
     * Use the methods for testing purposes or to recover from a reboot.
     */
    interface Debug {
        /**
         * Should set the provided state as active state.
         * @param state The state to set as current state.
         */
        fun setState(state: State)

        /**
         * Sets the provided state as active state and starts child machines if present, e.g. to resume after a reboot.
         *  - IMPORTANT: This method does not call the entry function of the state.
         * @param state The state to set as current state.
         */
        fun resume(state: State)

        /**
         * Gets a snapshot of the states.
         */
        val stateDump: List<StateContainerBase<out IState>>

        /**
         * Gets initial state.
         */
        val initialState: InitialStateContainer
    }

    /**
     * Sets the provided state as active state.
     * @param state The state to set as current state.
     */
    private fun setState(state: StateContainerBase<out IState>) {
        val stateBefore = currentState
        currentState = state
        raiseStateChanged(stateBefore, currentState)
    }

    /**
     * Sets the provided state as active state and starts child machines if present, e.g. to resume after a reboot.
     *  - IMPORTANT: This method does not call the entry function of the state.
     * @param state The state to set as current state.
     */
    private fun resume(state: State) {
        setState(state.container)
        state.container.startChildren()
    }

    /**
     * Gets an object implementing the debug interface. This allows the access to special functions which are mainly
     * provided for tests.
     */
    val debugInterface =
        object : Debug {
            override fun setState(state: State) = this@Fsm.setState(state.container)

            override fun resume(state: State) = this@Fsm.resume(state)

            override val stateDump: List<StateContainerBase<out IState>> = states

            override val initialState: InitialStateContainer = initial
        }

    companion object {
        /**
         * Checks whether the provided parameter is valid.
         *
         * @param trigger The event parameter to check.
         */
        internal fun checkParameter(trigger: Event) {
            if (trigger::class == NoEvent::class || trigger::class == DataNoEvent::class) {
                throw FsmException("Fsm.trigger: A trigger event cannot be NoEvent!")
            }
        }
    }
}

/**
 * Class managing the states of a synchronous FSM (finite state machine).
 * Initializes a new instance of the FsmSync class.
 *
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param onTriggered Callback to be informed about a trigger of an event. This event is fired before a state
 * is changed. It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
class FsmSync(
    name: String,
    onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    otherStates: List<StateContainerBase<out IState>>,
) : Fsm(name, onStateChanged, onTriggered, startState, otherStates) {
    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @return Returns true if the event was handled; false otherwise.
     */
    override fun trigger(trigger: Event): Boolean = triggerEvent(trigger)
}

/**
 * Class managing the states of an asynchronous FSM (finite state machine).
 * Initializes a new instance of the FsmAsync class.
 *
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param onTriggered Callback to be informed about a trigger of an event. This event is fired before a state
 * is changed. It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
class FsmAsync(
    name: String,
    onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    otherStates: List<StateContainerBase<out IState>>,
    override val coroutineContext: CoroutineContext = CoroutineScope(Dispatchers.Default.limitedParallelism(1)).coroutineContext,
) : Fsm(name, onStateChanged, onTriggered, startState, otherStates),
    CoroutineScope {
    /**
     * The mutex to synchronize the placing of the events.
     * It is initially locked and will be unlocked when the state machine is started.
     * Because the trigger method is synchronized, the mutex is not necessary for synchronization - it only blocks
     * triggering events before calling fsm.start().
     */
    private val mutex = Mutex(true)

    /**
     * Called when the FSM starts event processing - unlocks the mutex used to synchronize the requests.
     */
    override fun onStart() {
        mutex.unlock()
    }

    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @return Returns true.
     */
    override fun trigger(trigger: Event): Boolean {
        this.launch {
            mutex.withLock {
                triggerEvent(trigger)
            }
        }

        return true
    }
}

/**
 * Creates a synchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun fsmOf(
    name: String,
    startState: StateContainerBase<out EndState>,
    vararg otherStates: StateContainerBase<out IState>,
): FsmSync = FsmSync(name, { _, _, _ -> }, { _, _, _, _ -> }, startState, otherStates.toList())

/**
 * Creates a synchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param onTriggered Callback to be informed about a trigger of an event. This event is fired before a state
 * is changed. It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun fsmOf(
    name: String,
    onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    vararg otherStates: StateContainerBase<out IState>,
): FsmSync = FsmSync(name, onStateChanged, onTriggered, startState, otherStates.toList())

/**
 * Creates a synchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun fsmOf(
    name: String,
    onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    startState: StateContainerBase<out EndState>,
    vararg otherStates: StateContainerBase<out IState>,
): FsmSync = FsmSync(name, onStateChanged, { _, _, _, _ -> }, startState, otherStates.toList())

/**
 * Creates an asynchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun fsmAsyncOf(
    name: String,
    startState: StateContainerBase<out EndState>,
    vararg otherStates: StateContainerBase<out IState>,
): FsmAsync = FsmAsync(name, { _, _, _ -> }, { _, _, _, _ -> }, startState, otherStates.toList())

/**
 * Creates an asynchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param onTriggered Callback to be informed about a trigger of an event. This event is fired before a state
 * is changed. It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun fsmAsyncOf(
    name: String,
    onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    vararg otherStates: StateContainerBase<out IState>,
): FsmAsync = FsmAsync(name, onStateChanged, onTriggered, startState, otherStates.toList())

/**
 * Creates an asynchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param onStateChanged Callback to be informed about a state change. - This function is called before the OnEntry
 * handler of the state is called.  It should be used mainly for informational purpose.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun fsmAsyncOf(
    name: String,
    onStateChanged: ((sender: Fsm, from: IState, to: IState) -> Unit),
    startState: StateContainerBase<out EndState>,
    vararg otherStates: StateContainerBase<out IState>,
): FsmAsync = FsmAsync(name, onStateChanged, { _, _, _, _ -> }, startState, otherStates.toList())
