package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.BookmarkDao
import com.example.data.database.dao.ReadingProgressDao
import com.example.data.database.dao.RecentContentDao
import com.example.data.database.dao.WritingDraftDao
import com.example.data.database.entities.BookmarkEntity
import com.example.data.database.entities.ReadingProgressEntity
import com.example.data.database.entities.RecentContentEntity
import com.example.data.database.entities.WritingDraftEntity

@Database(
    entities = [
        BookmarkEntity::class,
        RecentContentEntity::class,
        WritingDraftEntity::class,
        ReadingProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun recentContentDao(): RecentContentDao
    abstract fun writingDraftDao(): WritingDraftDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "written_academy_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
