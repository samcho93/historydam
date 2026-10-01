package com.samdori93.yeoksadam.core.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * FusedLocation 기반 위치 스트림.
 * 권한 미허용/측위 실패 시 기본 위치(경복궁)를 emit 해 홈이 깨지지 않도록 한다(graceful 폴백).
 */
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedClient: FusedLocationProviderClient,
) : LocationRepository {

    /** 권한이 없으면 기본 위치를 먼저 내보내고, 권한이 생기면(앱 시작 때 허용 등) 실제 위치로 넘어간다. */
    override fun observeLocation(): Flow<LatLng> = flow {
        emit(DEFAULT_LOCATION)
        while (!hasLocationPermission()) delay(PERMISSION_POLL_MS)
        emitAll(updates())
    }

    @SuppressLint("MissingPermission")
    private fun updates(): Flow<LatLng> = callbackFlow {
        // 마지막으로 알려진 위치를 먼저 (첫 측위를 기다리지 않게)
        fusedClient.lastLocation.addOnSuccessListener { it?.let { l -> trySend(LatLng(l.latitude, l.longitude)) } }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(LatLng(it.latitude, it.longitude)) }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val UPDATE_INTERVAL_MS = 10_000L
        const val MIN_UPDATE_INTERVAL_MS = 5_000L
        const val PERMISSION_POLL_MS = 1_500L

        /** 수원 화성행궁 — 측위 불가 시 데모 기본 위치 (시범 지역 경기도, 웹앱과 같음). */
        val DEFAULT_LOCATION = LatLng(37.2818, 127.0137)
    }
}
