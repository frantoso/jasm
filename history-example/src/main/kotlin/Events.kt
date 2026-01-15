package io.github.frantoso

import io.github.frantoso.jasm.Event

sealed class Events : Event() {
    object Break : Events()

    object Continue : Events()

    object ContinueDeep : Events()

    object Next : Events()

    object Restart : Events()
}
