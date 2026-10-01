package com.samdori93.yeoksadam.feature.map.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.CircleOverlay
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberFusedLocationSource
import com.naver.maps.map.compose.rememberMarkerState
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.feature.map.viewmodel.MapSelection
import com.samdori93.yeoksadam.feature.map.viewmodel.MapUiState
import com.samdori93.yeoksadam.feature.map.viewmodel.MapViewModel
import kotlin.math.cos
import kotlin.math.roundToInt

private val RADII = listOf(1_000, 3_000, 5_000, 10_000)

/** 지도 (목업 4번) — 네이버 지도 + 인물 핑 · 표시 반경 원 · 반경 안 유적 · AR 길찾기. */
@Composable
fun MapRoute(
    onEnterAr: (figureId: String, siteId: String) -> Unit,
    onStartConversation: (figureId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(
        state = state,
        onSelect = viewModel::select,
        onRadius = viewModel::selectRadius,
        onEnterAr = onEnterAr,
        onStartConversation = onStartConversation,
        modifier = modifier,
    )
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MapScreen(
    state: MapUiState,
    onSelect: (MapSelection) -> Unit,
    onRadius: (Int) -> Unit,
    onEnterAr: (figureId: String, siteId: String) -> Unit,
    onStartConversation: (figureId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(LatLng(37.2818, 127.0137), 13.0)
    }

    // 위치 권한
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasLocationPermission = result.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val locationSource = rememberFusedLocationSource()

    // 처음 위치를 받았을 때 / 반경을 바꿨을 때: 반경 원이 화면에 꽉 차게
    var fittedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(state.here, state.fitRequest) {
        val here = state.here ?: return@LaunchedEffect
        if (fittedOnce && state.fitRequest == 0) return@LaunchedEffect
        fittedOnce = true
        val dLat = state.radiusM / 111_320.0
        val dLng = state.radiusM / (111_320.0 * cos(Math.toRadians(here.lat)))
        val bounds = LatLngBounds(LatLng(here.lat - dLat, here.lng - dLng), LatLng(here.lat + dLat, here.lng + dLng))
        cameraPositionState.move(CameraUpdate.fitBounds(bounds, with(density) { 24.dp.roundToPx() }))
    }

    Box(modifier = modifier.fillMaxSize().background(DancheongColors.HanjiDim)) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            locationSource = locationSource,
            properties = MapProperties(
                locationTrackingMode = if (hasLocationPermission) LocationTrackingMode.NoFollow else LocationTrackingMode.None,
            ),
            uiSettings = MapUiSettings(
                isZoomControlEnabled = false,
                isCompassEnabled = true,
                isScaleBarEnabled = false,
                isLocationButtonEnabled = true,
            ),
        ) {
            // 표시 반경 — 반투명 영역 (웹앱과 같은 진사색)
            state.here?.let { here ->
                CircleOverlay(
                    center = LatLng(here.lat, here.lng),
                    radius = state.radiusM.toDouble(),
                    color = DancheongColors.Jujak.copy(alpha = 0.08f),
                    outlineWidth = 1.5.dp,
                    outlineColor = DancheongColors.Jujak.copy(alpha = 0.55f),
                )
            }
            // 반경 안 일반 유적 — 넓게 볼 때는 이름을 숨긴다
            state.sites.forEach { ns ->
                Marker(
                    state = rememberMarkerState(key = "s:${ns.site.id}", position = LatLng(ns.site.lat, ns.site.lng)),
                    width = 18.dp,
                    height = 24.dp,
                    iconTintColor = if (ns.site.local) DancheongColors.Cheongnok else DancheongColors.MeokSoft,
                    captionText = ns.site.name,
                    captionTextSize = 11.sp,
                    captionMinZoom = 13.0,
                    zIndex = 1,
                    onClick = {
                        onSelect(MapSelection.OfSite(ns))
                        true
                    },
                )
            }
            // 인물 핑
            state.figures.forEach { nf ->
                Marker(
                    state = rememberMarkerState(key = "f:${nf.figure.id}", position = LatLng(nf.site.lat, nf.site.lng)),
                    iconTintColor = DancheongColors.Jujak,
                    captionText = nf.figure.name,
                    subCaptionText = nf.site.name,
                    zIndex = 10,
                    onClick = {
                        onSelect(MapSelection.OfFigure(nf))
                        true
                    },
                )
            }
        }

        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            // 경로 헤더 (고른 인물)
            (state.selection as? MapSelection.OfFigure)?.nearby?.let { nf ->
                val near = nf.distanceM <= 3_000
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DancheongColors.HanjiCard, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).background(DancheongColors.Cheongnok, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${nf.site.name}까지 ${if (near) "걷기" else "이동"}",
                            fontFamily = NanumMyeongjo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DancheongColors.Meok,
                            maxLines = 1,
                        )
                        Text(
                            (if (near) "도보 ${walkMinutes(nf.distanceM)}분 · " else "") + "${formatDistance(nf.distanceM)} 이동",
                            fontSize = 11.sp,
                            color = DancheongColors.MeokSoft,
                        )
                    }
                    Text(
                        "AR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(DancheongColors.Jujak, RoundedCornerShape(8.dp))
                            .clickable { onEnterAr(nf.figure.id, nf.site.id) }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            // 표시 반경 선택 (AR 화면과 공유)
            if (state.here != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(DancheongColors.HanjiCard.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    RADII.forEach { r ->
                        val on = r == state.radiusM
                        Text(
                            "${r / 1000}km",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) Color.White else DancheongColors.MeokSoft,
                            modifier = Modifier
                                .background(if (on) DancheongColors.Jujak else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { onRadius(r) }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        // 주변 유적 AR 탐색 (카메라로 비추면 반경 안 유적이 라벨로)
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                .background(DancheongColors.Jujak, RoundedCornerShape(16.dp))
                .clickable { onEnterAr("", "") }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("주변 탐색", color = Color.White, fontSize = 10.sp)
        }

        // 하단 카드
        Box(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            when (val sel = state.selection) {
                is MapSelection.OfFigure -> FigureCard(sel, onEnterAr, onStartConversation)
                is MapSelection.OfSite -> SiteCard(sel, context, onStartConversation)
                null -> Unit
            }
        }
    }
}

@Composable
private fun FigureCard(sel: MapSelection.OfFigure, onEnterAr: (String, String) -> Unit, onTalk: (String) -> Unit) {
    val nf = sel.nearby
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedallionPortrait(
            portraitUrl = nf.figure.portraitUrl.ifBlank { null },
            name = nf.figure.name,
            sealMark = nf.figure.seal.ifBlank { null },
            size = 52.dp,
            modifier = Modifier.clickable { onTalk(nf.figure.id) },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(nf.figure.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
            Text("${nf.site.name} · 약 ${formatDistance(nf.distanceM)} 거리", fontSize = 12.sp, color = DancheongColors.CheongnokDeep, maxLines = 1)
        }
        Pill("찾기", Icons.Filled.Search) { onEnterAr(nf.figure.id, nf.site.id) }
    }
}

@Composable
private fun SiteCard(sel: MapSelection.OfSite, context: Context, onTalk: (String) -> Unit) {
    val s = sel.nearby.site
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(s.name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DancheongColors.Meok)
        Text(
            listOf(if (s.local) "향토유산" else if (s.tour) "역사관광지" else s.designation, s.city, formatDistance(sel.nearby.distanceM))
                .filter { it.isNotBlank() }.joinToString(" · "),
            fontSize = 12.sp,
            color = DancheongColors.MeokSoft,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("해설사와 대화", null) { onTalk("guide:${s.id}") }
            Text(
                "🧭 길찾기",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DancheongColors.Meok,
                modifier = Modifier
                    .border(1.dp, DancheongColors.Meok.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { openNaverRoute(context, s.lat, s.lng, s.name) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun Pill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(DancheongColors.Jujak, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** 네이버 지도 앱 도보 길찾기 (앱이 없으면 웹 지도) */
private fun openNaverRoute(context: Context, lat: Double, lng: Double, name: String) {
    val app = Uri.parse("nmap://route/walk?dlat=$lat&dlng=$lng&dname=${Uri.encode(name)}&appname=${context.packageName}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        val web = Uri.parse("https://map.naver.com/p/search/${Uri.encode(name)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, web).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun formatDistance(m: Float): String = if (m >= 1000) "%.1fkm".format(m / 1000) else "${m.roundToInt()}m"

/** 도보 시속 4.5km */
private fun walkMinutes(m: Float): Int = (m / 75f).roundToInt().coerceAtLeast(1)
