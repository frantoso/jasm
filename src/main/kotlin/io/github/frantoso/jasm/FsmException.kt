package io.github.frantoso.jasm

/**
 * Exception class used with the FSM.
 * @param message The error message that explains the reason for the exception.
 * @param stateName The state which throws this exception.
 * @param cause The exception that is the cause of the current exception.
 *         If the innerException parameter is not a null reference, the current exception is
 *         raised in a catch block that handles the inner exception.
 */
class FsmException(
    message: String? = null,
    val stateName: String = "",
    cause: Throwable? = null,
) : Exception(message, cause)
