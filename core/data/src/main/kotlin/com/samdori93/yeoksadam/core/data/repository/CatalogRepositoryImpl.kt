package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.geo.GeoMath
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.domain.model.SiteDetail
import com.samdori93.yeoksadam.core.domain.model.SiteImage
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.network.catalog.CatalogApi
import com.samdori93.yeoksadam.core.network.catalog.CatalogFigureDto
import com.samdori93.yeoksadam.core.network.catalog.SiteDto
import com.samdori93.yeoksadam.core.network.di.NetworkModule
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 역사담 웹앱 데이터로 동작하는 카탈로그.
 * 유적 목록(약 350KB)·인물 목록은 처음 한 번 받아 메모리에 두고, HTTP 디스크 캐시로 다음 실행도 빠르게 한다.
 */
@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val api: CatalogApi,
    private val dispatchers: DispatcherProvider,
) : CatalogRepository {

    private val lock = Mutex()
    private var siteList: List<HeritageSite>? = null
    private var siteMap: Map<String, HeritageSite> = emptyMap()
    private var figureList: List<Figure>? = null
    private val details = HashMap<String, SiteDetail>()

    override suspend fun sites(): Result<List<HeritageSite>> = load { ensureSites() }

    override suspend fun figures(): Result<List<Figure>> = load { ensureFigures() }

    override suspend fun figure(id: String): Figure? = figures().getOrNull()?.firstOrNull { it.id == id }

    override suspend fun site(id: String): HeritageSite? {
        sites()
        return siteMap[id]
    }

    override suspend fun siteDetail(id: String): Result<SiteDetail> = load {
        details[id] ?: api.detail(id).let { d ->
            SiteDetail(
                id = d.id,
                name = d.name,
                hanja = d.nameHanja,
                designation = d.designation,
                category = d.category,
                era = d.era,
                city = d.city,
                address = d.address,
                description = d.description,
                images = d.images.map { SiteImage(it.url, it.desc) },
                lat = d.lat,
                lng = d.lng,
                tel = d.tel,
                sourceUrl = d.sourceUrl,
                local = d.local,
                tour = d.tour,
            )
        }.also { details[id] = it }
    }

    override suspend fun nearbySites(origin: LatLng, radiusM: Int): List<NearbySite> {
        val all = sites().getOrNull() ?: return emptyList()
        return withContext(dispatchers.default) {
            all.mapNotNull { s ->
                val d = GeoMath.distanceMeters(origin.lat, origin.lng, s.lat, s.lng)
                if (d > radiusM) null else NearbySite(s, d, GeoMath.bearingDegrees(origin.lat, origin.lng, s.lat, s.lng))
            }.sortedBy { it.distanceM }
        }
    }

    override suspend fun nearestSiteOf(figure: Figure, origin: LatLng): NearbyFigure? {
        sites()
        return figure.relatedSiteIds.mapNotNull { siteMap[it] }.map { s ->
            NearbyFigure(
                figure = figure,
                site = s,
                distanceM = GeoMath.distanceMeters(origin.lat, origin.lng, s.lat, s.lng),
                bearingDeg = GeoMath.bearingDegrees(origin.lat, origin.lng, s.lat, s.lng),
            )
        }.minByOrNull { it.distanceM }
    }

    private suspend fun ensureSites(): List<HeritageSite> = lock.withLock {
        siteList ?: api.index().items.map { it.toDomain() }.also { list ->
            siteList = list
            siteMap = list.associateBy { it.id }
        }
    }

    private suspend fun ensureFigures(): List<Figure> = lock.withLock {
        figureList ?: api.figures().figures.map { it.toDomain() }.also { figureList = it }
    }

    private suspend fun <T> load(block: suspend () -> T): Result<T> = withContext(dispatchers.io) {
        try {
            Result.Success(block())
        } catch (e: HttpException) {
            Result.Failure(AppError.Server(e.code(), e.message()))
        } catch (e: IOException) {
            Result.Failure(AppError.Network(e))
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e))
        }
    }

    private fun SiteDto.toDomain() = HeritageSite(
        id = id,
        name = name,
        lat = lat,
        lng = lng,
        description = "",
        geofenceRadiusM = ARRIVAL_RADIUS_M,
        designation = designation,
        category = category,
        era = era,
        city = city,
        local = local,
        tour = tour,
        thumbUrl = thumb,
    )

    private fun CatalogFigureDto.toDomain() = Figure(
        id = id,
        name = name,
        title = title,
        portraitUrl = NetworkModule.assetUrl(portrait).orEmpty(),
        cutoutUrl = NetworkModule.assetUrl(cutout),
        relatedSiteIds = sites.map { it.id },
        voiceId = voice,
        hanja = hanja,
        years = years,
        bio = bio,
        style = style,
        seal = seal,
        siteNotes = sites.associate { it.id to it.note },
        died = died,
        fullBody = fullBody,
        portraitCredit = portraitCredit,
    )

    private companion object {
        /** 웹앱과 같은 도착 판정 거리 */
        const val ARRIVAL_RADIUS_M = 150f
    }
}
