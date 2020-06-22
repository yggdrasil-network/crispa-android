package io.github.chronosx88.yggdrasil

import android.app.Activity
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
import com.google.gson.Gson
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.config.PeerInfoListAdapter
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


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
        const val PEER_LIST_CODE = 1000
        const val PEER_LIST = "PEERS_LIST"
        const val CURRENT_PEERS = "CURRENT_PEER_INFO"
        const val START_VPN = "START_VPN"
        private const val TAG="Yggdrasil"
        private const val VPN_REQUEST_CODE = 0x0F

        @JvmStatic
        fun deserializeStringList2PeerInfoSet(list: List<String>): MutableSet<PeerInfo> {
            var gson = Gson()
            var out = mutableSetOf<PeerInfo>()
            for(s in list) {
                out.add(gson.fromJson(s, PeerInfo::class.java))
            }
            return out
        }

        @JvmStatic
        fun deserializeStringSet2PeerInfoSet(list: Set<String>): MutableSet<PeerInfo> {
            var gson = Gson()
            var out = mutableSetOf<PeerInfo>()
            for(s in list) {
                out.add(gson.fromJson(s, PeerInfo::class.java))
            }
            return out
        }

        @JvmStatic
        fun serializePeerInfoSet2StringList(list: Set<PeerInfo>): ArrayList<String> {
            var gson = Gson()
            var out = ArrayList<String>()
            for(p in list) {
                out.add(gson.toJson(p))
            }
            return out
        }
    }

    private var startVpnFlag = false
    private var currentPeers = setOf<PeerInfo>()
    private var isStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val listView = findViewById<ListView>(R.id.peers)
        //save to shared preferences
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        currentPeers = deserializeStringSet2PeerInfoSet(preferences.getStringSet(CURRENT_PEERS, HashSet())!!)

        val adapter = PeerInfoListAdapter(this, ArrayList(currentPeers))
        listView.adapter = adapter
        val editPeersButton = findViewById<Button>(R.id.edit)
        editPeersButton.setOnClickListener {
            if(isStarted){
                showToast("Service is running. Please stop service before edit Peers list")
                return@setOnClickListener
            }
            val intent = Intent(this, PeerListActivity::class.java)
            intent.putStringArrayListExtra(PEER_LIST, serializePeerInfoSet2StringList(currentPeers))
            startActivityForResult(intent, PEER_LIST_CODE)
        }
        if(intent.extras!==null) {
            startVpnFlag = intent.extras!!.getBoolean(START_VPN, false)
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
        val ipLayout = findViewById<LinearLayout>(R.id.ipLayout)
        ipLayout.visibility = View.VISIBLE
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
            if(currentPeers.size==0){
                showToast("No peers selected!")
                return
            }
            val intent = Intent(this, YggdrasilTunService::class.java)
            val TASK_CODE = 100
            val pi = createPendingResult(TASK_CODE, intent, 0)
            intent.putExtra(PARAM_PINTENT, pi)
            intent.putExtra(COMMAND, START)
            intent.putStringArrayListExtra(PEERS, serializePeerInfoSet2StringList(currentPeers))
            startService(intent)
        }
        if (requestCode == PEER_LIST_CODE && resultCode== Activity.RESULT_OK){
            if(data!!.extras!=null){
                var currentPeers = data.extras!!.getStringArrayList(PEER_LIST)
                if(currentPeers==null || currentPeers.size==0){
                    showToast("No peers selected!")
                } else {
                    this.currentPeers = deserializeStringList2PeerInfoSet(currentPeers)
                    val adapter = PeerInfoListAdapter(this, ArrayList(this.currentPeers))
                    val listView = findViewById<ListView>(R.id.peers)
                    listView.adapter = adapter

                    //save to shared preferences
                    val preferences =
                        PreferenceManager.getDefaultSharedPreferences(this.baseContext)
                    preferences.edit().putStringSet(CURRENT_PEERS, HashSet(currentPeers)).apply()
                    if(isStarted){
                        //apply peer changes
                        stopVpn()
                        val i = baseContext.packageManager
                            .getLaunchIntentForPackage(baseContext.packageName)
                        i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.putExtra(START_VPN, true)
                        finish()
                        startActivity(i)

                    }
                }
            }
        }
        when (resultCode) {
            STATUS_START -> print("service started")
            STATUS_FINISH -> {
                val result: String = data!!.getStringExtra(IPv6)
                findViewById<TextView>(R.id.ip).text = result
                isStarted = true
            }
            STATUS_STOP -> {
                isStarted = false
                finish()
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
        if(startVpnFlag){
            switchOn.isChecked = true
            startVpnFlag = false
            startVpn()
        } else {
            switchOn.isChecked = false
        }
        switchOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startVpn()
            } else {
                stopVpn()
            }
        }
        return true
    }

    fun showToast(text: String){
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(applicationContext, text, duration)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

}
