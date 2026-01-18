package io.github.frantoso.jasm.model

import com.google.gson.annotations.SerializedName

/**
 * A class representing a finite state machine in the diagram generator.
 * @param name The name of the state machine.
 * @param states The states contained in this machine.
 */
data class FsmInfo(
    @SerializedName("Name") val name: String,
    @SerializedName("States") val states: List<StateInfo>,
) {
    /**
     * Returns a string that represents the current object.
     */
    override fun toString(): String = this.name
}
