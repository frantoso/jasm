package io.github.frantoso.jasm

/**
 * Helper class to have a well-defined type to support history.
 */
sealed class History {
    data object None : History()

    data object H : History()

    data object Hd : History()

    /**
     * Gets a value indicating whether this instance represents history.
     */
    val isHistory get() = this == H

    /**
     * Gets a value indicating whether this instance represents deep history.
     */
    val isDeepHistory get() = this == Hd
}
