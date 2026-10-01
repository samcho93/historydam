package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.database.dao.ChatDao
import com.samdori93.yeoksadam.core.database.entity.ChatMessageEntity
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.ChatMode
import com.samdori93.yeoksadam.core.domain.model.Citation
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import com.samdori93.yeoksadam.core.network.worker.ExternalBody
import com.samdori93.yeoksadam.core.network.worker.TalkBody
import com.samdori93.yeoksadam.core.network.worker.TalkLine
import com.samdori93.yeoksadam.core.network.worker.TtsBody
import com.samdori93.yeoksadam.core.network.worker.WorkerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * 인물 대화 — 역사담 웹앱과 같은 서버·규칙.
 *  - 기본: Worker /chat (인물에 연결된 유적 설명을 근거로)
 *  - RAG 서버: Worker /rag (질문마다 유적 1,486곳 자료 검색, 근거 유적 표시)
 *  - 외부 RAG: historydam backend /v1/chat (없으면 예전 /chat)
 * 질문·답은 Room 에 저장해 다시 들어와도 이어진다(대화 기록 보존). 오류 안내는 저장하지 않는다.
 */
class ChatRepositoryImpl @Inject constructor(
    private val worker: WorkerApi,
    private val chatDao: ChatDao,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
) : ChatRepository {

    @Serializable
    private data class StoredCitation(val source: String, val excerpt: String, val refId: String? = null)

    override fun sendMessage(figureId: String, message: String): Flow<ChatMessage> = flow {
        chatDao.insertMessage(ChatMessageEntity(figureId = figureId, role = Role.USER.name, text = message))
        val settings = settingsRepository.settings.first()
        val reply = try {
            when (settings.mode) {
                ChatMode.EXTERNAL -> external(settings.externalUrl, figureId, message)
                else -> {
                    val lines = chatDao.recent(figureId, HISTORY_LIMIT)
                        .map { TalkLine(mine = it.role == Role.USER.name, text = it.text) }
                    val body = TalkBody(figureId = figureId, lines = lines, boundary = settings.boundary)
                    val r = if (settings.mode == ChatMode.RAG) worker.rag(body) else worker.chat(body)
                    ChatMessage(
                        role = Role.FIGURE,
                        text = r.text.ifBlank { "…" },
                        citations = r.sources.map { Citation(source = it.name, excerpt = it.quote, refId = it.id) },
                    )
                }
            }
        } catch (e: HttpException) {
            systemMessage(serverError(e))
        } catch (e: SocketTimeoutException) {
            systemMessage("답이 너무 늦어요. 잠시 뒤 다시 물어봐 주세요.")
        } catch (e: IOException) {
            systemMessage("네트워크 연결을 확인해 주세요.")
        } catch (e: IllegalStateException) {
            systemMessage(e.message ?: "대답을 받지 못했습니다.")
        }
        if (!reply.system) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    figureId = figureId,
                    role = Role.FIGURE.name,
                    text = reply.text,
                    citationsJson = if (reply.citations.isEmpty()) "" else json.encodeToString(
                        reply.citations.map { StoredCitation(it.source, it.excerpt, it.refId) },
                    ),
                ),
            )
        }
        emit(reply)
    }

    /** 외부 RAG 서버 — historydam 새 주소 /v1/chat 을 먼저, 없으면(404·405) 예전 /chat */
    private suspend fun external(base: String, figureId: String, question: String): ChatMessage {
        check(base.startsWith("https://") || base.startsWith("http://")) { "메뉴 설정에서 외부 RAG 서버 주소를 입력해 주세요." }
        val body = ExternalBody(figureId = figureId, question = question)
        val r = try {
            worker.external("$base/v1/chat", body)
        } catch (e: HttpException) {
            if (e.code() == 404 || e.code() == 405) worker.external("$base/chat", body) else throw e
        }
        check(r.answer.isNotBlank()) { "외부 RAG 서버가 답하지 않았어요." }
        return ChatMessage(
            role = Role.FIGURE,
            text = r.answer,
            citations = r.referencedData.map { Citation(source = "참고 자료", excerpt = it.take(90)) },
        )
    }

    override fun getChatHistory(figureId: String): Flow<List<ChatMessage>> =
        chatDao.getMessagesByFigureId(figureId).map { entities ->
            entities.map { e ->
                ChatMessage(
                    role = Role.valueOf(e.role),
                    text = e.text,
                    citations = if (e.citationsJson.isBlank()) {
                        emptyList()
                    } else {
                        runCatching { json.decodeFromString<List<StoredCitation>>(e.citationsJson) }
                            .getOrDefault(emptyList())
                            .map { Citation(it.source, it.excerpt, it.refId) }
                    },
                )
            }
        }

    override suspend fun clearHistory(figureId: String?) {
        if (figureId == null) chatDao.clearAll() else chatDao.clearHistory(figureId)
    }

    override fun observeRoomCount(): Flow<Int> = chatDao.roomCount()

    override suspend fun speech(figureId: String, text: String): Result<ByteArray> = try {
        Result.Success(worker.tts(TtsBody(figureId, text.take(TTS_MAX))).bytes())
    } catch (e: HttpException) {
        Result.Failure(AppError.Server(e.code(), e.message()))
    } catch (e: IOException) {
        Result.Failure(AppError.Network(e))
    }

    private fun systemMessage(text: String) = ChatMessage(role = Role.FIGURE, text = text, system = true)

    /** 서버 오류 본문 {error} 를 그대로 보여 준다 */
    private fun serverError(e: HttpException): String {
        val msg = runCatching {
            json.parseToJsonElement(e.response()?.errorBody()?.string().orEmpty()).jsonObject["error"]?.jsonPrimitive?.content
        }.getOrNull()
        return msg ?: "대답을 받지 못했습니다. (${e.code()})"
    }

    private companion object {
        /** 서버에 보낼 최근 대화 줄 수 (웹앱과 같음) */
        const val HISTORY_LIMIT = 20
        const val TTS_MAX = 600
    }
}
