package io.github.chronosx88.yggdrasil

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dummy.ConduitEndpoint
import io.github.chronosx88.yggdrasil.models.DNSInfo
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.config.Peer
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.convertPeer2PeerStringList
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.convertPeerInfoSet2PeerIdSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2DNSInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2PeerInfoSet
import kotlinx.coroutines.*
import mobile.Mobile
import mobile.Yggdrasil
import java.io.*
import java.net.Inet6Address

class YggdrasilTunService : VpnService() {

    private lateinit var ygg: Yggdrasil
    private var isClosed = false

    /** Maximum packet size is constrained by the MTU, which is given as a signed short - 256  */
    private val MAX_PACKET_SIZE = 65535

    companion object {
        private const val TAG = "Yggdrasil-service"
    }
    private var tunInterface: ParcelFileDescriptor? = null
    private var tunInputStream: InputStream? = null
    private var tunOutputStream: OutputStream? = null
    private var scope: CoroutineScope? = null
    private var address: String? = null

    private var mNotificationManager: NotificationManager? = null

    private val FOREGROUND_ID = 1338

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi: PendingIntent? = intent?.getParcelableExtra(MainActivity.PARAM_PINTENT)
        when(intent?.getStringExtra(MainActivity.COMMAND)){
            MainActivity.STOP ->{
                stopVpn(pi)
                startForeground(FOREGROUND_ID, foregroundNotification("Yggdrasil service stopped"))
            }
            MainActivity.START ->{
                val peers = deserializeStringList2PeerInfoSet(intent.getStringArrayListExtra(MainActivity.PEERS))
                val dns = deserializeStringList2DNSInfoSet(intent.getStringArrayListExtra(MainActivity.DNS))
                val staticIP: Boolean = intent.getBooleanExtra(MainActivity.STATIC_IP, false)
                ygg = Yggdrasil()
                setupTunInterface(pi, peers, dns, staticIP)
                startForeground(FOREGROUND_ID, foregroundNotification("Yggdrasil service started"))
            }
            MainActivity.UPDATE_DNS ->{
                val dns = deserializeStringList2DNSInfoSet(intent.getStringArrayListExtra(MainActivity.DNS))
                setupIOStreams(dns)
            }
            MainActivity.UPDATE_PEERS ->{
                sendMeshPeerStatus(pi)
            }
        }

        return START_NOT_STICKY
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
        /*
        fix for DNS unavailability
         */
        if(!hasIpv6DefaultRoute()){
            builder.addRoute("2000::",3)
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
        dns: MutableSet<DNSInfo>,
        staticIP: Boolean
    ) {
        pi!!.send(MainActivity.STATUS_START)
        var configJson = Mobile.generateConfigJSON()
        val gson = Gson()
        var config = gson.fromJson(String(configJson), Map::class.java).toMutableMap()
        config = fixConfig(config, peers, staticIP)

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

    private fun sendMeshPeerStatus(pi: PendingIntent?){
        class Token : TypeToken<List<Peer>>()
        var add = ygg.addressString
        var meshPeers: List<Peer> = gson.fromJson(ygg.peersJSON, Token().type)
        val intent: Intent = Intent().putStringArrayListExtra(
            MainActivity.MESH_PEERS,
            convertPeer2PeerStringList(meshPeers)
        );
        pi?.send(this, MainActivity.STATUS_PEERS_UPDATE, intent)

    }

    private fun fixConfig(
        config: MutableMap<Any?, Any?>,
        peers: Set<PeerInfo>,
        staticIP: Boolean
    ): MutableMap<Any?, Any?> {

        val whiteList = arrayListOf<String>()
        whiteList.add("")
        val blackList = arrayListOf<String>()
        blackList.add("")
        config["Peers"] = convertPeerInfoSet2PeerIdSet(peers)
        config["Listen"] = ""
        config["AdminListen"] = "tcp://localhost:9001"
        config["IfName"] = "tun0"
        if(staticIP) {
            val preferences =
                PreferenceManager.getDefaultSharedPreferences(this.baseContext)
            if(preferences.getString(MainActivity.STATIC_IP, null)==null) {
                val encryptionPublicKey = config["EncryptionPublicKey"].toString()
                val encryptionPrivateKey = config["EncryptionPrivateKey"].toString()
                val signingPublicKey = config["SigningPublicKey"].toString()
                val signingPrivateKey = config["SigningPrivateKey"].toString()
                preferences.edit()
                    .putString(MainActivity.signingPrivateKey, signingPrivateKey)
                    .putString(MainActivity.signingPublicKey, signingPublicKey)
                    .putString(MainActivity.encryptionPrivateKey, encryptionPrivateKey)
                    .putString(MainActivity.encryptionPublicKey, encryptionPublicKey)
                    .putString(MainActivity.STATIC_IP,MainActivity.STATIC_IP).apply()
            } else {
                val signingPrivateKey = preferences.getString(MainActivity.signingPrivateKey, null)
                val signingPublicKey = preferences.getString(MainActivity.signingPublicKey, null)
                val encryptionPrivateKey = preferences.getString(MainActivity.encryptionPrivateKey, null)
                val encryptionPublicKey = preferences.getString(MainActivity.encryptionPublicKey, null)

                config["SigningPrivateKey"] = signingPrivateKey
                config["SigningPublicKey"] = signingPublicKey
                config["EncryptionPrivateKey"] = encryptionPrivateKey
                config["EncryptionPublicKey"] = encryptionPublicKey
            }
        }

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
        try {
            // Read the outgoing packet from the input stream.
            val length = tunInputStream?.read(buffer) ?: 1
            if (length > 0){
                yggConduitEndpoint.send(buffer.sliceArray(IntRange(0, length - 1)))
            } else {
                Thread.sleep(100)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writePacketsToTun(yggConduitEndpoint: ConduitEndpoint) {
        val buffer = yggConduitEndpoint.recv()
        if(buffer!=null) {
            try {
                tunOutputStream?.write(buffer)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopVpn(pi: PendingIntent?) {
        isClosed = true;
        tunInputStream!!.close()
        tunOutputStream!!.close()
        tunInterface!!.close()
        tunInterface = null
        scope!!.coroutineContext.cancelChildren()
        Log.d(TAG,"Stop is running from service")
        ygg.stop()
        val intent: Intent = Intent()
        pi!!.send(this, MainActivity.STATUS_STOP, intent)
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopSelf()
    }

    private fun hasIpv6DefaultRoute(): Boolean {
        val cm =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = cm.allNetworks
        for (network in networks) {
            val linkProperties = cm.getLinkProperties(network)
            val routes = linkProperties.routes
            for (route in routes) {
                if (route.isDefaultRoute && route.gateway is Inet6Address) {
                    return true
                }
            }
        }
        return false
    }

    private fun foregroundNotification(text: String): Notification? {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(TAG, "Yggdrasil service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }
        val b = NotificationCompat.Builder(this, channelId)
        b.setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
        return b.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = getColor(R.color.dark_10)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}
