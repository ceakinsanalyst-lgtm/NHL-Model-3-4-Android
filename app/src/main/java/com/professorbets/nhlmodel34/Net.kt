package com.professorbets.nhlmodel34

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object Net {
    fun get(url: String, headers: Map<String,String> = emptyMap()): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "NHLModel34Android/2.0")
        headers.forEach { (k,v) -> c.setRequestProperty(k,v) }
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
