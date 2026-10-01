package com.samdori93.yeoksadam.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.samdori93.yeoksadam.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE figureId = :figureId ORDER BY timestamp ASC")
    fun getMessagesByFigureId(figureId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE figureId = :figureId")
    suspend fun clearHistory(figureId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()

    /** 서버에 보낼 최근 대화 (최신 limit 개를 오래된 순으로) */
    @Query(
        "SELECT * FROM (SELECT * FROM chat_messages WHERE figureId = :figureId " +
            "ORDER BY timestamp DESC, id DESC LIMIT :limit) ORDER BY timestamp ASC, id ASC",
    )
    suspend fun recent(figureId: String, limit: Int): List<ChatMessageEntity>

    @Query("SELECT COUNT(DISTINCT figureId) FROM chat_messages")
    fun roomCount(): Flow<Int>
}