package io.github.frantoso.jasm.model

import com.google.gson.annotations.SerializedName

/**
 * Helper class representing a JASM command. Used for commands exchanged with the debug adapter.
 *
 * @param fsm The name of the state machine related to this command.
 * @param command The command.
 * @param payload The payload.
 */
data class JasmCommand(
    @SerializedName("Fsm") val fsm: String,
    @SerializedName("Command") val command: String,
    @SerializedName("Payload") val payload: String,
)
