package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.GeminiClient
import com.example.api.GradeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class LatchRepository(private val db: AppDatabase) {
    private val TAG = "LatchRepository"

    // DAOs
    private val blockedAppDao = db.blockedAppDao()
    private val goalDao = db.goalDao()
    private val checkpointDao = db.checkpointDao()
    private val writingPersonaDao = db.writingPersonaDao()
    private val acceptedExplanationDao = db.acceptedExplanationDao()
    private val attemptDao = db.attemptDao()
    private val timeBudgetDao = db.timeBudgetDao()

    // Flows
    val allBlockedAppsFlow: Flow<List<BlockedApp>> = blockedAppDao.getAllBlockedAppsFlow()
    val allGoalsFlow: Flow<List<Goal>> = goalDao.getAllGoalsFlow()
    val activeGoalFlow: Flow<Goal?> = goalDao.getActiveGoalFlow()
    val writingPersonaFlow: Flow<WritingPersona?> = writingPersonaDao.getWritingPersonaFlow()
    val recentAcceptedExplanationsFlow: Flow<List<AcceptedExplanation>> = acceptedExplanationDao.getRecentAcceptedExplanationsFlow()
    val allAttemptsFlow: Flow<List<Attempt>> = attemptDao.getAllAttemptsFlow()
    val timeBudgetFlow: Flow<TimeBudget?> = timeBudgetDao.getTimeBudgetFlow()

    fun getCheckpointsForGoalFlow(goalId: Int): Flow<List<Checkpoint>> {
        return checkpointDao.getCheckpointsForGoalFlow(goalId)
    }

    /**
     * Bootstraps the initial persona
     */
    suspend fun bootstrapWritingPersona(onboardingText: String, customApiKey: String?): WritingPersona = withContext(Dispatchers.IO) {
        val apiKey = GeminiClient.getApiKey(customApiKey)
        val result = GeminiClient.bootstrapPersona(apiKey, onboardingText)
        
        val newPersona = WritingPersona(
            id = 1,
            sampleCount = 1,
            coldStart = false,
            vocabularyLevel = result.vocabularyLevel,
            sentenceLength = result.sentenceLength,
            toneDescription = result.toneDescription,
            personaSummary = result.personaSummary
        )
        writingPersonaDao.insertWritingPersona(newPersona)
        newPersona
    }

    /**
     * Resets persona back to cold-start state
     */
    suspend fun resetPersona() = withContext(Dispatchers.IO) {
        val reset = WritingPersona(
            id = 1,
            sampleCount = 0,
            coldStart = true,
            vocabularyLevel = "Medium",
            sentenceLength = 12,
            toneDescription = "Conversational and simple",
            personaSummary = "No style persona established yet. Writing is soft-mode/cold-started."
        )
        writingPersonaDao.insertWritingPersona(reset)
        acceptedExplanationDao.clearAll()
    }

    /**
     * Adds an app to block
     */
    suspend fun blockApp(packageName: String, appName: String) = withContext(Dispatchers.IO) {
        blockedAppDao.insertBlockedApp(BlockedApp(packageName, appName, true))
    }

    /**
     * Unblocks an app
     */
    suspend fun unblockApp(packageName: String) = withContext(Dispatchers.IO) {
        blockedAppDao.deleteBlockedApp(packageName)
    }

    /**
     * Standard initializer for TimeBudget
     */
    suspend fun initializeTimeBudgetIfEmpty() = withContext(Dispatchers.IO) {
        val current = timeBudgetDao.getTimeBudget()
        if (current == null) {
            timeBudgetDao.insertTimeBudget(TimeBudget())
        }
    }

    /**
     * Adds manual study credit (e.g., onboarding gift)
     */
    suspend fun addTimeCredit(minutes: Float) = withContext(Dispatchers.IO) {
        val current = timeBudgetDao.getTimeBudget() ?: TimeBudget()
        val updated = current.copy(
            remainingMinutes = current.remainingMinutes + minutes,
            totalEarnedMinutes = current.totalEarnedMinutes + minutes.toInt(),
            lastDrainTimestamp = System.currentTimeMillis()
        )
        timeBudgetDao.insertTimeBudget(updated)
    }

    /**
     * Drain time budget based on foreground package usage
     */
    suspend fun drainTimeBudget(minutesToDrain: Float) = withContext(Dispatchers.IO) {
        val current = timeBudgetDao.getTimeBudget()
        if (current != null) {
            val updatedRemaining = (current.remainingMinutes - minutesToDrain).coerceAtLeast(0.0f)
            val updated = current.copy(
                remainingMinutes = updatedRemaining,
                lastDrainTimestamp = System.currentTimeMillis()
            )
            timeBudgetDao.insertTimeBudget(updated)
        }
    }

    /**
     * Create a learning plan and save checkpoints
     */
    suspend fun createGoalAndPlan(
        topic: String,
        materials: String,
        targetDate: Long,
        targetScore: Int,
        customApiKey: String?
    ): Goal = withContext(Dispatchers.IO) {
        // Deactivate past goals
        goalDao.deactivateAllGoals()

        // Create new goal
        val newGoal = Goal(
            topic = topic,
            notesOrMaterials = materials,
            targetDate = targetDate,
            targetScore = targetScore,
            isActive = true
        )
        val goalId = goalDao.insertGoal(newGoal).toInt()
        val savedGoal = newGoal.copy(id = goalId)

        // Generate Plan Checkpoints via Gemini or Mock
        val apiKey = GeminiClient.getApiKey(customApiKey)
        val fixtures = GeminiClient.generateLearningPlan(apiKey, topic, materials)

        val checkpoints = fixtures.map {
            Checkpoint(
                goalId = goalId,
                subtopicName = it.subtopicName,
                extractText = it.extractText,
                promptText = it.promptText,
                masteryScore = 0.0f,
                isCompleted = false
            )
        }
        checkpointDao.insertCheckpoints(checkpoints)
        savedGoal
    }

    /**
     * Submits a teach-back explanation to Gemini, saves attempt, and rewards credit if valid
     */
    suspend fun submitExplanation(
        checkpointId: Int,
        userExplanation: String,
        customApiKey: String?
    ): GradeResult = withContext(Dispatchers.IO) {
        val checkpoint = checkpointDao.getCheckpointById(checkpointId)
            ?: throw IllegalArgumentException("Checkpoint not found")

        val persona = writingPersonaDao.getWritingPersona() ?: WritingPersona()
        val pastAccepted = acceptedExplanationDao.getRecentAcceptedExplanations()
        val apiKey = GeminiClient.getApiKey(customApiKey)

        // Grade via Gemini Client
        val gradeResult = GeminiClient.gradeExplanation(
            apiKey = apiKey,
            subtopicName = checkpoint.subtopicName,
            extractText = checkpoint.extractText,
            promptText = checkpoint.promptText,
            userExplanation = userExplanation,
            personaSummary = persona.personaSummary,
            pastExplanations = pastAccepted
        )

        // Create and save Attempt
        val attempt = Attempt(
            checkpointId = checkpointId,
            userExplanation = userExplanation,
            understandingOk = gradeResult.understandingOk,
            authenticityOk = gradeResult.authenticityOk,
            understandingScore = gradeResult.understandingScore,
            authenticityScore = gradeResult.authenticityScore,
            feedback = gradeResult.feedback
        )
        attemptDao.insertAttempt(attempt)

        // If BOTH understanding and authenticity pass, complete checkpoint and reward credit
        if (gradeResult.understandingOk && gradeResult.authenticityOk) {
            // Update Checkpoint progress
            val updatedCheckpoint = checkpoint.copy(
                masteryScore = gradeResult.understandingScore,
                isCompleted = true
            )
            checkpointDao.updateCheckpoint(updatedCheckpoint)

            // Save Accepted Explanation
            val accepted = AcceptedExplanation(
                checkpointId = checkpointId,
                subtopicName = checkpoint.subtopicName,
                userExplanation = userExplanation
            )
            acceptedExplanationDao.insertAcceptedExplanation(accepted)

            // Update style persona
            val updatedPersona = persona.copy(
                sampleCount = persona.sampleCount + 1,
                coldStart = false,
                vocabularyLevel = gradeResult.vocabularyLevel,
                sentenceLength = gradeResult.sentenceLength,
                toneDescription = gradeResult.toneDescription,
                personaSummary = gradeResult.personaSummary
            )
            writingPersonaDao.insertWritingPersona(updatedPersona)

            // Reward time credit
            if (gradeResult.earnedMinutes > 0) {
                addTimeCredit(gradeResult.earnedMinutes.toFloat())
            }
        }

        gradeResult
    }

    suspend fun deleteGoal(id: Int) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(id)
    }
}
