package com.samdori93.yeoksadam.core.network.worker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * 역사담 대화·음성·인식 서버 (Cloudflare Worker, BuildConfig.WORKER_BASE_URL).
 * 웹앱과 같은 서버를 쓰며, 앱은 X-App-Key 헤더로 들어간다([AppKeyInterceptor]).
 */
interface WorkerApi {
    /** 기본 대화 — 인물에 연결된 유적 설명을 근거로 */
    @POST("chat")
    suspend fun chat(@Body body: TalkBody): TalkResponse

    /** RAG 대화 — 질문마다 유적 자료를 검색해 근거로, 출처 표시 */
    @POST("rag")
    suspend fun rag(@Body body: TalkBody): TalkResponse

    /** 인물 목소리 (Azure 신경망 음성, audio/mpeg) */
    @Streaming
    @POST("tts")
    suspend fun tts(@Body body: TtsBody): ResponseBody

    /** 사진 속 문화재 판별 (Claude 비전) */
    @POST("vision")
    suspend fun vision(@Body body: VisionBody): VisionResponse

    /** 외부 RAG 서버 (historydam backend: POST /v1/chat {figureId, question}) */
    @POST
    suspend fun external(@Url url: String, @Body body: ExternalBody): ExternalResponse
}

@Serializable
data class TalkLine(val mine: Boolean, val text: String, val system: Boolean? = null)

@Serializable
data class TalkBody(
    val figureId: String,
    val siteId: String? = null,
    val lines: List<TalkLine>,
    val boundary: Boolean? = null,
)

@Serializable
data class SourceDto(val id: String? = null, val name: String = "", val quote: String = "")

@Serializable
data class TalkResponse(
    val text: String = "",
    val sources: List<SourceDto> = emptyList(),
    val retrieved: List<String> = emptyList(),
    val error: String? = null,
)

@Serializable
data class TtsBody(val figureId: String, val text: String)

@Serializable
data class VisionCandidate(val id: String, val name: String, val kind: String, val city: String? = null)

@Serializable
data class VisionBody(val image: String, val candidates: List<VisionCandidate>)

@Serializable
data class VisionResponse(
    val matchedId: String? = null,
    val name: String = "",
    val kind: String = "",
    val era: String = "",
    val description: String = "",
    val confidence: Int = 0,
    val isHeritage: Boolean = false,
    val category: String = "relic",
)

@Serializable
data class ExternalBody(val figureId: String, val question: String, val figureName: String? = null)

@Serializable
data class ExternalResponse(
    val answer: String = "",
    @SerialName("referenced_data") val referencedData: List<String> = emptyList(),
)
