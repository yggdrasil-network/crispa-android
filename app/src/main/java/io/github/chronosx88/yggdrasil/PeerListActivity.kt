package io.github.chronosx88.yggdrasil

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hbb20.CCPCountry
import io.github.chronosx88.yggdrasil.models.PeerInfo
import io.github.chronosx88.yggdrasil.models.Status
import io.github.chronosx88.yggdrasil.models.config.DropDownAdapter
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
import java.net.InetAddress
import java.net.URI
import java.net.URL
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

    var popup: PopupWindow? = null
    var adapter: DropDownAdapter? = null

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
        val countryCode: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.resources.configuration.locales[0].country
        } else {
            this.resources.configuration.locale.country
        }
        var schemaInput = view.findViewById<TextView>(R.id.schemaInput)
        var ipInput = view.findViewById<TextView>(R.id.ipInput)
        ipInput.requestFocus()
        schemaInput.showSoftInputOnFocus = false
        schemaInput.setOnFocusChangeListener { v, b ->
            if(schemaInput.isFocused) {
                onClickSchemaList(v)
            }
        }
        schemaInput.setOnClickListener { v->
            onClickSchemaList(v)
        }
        getPopupWindow(R.layout.spinner_item, resources.getStringArray(R.array.schemas), schemaInput, getString(R.string.schema));
        var ccp = view.findViewById<com.hbb20.CountryCodePicker>(R.id.ccp)
        ccp.setCountryForNameCode(countryCode)
        val ab: AlertDialog.Builder = AlertDialog.Builder(this)
        ab.setCancelable(true).setView(view)
        var ad = ab.show()
        var addButton = view.findViewById<Button>(R.id.add)
        addButton.setOnClickListener{
            var portInput = view.findViewById<TextView>(R.id.portInput)
            var ccpInput = view.findViewById<com.hbb20.CountryCodePicker>(R.id.ccp)
            var schema = schemaInput.text.toString().toLowerCase()
            var ip = ipInput.text.toString().toLowerCase()
            var port = portInput.text.toString().toInt()
            var ccp = ccpInput.selectedCountryNameCode
            GlobalScope.launch {
                var pi = PeerInfo(schema, InetAddress.getByName(ip), port, ccp)
                try {
                    var ping = ping(pi.address, pi.port)
                    pi.ping = ping
                } catch(e: Throwable){
                    pi.ping = Int.MAX_VALUE
                }
                withContext(Dispatchers.Main) {
                    var selectAdapter = (findViewById<ListView>(R.id.peerList).adapter as SelectPeerInfoListAdapter)
                    selectAdapter.addItem(0, pi)
                    selectAdapter.notifyDataSetChanged()
                    ad.dismiss()
                }
            }
        }
    }

    fun onClickSchemaList(v: View) {
        val height = -1 * v.height +30
        getAddressListPopup()?.showAsDropDown(v, -5, height)
    }

    private fun getAddressListPopup(): PopupWindow? {
        return popup
    }

    private fun getPopupWindow(
        textViewResourceId: Int,
        objects: Array<String>,
        editText: TextView,
        hint: String?
    ): PopupWindow? {
        // initialize a pop up window type
        val popupWindow = PopupWindow(this)
        // the drop down list is a list view
        val listView = ListView(this)
        listView.dividerHeight = 0
        // set our adapter and pass our pop up window contents
        adapter = DropDownAdapter(this, textViewResourceId, objects, popupWindow, editText)
        listView.adapter = adapter
        // set the item click listener
        listView.onItemClickListener = adapter
        // some other visual settings
        popupWindow.isFocusable = true
        //popupWindow.setWidth(400);
        val display: Display =
            (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).getDefaultDisplay()
        popupWindow.width = 320
        popupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        // set the list view as pop up window content
        popupWindow.contentView = listView
        popup = popupWindow
        return popupWindow
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