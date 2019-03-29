package com.proxyrack.android

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.proxyrack.network.service.GeolocationService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy

class ProxyService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("CheckResult")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val geoClient = Retrofit.Builder()
            .baseUrl("http://ip-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        val geoService = geoClient.create(GeolocationService::class.java)

        geoService.location
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                Log.d("tagx", "echo: $result")
            }, { t ->
                throw t
            })

        return super.onStartCommand(intent, flags, startId)
    }

    private fun connectToWebsite() {
        // backconnect server's IP address
        val hostname = "68.183.109.83"
        // default secure port
        val port = 443

        val proxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(hostname, port)
        )
        val client = OkHttpClient.Builder()
            .proxy(proxy)
            .build()

    }
}
