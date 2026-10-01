package com.samdori93.yeoksadam.core.ui.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/** 카메라가 향한 방위 (북쪽 0°, 시계방향) 와 위아래 기울기 (수평 0°, 위가 +). */
data class Orientation(val heading: Float, val pitch: Float, val available: Boolean)

/**
 * 폰을 세워 들고 카메라로 볼 때의 방위 — 회전 벡터 센서(자이로+지자기 융합)를
 * 카메라 축 기준으로 바꿔(remap X, Z) 계산하고 저역통과 필터로 흔들림을 줄인다. (웹앱 orientation.ts 와 같은 역할)
 */
@Composable
fun rememberCameraOrientation(): State<Orientation> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(Orientation(0f, 0f, available = false)) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            onDispose { }
        } else {
            val rot = FloatArray(9)
            val remapped = FloatArray(9)
            val out = FloatArray(3)
            var heading = Float.NaN
            var pitch = 0f
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rot, e.values)
                    SensorManager.remapCoordinateSystem(rot, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped)
                    SensorManager.getOrientation(remapped, out)
                    val h = ((Math.toDegrees(out[0].toDouble()).toFloat()) + 360f) % 360f
                    val p = -Math.toDegrees(out[1].toDouble()).toFloat()
                    heading = if (heading.isNaN()) h else smooth(heading, h)
                    pitch += (p - pitch) * ALPHA
                    state.value = Orientation(heading, pitch, available = true)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    return state
}

private const val ALPHA = 0.15f

/** 359°→1° 처럼 경계를 넘을 때도 짧은 쪽으로 보간 */
private fun smooth(prev: Float, next: Float): Float {
    val d = angleDiff(prev, next)
    return (prev + d * ALPHA + 360f) % 360f
}

/** from → to 의 부호 있는 각도 차 (-180~180) */
fun angleDiff(from: Float, to: Float): Float {
    var d = (to - from) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

fun isWithin(a: Float, b: Float, deg: Float) = abs(angleDiff(a, b)) <= deg
