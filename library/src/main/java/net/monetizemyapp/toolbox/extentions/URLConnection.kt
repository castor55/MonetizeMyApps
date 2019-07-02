package net.monetizemyapp.toolbox.extentions

import java.io.ByteArrayOutputStream
import java.net.URLConnection

fun URLConnection.waitForExternalBytes(): ByteArray {

    val outputStream = ByteArrayOutputStream()
    val data = ByteArray(4096)

    var length = getInputStream().read(data)
    while (length > 0) {
        outputStream.write(data, 0, length)
        length = getInputStream().read(data)
    }
    return outputStream.toByteArray()
}

fun URLConnection.sendBytes(bytes: ByteArray) {
    getOutputStream().write(bytes)
    getOutputStream().flush()
}