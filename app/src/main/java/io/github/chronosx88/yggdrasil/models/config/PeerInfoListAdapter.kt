package io.github.chronosx88.yggdrasil.models.config

import android.content.Context
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


class PeerInfoListAdapter(
    context: Context,
    allPeers: List<PeerInfo>
) : ArrayAdapter<PeerInfo?> (context, 0, allPeers) {

    private val mContext: Context = context
    private var allPeers: List<PeerInfo> = allPeers

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var peerInfoHolder = PeerInfoHolder()
        var listItem: View? = convertView
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.peers_list_item, parent, false)
            peerInfoHolder.countryFlag = listItem.findViewById(R.id.countryFlag) as ImageView
            peerInfoHolder.peerInfoText = listItem.findViewById(R.id.peerInfoText) as TextView
            listItem.tag = peerInfoHolder
        } else {
            peerInfoHolder = listItem.tag as PeerInfoHolder
        }
        val currentPeer = allPeers[position]
        peerInfoHolder.countryFlag.setImageResource(currentPeer.getCountry(mContext)!!.flagID)
        val peerId = currentPeer.toString()
        peerInfoHolder.peerInfoText.text = peerId
        return listItem!!
    }

    class PeerInfoHolder {
        lateinit var countryFlag: ImageView
        lateinit var peerInfoText: TextView
    }

}