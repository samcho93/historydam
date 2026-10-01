package com.samdori93.yeoksadam.core.network.worker

import com.samdori93.yeoksadam.core.network.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 역사담 서버(Worker)로 가는 요청에만 앱 식별 헤더를 붙인다.
 * 웹앱은 Origin 으로, 앱은 X-App-Key 로 확인한다 (Worker 의 APP_KEYS 와 같은 값).
 */
class AppKeyInterceptor : Interceptor {
    private val workerHost = BuildConfig.WORKER_BASE_URL.toHttpUrl().host

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (req.url.host != workerHost) return chain.proceed(req)
        return chain.proceed(req.newBuilder().header("X-App-Key", BuildConfig.APP_KEY).build())
    }
}
