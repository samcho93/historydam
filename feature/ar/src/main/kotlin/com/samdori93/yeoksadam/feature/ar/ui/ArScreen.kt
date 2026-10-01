package com.samdori93.yeoksadam.feature.ar.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.samdori93.yeoksadam.core.common.geo.GeoMath
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.ui.ar.CameraBackground
import com.samdori93.yeoksadam.core.ui.ar.angleDiff
import com.samdori93.yeoksadam.core.ui.ar.rememberCameraOrientation
import com.samdori93.yeoksadam.feature.ar.viewmodel.ArUiState
import com.samdori93.yeoksadam.feature.ar.viewmodel.ArViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

private val RADII = listOf(1_000, 3_000, 5_000, 10_000)

@Composable
fun ArRoute(
    onBack: () -> Unit,
    onTalk: (figureId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }
    ArScreen(state, hasCamera, viewModel, onBack, onTalk, modifier)
}

/** 위치 기반 AR (웹앱 AR 화면) — 카메라에 비친 방향의 유적을 라벨로, 겹치면 대표 + 「+N」. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArScreen(
    state: ArUiState,
    hasCamera: Boolean,
    vm: ArViewModel,
    onBack: () -> Unit,
    onTalk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val orientation by rememberCameraOrientation()

    BoxWithConstraints(modifier.fillMaxSize().background(Color(0xFF1A140F))) {
        if (hasCamera) CameraBackground(Modifier.fillMaxSize())
        val w = maxWidth.value
        val h = maxHeight.value
        val layout = layoutLabels(state.sites, orientation.heading, orientation.pitch, w, h, state.radiusM, state.target, state.important)

        // 라벨
        layout.labels.forEach { l ->
            Box(
                Modifier
                    .zIndex(if (l.isTarget) 2000f else 1000f + l.score)
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            p.place((l.x.dp.roundToPx() - p.width / 2), (l.y.dp.roundToPx() - p.height))
                        }
                    },
            ) {
                ArLabel(l) { if (l.hidden.isNotEmpty()) vm.openGroup(listOf(l.nearby) + l.hidden) else vm.open(l.nearby) }
            }
        }

        // 상단: 홈 · 타깃 칩 · 반경
        Column(Modifier.zIndex(5000f).statusBarsPadding().padding(12.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Home, "홈", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.weight(1f).background(DancheongColors.HanjiCard.copy(alpha = 0.95f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val t = state.target
                    if (t == null) {
                        Text("주변 유적지 탐색", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DancheongColors.Meok, modifier = Modifier.weight(1f))
                    } else {
                        state.targetFigure?.let {
                            MedallionPortrait(portraitUrl = it.portraitUrl.ifBlank { null }, name = it.name, sealMark = it.seal.ifBlank { null }, size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(state.targetFigure?.name ?: t.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DancheongColors.Meok, maxLines = 1)
                            Text(if (state.targetFigure != null) t.name else "여기로 안내하는 중", fontSize = 11.sp, color = DancheongColors.MeokSoft, maxLines = 1)
                        }
                        Text("✕", fontSize = 14.sp, color = DancheongColors.MeokSoft, modifier = Modifier.clickable { vm.clearTarget() }.padding(4.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.align(Alignment.CenterHorizontally).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RADII.forEach { r ->
                    val on = r == state.radiusM
                    Text(
                        "${r / 1000}km",
                        color = Color.White.copy(alpha = if (on) 1f else 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.background(if (on) DancheongColors.Jujak else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { vm.selectRadius(r) }.padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
            }
            if (!orientation.available) {
                Text(
                    "방향 센서를 찾을 수 없어요. 라벨 방향이 맞지 않을 수 있어요.",
                    color = Color.White, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(8.dp),
                )
            }
        }

        // 타깃 방향 화살표
        val here = state.here
        val target = state.target
        if (target != null && here != null) {
            val dist = GeoMath.distanceMeters(here.lat, here.lng, target.lat, target.lng)
            val delta = angleDiff(orientation.heading, GeoMath.bearingDegrees(here.lat, here.lng, target.lat, target.lng))
            Column(
                Modifier.zIndex(4000f).align(Alignment.Center).padding(top = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(64.dp).background(DancheongColors.Jujak.copy(alpha = 0.85f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(40.dp).rotate(delta))
                }
                Text(
                    "${target.name}까지 ${formatDistance(dist)}" + when {
                        dist < 30 -> " · 도착했어요!"
                        abs(delta) < 15 -> " · 정면"
                        delta > 0 -> " · 오른쪽으로 ${delta.roundToInt()}°"
                        else -> " · 왼쪽으로 ${(-delta).roundToInt()}°"
                    },
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        // 하단 HUD: ◀ 왼쪽 밖 수 · 방위 · 오른쪽 밖 수 ▶
        Row(
            Modifier.zIndex(5000f).align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (layout.offLeft > 0) "◀ ${layout.offLeft}" else "", color = Color.White, fontSize = 13.sp)
            Text(
                "${orientation.heading.roundToInt()}° ${toCompass(orientation.heading)}",
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
            )
            Text(if (layout.offRight > 0) "${layout.offRight} ▶" else "", color = Color.White, fontSize = 13.sp)
        }
    }

    // 유적 카드
    state.opened?.let { opened ->
        ModalBottomSheet(onDismissRequest = vm::close, containerColor = DancheongColors.HanjiCard) {
            SiteSheet(state, opened, onGuide = { vm.guideTo(opened.site) }, onRoute = { openNaverRoute(context, opened.site.lat, opened.site.lng, opened.site.name) }, onTalk = {
                vm.close()
                onTalk(it)
            })
        }
    }
    // 겹친 유적 목록
    if (state.group.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = vm::close, containerColor = DancheongColors.HanjiCard) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("이 방향의 유적 ${state.group.size}곳", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = DancheongColors.Meok)
                Text("겹쳐 보이던 유적을 모았어요", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    state.group.forEachIndexed { i, n ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.open(n) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text((if (i == 0) "★ " else "") + n.site.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Meok)
                                Text(kindOf(n) + " · " + n.site.city, fontSize = 12.sp, color = DancheongColors.MeokSoft)
                            }
                            Text(formatDistance(n.distanceM), fontSize = 12.sp, color = DancheongColors.CheongnokDeep)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArLabel(l: PlacedLabel, onClick: () -> Unit) {
    val s = l.nearby.site
    val ring = when {
        l.isTarget -> DancheongColors.Jujak
        s.local -> DancheongColors.Cheongnok
        s.tour -> DancheongColors.Gamcheong
        else -> DancheongColors.Hwangto
    }
    Box(Modifier.scale(l.scale)) {
        Column(
            Modifier
                .widthIn(min = 96.dp, max = 176.dp)
                .background(DancheongColors.HanjiCard.copy(alpha = 0.94f), RoundedCornerShape(12.dp))
                .border(1.5.dp, ring, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DancheongColors.Meok, maxLines = 2)
            Text(kindOf(l.nearby), fontSize = 10.sp, color = DancheongColors.MeokSoft, maxLines = 1)
            Text(formatDistance(l.nearby.distanceM), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DancheongColors.CheongnokDeep)
        }
        if (l.hidden.isNotEmpty()) {
            Text(
                "+${l.hidden.size}",
                color = DancheongColors.HwangtoLight, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp).background(DancheongColors.Meok, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun SiteSheet(state: ArUiState, opened: NearbySite, onGuide: () -> Unit, onRoute: () -> Unit, onTalk: (String) -> Unit) {
    val s = opened.site
    val d = state.openedDetail
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
        Text(kindOf(opened), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.background(DancheongColors.Jujak, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
        Text(s.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = DancheongColors.Meok, modifier = Modifier.padding(top = 6.dp))
        Text(listOf(s.city, s.era, formatDistance(opened.distanceM)).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 12.sp, color = DancheongColors.MeokSoft)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("📍", "여기로 안내", Modifier.weight(1f), onGuide)
            Action("🧭", "길찾기", Modifier.weight(1f), onRoute)
        }
        Spacer(Modifier.height(10.dp))
        state.openedFigures.forEach { f ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).background(DancheongColors.Hanji, RoundedCornerShape(14.dp))
                    .border(1.dp, DancheongColors.Meok.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).clickable { onTalk(f.id) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MedallionPortrait(portraitUrl = f.portraitUrl.ifBlank { null }, name = f.name, sealMark = f.seal.ifBlank { null }, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text("${f.name}${withWa(f.name)} 대화", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Meok)
            }
        }
        Text(
            "💬 해설사와 이야기하기",
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).background(DancheongColors.Jujak, RoundedCornerShape(24.dp)).clickable { onTalk("guide:${s.id}") }.padding(vertical = 13.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (d != null) {
            if (d.images.isNotEmpty()) {
                Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.images.take(6).forEach { img ->
                        AsyncImage(model = img.url, contentDescription = img.desc, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(200.dp, 140.dp).background(DancheongColors.HanjiDim, RoundedCornerShape(10.dp)))
                    }
                }
            }
            if (d.description.isNotBlank()) {
                Text("설명", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Jujak, modifier = Modifier.padding(top = 14.dp))
                Text(
                    d.description.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), " "),
                    fontSize = 14.sp, lineHeight = 22.sp, color = DancheongColors.Meok, modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (d.address.isNotBlank()) Text("📍 ${d.address}", fontSize = 12.sp, color = DancheongColors.MeokSoft, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun Action(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.background(DancheongColors.Hanji, RoundedCornerShape(14.dp)).border(1.dp, DancheongColors.Meok.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 18.sp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DancheongColors.Meok)
    }
}

private fun kindOf(n: NearbySite): String = when {
    n.site.local -> "향토유산"
    n.site.tour -> "역사관광지"
    else -> n.site.designation.ifBlank { "유적" }
}

/** 받침에 맞는 「과/와」 */
private fun withWa(word: String): String {
    val last = word.lastOrNull { it in '가'..'힣' } ?: return "와"
    return if ((last - '가') % 28 != 0) "과" else "와"
}

private fun formatDistance(m: Float): String = if (m >= 1000) "%.1fkm".format(m / 1000) else "${m.roundToInt()}m"

/** 네이버 지도 앱 도보 길찾기 (앱이 없으면 웹 지도) */
private fun openNaverRoute(context: Context, lat: Double, lng: Double, name: String) {
    val app = Uri.parse("nmap://route/walk?dlat=$lat&dlng=$lng&dname=${Uri.encode(name)}&appname=${context.packageName}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/p/search/${Uri.encode(name)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
