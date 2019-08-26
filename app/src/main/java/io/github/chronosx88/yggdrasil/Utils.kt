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

fun Context.execYgg(cmd: String) = getRuntime().exec(
    "${yggBin.absolutePath} $cmd"
)

val Context.yggBin get() = File(filesDir, "yggdrasil-$YGGDRASIL_VERSION-linux-${CPU_ABI.let {
        when {
            it.contains("v7") -> "armhf"
            it.contains("v8") -> "arm64"
            else ->  throw Exception("Unsupported ABI")
        } 
    }
}")

@Throws(RuntimeException::class)
fun Context.getYggConfig(): Config {
    val configFile = File(filesDir, "yggdrasil.conf")
    if(!configFile.exists()) {
        generateYggConfig()
    }
    val configStr = configFile.readText()
    val configHjsonObject = JsonValue.readHjson(configStr)
    return gson.fromJson(configHjsonObject.toString(Stringify.PLAIN), Config::class.java)
}

fun Context.generateYggConfig() {
    val process = execYgg("-genconf > ${filesDir.absolutePath}/yggdrasil.conf")
    process.waitFor()
    val configBytes = process.inputStream.readBytes()
    val configStr = String(configBytes)
    val configFile = File(filesDir, "yggdrasil.conf")
    configFile.writeText(configStr)
}

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

fun Context.installBinary() {
    val type = "yggdrasil-$YGGDRASIL_VERSION-linux-${CPU_ABI.let {
        when{
            it.contains("v8") -> "arm64"
            it.contains("v7") -> "armhf"
            else -> throw Exception("Unsupported ABI")
        }
    }}"

    yggBin.apply {
        delete()
        createNewFile()
    }

    val input = assets.open(type)
    val output = yggBin.outputStream()

    try {
        input.copyTo(output)
    } finally {
        input.close(); output.close()
    }

    yggBin.setExecutable(true)

    val yggConfig = getYggConfig() // it generates config automatically
    yggConfig.ifName = "tun0"
    saveYggConfig(yggConfig)

    Log.i("Utils", "# Binary installed successfully")
}