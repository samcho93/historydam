package com.samdori93.yeoksadam.feature.figure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.LockedMedallion
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.feature.figure.viewmodel.AllFiguresUiState

/** 모든 인물 (목업 7번) — 필터·검색·세그먼트·가나다 인덱스 리스트. */
@Composable
fun AllFiguresScreen(
    state: AllFiguresUiState,
    onBack: () -> Unit,
    onFigureClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var byDistance by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = state.figures
        .filter { query.isBlank() || it.figure.name.contains(query.trim()) || it.site.name.contains(query.trim()) }
        .let { list -> if (byDistance) list.sortedBy { it.distanceM } else list.sortedBy { it.figure.name } }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding(),
    ) {
        // 상단바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", onBack)
            Text(
                "모든 인물",
                modifier = Modifier.weight(1f),
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DancheongColors.Meok,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.width(38.dp))
        }

        // 필터 + 검색
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .background(DancheongColors.Jujak, RoundedCornerShape(19.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.FilterList, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("필터", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(DancheongColors.HanjiCard, RoundedCornerShape(19.dp))
                    .border(1.dp, DancheongColors.Meok.copy(alpha = 0.10f), RoundedCornerShape(19.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = DancheongColors.MeokSoft, modifier = Modifier.size(15.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("인물·유적 검색", color = DancheongColors.MeokSoft, fontSize = 13.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = DancheongColors.Meok),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 세그먼트
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(DancheongColors.HanjiDim, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Segment("가나다순", !byDistance) { byDistance = false }
            Segment("주변 인물", byDistance) { byDistance = true }
        }

        Spacer(Modifier.height(8.dp))

        // 리스트
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            items(shown, key = { it.figure.id }) { nf ->
                FigureRow(nf, discovered = nf.figure.id in state.discovered, onClick = { onFigureClick(nf.figure.id) })
            }
        }
    }
}

@Composable
private fun Segment(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (on) DancheongColors.HanjiCard else Color.Transparent, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (on) DancheongColors.Jujak else DancheongColors.MeokSoft,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FigureRow(nf: NearbyFigure, discovered: Boolean, onClick: () -> Unit) {
    val fig = nf.figure
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (discovered) {
            MedallionPortrait(portraitUrl = fig.portraitUrl.ifBlank { null }, name = fig.name, sealMark = fig.seal.ifBlank { null }, size = 52.dp)
        } else {
            LockedMedallion(size = 52.dp, label = fig.name.take(1))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(fig.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
            Text("${fig.title} · ${nf.site.name}", fontSize = 12.sp, color = DancheongColors.MeokSoft, maxLines = 1)
        }
        Text(
            if (discovered) formatDistance(nf.distanceM) else "미발견 · ${formatDistance(nf.distanceM)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (discovered) DancheongColors.CheongnokDeep else DancheongColors.MeokSoft.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = cd, tint = DancheongColors.Meok, modifier = Modifier.size(20.dp))
    }
}

@Preview(heightDp = 760)
@Composable
private fun AllFiguresPreview() {
    YeoksadamTheme {
        AllFiguresScreen(AllFiguresUiState(loading = false), {}, {})
    }
}
