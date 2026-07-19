package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.LatchViewModel
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(viewModel: LatchViewModel) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    
    val isUsageGranted by viewModel.isUsagePermissionGranted.collectAsState()
    val isAccessibilityGranted by viewModel.isAccessibilityPermissionGranted.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    var sampleText by remember { mutableStateOf("") }

    // Re-check permissions when screen comes back into focus
    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LatchBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                1 -> {
                    // STEP 1: Pitch
                    Spacer(modifier = Modifier.height(30.dp))
                    LatchBrandingLogo(modifier = Modifier.size(160.dp))
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "LATCH",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = LatchInk,
                        letterSpacing = MaterialTheme.typography.displayMedium.letterSpacing
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explain to learn. Redo to unlock.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LatchOrange,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Latch locks your specified apps (like social media or games) when your study budget runs empty. To earn more minutes, read a short extract and explain the ideas in your own words. No multiple choice. Real active recall.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LatchInkMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = { step = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("get_started_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Get Started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
                2 -> {
                    // STEP 2: Permissions Gate
                    Text(
                        text = "Self-Control System",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = LatchInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configure the firm-lock permissions. These let Latch track study limits and lock restricted apps offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LatchInkMuted,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // CARD 1: Usage Stats
                    PermissionCard(
                        title = "1. Usage Statistics",
                        description = "Required to track how many minutes you spend inside latched applications.",
                        isGranted = isUsageGranted,
                        onGrantClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // CARD 2: Accessibility overlay
                    PermissionCard(
                        title = "2. Accessibility Overlay",
                        description = "Intercepts restricted apps when your credit runs dry and redirects to active teach-back recalls.",
                        isGranted = isAccessibilityGranted,
                        onGrantClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Note: Revoking these permissions later disables Latch locking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LatchInkMuted,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { step = 1 }) {
                            Text("Back", color = LatchInkMuted, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.checkPermissions(); step = 3 },
                            colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                            shape = CircleShape,
                            modifier = Modifier
                                .width(160.dp)
                                .height(52.dp)
                                .testTag("permission_next_button")
                        ) {
                            Text("Next Step", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                3 -> {
                    // STEP 3: Bootstrap Sample
                    Text(
                        text = "Write your Persona",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = LatchInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explain something you already know well in 3-4 sentences. This establishes your initial Writing Persona. Gemini will analyze your tone, vocabulary, and phrasing to reject future AI copy-pastes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LatchInkMuted,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
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
                                "Prompts you could answer:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LatchOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Explain how a bicycle brakes.", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                            Text("• Explain why gravity makes things fall down.", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                            Text("• Explain why bread rises in the oven.", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = sampleText,
                        onValueChange = { sampleText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("bootstrap_sample_input"),
                        placeholder = { Text("Write your explanation here... (be natural, typos are okay!)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LatchOrange,
                            unfocusedBorderColor = LatchBorder
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    
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
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (isLoading) {
                        CircularProgressIndicator(color = LatchOrange)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Analyzing your writing style via Gemini AI...", style = MaterialTheme.typography.bodySmall, color = LatchInkMuted)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { step = 2 }) {
                                Text("Back", color = LatchInkMuted, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { viewModel.bootstrapPersona(sampleText) },
                                colors = ButtonDefaults.buttonColors(containerColor = LatchOrange),
                                shape = CircleShape,
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(52.dp)
                                    .testTag("bootstrap_submit_button")
                            ) {
                                Text("Complete Setup", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(
                            onClick = { viewModel.completeOnboarding() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Skip Bootstrap (Use Cold-Start Mode)", color = LatchOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LatchBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = LatchSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LatchInk)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = LatchInkMuted)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isGranted) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = LatchSuccess)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Permission Active", color = LatchSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Icon(Icons.Filled.Error, contentDescription = "Missing", tint = LatchDanger)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Not Configured", color = LatchDanger, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            IconButton(
                onClick = onGrantClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isGranted) LatchOrangeSoft else LatchOrange
                ),
                modifier = Modifier
                    .size(52.dp)
                    .border(1.dp, if (isGranted) LatchOrange.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Configure Permission",
                    tint = if (isGranted) LatchOrange else Color.White
                )
            }
        }
    }
}

@Composable
fun LatchBrandingLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width * 0.35f

        // Draw outer orange lock hoop
        drawArc(
            color = LatchOrange,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius * 1.3f),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 24f)
        )

        // Draw soft orange lock base plate
        drawRoundRect(
            color = LatchInk,
            topLeft = Offset(centerX - radius * 1.2f, centerY - radius * 0.3f),
            size = Size(radius * 2.4f, radius * 1.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
        )

        // Draw center orange lock core circle
        drawCircle(
            color = LatchOrange,
            radius = radius * 0.25f,
            center = Offset(centerX, centerY + radius * 0.25f)
        )

        // Draw lock key bar
        drawRect(
            color = LatchOrange,
            topLeft = Offset(centerX - 10f, centerY + radius * 0.25f),
            size = Size(20f, radius * 0.45f)
        )
    }
}
