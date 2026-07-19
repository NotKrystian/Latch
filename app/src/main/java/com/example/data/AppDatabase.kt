package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedApp::class,
        Goal::class,
        Checkpoint::class,
        WritingPersona::class,
        AcceptedExplanation::class,
        Attempt::class,
        TimeBudget::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun goalDao(): GoalDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun writingPersonaDao(): WritingPersonaDao
    abstract fun acceptedExplanationDao(): AcceptedExplanationDao
    abstract fun attemptDao(): AttemptDao
    abstract fun timeBudgetDao(): TimeBudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "latch_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
