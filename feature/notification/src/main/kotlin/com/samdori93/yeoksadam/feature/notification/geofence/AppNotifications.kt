package com.samdori93.yeoksadam.feature.notification.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** 로컬 알림 채널 생성 + 알림 게시 헬퍼. */
object AppNotifications {

    const val CHANNEL_ID = "heritage_geofence"
    private var notifId = 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "유적지 방문 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "유적지 진입 시 알려드립니다." }
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(channel)
        }
    }

    /** 알림 게시. POST_NOTIFICATIONS 미허용 시 조용히 무시(크래시 없음). */
    fun post(context: Context, title: String, body: String) {
        ensureChannel(context)
        // 알림을 누르면 앱을 연다
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            android.app.PendingIntent.getActivity(
                context,
                0,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(open)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId++, notification)
        }
    }
}
