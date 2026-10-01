package com.samdori93.yeoksadam.feature.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import com.samdori93.yeoksadam.core.domain.usecase.GetNearbyFiguresUseCase
import com.samdori93.yeoksadam.core.domain.usecase.GetNearbySitesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 지도에서 고른 것 — 인물 핀 또는 유적 핀 */
sealed interface MapSelection {
    data class OfFigure(val nearby: NearbyFigure) : MapSelection
    data class OfSite(val nearby: NearbySite) : MapSelection
}

data class MapUiState(
    val here: LatLng? = null,
    /** 표시 반경(m) — AR 화면과 같은 설정 */
    val radiusM: Int = 10_000,
    val figures: List<NearbyFigure> = emptyList(),
    /** 반경 안 일반 유적 (인물 유적 제외, 가까운 순 최대 [SITE_PIN_MAX]) */
    val sites: List<NearbySite> = emptyList(),
    val selection: MapSelection? = null,
    /** 반경을 바꾸면 증가 — 화면이 원에 맞춰 지도를 옮긴다 */
    val fitRequest: Int = 0,
)

const val SITE_PIN_MAX = 150

/** 지도 — 웹앱과 같은 동작: 인물 핑 · 반경 원 · 반경 안 유적 핀 · 고른 핀 카드. */
@HiltViewModel
class MapViewModel @Inject constructor(
    location: LocationRepository,
    private val settings: SettingsRepository,
    getNearbyFigures: GetNearbyFiguresUseCase,
    private val getNearbySites: GetNearbySitesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getNearbyFigures().collect { list ->
                _uiState.update { s -> s.copy(figures = list, selection = s.selection ?: list.firstOrNull()?.let { MapSelection.OfFigure(it) }) }
            }
        }
        viewModelScope.launch {
            combine(location.observeLocation(), settings.settings.map { it.radiusM }.distinctUntilChanged()) { here, r -> here to r }
                .collect { (here, r) ->
                    val figureSites = _uiState.value.figures.flatMap { it.figure.relatedSiteIds }.toSet()
                    val sites = getNearbySites(here, r).filter { it.site.id !in figureSites }.take(SITE_PIN_MAX)
                    _uiState.update { it.copy(here = here, radiusM = r, sites = sites) }
                }
        }
    }

    fun selectRadius(radiusM: Int) {
        viewModelScope.launch {
            settings.update { it.copy(radiusM = radiusM) }
            _uiState.update { it.copy(fitRequest = it.fitRequest + 1) }
        }
    }

    fun select(selection: MapSelection) {
        _uiState.update { it.copy(selection = selection) }
    }
}
