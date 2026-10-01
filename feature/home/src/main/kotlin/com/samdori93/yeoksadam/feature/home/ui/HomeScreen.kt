package com.samdori93.yeoksadam.feature.home.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeUiEvent
import com.samdori93.yeoksadam.feature.home.viewmodel.HomeUiState

/** 홈 — 내 주변 인물 (stateless, 목업 1번 화면). */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji),
    ) {
        BrandTopBar(onBell = { onEvent(HomeUiEvent.OpenNotifications) })

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    color = DancheongColors.Jujak,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.errorMessage != null -> ErrorContent(
                    message = state.errorMessage,
                    onRetry = { onEvent(HomeUiEvent.Retry) },
                    modifier = Modifier.align(Alignment.Center),
                )
                state.isEmpty -> EmptyContent(Modifier.align(Alignment.Center))
                else -> NearbyContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun BrandTopBar(onBell: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(DancheongColors.Jujak, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("談", color = Color.White, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "역사담",
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = DancheongColors.Meok,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
                .clickable(onClick = onBell),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = DancheongColors.Meok, modifier = Modifier.size(20.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(8.dp)
                    .background(DancheongColors.Jujak, CircleShape),
            )
        }
    }
}

@Composable
private fun NearbyContent(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
) {
    val selected = state.selected ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "내 주변 인물",
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 2.dp, bottom = 4.dp),
            fontFamily = NanumMyeongjo,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DancheongColors.Meok,
        )

        // hero: ◀  medallion  ▶
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Chevron(
                icon = Icons.Filled.ChevronLeft,
                enabled = state.hasPrev,
                onClick = { onEvent(HomeUiEvent.PrevFigure) },
            )
            MedallionPortrait(
                portraitUrl = selected.figure.portraitUrl.ifBlank { null },
                name = selected.figure.name,
                sealMark = selected.figure.seal.ifBlank { selected.figure.name.take(1) },
                size = 170.dp,
                selected = true,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clickable { onEvent(HomeUiEvent.OpenFigureSheet(selected.figure.id)) },
            )
            Chevron(
                icon = Icons.Filled.ChevronRight,
                enabled = state.hasNext,
                onClick = { onEvent(HomeUiEvent.NextFigure) },
            )
        }

        // 이름 + 호
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = selected.figure.name,
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = DancheongColors.Meok,
            )
            if (selected.figure.title.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = selected.figure.title,
                    fontSize = 12.sp,
                    color = DancheongColors.CheongnokDeep,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }

        // 페이지 도트
        Dots(count = state.nearby.size, selected = state.selectedIndex, modifier = Modifier.padding(vertical = 11.dp))

        // 거리 pill
        DistancePill(text = "${selected.site.name} · ${formatDistance(selected.distanceM)}")

        Spacer(Modifier.height(14.dp))

        // 모든 인물 보기
        AllFiguresButton(onClick = { onEvent(HomeUiEvent.OpenAllFigures) })

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Chevron(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = DancheongColors.MeokSoft, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun Dots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (on) 18.dp else 6.dp)
                    .background(
                        if (on) DancheongColors.Jujak else DancheongColors.Meok.copy(alpha = 0.12f),
                        RoundedCornerShape(6.dp),
                    ),
            )
        }
    }
}

@Composable
private fun DistancePill(text: String) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .background(DancheongColors.Cheongnok.copy(alpha = 0.14f), RoundedCornerShape(30.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = DancheongColors.CheongnokDeep, modifier = Modifier.size(13.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DancheongColors.CheongnokDeep)
    }
}

@Composable
private fun AllFiguresButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .background(DancheongColors.Meok, RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("모든 인물 보기", color = DancheongColors.Hanji, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DancheongColors.Hanji, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("주변에 만날 수 있는 인물이 없습니다.", fontWeight = FontWeight.Medium, color = DancheongColors.MeokSoft, textAlign = TextAlign.Center)
        Text("유적지 근처로 이동해 보세요.", fontSize = 13.sp, color = DancheongColors.MeokSoft, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = DancheongColors.JujakDeep, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        AllFiguresButton(onClick = onRetry) // 재시도 자리 (간이)
    }
}

// ----- Preview -----

private val previewState = HomeUiState(
    isLoading = false,
    selectedIndex = 0,
    nearby = listOf(
        NearbyFigure(
            figure = Figure(id = "fig_chae", name = "채제공", title = "번암", portraitUrl = ""),
            site = HeritageSite("s1", "수원화성", 37.28, 127.01, "", 300f),
            distanceM = 30f,
            bearingDeg = 45f,
        ),
        NearbyFigure(
            figure = Figure(id = "fig_sejong", name = "세종", title = "조선 제4대 왕", portraitUrl = ""),
            site = HeritageSite("s2", "경복궁", 37.57, 126.97, "", 150f),
            distanceM = 1240f,
            bearingDeg = 120f,
        ),
        NearbyFigure(
            figure = Figure(id = "fig_sin", name = "신사임당", title = "사임당", portraitUrl = ""),
            site = HeritageSite("s3", "오죽헌", 37.78, 128.88, "", 150f),
            distanceM = 2100f,
            bearingDeg = 200f,
        ),
        NearbyFigure(
            figure = Figure(id = "fig_go", name = "고종", title = "광무제", portraitUrl = ""),
            site = HeritageSite("s4", "덕수궁", 37.56, 126.97, "", 120f),
            distanceM = 3300f,
            bearingDeg = 300f,
        ),
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFFF4EAD9, heightDp = 720)
@Composable
private fun HomeScreenPreview() {
    YeoksadamTheme {
        HomeScreen(state = previewState, onEvent = {})
    }
}
