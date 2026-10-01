package com.samdori93.yeoksadam.feature.ar.ui

import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.domain.model.NearbySite
import com.samdori93.yeoksadam.core.ui.ar.angleDiff
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/** 세로 모드 기준 대략적인 후면 카메라 시야각 (웹앱 arEngine.ts 와 같음) */
const val H_FOV = 55f
const val V_FOV = 70f
private const val MAX_LABELS = 12

/** 화면에 놓을 라벨 하나 (좌표는 dp) */
data class PlacedLabel(
    val nearby: NearbySite,
    val x: Float,
    val y: Float,
    val scale: Float,
    val isTarget: Boolean,
    /** 겹쳐서 이 라벨 뒤로 숨긴 유적 */
    val hidden: List<NearbySite>,
    val score: Float,
)

data class ArLayoutResult(val labels: List<PlacedLabel>, val offLeft: Int, val offRight: Int)

/**
 * 라벨 배치 — 방위각이 시야 안에 든 유적을 화면에 놓고,
 * 중요한 유적부터 자리를 잡아 겹치는 라벨은 대표 라벨의 「+N」으로 묶는다.
 */
fun layoutLabels(
    sites: List<NearbySite>,
    heading: Float,
    pitch: Float,
    widthDp: Float,
    heightDp: Float,
    radiusM: Int,
    target: HeritageSite?,
    important: Set<String>,
): ArLayoutResult {
    val pxPerDegX = widthDp / H_FOV
    val pxPerDegY = heightDp / V_FOV
    val horizonY = heightDp / 2 + pitch * pxPerDegY
    var left = 0
    var right = 0
    data class Cand(val n: NearbySite, val delta: Float, val isTarget: Boolean, val x: Float, val y: Float, val scale: Float, val box: FloatArray, val score: Float)

    val inView = sites.mapNotNull { n ->
        val delta = angleDiff(heading, n.bearingDeg)
        if (abs(delta) > H_FOV / 2 + 5) {
            if (delta < 0) left++ else right++
            return@mapNotNull null
        }
        val isTarget = n.site.id == target?.id
        // 멀수록 위쪽·작게, 타깃은 멀리 있어도 눈높이 근처
        val t = if (isTarget) 0.2f else min(1f, (ln(1f + n.distanceM / 100f) / ln(1f + radiusM / 100f)))
        val scale = if (isTarget) 1.15f else 1.05f - t * 0.4f
        val x = widthDp / 2 + delta * pxPerDegX
        val y = horizonY - 40 - t * heightDp * 0.28f
        val bw = min(176f, 44f + n.site.name.length * 13f) * scale
        val bh = 58f * scale
        Cand(n, delta, isTarget, x, y, scale, floatArrayOf(x - bw / 2, y - bh, x + bw / 2, y), score(n, isTarget, important))
    }.sortedByDescending { it.score }

    val placed = mutableListOf<Cand>()
    val groups = HashMap<String, MutableList<NearbySite>>()
    for (c in inView) {
        val hit = placed.firstOrNull { p ->
            c.box[0] < p.box[2] + 4 && c.box[2] > p.box[0] - 4 && c.box[1] < p.box[3] + 4 && c.box[3] > p.box[1] - 4
        }
        if (hit != null || placed.size >= MAX_LABELS) {
            val owner = hit ?: placed.minBy { abs(it.x - c.x) }
            groups.getOrPut(owner.n.site.id) { mutableListOf() }.add(c.n)
            continue
        }
        placed += c
    }
    return ArLayoutResult(
        labels = placed.map { PlacedLabel(it.n, it.x, it.y, it.scale, it.isTarget, groups[it.n.site.id].orEmpty(), it.score) },
        offLeft = left,
        offRight = right,
    )
}

/** 대표로 보일 우선순위: 찾아갈 장소 > 지정 등급 > 인물 연결 > 가까움 */
private fun score(n: NearbySite, isTarget: Boolean, important: Set<String>): Float {
    if (isTarget) return 1e6f
    val s = n.site
    val g = s.designation
    val grade = when {
        "국보" in g -> 100
        "보물" in g -> 90
        "사적" in g -> 85
        "명승" in g || "천연기념물" in g -> 75
        Regex("국가(민속|무형)").containsMatchIn(g) -> 70
        s.local -> 40
        s.tour -> 45
        "등록" in g || "문화유산자료" in g -> 50
        else -> 60
    }
    // 유물·기록유산·무형유산은 찾아가 볼 '장소'가 아니므로 대표에서 뒤로
    val place = if (Regex("유물|기록유산|무형").containsMatchIn(s.category)) -75 else 0
    return grade + place + (if (s.id in important) 25 else 0) - log10(max(n.distanceM, 10f)) * 6
}

fun toCompass(h: Float): String = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")[((h / 45f).toInt() + if (h % 45f >= 22.5f) 1 else 0) % 8]
