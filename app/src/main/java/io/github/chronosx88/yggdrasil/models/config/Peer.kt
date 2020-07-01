package io.github.chronosx88.yggdrasil.models.config

import com.google.gson.annotations.SerializedName

data class Peer (
    //Example [{"PublicKey":[154,201,118,156,19,74,134,115,94,159,76,86,36,192,221,105,220,254,226,161,108,226,17,192,75,243,225,15,42,195,155,2],"Endpoint":"(self)","BytesSent":0,"BytesRecvd":0,"Protocol":"self","Port":0,"Uptime":209900460}]
    @SerializedName("Endpoint") var endpoint : String,
    @SerializedName("Port") var port : Int,
    @SerializedName("Uptime") var uptime : Long,
    @SerializedName("Protocol") var protocol : String,
    @SerializedName("BytesSent") var bytesSent : Long,
    @SerializedName("BytesRecvd") var bytesReceived : Long
)