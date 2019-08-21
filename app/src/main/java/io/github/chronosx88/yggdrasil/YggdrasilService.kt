package io.github.chronosx88.yggdrasil

import android.app.Service
import android.content.Intent
import android.os.Build.CPU_ABI
import android.util.Log
import kotlin.system.exitProcess

class YggdrasilService : Service() {
    private val LOG_TAG: String = YggdrasilService::class.java.simpleName

    override fun onBind(intent: Intent) = null

    companion object {
        var daemon: Process? = null
    }

    private fun installBinary() {
        val type = CPU_ABI.let {
            when{
                it.contains("v8") -> "arm64"
                it.contains("v7") -> "armhf"
                else -> throw Exception("Unsupported ABI")
            }
        }

        yggBin.apply {
            delete()
            createNewFile()
        }

        val input = assets.open(type)
        val output = yggBin.outputStream()

        try {
            input.copyTo(output)
        } finally {
            input.close(); output.close()
        }

        yggBin.setExecutable(true)
        execYgg("-genconf > yggdrasil.conf").waitFor() // Generate config
        Log.i(LOG_TAG, "# Binary installed successfully")
    }

    private fun start() {
        execYgg("-useconffile yggdrasil.conf").apply {
            daemon = this
        }
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
}
