package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StatusEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statusDao(): StatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "status_vault.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            db.execSQL("DELETE FROM statuses WHERE source = 'SAMPLE' OR id LIKE 'sample_%' OR filePath LIKE '%sample%' OR title LIKE 'status_%'")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
