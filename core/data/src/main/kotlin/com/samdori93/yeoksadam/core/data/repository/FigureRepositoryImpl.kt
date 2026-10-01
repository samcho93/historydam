package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.FigureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 인물 — 역사담 웹앱의 figures.json (16명). 거리는 인물의 관련 유적 중 가장 가까운 곳 기준(웹앱과 같음).
 */
class FigureRepositoryImpl @Inject constructor(
    private val catalog: CatalogRepository,
) : FigureRepository {

    override fun observeNearbyFigures(origin: LatLng, radiusM: Int): Flow<List<NearbyFigure>> = flow {
        val figures = when (val r = catalog.figures()) {
            is Result.Success -> r.data
            is Result.Failure -> throw IllegalStateException("인물 자료를 불러오지 못했습니다. 네트워크를 확인해 주세요.")
        }
        emit(figures.mapNotNull { catalog.nearestSiteOf(it, origin) }.filter { it.distanceM <= radiusM })
    }

    override fun observeFigures(): Flow<List<Figure>> = flow {
        emit(catalog.figures().getOrNull().orEmpty().sortedBy { it.name })
    }

    override suspend fun getFigure(id: String): Result<Figure> =
        catalog.figure(id)?.let { Result.Success(it) }
            ?: Result.Failure(AppError.Server(code = 404, message = "인물을 찾을 수 없습니다: $id"))
}
