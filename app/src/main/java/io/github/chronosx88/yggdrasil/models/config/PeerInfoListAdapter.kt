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
import java.util.ArrayList


class PeerInfoListAdapter(
    context: Context,
    allPeers: List<PeerInfo>,
    currentPeers: ArrayList<String>
) : ArrayAdapter<PeerInfo?> (context, 0, allPeers) {

    private val mContext: Context = context
    private var allPeers: List<PeerInfo> = allPeers
    private var currentPeers: ArrayList<String> = currentPeers

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var listItem: View? = convertView
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.peers_list_item_edit, parent, false)
        }
        val currentPeer = allPeers[position]
        val image: ImageView = listItem?.findViewById(R.id.countryFlag) as ImageView
        image.setImageResource(currentPeer.getCountry(mContext)!!.flagID)
        val name = listItem.findViewById(R.id.peerInfoText) as TextView
        val peerId = currentPeer.toString()
        name.text = peerId
        val checkbox = listItem.findViewById(R.id.checkbox) as CheckBox
        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                if(!currentPeers.contains(peerId)){
                    currentPeers.add(peerId)
                }
            } else {
                if(currentPeers.contains(peerId)){
                    currentPeers.remove(peerId)
                }
            }
        }
        if(this.currentPeers.contains(peerId)){
            checkbox.isChecked = true
        }
        return listItem
    }

    public fun getSelectedPeers(): ArrayList<String> {
        return currentPeers
    }

}