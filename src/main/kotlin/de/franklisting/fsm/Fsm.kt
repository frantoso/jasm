package de.franklisting.fsm

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
 *
 * @param T The type of data provided to the condition and action handlers.
 */
abstract class Fsm<T>(
    val name: String,
    private val onStateChanged: ((sender: Fsm<T>, from: IState, to: IState) -> Unit),
    private val onTriggered: ((sender: Fsm<T>, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<T, out EndState>,
    otherStates: List<StateContainerBase<T, out IState>>,
) {
    /**
     * Gets the initial state.
     */
    private val initial: InitialStateContainer<T> = InitialState().with<T>().transition(startState.state)

    /**
     * A list of all states excluding the initial state.
     */
    private val states: List<StateContainerBase<T, out IState>> =
        listOf(startState) + otherStates + destinationOnlyStates(startState, otherStates) + finalStateOrNot(otherStates)

    /**
     * Gets the final state (as list) or an empty list if there is no final state used in the transitions.
     */
    private fun finalStateOrNot(otherStates: List<StateContainerBase<T, out IState>>): List<StateContainerBase<T, out IState>> =
        if (otherStates.flatMap { it.debugInterface.transitionDump }.none { it.isToFinal }) {
            emptyList()
        } else {
            listOf(FinalState().with())
        }

    /**
     * Gets a list with states used only as destination in a transition.
     * It's not a normal use case, but it may happen.
     */
    private fun destinationOnlyStates(
        startState: StateContainerBase<T, out EndState>,
        otherStates: List<StateContainerBase<T, out IState>>,
    ): List<StateContainerBase<T, out IState>> =
        (listOf(startState) + otherStates).let { knownStates ->
            knownStates
                .asSequence()
                .flatMap { st ->
                    st.debugInterface.transitionDump.map { it.endPoint.state }
                }.distinct()
                .filterIsInstance<State>()
                .filterNot { state -> knownStates.map { it.state }.contains(state) }
                .map { it.with<T>() }
                .toList()
        }

    /**
     * Gets the name of the currently active state.
     */
    var currentState: StateContainerBase<T, out IState> = initial
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
     *
     * @param data The data object.
     */
    fun start(data: T) {
        currentState = initial
        triggerEvent(StartEvent, data)
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
     * Fires the Do event.
     *
     * @param data The data object.
     */
    fun doAction(data: T) = currentState.fireDoInState(data)

    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns true if the event was handled; false otherwise. In case of
     * asynchronous processing it returns null.
     */
    abstract fun trigger(
        trigger: Event,
        data: T,
    ): Boolean

    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns true if the event was handled; false otherwise. In case of
     * asynchronous processing it returns null.
     */
    protected fun triggerEvent(
        trigger: Event,
        data: T,
    ): Boolean {
        synchronized(this) {
            checkParameter(trigger)

            val changeStateData = currentState.trigger(trigger, data)
            raiseTriggered(trigger, changeStateData.handled)
            activateState(changeStateData, data)

            return changeStateData.handled
        }
    }

    /**
     * Activates the new state.
     *
     * @param changeStateData The data needed to activate the next state.
     * @param data The data.
     */
    private fun activateState(
        changeStateData: ChangeStateData,
        data: T,
    ) {
        if (changeStateData.endPoint == null) {
            return
        }

        val oldState = currentState

        currentState = changeStateData.endPoint.state.container

        raiseStateChanged(oldState, currentState)

        currentState.start(data, changeStateData.endPoint.history)
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
        oldState: StateContainerBase<T, out IState>,
        newState: StateContainerBase<T, out IState>,
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
    interface Debug<T> {
        /**
         * Should set the provided state as active state.
         * @param state The state to set as current state.
         */
        fun setState(state: State)

        /**
         * Sets the provided state as active state and starts child machines if present, e.g. to resume after a reboot.
         *  - IMPORTANT: This method does not call the entry function of the state.
         * @param state The state to set as current state.
         * @param data The data object.
         */
        fun resume(
            state: State,
            data: T,
        )

        /**
         * Gets a snapshot of the states.
         */
        val stateDump: List<StateContainerBase<T, out IState>>

        /**
         * Gets initial state.
         */
        val initialState: InitialStateContainer<T>
    }

    /**
     * Sets the provided state as active state.
     * @param state The state to set as current state.
     */
    private fun setState(state: StateContainerBase<T, out IState>) {
        val stateBefore = currentState
        currentState = state
        raiseStateChanged(stateBefore, currentState)
    }

    /**
     * Sets the provided state as active state and starts child machines if present, e.g. to resume after a reboot.
     *  - IMPORTANT: This method does not call the entry function of the state.
     * @param state The state to set as current state.
     * @param data The data object.
     */
    private fun resume(
        state: State,
        data: T,
    ) {
        setState(state.container)
        state.container.startChildren(data)
    }

    /**
     * Gets an object implementing the debug interface. This allows the access to special functions which are mainly
     * provided for tests.
     */
    val debugInterface =
        object : Debug<T> {
            override fun setState(state: State) = this@Fsm.setState(state.container)

            override fun resume(
                state: State,
                data: T,
            ) = this@Fsm.resume(state, data)

            override val stateDump: List<StateContainerBase<T, out IState>> = states

            override val initialState: InitialStateContainer<T> = initial
        }

    companion object {
        /**
         * Checks whether the provided parameter is valid.
         *
         * @param trigger The event parameter to check.
         */
        internal fun checkParameter(trigger: Event) {
            if (trigger == NoEvent) {
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
 *
 * @param T The type of data provided to the condition and action handlers.
 */
class FsmSync<T>(
    name: String,
    onStateChanged: ((sender: Fsm<T>, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm<T>, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<T, out EndState>,
    otherStates: List<StateContainerBase<T, out IState>>,
) : Fsm<T>(name, onStateChanged, onTriggered, startState, otherStates) {
    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns true if the event was handled; false otherwise.
     */
    override fun trigger(
        trigger: Event,
        data: T,
    ): Boolean = triggerEvent(trigger, data)
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
 *
 * @param T The type of data provided to the condition and action handlers.
 */
class FsmAsync<T>(
    name: String,
    onStateChanged: ((sender: Fsm<T>, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm<T>, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<T, out EndState>,
    otherStates: List<StateContainerBase<T, out IState>>,
    override val coroutineContext: CoroutineContext = CoroutineScope(Dispatchers.Default.limitedParallelism(1)).coroutineContext,
) : Fsm<T>(name, onStateChanged, onTriggered, startState, otherStates),
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
     * @param data The data provided to the condition and action handlers.
     * @return Returns true.
     */
    override fun trigger(
        trigger: Event,
        data: T,
    ): Boolean {
        this.launch {
            mutex.withLock {
                triggerEvent(trigger, data)
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
fun <T> fsmOf(
    name: String,
    startState: StateContainerBase<T, out EndState>,
    vararg otherStates: StateContainerBase<T, out IState>,
): FsmSync<T> = FsmSync(name, { _, _, _ -> }, { _, _, _, _ -> }, startState, otherStates.toList())

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
fun <T> fsmOf(
    name: String,
    onStateChanged: ((sender: Fsm<T>, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm<T>, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<T, out EndState>,
    vararg otherStates: StateContainerBase<T, out IState>,
): FsmSync<T> = FsmSync(name, onStateChanged, onTriggered, startState, otherStates.toList())

/**
 * Creates an asynchronous FSM from the provided data.
 * @param name The name of the FSM.
 * @param startState The start state (first state) of the FSM.
 * @param otherStates The other states of the FSM.
 */
fun <T> fsmAsyncOf(
    name: String,
    startState: StateContainerBase<T, out EndState>,
    vararg otherStates: StateContainerBase<T, out IState>,
): FsmAsync<T> = FsmAsync(name, { _, _, _ -> }, { _, _, _, _ -> }, startState, otherStates.toList())

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
fun <T> fsmAsyncOf(
    name: String,
    onStateChanged: ((sender: Fsm<T>, from: IState, to: IState) -> Unit),
    onTriggered: ((sender: Fsm<T>, currentState: IState, event: Event, handled: Boolean) -> Unit),
    startState: StateContainerBase<T, out EndState>,
    vararg otherStates: StateContainerBase<T, out IState>,
): FsmAsync<T> = FsmAsync(name, onStateChanged, onTriggered, startState, otherStates.toList())
