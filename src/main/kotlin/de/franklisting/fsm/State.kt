package de.franklisting.fsm

import kotlinx.atomicfu.atomic

/**
 * This class acts as base for all states of the state machine.
 * @param name The name of the state.
 */
abstract class StateBase(
    val name: String,
) {
    /**
     * Returns a String that represents the current Object.
     */
    override fun toString(): String = name

    /**
     * Gets a unique identifier of this object.
     */
    val id = "State_%04d".format(instanceCounter.getAndIncrement())

    companion object {
        private val instanceCounter = atomic(0)
    }
}

/**
 * An interface to be implemented by all states.
 */
interface IState {
    /**
     * Gets the name of the state.
     */
    val name: String
}

/**
 * An interface to mark a start state (stateFrom in a transition)
 */
interface StartState : IState

/**
 * An interface to mark an end state (stateTo in a transition)
 */
interface EndState : IState {
    /**
     * Gets the deep history transition end point for this state.
     */
    val deepHistory: TransitionEndPoint

    /**
     * Gets the history transition end point for this state.
     */
    val history: TransitionEndPoint
}

/**
 * A base class for all send state implementations.
 * @param name The name of the state.
 */
abstract class EndStateBase(
    name: String,
) : StateBase(name),
    EndState {
    /**
     * Gets the deep history transition end point for this state.
     */
    override val deepHistory: TransitionEndPoint by lazy { TransitionEndPoint(this, History.Hd) }

    /**
     * Gets the history transition end point for this state.
     */
    override val history: TransitionEndPoint by lazy { TransitionEndPoint(this, History.H) }
}

/**
 * A class to model a normal state.
 * @param name The name of the state.
 */
class State(
    name: String,
) : EndStateBase(name),
    StartState

/**
 * A class to model the special state 'initial'.
 */
class InitialState :
    StateBase("Initial"),
    StartState

/**
 * A class to model the special state 'final'.
 */
class FinalState : EndStateBase("Final") {
    /**
     * Returns a value indicating whether this instance is equal to [other].
     * Special handling for the final state class: The object itself is not relevant, only the type.
     */
    override fun equals(other: Any?): Boolean = other != null && this::class == other::class

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int = javaClass.hashCode()
}
