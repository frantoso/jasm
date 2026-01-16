package io.github.frantoso.jasm

import io.github.frantoso.jasm.model.JasmCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets.UTF_8

class AdapterExtensionsTest {
    @Test
    fun makeKeyTest() {
        val key = "MyFsm".makeKey("do-it")
        assertThat(key).isEqualTo("MyFsm::do-it")
    }

    @Test
    fun serializeDeserializeTest() {
        val original = JasmCommand("MyFsm", "do-it", "payload-data")

        val json = original.serialize()
        val deserialized = json.deserializeMessage()

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.fsm).isEqualTo(original.fsm)
        assertThat(deserialized.command).isEqualTo(original.command)
        assertThat(deserialized.payload).isEqualTo(original.payload)
    }

    @Test
    fun serializeNullObjectTest() {
        val json = (null as Any?).serialize()
        assertThat(json).isEqualTo("null")
    }

    @Test
    fun deserializeInvalidJsonTest() {
        val invalidJson = "{ invalid json }"
        assertThatThrownBy { invalidJson.deserializeMessage() }
            .isInstanceOf(Exception::class.java)
    }

    @Test
    fun deserializeNullJsonTest() {
        val nullJson = "null"
        val command = nullJson.deserializeMessage()
        assertThat(command).isNull()
    }

    @Test
    fun compressTest() {
        val text =
            "This is a test string for compression." +
                "But this only works if the text is a little bit longer" +
                "Otherwise the compressed data are longer than the original text"
        val compressed = text.compress()

        assertThat(compressed.size).isLessThan(text.length)

        val decompressed = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(compressed)).use { it.copyTo(decompressed) }

        val decompressedText = decompressed.toByteArray().toString(UTF_8)

        assertThat(decompressedText).isEqualTo(text)
    }
}
