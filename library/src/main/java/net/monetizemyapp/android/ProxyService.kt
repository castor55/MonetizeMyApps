/*
package net.monetizemyapp.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.proxyrack.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.monetizemyapp.MonetizeMyApp
import net.monetizemyapp.Properties
import net.monetizemyapp.di.InjectorUtils
import net.monetizemyapp.network.TcpClient
import net.monetizemyapp.network.deviceId
import net.monetizemyapp.network.getSystemInfo
import net.monetizemyapp.network.model.base.ServerMessageEmpty
import net.monetizemyapp.network.model.response.IpApiResponse
import net.monetizemyapp.network.model.step0.Ping
import net.monetizemyapp.network.model.step0.Pong
import net.monetizemyapp.network.model.step1.Hello
import net.monetizemyapp.network.model.step1.HelloBody
import net.monetizemyapp.network.model.step2.Backconnect
import net.monetizemyapp.network.socks.SocksServer
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.CoroutineContext

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class ProxyService : Service(), CoroutineScope {

    private val lifecycleJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    companion object {
        private val TAG: String = ProxyService::class.java.canonicalName ?: ProxyService::class.java.name
    }

    private val locationApi by lazy { InjectorUtils.Api.provideLocationApi() }
    private val socketServer by lazy { SocksServer() }

    private val mainTcpClient by lazy {
        InjectorUtils.TcpClient.provideServerTcpClient()
            .apply {
                listener = object : TcpClient.OnSocketResponseSimpleListener() {
                    override fun onNewMessage(client: TcpClient, message: String) {
                        logd(TAG, "mainTcpClient onNewMessage, message : $message")
                        val response = message.toObject()
                        logd(TAG, "mainTcpClient onNewMessage, response : $response")
                        when (response) {
                            is ServerMessageEmpty -> {
                                //logd(TAG, "response message is empty")
                            }
                            is Ping -> {
                                logd(TAG, "mainTcpClient response message is Ping")
                                client.sendMessage(Pong().toJson())
                            }
                            is Backconnect -> {
                                logd(TAG, "mainTcpClient response message is Backconnect")
                                socketServer.startNewBackconnectSession(response)
                            }
                        }
                    }

                    override fun onError(client: TcpClient, error: String) {
                        loge(TAG, "mainTcpClient onError : $error")
                        onConnectionLost()
                    }
                }
            }
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var isConnected = false
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd(TAG, "onStartCommand")
        if (!isConnected) {
            startProxy()
        }

        return START_STICKY_COMPATIBILITY
    }

    private fun startProxy() {

        logd(TAG, "StartProxy")

        val clientKey = getAppInfo()?.metaData?.getString("monetize_app_key")

        if (clientKey.isNullOrBlank()) {
            throw IllegalArgumentException("Error: \"monetize_app_key\" is null. Provide \"monetize_app_key\" in Manifest to enable SDK")
        }

        launch {
            val location: Response<IpApiResponse>? = try {
                locationApi.getLocation()
            } catch (ex: HttpException) {
                loge(TAG, "Location request error : ${ex.message}")
                null
            } catch (ex: IOException) {
                loge(TAG, "Location request error : ${ex.message}")
                null
            }

            location?.takeIf { it.isSuccessful }?.body()?.let {
                val message = Hello(
                    HelloBody(
                        clientKey,
                        it.query,
                        deviceId,
                        it.city,
                        Properties.TEST_COUNTRY_CODE.takeIf { BuildConfig.DEBUG } ?: it.countryCode,
                        getSystemInfo()
                    )
                )

                mainTcpClient.sendMessage(message.toJson())
                isConnected = true
            }
        }
    }

    private fun onConnectionLost() {
        stopSelf()
        MonetizeMyApp.scheduleServiceStart()
    }

    private fun stopAllConnections() {
        if (isConnected) {
            logd(TAG, "stopping main connection")
            mainTcpClient.stop()
        }
        logd(TAG, "stopping server")
        socketServer.stopServer()
        lifecycleJob.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopAllConnections()
        MonetizeMyApp.scheduleServiceStart()
        logd(TAG, "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            stopAllConnections()
        } catch (e: Exception) {
            logd(
                TAG,
                "Exception while trying to stop all connection. Probably it tries to stop connection on not initialized TcpClient \n${e.message}"
            )
        }
        MonetizeMyApp.scheduleServiceStart()
        super.onDestroy()
    }
}*/
