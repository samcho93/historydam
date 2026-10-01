package com.samdori93.yeoksadam.feature.notification.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * 유적지 지오펜스 등록 (CLAUDE.md §10) — 앱이 닫혀 있어도 유적에 들어서면 알림.
 * 안드로이드는 앱당 100곳까지라, 인물 유적 + 지금 위치에서 가까운 유적을 골라 등록한다.
 */
object GeofenceManager {

    /** @param figure 이 유적에서 만날 수 있는 인물 (없으면 null) */
    data class Site(val id: String, val name: String, val lat: Double, val lng: Double, val figure: String? = null, val radiusM: Float = 150f)

    const val MAX_FENCES = 95
    private const val SEP = "\u001F"

    /** 지오펜스 id 에 유적 정보를 담는다 (수신기가 DB 없이 알림 문구를 만든다) */
    fun requestId(site: Site) = listOf(site.id, site.name, site.figure.orEmpty()).joinToString(SEP).take(100)

    fun parse(requestId: String): Triple<String, String, String?> {
        val p = requestId.split(SEP)
        return Triple(p.getOrElse(0) { "" }, p.getOrElse(1) { "유적지" }, p.getOrNull(2)?.ifBlank { null })
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** 앱이 닫혀 있을 때도 알림을 받으려면 「항상 허용」(Android 10+) */
    fun hasBackgroundPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** 기존 지오펜스를 지우고 다시 등록. 권한이 없으면 false. */
    @SuppressLint("MissingPermission")
    fun register(context: Context, sites: List<Site>, onResult: (Boolean) -> Unit = {}) {
        if (!hasLocationPermission(context) || sites.isEmpty()) {
            onResult(false)
            return
        }
        val client = LocationServices.getGeofencingClient(context)
        val geofences = sites.take(MAX_FENCES).map { site ->
            Geofence.Builder()
                .setRequestId(requestId(site))
                .setCircularRegion(site.lat, site.lng, site.radiusM)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }
        val request = GeofencingRequest.Builder()
            // 이미 안에 있을 때는 알리지 않는다 (앱을 열 때마다 같은 알림이 쌓이지 않게)
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()
        client.removeGeofences(pendingIntent(context)).addOnCompleteListener {
            client.addGeofences(request, pendingIntent(context))
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        }
    }
}
