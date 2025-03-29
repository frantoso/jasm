package io.github.frantoso.jasm

import kotlin.reflect.KClass

/**
 * The interface an action must provide.
 */
interface IAction {
    /**
     * When implemented fires the action.
     */
    fun fire(event: IEvent)
}

/**
 * Dummy action doing nothing.
 */
object NoAction : IAction {
    /**
     * Does nothing.
     */
    override fun fire(event: IEvent) {}
}

/**
 * Encapsulates an action.
 * @param action The function to execute.
 */
class Action(
    private val action: () -> Unit,
) : IAction {
    /**
     * Fires the action.
     */
    override fun fire(event: IEvent) =
        try {
            action()
        } catch (ex: Throwable) {
            throw FsmException("Error calling the action", "?", ex)
        }
}

/**
 * Encapsulates an action with a parameter of a state.
 * @param action The function to execute.
 * @param dataType The type of the parameters' data.
 */
class DataAction<T : Any>(
    private val dataType: KClass<T>,
    private val action: (T?) -> Unit,
) : IAction {
    /**
     * Fires the action.
     * @param data The data to provide as parameter.
     */
    private fun fire(data: T?) {
        try {
            action(data)
        } catch (ex: Throwable) {
            throw FsmException("Error calling the action", "?", ex)
        }
    }

    /**
     * Fires the action.
     * @param event The event which originally started the process which results in theis action.
     */
    override fun fire(event: IEvent) {
        val dataEvent = event as? DataEvent<*>
        if (dataEvent == null) {
            fire(null)
            return
        }

        if (dataEvent.data::class != dataType) {
            fire(null)
            return
        }

        val data = dataType.javaObjectType.cast(dataEvent.data)
        fire(data)
    }
}
