package io.github.chronosx88.yggdrasil

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var isYggStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connect_button)
        connectButton.setOnClickListener {
            if(!isYggStarted) {
                VpnService.prepare(this)
                val intent = Intent(this, YggdrasilTunService::class.java)
                startService(intent)
                connectButton.text = "Disconnect"
                isYggStarted = true
            } else {
                val intent = Intent(this, YggdrasilTunService::class.java)
                stopService(intent)
                connectButton.text = "Connect"
                isYggStarted = false
            }
        }
    }
}
