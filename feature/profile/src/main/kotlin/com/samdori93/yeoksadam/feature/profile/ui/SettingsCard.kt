package com.samdori93.yeoksadam.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.domain.model.ChatMode
import com.samdori93.yeoksadam.core.domain.model.ChatSettings
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 설정 — 인물 대화 방식 · 지식 경계 필터 · 대화 기록 (웹앱 메뉴 설정과 같음). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {
    val settings: StateFlow<ChatSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatSettings())
    val roomCount: StateFlow<Int> = chatRepository.observeRoomCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun update(transform: (ChatSettings) -> ChatSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun clearAll() {
        viewModelScope.launch { chatRepository.clearHistory(null) }
    }
}

private val MODES = listOf(
    Triple(ChatMode.BASIC, "기본", "인물에 연결된 유적 자료를 통째로 참고해 답해요."),
    Triple(ChatMode.RAG, "RAG 서버 (역사담 내장)", "질문마다 경기도 유적 1,486곳 자료에서 관련 내용을 검색해 답하고, 근거 유적을 보여 줘요."),
    Triple(ChatMode.EXTERNAL, "외부 RAG 서버 (historydam backend)", "직접 운영하는 RAG 서버에 질문을 보내요. POST /v1/chat {figureId, question}"),
)

@Composable
fun SettingsCard(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val rooms by viewModel.roomCount.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable(s.externalUrl) { mutableStateOf(s.externalUrl) }
    val modeLabel = MODES.first { it.first == s.mode }.second

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DancheongColors.HanjiCard, RoundedCornerShape(16.dp))
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚙ 설정 · 인물 대화 방식: ", fontSize = 13.sp, color = DancheongColors.MeokSoft)
            Text(modeLabel, fontSize = 13.sp, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, color = DancheongColors.Jujak)
            Spacer(Modifier.weight(1f))
            Text(if (open) "▲" else "▼", fontSize = 11.sp, color = DancheongColors.MeokSoft)
        }
        if (!open) return@Column

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            MODES.forEach { (mode, name, desc) ->
                Option(
                    selected = s.mode == mode,
                    title = name,
                    desc = desc,
                    radio = true,
                    onClick = { viewModel.update { it.copy(mode = mode) } },
                )
            }
            if (s.mode == ChatMode.EXTERNAL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DancheongColors.Hanji, RoundedCornerShape(10.dp))
                        .border(1.dp, DancheongColors.Meok.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            viewModel.update { cur -> cur.copy(externalUrl = it) }
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = DancheongColors.Meok),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (url.isEmpty()) Text("https://내-rag-서버.example.com", fontSize = 13.sp, color = DancheongColors.MeokSoft)
                            inner()
                        },
                    )
                }
            }
            Option(
                selected = s.boundary,
                title = "지식 경계 필터",
                desc = "인물은 세상을 떠난 해 이후의 일을 모른다고 답해요. RAG 서버 방식에서는 사후의 기록을 검색에서 빼요. (해설사는 해당 없음)",
                radio = false,
                onClick = { viewModel.update { it.copy(boundary = !it.boundary) } },
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    if (rooms > 0) "대화 기록: 인물 ${rooms}명과의 대화가 저장됨" else "대화 기록: 저장된 대화 없음",
                    fontSize = 12.sp,
                    color = DancheongColors.MeokSoft,
                    modifier = Modifier.weight(1f),
                )
                if (rooms > 0) {
                    Text(
                        "모두 지우기",
                        fontSize = 12.sp,
                        color = DancheongColors.Jujak,
                        modifier = Modifier
                            .border(1.dp, DancheongColors.Jujak.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { confirmClear = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text("모든 인물과의 대화 기록을 지울까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    confirmClear = false
                }) { Text("지우기", color = DancheongColors.Jujak) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun Option(selected: Boolean, title: String, desc: String, radio: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DancheongColors.Hanji, RoundedCornerShape(12.dp))
            .border(1.dp, DancheongColors.Meok.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (radio) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = DancheongColors.Jujak))
        } else {
            Checkbox(checked = selected, onCheckedChange = { onClick() }, colors = CheckboxDefaults.colors(checkedColor = DancheongColors.Jujak))
        }
        Spacer(Modifier.width(2.dp))
        Column(Modifier.padding(top = 10.dp, end = 6.dp)) {
            Text(title, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DancheongColors.Meok)
            Text(desc, fontSize = 11.5.sp, lineHeight = 16.sp, color = DancheongColors.MeokSoft)
        }
    }
}
