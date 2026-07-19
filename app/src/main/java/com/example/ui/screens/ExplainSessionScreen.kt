package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
fun ExplainSessionScreen(viewModel: LatchViewModel, checkpointId: Int) {
    val checkpoints by viewModel.activeCheckpoints.collectAsState()
    val checkpoint = checkpoints.firstOrNull { it.id == checkpointId }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val gradeResult by viewModel.gradeResult.collectAsState()

    var explanationText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LatchBg)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // TOP ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LatchInk)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Active Teach-Back",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = LatchInk
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (checkpoint == null) {
            Text("Error: Checkpoint not found.", color = LatchDanger)
            return
        }

        // STEP INDICATOR
        Text(
            text = "SUBTOPIC: ${checkpoint.subtopicName}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = LatchOrange
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // THE READING PASSAGE CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LatchBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = LatchSurface),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "1. Read and digest",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LatchOrange
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = checkpoint.extractText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LatchInk,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // THE PROMPT CALL CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LatchOrange.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = LatchOrangeSoft),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = "Prompt", tint = LatchOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Teach-back Prompt",
                        fontWeight = FontWeight.Bold,
                        color = LatchOrange,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = checkpoint.promptText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LatchInk,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // USER EXPLANATION INPUT
        Text(
            text = "2. Explain in your own words:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchInk
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = explanationText,
            onValueChange = { explanationText = it },
            placeholder = { Text("Paraphrase the core ideas. Typos/gaps are okay, but write in YOUR active style. Exact copy-pastes will be rejected.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("explain_answer_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LatchOrange,
                unfocusedBorderColor = LatchBorder
            ),
            shape = RoundedCornerShape(24.dp),
            enabled = gradeResult == null // disable editing once graded, unless they trigger a redo
        )

        // Loading Indicator
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LatchOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Latch Grader is evaluating understanding & authenticity...", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                }
            }
        }

        // Error message
        errorMsg?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Error, contentDescription = "Error", tint = LatchDanger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(msg, color = LatchDanger, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // GRADING RESULT ANIMATION DISPLAY
        AnimatedVisibility(
            visible = gradeResult != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            gradeResult?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (result.mustRedo) LatchOrange.copy(alpha = 0.2f) else LatchSuccess.copy(alpha = 0.2f),
                            RoundedCornerShape(28.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.mustRedo) LatchOrangeSoft else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (result.mustRedo) Icons.Filled.Error else Icons.Filled.CheckCircle,
                                contentDescription = "Result Icon",
                                tint = if (result.mustRedo) LatchOrange else LatchSuccess,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (result.mustRedo) "Revision Required" else "Subtopic Mastered!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (result.mustRedo) LatchOrange else LatchSuccess
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = result.feedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LatchInk,
                            lineHeight = 20.sp
                        )

                        if (result.missingPoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Core Points to cover:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = LatchInk
                            )
                            result.missingPoints.forEach { pt ->
                                Text("• $pt", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Understanding: ${(result.understandingScore * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                                Text("Style Authenticity: ${(result.authenticityScore * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                            }
                            
                            if (result.earnedMinutes > 0) {
                                Badge(
                                    containerColor = LatchSuccess,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        "+${result.earnedMinutes} MINS EARNED",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (result.mustRedo) {
                            Button(
                                onClick = { viewModel.clearActiveGradeResult() },
                                colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("redo_session_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rewrite & Redo", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.clearActiveGradeResult()
                                    viewModel.navigateTo(Screen.Dashboard)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LatchSuccess),
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("unlock_earned_button")
                            ) {
                                Icon(Icons.Filled.Celebration, contentDescription = "Unlock", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Collect Unlock Credit", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BOTTOM MAIN CTAS (Only show if not graded yet)
        if (gradeResult == null && !isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Text("Skip for now", color = LatchInkMuted, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.submitTeachBack(checkpointId, explanationText) },
                    colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                    shape = CircleShape,
                    modifier = Modifier
                        .width(180.dp)
                        .height(52.dp)
                        .testTag("submit_teachback_button")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Submit", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Recall", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
