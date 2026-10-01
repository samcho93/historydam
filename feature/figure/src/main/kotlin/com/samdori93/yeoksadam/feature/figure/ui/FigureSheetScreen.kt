package com.samdori93.yeoksadam.feature.figure.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import androidx.compose.ui.text.style.TextAlign
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure

/** 인물 선택 시트 (목업 2번) — 배경 딤 + 초상 + [관련 유적지 / 대화하기]. */
@Composable
fun FigureSheetScreen(
    figureId: String,
    nearby: NearbyFigure?,
    onBack: () -> Unit,
    onRelatedSites: (String) -> Unit,
    onStartConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val figure = nearby?.figure
    val name = figure?.name ?: "…"
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC140E09))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            MedallionPortrait(
                portraitUrl = figure?.portraitUrl?.ifBlank { null },
                name = name,
                sealMark = figure?.seal?.ifBlank { null } ?: name.take(1),
                size = 150.dp,
                selected = true,
            )
            Spacer(Modifier.height(18.dp))
            Text(name, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DancheongColors.Baek)
            if (figure != null) {
                Text("${figure.title} · ${figure.years}", fontSize = 13.sp, color = DancheongColors.HanjiDim)
                Text("${nearby.site.name} · ${formatDistance(nearby.distanceM)}", fontSize = 13.sp, color = DancheongColors.HwangtoLight)
                if (figure.bio.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        figure.bio,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = DancheongColors.Baek.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SheetPill(
                icon = Icons.Outlined.Place,
                label = "관련 유적지",
                solid = false,
                onClick = { onRelatedSites(figureId) },
            )
            Spacer(Modifier.height(12.dp))
            SheetPill(
                icon = Icons.Outlined.QuestionAnswer,
                label = "대화하기",
                solid = true,
                onClick = { onStartConversation(figureId) },
            )
        }

        // 닫기(아래로)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "닫기", tint = Color.White)
        }
    }
}

@Composable
private fun SheetPill(icon: ImageVector, label: String, solid: Boolean, onClick: () -> Unit) {
    val fg = if (solid) Color.White else DancheongColors.Baek
    val shape = RoundedCornerShape(26.dp)
    val base = Modifier
        .fillMaxWidth()
        .height(52.dp)
    val styled = if (solid) {
        base.background(DancheongColors.Jujak, shape)
    } else {
        base.border(1.dp, Color.White.copy(alpha = 0.55f), shape)
    }
    Row(
        modifier = styled.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** 1.2km · 350m */
internal fun formatDistance(m: Float): String = if (m >= 1000) "%.1fkm".format(m / 1000) else "${m.toInt()}m"

@Preview
@Composable
private fun FigureSheetPreview() {
    YeoksadamTheme {
        FigureSheetScreen("chae-jegong", null, {}, {}, {})
    }
}
