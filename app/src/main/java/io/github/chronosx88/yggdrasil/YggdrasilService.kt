package io.github.chronosx88.yggdrasil

import android.app.Service
import android.content.Intent
import kotlin.system.exitProcess

class YggdrasilService : Service() {
    private val LOG_TAG: String = YggdrasilService::class.java.simpleName

    override fun onBind(intent: Intent) = null

    companion object {
        var daemon: Process? = null
    }

    private fun start() {
        val process = execYgg("-useconffile ${filesDir.absolutePath}/yggdrasil.conf")
        process.waitFor()
        daemon = process
    }

    private fun stop() {
        daemon?.destroy()
        daemon = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            "start" -> start()
            "stop" -> stop()
            "restart" -> {
                stop(); start()
            }
            "exit" -> exitProcess(0)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }
}
