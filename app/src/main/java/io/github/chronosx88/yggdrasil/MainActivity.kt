package io.github.chronosx88.yggdrasil

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.net.VpnService
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import io.github.chronosx88.yggdrasil.models.DNSInfo
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.config.DNSInfoListAdapter
import io.github.chronosx88.yggdrasil.models.config.PeerInfoListAdapter
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2DNSInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2PeerInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringSet2DNSInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringSet2PeerInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.serializeDNSInfoSet2StringList
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.serializePeerInfoSet2StringList


class MainActivity : AppCompatActivity() {

    companion object {
        const val COMMAND = "COMMAND"
        const val STOP = "STOP"
        const val START = "START"
        const val UPDATE_DNS = "UPDATE_DNS"
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
        const val CURRENT_PEERS = "CURRENT_PEERS_v1.2"
        const val CURRENT_DNS = "CURRENT_DNS_v1.2"
        const val START_VPN = "START_VPN"
        private const val TAG="Yggdrasil"
        private const val VPN_REQUEST_CODE = 0x0F

        @JvmStatic var isStarted = false
        @JvmStatic var isCancelled = false
        @JvmStatic var address = ""
    }

    private var receiver: MainActivity.WiFiDirectBroadcastReceiver? = null
    private var mChannel: WifiP2pManager.Channel? = null
    private var mManager: WifiP2pManager? = null
    private var currentPeers = setOf<PeerInfo>()
    private var currentDNS = setOf<DNSInfo>()

    private val wirelessPeers = mutableListOf<WifiP2pDevice>()
    private val intentFilter = IntentFilter()

    private val peerListListener = WirelessPeerList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        /*p2p part*/
        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        isStarted = isYggServiceRunning(this)
        val switchOn = findViewById<Switch>(R.id.switchOn)
        switchOn.isChecked = isStarted
        val wifiDirect = findViewById<Switch>(R.id.wifiDirect)
        switchOn.setOnCheckedChangeListener { _, isChecked ->
            if(currentPeers.isEmpty() && !wifiDirect.isChecked){
                switchOn.isChecked = false
                return@setOnCheckedChangeListener
            }
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


        wifiDirect.setOnCheckedChangeListener { _, isChecked ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnCheckedChangeListener
            }
            mManager!!.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Code for when the discovery initiation is successful goes here.
                    // No services have actually been discovered yet, so this method
                    // can often be left blank.  Code for peer discovery goes in the
                    // onReceive method, detailed below.
                    //showToast("discover peers success")
                }

                override fun onFailure(reasonCode: Int) {
                    // Code for when the discovery initiation fails goes here.
                    // Alert the user that something went wrong.
                    showToast("discover peers failed, code="+reasonCode)
                }
            })

        }

        val peersListView = findViewById<ListView>(R.id.peers)
        //save to shared preferences
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
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
            STATUS_START -> print("service started")
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

    inner class WiFiDirectBroadcastReceiver(mManager: WifiP2pManager,
                                              mChannel: WifiP2pManager.Channel): BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                val state = intent!!.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    this@MainActivity.setIsWifiP2pEnabled(true)
                } else {
                    this@MainActivity.setIsWifiP2pEnabled(false)
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
                // The peer list has changed!  We should probably do something about
                // that.
                // Request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                if (mManager != null) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    mManager!!.requestPeers(mChannel, peerListListener);
                }
                Log.d(TAG, "P2P peers changed");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {

                // Connection state changed!  We should probably do something about
                // that.
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
                this@MainActivity.updateThisDevice(
                    intent!!.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                    ) as WifiP2pDevice
                )
            }
        }
    }

    private fun updateThisDevice(wifiP2pDevice: WifiP2pDevice) {
        //showToast("update device:"+wifiP2pDevice.deviceName+" address:"+wifiP2pDevice.deviceAddress)
    }

    private fun setIsWifiP2pEnabled(b: Boolean) {
        //showToast("WifiP2pEnabled="+b)
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    override fun onResume() {
        super.onResume()
        mManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager!!.initialize(this, mainLooper, null);
        receiver = WiFiDirectBroadcastReceiver(mManager!!, mChannel!!)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    inner class WirelessPeerList:WifiP2pManager.PeerListListener{

        override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
            // Out with the old, in with the new.
            this@MainActivity.wirelessPeers.clear()
            this@MainActivity.wirelessPeers.addAll(peers!!.deviceList);
            // If an AdapterView is backed by this data, notify it // of the change. For instance, if you have a ListView of available // peers, trigger an update.
            //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged(); if (peers.size() == 0) { Log.d(WiFiDirectActivity.TAG, "No devices found"); return; } }
            //just show message
            //showToast("available peers:"+this@MainActivity.wirelessPeers.size)
        }

    }
}
