package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.LatchRepository
import com.example.ui.LatchViewModel
import com.example.ui.LatchViewModelFactory
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.LatchTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: LatchRepository

    // Lazy instantiation of viewmodel using simple constructor injection factory
    private val viewModel: LatchViewModel by viewModels {
        database = AppDatabase.getDatabase(this)
        repository = LatchRepository(database)
        LatchViewModelFactory(application, repository)
    }

    // Live state to pass blocked app warnings directly to the Dashboard Composable
    private var interruptedPackageState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial intent
        handleIncomingIntent(intent)

        setContent {
            LatchTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val interruptedPackage by interruptedPackageState

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                            when (screen) {
                                is Screen.Onboarding -> OnboardingScreen(viewModel)
                                is Screen.Dashboard -> DashboardScreen(viewModel, interruptedPackage)
                                is Screen.AppPicker -> AppPickerScreen(viewModel)
                                is Screen.GoalsInput -> GoalsInputScreen(viewModel)
                                is Screen.PlanOverview -> PlanOverviewScreen(viewModel)
                                is Screen.ExplainSession -> ExplainSessionScreen(viewModel, screen.checkpointId)
                                is Screen.Settings -> SettingsScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions and budget state when user returns to app
        viewModel.checkPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        
        val triggerExplain = intent.getBooleanExtra("TRIGGER_EXPLAIN", false)
        val interruptedPackage = intent.getStringExtra("INTERRUPTED_PACKAGE")

        if (triggerExplain) {
            interruptedPackageState.value = interruptedPackage

            // Route user directly to the explain session of the first incomplete checkpoint
            lifecycleScope.launch {
                val checkpoints = viewModel.activeCheckpoints.firstOrNull() ?: emptyList()
                val activeGoal = viewModel.activeGoal.firstOrNull()
                val onboardingCompleted = viewModel.isOnboardingCompleted.firstOrNull() ?: false

                if (!onboardingCompleted) {
                    // Force complete onboarding / permissions first
                    viewModel.navigateTo(Screen.Onboarding)
                    return@launch
                }

                val nextCheckpoint = checkpoints.firstOrNull { !it.isCompleted }
                if (nextCheckpoint != null) {
                    viewModel.navigateTo(Screen.ExplainSession(nextCheckpoint.id))
                } else if (activeGoal != null) {
                    viewModel.navigateTo(Screen.PlanOverview)
                } else {
                    viewModel.navigateTo(Screen.GoalsInput)
                }
            }
        }
    }
}
