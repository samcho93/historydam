package com.samdori93.yeoksadam.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samdori93.yeoksadam.core.database.dao.ChatDao
import com.samdori93.yeoksadam.core.database.dao.FigureDao
import com.samdori93.yeoksadam.core.database.entity.ChatMessageEntity
import com.samdori93.yeoksadam.core.database.entity.FigureEntity

@Database(
    entities = [FigureEntity::class, ChatMessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class YeoksadamDatabase : RoomDatabase() {
    abstract fun figureDao(): FigureDao
    abstract fun chatDao(): ChatDao
}