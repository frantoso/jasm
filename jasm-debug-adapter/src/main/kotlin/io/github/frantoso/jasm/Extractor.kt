package io.github.frantoso.jasm

import io.github.frantoso.jasm.model.FsmInfo
import io.github.frantoso.jasm.model.StateInfo
import io.github.frantoso.jasm.model.TransitionInfo

/**
 * The special ID used for final states.
 */
const val FINAL_STATE_ID = "Final.ID"

/**
 * Normalizes the ID for a final state, for normal states takes the original one.
 *
 * @param ownerName Name of the owner.
 * @return Returns the ID.
 */
fun IState.normalizedId(ownerName: String) = if (this is FinalState) "$ownerName-$FINAL_STATE_ID" else this.id

/**
 * Converts an [ITransition] to a [TransitionInfo].
 *
 * @return Returns the converted object.
 */
fun ITransition.convert(ownerName: String): TransitionInfo =
    TransitionInfo(
        this.endPoint.state.normalizedId(ownerName),
        this.endPoint.history.isHistory,
        this.endPoint.history.isDeepHistory,
        this.isToFinal,
    )

/**
 * Converts a [StateContainerBase] to a [StateInfo].
 *
 * @returns Returns the converted object.
 */
fun StateContainerBase<out IState>.convert(ownerName: String): StateInfo {
    val transitions = this.transitions.map { t -> t.convert(ownerName) }
    val children = this.children.map { fsm -> fsm.convert() }
    return StateInfo(
        this.state.normalizedId(ownerName),
        this.state.name,
        this.state is InitialState,
        this.state is FinalState,
        transitions,
        children,
        hasHistory = false,
        hasDeepHistory = false,
    )
}

/**
 * Updates a [StateInfo] with history information extracted from the specified transitions.
 *
 * @param transitions The transitions to analyze.
 * @return Returns a new state with the updated information.
 */
fun StateInfo.update(transitions: List<TransitionInfo>): StateInfo {
    val (hasHistory, hasDeepHistory) =
        transitions
            .filter { transition -> transition.endPointId == this.id }
            .fold(
                false to false,
            )
            { combined, info -> (combined.first || info.isHistory) to (combined.second || info.isDeepHistory) }

    return this.update(hasHistory, hasDeepHistory)
}

/**
 * Converts an [Fsm] to an [FsmInfo] object.
 *
 * @return Returns the converted object.
 */
fun Fsm.convert(): FsmInfo {
    val rawStates =
        (listOf(this.debugInterface.initialState) + this.debugInterface.stateDump)
            .map { co -> co.convert(this.name) }
    val transitions = rawStates.flatMap { it.transitions }
    val states = rawStates.map { st -> st.update(transitions) }

    return FsmInfo(this.name, states)
}

/**
 * Converts a <see cref="Fsm" /> to a <see cref="FsmInfo" />.
 *
 * @return Returns the converted object.
 */
fun Fsm.allMachines(): List<Fsm> =
    this.debugInterface.stateDump.fold(
        listOf(this),
    ) { list, s -> list + s.children.flatMap { c -> c.allMachines() } }
