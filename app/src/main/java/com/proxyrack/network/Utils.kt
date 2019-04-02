package com.proxyrack.network

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
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

fun createRetrofitClient(url: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
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