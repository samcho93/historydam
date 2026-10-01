package com.samdori93.yeoksadam.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val figureId: String,
    val role: String, // "USER" 또는 "FIGURE"
    val text: String,
    /** RAG 근거 (JSON 배열 [{source, excerpt, refId}]) */
    val citationsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)