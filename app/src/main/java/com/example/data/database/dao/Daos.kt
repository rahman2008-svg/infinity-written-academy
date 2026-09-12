package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entities.BookmarkEntity
import com.example.data.database.entities.RecentContentEntity
import com.example.data.database.entities.WritingDraftEntity
import com.example.data.database.entities.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :id)")
    fun isBookmarked(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface RecentContentDao {
    @Query("SELECT * FROM recent_content ORDER BY lastOpened DESC LIMIT 20")
    fun getRecentContents(): Flow<List<RecentContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recent: RecentContentEntity)

    @Query("DELETE FROM recent_content WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_content")
    suspend fun clearAll()
}

@Dao
interface WritingDraftDao {
    @Query("SELECT * FROM writing_drafts ORDER BY lastModified DESC")
    fun getAllDrafts(): Flow<List<WritingDraftEntity>>

    @Query("SELECT * FROM writing_drafts WHERE topicId = :topicId LIMIT 1")
    suspend fun getDraftForTopic(topicId: String): WritingDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: WritingDraftEntity)

    @Query("DELETE FROM writing_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: String)

    @Query("DELETE FROM writing_drafts")
    suspend fun clearAllDrafts()
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE id = :id")
    suspend fun getProgress(id: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)
}
