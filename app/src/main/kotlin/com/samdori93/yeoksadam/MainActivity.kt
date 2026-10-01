package com.samdori93.yeoksadam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.navigation.YeoksadamAppRoot
import androidx.lifecycle.lifecycleScope
import com.samdori93.yeoksadam.feature.notification.geofence.GeofenceSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var geofenceSync: GeofenceSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 위치 권한이 있으면 유적 도착 지오펜스를 다시 맞춘다 (재부팅·업데이트로 지워진 것 복구, 주변 유적 갱신)
        lifecycleScope.launch { runCatching { geofenceSync.sync() } }
        setContent {
            YeoksadamTheme {
                YeoksadamAppRoot(onLocationGranted = { lifecycleScope.launch { runCatching { geofenceSync.sync() } } })
            }
        }
    }
}
