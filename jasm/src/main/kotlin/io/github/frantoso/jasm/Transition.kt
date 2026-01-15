package io.github.frantoso.jasm

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * An interface defining all information needed to process a transition.
 */
interface ITransition {
    /**
     * Gets the type of the event that initiates this transition.
     */
    val eventType: KClass<out IEvent>

    /**
     * Gets the end point of this transition.
     */
    val endPoint: TransitionEndPoint

    /**
     * Gets a value indicating whether the end point is the final state.
     */
    val isToFinal get() = endPoint.state is FinalState

    /**
     * Returns a value indicating whether the transition is allowed for the given [event].
     */
    fun isAllowed(event: IEvent): Boolean
}

/**
 * A class holding all information about a transition.
 * @param eventType The type of the event that initiates this transition.
 * @param endPoint A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 */
data class Transition(
    override val eventType: KClass<out Event>,
    override val endPoint: TransitionEndPoint,
    internal val guard: () -> Boolean,
) : ITransition {
    /**
     * Alternative initialization with state as an end point.
     * @param eventType The type of the event that initiates this transition.
     * @param state The destination state of this transition.
     * @param guard Condition handler of this transition.
     */
    constructor(
        eventType: KClass<out Event>,
        state: EndState,
        guard: () -> Boolean,
    ) : this(eventType, TransitionEndPoint(state), guard)

    override fun isAllowed(event: IEvent): Boolean = if (!event.type.isSubclassOf(eventType)) false else guard()
}

/**
 * A class holding all information about a transition.
 * @param eventType The type of the event that initiates this transition.
 * @param endPoint A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 */
data class DataTransition<T : Any>(
    override val eventType: KClass<out Event>,
    val dataType: KClass<T>,
    override val endPoint: TransitionEndPoint,
    internal val guard: (T?) -> Boolean,
) : ITransition {
    /**
     * Alternative initialization with state as an end point.
     * @param eventType The type of the event that initiates this transition.
     * @param state A reference to the destination state of this transition.
     * @param guard Condition handler of this transition.
     */
    constructor(
        eventType: KClass<out Event>,
        dataType: KClass<T>,
        state: EndState,
        guard: (T?) -> Boolean,
    ) : this(eventType, dataType, TransitionEndPoint(state), guard)

    override fun isAllowed(event: IEvent): Boolean {
        if (!event.type.isSubclassOf(eventType)) return false
        val dataEvent = event as? DataEvent<*> ?: return false
        if (!dataEvent.data::class.isSubclassOf(dataType)) return false

        val data = dataType.javaObjectType.cast(dataEvent.data)
        return guard(data)
    }
}
