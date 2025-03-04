package de.franklisting.fsm

import de.franklisting.fsm.History.H
import de.franklisting.fsm.History.Hd

/**
 * Helper class to have a well-defined type to support history.
 */
sealed class History {
    data object None : History()

    data object H : History()

    data object Hd : History()
}

/**
 * Gets a value indicating whether this instance represents history.
 */
val History.isHistory get() = this == H

/**
 * Gets a value indicating whether this instance represents deep history.
 */
val History.isDeepHistory get() = this == Hd
