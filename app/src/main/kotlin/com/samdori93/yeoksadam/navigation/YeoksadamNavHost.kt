package com.samdori93.yeoksadam.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samdori93.yeoksadam.core.ui.component.YeoksadamBottomBar
import com.samdori93.yeoksadam.core.ui.navigation.TopLevelDestination
import com.samdori93.yeoksadam.feature.camera.navigation.CameraGraph
import com.samdori93.yeoksadam.feature.camera.navigation.cameraScreen
import com.samdori93.yeoksadam.feature.camera.navigation.navigateToCamera
import com.samdori93.yeoksadam.feature.chat.navigation.Chat
import com.samdori93.yeoksadam.feature.chat.navigation.chatScreen
import com.samdori93.yeoksadam.feature.chat.navigation.navigateToChat
import com.samdori93.yeoksadam.feature.figure.navigation.figureScreens
import com.samdori93.yeoksadam.feature.figure.navigation.navigateToAllFigures
import com.samdori93.yeoksadam.feature.figure.navigation.navigateToFigureSheet
import com.samdori93.yeoksadam.feature.home.navigation.Home
import com.samdori93.yeoksadam.feature.home.navigation.homeScreen
import com.samdori93.yeoksadam.feature.home.navigation.navigateToHome
import com.samdori93.yeoksadam.feature.map.navigation.MapGraph
import com.samdori93.yeoksadam.feature.map.navigation.mapScreen
import com.samdori93.yeoksadam.feature.map.navigation.navigateToMap
import com.samdori93.yeoksadam.feature.ar.navigation.arScreen
import com.samdori93.yeoksadam.feature.ar.navigation.navigateToAr
import com.samdori93.yeoksadam.feature.chat.navigation.ChatList
import com.samdori93.yeoksadam.feature.chat.navigation.navigateToChatList
import com.samdori93.yeoksadam.feature.notification.navigation.navigateToNotification
import com.samdori93.yeoksadam.feature.notification.navigation.notificationScreen
import com.samdori93.yeoksadam.feature.profile.navigation.Profile
import com.samdori93.yeoksadam.feature.profile.navigation.profileScreen
import com.samdori93.yeoksadam.feature.profile.navigation.navigateToProfile
import com.samdori93.yeoksadam.feature.voice.navigation.Voice
import com.samdori93.yeoksadam.feature.ar.navigation.ArSearch
import com.samdori93.yeoksadam.feature.voice.navigation.navigateToVoice
import com.samdori93.yeoksadam.feature.voice.navigation.voiceScreen

/**
 * 앱 셸 — 하단 5탭 Scaffold + 전체 feature 그래프 조립.
 * feature 간 직접 의존이 없으므로 화면 전환은 모두 이 NavHost 가 콜백으로 연결한다(CLAUDE.md §3).
 */
@Composable
fun YeoksadamAppRoot(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchyHasTab(tab) == true
    } ?: TopLevelDestination.HOME

    // 인물 대면 대화·AR 은 카메라 화면을 가득 쓴다 (하단 탭 숨김, 웹앱의 전체 화면 모드)
    val immersive = currentDestination?.hierarchy?.any { it.hasRoute(Voice::class) || it.hasRoute(ArSearch::class) } == true

    Scaffold(
        bottomBar = {
            if (!immersive) {
                YeoksadamBottomBar(
                    current = currentTab,
                    onSelect = { dest -> navController.navigateToTab(dest) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(padding),
        ) {
            homeScreen(
                onNavigateToFigureSheet = { navController.navigateToFigureSheet(it) },
                onNavigateToAllFigures = { navController.navigateToAllFigures() },
                onNavigateToNotifications = { navController.navigateToNotification() },
            )
            figureScreens(
                onBack = { navController.popBackStack() },
                // 인물과 마주 보는 AR 대화 (텍스트 대화는 Q&A 탭)
                onStartConversation = { navController.navigateToVoice(it) },
                onFigureClick = { navController.navigateToFigureSheet(it) },
            )
            mapScreen(
                onEnterAr = { figureId, siteId -> navController.navigateToAr(figureId, siteId) },
                onStartConversation = { navController.navigateToVoice(it) },
            )
            arScreen(
                onStartConversation = { navController.navigateToChat(it) },
            )
            cameraScreen(
                onBack = { navController.navigateToHome() },
                onOpenFigure = { navController.navigateToFigureSheet(it) },
            )
            chatScreen(
                onFigureClick = { figureId ->
                    // 1. 목록에서 인물 카드를 누르면 해당 figureId 대화방으로 이동
                    navController.navigateToChat(figureId)
                },
                onBack = {
                    // 2. 대화방에서 뒤로가기 누르면 인물 목록 화면으로 복귀
                    navController.popBackStack()
                },
                onSwitchToVoice = { figureId ->
                    // 3. 기존 음성 대화 전환 로직 유지
                    navController.navigateToVoice(figureId)
                }
            )
            voiceScreen(
                onBack = { navController.popBackStack() },
            )
            profileScreen(
                onBack = { navController.navigateToHome() },
            )
            notificationScreen()
        }
    }
}

private fun androidx.navigation.NavDestination.hierarchyHasTab(tab: TopLevelDestination): Boolean =
    hierarchy.any { dest ->
        when (tab) {
            TopLevelDestination.HOME -> dest.hasRoute(Home::class)
            TopLevelDestination.MAP -> dest.hasRoute(MapGraph::class)
            TopLevelDestination.CAMERA -> dest.hasRoute(CameraGraph::class)
            TopLevelDestination.CHAT -> dest.hasRoute(route = ChatList::class) || dest.hasRoute(route = Chat::class)
            TopLevelDestination.MENU -> dest.hasRoute(Profile::class)
        }
    }

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    val options = androidx.navigation.navOptions {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (tab) {
        TopLevelDestination.HOME -> navigateToHome(options)
        TopLevelDestination.MAP -> navigateToMap(options)
        TopLevelDestination.CAMERA -> navigateToCamera(options)
        TopLevelDestination.CHAT -> navigateToChatList(navOptions = options)
        TopLevelDestination.MENU -> navigateToProfile(options)
    }
}
