package io.github.chronosx88.yggdrasil.models.config

import com.google.gson.annotations.SerializedName

// FIXME This is old config scheme
data class Config (
	@SerializedName("peers") var peers : List<String>,
	@SerializedName("interfacePeers") var interfacePeers : Map<String, List<String>>,
	@SerializedName("listen") var listen : List<String>,
	@SerializedName("adminListen") var adminListen : String,
	@SerializedName("multicastInterfaces") var multicastInterfaces : List<String>,
	@SerializedName("allowedEncryptionPublicKeys") var allowedEncryptionPublicKeys : List<String>,
	@SerializedName("encryptionPublicKey") var encryptionPublicKey : String,
	@SerializedName("encryptionPrivateKey") var encryptionPrivateKey : String,
	@SerializedName("signingPublicKey") var signingPublicKey : String,
	@SerializedName("signingPrivateKey") var signingPrivateKey : String,
	@SerializedName("linkLocalTCPPort") var linkLocalTCPPort : Int,
	@SerializedName("ifName") var ifName : String,
	@SerializedName("ifTAPMode") var ifTAPMode : Boolean,
	@SerializedName("ifMTU") var ifMTU : Long,
	@SerializedName("sessionFirewall") var sessionFirewall : SessionFirewall,
	@SerializedName("tunnelRouting") var tunnelRouting : TunnelRouting,
	@SerializedName("switchOptions") var switchOptions : SwitchOptions,
	@SerializedName("nodeInfoPrivacy") var nodeInfoPrivacy : Boolean,
	@SerializedName("nodeInfo") var nodeInfo : Map<String, Any>
)