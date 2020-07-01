package io.github.chronosx88.yggdrasil

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hbb20.CCPCountry
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.Status
import io.github.chronosx88.yggdrasil.models.config.SelectPeerInfoListAdapter
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.deserializeStringList2PeerInfoSet
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.ping
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.serializePeerInfoSet2StringList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.net.*
import java.nio.charset.Charset


class PeerListActivity : AppCompatActivity() {

    companion object {
        const val PEER_LIST_URL = "https://publicpeers.neilalexander.dev/publicnodes.json"
    }

    fun downloadJson(link: String): String {
        URL(link).openStream().use { input ->
            var outStream = ByteArrayOutputStream()
            outStream.use { output ->
                input.copyTo(output)
            }
            return String(outStream.toByteArray(), Charset.forName("UTF-8"))
        }
    }

    var isLoading = true;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peer_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            addNewPeer()
        }
        var extras = intent.extras
        var peerList = findViewById<ListView>(R.id.peerList)
        var adapter = SelectPeerInfoListAdapter(this, arrayListOf(), mutableSetOf())
        peerList.adapter = adapter

        GlobalScope.launch {
            try {
                var cp = deserializeStringList2PeerInfoSet(
                    extras!!.getStringArrayList(MainActivity.PEER_LIST)!!
                )
                for(pi in cp){
                    var ping = ping(pi.address, pi.port)
                    pi.ping = ping
                }
                var json = downloadJson(PEER_LIST_URL)
                var countries = CCPCountry.getLibraryMasterCountriesEnglish()
                val mapType: Type = object :
                    TypeToken<Map<String?, Map<String, Status>>>() {}.type
                val peersMap: Map<String, Map<String, Status>> = Gson().fromJson(json, mapType)
                for ((country, peers) in peersMap.entries) {
                    for ((peer, status) in peers) {
                        if (status.up) {
                            for (ccp in countries) {
                                if (ccp.name.toLowerCase()
                                        .contains(country.replace(".md", "").replace("-", " "))
                                ) {
                                    var url = URI(peer)
                                    try {
                                        var address = InetAddress.getByName(url.host)
                                        var peerInfo =
                                            PeerInfo(url.scheme, address, url.port, ccp.nameCode)
                                        if(cp.contains(peerInfo)){
                                            continue
                                        }
                                        var ping = ping(address, url.port)
                                        peerInfo.ping = ping
                                        withContext(Dispatchers.Main) {
                                            adapter.addItem(peerInfo)
                                            if(adapter.count % 5 == 0) {
                                                adapter.sort()
                                            }
                                        }
                                    } catch (e: Throwable){
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
                var currentPeers = ArrayList(cp.sortedWith(compareBy { it.ping }))
                withContext(Dispatchers.Main) {
                    adapter.addAll(0, currentPeers)
                    isLoading = false
                    adapter.setLoading(isLoading)
                }
            } catch (e: Throwable){
                e.printStackTrace()
            }
        }
    }

    private fun addNewPeer() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.new_peer_dialog, null)
        val ab: AlertDialog.Builder = AlertDialog.Builder(this)
        ab.setCancelable(true).setView(view)
        ab.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.save, menu)
        val item = menu.findItem(R.id.saveItem) as MenuItem
        item.setActionView(R.layout.menu_save)
        val saveButton = item
            .actionView.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            if(isLoading){
                return@setOnClickListener
            }
            val result = Intent(this, MainActivity::class.java)
            var adapter = findViewById<ListView>(R.id.peerList).adapter as SelectPeerInfoListAdapter
            val selectedPeers = adapter.getSelectedPeers()
            /* WiFi Direct test - no peers is needed
            if(selectedPeers.size>0) {
                result.putExtra(MainActivity.PEER_LIST, serializePeerInfoSet2StringList(selectedPeers))
                setResult(Activity.RESULT_OK, result)
                finish()
            } else {
                val text = "Select at least one peer"
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(applicationContext, text, duration)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }*/
            result.putExtra(MainActivity.PEER_LIST, serializePeerInfoSet2StringList(selectedPeers))
            setResult(Activity.RESULT_OK, result)
            finish()
        }
        return true
    }
}