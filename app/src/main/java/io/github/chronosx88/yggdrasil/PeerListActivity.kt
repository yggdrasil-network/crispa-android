package io.github.chronosx88.yggdrasil

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.hbb20.CountryCodePicker
import io.github.chronosx88.yggdrasil.models.config.PeerInfo
import io.github.chronosx88.yggdrasil.models.config.PeerInfoListAdapter
import java.net.Inet4Address


class PeerListActivity : AppCompatActivity() {

    companion object {
        val peers = arrayListOf(
            PeerInfo(
                "tcp",
                Inet4Address.getByName("194.177.21.156"),
                5066,
                "RU",
                CountryCodePicker.Language.RUSSIAN
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("46.151.26.194"),
                60575,
                "RU",
                CountryCodePicker.Language.RUSSIAN
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("188.226.125.64"),
                54321,
                "RU",
                CountryCodePicker.Language.RUSSIAN
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("88.201.129.205"),
                8777,
                "RU",
                CountryCodePicker.Language.RUSSIAN
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("45.11.19.26"),
                5001,
                "DE",
                CountryCodePicker.Language.GERMAN
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("82.165.69.111"),
                61216,
                "DE",
                CountryCodePicker.Language.GERMAN
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("104.248.15.125"),
                31337,
                "US",
                CountryCodePicker.Language.ENGLISH
            ),
            PeerInfo(
                "tcp",
                Inet4Address.getByName("108.175.10.127"),
                61216,
                "US",
                CountryCodePicker.Language.ENGLISH
            )
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peer_list)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        var extras = intent.getExtras()
        var peerList = findViewById<ListView>(R.id.peerList)
        if (extras != null) {
            var currentPeers = extras.getStringArrayList(MainActivity.PEER_LIST)!!
            var adapter = PeerInfoListAdapter(this, peers, currentPeers)
            peerList.adapter = adapter
        } else {
            var adapter = PeerInfoListAdapter(this, peers, ArrayList())
            peerList.adapter = adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.save_peers, menu)
        val item = menu.findItem(R.id.saveItem) as MenuItem
        item.setActionView(R.layout.menu_save)
        val saveButton = item
            .actionView.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val result = Intent(this, MainActivity::class.java)
            var adapter = findViewById<ListView>(R.id.peerList).adapter as PeerInfoListAdapter
            val selectedPeers = adapter.getSelectedPeers()
            if(selectedPeers.size>0) {
                result.putExtra(MainActivity.PEER_LIST, adapter.getSelectedPeers())
                setResult(Activity.RESULT_OK, result)
                finish()
            } else {
                val text = "Select at least one peer"
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(applicationContext, text, duration)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }

        }
        return true
    }
}