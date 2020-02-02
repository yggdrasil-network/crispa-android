package io.github.chronosx88.yggdrasil

import android.content.Context
import android.os.Build.CPU_ABI
import android.util.Log
import com.google.gson.Gson
import config.NodeConfig
import io.github.chronosx88.yggdrasil.models.config.Config
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.File
import java.lang.Runtime.getRuntime

val gson = Gson()

fun createNativeYggConfig(config: Config): NodeConfig {
    val nativeConfig = NodeConfig()
    nativeConfig.adminListen = config.adminListen
    nativeConfig.encryptionPrivateKey = config.encryptionPrivateKey
    nativeConfig.encryptionPublicKey = config.encryptionPublicKey
    nativeConfig.ifMTU = config.ifMTU
    nativeConfig.ifName = config.ifName
    nativeConfig.ifTAPMode = config.ifTAPMode
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