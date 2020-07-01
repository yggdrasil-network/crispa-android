package io.github.chronosx88.yggdrasil

import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.chronosx88.yggdrasil.models.DNSInfo
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.config.DNSInfoListAdapter
import io.github.chronosx88.yggdrasil.models.config.PeerInfoListAdapter
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializePeerStringList2PeerInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2DNSInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2PeerInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringSet2DNSInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringSet2PeerInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.serializeDNSInfoSet2StringList
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.serializePeerInfoSet2StringList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    companion object {
        const val STATUS_PEERS_UPDATE = 12
        const val MESH_PEERS = "MESH_PEERS"
        const val STATIC_IP = "STATIC_IP_FLAG"
        const val signingPrivateKey = "signingPrivateKey"
        const val signingPublicKey = "signingPublicKey"
        const val encryptionPrivateKey = "encryptionPrivateKey"
        const val encryptionPublicKey = "encryptionPublicKey"
        const val COMMAND = "COMMAND"
        const val STOP = "STOP"
        const val START = "START"
        const val UPDATE_DNS = "UPDATE_DNS"
        const val UPDATE_PEERS = "UPDATE_PEERS"
        const val PARAM_PINTENT = "pendingIntent"
        const val STATUS_START = 7
        const val STATUS_FINISH = 8
        const val STATUS_STOP = 9
        const val IPv6: String = "IPv6"
        const val PEERS: String = "PEERS"
        const val DNS: String = "DNS"
        const val PEER_LIST_CODE = 1000
        const val DNS_LIST_CODE = 2000
        const val PEER_LIST = "PEERS_LIST"
        const val DNS_LIST = "DNS_LIST"
        const val CURRENT_PEERS = "CURRENT_PEERS_v1.2.1"
        const val CURRENT_DNS = "CURRENT_DNS_v1.2"
        const val START_VPN = "START_VPN"
        private const val TAG="Yggdrasil"
        private const val VPN_REQUEST_CODE = 0x0F

        @JvmStatic var isStarted = false
        @JvmStatic var isCancelled = false
        @JvmStatic var address = ""
    }

    private var currentPeers = setOf<PeerInfo>()
    private var currentDNS = setOf<DNSInfo>()
    private var meshPeersReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        isStarted = isYggServiceRunning(this)
        val switchOn = findViewById<Switch>(R.id.switchOn)
        switchOn.isChecked = isStarted

        switchOn.setOnCheckedChangeListener { _, isChecked ->
            if(isCancelled){
                switchOn.isChecked = false
                isCancelled = false
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                startVpn()
            } else {
                stopVpn()
            }
        }
        //save to shared preferences
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        val staticIP = findViewById<Switch>(R.id.staticIP)
        staticIP.isChecked =
            preferences.getString(STATIC_IP, null) != null
        val peersListView = findViewById<ListView>(R.id.peers)

        currentPeers = deserializeStringSet2PeerInfoSet(preferences.getStringSet(CURRENT_PEERS, HashSet())!!)
        val adapter = PeerInfoListAdapter(this, currentPeers.sortedWith(compareBy { it.ping }))
        peersListView.adapter = adapter

        val copyAddressButton = findViewById<Button>(R.id.copyIp)
        copyAddressButton.setOnClickListener {
            val ip = findViewById<TextView>(R.id.ip)
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText("IP address", ip.text.toString())
            clipboard.setPrimaryClip(clip)
            showToast(getString(R.string.address_copied))
        }
        val editPeersButton = findViewById<Button>(R.id.edit)
        editPeersButton.setOnClickListener {
            if(isStarted){
                showToast("Service is running. Please stop service before edit Peers list")
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity, PeerListActivity::class.java)
            intent.putStringArrayListExtra(PEER_LIST, serializePeerInfoSet2StringList(currentPeers))
            startActivityForResult(intent, PEER_LIST_CODE)
        }

        val listViewDNS = findViewById<ListView>(R.id.dns)
        currentDNS = deserializeStringSet2DNSInfoSet(preferences.getStringSet(CURRENT_DNS, HashSet())!!)
        val adapterDns = DNSInfoListAdapter(this, currentDNS.sortedWith(compareBy { it.ping }))
        listViewDNS.adapter = adapterDns
        val editDnsButton = findViewById<Button>(R.id.editDNS)
        editDnsButton.setOnClickListener {
            if(!isStarted){
                showToast("Service is not running. DNS ping will not be run")
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity, DNSListActivity::class.java)
            intent.putStringArrayListExtra(DNS_LIST, serializeDNSInfoSet2StringList(currentDNS))
            startActivityForResult(intent, DNS_LIST_CODE)
        }
        if(isStarted){
            val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
            ipLayout.visibility = View.VISIBLE
            findViewById<TextView>(R.id.ip).text = address
        }

    }

    private fun stopVpn(){
        Log.d(TAG,"Stop")
        val intent = Intent(this, YggdrasilTunService::class.java)
        val TASK_CODE = 100
        val pi = createPendingResult(TASK_CODE, intent, 0)
        intent.putExtra(PARAM_PINTENT, pi)
        intent.putExtra(COMMAND, STOP)
        startService(intent)
    }

    private fun startVpn(){
        Log.d(TAG,"Start")
        val intent= VpnService.prepare(this)
        if (intent!=null){
            startActivityForResult(intent, VPN_REQUEST_CODE)
        }else{
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
        }
    }

    private fun updateDNS(){
        Log.d(TAG,"Update DNS")
        val intent = Intent(this, YggdrasilTunService::class.java)
        val TASK_CODE = 100
        val pi = createPendingResult(TASK_CODE, intent, 0)
        intent.putExtra(PARAM_PINTENT, pi)
        intent.putExtra(COMMAND, UPDATE_DNS)
        intent.putStringArrayListExtra(DNS, serializeDNSInfoSet2StringList(currentDNS))
        startService(intent)
    }

    private fun updatePeers(){
        Log.d(TAG,"Update Peers")
        val intent = Intent(this, YggdrasilTunService::class.java)
        val TASK_CODE = 100
        val pi = createPendingResult(TASK_CODE, intent, 0)
        intent.putExtra(PARAM_PINTENT, pi)
        intent.putExtra(COMMAND, UPDATE_PEERS)
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VPN_REQUEST_CODE && resultCode== Activity.RESULT_OK){
            /*
            if(currentPeers.isEmpty()){
                showToast("No peers selected!")
                return
            }*/
            val intent = Intent(this, YggdrasilTunService::class.java)
            val TASK_CODE = 100
            val pi = createPendingResult(TASK_CODE, intent, 0)
            intent.putExtra(PARAM_PINTENT, pi)
            intent.putExtra(COMMAND, START)
            intent.putStringArrayListExtra(PEERS, serializePeerInfoSet2StringList(currentPeers))
            intent.putStringArrayListExtra(DNS, serializeDNSInfoSet2StringList(currentDNS))
            intent.putExtra(STATIC_IP, findViewById<Switch>(R.id.staticIP).isChecked)

            startService(intent)
        }
        if (requestCode == VPN_REQUEST_CODE && resultCode== Activity.RESULT_CANCELED){
            isCancelled = true
        }
        if (requestCode == PEER_LIST_CODE && resultCode== Activity.RESULT_OK){
            if(data!!.extras!=null){
                var currentPeers = data.extras!!.getStringArrayList(PEER_LIST)
                /*WiFi Direct test. need peer empty list*/
                //if(currentPeers==null || currentPeers.size==0){
                //    showToast("No peers selected!")
                //} else {
                    this.currentPeers = deserializeStringList2PeerInfoSet(currentPeers)
                    val adapter = PeerInfoListAdapter(this, this.currentPeers.sortedWith(compareBy { it.ping }))
                    val listView = findViewById<ListView>(R.id.peers)
                    listView.adapter = adapter

                    //save to shared preferences
                    val preferences =
                        PreferenceManager.getDefaultSharedPreferences(this.baseContext)
                    preferences.edit().putStringSet(CURRENT_PEERS, HashSet(currentPeers)).apply()
                    if(isStarted){
                        //TODO implement UpdateConfig method in native interface and apply peer changes
                        stopVpn()
                        val i = baseContext.packageManager
                            .getLaunchIntentForPackage(baseContext.packageName)
                        i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.putExtra(START_VPN, true)
                        startActivity(i)
                    }
                //}
            }
        }

        if (requestCode == DNS_LIST_CODE && resultCode== Activity.RESULT_OK){
            if(data!!.extras!=null){
                var currentDNS = data.extras!!.getStringArrayList(DNS_LIST)
                if(currentDNS==null || currentDNS.size==0){
                    showToast("No DNS selected!")
                } else {
                    this.currentDNS = deserializeStringList2DNSInfoSet(currentDNS)
                    val adapter = DNSInfoListAdapter(this, this.currentDNS.sortedWith(compareBy { it.ping }))
                    val listView = findViewById<ListView>(R.id.dns)
                    listView.adapter = adapter
                    //save to shared preferences
                    val preferences =
                        PreferenceManager.getDefaultSharedPreferences(this.baseContext)
                    preferences.edit().putStringSet(CURRENT_DNS, HashSet(currentDNS)).apply()
                    if(isStarted){
                        updateDNS()
                    }
                }
            }
        }

        when (resultCode) {
            STATUS_START -> {
                print("service started")
                if(this.currentPeers.isEmpty()){
                    //this is Mesh mode, send Peers update every 5 sec
                    thread(start = true) {
                        while(true) {
                            Thread.sleep(5000)
                            if(isStarted) {
                                updatePeers()
                            }
                        }
                    }
                }
            }
            STATUS_FINISH -> {
                isStarted = true
                val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
                ipLayout.visibility = View.VISIBLE
                address = data!!.getStringExtra(IPv6)
                findViewById<TextView>(R.id.ip).text = address
            }
            STATUS_STOP -> {
                isStarted = false
                val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
                ipLayout.visibility = View.GONE
            }
            STATUS_PEERS_UPDATE ->{
                if(data!!.extras!=null) {
                    thread(start = true) {
                        val meshPeers = deserializePeerStringList2PeerInfoSet(
                            data.extras!!.getStringArrayList(MESH_PEERS)
                        )
                        val listView = findViewById<ListView>(R.id.peers)
                        val adapter = PeerInfoListAdapter(
                            this@MainActivity,
                            meshPeers.sortedWith(compareBy { it.ping })
                        )
                        runOnUiThread {
                            listView.adapter = adapter
                        }
                    }
                }
            }
            else -> { // Note the block

            }
        }
    }

    private fun showToast(text: String){
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    //TODO reimplement it
    private fun isYggServiceRunning(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (YggdrasilTunService::class.java.getName() == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        findViewById<Switch>(R.id.staticIP).isChecked =
            preferences.getString(STATIC_IP, null) != null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (meshPeersReceiver != null){
            unregisterReceiver(meshPeersReceiver);
        }
    }

}
