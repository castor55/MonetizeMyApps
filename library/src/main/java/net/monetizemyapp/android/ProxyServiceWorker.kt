package net.monetizemyapp.android

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proxyrack.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat


/*class ProxyServiceWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    override fun doWork(): Result {
        return try {
            logd(Properties.APP_TAG, "ProxyServiceWorker: trying to start ProxyService")
            applicationContext.startService(Intent(applicationContext, ProxyService::class.java))
            Result.success()
        } catch (e: IllegalStateException) {
            logd(Properties.APP_TAG, "ProxyServiceWorker: Start ProxyService failed")
            Result.failure()
        }
    }
}*/
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

    private var isConnected = false

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
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

    private val sdf by lazy { SimpleDateFormat("hh:MM:ss") }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    override suspend fun doWork(): Result {
        startProxy()

        while (!isStopped) {
            appendLog("${sdf.format(System.currentTimeMillis())} Still Alive")
            delay(1_000 * 60 * 5)
        }
        return Result.success()
    }


    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private fun startProxy() {

        logd(TAG, "StartProxy")

        val clientKey = applicationContext.getAppInfo()?.metaData?.getString("monetize_app_key")

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
        MonetizeMyApp.scheduleServiceStart()
    }

    fun appendLog(text: String) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val filename: String = applicationContext.externalCacheDir.absolutePath + "/logs"
            val logFile = File(filename)
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }

            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                val buf = BufferedWriter(FileWriter(logFile, true))
                buf.append(text)
                buf.newLine()
                buf.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

}
