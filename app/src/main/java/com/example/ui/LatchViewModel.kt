package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GradeResult
import com.example.data.*
import com.example.utils.PermissionUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Onboarding : Screen()
    object Dashboard : Screen()
    object AppPicker : Screen()
    object GoalsInput : Screen()
    object PlanOverview : Screen()
    data class ExplainSession(val checkpointId: Int) : Screen()
    object Settings : Screen()
}

class LatchViewModel(
    application: Application,
    private val repository: LatchRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("latch_prefs", Context.MODE_PRIVATE)

    // UI Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Onboarding)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // SharedPreferences State
    private val _customApiKey = MutableStateFlow(sharedPrefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(sharedPrefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Room Database Observables
    val allBlockedApps: StateFlow<List<BlockedApp>> = repository.allBlockedAppsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<Goal>> = repository.allGoalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoal: StateFlow<Goal?> = repository.activeGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val writingPersona: StateFlow<WritingPersona?> = repository.writingPersonaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentAcceptedExplanations: StateFlow<List<AcceptedExplanation>> = repository.recentAcceptedExplanationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttempts: StateFlow<List<Attempt>> = repository.allAttemptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeBudget: StateFlow<TimeBudget?> = repository.timeBudgetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dynamic checkpoints for active goal
    val activeCheckpoints: StateFlow<List<Checkpoint>> = activeGoal
        .flatMapLatest { goal ->
            if (goal != null) {
                repository.getCheckpointsForGoalFlow(goal.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Async Loading & Feedback State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    private val _gradeResult = MutableStateFlow<GradeResult?>(null)
    val gradeResult: StateFlow<GradeResult?> = _gradeResult.asStateFlow()

    // Permissions State
    private val _isUsagePermissionGranted = MutableStateFlow(false)
    val isUsagePermissionGranted: StateFlow<Boolean> = _isUsagePermissionGranted.asStateFlow()

    private val _isAccessibilityPermissionGranted = MutableStateFlow(false)
    val isAccessibilityPermissionGranted: StateFlow<Boolean> = _isAccessibilityPermissionGranted.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeTimeBudgetIfEmpty()
            checkPermissions()
            
            // Auto route to Dashboard if onboarding is completed
            if (_isOnboardingCompleted.value) {
                _currentScreen.value = Screen.Dashboard
            } else {
                _currentScreen.value = Screen.Onboarding
            }
        }
    }

    /**
     * Checks permission state and updates observers
     */
    fun checkPermissions() {
        val context = getApplication<Application>()
        _isUsagePermissionGranted.value = PermissionUtils.isUsageStatsPermissionGranted(context)
        _isAccessibilityPermissionGranted.value = PermissionUtils.isAccessibilityServiceEnabled(context)
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _gradeResult.value = null // Reset grading result when shifting screens
        _errorMsg.value = null
    }

    fun saveCustomApiKey(key: String) {
        _customApiKey.value = key
        sharedPrefs.edit().putString("custom_api_key", key).apply()
    }

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
        navigateTo(Screen.Dashboard)
    }

    fun blockPackage(packageName: String, appName: String) {
        viewModelScope.launch {
            repository.blockApp(packageName, appName)
        }
    }

    fun unblockPackage(packageName: String) {
        viewModelScope.launch {
            repository.unblockApp(packageName)
        }
    }

    fun addManualMinutes(minutes: Float) {
        viewModelScope.launch {
            repository.addTimeCredit(minutes)
        }
    }

    fun drainMinutes(minutes: Float) {
        viewModelScope.launch {
            repository.drainTimeBudget(minutes)
        }
    }

    fun resetWritingPersona() {
        viewModelScope.launch {
            repository.resetPersona()
        }
    }

    /**
     * Actions called from Screens
     */
    fun generatePlan(topic: String, materials: String, targetDate: Long, targetScore: Int) {
        if (topic.trim().isEmpty()) {
            _errorMsg.value = "Please enter a study topic"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            try {
                repository.createGoalAndPlan(
                    topic = topic,
                    materials = materials,
                    targetDate = targetDate,
                    targetScore = targetScore,
                    customApiKey = _customApiKey.value
                )
                navigateTo(Screen.PlanOverview)
            } catch (e: Exception) {
                _errorMsg.value = "Failed to generate study plan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun bootstrapPersona(onboardingText: String) {
        if (onboardingText.trim().length < 15) {
            _errorMsg.value = "Please write a slightly longer sample (at least 15 words)"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            try {
                repository.bootstrapWritingPersona(onboardingText, _customApiKey.value)
                completeOnboarding()
            } catch (e: Exception) {
                _errorMsg.value = "Failed to analyze sample: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitTeachBack(checkpointId: Int, explanation: String) {
        if (explanation.trim().isEmpty()) {
            _errorMsg.value = "Please write your explanation first"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            _gradeResult.value = null
            try {
                val result = repository.submitExplanation(
                    checkpointId = checkpointId,
                    userExplanation = explanation,
                    customApiKey = _customApiKey.value
                )
                _gradeResult.value = result
            } catch (e: Exception) {
                _errorMsg.value = "Grading error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearActiveGradeResult() {
        _gradeResult.value = null
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }
}

class LatchViewModelFactory(
    private val application: Application,
    private val repository: LatchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LatchViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
