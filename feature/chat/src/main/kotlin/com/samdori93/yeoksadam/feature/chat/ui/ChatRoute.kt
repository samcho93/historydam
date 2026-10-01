package com.samdori93.yeoksadam.feature.chat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samdori93.yeoksadam.feature.chat.viewmodel.ChatViewModel

@Composable
fun ChatRoute(
    figureId: String,
    onBack: () -> Unit,
    onSwitchToVoice: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        figureId = figureId,
        state = state,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onBack = onBack,
        onSwitchToVoice = onSwitchToVoice,
        modifier = modifier,
        onReset = viewModel::onReset,
    )
}
