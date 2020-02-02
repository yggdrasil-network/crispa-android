package io.github.chronosx88.yggdrasil

import android.app.Activity
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
                // Prepare to establish a VPN connection.
                // This method returns null if the VPN application is already prepared
                // or if the user has previously consented to the VPN application.
                // Otherwise, it returns an Intent to a system activity.
                val vpnIntent = VpnService.prepare(this)
                if (vpnIntent != null) startActivityForResult(
                    vpnIntent,
                    0x0F
                )
                else onActivityResult(0x0F, Activity.RESULT_OK, null)
            } else {
                // FIXME fix this shit, this code doesn't stop service for some reasons
                val intent = Intent(this, YggdrasilTunService::class.java)
                stopService(intent)
                connectButton.text = "Connect"
                isYggStarted = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x0F && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, YggdrasilTunService::class.java)
            startService(intent)
            val connectButton = findViewById<Button>(R.id.connect_button)
            connectButton.text = "Disconnect"
            isYggStarted = true
        }
    }
}
