package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BlockedApp
import com.example.ui.LatchViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun AppPickerScreen(viewModel: LatchViewModel) {
    val blockedApps by viewModel.allBlockedApps.collectAsState()
    
    // Hardcoded list of popular distraction apps for fast click setup
    val popularApps = remember {
        listOf(
            AppInfo("TikTok", "com.zhiliaoapp.musically"),
            AppInfo("Instagram", "com.instagram.android"),
            AppInfo("YouTube", "com.google.android.youtube"),
            AppInfo("X / Twitter", "com.twitter.android"),
            AppInfo("Reddit", "com.reddit.frontpage"),
            AppInfo("Snapchat", "com.snapchat.android"),
            AppInfo("Netflix", "com.netflix.mediaclient"),
            AppInfo("Facebook", "com.facebook.katana"),
            AppInfo("Chrome", "com.android.chrome"),
            AppInfo("Brawl Stars", "com.supercell.brawlstars"),
            AppInfo("Roblox", "com.roblox.client"),
            AppInfo("Discord", "com.discord")
        )
    }

    var customAppName by remember { mutableStateOf("") }
    var customPackageName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = popularApps.filter {
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LatchBg)
            .padding(16.dp)
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
                "Latch Applications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = LatchInk
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Select which applications on this device should be locked when you have zero remaining social minutes.",
            style = MaterialTheme.typography.bodyMedium,
            color = LatchInkMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search popular distractors...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = LatchInkMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LatchOrange,
                unfocusedBorderColor = LatchBorder
            ),
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().testTag("app_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ADD CUSTOM APP SECTION
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
                    "Add Custom Package Lock",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LatchOrange
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customAppName,
                        onValueChange = { customAppName = it },
                        placeholder = { Text("App Name (e.g. Games)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LatchOrange,
                            unfocusedBorderColor = LatchBorder
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it },
                        placeholder = { Text("com.package.name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LatchOrange,
                            unfocusedBorderColor = LatchBorder
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1.2f).height(52.dp),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (customAppName.isNotEmpty() && customPackageName.isNotEmpty()) {
                            viewModel.blockPackage(customPackageName.trim(), customAppName.trim())
                            customAppName = ""
                            customPackageName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LatchInk),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("add_custom_app_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Package Lock", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // POPULAR APPS LIST
        Text(
            "Distraction Packages",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LatchInk
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredApps) { app ->
                val isBlocked = blockedApps.any { it.packageName == app.packageName }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isBlocked) LatchOrange.copy(alpha = 0.2f) else LatchBorder,
                            RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBlocked) LatchOrangeSoft else LatchSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = LatchInk
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = LatchInkMuted
                            )
                        }
                        
                        Switch(
                            checked = isBlocked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    viewModel.blockPackage(app.packageName, app.name)
                                } else {
                                    viewModel.unblockPackage(app.packageName)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LatchOrange,
                                uncheckedThumbColor = LatchInkMuted,
                                uncheckedTrackColor = LatchBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

data class AppInfo(val name: String, val packageName: String)
