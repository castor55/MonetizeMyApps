package net.monetizemyapp.toolbox.extentions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket

@ExperimentalStdlibApi
fun InputStream.getString(): String =
    use {
        val data = ByteArray(4096)
        read(data)
        val result = data.decodeToString()
        val endOfStringIndex = result.indexOf(endOfString())
        return result.substring(0, endOfStringIndex)
    }


@ExperimentalStdlibApi
@Throws(IOException::class)
fun Socket.waitForStringResponse(): String? = DataInputStream(getInputStream()).getString()

fun Socket.sendJson(message: String) {
    // Start a connection
    val writerStream = DataOutputStream(outputStream)
    // Send message to server immediately
    writerStream.write(message.toByteArray())
    writerStream.flush()
}

fun InputStream.getBytes(): ByteArray {

    val data = ByteArray(4096)
    val length = read(data).takeIf { it >= 0 } ?: 0

    return data.copyOfRange(0, length)
}

fun Socket.waitForBytes(): ByteArray = getInputStream().getBytes()

fun Socket.sendBytes(bytes: ByteArray) {
    // Start a connection
    val writerStream = DataOutputStream(outputStream)
    // Send message to server immediately
    writerStream.write(bytes)
    writerStream.flush()
}
