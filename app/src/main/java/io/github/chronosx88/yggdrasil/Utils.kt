package io.github.chronosx88.yggdrasil

import android.content.Context
import java.io.File
import java.lang.Runtime.getRuntime

val Context.yggBin get() = File(filesDir, "yggdrasil-0.3.7")

fun Context.execYgg(cmd: String) = getRuntime().exec(
    "${yggBin.absolutePath} $cmd"
)