package io.github.frantoso.jasm.model

import com.google.gson.annotations.SerializedName

/**
 * A class representing a state in the diagram generator.
 *
 * @param id The ID of the state.
 * @param name The name of the state.
 * @param isInitial A value indicating whether this is an initial state.
 * @param isFinal A value indicating whether this is a final state.
 * @param transitions A list of outgoing transitions.
 * @param children A list of sub-state-machines.
 * @param hasHistory A value indicating whether this state contains a history end point.
 * @param hasDeepHistory A value indicating whether this state contains a deep history end point.
 */
data class StateInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("IsInitial") val isInitial: Boolean,
    @SerializedName("IsFinal") val isFinal: Boolean,
    @SerializedName("Transitions") val transitions: List<TransitionInfo>,
    @SerializedName("Children") val children: List<FsmInfo>,
    @SerializedName("HasHistory") val hasHistory: Boolean,
    @SerializedName("HasDeepHistory") val hasDeepHistory: Boolean,
) {
    /**
     * Updates this <see cref="StateInfo" /> with history information.
     *
     * @param hasHistory A value indicating whether this state contains a history end point.
     * @param hasDeepHistory A value indicating whether this state contains a deep history end point.
     * @return Returns a new [StateInfo] instance with the updated information.
     */
    fun update(
        hasHistory: Boolean,
        hasDeepHistory: Boolean,
    ): StateInfo =
        StateInfo(
            this.id,
            this.name,
            this.isInitial,
            this.isFinal,
            this.transitions,
            this.children,
            this.hasHistory || hasHistory,
            this.hasDeepHistory || hasDeepHistory,
        )

    /**
     * Returns a string that represents the current object.
     */
    override fun toString(): String = this.name
}
