package io.github.chronosx88.yggdrasil.models.config

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class Utils {

    companion object {

        @JvmStatic
        fun ping(address: InetAddress, port:Int): Int {
            val start = System.currentTimeMillis()
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(address, port), 5000)
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                print(address)
                return Int.MAX_VALUE
            }
            return (System.currentTimeMillis() - start).toInt()
        }

    }
}