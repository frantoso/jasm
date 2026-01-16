package io.github.frantoso.jasm.model

import com.google.gson.annotations.SerializedName

/**
 * A class representing a transition in the diagram generator.
 * @param endPointId The end point identifier.
 * @param isHistory A value indicating whether this transition ends in a history state.
 * @param isDeepHistory A value indicating whether this transition ends in a deep history state.
 * @param isToFinal A value indicating whether this transition ends in the final state.
 */
data class TransitionInfo(
    @SerializedName("EndPointId") val endPointId: String,
    @SerializedName("IsHistory") val isHistory: Boolean,
    @SerializedName("IsDeepHistory") val isDeepHistory: Boolean,
    @SerializedName("IsToFinal") val isToFinal: Boolean,
)
