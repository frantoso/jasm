package io.github.frantoso.jasm

fun main() {
    val controller = Controller()
    DebugAdapter.of(controller.mainFsm.machine)
    controller.run()
}
