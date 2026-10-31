package com.nevercrynow.ioshub.data

import java.net.HttpURLConnection
import java.net.URL

/** 极简 HTTP：只用 HttpURLConnection，不引入 OkHttp / Retrofit。 */
object Http {

    fun get(url: String, accept: String? = null, timeoutMs: Int = 15000): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", "iOSHUB-Android/1.0")
            if (accept != null) {
                connection.setRequestProperty("Accept", accept)
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val message = when (code) {
                    403, 429 -> "请求过于频繁（$code），稍后再试。"
                    404 -> "内容不存在（404）。"
                    else -> "服务器返回 $code"
                }
                throw IllegalStateException(message)
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
