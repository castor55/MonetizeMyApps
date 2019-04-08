package com.proxyrack.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import com.proxyrack.network.model.SystemInfo
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("HardwareIds")
fun Context.getDeviceId(): String {
    return Secure.getString(
        contentResolver,
        Secure.ANDROID_ID
    )
}

@SuppressLint("HardwareIds")
fun getSystemInfo(): SystemInfo {
    return SystemInfo(
        sdkVersion = Build.VERSION.RELEASE,
        architecture = System.getProperty("os.arch")!!
    )
}

fun createRetrofitClient(url: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
}

fun endOfString(): String {
    return String(Character.toChars(Integer.parseInt("0000", 16)))
}

@SuppressLint("TrustAllX509TrustManager")
fun disableHttpsSecurity() {
    // Create a trust manager that does not validate certificate chains
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf() //To change body of created functions use File | Settings | File Templates.
        }

        override fun checkClientTrusted(
            certs: Array<java.security.cert.X509Certificate>, authType: String
        ) {
        }

        override fun checkServerTrusted(
            certs: Array<java.security.cert.X509Certificate>, authType: String
        ) {
        }
    })

    // Install the all-trusting trust manager
    try {
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
    } catch (e: GeneralSecurityException) {
    }
}

fun InputStream.getResponse(): String {
    // Check for response from server/
    val data = ByteArray(2048)
    val length = read(data)
    return if (length > 0) {
        String(data, 0, length)
            // Trim "end of line" character to keep JSON string valid
            .replace(endOfString(), "")
    } else {
        // No data received
        ""
    }
}