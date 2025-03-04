package de.franklisting.fsm

/**
 * This class represents a particular state of the state machine.
 * Initializes a new instance of the State class.
 *
 * @param T The type of data provided to the condition and action handlers.
 * @param name The name of the state.
 * @param type The type of the state; default: [StateType.Normal].
 */
open class State<T> protected constructor(
    val name: String,
    private val type: StateType,
) {
    /**
     * Initializes a new instance of the State class as normal state.
     *
     * @param name The name of the state.
     */
    constructor(name: String) : this(name, StateType.Normal)

    /**
     * The list of currently working child state machines.
     */
    private val activeChildren = ArrayList<FsmSync<T>>()

    /**
     * The list of all child state machines.
     */
    private val children = ArrayList<FsmSync<T>>()

    /**
     * Map storing the transition information.
     */
    private val transitions = ArrayList<Transition<T>>()

    /**
     * Possible state types.
     */
    protected enum class StateType {
        /**
         * Marks a normal state.
         */
        Normal,

        /**
         * Marks the initial pseudo state.
         */
        Initial,

        /**
         * Marks the final pseudo state.
         */
        Final,

        /**
         * Marks the state as invalid.
         */
        Invalid,
    }

    /**
     * Gets a value indicating whether this instance is the initial state.
     */
    val isInitial = type == StateType.Initial

    /**
     * Gets a value indicating whether this instance is the final state.
     */
    val isFinal = type == StateType.Final

    /**
     * Gets a value indicating whether this instance is an invalid state.
     */
    val isInvalid = type == StateType.Invalid

    /**
     * Gets the deep history transition end point for this state.
     */
    val deepHistory: TransitionEndPoint<T> by lazy { TransitionEndPoint(this, History.Hd) }

    /**
     * Gets the history transition end point for this state.
     */
    val history: TransitionEndPoint<T> by lazy { TransitionEndPoint(this, History.H) }

    /**
     * Gets or sets the handler method for the states entry action.
     */
    private var onEntry: (T) -> Unit = {}

    /**
     * Gets or sets the handler method for the states exit action.
     */
    private var onExit: (T) -> Unit = {}

    /**
     * Gets or sets the handler method for the reaction in the state.
     */
    private var onDoInState: (T) -> Unit = {}

    /**
     * Gets a value indicating whether this state has active child machines.
     */
    private val hasActiveChildren: Boolean
        get() = activeChildren.isNotEmpty()

    /**
     * Gets a value indicating whether this state has active child machines.
     */
    val hasTransitions: Boolean
        get() = transitions.isNotEmpty()

    /**
     * Calls the OnEntry handler of this state and starts all child FSMs if there are some.
     *
     * @param data The data object.
     * @param history The kind of history to use.
     */
    fun start(
        data: T,
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
    private fun tryStartHistory(data: T): Boolean {
        if (!hasActiveChildren) {
            return false
        }

        children.forEach { child -> child.currentState.start(data, History.None) }

        return true
    }

    /**
     * Let all child FSMs continue working. Calls start() if there is no active child.
     *
     * @return Returns a value indicating whether the start was successful.
     */
    private fun tryStartDeepHistory(): Boolean = hasActiveChildren

    /**
     * Adds a new child machine.
     *
     * @param fsm The child to add.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun child(fsm: FsmSync<T>): State<T> {
        checkSpecialStateUsage("children")
        children.add(fsm)
        return this
    }

    /**
     * Sets the handler method for the states entry action.
     *
     * @param action The handler method for the states entry action.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun entry(action: (T) -> Unit): State<T> {
        checkSpecialStateUsage("entry actions")
        onEntry = action
        return this
    }

    /**
     * Sets the handler method for the states exit action.
     *
     * @param action The handler method for the states exit action.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun exit(action: (T) -> Unit): State<T> {
        checkSpecialStateUsage("exit actions")
        onExit = action
        return this
    }

    /**
     * Sets the handler method for the reaction in the state.
     *
     * @param action The handler method for the states do action.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun doInState(action: (T) -> Unit): State<T> {
        checkSpecialStateUsage("do actions")
        onDoInState = action
        return this
    }

    /**
     * Adds a new transition to the state.
     *
     * @param trigger The Event that initiates this transition.
     * @param stateTo A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun transition(
        trigger: Event,
        stateTo: State<T>,
        guard: (T) -> Boolean = { true },
    ): State<T> {
        addTransition(trigger, TransitionEndPoint(stateTo), guard)
        return this
    }

    /**
     * Adds a new transition to the state.
     *
     * @param trigger The Event that initiates this transition.
     * @param guard Condition handler of this transition.
     * @param endPoint A reference to the end point of this transition.
     * @return Returns a reference to the state object to support method chaining.
     */
    fun transition(
        trigger: Event,
        endPoint: TransitionEndPoint<T>,
        guard: (T) -> Boolean = { true },
    ): State<T> {
        addTransition(trigger, endPoint, guard)
        return this
    }

    /**
     * Returns a String that represents the current Object.
     */
    override fun toString(): String = name

    /**
     * Fires the Do event.
     *
     * @param data The data object.
     */
    fun fireDoInState(data: T) {
        activeChildren.forEach { child -> child.doAction(data) }
        fire(onDoInState, data, "State.OnDo")
    }

    /**
     * Triggers a transition.
     *
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns the data needed to proceed with the new state.
     */
    internal fun trigger(
        trigger: Event,
        data: T,
    ): ChangeStateData<T> {
        val result = processChildren(trigger, data)
        return if (result.first) ChangeStateData(true) else processTransitions(result.second, data)
    }

    /**
     * Adds a new transition to the state.
     *
     * @param trigger The Event that initiates this transition.
     * @param endPoint A reference to the end point of this transition.
     * @param guard Condition handler of this transition.
     */
    private fun addTransition(
        trigger: Event,
        endPoint: TransitionEndPoint<T>,
        guard: (T) -> Boolean,
    ) {
        checkSpecialStateTransitionUsage(endPoint.state)

        val transition = Transition(trigger, endPoint, guard)
        transitions.add(transition)
    }

    /**
     * Starts all registered sub state-machines.
     *
     * @param data The data object.
     */
    private fun startChildren(data: T) {
        activeChildren.clear()
        children.forEach { startChild(it, data) }
    }

    /**
     * Starts the specified child machine.
     *
     * @param child The child machine to start.
     * @param data The data object.
     */
    private fun startChild(
        child: FsmSync<T>,
        data: T,
    ) {
        activeChildren.add(child)
        child.start(data)
    }

    /**
     * Checks the special state usage.
     *
     * @param stateTo A reference to the end point of the transition.
     */
    private fun checkSpecialStateTransitionUsage(stateTo: State<T>) {
        if (isFinal) {
            throw FsmException("The final state cannot be the start point of a transition!", name)
        }

        if (stateTo.isInitial) {
            throw FsmException("The start state cannot be the end point of a transition!", name)
        }

        if (stateTo.isInvalid) {
            throw FsmException("An invalid state cannot be the end point of a transition!", name)
        }
    }

    /**
     * Checks special states for action usage.
     *
     * @param errorLocation The location of the error.
     * @exception FsmException
     *      The start state cannot have any {errorLocation}
     *      or
     *      The final state cannot have any {errorLocation}
     */
    private fun checkSpecialStateUsage(errorLocation: String) {
        if (isInitial) {
            throw FsmException("The start state cannot have any $errorLocation!", name)
        }

        if (isFinal) {
            throw FsmException("The final state cannot have any $errorLocation!", name)
        }
    }

    /**
     * Processes the transitions.
     *
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun processTransitions(
        trigger: Event,
        data: T,
    ): ChangeStateData<T> {
        for (transition in transitions.filter { it.trigger == trigger && it.guard(data) }) {
            return changeState(transition, data)
        }

        return ChangeStateData(false)
    }

    /**
     * Processes the child machines.
     *
     *
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
        data: T,
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
     *
     * @param transition The actual transition.
     * @param data The data provided to the condition and action handlers.
     * @return Returns the data needed to proceed with the new state.
     */
    private fun changeState(
        transition: Transition<T>,
        data: T,
    ): ChangeStateData<T> {
        fireOnExit(data)
        return ChangeStateData(!transition.isToFinal, transition.endPoint)
    }

    /**
     * Triggers the child machines.
     *
     * @param trigger The event occurred.
     * @param data The data provided to the condition and action handlers.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChildren(
        trigger: Event,
        data: T,
    ): Boolean {
        var handled = false
        activeChildren
            .toList()
            .forEach { handled = handled or triggerChild(it, trigger, data) }

        return handled
    }

    /**
     * Triggers the specified child machine.
     *
     * @param child The child machine to trigger.
     * @param trigger The event occurred.
     * @param data The data object.
     * @return Returns true if the event was handled; false otherwise.
     */
    private fun triggerChild(
        child: FsmSync<T>,
        trigger: Event,
        data: T,
    ): Boolean {
        val handled = child.trigger(trigger, data)

        if (child.hasFinished) {
            activeChildren.remove(child)
        }

        return handled
    }

    /**
     * Fires the OnEntry event.
     *
     * @param data The data object.
     */
    private fun fireOnEntry(data: T) = fire(onEntry, data, "State.OnEntry")

    /**
     * Fires the OnExit event.
     *
     * @param data The data object.
     */
    private fun fireOnExit(data: T) = fire(onExit, data, "State.OnExit")

    /**
     * Calls one of the actions of the state.
     *
     * @param handler Action to perform.
     * @param data The data object.
     * @param action The name of the action for error processing.
     */
    private fun fire(
        handler: (T) -> Unit,
        data: T,
        action: String,
    ) {
        try {
            handler(data)
        } catch (ex: Throwable) {
            val message = "Error calling the $action action"
            throw FsmException(message, name, ex)
        }
    }
}

/**
 * A class to model the special state 'initial'.
 * Initializes a new instance of the InitialState class.
 *
 * @param T The type of data provided to the condition and action handlers.
 */
internal class InitialState<T> : State<T>(StateType.Initial.toString(), StateType.Initial)

/**
 * A class to model the special state 'final'.
 * Initializes a new instance of the FinalState class.
 *
 * @param T The type of data provided to the condition and action handlers.
 */
internal class FinalState<T> : State<T>(StateType.Final.toString(), StateType.Final)

/**
 * A class to model the special state 'final'.
 * Initializes a new instance of the FinalState class.
 *
 * @param T The type of data provided to the condition and action handlers.
 */
internal class InvalidState<T> : State<T>(StateType.Invalid.toString(), StateType.Invalid)
