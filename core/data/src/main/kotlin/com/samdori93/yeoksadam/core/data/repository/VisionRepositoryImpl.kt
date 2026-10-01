package com.samdori93.yeoksadam.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.VisionRepository
import com.samdori93.yeoksadam.core.network.worker.VisionBody
import com.samdori93.yeoksadam.core.network.worker.VisionCandidate
import com.samdori93.yeoksadam.core.network.worker.WorkerApi
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max

/**
 * 사진 속 문화재 판별 — 역사담 서버 /vision (Claude 비전, 웹앱과 같음).
 * 앱에 AI 키를 넣지 않는다. 사진은 긴 변 1280px JPEG 로 줄여 보낸다.
 */
class VisionRepositoryImpl @Inject constructor(
    private val worker: WorkerApi,
    private val dispatchers: DispatcherProvider,
) : VisionRepository {

    override suspend fun recognize(imageJpeg: ByteArray, candidates: List<RecognizedHeritage>): Result<HeritageVisionResult> =
        try {
            val image = withContext(dispatchers.default) { Base64.encodeToString(shrink(imageJpeg), Base64.NO_WRAP) }
            val r = worker.vision(
                VisionBody(
                    image = image,
                    candidates = candidates.take(MAX_CANDIDATES).map { VisionCandidate(it.id, it.name, it.kind, it.address) },
                ),
            )
            Result.Success(
                HeritageVisionResult(
                    name = r.name,
                    kind = r.kind,
                    era = r.era,
                    description = r.description,
                    matchedHeritageId = r.matchedId,
                    confidence = r.confidence,
                    isHeritage = r.isHeritage,
                    category = r.category,
                ),
            )
        } catch (e: HttpException) {
            Result.Failure(AppError.Server(e.code(), "사진을 판별하지 못했습니다."))
        } catch (e: IOException) {
            Result.Failure(AppError.Network(e))
        }

    private fun shrink(jpeg: ByteArray): ByteArray {
        val src = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return jpeg
        val scale = MAX_SIDE.toFloat() / max(src.width, src.height)
        val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true) else src
        return ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val MAX_SIDE = 1280
        const val MAX_CANDIDATES = 12
    }
}
