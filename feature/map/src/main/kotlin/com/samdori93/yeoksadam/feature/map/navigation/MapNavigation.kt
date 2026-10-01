package com.samdori93.yeoksadam.feature.map.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.map.ui.MapRoute
import kotlinx.serialization.Serializable

/** 지도 라우트 — 네이버 지도 + 인물 핑 · 표시 반경 원 → AR 진입(CLAUDE.md §6 #4). */
@Serializable
data object MapGraph

fun NavController.navigateToMap(navOptions: NavOptions? = null) =
    navigate(MapGraph, navOptions)

fun NavGraphBuilder.mapScreen(
    onEnterAr: (figureId: String, siteId: String) -> Unit,
    onStartConversation: (figureId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<MapGraph> {
        MapRoute(onEnterAr = onEnterAr, onStartConversation = onStartConversation, modifier = modifier)
    }
}
