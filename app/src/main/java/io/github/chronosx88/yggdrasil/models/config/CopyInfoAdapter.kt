package io.github.chronosx88.yggdrasil.models.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.github.chronosx88.yggdrasil.R
import io.github.chronosx88.yggdrasil.models.NodeInfo

class CopyInfoAdapter(
    context: Context,
    nodeInfoList: List<NodeInfo>,
) : ArrayAdapter<NodeInfo?> (context, 0, nodeInfoList) {

    private val mContext: Context = context
    private var nodeInfoList: MutableList<NodeInfo> = nodeInfoList as MutableList<NodeInfo>

    override fun getItem(position: Int): NodeInfo? {
        return nodeInfoList[position]
    }

    override fun getCount(): Int {
        return nodeInfoList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var copyNodeInfoHolder = CopyInfoHolder()
        var listItem: View? = convertView
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.copy_node_info_list_item, parent, false)
            copyNodeInfoHolder.copyButton = listItem.findViewById(R.id.nodeInfoButton) as Button
            copyNodeInfoHolder.nodeInfoText = listItem.findViewById(R.id.nodeInfoText) as TextView
            copyNodeInfoHolder.nodeInfoKey = listItem.findViewById(R.id.nodeInfoKey) as TextView
            listItem.tag = copyNodeInfoHolder
        } else {
            copyNodeInfoHolder = listItem.tag as CopyInfoHolder
        }
        copyNodeInfoHolder.nodeInfoKey.text = nodeInfoList[position].key
        copyNodeInfoHolder.nodeInfoText.text = nodeInfoList[position].value
        copyNodeInfoHolder.copyButton.setOnClickListener{ _ ->
            val clipboard: ClipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText(nodeInfoList[position].key, nodeInfoList[position].value)
            clipboard.setPrimaryClip(clip)
            showToast(nodeInfoList[position].key + " " + context.getString(R.string.node_info_copied))
        }
        return listItem!!
    }

    private fun showToast(text: String){
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, text, duration)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    class CopyInfoHolder {
        lateinit var nodeInfoKey: TextView
        lateinit var nodeInfoText: TextView
        lateinit var copyButton: Button
    }
}