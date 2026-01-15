package io.github.frantoso.jasm

/**
 * Helper class to store information when iterating through states.
 */
data class StateTreeNode(
    val state: IState,
    val children: List<StateTreeNode>,
)

/**
 * Helper class to store information when iterating through states.
 */
data class StateContainerTreeNode(
    val container: StateContainerBase<*>,
    val children: List<StateContainerTreeNode>,
)
