package com.samdori93.yeoksadam.feature.profile.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.component.MonoMedallion
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.model.DiscoveryType
import com.samdori93.yeoksadam.feature.profile.viewmodel.ProfileViewModel

/** 내 프로필 · 역사의 전당 도감 (목업 8번). */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val discoveries by viewModel.discoveries.collectAsStateWithLifecycle()
    ProfileContent(
        discoveries = discoveries,
        onBack = onBack,
        onDelete = viewModel::remove,
        modifier = modifier,
    )
}

private val TAB_TYPES = listOf(DiscoveryType.SITE, DiscoveryType.FIGURE, DiscoveryType.RELIC)
private val TAB_LABELS = listOf("유적지", "인물", "유물")

@Composable
private fun ProfileContent(
    discoveries: List<Discovery>,
    onBack: () -> Unit,
    onDelete: (Discovery) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(2) } // 0 유적지, 1 인물, 2 유물
    var selected by remember { mutableStateOf<Discovery?>(null) }
    val figureCount = discoveries.count { it.type == DiscoveryType.FIGURE }
    val current = discoveries.filter { it.type == TAB_TYPES[tab] }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding(),
    ) {
        // 상단바
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Home, "홈", tint = DancheongColors.Meok, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
            Text(
                "내 프로필",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DancheongColors.Meok,
            )
            Icon(Icons.Filled.MoreHoriz, "더보기", tint = DancheongColors.Meok, modifier = Modifier.size(24.dp))
        }

        // 내 카드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoMedallion(text = "희", size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("희수", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DancheongColors.Meok)
                Text("역사 탐험가", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Star, null, tint = DancheongColors.Hwangto, modifier = Modifier.size(13.dp))
                    Text(
                        "발견 ${discoveries.size} · 만난 인물 ${figureCount}명",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DancheongColors.CheongnokDeep,
                    )
                }
            }
        }

        // 설정 (대화 방식 · 지식 경계 · 대화 기록)
        SettingsCard()
        Spacer(Modifier.size(14.dp))

        // 역사의 전당
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.size(24.dp).background(DancheongColors.Jujak, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("堂", color = Color.White, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text("역사의 전당", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
        }

        // 탭
        Row(
            modifier = Modifier
                .padding(16.dp)
                .background(DancheongColors.HanjiDim, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TAB_LABELS.forEachIndexed { i, label ->
                val on = i == tab
                val count = discoveries.count { it.type == TAB_TYPES[i] }
                Box(
                    modifier = Modifier
                        .background(if (on) DancheongColors.HanjiCard else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { tab = i }
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (count > 0) "$label $count" else label,
                        color = if (on) DancheongColors.Jujak else DancheongColors.MeokSoft,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // 도감 그리드 / 빈 상태
        if (current.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "아직 발견한 ${TAB_LABELS[tab]}이 없어요.\n카메라로 문화재를 촬영해 보세요.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = DancheongColors.MeokSoft,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(current, key = { "${it.type}_${it.refId}" }) { item ->
                    DiscoveryCell(item, onClick = { selected = item })
                }
            }
        }
    }

    selected?.let { item ->
        DiscoveryDetailDialog(
            discovery = item,
            onDismiss = { selected = null },
            onDelete = {
                onDelete(item)
                selected = null
            },
        )
    }
}

@Composable
private fun DiscoveryCell(item: Discovery, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(84.dp).clip(CircleShape).background(DancheongColors.HanjiDim),
            )
        } else {
            MedallionPortrait(portraitUrl = null, name = item.name, size = 84.dp)
        }
        Spacer(Modifier.size(6.dp))
        Text(
            item.name,
            fontSize = 12.sp,
            color = DancheongColors.Meok,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 도감 항목을 누르면 뜨는 상세 — 이미지 + 명칭 + 설명 다시 보기. */
@Composable
private fun DiscoveryDetailDialog(discovery: Discovery, onDismiss: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DancheongColors.Hanji, RoundedCornerShape(20.dp))
                .padding(18.dp),
        ) {
            if (discovery.imageUrl != null) {
                AsyncImage(
                    model = discovery.imageUrl,
                    contentDescription = discovery.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DancheongColors.HanjiDim),
                )
                Spacer(Modifier.size(12.dp))
            }
            Text(
                discovery.name,
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = DancheongColors.Meok,
            )
            if (discovery.subtitle.isNotBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(discovery.subtitle, fontSize = 12.sp, color = DancheongColors.MeokSoft)
            }
            Spacer(Modifier.size(12.dp))
            Text(
                discovery.description.ifBlank { "저장된 설명이 없습니다." },
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = DancheongColors.Meok,
                modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.size(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // 삭제 (테두리)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, DancheongColors.Jujak, RoundedCornerShape(24.dp))
                        .clickable(onClick = onDelete)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = DancheongColors.Jujak, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("삭제", color = DancheongColors.Jujak, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                // 닫기 (채움)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DancheongColors.Jujak, RoundedCornerShape(24.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("닫기", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun ProfilePreview() {
    YeoksadamTheme {
        ProfileContent(
            discoveries = listOf(
                Discovery(DiscoveryType.RELIC, "r1", "백자 달항아리", "국보 · 18세기", "조선의 백자 달항아리입니다.", null, 3),
                Discovery(DiscoveryType.FIGURE, "fig_chae", "채제공", "번암", "정조 대의 명재상.", null, 1),
            ),
            onBack = {},
            onDelete = {},
        )
    }
}
