package io.github.frantoso

import io.github.frantoso.jasm.DebugAdapter

fun main() {
    val controller = Controller()
    DebugAdapter.of(controller.mainFsm.machine)
    controller.run()
}
