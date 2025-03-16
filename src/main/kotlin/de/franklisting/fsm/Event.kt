package de.franklisting.fsm

/**
 * Class representing an event.
 * @param name The name of this event. If there is no name provided, the name of the class is used.
 * Anonymous objects without a name provided will throw an exception.
 */
abstract class Event(
    name: String = "",
) {
    /**
     * Gets the name of this event.
     * Anonymous objects without a name provided will throw an exception.
     */
    val name: String = name.ifBlank { this::class.simpleName!! }

    /**
     * Returns a string representation of the event - it's name.
     */
    override fun toString(): String = name
}

/**
 * Gets the constant used to specify that no event is necessary.
 */
object NoEvent : Event()

/**
 * Gets the Event used to start the behavior of an FSM.
 */
internal object StartEvent : Event()
