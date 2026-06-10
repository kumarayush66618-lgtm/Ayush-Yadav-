package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DiaryEntry
import com.example.ui.DiaryViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Elegant, high-contrast Dark theme tailored specifically for Smart Diary application
            SmartDiaryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DiaryAppScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Gorgeous Custom Obsidian Dark Theme with deep slate and colorful neon-like accent highlights
@Composable
fun SmartDiaryTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        background = Color(0xFF0F1115), // Deep Midnight Slate
        surface = Color(0xFF171921),    // Medium Slate Dark
        surfaceVariant = Color(0xFF222530), // Lighter Accent Slate
        primary = Color(0xFF6366F1),    // Neon Indigo
        onPrimary = Color.White,
        secondary = Color(0xFF10B981),  // Teal-Emerald Goodness
        tertiary = Color(0xFFF59E0B),   // Honey Yellow
        onBackground = Color(0xFFE2E8F0), // Off White text
        onSurface = Color(0xFFF1F5F9)
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

// Map categories to modern distinct accent colors
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "work" -> Color(0xFF38BDF8)     // Electric Cyan
        "personal" -> Color(0xFF34D399) // Mint Emerald
        "urgent" -> Color(0xFFF87171)   // Soft Crimson
        else -> Color(0xFFA78BFA)       // Light Lavender (General)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiaryAppScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // UI state observers
    val filteredEntries by viewModel.filteredEntries.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Dialog coordination
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    // Automatic Notification Permission request on startup for newer devices (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Permission denied. Task alarm alerts will display but cannot popup sound-system banners.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEntry = null
                    showEditorDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_entry_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Diary Entry")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            
            // Header Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val simpleDate = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
                        Text(
                            text = simpleDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Smart Notebook",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    // Simple Stats Indicator
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Alarms Active: ${upcomingTasks.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Search Field with matching outline and icons
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search task lists, diary details...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input"),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal Category filter selection row
                val categories = listOf("All", "Work", "Personal", "Urgent", "General")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { categoryName ->
                        val isSelected = selectedCategory == categoryName
                        val pillColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(pillColor)
                                .clickable { viewModel.setCategoryFilter(categoryName) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_${categoryName.lowercase()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (categoryName != "All") {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(categoryName))
                                            .padding(end = 6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // Central content region with smooth scrolling
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                
                // Section 1: Upcoming Tasks with active Alarms
                if (upcomingTasks.isNotEmpty() && selectedCategory == "All" && searchQuery.isEmpty()) {
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Upcoming Alarm Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(upcomingTasks) { task ->
                                    AlarmTaskCard(
                                        task = task,
                                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                        onCardClick = {
                                            editingEntry = task
                                            showEditorDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Recent and filtered Diary entries
                item {
                    Text(
                        text = if (selectedCategory == "All") "Notebook Entries" else "$selectedCategory Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (filteredEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Your notebook is empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap the '+' bubble below to record your first entry or set an alarm task.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        DiaryEntryRow(
                            entry = entry,
                            onToggleComplete = { viewModel.toggleTaskCompletion(entry) },
                            onDelete = { viewModel.deleteEntry(entry.id) },
                            onClick = {
                                editingEntry = entry
                                showEditorDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Dynamic Creation and Editing Overlay
    if (showEditorDialog) {
        NotebookEditorDialog(
            entry = editingEntry,
            onDismiss = { showEditorDialog = false },
            onSave = { title, content, type, category, alarmTime, isAlarmEnabled ->
                viewModel.saveEntry(
                    id = editingEntry?.id ?: 0,
                    title = title,
                    content = content,
                    type = type,
                    category = category,
                    alarmTime = alarmTime,
                    isAlarmEnabled = isAlarmEnabled
                )
                showEditorDialog = false
            }
        )
    }
}

// Gorgeous Card for tasks with active near-future alerts
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmTaskCard(
    task: DiaryEntry,
    onToggleComplete: () -> Unit,
    onCardClick: () -> Unit
) {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val alarmStr = task.alarmTime?.let { formatter.format(Date(it)) } ?: "No Alarm"

    Surface(
        modifier = Modifier
            .width(220.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCardClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, getCategoryColor(task.category).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Completed indicator
                IconButton(
                    onClick = { onToggleComplete() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (task.isTaskComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isTaskComplete) MaterialTheme.colorScheme.secondary else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Category pill on the top-right
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(getCategoryColor(task.category).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category.uppercase(),
                        fontSize = 9.sp,
                        color = getCategoryColor(task.category),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isTaskComplete) TextDecoration.LineThrough else TextDecoration.None
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = alarmStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Visual layout row representing each diary element
@Composable
fun DiaryEntryRow(
    entry: DiaryEntry,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault())
    val alarmText = entry.alarmTime?.let { formatter.format(Date(it)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("entry_item_${entry.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leftside icon indicating Note-taking / Task nature
                val typeIcon = if (entry.type == "Task") Icons.Default.Checklist else Icons.Default.StickyNote2
                val typeIconColor = if (entry.type == "Task") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(typeIconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        typeIcon,
                        contentDescription = entry.type,
                        tint = typeIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (entry.isTaskComplete) TextDecoration.LineThrough else TextDecoration.None
                        )

                        // Category tag indicator bullet
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(getCategoryColor(entry.category).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = entry.category,
                                fontSize = 9.sp,
                                color = getCategoryColor(entry.category),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val dateText = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
                    Text(
                        text = "Created: $dateText",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                if (entry.type == "Task") {
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            imageVector = if (entry.isTaskComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete Task",
                            tint = if (entry.isTaskComplete) MaterialTheme.colorScheme.secondary else Color.Gray
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            if (entry.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom alarm metadata block
            if (entry.isAlarmEnabled && alarmText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = "Scheduled alert active",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scheduled Alarm Reminder: $alarmText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Rich full-screen dialog overlay for creation/updating entries
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookEditorDialog(
    entry: DiaryEntry?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        type: String,
        category: String,
        alarmTime: Long?,
        isAlarmEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = entry != null

    // Editor field states
    var title by remember { mutableStateOf(entry?.title ?: "") }
    var content by remember { mutableStateOf(entry?.content ?: "") }
    var type by remember { mutableStateOf(entry?.type ?: "Note") } // "Note" or "Task"
    var category by remember { mutableStateOf(entry?.category ?: "General") } // "Work", "Personal", "Urgent", "General"
    var isAlarmEnabled by remember { mutableStateOf(entry?.isAlarmEnabled ?: false) }
    var alarmTimeMs by remember { mutableStateOf(entry?.alarmTime) }

    val showDatePicker = {
        val calendar = Calendar.getInstance()
        alarmTimeMs?.let { calendar.timeInMillis = it }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Instantly follow up with Time Picker
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(
                                context,
                                "Warning: Alarms must be scheduled in the future.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            alarmTimeMs = calendar.timeInMillis
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isEditMode) "Edit Notebook Entry" else "New Entry & Task",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Segmented toggle: Note vs Task
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val segments = listOf("Note", "Task")
                    segments.forEach { seg ->
                        val isSel = type == seg
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { type = seg }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = seg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title field with test tag
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detail content field with test tag
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write diary thoughts, notes or task details...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("entry_content_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category tag selector list
                Text(
                    text = "Category Tag",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                val tags = listOf("General", "Work", "Personal", "Urgent")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val isSel = category.lowercase() == tag.lowercase()
                        val tagColor = getCategoryColor(tag)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) tagColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { category = tag }
                                .border(
                                    1.dp,
                                    if (isSel) tagColor else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) tagColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Alarm Scheduler Toggle Switch Module
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = if (isAlarmEnabled) MaterialTheme.colorScheme.tertiary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Set Alarm Reminder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trigger notification alert on time",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { isAlarmEnabled = it },
                        modifier = Modifier.testTag("alarm_toggle_switch")
                    )
                }

                if (isAlarmEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val timeString = alarmTimeMs?.let {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
                    } ?: "Not Scheduled yet"

                    OutlinedButton(
                        onClick = { showDatePicker() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_datetime_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (alarmTimeMs != null) "Time: $timeString" else "Tap to Set Alarm Date & Time",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Cancel or Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Title cannot be blank", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isAlarmEnabled && alarmTimeMs == null) {
                                Toast.makeText(context, "Please set alarm date and time", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSave(title, content, type, category, alarmTimeMs, isAlarmEnabled)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_entry_button")
                    ) {
                        Text("Save Entry")
                    }
                }
            }
        }
    }
}
