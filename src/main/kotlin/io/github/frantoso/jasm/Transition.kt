package io.github.frantoso.jasm

import kotlin.reflect.KClass

interface ITransition {
    val triggerType: KClass<out Event>
    val endPoint: TransitionEndPoint

    /**
     * Gets a value indicating whether the end point is the final state.
     */
    val isToFinal get() = endPoint.state is FinalState

    fun isAllowed(trigger: Event): Boolean
}

/**
 * A class holding all information about a transition.
 * @param T The type of data provided to the condition and action handlers.
 * @param trigger The Event that initiates this transition.
 * @param endPoint A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 */
data class Transition(
    override val triggerType: KClass<out Event>,
    override val endPoint: TransitionEndPoint,
    private val guard: () -> Boolean,
) : ITransition {
    /**
     * Alternative initialization with state as end point.
     * @param trigger The Event that initiates this transition.
     * @param state A reference to the destination state of this transition.
     * @param guard Condition handler of this transition.
     */
    constructor(
        triggerType: KClass<out Event>,
        state: EndState,
        guard: () -> Boolean,
    ) : this(triggerType, TransitionEndPoint(state), guard)

    override fun isAllowed(trigger: Event): Boolean = if (trigger::class != triggerType) false else guard()
}

inline fun <reified T : Event> transitionX(
    endPoint: TransitionEndPoint,
    noinline guard: () -> Boolean,
): ITransition = Transition(T::class, endPoint, guard)

inline fun <reified T : Event> transitionX(
    state: EndState,
    noinline guard: () -> Boolean,
): ITransition = Transition(T::class, state, guard)

/**
 * A class holding all information about a transition.
 * @param T The type of data provided to the condition and action handlers.
 * @param trigger The Event that initiates this transition.
 * @param endPoint A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 */
data class DataTransition<T : Any>(
    override val triggerType: KClass<out DataEvent<*>>,
    val dataType: KClass<T>,
    override val endPoint: TransitionEndPoint,
    private val guard: (T?) -> Boolean,
) : ITransition {
    /**
     * Alternative initialization with state as end point.
     * @param trigger The Event that initiates this transition.
     * @param state A reference to the destination state of this transition.
     * @param guard Condition handler of this transition.
     */
    constructor(
        triggerType: KClass<out DataEvent<*>>,
        dataType: KClass<T>,
        state: EndState,
        guard: (T?) -> Boolean,
    ) : this(triggerType, dataType, TransitionEndPoint(state), guard)

    override fun isAllowed(trigger: Event): Boolean {
        if (trigger::class != triggerType) return false
        val dataEvent = trigger as? DataEvent<*> ?: return false
        if (dataEvent.data::class != dataType) return false

        val data = dataType.javaObjectType.cast(dataEvent.data)
        return guard(data)
    }
}

inline fun <reified E : DataEvent<T>, reified T : Any> transitionX(
    endPoint: TransitionEndPoint,
    noinline guard: (data: T?) -> Boolean,
): ITransition = DataTransition(E::class, T::class, endPoint, guard)

inline fun <reified E : DataEvent<T>, reified T : Any> transitionX(
    state: EndState,
    noinline guard: (data: T?) -> Boolean,
): ITransition = DataTransition(E::class, T::class, state, guard)
