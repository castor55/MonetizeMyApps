package net.monetizemyapp.android

import android.content.Context
import android.os.PowerManager
import android.widget.Toast
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

class ProxyServerWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), CoroutineScope {

    companion object {
        val TAG: String = ProxyServerWorker::class.java.name
    }

    override val coroutineContext: CoroutineDispatcher
        get() = CoroutineContextPool.network

    private val locationApi by lazy { InjectorUtils.Api.provideLocationApi() }
    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private val socketServer by lazy { SocksServer() }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private val mainTcpClient by lazy { InjectorUtils.TcpClient.provideServerTcpClient() }

    private val sdf by lazy { SimpleDateFormat("HH:mm:ss") }

    private val powerManager by lazy { (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager) }


    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    override suspend fun doWork(): Result {
        appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())} Start new Work")
        //uncomment for testing purposes only
        /*withContext(CoroutineContextPool.ui) {
            Toast.makeText(applicationContext, "ProxyServerWorker.doWork", Toast.LENGTH_LONG).show()
        }*/
        val batteryInfo = applicationContext.getBatteryInfo()
        appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())} doWork: Battery Info = $batteryInfo")
        if (/*batteryInfo.isCharging ||*/ batteryInfo.level >= Properties.Worker.REQUIRED_BATTERY_LEVEL) {
            powerManager.run {
                val appName = applicationContext.getApplicationName()
                val wakeLockTag = "$appName::ProxyWakeLock"
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
            }?.let {
                it.acquire(15 * 60 * 1_000 /*1s*/)//acquire for 15 min
                try {
                    logd(TAG, "doWork: call startProxy()")
                    appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())} doWork: call startProxy()")
                    //starts listen to requests and suspends this coroutine.
                    startProxy()
                } catch (e: CancellationException) {
                    loge(TAG, e.message)
                } finally {
                    appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())} Stopping the work ")
                    appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())} Releasing WakeLock ")
                    logd(TAG, "releasing WakeLock")
                    it.release()
                }
            }
            //restarts this Worker if server was stopped or an error occurred
            restartWork()
        }
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

            mainTcpClient?.let { mainTcpClient ->
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
                            logd(TAG, "mainTcpClient server response = $response")
                            val responseObj = response.toObject()
                            logd(TAG, "mainTcpClient onNewMessage, responseObj : $responseObj")
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
                            appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())}\t I'm still alive ")
                        }
                    }

                } catch (e: Exception) {
                    loge(TAG, "mainTcpClient onError : ${e.message}")
                    coroutineContext.cancel()
                }
            }
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private fun stopAllConnections() {
        logd(TAG, "Stopping mainTcpClient")
        mainTcpClient?.stop()
        logd(TAG, "Stopping socketServer")
        socketServer.stopServer()
        if (coroutineContext.isActive) {
            coroutineContext.cancel()
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    private fun restartWork() {
        stopAllConnections()
        logd(TAG, "Scheduling new Worker start")
        appendLogToFile(applicationContext,"${sdf.format(System.currentTimeMillis())}\t Scheduling new Worker start")
        MonetizeMyApp.scheduleServiceStart(MonetizeMyApp.StartMode.SingeLaunch)
    }
}
