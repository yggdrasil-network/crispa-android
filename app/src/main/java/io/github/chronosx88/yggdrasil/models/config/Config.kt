package io.github.chronosx88.yggdrasil.models.config

import com.google.gson.annotations.SerializedName

data class Config (
	@SerializedName("peers") val peers : List<String>,
	@SerializedName("interfacePeers") val interfacePeers : Map<String, List<String>>,
	@SerializedName("listen") val listen : List<String>,
	@SerializedName("adminListen") val adminListen : String,
	@SerializedName("multicastInterfaces") val multicastInterfaces : List<String>,
	@SerializedName("allowedEncryptionPublicKeys") val allowedEncryptionPublicKeys : List<String>,
	@SerializedName("encryptionPublicKey") val encryptionPublicKey : String,
	@SerializedName("encryptionPrivateKey") val encryptionPrivateKey : String,
	@SerializedName("signingPublicKey") val signingPublicKey : String,
	@SerializedName("signingPrivateKey") val signingPrivateKey : String,
	@SerializedName("linkLocalTCPPort") val linkLocalTCPPort : Int,
	@SerializedName("ifName") val ifName : String,
	@SerializedName("ifTAPMode") val ifTAPMode : Boolean,
	@SerializedName("ifMTU") val ifMTU : Long,
	@SerializedName("sessionFirewall") val sessionFirewall : SessionFirewall,
	@SerializedName("tunnelRouting") val tunnelRouting : TunnelRouting,
	@SerializedName("switchOptions") val switchOptions : SwitchOptions,
	@SerializedName("nodeInfoPrivacy") val nodeInfoPrivacy : Boolean,
	@SerializedName("nodeInfo") val nodeInfo : Map<String, Any>
)