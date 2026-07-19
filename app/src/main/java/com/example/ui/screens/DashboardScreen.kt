package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockedApp
import com.example.data.Checkpoint
import com.example.data.Goal
import com.example.data.TimeBudget
import com.example.ui.LatchViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(viewModel: LatchViewModel, interruptedPackage: String? = null) {
    val activeGoal by viewModel.activeGoal.collectAsState()
    val checkpoints by viewModel.activeCheckpoints.collectAsState()
    val budget by viewModel.timeBudget.collectAsState()
    val blockedApps by viewModel.allBlockedApps.collectAsState()
    val persona by viewModel.writingPersona.collectAsState()

    val isUsageGranted by viewModel.isUsagePermissionGranted.collectAsState()
    val isAccessibilityGranted by viewModel.isAccessibilityPermissionGranted.collectAsState()

    // Refresh permissions on resume
    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LatchBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LATCH",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = LatchInk,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (persona?.coldStart == false) "Persona: established" else "Persona: soft-mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = LatchInkMuted
                )
            }

            IconButton(
                onClick = { viewModel.navigateTo(Screen.Settings) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = LatchSurface),
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, LatchBorder, CircleShape)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = LatchInk)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // WARNING BANNER: Permissions Missing
        if (!isUsageGranted || !isAccessibilityGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, LatchOrange.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .clickable { viewModel.navigateTo(Screen.Onboarding) },
                colors = CardDefaults.cardColors(containerColor = LatchOrangeSoft),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = "Warning", tint = LatchOrange)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Locks inactive (Permissions missing)",
                            fontWeight = FontWeight.Bold,
                            color = LatchInk,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Tap here to grant system permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = LatchInkMuted
                        )
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Go", tint = LatchOrange)
                }
            }
        }

        // TIME BUDGET DIAL
        BudgetDialCard(budget = budget, onExplainClick = {
            // Pick first incomplete checkpoint or navigate to plan
            val nextCheckpoint = checkpoints.firstOrNull { !it.isCompleted }
            if (nextCheckpoint != null) {
                viewModel.navigateTo(Screen.ExplainSession(nextCheckpoint.id))
            } else if (activeGoal != null) {
                viewModel.navigateTo(Screen.PlanOverview)
            } else {
                viewModel.navigateTo(Screen.GoalsInput)
            }
        })

        Spacer(modifier = Modifier.height(16.dp))

        // INTERRUPTED ALERT BLOCK
        if (interruptedPackage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, LatchOrange.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = LatchOrangeSoft),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = LatchOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Interception Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LatchInk
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You were blocked from opening your latched app because you have 0 minutes of free time. Pass an active recall explain session to earn credit!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LatchInkMuted
                    )
                }
            }
        }

        // GOAL & STUDY COMPANION BLOCK
        StudyGoalCard(
            activeGoal = activeGoal,
            checkpoints = checkpoints,
            onAddGoalClick = { viewModel.navigateTo(Screen.GoalsInput) },
            onViewPlanClick = { viewModel.navigateTo(Screen.PlanOverview) },
            onCheckpointClick = { id -> viewModel.navigateTo(Screen.ExplainSession(id)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // LATCHED APPS DRAWER
        LatchedAppsCard(
            blockedApps = blockedApps,
            onManageClick = { viewModel.navigateTo(Screen.AppPicker) }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BudgetDialCard(budget: TimeBudget?, onExplainClick: () -> Unit) {
    val remaining = budget?.remainingMinutes ?: 15.0f
    val displayMins = remaining.roundToInt()
    val totalEarned = budget?.totalEarnedMinutes ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LatchBorder, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = LatchSurface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Remaining Social Minutes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = LatchInkMuted)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Large circular countdown text
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(LatchOrangeSoft, shape = CircleShape)
                    .border(1.dp, LatchOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayMins.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (displayMins == 0) LatchDanger else LatchOrange
                    )
                    Text(
                        text = "MINS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LatchInk
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Verified, contentDescription = "Earned", tint = LatchSuccess, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lifetime Unlocked: $totalEarned mins", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onExplainClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (displayMins == 0) LatchOrange else LatchInk
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("explain_to_unlock_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EditNote, contentDescription = "Explain", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (displayMins == 0) "Begin Explain Session" else "Earn More Minutes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StudyGoalCard(
    activeGoal: Goal?,
    checkpoints: List<Checkpoint>,
    onAddGoalClick: () -> Unit,
    onViewPlanClick: () -> Unit,
    onCheckpointClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LatchBorder, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = LatchSurface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Study Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = LatchInk
                )
                
                if (activeGoal != null) {
                    IconButton(onClick = onViewPlanClick) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "View Plan", tint = LatchOrange)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeGoal == null) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "No Goal",
                        tint = LatchInkMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No study plan active.",
                        fontWeight = FontWeight.Bold,
                        color = LatchInk,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Upload materials to generate checkpoints",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LatchInkMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddGoalClick,
                        colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                        shape = CircleShape,
                        modifier = Modifier.testTag("create_goal_button")
                    ) {
                        Text("Create Learning Goal", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                // Goal active
                Text(
                    text = activeGoal.topic,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = LatchOrange
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                val completedCount = checkpoints.count { it.isCompleted }
                val totalCheckpoints = checkpoints.size
                val progressFraction = if (totalCheckpoints > 0) completedCount.toFloat() / totalCheckpoints else 0.0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Learning Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LatchInk
                    )
                    Text(
                        "$completedCount of $totalCheckpoints Mastered",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LatchOrange
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = LatchOrange,
                    trackColor = LatchOrangeSoft,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Short list of checkpoints
                checkpoints.take(3).forEach { checkpoint ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onCheckpointClick(checkpoint.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (checkpoint.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (checkpoint.isCompleted) LatchSuccess else LatchInkMuted
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = checkpoint.subtopicName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (checkpoint.isCompleted) FontWeight.Medium else FontWeight.Normal,
                            color = if (checkpoint.isCompleted) LatchInkMuted else LatchInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!checkpoint.isCompleted) {
                            Text(
                                "Start",
                                style = MaterialTheme.typography.bodySmall,
                                color = LatchOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (totalCheckpoints > 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onViewPlanClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View all subtopics", color = LatchOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LatchedAppsCard(blockedApps: List<BlockedApp>, onManageClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LatchBorder, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = LatchSurface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = LatchInk, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Latched Applications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = LatchInk
                    )
                }

                TextButton(onClick = onManageClick) {
                    Text("Manage", color = LatchOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (blockedApps.isEmpty()) {
                Text(
                    text = "No apps blocked. Open 'Manage' to lock social or game packages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LatchInkMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    blockedApps.take(4).forEach { app ->
                        AssistChip(
                            onClick = {},
                            label = { Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = LatchOrangeSoft,
                                labelColor = LatchOrange
                            ),
                            border = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    if (blockedApps.size > 4) {
                        AssistChip(
                            onClick = onManageClick,
                            label = { Text("+${blockedApps.size - 4}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = LatchInk.copy(alpha = 0.05f),
                                labelColor = LatchInk
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}
