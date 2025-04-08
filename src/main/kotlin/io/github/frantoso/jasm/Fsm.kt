package io.github.frantoso.jasm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * A class managing the states of an FSM (finite state machine).
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
    private val onTriggered: ((sender: Fsm, currentState: IState, event: IEvent, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    otherStates: List<StateContainerBase<out IState>>,
) {
    /**
     * Gets the initial state.
     */
    private val initial: InitialStateContainer = InitialState().transition(startState.state)

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
            listOf(FinalStateContainer(FinalState()))
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
                .map { it.toContainer() }
                .toList()
        }

    init {
        val isNoEventMisused =
            states
                .flatMap { state -> state.debugInterface.transitionDump.map { state to it } }
                .any { !it.first.hasChildren && (it.second.eventType == NoEvent::class) }

        if (isNoEventMisused) {
            throw FsmException("A transition without event can only be used for nested states!")
        }
    }

    /**
     * Gets the container of the currently active state.
     */
    var currentStateContainer: StateContainerBase<out IState> = initial
        private set

    /**
     * Gets the currently active state.
     */
    val currentState: IState get() = currentStateContainer.state

    /**
     * Gets the currently active state and, if available, all active child states.
     */
    val currentStateTree: StateTreeNode
        get() =
            StateTreeNode(
                currentState,
                currentStateContainer.children.map { it.currentStateTree },
            )

    /**
     * Gets the currently active state container and, if available, all active child containers.
     */
    val currentStateContainerTree: StateContainerTreeNode
        get() =
            StateContainerTreeNode(
                currentStateContainer,
                currentStateContainer.children.map { it.currentStateContainerTree },
            )

    /**
     * Gets a value indicating whether the automaton is started and has not reached the final state.
     */
    val isRunning get() = currentState !is InitialState && !hasFinished

    /**
     * Gets a value indicating whether the automaton has reached the final state.
     */
    val hasFinished get() = currentState is FinalState

    /**
     * Starts the behavior of the Fsm class. Executes the transition from the start state to the first user defined state.
     * This method calls the initial states OnEntry method.
     */
    fun start() {
        currentStateContainer = initial
        triggerEvent(StartEvent)
        onStart()
    }

    /**
     * Starts the behavior of the Fsm class. Executes the transition from the start state to the first user defined state.
     * This method calls the initial states OnEntry method.
     * @param T The type of the data parameter.
     * @param data The data to provide to the function.
     */
    fun <T : Any> start(data: T) {
        currentStateContainer = initial
        triggerEvent(DataEvent(data, StartEvent::class))
        onStart()
    }

    /**
     * Fires the parametrised Do event of the current state.
     * @param T The type of the data parameter.
     * @param data The data to provide to the function.
     */
    fun <T : Any> doAction(data: T) = currentStateContainer.onDoInState.fire(DataEvent(data, NoEvent::class))

    /**
     * Fires the Do event of the current state.
     */
    fun doAction() = currentStateContainer.onDoInState.fire(NoEvent)

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
     * @param event The event occurred.
     * @return Returns true if the event was handled; false otherwise. In case of
     * asynchronous processing it returns true.
     */
    abstract fun trigger(event: IEvent): Boolean

    /**
     * Triggers a transition.
     * @param event The event occurred.
     * @return Returns true if the event was handled; false otherwise. In case of
     * asynchronous processing it returns true.
     */
    protected fun triggerEvent(event: IEvent): Boolean {
        synchronized(this) {
            checkParameter(event)

            val changeStateData = currentStateContainer.trigger(event)
            raiseTriggered(event, changeStateData.handled)
            activateState(event, changeStateData)

            return changeStateData.handled
        }
    }

    /**
     * Activates the new state.
     * @param event The event occurred.
     * @param changeStateData The data needed to activate the next state.
     */
    private fun activateState(
        event: IEvent,
        changeStateData: ChangeStateData,
    ) {
        if (changeStateData.endPoint == null) {
            return
        }

        val oldState = currentStateContainer
        currentStateContainer = changeStateData.endPoint.state.container
        raiseStateChanged(oldState, currentStateContainer)
        currentStateContainer.start(event, changeStateData.endPoint.history)
    }

    /**
     * Gets the state container of the provided state. Will crash if the state is not in the list of states.
     */
    private val EndState.container get() = states.first { it.state == this }

    /**
     * Raises the state changed event.
     * @param oldState The old state.
     * @param newState The new state.
     */
    private fun raiseStateChanged(
        oldState: StateContainerBase<out IState>,
        newState: StateContainerBase<out IState>,
    ) = try {
        onStateChanged(this, oldState.state, newState.state)
    } catch (ex: Throwable) {
        throw FsmException("Error calling onStateChanged on machine $name.", "", ex)
    }

    /**
     * Raises the triggered event.
     * @param event The event which was processed.
     * @param handled A value indicating whether the event was handled (true) or not (false).
     */
    private fun raiseTriggered(
        event: IEvent,
        handled: Boolean,
    ) {
        try {
            onTriggered(this, currentState, event, handled)
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
         * Triggers a transition synchronously. Independent of the concrete implementation it calls the synchronous
         * trigger function from the base class.
         * @param event The event occurred.
         * @return Returns true if the event was handled; false otherwise.
         */
        fun triggerSync(event: IEvent): Boolean

        /**
         * Triggers a transition synchronously. Independent of the concrete implementation it calls the synchronous
         * trigger function from the base class.
         * @param T The type of the data parameter.
         * @param event The event occurred.
         * @param data The data to send with the event.
         * @return Returns true if the event was handled; false otherwise.
         */
        fun <T : Any> triggerSync(
            event: Event,
            data: T,
        ): Boolean = triggerSync(DataEvent(data, event::class))

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
        val stateBefore = currentStateContainer
        currentStateContainer = state
        raiseStateChanged(stateBefore, currentStateContainer)
    }

    /**
     * Sets the provided state as active state and starts child machines if present, e.g. to resume after a reboot.
     *  - IMPORTANT: This method does not call the entry function of the state.
     * @param state The state to set as current state.
     */
    private fun resume(state: State) {
        setState(state.container)
        state.container.startChildren(NoEvent)
    }

    /**
     * Gets an object implementing the debug interface. This allows the access to special functions which are mainly
     * provided for tests.
     */
    val debugInterface =
        object : Debug {
            override fun setState(state: State) = this@Fsm.setState(state.container)

            override fun resume(state: State) = this@Fsm.resume(state)

            override fun triggerSync(event: IEvent): Boolean = triggerEvent(event)

            override val stateDump: List<StateContainerBase<out IState>> = states

            override val initialState: InitialStateContainer = initial
        }

    companion object {
        /**
         * Checks whether the provided parameter is valid.
         * @param event The event parameter to check.
         */
        internal fun checkParameter(event: IEvent) {
            if (event::class == NoEvent::class) {
                throw FsmException("Fsm.trigger: A trigger event cannot be NoEvent!")
            }
        }
    }
}

/**
 * Class managing the states of a synchronous FSM (finite state machine).
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
    onTriggered: ((sender: Fsm, currentState: IState, event: IEvent, handled: Boolean) -> Unit),
    startState: StateContainerBase<out EndState>,
    otherStates: List<StateContainerBase<out IState>>,
) : Fsm(name, onStateChanged, onTriggered, startState, otherStates) {
    /**
     * Triggers a transition.
     * @param event The event occurred.
     * @return Returns true if the event was handled; false otherwise.
     */
    override fun trigger(event: IEvent): Boolean = triggerEvent(event)

    /**
     * Triggers a transition.
     * @param T The type of the data parameter.
     * @param event The event occurred.
     * @param data The data to send with the event.
     * @return Returns true if the event was handled; false otherwise.
     */
    fun <T : Any> trigger(
        event: Event,
        data: T,
    ): Boolean = trigger(DataEvent(data, event::class))
}

/**
 * Class managing the states of an asynchronous FSM (finite state machine).
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
    onTriggered: ((sender: Fsm, currentState: IState, event: IEvent, handled: Boolean) -> Unit),
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
     * @param event The event occurred.
     * @return Returns true.
     */
    override fun trigger(event: IEvent): Boolean {
        this.launch {
            mutex.withLock {
                triggerEvent(event)
            }
        }

        return true
    }

    /**
     * Triggers a transition.
     * @param T The type of the data parameter.
     * @param event The event occurred.
     * @param data The data to send with the event.
     * @return Returns true.
     */
    fun <T : Any> trigger(
        event: Event,
        data: T,
    ): Boolean = trigger(DataEvent(data, event::class))
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
    onTriggered: ((sender: Fsm, currentState: IState, event: IEvent, handled: Boolean) -> Unit),
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
    onTriggered: ((sender: Fsm, currentState: IState, event: IEvent, handled: Boolean) -> Unit),
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

/**
 * Encapsulates an initial state in a container.
 */
private fun InitialState.toContainer(): InitialStateContainer = InitialStateContainer(state = this, transitions = emptyList())

/**
 * Adds a new transition to the state.
 * @param stateTo A reference to the end point of this transition.
 * @return Returns a new state container.
 */
private fun InitialState.transition(stateTo: EndState): InitialStateContainer = toContainer().transition(stateTo)
