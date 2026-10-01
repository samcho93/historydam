package com.samdori93.yeoksadam.feature.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.feature.notification.geofence.GeofenceSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val geofenceSync: GeofenceSync,
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    /** 인물 유적 + 주변 유적 지오펜스 등록 */
    fun enable(background: Boolean) {
        viewModelScope.launch {
            _status.value = "등록하는 중…"
            val ok = geofenceSync.sync()
            _status.value = when {
                !ok -> "등록하지 못했어요. 위치 권한과 위치 서비스를 확인해 주세요."
                background -> "인물 유적과 주변 유적에 도착하면 알려 드려요. (앱이 닫혀 있어도)"
                else -> "등록했어요. 앱이 닫혀 있을 때도 받으려면 위치 권한을 「항상 허용」으로 바꿔 주세요."
            }
        }
    }
}
