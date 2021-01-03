package io.github.chronosx88.yggdrasil

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.chronosx88.yggdrasil.models.config.NodeInfoListAdapter

class CopyLocalNodeInfoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_copy_local_node_info)
        setSupportActionBar(findViewById(R.id.toolbar))
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        val ipv6Address = intent.extras!!.getString(MainActivity.IPv6, "")
        val signingPublicKey = preferences.getString(MainActivity.signingPublicKey, "***")
        val encryptionPublicKey = preferences.getString(MainActivity.encryptionPublicKey, "***")
        var nodeInfoListView = findViewById<RecyclerView>(R.id.node_info_list)
        val nodeInfoList = listOf<Pair<String, String>>(Pair("IP address", ipv6Address!!), Pair("Encryption Public Key", encryptionPublicKey!!), Pair("Signing Public Key", signingPublicKey!!));
        val adapter =
            NodeInfoListAdapter(
                this,
                nodeInfoList.toTypedArray()
            )
        nodeInfoListView.adapter = adapter
        nodeInfoListView.layoutManager = LinearLayoutManager(this)

        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
        getSupportActionBar()?.setDisplayShowHomeEnabled(true);
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}