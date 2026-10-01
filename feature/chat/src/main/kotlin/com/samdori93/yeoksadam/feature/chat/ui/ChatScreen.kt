package com.samdori93.yeoksadam.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.feature.chat.viewmodel.ChatUiState

/** 챗봇 텍스트 대화 (실제 AI 및 RAG 연동) */
@Composable
fun ChatScreen(
    figureId: String,
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onSwitchToVoice: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReset: () -> Unit = {},
) {
    val figure = state.figure?.figure
    val name = figure?.name ?: "…"
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding(),
    ) {
        // chat head
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = DancheongColors.Meok,
                modifier = Modifier.size(30.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(8.dp))
            MedallionPortrait(
                portraitUrl = figure?.portraitUrl?.ifBlank { null },
                name = name,
                sealMark = figure?.seal?.ifBlank { null },
                size = 38.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
                    state.modeBadge?.let { badge ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(DancheongColors.Cheongnok, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    if (state.responding) "● 답을 생각하는 중…" else "● 대화 중 · ${figure?.title.orEmpty()}",
                    fontSize = 11.sp,
                    color = DancheongColors.Cheongnok,
                )
            }
            if (state.messages.size > 1) {
                Text(
                    "새로 시작",
                    fontSize = 12.sp,
                    color = DancheongColors.Jujak,
                    modifier = Modifier
                        .border(1.dp, DancheongColors.Jujak.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onReset)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // body
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "오늘",
                        fontSize = 11.sp,
                        color = DancheongColors.MeokSoft,
                        modifier = Modifier
                            .background(DancheongColors.HanjiDim, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 3.dp),
                    )
                }
            }
            items2(state.messages)
            if (state.responding) {
                item {
                    Bubble(message = ChatMessage(role = Role.FIGURE, text = "…"), them = true)
                }
            }
            state.notice?.let { notice ->
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(notice, fontSize = 12.sp, color = DancheongColors.Jujak)
                    }
                }
            }
        }

        // input
        Column(
            modifier = Modifier.background(DancheongColors.HanjiCard).navigationBarsPadding().padding(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .background(DancheongColors.Cheongnok.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .clickable { onSwitchToVoice(figureId) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.SwapHoriz, null, tint = DancheongColors.CheongnokDeep, modifier = Modifier.size(14.dp))
                Text("초상화 대화로 전환", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DancheongColors.CheongnokDeep)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                    placeholder = { Text("메시지를 입력하세요…", color = DancheongColors.MeokSoft, fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = DancheongColors.Meok),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DancheongColors.Hanji,
                        unfocusedContainerColor = DancheongColors.Hanji,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = DancheongColors.Jujak,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DancheongColors.Jujak, RoundedCornerShape(24.dp))
                        .clickable(onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "전송", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items2(messages: List<ChatMessage>) {
    items(messages.size) { i ->
        val m = messages[i]
        val them = m.role == Role.FIGURE
        Bubble(message = m, them = them)
    }
}

@Composable
private fun Bubble(message: ChatMessage, them: Boolean) {
    val shape = if (them) {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (them) Arrangement.Start else Arrangement.End,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(if (them) DancheongColors.HanjiCard else DancheongColors.Jujak, shape)
                .border(
                    width = if (them) 1.dp else 0.dp,
                    color = if (them) DancheongColors.Meok.copy(alpha = 0.08f) else Color.Transparent,
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (them) DancheongColors.Meok else Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            // RAG 사료 근거가 존재할 경우 말풍선 하단에 단청 스탈로 표시
            if (message.citations.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(
                    color = if (them) DancheongColors.Meok.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(4.dp))
                // RAG 서버: 답변이 실제로 인용한 유적 / 외부 RAG: 참고 문장
                Text(
                    text = "📚 근거  " + message.citations.joinToString("  ·  ") { c ->
                        if (c.refId != null) c.source else "\"${c.excerpt}\""
                    },
                    fontSize = 11.sp,
                    color = if (them) DancheongColors.CheongnokDeep else Color.White.copy(alpha = 0.85f),
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Preview(heightDp = 760)
@Composable
private fun ChatPreview() {
    YeoksadamTheme {
        ChatScreen(
            figureId = "chae-jegong",
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(role = Role.FIGURE, text = "어서 오시게."),
                    ChatMessage(role = Role.USER, text = "반갑습니다.")
                )
            ),
            onInputChange = {},
            onSend = {},
            onBack = {},
            onSwitchToVoice = {},
        )
    }
}