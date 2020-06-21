package io.github.chronosx88.yggdrasil.models.config

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import io.github.chronosx88.yggdrasil.R
import io.github.chronosx88.yggdrasil.models.PeerInfo
import java.util.ArrayList


class SelectPeerInfoListAdapter(
    context: Context,
    allPeers: List<PeerInfo>,
    currentPeers: ArrayList<PeerInfo>
) : ArrayAdapter<PeerInfo?> (context, 0, allPeers) {

    private val mContext: Context = context
    private var allPeers: List<PeerInfo> = allPeers
    private var currentPeers: ArrayList<PeerInfo> = currentPeers

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var peerInfoHolder = PeerInfoHolder()
        var listItem: View? = convertView
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.peers_list_item_edit, parent, false)
            peerInfoHolder.checkbox = listItem.findViewById(R.id.checkbox) as CheckBox
            peerInfoHolder.countryFlag = listItem.findViewById(R.id.countryFlag) as ImageView
            peerInfoHolder.peerInfoText = listItem.findViewById(R.id.peerInfoText) as TextView
            peerInfoHolder.ping = listItem.findViewById(R.id.ping) as TextView
            listItem.tag = peerInfoHolder
        } else {
            peerInfoHolder = listItem.tag as PeerInfoHolder
        }
        val currentPeer = allPeers[position]
        peerInfoHolder.countryFlag.setImageResource(currentPeer.getCountry(mContext)!!.flagID)
        val peerId = currentPeer.toString()
        if(currentPeer.ping == Int.MAX_VALUE){
            peerInfoHolder.peerInfoText.text = peerId
            peerInfoHolder.ping.text=""
            peerInfoHolder.peerInfoText.setTextColor(Color.GRAY)
        } else {
            peerInfoHolder.peerInfoText.text = peerId
            peerInfoHolder.ping.text = currentPeer.ping.toString() + " ms"
            peerInfoHolder.peerInfoText.setTextColor(Color.WHITE)
        }
        peerInfoHolder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                if(!currentPeers.contains(currentPeer)){
                    currentPeers.add(currentPeer)
                }
            } else {
                if(currentPeers.contains(currentPeer)){
                    currentPeers.remove(currentPeer)
                }
            }
        }
        peerInfoHolder.checkbox.isChecked = this.currentPeers.contains(currentPeer)
        return listItem!!
    }

    fun getSelectedPeers(): ArrayList<PeerInfo> {
        return currentPeers
    }

    class PeerInfoHolder {
        lateinit var checkbox: CheckBox
        lateinit var countryFlag: ImageView
        lateinit var peerInfoText: TextView
        lateinit var ping: TextView
    }

}