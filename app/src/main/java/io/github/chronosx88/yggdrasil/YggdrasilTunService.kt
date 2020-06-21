package io.github.chronosx88.yggdrasil

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import com.google.gson.Gson
import dummy.ConduitEndpoint
import io.github.chronosx88.yggdrasil.models.PeerInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mobile.Mobile
import mobile.Yggdrasil
import java.io.*
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext


class YggdrasilTunService : VpnService() {

    private lateinit var ygg: Yggdrasil
    private var isClosed = false

    /** Maximum packet size is constrained by the MTU, which is given as a signed short.  */
    private val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()

    companion object {
        private const val TAG = "Yggdrasil-service"

        @JvmStatic
        fun convertPeerInfoList2PeerIdList(list: ArrayList<PeerInfo>): ArrayList<String> {
            var out = ArrayList<String>()
            for(p in list) {
                out.add(p.toString())
            }
            return out
        }
    }
    private var tunInterface: ParcelFileDescriptor? = null
    private lateinit var yggConduitEndpoint: ConduitEndpoint
    private var tunInputStream: InputStream? = null
    private var tunOutputStream: OutputStream? = null
    private lateinit var readCoroutine: CoroutineContext
    private lateinit var writeCoroutine: CoroutineContext

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.getStringExtra(MainActivity.COMMAND) == MainActivity.STOP) {
            val pi: PendingIntent = intent.getParcelableExtra(MainActivity.PARAM_PINTENT)
            stopVpn(pi)
        }
        if (intent?.getStringExtra(MainActivity.COMMAND) == MainActivity.START) {
            val peers = MainActivity.deserializeStringList2PeerInfoList(intent.getStringArrayListExtra(MainActivity.PEERS))
            val pi: PendingIntent = intent.getParcelableExtra(MainActivity.PARAM_PINTENT)
            ygg = Yggdrasil()
            setupTunInterface(pi, peers)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupTunInterface(pi: PendingIntent, peers: ArrayList<PeerInfo>) {
        pi.send(MainActivity.STATUS_START)
        val builder = Builder()

        var configJson = Mobile.generateConfigJSON()
        val gson = Gson()
        var config = gson.fromJson(String(configJson), Map::class.java).toMutableMap()
        config = fixConfig(config, peers)
        configJson = gson.toJson(config).toByteArray()

        yggConduitEndpoint = ygg.startJSON(configJson)
        val address = ygg.addressString // hack for getting generic ipv6 string from NodeID

        tunInterface = builder
            .addAddress(address, 7)
            .allowFamily(OsConstants.AF_INET)
            .setMtu(MAX_PACKET_SIZE)
            .establish()

        tunInputStream = FileInputStream(tunInterface!!.fileDescriptor)
        tunOutputStream = FileOutputStream(tunInterface!!.fileDescriptor)
        readCoroutine = GlobalScope.launch {
            val buffer = ByteArray(2048)
            try{
                while (true) {
                        readPacketsFromTun(buffer)
                }
            } catch (e: IOException){
                e.printStackTrace()
                tunInputStream!!.close()
            }
        }
        writeCoroutine = GlobalScope.launch {
            while (true) {
                writePacketsToTun()
            }
        }
        val intent: Intent = Intent().putExtra(MainActivity.IPv6, address)
        pi.send(this, MainActivity.STATUS_FINISH, intent)
    }

    private fun fixConfig(config: MutableMap<Any?, Any?>, peers: ArrayList<PeerInfo>): MutableMap<Any?, Any?> {

        val whiteList = arrayListOf<String>()
        whiteList.add("")
        val blackList = arrayListOf<String>()
        blackList.add("")
        config["Peers"] = convertPeerInfoList2PeerIdList(peers)
        config["Listen"] = ""
        config["AdminListen"] = "tcp://localhost:9001"
        config["IfName"] = "tun0"
        //config["EncryptionPublicKey"] = "b15633cf66e63a04f03e9d1a5b2ac6411af819cde9e74175cf574d5599b1296c"
        //config["EncryptionPrivateKey"] = "a39e2da3ccbb5afc3854574a2e3823e881d2d720754d6fdc877f57b252d3b521"
        //config["SigningPublicKey"] = "4f248483c094aea370fba86f1630ba5099cb230aa1337ab6ef6ff0b132be2c2b"
        //config["SigningPrivateKey"] = "e4d56eb2e15e25d9098731e39d661a80c523f31d38b71cbd0ad25a5cde745eac4f248483c094aea370fba86f1630ba5099cb230aa1337ab6ef6ff0b132be2c2b"
        (config["SessionFirewall"] as MutableMap<Any, Any>)["Enable"] = false
        //(config["SessionFirewall"] as MutableMap<Any, Any>)["AllowFromDirect"] = true
        //(config["SessionFirewall"] as MutableMap<Any, Any>)["AllowFromRemote"] = true
        //(config["SessionFirewall"] as MutableMap<Any, Any>)["AlwaysAllowOutbound"] = true
        //(config["SessionFirewall"] as MutableMap<Any, Any>)["WhitelistEncryptionPublicKeys"] = whiteList
        //(config["SessionFirewall"] as MutableMap<Any, Any>)["BlacklistEncryptionPublicKeys"] = blackList

        (config["SwitchOptions"] as MutableMap<Any, Any>)["MaxTotalQueueSize"] = 4194304
        if (config["AutoStart"] == null) {
            val tmpMap = emptyMap<String, Boolean>().toMutableMap()
            tmpMap["WiFi"] = false
            tmpMap["Mobile"] = false
            config["AutoStart"] = tmpMap
        }
        return config
    }

    private fun readPacketsFromTun(buffer: ByteArray) {
        if(!isClosed) {
            // Read the outgoing packet from the input stream.
            val length = tunInputStream!!.read(buffer)
            if (length > 0) {
                val byteBuffer = ByteBuffer.allocate(length)
                byteBuffer.put(buffer, 0, length)
                yggConduitEndpoint.send(byteBuffer.array())
            } else {
                Thread.sleep(10)
            }
        }
    }

    private fun writePacketsToTun() {
        if(tunOutputStream != null) {
            val buffer = yggConduitEndpoint.recv()
            tunOutputStream!!.write(buffer)
        }
    }

    private fun stopVpn(pi: PendingIntent) {
        isClosed = true;
        readCoroutine.cancel()
        writeCoroutine.cancel()
        tunInputStream!!.close()
        tunOutputStream!!.close()
        tunInterface!!.close()
        tunInterface = null
        //this hack due to https://github.com/yggdrasil-network/yggdrasil-go/issues/714 bug
        ygg.startAutoconfigure()
        ygg.stop()
        val intent: Intent = Intent()
        pi.send(this, MainActivity.STATUS_STOP, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
    }
}
