package io.github.chronosx88.yggdrasil

import android.content.Context
import com.google.gson.Gson
import io.github.chronosx88.yggdrasil.models.config.Config
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.File
import java.lang.Runtime.getRuntime

val gson = Gson()

val Context.yggBin get() = File(filesDir, "yggdrasil-0.3.7")

fun Context.execYgg(cmd: String) = getRuntime().exec(
    "${yggBin.absolutePath} $cmd"
)

@Throws(RuntimeException::class)
fun Context.getYggConfig(): Config {
    val configFile = File(filesDir, "yggdrasil.conf")
    if(!configFile.exists()) {
        throw RuntimeException("Config file don't exist!")
    }
    val configStr = configFile.readText()
    val configHjsonObject = JsonValue.readHjson(configStr)
    return gson.fromJson(configHjsonObject.toString(Stringify.PLAIN), Config::class.java)
}

fun Context.saveYggConfig(config: Config) {
    val configJson = gson.toJson(config)
    val configHjson = JsonValue.readHjson(configJson).toString(Stringify.HJSON)
    val configFile = File(filesDir, "yggdrasil.conf")
    configFile.writeText(configHjson)
}