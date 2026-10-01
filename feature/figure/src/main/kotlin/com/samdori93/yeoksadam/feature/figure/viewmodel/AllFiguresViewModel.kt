package com.samdori93.yeoksadam.feature.figure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.domain.model.DiscoveryType
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.usecase.GetAllFiguresUseCase
import com.samdori93.yeoksadam.core.domain.usecase.ObserveDiscoveriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AllFiguresUiState(
    val figures: List<NearbyFigure> = emptyList(),
    val discovered: Set<String> = emptySet(),
    val loading: Boolean = true,
)

/** 모든 인물 — 인물별 가장 가까운 관련 유적과 거리, 만난 인물 여부(역사의 전당). */
@HiltViewModel
class AllFiguresViewModel @Inject constructor(
    getAllFigures: GetAllFiguresUseCase,
    observeDiscoveries: ObserveDiscoveriesUseCase,
) : ViewModel() {

    private val figures = MutableStateFlow<List<NearbyFigure>?>(null)

    val uiState: StateFlow<AllFiguresUiState> = combine(figures, observeDiscoveries()) { list, found ->
        AllFiguresUiState(
            figures = list.orEmpty(),
            discovered = found.filter { it.type == DiscoveryType.FIGURE }.map { it.refId }.toSet(),
            loading = list == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllFiguresUiState())

    init {
        viewModelScope.launch { figures.value = getAllFigures() }
    }
}
