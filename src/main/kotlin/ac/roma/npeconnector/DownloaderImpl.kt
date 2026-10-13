package ac.roma.npeconnector

import ac.mdiq.podcini.shared.PodciniHttpClient.documentKtor
import ac.mdiq.podcini.shared.PodciniHttpClient.getKtorClient
import ac.mdiq.podcini.shared.USER_AGENT
import android.util.Log
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

class DownloaderImpl private constructor() : Downloader() {
    val mCookies: MutableMap<String?, String?> = HashMap()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response = runBlocking(Dispatchers.IO) {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val extraHeaders = request.headers().toMutableMap()
        val dataToSend = request.dataToSend()
        val youtubeCookie = if (url.contains(YOUTUBE_DOMAIN)) mCookies[YOUTUBE_RESTRICTED_MODE_COOKIE_KEY] else null
        val cookies = listOfNotNull(youtubeCookie, mCookies[RECAPTCHA_COOKIES_KEY]).flatMap { it.split(Regex("; *")).filter { it1 -> it1.isNotEmpty() } }.distinct().joinToString("; ")

        val response = documentKtor.request {
            url(url)
            method = HttpMethod.parse(httpMethod)
            headers {
                if (cookies.isNotEmpty()) append(HttpHeaders.Cookie, cookies)
                extraHeaders.forEach { (name, values) ->
                    if (name != null) {
                        remove(name)
                        values.orEmpty().filterNotNull().forEach { value -> append(name, value) }
                    }
                }
            }
            if (dataToSend != null) setBody(dataToSend)
        }
        if (response.status.value == 429) throw ReCaptchaException("reCaptcha Challenge requested", url)
        return@runBlocking Response(response.status.value, response.status.description, response.headers.entries().associate { it.key to it.value }, response.bodyAsText(), response.call.request.url.toString())
    }

    companion object {
        const val YOUTUBE_RESTRICTED_MODE_COOKIE_KEY: String = "youtube_restricted_mode_key"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE: String = "PREF=f2=8000000"
        const val YOUTUBE_DOMAIN: String = "youtube.com"

        const val RECAPTCHA_COOKIES_KEY = "recaptcha_cookies"

        var instance: DownloaderImpl? = null

        /**
         * It's recommended to call exactly once in the entire lifetime of the application.
         *
         * @param builder if null, default builder will be used
         * @return a new instance of [DownloaderImpl]
         */
        fun init(): DownloaderImpl {
            instance = DownloaderImpl()
            return instance!!
        }
    }
}
