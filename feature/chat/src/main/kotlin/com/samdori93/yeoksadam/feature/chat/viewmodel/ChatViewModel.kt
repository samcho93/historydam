package com.samdori93.yeoksadam.feature.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.ChatMode
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import com.samdori93.yeoksadam.core.domain.usecase.GetFigureWithSiteUseCase
import com.samdori93.yeoksadam.feature.chat.navigation.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val responding: Boolean = false,
    val figure: NearbyFigure? = null,
    /** 대화 방식 표시 (RAG · 외부 RAG), 기본이면 null */
    val modeBadge: String? = null,
    /** 저장하지 않는 안내 (오류 등) */
    val notice: String? = null,
)

/**
 * 인물 텍스트 대화. 기록은 Room 에 저장되어 다시 들어와도 이어진다(대화 기록 보존).
 * 기록이 없으면 인물의 첫인사를 보여 준다(저장하지 않음 — 서버도 첫 user 메시지부터 받는다).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    getFigure: GetFigureWithSiteUseCase,
) : ViewModel() {

    private val chatArgs = savedStateHandle.toRoute<Chat>()
    val figureId: String = chatArgs.figureId

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val nf = getFigure(figureId)
            _uiState.update { it.copy(figure = nf, messages = withGreeting(it.messages, nf)) }
        }
        viewModelScope.launch {
            chatRepository.getChatHistory(figureId).collect { history ->
                _uiState.update { it.copy(messages = withGreeting(history, it.figure)) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                val badge = when (s.mode) {
                    ChatMode.RAG -> "RAG"
                    ChatMode.EXTERNAL -> "외부 RAG"
                    ChatMode.BASIC -> null
                }
                _uiState.update { it.copy(modeBadge = badge) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onSend() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.responding) return
        _uiState.update { it.copy(input = "", responding = true, notice = null) }
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(figureId, text).collect { reply ->
                    if (reply.system) _uiState.update { it.copy(notice = reply.text) }
                }
            } finally {
                _uiState.update { it.copy(responding = false) }
            }
        }
    }

    /** 이 인물과 새로 시작 — 저장된 대화를 지운다 */
    fun onReset() {
        viewModelScope.launch { chatRepository.clearHistory(figureId) }
    }

    private fun withGreeting(history: List<ChatMessage>, nf: NearbyFigure?): List<ChatMessage> = when {
        history.isNotEmpty() -> history
        nf != null -> listOf(ChatMessage(Role.FIGURE, greeting(nf)))
        else -> emptyList()
    }

    /** 웹앱과 같은 첫인사 (말씨별) */
    private fun greeting(nf: NearbyFigure?): String {
        if (nf == null) return ""
        val site = nf.site.name
        return when (nf.figure.style) {
            "lady" -> "어서 오세요. ${site}에서 뵙게 되어 반갑습니다."
            "king" -> "그대가 ${josa(site, "을", "를")} 찾아왔구나. 무엇이 궁금한고?"
            else -> "어서 오시게. ${site}에는 들러보셨는가?"
        }
    }

    /** 받침에 맞는 조사 (「을(를)」처럼 괄호로 쓰지 않는다) */
    private fun josa(word: String, withBatchim: String, without: String): String {
        val last = word.lastOrNull { it in '가'..'힣' } ?: return word + without
        return word + if ((last - '가') % 28 != 0) withBatchim else without
    }
}
