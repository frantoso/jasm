package de.franklisting.fsm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * Class managing the states of a synchronous FSM (finite state machine).
 * Initializes a new instance of the Fsm class.
 *
 * @param name The name of the FSM.
 * @param T The type of data provided to the condition and action handlers.
 */
abstract class Fsm<T>(
    val name: String,
) {
    /**
     * Gets the initial state.
     */
    internal val initial: State<T> = InitialState()

    /**
     * Gets the name of the currently active state.
     */
    var currentState: State<T> = initial
        private set

    /**
     * Informs about a state change.
     *
     * This event is fired before the OnEntry handler of the state is called.
     * It should be used mainly for informational purpose.
     */
    var onStateChanged: ((sender: Fsm<T>, from: State<T>, to: State<T>) -> Unit)? = null

    /**
     * Informs about a trigger of an event.
     *
     * This event is fired before a state is changed.
     * It should be used mainly for informational purpose.
     */
    var onTriggered: ((sender: Fsm<T>, currentState: State<T>, event: Event, handled: Boolean) -> Unit)? = null

    /**
     * Gets the final state.
     */
    val final: State<T> = FinalState()

    /**
     * Gets a value indicating whether the automaton is started and has not reached the final state.
     */
    val isRunning
        get() = !currentState.isInitial && !hasFinished

    /**
     * Gets a value indicating whether the automaton has reached the final state.
     */
    val hasFinished
        get() = currentState.isFinal

    /**
     * Adds a new transition to the initial state.
     *
     * @param stateTo A reference to the end point of this transition.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun initialTransition(stateTo: State<T>) {
        if (initial.hasTransitions) {
            throw FsmException("The start state must have only one transition!", name)
        }

        initial.transition(StartEvent, TransitionEndPoint(stateTo))
    }

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
        changeStateData: ChangeStateData<T>,
        data: T,
    ) {
        if (changeStateData.endPoint.state.isInvalid) {
            return
        }

        val oldState = currentState
        currentState = changeStateData.endPoint.state

        raiseStateChanged(oldState, currentState)

        currentState.start(data, changeStateData.endPoint.history)
    }

    /**
     * Raises the state changed event.
     *
     * @param oldState The old state.
     * @param newState The new state.
     */
    private fun raiseStateChanged(
        oldState: State<T>,
        newState: State<T>,
    ) = onStateChanged?.invoke(this, oldState, newState)

    /**
     * Raises the triggered event.
     *
     * @param event The event which was processed.
     * @param handled A value indicating whether the event was handled (true) or not (false).
     */
    private fun raiseTriggered(
        event: Event,
        handled: Boolean,
    ) = onTriggered?.invoke(this, currentState, event, handled)

    /**
     * This interface provides methods which are not intended to use for normal operation.
     * Use the methods for testing purposes or to recover from a reboot.
     */
    interface Debug<T> {
        /**
         * Should set the provided state as active state.
         * @param state The state to set as current state.
         */
        fun setState(state: State<T>)

        /**
         * Sets the provided state as active state and starts child machines if present, e.g. to resume after a reboot.
         *  - IMPORTANT: This method does not call the entry function of the state.
         * @param state The state to set as current state.
         * @param data The data object.
         */
        fun resume(
            state: State<T>,
            data: T,
        )
    }

    /**
     * Sets the provided state as active state.
     * @param state The state to set as current state.
     */
    private fun setState(state: State<T>) {
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
        state: State<T>,
        data: T,
    ) {
        setState(state)
        state.startChildren(data)
    }

    /**
     * Gets an object implementing the debug interface. This allows the access to special functions which are mainly
     * provided for tests.
     */
    val debugInterface =
        object : Debug<T> {
            override fun setState(state: State<T>) = this@Fsm.setState(state)

            override fun resume(
                state: State<T>,
                data: T,
            ) = this@Fsm.resume(state, data)
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

class FsmSync<T>(
    name: String,
) : Fsm<T>(name) {
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

class FsmAsync<T>(
    name: String,
    override val coroutineContext: CoroutineContext = CoroutineScope(Dispatchers.Default.limitedParallelism(1)).coroutineContext,
) : Fsm<T>(name),
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
