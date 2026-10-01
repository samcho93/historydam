package com.samdori93.yeoksadam.feature.ar.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.ar.ui.ArRoute
import kotlinx.serialization.Serializable

/**
 * 위치 기반 AR (CLAUDE.md §9) — 카메라에 비친 방향의 유적 라벨.
 * figureId·siteId 가 있으면 그 유적으로 방향 안내, 비어 있으면 주변 탐색.
 */
@Serializable
data class ArSearch(val figureId: String = "", val siteId: String = "")

fun NavController.navigateToAr(figureId: String = "", siteId: String = "", navOptions: NavOptions? = null) =
    navigate(ArSearch(figureId, siteId), navOptions)

fun NavGraphBuilder.arScreen(
    onStartConversation: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    composable<ArSearch> {
        ArRoute(onBack = onBack, onTalk = onStartConversation, modifier = modifier)
    }
}
