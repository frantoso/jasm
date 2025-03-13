package de.franklisting.fsm

/**
 * This class models the behavior of a state of the state machine. It encapsulates a state and stores all it's
 * transitions, children and action functions.
 * @param TData The type of data provided to the condition and action handlers.
 * @param state
 * @param children Gets a list of all child state machines.
 * @param transitions Gets a list storing the transition information.
 * @param onEntry Gets the handler method for the states entry action.
 * @param onExit Gets the handler method for the states exit action.
 * @param onDoInState Gets the handler method for the reaction in the state.
 */
abstract class StateContainerBase<TData, TState : IState>(
    val state: TState,
    protected val children: List<FsmSync<TData>>,
    internal val transitions: List<Transition<TData>>,
    protected val onEntry: (TData) -> Unit,
    protected val onExit: (TData) -> Unit,
    protected val onDoInState: (TData) -> Unit,
) {
    /**
     * The list of currently working child state machines.
     */
    private val activeChildren = ArrayList<FsmSync<TData>>()

    /**
     * Gets a value indicating whether this state has active child machines.
     */
    private val hasActiveChildren: Boolean
        get() = activeChildren.isNotEmpty()

    /**
     * Gets the name of the enclosed state.
     */
    val name = state.name

    /**
     * Gets a value indicating whether this state has transitions.
     */
    val hasTransitions = transitions.isNotEmpty()

    /**
     * Gets a value indicating whether this state has child machines.
     */
    val hasChildren = children.isNotEmpty()

    /**
     * Calls the OnEntry handler of this state and starts all child FSMs if there are some.
     * @param data The data object.
     * @param history The kind of history to use.
     */
    fun start(
        data: TData,
        history: History,
    ) {
        if (history.isHistory && tryStartHistory(data)) {
            return
        }

        if (history.isDeepHistory && tryStartDeepHistory()) {
            return
        }

        fireOnEntry(data)
        startChildren(data)
    }

    /**
     * Let all direct child FSMs continue working. Calls start() if there is no active child.
     * @param data The data object.
     * @return Returns a value indicating whether the start was successful.
     */
    private fun tryStartHistory(data: TData): Boolean {
        if (!hasActiveChildren) {
            return false
        }

        children.forEach { child -> child.currentState.start(data, History.None) }

        return true
    }

    /**
     * Let all child FSMs continue working. Calls start() if there is no active child.
     * @return Returns a value indicating whether the start was successful.
     */
    private fun tryStartDeepHistory(): Boolean = hasActiveChildren

    /**
     * Fires the Do event.
     * @param data The data object.
     */
    fun fireDoInState(data: TData) {
        activeChildren.forEach { child -> child.doAction(data) }
        fire(onDoInState, data, "State.OnDo")
    }

    /**
     * Triggers a transition.
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns the data needed to proceed with the new state.
     */
    internal fun trigger(
        trigger: Event,
        data: TData,
    ): ChangeStateData {
        val result = processChildren(trigger, data)
        return if (result.first) ChangeStateData(true) else processTransitions(result.second, data)
    }

    /**
     * Starts all registered sub state-machines.
     * @param data The data object.
     */
    internal fun startChildren(data: TData) {
        activeChildren.clear()
        children.forEach { startChild(it, data) }
    }

    /**
     * Starts the specified child machine.
     * @param child The child machine to start.
     * @param data The data object.
     */
    private fun startChild(
        child: FsmSync<TData>,
        data: TData,
    ) {
        activeChildren.add(child)
        child.start(data)
    }

    /**
     * Processes the transitions.
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun processTransitions(
        trigger: Event,
        data: TData,
    ): ChangeStateData {
        for (transition in transitions.filter { it.trigger == trigger && it.guard(data) }) {
            return changeState(transition, data)
        }

        return ChangeStateData(false)
    }

    /**
     * Processes the child machines.
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns as result true if the event was handled; false otherwise. Normally the returned trigger is the
     *      original one.
     *      Exception:
     *      If the last child machine went to the final state, the returned result is false (Pair.first) and
     *      the returned trigger is FsmEvent.NoEvent (Pair.second).
     */
    private fun processChildren(
        trigger: Event,
        data: TData,
    ): Pair<Boolean, Event> {
        if (!hasActiveChildren) {
            return Pair(false, trigger)
        }

        val handled = triggerChildren(trigger, data)
        if (handled) {
            return Pair(true, trigger)
        }

        return Pair(false, if (hasActiveChildren) trigger else NoEvent)
    }

    /**
     * Changes the state to the one stored in the transition object.
     * @param transition The actual transition.
     * @param data The data provided to the condition and action handlers.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun changeState(
        transition: Transition<TData>,
        data: TData,
    ): ChangeStateData {
        fireOnExit(data)
        return ChangeStateData(!transition.isToFinal, transition.endPoint)
    }

    /**
     * Triggers the child machines.
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChildren(
        trigger: Event,
        data: TData,
    ): Boolean {
        var handled = false
        activeChildren
            .toList()
            .forEach { handled = handled or triggerChild(it, trigger, data) }

        return handled
    }

    /**
     * Triggers the specified child machine.
     * @param child The child machine to trigger.
     * @param trigger The event occurred.
     * @param data The data object.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChild(
        child: FsmSync<TData>,
        trigger: Event,
        data: TData,
    ): Boolean {
        val handled = child.trigger(trigger, data)

        if (child.hasFinished) {
            activeChildren.remove(child)
        }

        return handled
    }

    /**
     * Fires the OnEntry event.
     * @param data The data object.
     */
    private fun fireOnEntry(data: TData) = fire(onEntry, data, "State.OnEntry")

    /**
     * Fires the OnExit event.
     * @param data The data object.
     */
    private fun fireOnExit(data: TData) = fire(onExit, data, "State.OnExit")

    /**
     * Calls one of the actions of the state.
     * @param handler Action to perform.
     * @param data The data object.
     * @param actionName The name of the action for error processing.
     */
    private fun fire(
        handler: (TData) -> Unit,
        data: TData,
        actionName: String,
    ) {
        try {
            handler(data)
        } catch (ex: Throwable) {
            throw FsmException("Error calling the $actionName action", state.name, ex)
        }
    }

    /**
     * This interface provides methods which are not intended to use for normal operation.
     * Use the methods for testing or diagnosis purposes.
     */
    interface Debug<T> {
        /**
         * Gets a snapshot of the transitions.
         */
        val transitionDump: List<Transition<T>>

        /**
         * Gets a snapshot of the child machines.
         */
        val childDump: List<Fsm<T>>
    }

    /**
     * Gets a unique identifier of this object.
     */
    val id = super.toString()

    /**
     * Gets an object implementing the debug interface. This allows the access to special functions which are mainly
     * provided for test and diagnosis.
     */
    val debugInterface =
        object : Debug<TData> {
            override val transitionDump: List<Transition<TData>> get() = transitions.toList()

            override val childDump: List<Fsm<TData>> get() = children.toList()
        }

    companion object {
        /**
         * Creates a new transition.
         * @param trigger The Event that initiates this transition.
         * @param endPoint A reference to the end point of this transition.
         * @param guard Condition handler of this transition.
         * @return Returns the new transition as list.
         */
        internal fun <T> createTransition(
            trigger: Event,
            endPoint: TransitionEndPoint,
            guard: (T) -> Boolean,
        ): List<Transition<T>> = listOf(Transition(trigger, endPoint, guard))
    }
}

/**
 * The container for normal states.
 * @param TData The type of data provided to the condition and action handlers.
 * @param state The state to encapsulate.
 * @param children Gets a list of all child state machines.
 * @param transitions Gets a list storing the transition information.
 * @param onEntry Gets the handler method for the states entry action.
 * @param onExit Gets the handler method for the states exit action.
 * @param onDoInState Gets the handler method for the reaction in the state.
 */
class StateContainer<TData>(
    state: State,
    children: List<FsmSync<TData>>,
    transitions: List<Transition<TData>>,
    onEntry: (TData) -> Unit,
    onExit: (TData) -> Unit,
    onDoInState: (TData) -> Unit,
) : StateContainerBase<TData, State>(state, children, transitions, onEntry, onExit, onDoInState) {
    /**
     * Adds a new child machine.
     * @param fsm The child to add.
     * @return Returns a new state container.
     */
    fun child(fsm: FsmSync<TData>): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children + listOf(fsm),
            transitions = transitions,
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Sets the handler method for the states entry action.
     * @param action The handler method for the states entry action.
     * @return Returns a new state container.
     */
    fun entry(action: (TData) -> Unit): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions,
            onEntry = action,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Sets the handler method for the states exit action.
     * @param action The handler method for the states exit action.
     * @return Returns a new state container.
     */
    fun exit(action: (TData) -> Unit): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions,
            onEntry = onEntry,
            onExit = action,
            onDoInState = onDoInState,
        )

    /**
     * Sets the handler method for the reaction in the state.
     * @param action The handler method for the states do action.
     * @return Returns a new state container.
     */
    fun doInState(action: (TData) -> Unit): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions,
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = action,
        )

    /**
     * Adds a new transition to the state.
     * @param trigger The Event that initiates this transition.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    fun transition(
        trigger: Event,
        stateTo: EndState,
        guard: (TData) -> Boolean = { true },
    ): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + createTransition(trigger, TransitionEndPoint(stateTo), guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the state.
     * @param trigger The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    fun transition(
        trigger: Event,
        endPoint: TransitionEndPoint,
        guard: (TData) -> Boolean = { true },
    ): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + createTransition(trigger, endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the final state.
     * @param trigger The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    fun transitionToFinal(
        trigger: Event,
        guard: (TData) -> Boolean = { true },
    ): StateContainer<TData> =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + createTransition(trigger, TransitionEndPoint(FinalState()), guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    companion object {
        /**
         * Adds a new transition to the state.
         * @param trigger The Event that initiates this transition.
         * @param endPoint A reference to the end point of this transition.
         * @param guard Condition handler of this transition.
         */
        internal fun <T> createTransition(
            trigger: Event,
            endPoint: TransitionEndPoint,
            guard: (T) -> Boolean,
        ): List<Transition<T>> = listOf(Transition(trigger, endPoint, guard))
    }
}

/**
 * This class represents a particular state of the state machine.
 * @param TData The type of data provided to the condition and action handlers.
 * @param state The initial state to encapsulate.
 * @param transitions Gets a list storing the transition information.
 */
class InitialStateContainer<TData>(
    state: InitialState,
    transitions: List<Transition<TData>>,
) : StateContainerBase<TData, InitialState>(state, emptyList(), transitions, {}, {}, {}) {
    /**
     * Adds a new transition to the state.
     * @param stateTo A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    fun transition(stateTo: State): InitialStateContainer<TData> =
        InitialStateContainer(
            state = state,
            transitions = transitions + listOf(Transition(StartEvent, TransitionEndPoint(stateTo)) { true }),
        )

    /**
     * Adds a new transition to the state.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    fun transition(endPoint: TransitionEndPoint): InitialStateContainer<TData> =
        InitialStateContainer(
            state = state,
            transitions = transitions + listOf(Transition(StartEvent, endPoint) { true }),
        )
}

/**
 * This class represents a particular state of the state machine.
 * @param TData The type of data provided to the condition and action handlers.
 * @param state The final state to encapsulate.
 */
class FinalStateContainer<TData>(
    state: FinalState,
) : StateContainerBase<TData, FinalState>(state, emptyList(), emptyList(), {}, {}, {})

/**
 * Encapsulates a normal state in a container.
 */
fun <T> State.toContainer(): StateContainer<T> =
    StateContainer(
        state = this,
        children = emptyList(),
        transitions = emptyList(),
        onEntry = {},
        onExit = {},
        onDoInState = {},
    )

/**
 * Encapsulates a final state in a container.
 */
fun <T> FinalState.toContainer(): FinalStateContainer<T> = FinalStateContainer(state = this)

/**
 * Adds a new transition to the state.
 * @param stateTo A reference to the end point of this transition.
 * @return Returns a new state container.
 */
fun <T> InitialState.transition(stateTo: EndState): InitialStateContainer<T> {
    val t = listOf(Transition<T>(StartEvent, TransitionEndPoint(stateTo)) { true })
    return InitialStateContainer(
        state = this,
        transitions = t,
    )
}

/**
 * Adds a new child machine.
 * @param fsm The child to add.
 * @return Returns a new state container.
 */
fun <T> State.child(fsm: FsmSync<T>): StateContainer<T> =
    StateContainer(
        state = this,
        children = listOf(fsm),
        transitions = emptyList(),
        onEntry = {},
        onExit = {},
        onDoInState = {},
    )

/**
 * Adds a new transition to the state.
 * @param trigger The Event that initiates this transition.
 * @param stateTo A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
fun <T> State.transition(
    trigger: Event,
    stateTo: EndState,
    guard: (T) -> Boolean = { true },
): StateContainer<T> {
    val t = StateContainerBase.createTransition(trigger, TransitionEndPoint(stateTo), guard)
    return StateContainer(
        state = this,
        children = emptyList(),
        transitions = t,
        onEntry = {},
        onExit = {},
        onDoInState = {},
    )
}

/**
 * Adds a new transition to the final state.
 * @param trigger The Event that initiates this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a state container.
 */
fun <T> State.transitionToFinal(
    trigger: Event,
    guard: (T) -> Boolean = { true },
): StateContainer<T> {
    val t = StateContainerBase.createTransition(trigger, TransitionEndPoint(FinalState()), guard)
    return StateContainer(
        state = this,
        children = emptyList(),
        transitions = t,
        onEntry = {},
        onExit = {},
        onDoInState = {},
    )
}

/**
 * Adds a new transition to the state.
 * @param trigger The Event that initiates this transition.
 * @param guard Condition handler of this transition.
 * @param endPoint A reference to the end point of this transition.
 * @return Returns a new state container.
 */
fun <T> State.transition(
    trigger: Event,
    endPoint: TransitionEndPoint,
    guard: (T) -> Boolean = { true },
): StateContainer<T> {
    val t = StateContainerBase.createTransition(trigger, endPoint, guard)
    return StateContainer(
        state = this,
        children = emptyList(),
        transitions = t,
        onEntry = {},
        onExit = {},
        onDoInState = {},
    )
}

/**
 * Sets the handler method for the states entry action.
 * @param action The handler method for the states entry action.
 * @return Returns a new state container.
 */
fun <T> State.entry(action: (T) -> Unit): StateContainer<T> =
    StateContainer(
        state = this,
        children = emptyList(),
        transitions = emptyList(),
        onEntry = action,
        onExit = {},
        onDoInState = {},
    )

/**
 * Sets the handler method for the states exit action.
 * @param action The handler method for the states exit action.
 * @return Returns a new state container.
 */
fun <T> State.exit(action: (T) -> Unit): StateContainer<T> =
    StateContainer(
        state = this,
        children = emptyList(),
        transitions = emptyList(),
        onEntry = {},
        onExit = action,
        onDoInState = {},
    )

/**
 * Sets the handler method for the states do action.
 * @param action The handler method for the states do action.
 * @return Returns a new state container.
 */
fun <T> State.doInState(action: (T) -> Unit): StateContainer<T> =
    StateContainer(
        state = this,
        children = emptyList(),
        transitions = emptyList(),
        onEntry = {},
        onExit = {},
        onDoInState = action,
    )
