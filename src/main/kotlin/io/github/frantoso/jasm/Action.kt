package io.github.frantoso.jasm

import kotlin.reflect.KClass

interface IAction {
    fun fire(trigger: Event)
}

object NoAction : IAction {
    override fun fire(trigger: Event) {}
}

class Action(
    private val action: () -> Unit,
    private val state: IState,
    private val actionName: String,
) : IAction {
    override fun fire(trigger: Event) =
        try {
            action()
        } catch (ex: Throwable) {
            throw FsmException("Error calling the $actionName action", state.name, ex)
        }
}

class DataAction<T : Any>(
    private val action: (T?) -> Unit,
    private val state: IState,
    private val actionName: String,
    private val dataType: KClass<T>,
) : IAction {
    private fun fire(data: T?) {
        try {
            action(data)
        } catch (ex: Throwable) {
            throw FsmException("Error calling the $actionName action", state.name, ex)
        }
    }

    override fun fire(trigger: Event) {
        val dataEvent = trigger as? DataEvent<*>
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

inline fun <reified T : Any> action(
    noinline action: () -> Unit = {},
    state: IState,
    actionName: String,
): IAction = Action(action, state, actionName)

inline fun <reified T : Any> action(
    noinline action: (T?) -> Unit,
    state: IState,
    actionName: String,
): IAction = DataAction(action, state, actionName, T::class)
