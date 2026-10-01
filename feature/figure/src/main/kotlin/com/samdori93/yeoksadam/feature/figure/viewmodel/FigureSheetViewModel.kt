package com.samdori93.yeoksadam.feature.figure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.model.DiscoveryType
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.usecase.GetFigureWithSiteUseCase
import com.samdori93.yeoksadam.core.domain.usecase.RecordDiscoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 인물 시트 — 인물과 가장 가까운 관련 유적. 시트를 열면(= 인물을 만나면) 역사의 전당 도감에 기록한다. */
@HiltViewModel
class FigureSheetViewModel @Inject constructor(
    private val getFigure: GetFigureWithSiteUseCase,
    private val recordDiscovery: RecordDiscoveryUseCase,
) : ViewModel() {

    private val _figure = MutableStateFlow<NearbyFigure?>(null)
    val figure: StateFlow<NearbyFigure?> = _figure.asStateFlow()

    fun onFigureShown(figureId: String) {
        viewModelScope.launch {
            val nf = getFigure(figureId) ?: return@launch
            _figure.value = nf
            val f = nf.figure
            recordDiscovery(
                Discovery(
                    type = DiscoveryType.FIGURE,
                    refId = f.id,
                    name = f.name,
                    subtitle = "${f.title} · ${f.years}",
                    description = "${f.bio} — ${nf.site.name}에서 만난 역사 인물입니다.",
                    imageUrl = f.portraitUrl.ifBlank { null },
                    discoveredAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
