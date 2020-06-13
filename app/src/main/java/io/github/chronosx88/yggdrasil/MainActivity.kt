package io.github.chronosx88.yggdrasil

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG="Yggdrasil";
        private const val VPN_REQUEST_CODE = 0x0F
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val connectRadioGroup = findViewById<RadioGroup>(R.id.connectRadioGroup)
        connectRadioGroup.setOnCheckedChangeListener(
                RadioGroup.OnCheckedChangeListener { group, checkedId ->
                    when (checkedId) {
                        R.id.disconnectButton -> stopVpn()
                        R.id.connectButton -> startVpn()
                        else -> { // Note the block
                            //print("x is neither 1 nor 2")
                        }
                    }
                })
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
            startService(intent)
        }
    }
}
