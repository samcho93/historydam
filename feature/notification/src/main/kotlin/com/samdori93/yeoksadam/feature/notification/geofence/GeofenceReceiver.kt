package com.samdori93.yeoksadam.feature.notification.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** 지오펜스 진입 → 로컬 알림 + 알림 목록 기록 (웹앱의 도착 알림과 같은 문구). */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        event.triggeringGeofences.orEmpty().forEach { fence ->
            val (siteId, name, figure) = GeofenceManager.parse(fence.requestId)
            val title = "${name}에 도착했습니다"
            val body = if (figure != null) {
                "$figure${josa(figure)} 만날 수 있어요. 앱을 열어 대화해 보세요."
            } else {
                "AR 로 주변 유적을 둘러보고 해설사와 이야기해 보세요."
            }
            AppNotifications.post(context, title, body)
            NotificationStore.add(context, NotificationStore.Event(title, body, System.currentTimeMillis(), siteId))
        }
    }

    /** 받침에 맞는 목적격 조사 */
    private fun josa(word: String): String {
        val last = word.lastOrNull { it in '가'..'힣' } ?: return "을"
        return if ((last - '가') % 28 != 0) "을" else "를"
    }
}
