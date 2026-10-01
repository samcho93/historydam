package com.samdori93.yeoksadam.feature.notification.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.feature.notification.geofence.AppNotifications
import com.samdori93.yeoksadam.feature.notification.geofence.GeofenceManager
import com.samdori93.yeoksadam.feature.notification.geofence.NotificationStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 위치 기반 알림 (목업) — 지오펜스 등록 + 수신 알림 목록 + 테스트. */
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: NotificationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val status by viewModel.status.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { NotificationStore.load(context) }
    val events by NotificationStore.events.collectAsStateWithLifecycle()

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // 2단계: 「항상 허용」(앱이 닫혀 있어도 도착 알림) — Android 11+ 는 설정 화면으로 이동
    val bgPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.enable(background = granted)
    }
    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        when {
            result.values.none { it } -> Toast.makeText(context, "위치 권한이 있어야 유적 도착을 알 수 있어요.", Toast.LENGTH_SHORT).show()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !GeofenceManager.hasBackgroundPermission(context) ->
                bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else -> viewModel.enable(background = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            "위치 알림",
            modifier = Modifier.padding(vertical = 12.dp),
            fontFamily = NanumMyeongjo,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = DancheongColors.Meok,
        )

        // 액션 버튼
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                label = "유적 도착 알림 켜기",
                solid = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    locPermLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
            )
            ActionButton(
                label = "테스트 알림",
                solid = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val title = "수원 화성에 도착했습니다"
                    val body = "정조를 만날 수 있어요. 앱을 열어 대화해 보세요."
                    AppNotifications.post(context, title, body)
                    NotificationStore.add(context, NotificationStore.Event(title, body, System.currentTimeMillis()))
                },
            )
        }

        status?.let {
            Text(it, fontSize = 12.sp, color = DancheongColors.CheongnokDeep, modifier = Modifier.padding(top = 10.dp))
        }

        Spacer(Modifier.height(16.dp))

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = DancheongColors.HanjiDim, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("받은 알림이 없습니다.", color = DancheongColors.MeokSoft, fontSize = 14.sp)
                    Text("유적지에 진입하면 알림이 도착합니다.", color = DancheongColors.MeokSoft, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(events) { e -> EventRow(e) }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, solid: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val base = modifier.height(46.dp)
    val styled = if (solid) base.background(DancheongColors.Jujak, shape) else base.border(1.5.dp, DancheongColors.Jujak, shape)
    Box(styled.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            label,
            color = if (solid) Color.White else DancheongColors.Jujak,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun EventRow(event: NotificationStore.Event) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DancheongColors.HanjiCard, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(DancheongColors.Cheongnok, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Place, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Meok)
            Text(event.body, fontSize = 12.sp, color = DancheongColors.MeokSoft)
        }
        Text(formatTime(event.timeMillis), fontSize = 11.sp, color = DancheongColors.MeokSoft)
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(millis))

@Preview(heightDp = 720)
@Composable
private fun NotificationPreview() {
    YeoksadamTheme {
        NotificationScreen()
    }
}
