package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.repository.FigureRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 현재 위치를 구독하면서 주변 인물을 거리순으로 방출한다.
 * 홈 화면(내 주변 인물)의 핵심 비즈니스 로직(CLAUDE.md §6 #1).
 */
class GetNearbyFiguresUseCase @Inject constructor(
    private val figureRepository: FigureRepository,
    private val locationRepository: LocationRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(radiusM: Int = DEFAULT_RADIUS_M): Flow<List<NearbyFigure>> =
        locationRepository.observeLocation().flatMapLatest { origin ->
            figureRepository.observeNearbyFigures(origin, radiusM).map { list ->
                list.sortedBy { it.distanceM }
            }
        }

    companion object {
        /** 인물은 16명이 경기도 곳곳에 있어, 거리와 관계없이 가까운 순으로 모두 보여 준다(웹앱과 같음). */
        const val DEFAULT_RADIUS_M = 300_000
    }
}
