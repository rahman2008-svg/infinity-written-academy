package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val language: String,
    val assetPath: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_content")
data class RecentContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val language: String,
    val assetPath: String,
    val lastOpened: Long = System.currentTimeMillis(),
    val progressFraction: Float = 0f
)

@Entity(tableName = "writing_drafts")
data class WritingDraftEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val topicTitle: String,
    val language: String,
    val category: String,
    val userText: String,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val id: String,
    val scrollOffset: Int,
    val isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
