package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.common.result.map
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.HeritageRepository
import javax.inject.Inject

/**
 * 카메라 인식용 국가유산 조회 — 역사담 웹앱 카탈로그(국가유산청 + 향토유산 + 역사관광지)를 쓴다.
 * (국가유산청 오픈API 를 매번 부르지 않아 빠르고, 향토유산까지 후보에 든다.)
 */
class HeritageRepositoryImpl @Inject constructor(
    private val catalog: CatalogRepository,
) : HeritageRepository {

    override suspend fun getNearbyHeritage(origin: LatLng, limit: Int): Result<List<RecognizedHeritage>> =
        Result.Success(
            catalog.nearbySites(origin, CANDIDATE_RADIUS_M).take(limit).map { n ->
                RecognizedHeritage(
                    id = n.site.id,
                    name = n.site.name,
                    hanja = "",
                    kind = if (n.site.local) "향토유산" else n.site.designation,
                    era = n.site.era,
                    category = n.site.category,
                    address = n.site.city,
                    description = "",
                    imageUrl = n.site.thumbUrl,
                    lat = n.site.lat,
                    lng = n.site.lng,
                    distanceM = n.distanceM,
                )
            },
        )

    override suspend fun getHeritageDetail(id: String): Result<RecognizedHeritage> =
        catalog.siteDetail(id).map { d ->
            RecognizedHeritage(
                id = d.id,
                name = d.name,
                hanja = d.hanja,
                kind = if (d.local) "향토유산" else d.designation,
                era = d.era,
                category = d.category,
                address = d.address,
                description = d.description.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), " ").trim(),
                imageUrl = d.images.firstOrNull()?.url,
                lat = d.lat,
                lng = d.lng,
                distanceM = 0f,
            )
        }

    private companion object {
        /** 사진 판별 힌트로 쓸 주변 반경 */
        const val CANDIDATE_RADIUS_M = 3_000
    }
}
