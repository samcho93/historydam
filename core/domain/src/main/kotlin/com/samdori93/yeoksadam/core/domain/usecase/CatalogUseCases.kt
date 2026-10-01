package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.domain.model.SiteDetail
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 인물 한 명 + 지금 위치에서 가장 가까운 관련 유적 (인물 시트·대화 화면). */
class GetFigureWithSiteUseCase @Inject constructor(
    private val catalog: CatalogRepository,
    private val location: LocationRepository,
) {
    suspend operator fun invoke(figureId: String): NearbyFigure? {
        val here = location.observeLocation().first()
        // 역사 인물이 없는 유적은 해설사가 안내한다 ("guide:<유적 id>", 서버도 같은 규칙)
        if (figureId.startsWith(GUIDE_PREFIX)) {
            val site = catalog.site(figureId.removePrefix(GUIDE_PREFIX)) ?: return null
            val guide = Figure(
                id = figureId,
                name = "해설사",
                title = "${site.name} 문화유산 해설사",
                portraitUrl = "",
                relatedSiteIds = listOf(site.id),
                style = "guide",
                seal = "解",
                fullBody = "guide",
            )
            return catalog.nearestSiteOf(guide, here)
        }
        val figure = catalog.figure(figureId) ?: return null
        return catalog.nearestSiteOf(figure, here)
    }

    companion object {
        const val GUIDE_PREFIX = "guide:"
    }
}

/** 모든 인물 + 각자의 가장 가까운 관련 유적 (모든 인물·Q&A 목록). */
class GetAllFiguresUseCase @Inject constructor(
    private val catalog: CatalogRepository,
    private val location: LocationRepository,
) {
    suspend operator fun invoke(): List<NearbyFigure> {
        val here = location.observeLocation().first()
        val figures = (catalog.figures() as? Result.Success)?.data.orEmpty()
        return figures.mapNotNull { catalog.nearestSiteOf(it, here) }
    }
}

/** 반경 안 유적 (지도 핀 · AR 라벨). */
class GetNearbySitesUseCase @Inject constructor(
    private val catalog: CatalogRepository,
) {
    suspend operator fun invoke(origin: com.samdori93.yeoksadam.core.domain.model.LatLng, radiusM: Int): List<NearbySite> =
        catalog.nearbySites(origin, radiusM)
}

/** 유적 상세 (설명·사진). */
class GetSiteDetailUseCase @Inject constructor(
    private val catalog: CatalogRepository,
) {
    suspend operator fun invoke(siteId: String): Result<SiteDetail> = catalog.siteDetail(siteId)
}
