package com.samdori93.yeoksadam.feature.voice.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.ChatMode
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import com.samdori93.yeoksadam.core.domain.usecase.GetFigureWithSiteUseCase
import com.samdori93.yeoksadam.feature.voice.navigation.Voice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class VoiceStatus { IDLE, LISTENING, THINKING, SPEAKING }

data class VoiceUiState(
    val figure: NearbyFigure? = null,
    /** 대화 기록 (텍스트 대화와 공유) — 기록이 없으면 첫인사 한 줄 */
    val lines: List<ChatMessage> = emptyList(),
    val status: VoiceStatus = VoiceStatus.IDLE,
    /** 듣는 중인 내 말 (실시간 자막) */
    val partial: String = "",
    /** 말하는 중인 줄과 지금까지 보여 줄 글자 수 (음성에 맞춰 한 글자씩) */
    val speakingText: String? = null,
    val revealed: Int = 0,
    /** 연속 대화: 답을 마치면 다시 듣기 */
    val conversing: Boolean = false,
    val muted: Boolean = false,
    val modeBadge: String? = null,
    /** 저장하지 않는 안내 (오류·권한) */
    val notice: String? = null,
    /** 이어 온 대화면 마지막으로 이야기한 기록이 있다는 표시 */
    val resumed: Boolean = false,
)

/**
 * 인물과 마주 보는 AR 대화 (웹앱 talk 화면과 같은 흐름).
 * 마이크 → 음성 인식(실시간 자막) → 서버 답변 → 인물 목소리(서버 Azure, 실패 시 기기 TTS) + 자막 → 다시 듣기.
 * 대화는 텍스트 대화와 같은 기록(Room)에 저장된다.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    getFigure: GetFigureWithSiteUseCase,
) : ViewModel() {

    val figureId: String = savedStateHandle.toRoute<Voice>().figureId

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var player: MediaPlayer? = null
    private var speakJob: Job? = null
    /** 이번에 처음 만나 건넨 첫인사 (저장하지 않고 맨 위에 둔다) */
    private var hello: ChatMessage? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                ttsReady = true
            }
        }
        viewModelScope.launch {
            val nf = getFigure(figureId)
            _uiState.update { it.copy(figure = nf) }
            val history = chatRepository.getChatHistory(figureId).first()
            if (history.isEmpty() && nf != null) {
                // 처음 만났으면 첫인사를 목소리로
                val h = ChatMessage(Role.FIGURE, greeting(nf))
                hello = h
                _uiState.update { it.copy(lines = listOf(h)) }
                speak(h.text)
            } else {
                _uiState.update { it.copy(resumed = history.isNotEmpty()) }
            }
            chatRepository.getChatHistory(figureId).collect { list ->
                _uiState.update { it.copy(lines = listOfNotNull(hello) + list) }
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

    /** 마이크: 연속 대화 시작 / 멈춤 */
    fun onMic() {
        val s = _uiState.value
        if (s.conversing || s.status == VoiceStatus.LISTENING) {
            _uiState.update { it.copy(conversing = false) }
            recognizer?.cancel()
            stopSpeaking()
            _uiState.update { it.copy(status = VoiceStatus.IDLE, partial = "") }
            return
        }
        _uiState.update { it.copy(conversing = true, notice = null) }
        if (s.status == VoiceStatus.IDLE || s.status == VoiceStatus.SPEAKING) {
            stopSpeaking()
            startListening()
        }
    }

    /** 글로 묻기 */
    fun onType(text: String) {
        val q = text.trim()
        if (q.isEmpty() || _uiState.value.status == VoiceStatus.THINKING) return
        stopSpeaking()
        ask(q)
    }

    fun onMute() {
        val muted = !_uiState.value.muted
        _uiState.update { it.copy(muted = muted) }
        if (muted) stopSpeaking()
    }

    /** 이 인물과 새로 시작 */
    fun onReset() {
        viewModelScope.launch {
            stopSpeaking()
            chatRepository.clearHistory(figureId)
            val nf = _uiState.value.figure ?: return@launch
            val h = ChatMessage(Role.FIGURE, greeting(nf))
            hello = h
            _uiState.update { it.copy(lines = listOf(h), resumed = false) }
            speak(h.text)
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(conversing = false, notice = "마이크 권한이 없어요. ⌨ 버튼으로 글을 입력해 주세요.") }
    }

    private fun ask(question: String) {
        _uiState.update { it.copy(status = VoiceStatus.THINKING, partial = "", notice = null) }
        viewModelScope.launch {
            var reply: ChatMessage? = null
            chatRepository.sendMessage(figureId, question).collect { reply = it }
            val r = reply ?: return@launch
            if (r.system) {
                _uiState.update { it.copy(status = VoiceStatus.IDLE, notice = r.text, conversing = false) }
                return@launch
            }
            speak(r.text)
        }
    }

    private fun speak(text: String) {
        speakJob?.cancel()
        speakJob = viewModelScope.launch {
            _uiState.update { it.copy(status = VoiceStatus.SPEAKING, speakingText = text, revealed = 0) }
            if (_uiState.value.muted) {
                revealByTime(text, (text.length / CHARS_PER_SEC * 1000).toLong())
            } else {
                val played = playServerVoice(text)
                if (!played) speakWithDevice(text)
            }
            _uiState.update { it.copy(status = VoiceStatus.IDLE, speakingText = null) }
            if (_uiState.value.conversing) {
                delay(600)
                if (_uiState.value.conversing && _uiState.value.status == VoiceStatus.IDLE) startListening()
            }
        }
    }

    /** 서버 신경망 음성 — 재생 위치에 맞춰 자막을 드러낸다 */
    private suspend fun playServerVoice(text: String): Boolean {
        val bytes = (chatRepository.speech(figureId, text) as? Result.Success)?.data ?: return false
        val file = File(context.cacheDir, "voice-${UUID.randomUUID()}.mp3").apply { writeBytes(bytes) }
        val done = CompletableDeferred<Unit>()
        val mp = try {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(file.absolutePath)
                setOnCompletionListener { done.complete(Unit) }
                setOnErrorListener { _, _, _ ->
                    done.complete(Unit)
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            file.delete()
            return false
        }
        player = mp
        try {
            while (!done.isCompleted) {
                val dur = mp.duration.coerceAtLeast(1)
                val pos = runCatching { mp.currentPosition }.getOrDefault(0)
                _uiState.update { it.copy(revealed = (text.length * pos / dur.toFloat() * 1.08f).toInt().coerceAtMost(text.length)) }
                delay(80)
            }
        } finally {
            runCatching { mp.release() }
            player = null
            file.delete()
        }
        _uiState.update { it.copy(revealed = text.length) }
        return true
    }

    /** 기기 음성 (서버 음성을 못 받았을 때) */
    private suspend fun speakWithDevice(text: String) {
        val engine = tts
        if (engine == null || !ttsReady) {
            revealByTime(text, (text.length / CHARS_PER_SEC * 1000).toLong())
            return
        }
        val done = CompletableDeferred<Unit>()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                done.complete(Unit)
            }

            @Deprecated("deprecated")
            override fun onError(utteranceId: String?) {
                done.complete(Unit)
            }
        })
        engine.setPitch(if (_uiState.value.figure?.figure?.style == "lady") 1.05f else 0.85f)
        engine.setSpeechRate(0.95f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
        val start = System.currentTimeMillis()
        while (!done.isCompleted) {
            val n = ((System.currentTimeMillis() - start) / 1000f * CHARS_PER_SEC).toInt()
            _uiState.update { it.copy(revealed = n.coerceAtMost(text.length)) }
            delay(80)
        }
        _uiState.update { it.copy(revealed = text.length) }
    }

    private suspend fun revealByTime(text: String, ms: Long) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < ms) {
            _uiState.update { it.copy(revealed = (text.length * (System.currentTimeMillis() - start) / ms.toFloat()).toInt()) }
            delay(80)
        }
        _uiState.update { it.copy(revealed = text.length) }
    }

    private fun stopSpeaking() {
        speakJob?.cancel()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        tts?.stop()
        _uiState.update { if (it.status == VoiceStatus.SPEAKING) it.copy(status = VoiceStatus.IDLE, speakingText = null) else it }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.update { it.copy(conversing = false, notice = "이 기기는 음성 인식을 지원하지 않아요. ⌨ 버튼으로 글을 입력해 주세요.") }
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(listener) }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        _uiState.update { it.copy(status = VoiceStatus.LISTENING, partial = "") }
        recognizer?.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            // 말이 없으면 연속 대화를 멈춘다
            _uiState.update { it.copy(status = VoiceStatus.IDLE, partial = "", conversing = false) }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) _uiState.update { it.copy(partial = text) }
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isBlank()) {
                _uiState.update { it.copy(status = VoiceStatus.IDLE, partial = "", conversing = false) }
                return
            }
            ask(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    /** 웹앱과 같은 첫인사 */
    private fun greeting(nf: NearbyFigure): String {
        val site = nf.site.name
        return when (nf.figure.style) {
            "guide" -> "어서 오세요. 저는 ${josa(site, "을", "를")} 안내하는 해설사입니다. 궁금한 것을 편하게 물어보세요."
            "lady" -> "어서 오세요. ${site}에서 뵙게 되어 반갑습니다."
            "king" -> "그대가 ${josa(site, "을", "를")} 찾아왔구나. 무엇이 궁금한고?"
            else -> "어서 오시게. ${site}에는 들러보셨는가?"
        }
    }

    private fun josa(word: String, withBatchim: String, without: String): String {
        val last = word.lastOrNull { it in '가'..'힣' } ?: return word + without
        return word + if ((last - '가') % 28 != 0) withBatchim else without
    }

    override fun onCleared() {
        stopSpeaking()
        recognizer?.destroy()
        tts?.shutdown()
    }

    private companion object {
        /** 한국어 음성 대략적인 속도(글자/초) — 기기 음성 자막용 */
        const val CHARS_PER_SEC = 7f
    }
}
