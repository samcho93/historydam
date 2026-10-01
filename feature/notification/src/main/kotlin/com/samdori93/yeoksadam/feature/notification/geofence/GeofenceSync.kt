package com.samdori93.yeoksadam.feature.notification.geofence

import android.content.Context
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 지오펜스에 넣을 유적 고르기 — 인물이 있는 유적(우선) + 지금 위치에서 가까운 유적, 최대 95곳.
 * 앱을 열 때마다 다시 등록해 재부팅·업데이트로 지워진 지오펜스를 복구하고, 위치가 바뀐 만큼 주변 유적을 바꾼다.
 */
@Singleton
class GeofenceSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: CatalogRepository,
    private val location: LocationRepository,
) {
    suspend fun sync(): Boolean {
        if (!GeofenceManager.hasLocationPermission(context)) return false
        val figures = catalog.figures().getOrNull().orEmpty()
        val figureAt = HashMap<String, String>()
        figures.forEach { f -> f.relatedSiteIds.forEach { figureAt.putIfAbsent(it, f.name) } }

        val here = location.observeLocation().first()
        val figureSites = figureAt.keys.mapNotNull { catalog.site(it) }
        val near = catalog.nearbySites(here, NEAR_RADIUS_M).map { it.site }.filter { it.id !in figureAt }
        val sites = (figureSites + near).take(GeofenceManager.MAX_FENCES).map {
            GeofenceManager.Site(it.id, it.name, it.lat, it.lng, figure = figureAt[it.id], radiusM = it.geofenceRadiusM)
        }
        return suspendCoroutine { cont -> GeofenceManager.register(context, sites) { cont.resume(it) } }
    }

    private companion object {
        const val NEAR_RADIUS_M = 20_000
    }
}
