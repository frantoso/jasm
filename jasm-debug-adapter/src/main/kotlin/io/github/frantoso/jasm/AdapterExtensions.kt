package io.github.frantoso.jasm

import com.google.gson.Gson
import io.github.frantoso.jasm.model.JasmCommand
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

/**
 * Generates a key for a command handler.
 *
 * @param command The command.
 * @return Returns the key.
 */
fun String.makeKey(command: String): String = "$this::$command"

/**
 * Serializes the specified value to JSON.
 *
 * @param T The type of the object to serialize.
 * @return Returns a JSON string with the serialized object.
 */
fun <T> T.serialize(): String = Gson().toJson(this)

/**
 * Deserializes the specified JSON string to a command object.
 *
 * @return Returns the <see cref="JasmCommand" /> object or null in case of an error.
 */
fun String.deserializeMessage(): JasmCommand? = Gson().fromJson<JasmCommand>(this, JasmCommand::class.java)

/**
 * Compresses the specified text (gzip).
 *
 * @returns Returns a byte array containing the compressed text.
 */
fun String.compress(): ByteArray =
    try {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(this)
        }

        byteStream.toByteArray()
    } catch (e: Exception) {
        throw RuntimeException("Error compressing string", e)
    }
