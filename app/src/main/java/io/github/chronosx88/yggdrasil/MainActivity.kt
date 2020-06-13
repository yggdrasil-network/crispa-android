package io.github.chronosx88.yggdrasil

import android.R.attr
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    companion object {
        const val PARAM_PINTENT = "pendingIntent"
        const val STATUS_START = 1
        const val STATUS_FINISH = 0
        const val IPv6: String = "IPv6"
        private const val TAG="Yggdrasil"
        private const val VPN_REQUEST_CODE = 0x0F
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val connectRadioGroup = findViewById<RadioGroup>(R.id.connectRadioGroup)
        connectRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.disconnectButton -> stopVpn()
                R.id.connectButton -> startVpn()
                else -> { // Note the block

                }
            }
        }
    }

    fun stopVpn(){
        Log.d(TAG,"Stop")
        val intent = Intent(this, YggdrasilTunService::class.java)
        intent.putExtra("COMMAND", "STOP")
        startService(intent)
    }

    fun startVpn(){
        Log.d(TAG,"Start")
        val intent= VpnService.prepare(this)
        if (intent!=null){
            startActivityForResult(intent, VPN_REQUEST_CODE);
        }else{
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode== Activity.RESULT_OK){
            val intent = Intent(this, YggdrasilTunService::class.java)
            val TASK_CODE = 100
            var  pi = createPendingResult(TASK_CODE, intent, 0);
            intent.putExtra("COMMAND", "START")
            intent.putExtra(PARAM_PINTENT, pi)
            startService(intent)
        }
        when (resultCode) {
            STATUS_START -> print("service started")
            STATUS_FINISH -> {
                val result: String = data!!.getStringExtra(IPv6)
                findViewById<TextView>(R.id.ip).setText(result)
            }
            else -> { // Note the block

            }
        }
    }
}
