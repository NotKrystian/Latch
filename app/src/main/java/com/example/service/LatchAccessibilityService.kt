package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.*

class LatchAccessibilityService : AccessibilityService() {
    private val TAG = "LatchAccessService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: AppDatabase
    
    // Tracks the current foreground app package
    private var activePackage: String? = null
    private var trackingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        Log.d(TAG, "Latch Accessibility Service Created")
        
        // Start background budget draining tracker
        startTimeBudgetDrainLoop()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Ignore ourselves
            if (packageName == this.packageName) {
                activePackage = packageName
                return
            }

            activePackage = packageName
            Log.d(TAG, "Active Package Changed to: $packageName")

            serviceScope.launch {
                try {
                    val blockedApps = database.blockedAppDao().getAllBlockedApps()
                    val isBlocked = blockedApps.any { it.packageName == packageName && it.isBlocked }

                    if (isBlocked) {
                        val budget = database.timeBudgetDao().getTimeBudget()
                        val remaining = budget?.remainingMinutes ?: 0.0f
                        
                        if (remaining <= 0.0f) {
                            Log.d(TAG, "Intercepted blocked app immediately: $packageName with 0 budget. Locking!")
                            lockActiveApp()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error evaluating accessibility event: ${e.message}")
                }
            }
        }
    }

    /**
     * Loops every 10 seconds to drain the time budget if a blocked app is active in the foreground
     */
    private fun startTimeBudgetDrainLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(10000) // check every 10 seconds
                val currentPackage = activePackage ?: continue
                if (currentPackage == packageName) continue

                try {
                    val blockedApps = database.blockedAppDao().getAllBlockedApps()
                    val isBlocked = blockedApps.any { it.packageName == currentPackage && it.isBlocked }

                    if (isBlocked) {
                        val budget = database.timeBudgetDao().getTimeBudget() ?: continue
                        val remaining = budget.remainingMinutes
                        
                        if (remaining > 0.0f) {
                            // 10 seconds = 10 / 60.0 of a minute = 0.1667 minutes
                            val drainAmount = 10.0f / 60.0f
                            val updatedRemaining = (remaining - drainAmount).coerceAtLeast(0.0f)
                            
                            val updated = budget.copy(
                                remainingMinutes = updatedRemaining,
                                lastDrainTimestamp = System.currentTimeMillis()
                            )
                            database.timeBudgetDao().insertTimeBudget(updated)
                            Log.d(TAG, "Drained $drainAmount mins from $currentPackage. Remaining: $updatedRemaining")
                            
                            if (updatedRemaining <= 0.0f) {
                                Log.d(TAG, "Budget hit 0 while using $currentPackage. Locking app!")
                                withContext(Dispatchers.Main) {
                                    lockActiveApp()
                                }
                            }
                        } else {
                            // remaining is 0, must lock
                            Log.d(TAG, "Remaining budget is 0 but blocked app is active. Locking app!")
                            withContext(Dispatchers.Main) {
                                lockActiveApp()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in budget drain loop: ${e.message}")
                }
            }
        }
    }

    private fun lockActiveApp() {
        // 1. Send Home key to minimize blocked app immediately
        performGlobalAction(GLOBAL_ACTION_HOME)
        
        // 2. Launch Interruption / Explain Screen in Latch
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("INTERRUPTED_PACKAGE", activePackage)
            putExtra("TRIGGER_EXPLAIN", true)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Latch Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "Latch Accessibility Service Destroyed")
    }
}
