package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.domain.model.SiteDetail

/**
 * 유적·인물 카탈로그 — 역사담 웹앱(arHeritage)이 GitHub Pages 에 게시하는 데이터를 그대로 쓴다.
 *   data/index.json (유적 1,486곳) · data/detail/<id>.json · data/figures.json
 * 처음 한 번 내려받아 메모리·디스크에 캐시한다.
 */
interface CatalogRepository {
    suspend fun figures(): Result<List<Figure>>

    suspend fun sites(): Result<List<HeritageSite>>

    suspend fun figure(id: String): Figure?

    suspend fun site(id: String): HeritageSite?

    suspend fun siteDetail(id: String): Result<SiteDetail>

    /** 반경 안 유적 (가까운 순). */
    suspend fun nearbySites(origin: LatLng, radiusM: Int): List<NearbySite>

    /** 인물의 관련 유적 중 가장 가까운 곳. */
    suspend fun nearestSiteOf(figure: Figure, origin: LatLng): NearbyFigure?
}
