package io.github.chronosx88.yggdrasil

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import com.google.gson.Gson
import dummy.ConduitEndpoint
import io.github.chronosx88.yggdrasil.models.DNSInfo
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.convertPeerInfoSet2PeerIdSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2DNSInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2PeerInfoSet
import kotlinx.coroutines.*
import mobile.Mobile
import mobile.Yggdrasil
import java.io.*
import java.nio.ByteBuffer


class YggdrasilTunService : VpnService() {

    private lateinit var ygg: Yggdrasil
    private var isClosed = false

    /** Maximum packet size is constrained by the MTU, which is given as a signed short.  */
    private val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()

    companion object {
        private const val TAG = "Yggdrasil-service"
    }
    private var tunInterface: ParcelFileDescriptor? = null
    private var tunInputStream: InputStream? = null
    private var tunOutputStream: OutputStream? = null
    private var scope: CoroutineScope? = null
    private var address: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.getStringExtra(MainActivity.COMMAND)){
            MainActivity.STOP ->{
                val pi: PendingIntent? = intent.getParcelableExtra(MainActivity.PARAM_PINTENT)
                stopVpn(pi)
            }
            MainActivity.START ->{
                val peers = deserializeStringList2PeerInfoSet(intent.getStringArrayListExtra(MainActivity.PEERS))
                val dns = deserializeStringList2DNSInfoSet(intent.getStringArrayListExtra(MainActivity.DNS))
                val pi: PendingIntent = intent.getParcelableExtra(MainActivity.PARAM_PINTENT)
                ygg = Yggdrasil()
                setupTunInterface(pi, peers, dns)
            }
            MainActivity.UPDATE_DNS ->{
                val dns = deserializeStringList2DNSInfoSet(intent.getStringArrayListExtra(MainActivity.DNS))
                setupIOStreams(dns)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupIOStreams(dns: MutableSet<DNSInfo>){
        address = ygg.addressString
        var builder = Builder()
            .addAddress(address, 7)
            .allowFamily(OsConstants.AF_INET)
            .setMtu(MAX_PACKET_SIZE)
        if (dns.size > 0) {
            builder.addDnsServer(address)
            for (d in dns) {
                builder.addDnsServer(d.address)
            }
        }
        if(tunInterface!=null){
            tunInterface!!.close()
            tunInputStream!!.close()
            tunOutputStream!!.close()
        }
        tunInterface = builder.establish()
        tunInputStream = FileInputStream(tunInterface!!.fileDescriptor)
        tunOutputStream = FileOutputStream(tunInterface!!.fileDescriptor)

    }

    private fun setupTunInterface(
        pi: PendingIntent?,
        peers: Set<PeerInfo>,
        dns: MutableSet<DNSInfo>
    ) {
        pi!!.send(MainActivity.STATUS_START)
        var configJson = Mobile.generateConfigJSON()
        val gson = Gson()
        var config = gson.fromJson(String(configJson), Map::class.java).toMutableMap()
        config = fixConfig(config, peers)
        configJson = gson.toJson(config).toByteArray()

        var yggConduitEndpoint = ygg.startJSON(configJson)

        setupIOStreams(dns)

        val job = SupervisorJob()
        scope = CoroutineScope(Dispatchers.Default + job)
        scope!!.launch {
            val buffer = ByteArray(2048)
            while (!isClosed) {
                readPacketsFromTun(yggConduitEndpoint, buffer)
            }
        }
        scope!!.launch {
            while (!isClosed) {
                writePacketsToTun(yggConduitEndpoint)
            }
        }

        val intent: Intent = Intent().putExtra(MainActivity.IPv6, address)
        pi.send(this, MainActivity.STATUS_FINISH, intent)
    }

    private fun fixConfig(config: MutableMap<Any?, Any?>, peers: Set<PeerInfo>): MutableMap<Any?, Any?> {

        val whiteList = arrayListOf<String>()
        whiteList.add("")
        val blackList = arrayListOf<String>()
        blackList.add("")
        config["Peers"] = convertPeerInfoSet2PeerIdSet(peers)
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

    private fun readPacketsFromTun(yggConduitEndpoint: ConduitEndpoint, buffer: ByteArray) {
        // Read the outgoing packet from the input stream.
         try{
            val length = tunInputStream!!.read(buffer)
            if (length > 0) {
                val byteBuffer = ByteBuffer.allocate(length)
                byteBuffer.put(buffer, 0, length)
                yggConduitEndpoint.send(byteBuffer.array())
            } else {
                Thread.sleep(10)
            }
        }catch(e: IOException){
            e.printStackTrace()
        }
    }

    private fun writePacketsToTun(yggConduitEndpoint: ConduitEndpoint) {
        val buffer = yggConduitEndpoint.recv()
        if(buffer!=null) {
            try {
                tunOutputStream!!.write(buffer)
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun stopVpn(pi: PendingIntent?) {
        isClosed = true;
        scope!!.coroutineContext.cancelChildren()
        tunInputStream!!.close()
        tunOutputStream!!.close()
        tunInterface!!.close()
        tunInterface = null
        Log.d(TAG,"Stop is running from service")
        ygg.stop()
        val intent: Intent = Intent()
        pi!!.send(this, MainActivity.STATUS_STOP, intent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
    }
}
