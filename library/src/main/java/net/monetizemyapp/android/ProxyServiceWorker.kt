package net.monetizemyapp.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proxyrack.BuildConfig
import kotlinx.coroutines.*
import net.monetizemyapp.MonetizeMyApp
import net.monetizemyapp.Properties
import net.monetizemyapp.di.InjectorUtils
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
import java.text.SimpleDateFormat

class ProxyServiceWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), CoroutineScope {

    companion object {
        val TAG: String = ProxyServiceWorker::class.java.name
    }

    override val coroutineContext: CoroutineDispatcher
        get() = CoroutineContextPool.default

    private val locationApi by lazy { InjectorUtils.Api.provideLocationApi() }
    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private val socketServer by lazy { SocksServer() }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private val mainTcpClient by lazy { InjectorUtils.TcpClient.provideServerTcpClient() }

    private val sdf by lazy { SimpleDateFormat("hh:mm:ss") }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    override suspend fun doWork(): Result {
        try {
            logd(TAG, "doWork: call startProxy()")
            //starts listen to requests and suspends this coroutine.
            startProxy()
        } catch (e: CancellationException) {
            loge(TAG, e.message)
        }
        //restarts this Worker if server was stopped or an error occurred
        restartWork()
        return Result.success()
    }


    /**
     * Starts proxy service and listening for requests.
     * This method is blocking operation and should be called from
     * working thread only.
     * */
    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private suspend fun startProxy() {

        logd(TAG, "StartProxy")

        val clientKey = applicationContext.getAppInfo()?.metaData?.getString("monetize_app_key")

        if (clientKey.isNullOrBlank()) {
            throw IllegalArgumentException("Error: \"monetize_app_key\" is null. Provide \"monetize_app_key\" in Manifest to enable SDK")
        }

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

            mainTcpClient.sendMessageSync(message.toJson())

            try {
                while (isActive) {
                    //logd(TAG, "listening loop started")
                    val response = mainTcpClient.waitForMessageSync()
                    //logd(TAG, "server response = $response")
                    if (response.isNullOrBlank()) {
                        continue
                        // listener?.onError(this@SocketTcpClient, "response is Empty")
                    } else {
                        logd(TAG, "server response = $response")
                        logd(TAG, "mainTcpClient onNewMessage, message : $message")
                        val responseObj = response.toObject()
                        logd(TAG, "mainTcpClient onNewMessage, response : $response")
                        when (responseObj) {
                            is ServerMessageEmpty -> {
                                //logd(TAG, "response message is empty")
                            }
                            is Ping -> {
                                logd(TAG, "mainTcpClient response message is Ping")
                                mainTcpClient.sendMessageSync(Pong().toJson())
                            }
                            is Backconnect -> {
                                logd(TAG, "mainTcpClient response message is Backconnect")
                                socketServer.startNewBackconnectSession(responseObj)
                            }
                        }
                        logd(TAG, "It's ${sdf.format(System.currentTimeMillis())} and I'm still alive")
                    }
                }

            } catch (e: Exception) {
                loge(TAG, "mainTcpClient onError : ${e.message}")
                coroutineScope { cancel() }
            }
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private fun stopAllConnections() {
        logd(TAG, "Stopping mainTcpClient")
        mainTcpClient.stop()
        logd(TAG, "Stopping socketServer")
        socketServer.stopServer()
        cancel()
    }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private fun restartWork() {
        stopAllConnections()
        logd(TAG, "Scheduling new Worker start")
        MonetizeMyApp.scheduleServiceStart(applicationContext)
    }
}
