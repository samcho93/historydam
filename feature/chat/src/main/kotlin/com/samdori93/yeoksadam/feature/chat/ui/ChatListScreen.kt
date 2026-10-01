package com.samdori93.yeoksadam.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.usecase.GetAllFiguresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Q&A — 대화할 인물 목록 (가까운 순). */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    getAllFigures: GetAllFiguresUseCase,
) : ViewModel() {
    private val _figures = MutableStateFlow<List<NearbyFigure>?>(null)
    val figures: StateFlow<List<NearbyFigure>?> = _figures.asStateFlow()

    init {
        viewModelScope.launch { _figures.value = getAllFigures().sortedBy { it.distanceM } }
    }
}

@Composable
fun ChatListRoute(
    onFigureClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val figures by viewModel.figures.collectAsStateWithLifecycle()
    ChatListScreen(figures = figures, onFigureClick = onFigureClick, modifier = modifier)
}

@Composable
fun ChatListScreen(
    figures: List<NearbyFigure>?,
    onFigureClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding(),
    ) {
        Text(
            "역사 인물 Q&A",
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            fontFamily = NanumMyeongjo,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = DancheongColors.Meok,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "대화할 인물을 고르세요. 가까운 인물부터 보여 드립니다.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = DancheongColors.MeokSoft,
        )
        if (figures == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DancheongColors.Jujak)
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(figures, key = { it.figure.id }) { nf ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFigureClick(nf.figure.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MedallionPortrait(
                        portraitUrl = nf.figure.portraitUrl.ifBlank { null },
                        name = nf.figure.name,
                        sealMark = nf.figure.seal.ifBlank { null },
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nf.figure.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
                        Text("${nf.figure.title} · ${nf.site.name}", fontSize = 12.sp, color = DancheongColors.MeokSoft, maxLines = 1)
                    }
                    Text(
                        if (nf.distanceM >= 1000) "%.0fkm".format(nf.distanceM / 1000) else "${nf.distanceM.toInt()}m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DancheongColors.CheongnokDeep,
                    )
                }
            }
        }
    }
}
