package io.github.chronosx88.yggdrasil.models.config

import android.content.Context
import com.hbb20.CCPCountry
import com.hbb20.CountryCodePicker
import java.net.InetAddress


class PeerInfo {
    constructor(schema: String, address: InetAddress, port: Int, countryCode: String, language: CountryCodePicker.Language){
        this.schema = schema
        this.address = address
        this.port = port
        this.countryCode = countryCode
        this.language = language
    }
    var schema: String
    var address: InetAddress
    var port = 0
    var countryCode: String
    var language: CountryCodePicker.Language
    var ping: Float = Float.MAX_VALUE

    override fun toString(): String {
        return this.schema+":/"+address.toString()+":"+port
    }

    fun getCountry(context: Context): CCPCountry? {
        return CCPCountry.getCountryForNameCodeFromLibraryMasterList(context, language, countryCode)
    }

}