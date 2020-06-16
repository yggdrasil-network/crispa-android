package io.github.chronosx88.yggdrasil

import android.content.Context
import com.google.gson.Gson
import config.NodeConfig
import io.github.chronosx88.yggdrasil.models.config.Config
import java.io.File

val gson = Gson()

fun createNativeYggConfig(config: Config): NodeConfig {
    val nativeConfig = NodeConfig()
    nativeConfig.adminListen = config.adminListen
    nativeConfig.encryptionPrivateKey = config.encryptionPrivateKey
    nativeConfig.encryptionPublicKey = config.encryptionPublicKey
    //nativeConfig.ifMTU = config.ifMTU
    nativeConfig.ifName = config.ifName
    //nativeConfig.ifTAPMode = config.ifTAPMode
    nativeConfig.nodeInfoPrivacy = config.nodeInfoPrivacy
    nativeConfig.signingPrivateKey = config.signingPrivateKey
    nativeConfig.signingPublicKey = config.signingPublicKey
    return nativeConfig
}

fun Context.saveYggConfig(config: Config) {
    val configJson = gson.toJson(config)
    val configFile = File(filesDir, "yggdrasil.conf")
    configFile.writeText(configJson)
}