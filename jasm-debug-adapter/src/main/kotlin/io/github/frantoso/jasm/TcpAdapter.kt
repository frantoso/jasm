package io.github.frantoso.jasm

import com.google.gson.JsonParser
import io.github.frantoso.jasm.model.JasmCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Singleton TCP adapter for communication with the debug server.
 */
object TcpAdapter : Closeable {
    private val nextConnectWaitingTime = 3.seconds

    /**
     * Scope to execute async operations.
     */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * The TCP client used for communication.
     */
    private var socket: Socket? = null

    /**
     * The command handlers.
     */
    private val commandHandlers = ConcurrentHashMap<String, (String) -> Unit>()

    /**
     * A flag indicating whether the instance has been closed.
     */
    private var isClosed = false

    init {
        scope.launch {
            connect(TcpSettings.read())
        }
    }

    /**
     * Sends the provided command and data to the server.
     *
     * @param fsm The state machine addressed by the command.
     * @param command The command to send.
     * @param payload The data associated with the command.
     */
    fun sendAsync(
        fsm: String,
        command: String,
        payload: String,
    ) = sendAsync(JasmCommand(fsm, command, payload).serialize())

    /**
     * Adds the provided command to the list of handlers.
     *
     * @param fsm The state machine addressed by the command.
     * @param command The command.
     * @param handler The handler.
     */
    fun addCommand(
        fsm: String,
        command: String,
        handler: (String) -> Unit,
    ) {
        commandHandlers[fsm.makeKey(command)] = handler
    }

    /**
     * Closes the TCP connection.
     */
    override fun close() {
        if (isClosed) return

        socket?.close()
        isClosed = true
    }

    /**
     * Connects to the server and starts the receiver loop.
     * If the connection fails, it will retry every 3 seconds until successful.
     *
     * @param settings The host and port to connect to.
     */
    private suspend fun connect(settings: TcpSettings) {
        while (!isClosed) {
            try {
                val s = Socket()
                withContext(Dispatchers.IO) {
                    s.connect(InetSocketAddress(settings.host, settings.port))
                }

                socket = s
                scope.launch { receiveLoop() }
                break
            } catch (_: Exception) {
                // ignored, server may be not ready yet
                delay(nextConnectWaitingTime)
            }
        }
    }

    /**
     * Sends the provided message to the server.
     *
     * @param message The message to send.
     */
    private fun sendAsync(message: String) {
        val s = socket ?: return
        if (!s.isConnected) return

        val stream = s.getOutputStream()
        stream.write(message.compress())
        stream.flush()
    }

    /**
     * Receiver loop to handle messages from the server.
     */
    private fun receiveLoop() {
        val s = socket ?: return

        val stream = s.getInputStream()
        val buffer = ByteArray(1024)
        while (s.isConnected && !s.isClosed) {
            val bytesRead = stream.read(buffer)
            if (bytesRead <= 0) {
                // connection closed
                break
            }

            val message = String(buffer, 0, bytesRead, Charsets.UTF_8)

            message.deserializeMessage()?.run { processMessage(fsm, command, payload) }
        }
    }

    /**
     * Processes a message received from the server.
     *
     * @param fsm The state machine addressed by the command.
     * @param command The command to execute.
     * @param payload The payload associated with the command.
     */
    private fun processMessage(
        fsm: String,
        command: String,
        payload: String,
    ) = commandHandlers[fsm.makeKey(command)]?.let {
        try {
            it(payload)
        } catch (_: Exception) {
        }
    }
}

/**
 * Represents configuration settings for establishing a TCP connection, including the host and port.
 *
 * The TcpSettings class provides methods to load connection settings from environment variables.
 * If no explicit values are provided, default values are used for the host and port.
 */
class TcpSettings private constructor() {
    /**
     * The host value configured by the user. May be null.
     */
    private var customHost: String? = null

    /**
     * The port value configured by the user. May be null.
     */
    private var customPort: Int? = null

    /**
     * Gets the host name or IP address used to establish a connection.
     */
    val host: String
        get() = customHost ?: DEFAULT_HOST

    /**
     * Gets the network port number used for the connection.
     */
    val port: Int
        get() = customPort ?: DEFAULT_PORT

    /**
     * Returns a string that represents the current object.
     */
    override fun toString(): String = "Host: $host, Port: $port"

    /**
     * Reads host and/or port from environment variables, if they are not already set.
     * Does not override!
     *
     * @return Returns a reference to this to allow chaining.
     */
    fun fromEnvironment(): TcpSettings {
        customHost = customHost ?: System.getenv("JASM_DEBUG_HOST")?.trim(' ', '"', '\t')
        customPort = customPort ?: System.getenv("JASM_DEBUG_PORT")?.toIntOrNull()
        return this
    }

    companion object {
        /*
         * The default host address (localhost).
         */
        const val DEFAULT_HOST = "127.0.0.1"

        /**
         * The default port number used for network connections.
         */
        const val DEFAULT_PORT = 4000

        /**
         * Reads settings from a configuration file and/or environment variables.
         *
         * The Host and/or Port properties may return the default values if there was no configuration,
         * or it was not complete.
         *
         * @return Returns a new <see cref="TcpSettings" /> instance, filled with the information read.
         */
        fun read(): TcpSettings = fromConfiguration().fromEnvironment()

        /**
         * Reads settings from a configuration file.
         *
         * The Host and/or Port properties may return the default values if there was no configuration,
         * or it was not complete.
         *
         * @return Returns a new <see cref="TcpSettings" /> instance, filled with the information read.
         */
        fun fromConfiguration(): TcpSettings {
            val cwd = Paths.get("").toAbsolutePath().toString()
            val configFile = Paths.get(cwd, "debug-settings.json").toFile()
            val settings = TcpSettings()

            if (!configFile.exists()) return settings

            try {
                val json = configFile.readText()
                val config = JsonParser.parseString(json).asJsonObject
                val params = config["JasmDebug"]?.asJsonObject?.get("TcpSettings")?.asJsonObject
                settings.customHost = params?.get("Host")?.asString?.trim(' ', '"', '\t')
                settings.customPort = params?.get("Port")?.asInt
            } catch (ex: Exception) {
                println("Error reading config file: ${ex.message}")
            }

            return settings
        }
    }
}
