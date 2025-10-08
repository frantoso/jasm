package io.github.frantoso.jasm

/**
 * Encapsulates a normal state in a container.
 */
fun State.toContainer(): StateContainer =
    StateContainer(
        state = this,
        children = (this as? CompositeState)?.subMachines ?: emptyList(),
        transitions = emptyList(),
        onEntry = NoAction,
        onExit = NoAction,
        onDoInState = NoAction,
    )

/**
 * Adds a new child machine.
 * @param stateMachine The child machine to add.
 * @return Returns a new state container.
 */
fun State.child(stateMachine: FsmSync): StateContainer = toContainer().child(stateMachine)

/**
 * Adds new child machines.
 * @param stateMachines The children to add.
 * @return Returns a new state container.
 */
fun State.children(stateMachines: List<FsmSync>): StateContainer = toContainer().children(stateMachines)

/**
 * Sets the handler method for the state entry action.
 * @param T The type of the action's parameter.
 * @param action The handler method for the state entry action.
 * @return Returns a new state container.
 */
inline fun <reified T : Any> State.entry(noinline action: (T?) -> Unit): StateContainer = toContainer().entry<T>(action)

/**
 * Sets the handler method for the state entry action.
 * @param action The handler method for the state entry action.
 * @return Returns a new state container.
 */
fun State.entry(action: () -> Unit): StateContainer = toContainer().entry(action)

/**
 * Sets the handler method for the state exit action.
 * @param T The type of the action's parameter.
 * @param action The handler method for the state entry action.
 * @return Returns a new state container.
 */
inline fun <reified T : Any> State.exit(noinline action: (T?) -> Unit): StateContainer = toContainer().exit<T>(action)

/**
 * Sets the handler method for the state exit action.
 * @param action The handler method for the state entry action.
 * @return Returns a new state container.
 */
fun State.exit(action: () -> Unit): StateContainer = toContainer().exit(action)

/**
 * Sets the handler method for the states do in state action.
 * @param T The type of the action's parameter.
 * @param action The handler method for the state exit action.
 * @return Returns a new state container.
 */
inline fun <reified T : Any> State.doInState(noinline action: (T?) -> Unit): StateContainer = toContainer().doInState<T>(action)

/**
 * Sets the handler method for the states do in state action.
 * @param action The handler method for the state exit action.
 * @return Returns a new state container.
 */
fun State.doInState(action: () -> Unit): StateContainer = toContainer().doInState(action)

/**
 * Adds a new transition to the state.
 * @param E The Event that initiates this transition.
 * @param stateTo A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
inline fun <reified E : Event> State.transition(
    stateTo: EndState,
    noinline guard: () -> Boolean = { true },
): StateContainer = toContainer().transition<E>(stateTo, guard)

/**
 * Adds a new transition to the state.
 * @param E The Event that initiates this transition.
 * @param T The type of the action's parameter.
 * @param stateTo A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
inline fun <reified E : Event, reified T : Any> State.transition(
    stateTo: EndState,
    noinline guard: (T?) -> Boolean,
): StateContainer = toContainer().transition<E, T>(stateTo, guard)

/**
 * Adds a new transition without an event to a nested state. The event 'NoEvent' is automatically used.
 * @param stateTo A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
fun State.transitionWithoutEvent(
    stateTo: EndState,
    guard: () -> Boolean = { true },
): StateContainer = toContainer().transitionWithoutEvent(stateTo, guard)

/**
 * Adds a new transition without an event to a nested state. The event 'NoEvent' is automatically used.
 * @param T The type of the action's parameter.
 * @param stateTo A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
inline fun <reified T : Any> State.transitionWithoutEvent(
    stateTo: EndState,
    noinline guard: (T?) -> Boolean,
): StateContainer = toContainer().transitionWithoutEvent<T>(stateTo, guard)

/**
 * Adds a new transition to the state.
 * @param E The Event that initiates this transition.
 * @param guard Condition handler of this transition.
 * @param endPoint A reference to the end point of this transition.
 * @return Returns a new state container.
 */
inline fun <reified E : Event> State.transition(
    endPoint: TransitionEndPoint,
    noinline guard: () -> Boolean = { true },
): StateContainer = toContainer().transition<E>(endPoint, guard)

/**
 * Adds a new transition to the state.
 * @param E The Event that initiates this transition.
 * @param T The type of the action's parameter.
 * @param guard Condition handler of this transition.
 * @param endPoint A reference to the end point of this transition.
 * @return Returns a new state container.
 */
inline fun <reified E : Event, reified T : Any> State.transition(
    endPoint: TransitionEndPoint,
    noinline guard: (T?) -> Boolean,
): StateContainer = toContainer().transition<E, T>(endPoint, guard)

/**
 * Adds a new transition without an event to a nested state. The event 'NoEvent' is automatically used.
 * @param guard Condition handler of this transition.
 * @param endPoint A reference to the end point of this transition.
 * @return Returns a new state container.
 */
fun State.transitionWithoutEvent(
    endPoint: TransitionEndPoint,
    guard: () -> Boolean = { true },
): StateContainer = toContainer().transitionWithoutEvent(endPoint, guard)

/**
 * Adds a new transition without an event to a nested state. The event 'NoEvent' is automatically used.
 * @param T The type of the action's parameter.
 * @param guard Condition handler of this transition.
 * @param endPoint A reference to the end point of this transition.
 * @return Returns a new state container.
 */
inline fun <reified T : Any> State.transitionWithoutEvent(
    endPoint: TransitionEndPoint,
    noinline guard: (T?) -> Boolean,
): StateContainer = toContainer().transitionWithoutEvent<T>(endPoint, guard)

/**
 * Adds a new transition to the final state.
 * @param E The Event that initiates this transition.
 * @param T The type of the action's parameter.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
inline fun <reified E : Event, reified T : Any> State.transitionToFinal(noinline guard: (T?) -> Boolean = { true }): StateContainer =
    toContainer().transitionToFinal<E, T>(guard)

/**
 * Adds a new transition to the final state.
 * @param E The Event that initiates this transition.
 * @param guard Condition handler of this transition.
 * @return Returns a new state container.
 */
inline fun <reified E : Event> State.transitionToFinal(noinline guard: () -> Boolean = { true }): StateContainer =
    toContainer().transitionToFinal<E>(guard)
