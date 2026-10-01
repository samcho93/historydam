package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * 인물에게 메시지를 보내고 답을 받는다. 질문·답은 대화 기록(Room)에 저장된다.
     * 설정의 대화 방식(기본 · RAG 서버 · 외부 RAG)과 지식 경계를 따른다.
     */
    fun sendMessage(figureId: String, message: String): Flow<ChatMessage>

    /** 인물과의 대화 기록 (오래된 순). */
    fun getChatHistory(figureId: String): Flow<List<ChatMessage>>

    /** 대화 기록 지우기 — figureId 가 null 이면 전부. */
    suspend fun clearHistory(figureId: String?)

    /** 대화 기록이 있는 인물 수. */
    fun observeRoomCount(): Flow<Int>

    /** 인물 목소리 (서버 Azure 신경망 음성, MP3). 실패하면 기기 음성으로 대체한다. */
    suspend fun speech(figureId: String, text: String): Result<ByteArray>
}
