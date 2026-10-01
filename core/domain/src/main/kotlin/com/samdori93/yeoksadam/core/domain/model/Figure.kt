package com.samdori93.yeoksadam.core.domain.model

/**
 * 역사 인물 (CLAUDE.md §7).
 * 데이터 원본은 역사담 웹앱(arHeritage)의 `data/figures.json` — 웹앱과 같은 인물·초상·목소리를 쓴다.
 */
data class Figure(
    val id: String,
    val name: String,
    val title: String,
    val portraitUrl: String,
    val cutoutUrl: String? = null,
    val relatedSiteIds: List<String> = emptyList(),
    val voiceId: String? = null,
    /** 한자 이름 (초상이 없을 때 인장) */
    val hanja: String = "",
    /** 생몰년 표기 (예: 1752–1800) */
    val years: String = "",
    val bio: String = "",
    /** 말씨·전신 모습 기준: king · scholar · lady · general */
    val style: String = "scholar",
    /** 메달 낙관 글자 */
    val seal: String = "",
    /** 관련 유적 id → 관계 설명 */
    val siteNotes: Map<String, String> = emptyMap(),
    /** 지식 경계: 세상을 떠난 해 */
    val died: Int? = null,
    /** 초상이 없을 때 AR 에 세울 전신 실루엣 종류 */
    val fullBody: String? = null,
    val portraitCredit: String? = null,
)

/** 유적지 (국가유산 · 향토유산 · 역사관광지). */
data class HeritageSite(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val geofenceRadiusM: Float,
    /** 국보·보물·사적·향토유산 등 */
    val designation: String = "",
    /** 유적건조물·유물 등 */
    val category: String = "",
    val era: String = "",
    val city: String = "",
    /** 시·군 향토유산 */
    val local: Boolean = false,
    /** 경기도 역사관광지(생가) */
    val tour: Boolean = false,
    val thumbUrl: String? = null,
)

/** 유적 상세 (설명·사진·주소). */
data class SiteDetail(
    val id: String,
    val name: String,
    val hanja: String,
    val designation: String,
    val category: String,
    val era: String,
    val city: String,
    val address: String,
    val description: String,
    val images: List<SiteImage>,
    val lat: Double,
    val lng: Double,
    val tel: String? = null,
    val sourceUrl: String? = null,
    val local: Boolean = false,
    val tour: Boolean = false,
)

data class SiteImage(val url: String, val desc: String)

/** 유물. */
data class Relic(
    val id: String,
    val name: String,
    val era: String,
    val relatedFigureIds: List<String>,
)

/** 홈/지도에서 노출하는 "내 주변 인물" 카드 데이터. */
data class NearbyFigure(
    val figure: Figure,
    val site: HeritageSite,
    val distanceM: Float,
    val bearingDeg: Float,
)

/** 주변 유적 (지도 핀 · AR 라벨). */
data class NearbySite(
    val site: HeritageSite,
    val distanceM: Float,
    val bearingDeg: Float,
)

/** AR 탐색 대상. */
data class ArTarget(
    val figureId: String,
    val siteId: String,
    val lat: Double,
    val lng: Double,
    val altitude: Double? = null,
    val headingDeg: Float? = null,
    val foundThresholdM: Float = 5f,
    val bearingToleranceDeg: Float = 20f,
)
