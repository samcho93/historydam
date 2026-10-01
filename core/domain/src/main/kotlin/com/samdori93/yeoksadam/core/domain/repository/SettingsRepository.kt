package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.ChatSettings
import kotlinx.coroutines.flow.Flow

/** 대화 방식·지식 경계·표시 반경 설정 (DataStore). */
interface SettingsRepository {
    val settings: Flow<ChatSettings>

    suspend fun update(transform: (ChatSettings) -> ChatSettings)
}
