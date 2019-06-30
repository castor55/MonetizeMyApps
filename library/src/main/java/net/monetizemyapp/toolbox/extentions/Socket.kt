package net.monetizemyapp.toolbox.extentions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket

fun InputStream.getString(): String {

    val data = ByteArray(4096)
    val length = read(data).takeIf { it > 0 } ?: 0
    val result = String(data, 0, length)
    return result.removeSuffix(endOfString())
}

fun Socket.waitForJson(): String? {
    return try {
        DataInputStream(getInputStream()).getString()
    } catch (ex: IOException) {
        loge(toString(), ex.message)
        null
    }
}

fun Socket.sendJson(message : String) {
    // Start a connection
    val writerStream = DataOutputStream(outputStream)
    // Send message to server immediately
    writerStream.write(message.toByteArray())
    writerStream.flush()
}