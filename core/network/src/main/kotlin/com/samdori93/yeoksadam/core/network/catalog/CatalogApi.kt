package com.samdori93.yeoksadam.core.network.catalog

import retrofit2.http.GET
import retrofit2.http.Path

/** 역사담 웹앱 정적 데이터 (BuildConfig.CATALOG_BASE_URL). */
interface CatalogApi {
    @GET("data/index.json")
    suspend fun index(): IndexDto

    @GET("data/figures.json")
    suspend fun figures(): FiguresDto

    @GET("data/detail/{id}.json")
    suspend fun detail(@Path("id") id: String): DetailDto
}
