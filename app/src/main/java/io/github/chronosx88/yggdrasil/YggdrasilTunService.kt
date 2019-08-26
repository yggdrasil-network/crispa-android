package io.github.chronosx88.yggdrasil

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.google.gson.Gson
import mobile.Yggdrasil


class YggdrasilTunService : VpnService() {
    companion object {
        private var isRunning: Boolean = false
    }
    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        setupTunInterface()
    }

    private fun setupTunInterface() {
        if(!yggBin.exists()) {
            installBinary()
        }

        val builder = Builder()

        val yggTmp = Yggdrasil()
        val tempYggConfig = getYggConfig()
        tempYggConfig.ifName = "none"
        yggTmp.startJSON(Gson().toJson(tempYggConfig).toByteArray())
        val address = yggTmp.addressString // hack for getting generic ipv6 string from NodeID

        tunInterface = builder
            .addAddress(address, 7)
            .addRoute("0200::", 7)
            .establish()

        createYggdrasilService()
    }

    private fun createYggdrasilService() {
        val intent = Intent(this, YggdrasilService::class.java)
        intent.action = "start"
        startService(intent)
    }

    private fun stopYggdrasilService() {
        val intent = Intent(this, YggdrasilService::class.java)
        stopService(intent)
    }

    override fun onRevoke() {
        super.onRevoke()
        isRunning = false
        stopYggdrasilService()
        tunInterface!!.close()
        tunInterface = null
    }
}
