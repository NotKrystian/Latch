package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LatchViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun PlanOverviewScreen(viewModel: LatchViewModel) {
    val activeGoal by viewModel.activeGoal.collectAsState()
    val checkpoints by viewModel.activeCheckpoints.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LatchBg)
            .padding(16.dp)
    ) {
        // TOP ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LatchInk)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Study Checklist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = LatchInk
                )
            }
            
            // Delete Goal Button
            activeGoal?.let { goal ->
                IconButton(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        viewModel.navigateTo(Screen.Dashboard)
                    }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Goal", tint = LatchDanger)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeGoal == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No active goal found.", color = LatchInkMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.navigateTo(Screen.GoalsInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                    shape = CircleShape
                ) {
                    Text("Create Goal", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val goal = activeGoal!!
            
            Text(
                text = "Topic: ${goal.topic}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LatchOrange
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target score: ${goal.targetScore}% mastery. Earn 10 minutes of unlocked apps for each completed subtopic below.",
                style = MaterialTheme.typography.bodyMedium,
                color = LatchInkMuted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // LIST OF CHECKPOINTS
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(checkpoints) { index, checkpoint ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (checkpoint.isCompleted) LatchOrange.copy(alpha = 0.1f) else LatchBorder,
                                RoundedCornerShape(24.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (checkpoint.isCompleted) LatchOrangeSoft.copy(alpha = 0.5f) else LatchSurface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (checkpoint.isCompleted) LatchSuccess else LatchOrange,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = checkpoint.subtopicName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = LatchInk
                                    )
                                }

                                if (checkpoint.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Mastered",
                                        tint = LatchSuccess
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Short extract preview
                            Text(
                                text = checkpoint.extractText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LatchInkMuted,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (checkpoint.isCompleted) {
                                Text(
                                    "Mastered with ${ (checkpoint.masteryScore * 100).toInt() }% score",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LatchSuccess
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.navigateTo(Screen.ExplainSession(checkpoint.id)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(38.dp)
                                        .testTag("start_checkpoint_${checkpoint.id}")
                                ) {
                                    Icon(Icons.Filled.School, contentDescription = "Study", modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Begin Recall", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
