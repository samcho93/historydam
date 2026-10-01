package com.samdori93.yeoksadam.feature.figure.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.feature.figure.ui.AllFiguresScreen
import com.samdori93.yeoksadam.feature.figure.ui.FigureSheetScreen
import com.samdori93.yeoksadam.feature.figure.viewmodel.AllFiguresViewModel
import com.samdori93.yeoksadam.feature.figure.viewmodel.FigureSheetViewModel
import kotlinx.serialization.Serializable

/** 인물 선택 시트 / 상세 라우트 (figureId 전달). */
@Serializable
data class FigureSheet(val figureId: String)

/** 모든 인물(가나다·주변 필터) 라우트. */
@Serializable
data object AllFigures

fun NavController.navigateToFigureSheet(figureId: String, navOptions: NavOptions? = null) =
    navigate(FigureSheet(figureId), navOptions)

fun NavController.navigateToAllFigures(navOptions: NavOptions? = null) =
    navigate(AllFigures, navOptions)

fun NavGraphBuilder.figureScreens(
    onBack: () -> Unit,
    onStartConversation: (String) -> Unit,
    onFigureClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<FigureSheet> { entry ->
        val args = entry.toRoute<FigureSheet>()
        val viewModel: FigureSheetViewModel = hiltViewModel()
        LaunchedEffect(args.figureId) { viewModel.onFigureShown(args.figureId) }
        val nearby by viewModel.figure.collectAsStateWithLifecycle()
        FigureSheetScreen(
            figureId = args.figureId,
            nearby = nearby,
            onBack = onBack,
            onRelatedSites = { /* TODO: 관련 유적지 화면 */ },
            onStartConversation = onStartConversation,
            modifier = modifier,
        )
    }
    composable<AllFigures> {
        val viewModel: AllFiguresViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        AllFiguresScreen(
            state = state,
            onBack = onBack,
            onFigureClick = onFigureClick,
            modifier = modifier,
        )
    }
}
