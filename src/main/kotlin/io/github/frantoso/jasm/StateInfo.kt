package io.github.frantoso.jasm

/**
 * Helper class to store information when iterating through states.
 */
data class StateInfo(
    val state: IState,
    val children: List<StateInfo>,
)

/**
 * Helper class to store information when iterating through states.
 */
data class StateContainerInfo(
    val container: StateContainerBase<*>,
    val children: List<StateContainerInfo>,
)
