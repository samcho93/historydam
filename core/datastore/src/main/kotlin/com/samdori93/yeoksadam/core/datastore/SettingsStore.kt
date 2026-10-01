package com.samdori93.yeoksadam.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 대화 방식·지식 경계·표시 반경 (원시 값 저장 — 도메인 매핑은 core:data). */
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    data class Raw(val mode: String, val externalUrl: String, val boundary: Boolean, val radiusM: Int)

    val raw: Flow<Raw> = dataStore.data.map {
        Raw(
            mode = it[MODE] ?: "BASIC",
            externalUrl = it[URL] ?: "",
            boundary = it[BOUNDARY] ?: true,
            radiusM = it[RADIUS] ?: 10_000,
        )
    }

    suspend fun save(value: Raw) {
        dataStore.edit {
            it[MODE] = value.mode
            it[URL] = value.externalUrl
            it[BOUNDARY] = value.boundary
            it[RADIUS] = value.radiusM
        }
    }

    private companion object {
        val MODE = stringPreferencesKey("chat_mode")
        val URL = stringPreferencesKey("external_rag_url")
        val BOUNDARY = booleanPreferencesKey("knowledge_boundary")
        val RADIUS = intPreferencesKey("radius_m")
    }
}
