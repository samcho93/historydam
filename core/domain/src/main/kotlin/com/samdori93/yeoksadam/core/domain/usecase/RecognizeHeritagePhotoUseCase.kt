package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.common.result.getOrNull
import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.PhotoRecognition
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.HeritageRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import com.samdori93.yeoksadam.core.domain.repository.VisionRepository
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 촬영한 사진을 AI 비전으로 판별하고, 주변 국가유산 힌트 + 국가유산청 상세로 보강한다.
 *
 * 흐름: 현재 위치 → 주변 국가유산 후보(힌트) → Gemini 비전 판별 → 일치 시 국가유산청 상세 조회.
 */
class RecognizeHeritagePhotoUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val heritageRepository: HeritageRepository,
    private val visionRepository: VisionRepository,
) {
    suspend operator fun invoke(imageJpeg: ByteArray): Result<PhotoRecognition> {
        val candidates = heritageRepository.getNearbyHeritage(currentLocation(), CANDIDATE_LIMIT).getOrNull().orEmpty()

        return when (val vision = visionRepository.recognize(imageJpeg, candidates)) {
            is Result.Success -> {
                val v = vision.data
                // 유물·인물이 주변 '유적지'로 덮어써지지 않도록: 건물/유적(site)일 때만 국가유산청 상세 보강
                val official = v.matchedHeritageId
                    ?.takeIf { v.category == "site" }
                    ?.let { heritageRepository.getHeritageDetail(it).getOrNull() }
                Result.Success(PhotoRecognition(v, candidates, official))
            }
            // AI 비전 불가(키 없음/네트워크 오류) → 위치 기반(LBS) 폴백
            is Result.Failure -> fallbackToNearest(candidates) ?: vision
        }
    }

    /** GEMINI_API_KEY 미설정 등으로 AI 인식이 안 될 때, 가장 가까운 국가유산으로 대체. */
    private suspend fun fallbackToNearest(candidates: List<RecognizedHeritage>): Result<PhotoRecognition>? {
        val nearest = candidates.firstOrNull() ?: return null
        val official = heritageRepository.getHeritageDetail(nearest.id).getOrNull()
        val identification = HeritageVisionResult(
            name = official?.name ?: nearest.name,
            kind = official?.kind ?: nearest.kind,
            era = official?.era.orEmpty(),
            description = "AI 이미지 인식 서버에 연결하지 못해, 현재 위치에서 가장 가까운 국가유산을 표시합니다. " +
                "네트워크를 확인한 뒤 다시 촬영해 보세요.",
            matchedHeritageId = nearest.id,
            confidence = 0,
        )
        return Result.Success(PhotoRecognition(identification, candidates, official))
    }

    private suspend fun currentLocation(): LatLng =
        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            locationRepository.observeLocation().drop(1).first()
        } ?: locationRepository.observeLocation().first()

    private companion object {
        const val CANDIDATE_LIMIT = 8
        const val LOCATION_TIMEOUT_MS = 2_500L
    }
}
