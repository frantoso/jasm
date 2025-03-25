package io.github.frantoso.jasm

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

abstract class DataEvent<T : Any>(
    name: String = "",
) : Event(name) {
    abstract val data: T
}

/**
 * Gets the constant used to specify that no event is necessary.
 */
class NoEvent : Event()

/**
 * Gets the constant used to specify that no event is necessary.
 */
class DataNoEvent<T : Any>(
    override val data: T,
) : DataEvent<T>()

/**
 * Gets the Event used to start the behavior of an FSM.
 */
internal object StartEvent : Event()
