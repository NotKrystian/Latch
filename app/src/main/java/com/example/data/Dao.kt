package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps")
    fun getAllBlockedAppsFlow(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAllBlockedApps(): List<BlockedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedApp)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE isActive = 1 LIMIT 1")
    fun getActiveGoalFlow(): Flow<Goal?>

    @Query("SELECT * FROM goals WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveGoal(): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Query("UPDATE goals SET isActive = 0")
    suspend fun deactivateAllGoals()

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Int)
}

@Dao
interface CheckpointDao {
    @Query("SELECT * FROM checkpoints WHERE goalId = :goalId ORDER BY id ASC")
    fun getCheckpointsForGoalFlow(goalId: Int): Flow<List<Checkpoint>>

    @Query("SELECT * FROM checkpoints WHERE goalId = :goalId ORDER BY id ASC")
    suspend fun getCheckpointsForGoal(goalId: Int): List<Checkpoint>

    @Query("SELECT * FROM checkpoints WHERE id = :id LIMIT 1")
    suspend fun getCheckpointById(id: Int): Checkpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoints(checkpoints: List<Checkpoint>)

    @Update
    suspend fun updateCheckpoint(checkpoint: Checkpoint)
}

@Dao
interface WritingPersonaDao {
    @Query("SELECT * FROM writing_personas WHERE id = 1 LIMIT 1")
    fun getWritingPersonaFlow(): Flow<WritingPersona?>

    @Query("SELECT * FROM writing_personas WHERE id = 1 LIMIT 1")
    suspend fun getWritingPersona(): WritingPersona?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWritingPersona(persona: WritingPersona)
}

@Dao
interface AcceptedExplanationDao {
    @Query("SELECT * FROM accepted_explanations ORDER BY timestamp DESC LIMIT 10")
    fun getRecentAcceptedExplanationsFlow(): Flow<List<AcceptedExplanation>>

    @Query("SELECT * FROM accepted_explanations ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentAcceptedExplanations(): List<AcceptedExplanation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcceptedExplanation(exp: AcceptedExplanation)

    @Query("DELETE FROM accepted_explanations")
    suspend fun clearAll()
}

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts ORDER BY timestamp DESC")
    fun getAllAttemptsFlow(): Flow<List<Attempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: Attempt)
}

@Dao
interface TimeBudgetDao {
    @Query("SELECT * FROM time_budgets WHERE id = 1 LIMIT 1")
    fun getTimeBudgetFlow(): Flow<TimeBudget?>

    @Query("SELECT * FROM time_budgets WHERE id = 1 LIMIT 1")
    suspend fun getTimeBudget(): TimeBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBudget(budget: TimeBudget)
}
