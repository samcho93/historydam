package com.samdori93.yeoksadam.feature.voice.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.voice.ui.VoiceRoute
import kotlinx.serialization.Serializable

/** 초상화 음성 대화 라우트 (STT/TTS + 실시간 자막, CLAUDE.md §11). */
@Serializable
data class Voice(val figureId: String, val sessionId: String? = null)

fun NavController.navigateToVoice(figureId: String, navOptions: NavOptions? = null) =
    navigate(Voice(figureId), navOptions)

fun NavGraphBuilder.voiceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<Voice> {
        VoiceRoute(
            onBack = onBack,
            modifier = modifier,
        )
    }
}
