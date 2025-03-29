package io.github.frantoso.jasm

import java.util.Collections

/**
 * This class models the behavior of a state of the state machine. It encapsulates a state and stores all it's
 * transitions, children and action functions.
 * @param state
 * @param children Gets a list of all child state machines.
 * @param transitions Gets a list storing the transition information.
 * @param onEntry Gets the handler method for the states entry action.
 * @param onExit Gets the handler method for the states exit action.
 * @param onDoInState Gets the handler method for the states do in state action.
 */
abstract class StateContainerBase<TState : IState>(
    val state: TState,
    val children: List<FsmSync>,
    val transitions: List<ITransition>,
    val onEntry: IAction,
    val onExit: IAction,
    val onDoInState: IAction,
) {
    /**
     * The list of currently working child state machines.
     */
    private val activeChildren = Collections.synchronizedList(ArrayList<FsmSync>())

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
     */
    fun start() = start(NoEvent, History.None)

    /**
     * Calls the OnEntry handler of this state and starts all child FSMs if there are some.
     * @param T The data type of the data to provide to the entry function.
     * @param data The data to provide to the entry function.
     */
    fun <T : Any> start(data: T) = start(DataEvent(data, NoEvent::class), History.None)

    /**
     * Calls the OnEntry handler of this state and starts all child FSMs if there are some.
     * @param trigger The event which initiated this start.
     * @param history The kind of history to use.
     */
    fun start(
        trigger: IEvent,
        history: History,
    ) {
        if (history.isHistory) {
            if (tryStartHistory(trigger)) {
                return
            }
        }

        if (history.isDeepHistory && tryStartDeepHistory()) {
            return
        }

        onEntry.fire(trigger)
        startChildren()
    }

    /**
     * Let all direct child FSMs continue working. Calls start() if there is no active child.
     * @return Returns a value indicating whether the start was successful.
     */
    private fun tryStartHistory(trigger: IEvent): Boolean {
        if (!hasActiveChildren) {
            return false
        }

        children.forEach { child -> child.currentState.start(trigger, History.None) }

        return true
    }

    /**
     * Let all child FSMs continue working. Calls start() if there is no active child.
     * @return Returns a value indicating whether the start was successful.
     */
    private fun tryStartDeepHistory(): Boolean = hasActiveChildren

    /**
     * Triggers a transition.
     * @param trigger The event occurred.
     * @return Returns the data needed to proceed with the new state.
     */
    internal fun trigger(trigger: IEvent): ChangeStateData {
        val result = processChildren(trigger)

        return if (result.first) ChangeStateData(true) else processTransitions(result.second)
    }

    /**
     * Starts all registered sub state-machines.
     */
    internal fun startChildren() {
        activeChildren.clear()
        children.forEach { startChild(it) }
    }

    /**
     * Starts the specified child machine.
     * @param child The child machine to start.
     */
    private fun startChild(child: FsmSync) {
        // ToDo provide start with data
        activeChildren.add(child)
        child.start()
    }

    /**
     * Processes the transitions.
     * @param trigger The event occurred.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun processTransitions(trigger: IEvent): ChangeStateData {
        for (transition in transitions.filter { it.isAllowed(trigger) }) {
            return changeState(trigger, transition)
        }

        return ChangeStateData(false)
    }

    /**
     * Processes the child machines.
     * @param trigger The event occurred.
     * @return Returns as result true if the event was handled; false otherwise. Normally the returned trigger is the
     *      original one.
     *      Exception:
     *      If the last child machine went to the final state, the returned result is false (Pair.first) and
     *      the returned trigger is FsmEvent.NoEvent (Pair.second).
     */
    private fun processChildren(trigger: IEvent): Pair<Boolean, IEvent> {
        if (!hasActiveChildren) {
            return Pair(false, trigger)
        }

        val handled = triggerChildren(trigger)
        if (handled) {
            return Pair(true, trigger)
        }

        return Pair(false, if (hasActiveChildren) trigger else toNoEvent(trigger))
    }

    /**
     * Checks the trigger for data and converts it to a NoEvent instance.
     */
    private fun toNoEvent(trigger: IEvent): IEvent = (trigger as? DataEvent<*>)?.fromData(NoEvent::class) ?: NoEvent

    /**
     * Changes the state to the one stored in the transition object.
     * @param transition The actual transition.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun changeState(
        trigger: IEvent,
        transition: ITransition,
    ): ChangeStateData {
        onExit.fire(trigger)
        return ChangeStateData(!transition.isToFinal, transition.endPoint)
    }

    /**
     * Triggers the child machines.
     * @param trigger The event occurred.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChildren(trigger: IEvent): Boolean = activeChildren.toList().map { triggerChild(it, trigger) }.any { it }

    /**
     * Triggers the specified child machine.
     * @param child The child machine to trigger.
     * @param trigger The event occurred.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChild(
        child: FsmSync,
        trigger: IEvent,
    ): Boolean {
        val handled = child.trigger(trigger)

        if (child.hasFinished) {
            activeChildren.remove(child)
        }

        return handled
    }

    /**
     * This interface provides methods which are not intended to use for normal operation.
     * Use the methods for testing or diagnosis purposes.
     */
    interface Debug {
        /**
         * Gets a snapshot of the transitions.
         */
        val transitionDump: List<ITransition>

        /**
         * Gets a snapshot of the child machines.
         */
        val childDump: List<Fsm>
    }

    /**
     * Gets an identifier of this object. The id of the encapsulated state.
     */
    val id = state.id

    /**
     * Gets an object implementing the debug interface. This allows the access to special functions which are mainly
     * provided for test and diagnosis.
     */
    val debugInterface =
        object : Debug {
            override val transitionDump: List<ITransition> get() = transitions.toList()

            override val childDump: List<Fsm> get() = children.toList()
        }
}

/**
 * The container for normal states.
 * @param state The state to encapsulate.
 * @param children Gets a list of all child state machines.
 * @param transitions Gets a list storing the transition information.
 * @param onEntry Gets the handler method for the states entry action.
 * @param onExit Gets the handler method for the states exit action.
 * @param onDoInState Gets the handler method for the states do in state action.
 */
class StateContainer(
    state: State,
    children: List<FsmSync>,
    transitions: List<ITransition>,
    onEntry: IAction,
    onExit: IAction,
    onDoInState: IAction,
) : StateContainerBase<State>(state, children, transitions, onEntry, onExit, onDoInState) {
    /**
     * Adds a new child machine.
     * @param stateMachine The child machine to add.
     * @return Returns a new state container.
     */
    fun child(stateMachine: FsmSync): StateContainer =
        StateContainer(
            state = state,
            children = children + listOf(stateMachine),
            transitions = transitions,
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds new child machines.
     * @param stateMachines The children to add.
     * @return Returns a new state container.
     */
    fun children(stateMachines: List<FsmSync>): StateContainer =
        StateContainer(
            state = state,
            children = children + stateMachines,
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
    fun entry(action: IAction): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions,
            onEntry = action,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Sets the handler method for the states entry action.
     * @param T The type of the action's parameter.
     * @param action The handler method for the states entry action.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> entry(noinline action: (T?) -> Unit): StateContainer = entry(DataAction(T::class, action))

    /**
     * Sets the handler method for the states entry action.
     * @param action The handler method for the states entry action.
     * @return Returns a new state container.
     */
    fun entry(action: () -> Unit): StateContainer = entry(Action(action))

    /**
     * Sets the handler method for the states exit action.
     * @param action The handler method for the states exit action.
     * @return Returns a new state container.
     */
    fun exit(action: IAction): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions,
            onEntry = onEntry,
            onExit = action,
            onDoInState = onDoInState,
        )

    /**
     * Sets the handler method for the states exit action.
     * @param T The type of the action's parameter.
     * @param action The handler method for the states entry action.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> exit(noinline action: (T?) -> Unit): StateContainer = exit(DataAction(T::class, action))

    /**
     * Sets the handler method for the states exit action.
     * @param action The handler method for the states entry action.
     * @return Returns a new state container.
     */
    fun exit(action: () -> Unit): StateContainer = exit(Action(action))

    /**
     * Sets the handler method for the states do in state action.
     * @param action The handler method for the states exit action.
     * @return Returns a new state container.
     */
    fun doInState(action: IAction): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions,
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = action,
        )

    /**
     * Sets the handler method for the states do in state action.
     * @param action The handler method for the states exit action.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> doInState(noinline action: (T?) -> Unit): StateContainer = doInState(DataAction(T::class, action))

    fun doInState(action: () -> Unit): StateContainer = doInState(Action(action))

    /**
     * Adds a new transition to the state.
     * @param E The Event that initiates this transition.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event> transition(
        stateTo: EndState,
        noinline guard: () -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + Transition(E::class, stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the state.
     * @param E The Event that initiates this transition.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event, reified T : Any> transition(
        stateTo: EndState,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + DataTransition(E::class, T::class, stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition without event to a nested state. The event 'NoEvent' is automatically used.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    fun transitionWithoutEvent(
        stateTo: EndState,
        guard: () -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + Transition(NoEvent::class, stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition without event to a nested state. The event 'NoEvent' is automatically used.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> transitionWithoutEvent(
        stateTo: EndState,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + DataTransition(NoEvent::class, T::class, stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the state.
     * @param E The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event> transition(
        endPoint: TransitionEndPoint,
        noinline guard: () -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + Transition(E::class, endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the state.
     * @param E The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event, reified T : Any> transition(
        endPoint: TransitionEndPoint,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + DataTransition(E::class, T::class, endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition without event to a nested state. The event 'NoEvent' is automatically used.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    fun transitionWithoutEvent(
        endPoint: TransitionEndPoint,
        guard: () -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + Transition(NoEvent::class, endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition without event to a nested state. The event 'NoEvent' is automatically used.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> transitionWithoutEvent(
        endPoint: TransitionEndPoint,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + DataTransition(NoEvent::class, T::class, endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the final state.
     * @param E The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event, reified T : Any> transitionToFinal(noinline guard: (T?) -> Boolean = { true }): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + DataTransition(E::class, T::class, FinalState(), guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )

    /**
     * Adds a new transition to the final state.
     * @param E The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event> transitionToFinal(noinline guard: () -> Boolean = { true }): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + Transition(E::class, FinalState(), guard),
            onEntry = onEntry,
            onExit = onExit,
            onDoInState = onDoInState,
        )
}

/**
 * This class represents a particular state of the state machine.
 * @param state The initial state to encapsulate.
 * @param transitions Gets a list storing the transition information.
 */
class InitialStateContainer(
    state: InitialState,
    transitions: List<ITransition>,
) : StateContainerBase<InitialState>(state, emptyList(), transitions, NoAction, NoAction, NoAction) {
    /**
     * Adds a new transition to the state.
     * @param stateTo A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    fun transition(stateTo: EndState): InitialStateContainer =
        InitialStateContainer(
            state = state,
            transitions = listOf(Transition(StartEvent::class, stateTo) { true }),
        )
}

/**
 * This class represents a particular state of the state machine.
 * @param state The final state to encapsulate.
 */
class FinalStateContainer(
    state: FinalState,
) : StateContainerBase<FinalState>(state, emptyList(), emptyList(), NoAction, NoAction, NoAction)

/**
 * Encapsulates a normal state in a container.
 */
fun State.with(): StateContainer =
    StateContainer(
        state = this,
        children = emptyList(),
        transitions = emptyList(),
        onEntry = NoAction,
        onExit = NoAction,
        onDoInState = NoAction,
    )

/**
 * Encapsulates a final state in a container.
 */
fun FinalState.with(): FinalStateContainer = FinalStateContainer(state = this)

/**
 * Encapsulates an initial state in a container.
 */
fun InitialState.with(): InitialStateContainer = InitialStateContainer(state = this, transitions = emptyList())
