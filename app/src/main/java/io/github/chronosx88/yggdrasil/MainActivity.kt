package io.github.chronosx88.yggdrasil

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        const val CURRENT_PEERS = "CURRENT_PEERS_v1.1"
        const val CURRENT_DNS = "CURRENT_DNS_v1.1"
        const val START_VPN = "START_VPN"
        private const val TAG="Yggdrasil"
        private const val VPN_REQUEST_CODE = 0x0F

        @JvmStatic var isStarted = false

    }

    private var currentPeers = setOf<PeerInfo>()
    private var currentDNS = setOf<DNSInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isStarted = isYggServiceRunning(this)
        val listView = findViewById<ListView>(R.id.peers)
        //save to shared preferences
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        currentPeers = deserializeStringSet2PeerInfoSet(preferences.getStringSet(CURRENT_PEERS, HashSet())!!)
        val adapter = PeerInfoListAdapter(this, currentPeers.sortedWith(compareBy { it.ping }))
        listView.adapter = adapter

        val copyAddressButton = findViewById<Button>(R.id.copyIp)
        copyAddressButton.setOnClickListener {
            val ipLabel = findViewById<TextView>(R.id.ipLabel)
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText("IP address", ipLabel.text.toString())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VPN_REQUEST_CODE && resultCode== Activity.RESULT_OK){
            if(currentPeers.isEmpty()){
                showToast("No peers selected!")
                return
            }
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
            //TODO implement
        }
        if (requestCode == PEER_LIST_CODE && resultCode== Activity.RESULT_OK){
            if(data!!.extras!=null){
                var currentPeers = data.extras!!.getStringArrayList(PEER_LIST)
                if(currentPeers==null || currentPeers.size==0){
                    showToast("No peers selected!")
                } else {
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
                }
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
                        //TODO implement UpdateConfig method in native interface and apply peer changes
                        stopVpn()
                        val i = baseContext.packageManager
                            .getLaunchIntentForPackage(baseContext.packageName)
                        i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.putExtra(START_VPN, true)
                        startActivity(i)
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
                val result: String = data!!.getStringExtra(IPv6)
                findViewById<TextView>(R.id.ip).text = result
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        val item = menu.findItem(R.id.switchId) as MenuItem
        item.setActionView(R.layout.menu_switch)
        val switchOn = item
            .actionView.findViewById<Switch>(R.id.switchOn)
        if(isStarted){
            switchOn.isChecked = true
        }
        switchOn.setOnCheckedChangeListener { _, isChecked ->
            if(currentPeers.isEmpty()){
                switchOn.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                startVpn()
            } else {
                stopVpn()
            }

        }
        return true
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
}
