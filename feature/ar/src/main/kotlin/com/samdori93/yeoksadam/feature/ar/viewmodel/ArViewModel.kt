package com.samdori93.yeoksadam.feature.ar.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.domain.model.SiteDetail
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import com.samdori93.yeoksadam.core.domain.usecase.GetNearbySitesUseCase
import com.samdori93.yeoksadam.feature.ar.navigation.ArSearch
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

data class ArUiState(
    val here: LatLng? = null,
    val radiusM: Int = 10_000,
    /** 반경 안 유적 (라벨 후보) */
    val sites: List<NearbySite> = emptyList(),
    /** 역사 인물과 연결된 유적 — 겹칠 때 대표로 우선 */
    val important: Set<String> = emptySet(),
    /** 찾아가는 유적 (방향 화살표·남은 거리) */
    val target: HeritageSite? = null,
    /** 타깃 유적에서 만날 인물 */
    val targetFigure: Figure? = null,
    /** 라벨을 눌러 연 유적 카드 */
    val opened: NearbySite? = null,
    val openedFigures: List<Figure> = emptyList(),
    val openedDetail: SiteDetail? = null,
    /** 겹친 유적 묶음 (+N 을 눌렀을 때) */
    val group: List<NearbySite> = emptyList(),
)

/** 위치 기반 AR — 카메라에 비친 방향의 유적을 라벨로 (웹앱 AR 화면과 같은 규칙). */
@HiltViewModel
class ArViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    location: LocationRepository,
    private val settings: SettingsRepository,
    private val catalog: CatalogRepository,
    private val getNearbySites: GetNearbySitesUseCase,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArSearch>()
    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()
    private var figures: List<Figure> = emptyList()

    init {
        viewModelScope.launch {
            figures = catalog.figures().getOrNull().orEmpty()
            val important = figures.flatMap { it.relatedSiteIds }.toSet()
            val target = args.siteId.takeIf { it.isNotBlank() }?.let { catalog.site(it) }
            val targetFigure = args.figureId.takeIf { it.isNotBlank() }?.let { id -> figures.firstOrNull { it.id == id } }
            _uiState.update { it.copy(important = important, target = target, targetFigure = targetFigure) }
        }
        viewModelScope.launch {
            combine(location.observeLocation(), settings.settings.map { it.radiusM }.distinctUntilChanged()) { here, r -> here to r }
                .collect { (here, r) ->
                    _uiState.update { it.copy(here = here, radiusM = r, sites = getNearbySites(here, r)) }
                }
        }
    }

    fun selectRadius(radiusM: Int) {
        viewModelScope.launch { settings.update { it.copy(radiusM = radiusM) } }
    }

    fun open(site: NearbySite) {
        _uiState.update { it.copy(opened = site, openedFigures = figuresOf(site.site.id), openedDetail = null, group = emptyList()) }
        viewModelScope.launch {
            val detail = catalog.siteDetail(site.site.id).getOrNull()
            _uiState.update { if (it.opened?.site?.id == site.site.id) it.copy(openedDetail = detail) else it }
        }
    }

    fun openGroup(sites: List<NearbySite>) {
        _uiState.update { it.copy(group = sites, opened = null) }
    }

    fun close() {
        _uiState.update { it.copy(opened = null, group = emptyList()) }
    }

    /** 여기로 안내 — 방향 화살표와 남은 거리 */
    fun guideTo(site: HeritageSite) {
        _uiState.update { it.copy(target = site, targetFigure = figuresOf(site.id).firstOrNull(), opened = null) }
    }

    fun clearTarget() {
        _uiState.update { it.copy(target = null, targetFigure = null) }
    }

    private fun figuresOf(siteId: String) = figures.filter { siteId in it.relatedSiteIds }
}
