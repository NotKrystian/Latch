package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.LatchViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun GoalsInputScreen(viewModel: LatchViewModel) {
    var topic by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }
    var targetDays by remember { mutableFloatStateOf(7.0f) }
    var targetScore by remember { mutableFloatStateOf(85.0f) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LatchBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                "Create Learning Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = LatchInk
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Latch works best when you set a specific topic and provide reference study materials. Gemini will digest them to generate reading extracts for your lock screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = LatchInkMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // INPUT 1: Topic
        Text(
            "1. What are you studying?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchInk
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            placeholder = { Text("e.g. AP Biology Photosynthesis, Calculus Derivatives...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LatchOrange,
                unfocusedBorderColor = LatchBorder
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("goal_topic_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // INPUT 2: Materials Text
        Text(
            "2. Paste study notes, slides, or syllabus (optional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchInk
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = materials,
            onValueChange = { materials = it },
            placeholder = { Text("Paste text here to help Gemini generate custom, highly accurate reading cards tailored exactly to what your class is learning...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LatchOrange,
                unfocusedBorderColor = LatchBorder
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("goal_materials_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SLIDER 1: Target Date (Days from now)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Target Timeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = LatchInk)
            Text("${targetDays.toInt()} days from now", fontWeight = FontWeight.Bold, color = LatchOrange, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = targetDays,
            onValueChange = { targetDays = it },
            valueRange = 1.0f..30.0f,
            colors = SliderDefaults.colors(
                thumbColor = LatchOrange,
                activeTrackColor = LatchOrange,
                inactiveTrackColor = LatchOrangeSoft
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SLIDER 2: Target Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Target Mastery Score", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = LatchInk)
            Text("${targetScore.toInt()}%", fontWeight = FontWeight.Bold, color = LatchOrange, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = targetScore,
            onValueChange = { targetScore = it },
            valueRange = 50.0f..100.0f,
            colors = SliderDefaults.colors(
                thumbColor = LatchOrange,
                activeTrackColor = LatchOrange,
                inactiveTrackColor = LatchOrangeSoft
            )
        )

        // Error message
        errorMsg?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Error, contentDescription = "Error", tint = LatchDanger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(msg, color = LatchDanger, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LatchOrange)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Gemini is generating your curriculum...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LatchOrange
                    )
                    Text(
                        "Creating custom reading passages and prompts...",
                        style = MaterialTheme.typography.bodySmall,
                        color = LatchInkMuted
                    )
                }
            }
        } else {
            Button(
                onClick = {
                    val targetTimestamp = System.currentTimeMillis() + (targetDays.toLong() * 24 * 60 * 60 * 1000)
                    viewModel.generatePlan(
                        topic = topic,
                        materials = materials,
                        targetDate = targetTimestamp,
                        targetScore = targetScore.toInt()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_plan_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Study Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
