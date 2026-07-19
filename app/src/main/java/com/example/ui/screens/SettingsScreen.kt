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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LatchViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun SettingsScreen(viewModel: LatchViewModel) {
    val customKey by viewModel.customApiKey.collectAsState()
    val persona by viewModel.writingPersona.collectAsState()
    val budget by viewModel.timeBudget.collectAsState()

    var apiKeyText by remember { mutableStateOf(customKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

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
                "Latch Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = LatchInk
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 1: GEMINI API KEY
        Text(
            "GEMINI API CREDENTIALS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchOrange
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LatchBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = LatchSurface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "To use high-fidelity AI understanding and style authenticity verification, Latch connects directly to Gemini. Enter your key securely below or configure it in AI Studio Secrets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LatchInkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = { Text("AI Studio Gemini Key...") },
                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = "Key", tint = LatchOrange) },
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle Key visibility"
                            )
                        }
                    },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LatchOrange,
                        unfocusedBorderColor = LatchBorder
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.saveCustomApiKey(apiKeyText.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.End).testTag("save_settings_button")
                ) {
                    Text("Save Key", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: WRITING STYLE PERSONA
        Text(
            "ESTABLISHED STYLE PERSONA",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchOrange
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LatchBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = LatchSurface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (persona == null || persona!!.coldStart) {
                    Text(
                        "Persona State: Cold-Start Mode",
                        fontWeight = FontWeight.Bold,
                        color = LatchInk,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "No style parameters locked yet. Latch is using soft authenticity mode, checking for exact copy-pasting but not restricting writing voice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LatchInkMuted
                    )
                } else {
                    val p = persona!!
                    Text(
                        "Voice Style: Active",
                        fontWeight = FontWeight.Bold,
                        color = LatchSuccess,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vocabulary level", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                        Text(p.vocabularyLevel, fontWeight = FontWeight.Bold, color = LatchInk, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sentence Length", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                        Text("${p.sentenceLength} words", fontWeight = FontWeight.Bold, color = LatchInk, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tone profile", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                        Text(p.toneDescription, fontWeight = FontWeight.Bold, color = LatchInk, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = LatchBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Compact Summary:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = LatchInk)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = p.personaSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = LatchInkMuted,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.resetWritingPersona() },
                    colors = ButtonDefaults.buttonColors(containerColor = LatchInk),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("reset_persona_button")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Persona Samples", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: TIME BUDGET TUNING (DEBUG & DEMO UTILS)
        Text(
            "LOCK BUDGET DEMO CONTROLS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchOrange
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LatchBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = LatchSurface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Adjust remaining social minutes. Set to 0 to test active app-blocking intercept overlays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LatchInkMuted
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.addManualMinutes(10.0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = LatchSuccess),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+10 min", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.drainMinutes(5.0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = LatchDanger),
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Drain")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-5 min", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { viewModel.drainMinutes(100.0f) },
                    colors = ButtonDefaults.buttonColors(containerColor = LatchInk),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Force Drain to 0 mins", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
