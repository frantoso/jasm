package io.github.frantoso.jasm

sealed class Events : Event() {
    object Break : Events()

    object Continue : Events()

    object ContinueDeep : Events()

    object Next : Events()

    object Restart : Events()
}
