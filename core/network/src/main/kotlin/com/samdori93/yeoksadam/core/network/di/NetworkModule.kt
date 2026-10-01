package com.samdori93.yeoksadam.core.network.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.samdori93.yeoksadam.core.network.BuildConfig
import com.samdori93.yeoksadam.core.network.api.YeoksadamApi
import com.samdori93.yeoksadam.core.network.catalog.CatalogApi
import com.samdori93.yeoksadam.core.network.worker.AppKeyInterceptor
import com.samdori93.yeoksadam.core.network.worker.WorkerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // 유적 데이터(JSON)·초상을 디스크에 캐시해 다시 열 때 빠르고, 통신이 끊겨도 마지막 자료로 동작
        .cache(Cache(File(context.cacheDir, "http"), 50L * 1024 * 1024))
        .addInterceptor(AppKeyInterceptor())
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // 음성(MP3)·사진(base64) 본문은 로그가 너무 커서 헤더만
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            },
        )
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideYeoksadamApi(client: OkHttpClient, json: Json): YeoksadamApi =
        retrofit(BuildConfig.API_BASE_URL, client, json).create(YeoksadamApi::class.java)

    @Provides
    @Singleton
    fun provideCatalogApi(client: OkHttpClient, json: Json): CatalogApi =
        retrofit(BuildConfig.CATALOG_BASE_URL, client, json).create(CatalogApi::class.java)

    @Provides
    @Singleton
    fun provideWorkerApi(client: OkHttpClient, json: Json): WorkerApi =
        retrofit(BuildConfig.WORKER_BASE_URL, client, json).create(WorkerApi::class.java)

    /** 상대 경로(figures/xxx.jpg) → 웹앱 주소 기준 절대 URL */
    fun assetUrl(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> BuildConfig.CATALOG_BASE_URL + path.trimStart('/')
    }
}
