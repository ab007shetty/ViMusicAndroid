package com.abshetty.vimusic.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE playlist ADD COLUMN coverUrl TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS search_history")
            db.execSQL(
                "CREATE TABLE search_history (" +
                    "query TEXT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "PRIMARY KEY(query, userId))"
            )
        }
    }

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ViMusicDatabase =
        Room.databaseBuilder(context, ViMusicDatabase::class.java, "vimusic.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)

            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideSongDao(db: ViMusicDatabase) = db.songDao()
    @Provides fun providePlaylistDao(db: ViMusicDatabase) = db.playlistDao()
    @Provides fun provideOutboxDao(db: ViMusicDatabase) = db.outboxDao()
    @Provides fun provideSearchHistoryDao(db: ViMusicDatabase) = db.searchHistoryDao()
    @Provides fun provideLyricsDao(db: ViMusicDatabase) = db.lyricsDao()
}
