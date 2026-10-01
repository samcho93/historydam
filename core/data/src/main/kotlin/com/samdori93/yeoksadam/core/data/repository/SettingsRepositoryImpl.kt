package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.datastore.SettingsStore
import com.samdori93.yeoksadam.core.domain.model.ChatMode
import com.samdori93.yeoksadam.core.domain.model.ChatSettings
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsStore,
) : SettingsRepository {

    override val settings: Flow<ChatSettings> = store.raw.map { r ->
        ChatSettings(
            mode = runCatching { ChatMode.valueOf(r.mode) }.getOrDefault(ChatMode.BASIC),
            externalUrl = r.externalUrl,
            boundary = r.boundary,
            radiusM = r.radiusM,
        )
    }

    override suspend fun update(transform: (ChatSettings) -> ChatSettings) {
        val next = transform(settings.first())
        store.save(
            SettingsStore.Raw(
                mode = next.mode.name,
                externalUrl = next.externalUrl.trim().trimEnd('/'),
                boundary = next.boundary,
                radiusM = next.radiusM,
            ),
        )
    }
}
