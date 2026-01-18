package io.github.frantoso.jasm.model

import com.google.gson.annotations.SerializedName
import io.github.frantoso.jasm.IState
import io.github.frantoso.jasm.normalizedId

@Suppress("unused")
class StateChangedInfo(
    @SerializedName("Fsm") val fsm: String,
    oldState: IState,
    newState: IState,
) {
    /**
     * Gets the name of the state before the state change.
     */
    @SerializedName("OldStateName")
    val oldStateName = oldState.name

    /**
     * Gets the id of the state before the state change.
     */
    @SerializedName("OldStateId")
    val oldStateId = oldState.normalizedId(fsm)

    /**
     * Gets the name of the state after the state change (the current state).
     */
    @SerializedName("NewStateName")
    val newStateName = newState.name

    /**
     * Gets the id of the state after the state change (the current state).
     */
    @SerializedName("NewStateId")
    val newStateId = newState.normalizedId(fsm)
}
