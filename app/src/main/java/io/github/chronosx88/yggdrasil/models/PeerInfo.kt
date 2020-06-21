package io.github.chronosx88.yggdrasil.models

import android.content.Context
import com.hbb20.CCPCountry
import com.hbb20.CountryCodePicker
import java.net.InetAddress


class PeerInfo {
    constructor(schema: String, address: InetAddress, port: Int, countryCode: String){
        this.schema = schema
        this.address = address
        this.port = port
        this.countryCode = countryCode
    }
    var schema: String
    var address: InetAddress
    var port = 0
    var countryCode: String
    var ping: Float = Float.MAX_VALUE

    override fun toString(): String {
        var a = address.toString();
        if(a.indexOf("/")>0){
            return this.schema+"://"+a.split("/")[0]+":"+port
        } else {
            if(a.contains(":")) {
                return this.schema + "://[" + a.substring(1) + "]:" + port
            } else {
                return this.schema + ":/" + a + ":" + port
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return toString() == other.toString()
    }

    fun getCountry(context: Context): CCPCountry? {
        return CCPCountry.getCountryForNameCodeFromLibraryMasterList(context, CountryCodePicker.Language.ENGLISH, countryCode)
    }

}