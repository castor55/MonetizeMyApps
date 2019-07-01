package net.monetizemyapp.toolbox.extentions

import net.monetizemyapp.network.getBytes
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket

@ExperimentalStdlibApi
fun InputStream.getString(): String {
    val data = ByteArray(4096)
    read(data)
    val result = data.decodeToString()
    val endOfStringIndex = result.indexOf(endOfString())
    return result.substring(0, endOfStringIndex)
}

@ExperimentalStdlibApi
fun Socket.waitForJson(): String? = try {
    DataInputStream(getInputStream()).getString()
} catch (ex: IOException) {
    loge("Socket", "waitForJson ${ex.message}")
    null
}


fun Socket.sendJson(message: String) {
    // Start a connection
    val writerStream = DataOutputStream(outputStream)
    // Send message to server immediately
    writerStream.write(message.toByteArray())
    writerStream.flush()
}

fun Socket.waitForBytes(): ByteArray = getInputStream().getBytes()

fun Socket.sendBytes(bytes: ByteArray) {
    // Start a connection
    val writerStream = DataOutputStream(outputStream)
    // Send message to server immediately
    writerStream.write(bytes)
    writerStream.flush()
}
