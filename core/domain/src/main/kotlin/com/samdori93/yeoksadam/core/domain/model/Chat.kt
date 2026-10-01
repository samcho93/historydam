package com.samdori93.yeoksadam.core.domain.model

/** 인물 대화 메시지. */
data class ChatMessage(
    val role: Role,
    val text: String,
    val citations: List<Citation> = emptyList(),
    /** 인물 발화가 아닌 앱 안내(오류 등) — 저장·서버 전송에서 제외 */
    val system: Boolean = false,
)

enum class Role { USER, FIGURE }

/**
 * RAG 응답의 사료 근거.
 * @param refId 근거 유적 id (인물 소개는 "fig:<id>", 외부 서버 자료는 null)
 */
data class Citation(
    val source: String,
    val excerpt: String,
    val refId: String? = null,
)

/**
 * 인물 대화 방식 (역사담 웹앱과 같음)
 * - BASIC: 서버 /chat — 인물에 연결된 유적 설명을 통째로 근거로
 * - RAG: 서버 /rag — 질문마다 유적 1,486곳 자료를 검색해 근거로, 출처 표시
 * - EXTERNAL: 외부 RAG 서버(historydam backend) /v1/chat
 */
enum class ChatMode { BASIC, RAG, EXTERNAL }

/** 대화·표시 설정 (DataStore). */
data class ChatSettings(
    val mode: ChatMode = ChatMode.BASIC,
    /** 외부 RAG 서버 주소 (EXTERNAL 일 때) */
    val externalUrl: String = "",
    /** 지식 경계 필터: 인물이 세상을 떠난 뒤의 일은 모른다 */
    val boundary: Boolean = true,
    /** 지도·AR 표시 반경(m): 1000 · 3000 · 5000 · 10000 */
    val radiusM: Int = 10_000,
)

// Discovery / DiscoveryType 는 Discovery.kt 로 이동(이미지·명칭 포함 확장판).

/** 위치(위경도) 값 객체. */
data class LatLng(
    val lat: Double,
    val lng: Double,
)
