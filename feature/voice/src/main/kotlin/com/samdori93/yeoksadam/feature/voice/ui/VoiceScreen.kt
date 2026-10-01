package com.samdori93.yeoksadam.feature.voice.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.ui.ar.CameraBackground
import com.samdori93.yeoksadam.core.ui.ar.angleDiff
import com.samdori93.yeoksadam.core.ui.ar.rememberCameraOrientation
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceStatus
import com.samdori93.yeoksadam.feature.voice.viewmodel.VoiceUiState
import kotlin.math.abs

/** 세로 모드 후면 카메라의 대략적인 가로 시야각 */
private const val H_FOV = 55f

private val CaptionShadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 8f)

/**
 * 인물과 마주 보는 AR 대화 (웹앱 talk 화면).
 * 후면 카메라 위에 배경을 걷어낸 인물이 서 있고, 대화는 라이브 방송 자막처럼 흐른다.
 * 인물은 대화를 시작한 방위에 고정 — 폰을 돌리면 화면 밖으로 밀려나고 ⟲ 로 정면에 다시 부른다.
 */
@Composable
fun VoiceScreen(
    state: VoiceUiState,
    hasCamera: Boolean,
    onMic: () -> Unit,
    onType: (String) -> Unit,
    onMute: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val orientation by rememberCameraOrientation()
    var anchor by remember { mutableFloatStateOf(Float.NaN) }
    if (anchor.isNaN() && orientation.available) anchor = orientation.heading
    var typing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val figure = state.figure?.figure

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFF1A140F))) {
        if (hasCamera) CameraBackground(Modifier.fillMaxSize())

        // 인물 — 대화를 시작한 방위에 고정
        val offsetDeg = if (anchor.isNaN() || !orientation.available) 0f else angleDiff(orientation.heading, anchor)
        val pxPerDeg = maxWidth.value / H_FOV
        val x by animateFloatAsState(offsetDeg * pxPerDeg, label = "figure-x")
        val outOfView = abs(offsetDeg) > H_FOV / 2 + 8
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = x.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.68f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FigureCutout(figure, speaking = state.status == VoiceStatus.SPEAKING)
        }

        // 위쪽 그림자 + 상단바
        Box(
            Modifier.fillMaxWidth().height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
        )
        Row(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.Filled.Home, "뒤로", onBack)
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("● LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(DancheongColors.Jujak, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                Text(figure?.name ?: "…", color = Color.White, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(figure?.title.orEmpty(), color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                state.modeBadge?.let {
                    Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(DancheongColors.Cheongnok, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            RoundIcon(if (state.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "음성 끄기", onMute)
        }

        // 화면 밖으로 밀려났을 때 안내
        if (outOfView && figure != null) {
            Text(
                if (offsetDeg < 0) "◀ ${figure.name}은(는) 이쪽에" else "${figure.name}은(는) 이쪽에 ▶",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(if (offsetDeg < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        // 자막 (라이브 방송형 — 배경 없이 그림자만)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .navigationBarsPadding()
                // 글 입력 키보드가 올라오면 자막·입력창을 키보드 위로
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Captions(state, figure?.name ?: "")
            state.notice?.let {
                Text(it, color = DancheongColors.HwangtoLight, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp).fillMaxWidth(), textAlign = TextAlign.Center)
            }
            if (state.resumed && state.status == VoiceStatus.IDLE) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("↑ 지난 대화를 이어 갑니다  ", color = Color.White.copy(alpha = 0.8f), fontSize = 11.5.sp)
                    Text("새로 시작", color = Color.White, fontSize = 11.5.sp,
                        modifier = Modifier.border(1.dp, DancheongColors.HwangtoLight, RoundedCornerShape(12.dp)).clickable(onClick = onReset).padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
            Text(
                statusText(state.status, state.conversing),
                color = DancheongColors.HwangtoLight,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
            if (typing) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(24.dp)).padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = DancheongColors.Meok),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onType(draft); draft = "" }),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) Text("말 대신 글로 여쭈어 보세요…", color = DancheongColors.MeokSoft, fontSize = 15.sp)
                            inner()
                        },
                    )
                    Box(
                        Modifier.padding(4.dp).size(40.dp).background(DancheongColors.Jujak, CircleShape)
                            .clickable { onType(draft); draft = "" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "보내기", tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(Icons.Filled.Keyboard, "글자로 입력") { typing = !typing }
                val active = state.conversing || state.status == VoiceStatus.LISTENING
                Box(
                    modifier = Modifier.size(72.dp).background(if (active) DancheongColors.JujakDeep else DancheongColors.Jujak, CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape).clickable(onClick = onMic),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (active) Icons.Filled.Stop else Icons.Filled.Mic, "대화", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                // 인물을 정면으로 다시 부르기
                RoundIcon(Icons.Filled.Refresh, "정면으로 부르기") { if (orientation.available) anchor = orientation.heading }
            }
        }
    }
}

@Composable
private fun Captions(state: VoiceUiState, figureName: String) {
    // 최근 줄 몇 개 + 말하는 중인 줄은 음성에 맞춰 한 글자씩
    val shown = state.lines.takeLast(4)
    shown.forEach { line ->
        val speaking = state.speakingText != null && line == shown.last() && line.role == Role.FIGURE && line.text == state.speakingText
        val text = if (speaking) line.text.take(state.revealed) else line.text
        CaptionLine(line, text, figureName)
    }
    // 서버 답이 아직 기록에 오르기 전에 말하는 첫인사·답
    if (state.speakingText != null && shown.none { it.text == state.speakingText }) {
        CaptionLine(ChatMessage(Role.FIGURE, state.speakingText), state.speakingText.take(state.revealed), figureName)
    }
    if (state.partial.isNotBlank()) CaptionLine(ChatMessage(Role.USER, state.partial), state.partial + "…", figureName, draft = true)
}

@Composable
private fun CaptionLine(line: ChatMessage, text: String, figureName: String, draft: Boolean = false) {
    val mine = line.role == Role.USER
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).alpha(if (draft) 0.75f else 1f),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Text(
            if (mine) "나" else figureName,
            color = if (mine) Color(0xFFF0D6A0) else DancheongColors.HwangtoLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = CaptionShadow),
        )
        Text(
            text,
            color = Color.White,
            fontSize = if (mine) 14.sp else 16.sp,
            lineHeight = if (mine) 20.sp else 23.sp,
            fontFamily = if (mine) null else NanumMyeongjo,
            textAlign = if (mine) TextAlign.End else TextAlign.Start,
            style = TextStyle(shadow = CaptionShadow),
            modifier = Modifier.widthIn(max = 330.dp),
        )
        if (!mine && line.citations.isNotEmpty()) {
            Text(
                "📚 근거  " + line.citations.joinToString(" · ") { if (it.refId != null) it.source else "\"${it.excerpt}\"" },
                color = Color(0xFFF3E2BD),
                fontSize = 11.sp,
                style = TextStyle(shadow = CaptionShadow),
            )
        }
    }
}

/** 배경을 걷어낸 인물 (없으면 한자 인장) */
@Composable
private fun FigureCutout(figure: Figure?, speaking: Boolean) {
    val bob by animateFloatAsState(if (speaking) 1.015f else 1f, label = "speak")
    if (figure?.cutoutUrl != null) {
        AsyncImage(
            model = figure.cutoutUrl,
            contentDescription = figure.name,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = bob; scaleY = bob },
        )
    } else {
        Column(
            modifier = Modifier.padding(bottom = 220.dp)
                .background(Color(0xCCF4EAD9), RoundedCornerShape(18.dp))
                .border(3.dp, DancheongColors.Hwangto, RoundedCornerShape(18.dp))
                .padding(horizontal = 26.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(figure?.hanja?.ifBlank { null } ?: figure?.seal ?: "", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 44.sp, color = DancheongColors.Jujak)
            Text(figure?.name.orEmpty(), fontFamily = NanumMyeongjo, fontSize = 15.sp, color = DancheongColors.Meok)
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, cd, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

private fun statusText(s: VoiceStatus, conversing: Boolean) = when (s) {
    VoiceStatus.IDLE -> if (conversing) "잠시 뒤 다시 들을게요" else "마이크를 눌러 말을 걸어 보세요"
    VoiceStatus.LISTENING -> "듣고 있어요…"
    VoiceStatus.THINKING -> "생각하는 중…"
    VoiceStatus.SPEAKING -> if (conversing) "말하는 중… 끝나면 다시 들을게요" else "말하는 중…"
}
