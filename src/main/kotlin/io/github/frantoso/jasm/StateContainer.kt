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
 */
abstract class StateContainerBase<TState : IState>(
    val state: TState,
    val children: List<FsmSync>,
    val transitions: List<ITransition>,
    val onEntry: IAction,
    val onExit: IAction,
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
    fun start() = start(NoEvent(), History.None)

    /**
     * Calls the OnEntry handler of this state and starts all child FSMs if there are some.
     */
    fun <T : Any> start(data: T) = start(DataNoEvent(data), History.None)

    /**
     * Calls the OnEntry handler of this state and starts all child FSMs if there are some.
     * @param trigger The event which initiated this start.
     * @param history The kind of history to use.
     */
    fun start(
        trigger: Event,
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
    private fun tryStartHistory(trigger: Event): Boolean {
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
    internal fun trigger(trigger: Event): ChangeStateData {
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
    private fun processTransitions(trigger: Event): ChangeStateData {
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
    private fun processChildren(trigger: Event): Pair<Boolean, Event> {
        if (!hasActiveChildren) {
            return Pair(false, trigger)
        }

        val handled = triggerChildren(trigger)
        if (handled) {
            return Pair(true, trigger)
        }

        return Pair(false, if (hasActiveChildren) trigger else NoEvent())
    }

    /**
     * Changes the state to the one stored in the transition object.
     * @param transition The actual transition.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun changeState(
        trigger: Event,
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
    private fun triggerChildren(trigger: Event): Boolean = activeChildren.toList().map { triggerChild(it, trigger) }.any { it }

    /**
     * Triggers the specified child machine.
     * @param child The child machine to trigger.
     * @param trigger The event occurred.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChild(
        child: FsmSync,
        trigger: Event,
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
 */
class StateContainer(
    state: State,
    children: List<FsmSync>,
    transitions: List<ITransition>,
    onEntry: IAction,
    onExit: IAction,
) : StateContainerBase<State>(state, children, transitions, onEntry, onExit) {
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
        )

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
        )

    /**
     * Adds a new transition to the state.
     * @param trigger The Event that initiates this transition.
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
            transitions = transitions + transitionX<E>(stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition to the state.
     * @param trigger The Event that initiates this transition.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : DataEvent<T>, reified T : Any> transition(
        stateTo: EndState,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + transitionX<E, T>(stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition without event to a nested state. The event 'NoEvent' is automatically used.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> transitionT(
        stateTo: EndState,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + transitionX<DataNoEvent<T>, T>(stateTo, guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition to the state.
     * @param trigger The Event that initiates this transition.
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
            transitions = transitions + transitionX<E>(endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition to the state.
     * @param trigger The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : DataEvent<T>, reified T : Any> transition(
        endPoint: TransitionEndPoint,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + transitionX<E, T>(endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition without event to a nested state. The event 'NoEvent' is automatically used.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified T : Any> transitionT(
        endPoint: TransitionEndPoint,
        noinline guard: (T?) -> Boolean = { true },
    ): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + transitionX<DataNoEvent<T>, T>(endPoint, guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition to the final state.
     * @param trigger The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : DataEvent<T>, reified T : Any> transitionToFinal(noinline guard: (T?) -> Boolean = { true }): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + transitionX<E, T>(FinalState(), guard),
            onEntry = onEntry,
            onExit = onExit,
        )

    /**
     * Adds a new transition to the final state.
     * @param trigger The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a new state container.
     */
    inline fun <reified E : Event> transitionToFinal(noinline guard: () -> Boolean = { true }): StateContainer =
        StateContainer(
            state = state,
            children = children,
            transitions = transitions + transitionX<E>(FinalState(), guard),
            onEntry = onEntry,
            onExit = onExit,
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
) : StateContainerBase<InitialState>(state, emptyList(), transitions, NoAction, NoAction) {
    /**
     * Adds a new transition to the state.
     * @param stateTo A reference to the end point of this transition.
     * @return Returns a new state container.
     */
    fun transition(stateTo: EndState): InitialStateContainer =
        InitialStateContainer(
            state = state,
            transitions = listOf(transitionX<StartEvent>(stateTo) { true }),
        )
}

/**
 * This class represents a particular state of the state machine.
 * @param state The final state to encapsulate.
 */
class FinalStateContainer(
    state: FinalState,
) : StateContainerBase<FinalState>(state, emptyList(), emptyList(), NoAction, NoAction)

/**
 * Encapsulates a normal state in a container.
 */
fun State.use(): StateContainer =
    StateContainer(
        state = this,
        children = emptyList(),
        transitions = emptyList(),
        onEntry = NoAction,
        onExit = NoAction,
    )

/**
 * Encapsulates a final state in a container.
 */
fun FinalState.use(): FinalStateContainer = FinalStateContainer(state = this)

/**
 * Encapsulates an initial state in a container.
 */
fun InitialState.use(): InitialStateContainer = InitialStateContainer(state = this, transitions = emptyList())
