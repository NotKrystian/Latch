package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val notesOrMaterials: String,
    val targetDate: Long,
    val targetScore: Int,
    val isActive: Boolean = true
)

@Entity(tableName = "checkpoints")
data class Checkpoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val subtopicName: String,
    val extractText: String,
    val promptText: String,
    val masteryScore: Float = 0.0f,
    val isCompleted: Boolean = false
)

@Entity(tableName = "writing_personas")
data class WritingPersona(
    @PrimaryKey val id: Int = 1, // Singleton row
    val sampleCount: Int = 0,
    val coldStart: Boolean = true,
    val vocabularyLevel: String = "Medium",
    val sentenceLength: Int = 12,
    val toneDescription: String = "Conversational and simple",
    val personaSummary: String = "No style persona established yet. Writing is soft-mode/cold-started."
)

@Entity(tableName = "accepted_explanations")
data class AcceptedExplanation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val checkpointId: Int,
    val subtopicName: String,
    val userExplanation: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "attempts")
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val checkpointId: Int,
    val userExplanation: String,
    val understandingOk: Boolean,
    val authenticityOk: Boolean,
    val understandingScore: Float,
    val authenticityScore: Float,
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "time_budgets")
data class TimeBudget(
    @PrimaryKey val id: Int = 1, // Singleton row
    val remainingMinutes: Float = 15.0f, // starts with 15 mins of default onboarding credit
    val totalEarnedMinutes: Int = 0,
    val lastDrainTimestamp: Long = System.currentTimeMillis()
)
