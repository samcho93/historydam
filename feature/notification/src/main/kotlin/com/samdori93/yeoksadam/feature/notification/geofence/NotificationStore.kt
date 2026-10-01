package com.samdori93.yeoksadam.feature.notification.geofence

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 받은 위치 알림 (최근 50건) — 기기에 저장되어 앱을 다시 열어도 남는다. */
object NotificationStore {

    @Serializable
    data class Event(val title: String, val body: String, val timeMillis: Long, val siteId: String? = null)

    private const val PREFS = "yeoksadam_notifications"
    private const val KEY = "events"
    private const val MAX = 50

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        _events.value = runCatching { Json.decodeFromString<List<Event>>(raw) }.getOrDefault(emptyList())
    }

    fun add(context: Context, event: Event) {
        load(context)
        val next = (listOf(event) + _events.value).take(MAX)
        _events.value = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, Json.encodeToString(next)).apply()
    }
}
