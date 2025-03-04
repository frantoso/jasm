package de.franklisting.fsm

/**
 * Class representing an event.
 */
abstract class Event {
    val name = this::class.simpleName ?: ""
}

/**
 * Gets the constant used to specify that no event is necessary.
 */
object NoEvent : Event()

/**
 * Gets the Event used to start the behavior of an FSM.
 */
internal object StartEvent : Event()
