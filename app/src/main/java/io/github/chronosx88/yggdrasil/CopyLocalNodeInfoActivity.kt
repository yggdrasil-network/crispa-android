package io.github.chronosx88.yggdrasil

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.chronosx88.yggdrasil.models.NodeInfo
import io.github.chronosx88.yggdrasil.models.config.CopyInfoAdapter
import io.github.chronosx88.yggdrasil.models.config.SelectDNSInfoListAdapter

class CopyLocalNodeInfoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_copy_local_node_info)
        setSupportActionBar(findViewById(R.id.toolbar))
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        val ipv6Address = intent.extras!!.getString(MainActivity.IPv6, "")
        val signingPublicKey = preferences.getString(MainActivity.signingPublicKey, "")
        val encryptionPublicKey = preferences.getString(MainActivity.encryptionPublicKey, "")
        var nodeInfoListView = findViewById<ListView>(R.id.nodeInfoList)
        val nodeInfoList = listOf<NodeInfo>(NodeInfo("IP address", ipv6Address!!), NodeInfo("Encryption Public Key", encryptionPublicKey!!), NodeInfo("Signing Public Key", signingPublicKey!!));
        var adapter = CopyInfoAdapter(this, nodeInfoList)
        nodeInfoListView.adapter = adapter
    }

}