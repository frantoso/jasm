package io.github.frantoso.jasm

import kotlin.reflect.KClass

/**
 * An interface all events must provide.
 */
interface IEvent {
    /**
     * When overridden, gets the type of the event.
     */
    val type: KClass<*>
}

/**
 * A class representing an event.
 */
abstract class Event : IEvent {
    /**
     * Gets the type of the event.
     */
    override val type: KClass<*> get() = this::class

    /**
     * Returns a string representation of the event - it's name.
     */
    override fun toString(): String = this::class.simpleName ?: "Event"

    /**
     * Returns a value indicating whether this instance is equal to [other].
     */
    override fun equals(other: Any?): Boolean = other != null && this::class == other::class

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int = javaClass.hashCode()
}

/**
 * A container to bundle an event with data.
 */
class DataEvent<T : Any>(
    val data: T,
    private val eventType: KClass<out IEvent>,
) : IEvent {
    /**
     * Gets the type of the encapsulated event.
     */
    override val type: KClass<*> get() = eventType

    /**
     * Returns a string representation of the event - it's name.
     */
    override fun toString(): String = eventType.simpleName ?: "Event"

    /**
     * Assigns the enclosed data to a new event type.
     */
    fun fromData(newType: KClass<out IEvent>): DataEvent<T> = DataEvent(data, newType)
}

/**
 * A helper function to create a DataEvent from an Event and data.
 */
inline fun <reified E : Event, T : Any> dataEvent(data: T): DataEvent<T> = DataEvent(data, E::class)

/**
 * Gets the constant used to specify that no event is necessary.
 */
object NoEvent : Event()

/**
 * Gets the Event used to start the behavior of an FSM.
 */
internal object StartEvent : Event()
