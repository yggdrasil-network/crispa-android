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
import io.github.chronosx88.yggdrasil.models.DNSInfo
import io.github.chronosx88.yggdrasil.models.config.SelectDNSInfoListAdapter
import io.github.chronosx88.yggdrasil.models.config.Utils.Companion.ping
import kotlinx.coroutines.*
import java.net.*
import kotlin.concurrent.thread


class DNSListActivity : AppCompatActivity() {

    companion object {
        val allDNS = arrayListOf(
            DNSInfo(
            InetAddress.getByName("[301:2522::53]"),
            "CZ",
            "DNS implementation for Yggdrasil. https://github.com/Revertron/wyrd"),
            DNSInfo(InetAddress.getByName("[301:2923::53]"),
            "SK",
            "DNS implementation for Yggdrasil. https://github.com/Revertron/wyrd"),
            DNSInfo(InetAddress.getByName("[300:4523::53]"),
                "DE",
                "DNS implementation for Yggdrasil. https://github.com/Revertron/wyrd"),
            DNSInfo(InetAddress.getByName("[303:8b1a::53]"),
            "RU",
            "DNS implementation for Yggdrasil. https://github.com/Revertron/wyrd")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dns_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        var extras = intent.extras
        var dnsList = findViewById<ListView>(R.id.dnsList)
        var adapter = SelectDNSInfoListAdapter(this, arrayListOf(), mutableSetOf())
        dnsList.adapter = adapter
        thread(start = true) {
            try {
                var cd = MainActivity.deserializeStringList2DNSInfoSet(
                    extras!!.getStringArrayList(MainActivity.DNS_LIST)!!
                )
                for (d in cd) {
                    var ping = ping(d.address, 53)
                    d.ping = ping
                }
                for (dns in allDNS) {
                    if (cd.contains(dns)) {
                        continue
                    }
                    var ping = ping(dns.address, 53)
                    dns.ping = ping
                    adapter.addItem(dns)
                    runOnUiThread(
                        Runnable
                        {
                            adapter.sort()
                        }
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.save, menu)
        val item = menu.findItem(R.id.saveItem) as MenuItem
        item.setActionView(R.layout.menu_save)
        val saveButton = item
            .actionView.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val result = Intent(this, MainActivity::class.java)
            var adapter = findViewById<ListView>(R.id.dnsList).adapter as SelectDNSInfoListAdapter
            val selectedDNS = adapter.getSelectedDNS()
            if(selectedDNS.isNotEmpty()) {
                result.putExtra(MainActivity.DNS_LIST, MainActivity.serializeDNSInfoSet2StringList(selectedDNS))
                setResult(Activity.RESULT_OK, result)
                finish()
            } else {
                val text = "Select at least one DNS"
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(applicationContext, text, duration)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        }
        return true
    }
}