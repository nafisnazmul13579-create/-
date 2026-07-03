package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import com.example.network.ImageGenerator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.Project
import com.example.data.Scene
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Color Scheme Definition for Studio Design
val DarkSlateBg = Color(0xFFFDF8F6)
val CardSurface = Color(0xFFFFFFFF)
val GoldAccent = Color(0xFF6750A4)
val NeonCyan = Color(0xFFD0BCFF)
val TextPrimary = Color(0xFF1D1B1E)
val TextSecondary = Color(0xFF49454F)

// Preset Stories
val storyPresets = listOf(
    Pair("Cyberpunk Rogue", "A rogue detective walks down a rain-slicked cyberpunk street illuminated by flickering pink neon lights. He stops in front of a dark holographic alleyway, adjusting his cybernetic eye. Suddenly, a hover-bike zooms overhead, casting sharp shadows on the wet pavement."),
    Pair("Cosmic Voyager", "A brave astronaut stands on the edge of a deep red Martian canyon, looking up at two giant moons. A dusty crimson wind swirls around her pressure suit as she activates her scanner. Beneath the red sand, a glowing ancient alien relic begins to hum with bright blue energy."),
    Pair("Whispering Woods", "A young explorer ventures into a luminous forest where trees glow with golden fireflies. She discovers a hidden mystical pool reflecting a sky full of shooting stars. When she touches the water, ancient glowing ruins rise slowly from the deep bank.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HMStudioApp(
    viewModel: MainViewModel,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onNavigateWithProject: (Screen, Long) -> Unit
) {
    val context = LocalContext.current
    val genState by viewModel.generationState.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    // Trigger sequential generation routing
    LaunchedEffect(genState) {
        if (genState is GenerationState.Preparing) {
            onNavigate(Screen.Generating)
        } else if (genState is GenerationState.Finished) {
            val activeId = viewModel.currentGeneratingProject.value?.id
            if (activeId != null) {
                viewModel.loadProject(activeId)
                onNavigateWithProject(Screen.Timeline, activeId)
                viewModel.resetGeneration()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        when (currentScreen) {
            Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onCreateNew = { onNavigate(Screen.Create) },
                    onOpenProject = { id ->
                        viewModel.loadProject(id)
                        onNavigateWithProject(Screen.Timeline, id)
                    }
                )
            }
            Screen.Create -> {
                CreateProjectScreen(
                    viewModel = viewModel,
                    onBack = { onNavigate(Screen.Home) }
                )
            }
            Screen.Generating -> {
                GeneratingScreen(
                    viewModel = viewModel,
                    onCancel = {
                        viewModel.resetGeneration()
                        onNavigate(Screen.Home)
                    }
                )
            }
            Screen.Timeline -> {
                TimelineScreen(
                    viewModel = viewModel,
                    onBack = { onNavigate(Screen.Home) }
                )
            }
        }
    }
}

// -----------------------------------------------------
// 1. HOME SCREEN
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCreateNew: () -> Unit,
    onOpenProject: (Long) -> Unit
) {
    val projects by viewModel.allProjects.collectAsState()
    val context = LocalContext.current
    var showApiKeyInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Custom high-fidelity Bento Grid header row matching HTML layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSlateBg)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRO PLATFORM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = GoldAccent
                    )
                    Text(
                        text = "HM AI",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                }
                
                IconButton(
                    onClick = { showApiKeyInfo = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE7E0EC))
                        .border(1.dp, Color(0xFFCAC4D0), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(GoldAccent, NeonCyan)
                                )
                            )
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNew,
                containerColor = GoldAccent,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("create_new_button"),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NEW PROJECT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        },
        containerColor = DarkSlateBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bento Feature Card: Hero Section with Live Scene Status indicator
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp))
                        .background(Color(0xFF1D1B1E))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_hero),
                        contentDescription = "Studio Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    
                    // Top-left live badge from Bento Grid HTML
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(100.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                        Text(
                            "SCENE 03 GENERATING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    // Bottom-right 4K active badge from HTML
                    Surface(
                        color = Color(0xFFD0BCFF),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Text(
                            "4K ACTIVE",
                            color = Color(0xFF381E72),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Next-Gen Video Generation",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Create stunning films from stories using high-quality free models",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Bento Grid Statistics Cards (Model Select & Scene Count)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Bento Card: Model Select
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "MODEL SELECT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F),
                                letterSpacing = 1.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Flux.1 Turbo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B1E)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                    }

                    // Right Bento Card: Scene Count
                    val totalScenesCount = projects.sumOf { project ->
                        val sentences = project.originalPrompt.split(Regex("[.!?]")).filter { it.trim().isNotEmpty() }
                        if (sentences.isEmpty()) 1 else sentences.size
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "SCENE COUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D),
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = String.format("%02d", totalScenesCount),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF21005D)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Scenes",
                                    fontSize = 12.sp,
                                    color = Color(0xFF21005D).copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Project History Title
            item {
                Text(
                    "MY CINEMATIC TIMELINES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Projects List
            if (projects.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.VideoLibrary,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Your movie studio is empty",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Write a story or prompt to generate your first editable video timeline.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onCreateNew,
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = GoldAccent),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                        ) {
                            Text("Create Project")
                        }
                    }
                }
            } else {
                itemsIndexed(projects) { index, project ->
                    ProjectCard(
                        project = project,
                        onClick = { onOpenProject(project.id) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }
            
            // Decorative spacer
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Settings / Key Info Dialog
        if (showApiKeyInfo) {
            Dialog(onDismissRequest = { showApiKeyInfo = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "HM AI System Platform",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "HM AI uses Google Gemini to split stories and generate optimized visual triggers. For actual drawing, we call the best free distributed GPU image models.\n\nAPI keys are managed via the Secrets panel in AI Studio and injected safely through BuildConfig.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showApiKeyInfo = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("DISMISS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder/Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEADDFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Movie,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    project.originalPrompt,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Color(0xFFE57373))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?", color = TextPrimary) },
            text = { Text("This will permanently remove the timeline, images, and scene sequence from your database.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                    Text("CANCEL")
                }
            },
            containerColor = CardSurface
        )
    }
}

// -----------------------------------------------------
// 2. CREATE PROJECT SCREEN
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var storyPrompt by remember { mutableStateOf("") }
    var selectedStyleModel by remember { mutableStateOf("flux-realism") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var selectedVisualPreset by remember { mutableStateOf("cinematic") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CREATE TIMELINE", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSlateBg,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkSlateBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Project / Movie Title") },
                placeholder = { Text("e.g. Cybernetic Dawn") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    focusedLabelColor = GoldAccent,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_title_input"),
                singleLine = true
            )

            // Story Prompt Input
            OutlinedTextField(
                value = storyPrompt,
                onValueChange = { storyPrompt = it },
                label = { Text("Enter your story script or prompts") },
                placeholder = { Text("Write sentences. HM AI will split your paragraphs into distinct scenes, generate prompts, automatically select models, and construct an interactive video timeline...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    focusedLabelColor = GoldAccent,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .testTag("story_prompt_input")
            )

            // Story presets
            Text(
                "OR SELECT A CINEMATIC PRESET",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(storyPresets) { _, preset ->
                    Surface(
                        onClick = {
                            title = preset.first
                            storyPrompt = preset.second
                        },
                        color = CardSurface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (title == preset.first) GoldAccent else Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            preset.first,
                            color = if (title == preset.first) GoldAccent else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Model Selection
            Text(
                "DEFAULT IMAGE BASE MODEL",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            val modelList = listOf(
                Pair("flux-realism", "Flux Realism 📷"),
                Pair("flux-anime", "Flux Anime 🌸"),
                Pair("flux-3d", "Flux 3D CGI 🧸"),
                Pair("dreamshaper", "Dreamshaper 🦄"),
                Pair("cyberpunk", "Cyberpunk Neon ⚡")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modelList.forEach { model ->
                    val selected = selectedStyleModel == model.first
                    Surface(
                        onClick = { selectedStyleModel = model.first },
                        color = if (selected) GoldAccent.copy(alpha = 0.15f) else CardSurface,
                        border = BorderStroke(1.dp, if (selected) GoldAccent else Color.Transparent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            model.second,
                            color = if (selected) GoldAccent else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Aspect Ratio selector
            Text(
                "CINEMATIC ASPECT RATIO",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("16:9" to Icons.Default.AspectRatio, "9:16" to Icons.Default.PhoneAndroid, "1:1" to Icons.Default.CropSquare).forEach { ratio ->
                    val selected = selectedAspectRatio == ratio.first
                    Surface(
                        onClick = { selectedAspectRatio = ratio.first },
                        color = if (selected) GoldAccent.copy(alpha = 0.15f) else CardSurface,
                        border = BorderStroke(1.dp, if (selected) GoldAccent else Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(ratio.second, contentDescription = null, tint = if (selected) GoldAccent else TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                ratio.first,
                                color = if (selected) GoldAccent else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Visual Style Preset selector
            Text(
                "VISUAL STYLE PRESET (APPLIED PROJECT-WIDE)",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            val presetList = listOf(
                "none" to "Default Style 🎨",
                "cinematic" to "Cinematic 🎬",
                "cartoon" to "Cartoon/CGI 🧸",
                "photorealistic" to "Photorealistic 📷",
                "vintage" to "Vintage Film 🎞️"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetList.forEach { preset ->
                    val selected = selectedVisualPreset == preset.first
                    Surface(
                        onClick = { selectedVisualPreset = preset.first },
                        color = if (selected) GoldAccent.copy(alpha = 0.15f) else CardSurface,
                        border = BorderStroke(1.dp, if (selected) GoldAccent else TextSecondary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            preset.second,
                            color = if (selected) GoldAccent else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    if (storyPrompt.isBlank()) {
                        title = "Need story!"
                    } else {
                        viewModel.createAndGenerateProject(title, storyPrompt, selectedStyleModel, selectedVisualPreset)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_timeline_button")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "CONSTRUCT SEQUENTIAL TIMELINE",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// -----------------------------------------------------
// 3. GENERATING SCREEN (SEQUENTIAL LOOP VISUALIZER)
// -----------------------------------------------------
@Composable
fun GeneratingScreen(
    viewModel: MainViewModel,
    onCancel: () -> Unit
) {
    val project by viewModel.currentGeneratingProject.collectAsState()
    val scenes by viewModel.currentGeneratingScenes.collectAsState()
    val currentIndex by viewModel.currentGeneratingIndex.collectAsState()
    val genState by viewModel.generationState.collectAsState()

    val totalScenes = scenes.size
    val currentScene = scenes.getOrNull(currentIndex)

    Scaffold(
        containerColor = DarkSlateBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "SEQUENTIAL TIMELINE ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldAccent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        project?.title ?: "Constructing Storyboard...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    "${(currentIndex + 1).coerceAtMost(totalScenes)} / $totalScenes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            // Realtime Sequential Status Display
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: current scene sentence being converted
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = CardSurface.copy(alpha = 0.5f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "ACTIVE SCENE PROCESSING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            currentScene?.sentence ?: "Parsing stories into narrative scenes...",
                            fontSize = 15.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                    }

                    // Middle: Animated display of active operation
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .drawBehind {
                                drawCircle(
                                    color = GoldAccent.copy(alpha = 0.1f),
                                    style = Stroke(width = 8.dp.toPx())
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = genState) {
                            is GenerationState.Preparing -> {
                                CircularProgressIndicator(color = GoldAccent, strokeWidth = 6.dp)
                            }
                            is GenerationState.GeneratingImage -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = GoldAccent, strokeWidth = 6.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Drawing photo...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "Attempting 4K",
                                        fontSize = 10.sp,
                                        color = GoldAccent
                                    )
                                }
                            }
                            is GenerationState.ConvertingVideo -> {
                                val infiniteTransition = rememberInfiniteTransition()
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .graphicsLayer { rotationZ = rotation },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Text(
                                    "CONVERTING VIDEO\n${(state.progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan,
                                    textAlign = TextAlign.Center
                                )
                            }
                            is GenerationState.AddingToTimeline -> {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            is GenerationState.Error -> {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            else -> {}
                        }
                    }

                    // Bottom: Live image render output
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when (genState) {
                                is GenerationState.GeneratingImage -> "Resolving best free AI model..."
                                is GenerationState.ConvertingVideo -> "Slicing frames at 60fps & adding zoom triggers..."
                                is GenerationState.AddingToTimeline -> "Successfully compiled. Sliding into Timeline!"
                                else -> "Analyzing script syntax..."
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Mini visualizer of the image as it resolves!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentScene != null && currentScene.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentScene.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Realtime preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Photo,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Real-time generated scene cards at the bottom
            Text(
                "COMPILED TIMELINE SCENES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                itemsIndexed(scenes) { idx, scene ->
                    val isCurrent = idx == currentIndex
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) GoldAccent.copy(alpha = 0.15f) else CardSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) GoldAccent else if (scene.isGenerated) Color.Green.copy(alpha = 0.5f) else Color.Transparent
                        ),
                        modifier = Modifier.width(140.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (scene.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(scene.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "SCENE ${idx + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrent) GoldAccent else TextPrimary
                                )
                                Text(
                                    scene.modelName.uppercase(),
                                    fontSize = 9.sp,
                                    color = if (scene.isGenerated) Color.Green else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Cancel action
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("ABORT TIMELINE GENERATION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// -----------------------------------------------------
// 4. TIMELINE SCREEN (CINEMATIC MOVIE EDITOR STUDIO)
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val project by viewModel.activeProject.collectAsState()
    val scenes by viewModel.activeScenes.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current

    // Timeline player state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingSceneIndex by remember { mutableStateOf(0) }
    var playbackTimeSeconds by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Editor sheets
    var editingScene by remember { mutableStateOf<Scene?>(null) }
    var showMusicSelector by remember { mutableStateOf(false) }

    // Ken burns camera movement animation values
    val animatedScale = remember { Animatable(1.0f) }
    val animatedOffsetX = remember { Animatable(0f) }

    val currentScene = scenes.getOrNull(currentPlayingSceneIndex)

    // Cinematic pan/zoom loop when a scene is active
    LaunchedEffect(currentPlayingSceneIndex, isPlaying) {
        if (isPlaying && currentScene != null) {
            animatedScale.snapTo(1.0f)
            animatedOffsetX.snapTo(0f)
            when (currentScene.effect) {
                "Zoom In" -> {
                    animatedScale.animateTo(
                        targetValue = 1.15f,
                        animationSpec = tween(
                            durationMillis = currentScene.durationSeconds * 1000,
                            easing = LinearEasing
                        )
                    )
                }
                "Zoom Out" -> {
                    animatedScale.snapTo(1.15f)
                    animatedScale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(
                            durationMillis = currentScene.durationSeconds * 1000,
                            easing = LinearEasing
                        )
                    )
                }
                "Pan Left" -> {
                    animatedScale.snapTo(1.1f)
                    animatedOffsetX.animateTo(
                        targetValue = -30f,
                        animationSpec = tween(
                            durationMillis = currentScene.durationSeconds * 1000,
                            easing = LinearEasing
                        )
                    )
                }
                "Pan Right" -> {
                    animatedScale.snapTo(1.1f)
                    animatedOffsetX.animateTo(
                        targetValue = 30f,
                        animationSpec = tween(
                            durationMillis = currentScene.durationSeconds * 1000,
                            easing = LinearEasing
                        )
                    )
                }
                else -> {
                    // Constant slow zoom default
                    animatedScale.animateTo(
                        targetValue = 1.08f,
                        animationSpec = tween(
                            durationMillis = currentScene.durationSeconds * 1000,
                            easing = LinearEasing
                        )
                    )
                }
            }
        } else {
            animatedScale.snapTo(1.02f)
            animatedOffsetX.snapTo(0f)
        }
    }

    // Playback controller ticking
    LaunchedEffect(isPlaying, currentPlayingSceneIndex, scenes) {
        if (isPlaying && scenes.isNotEmpty()) {
            while (isPlaying) {
                delay(100)
                val scene = scenes.getOrNull(currentPlayingSceneIndex)
                if (scene != null) {
                    playbackTimeSeconds += 0.1f
                    if (playbackTimeSeconds >= scene.durationSeconds) {
                        // Move to next scene
                        if (currentPlayingSceneIndex < scenes.size - 1) {
                            currentPlayingSceneIndex++
                            playbackTimeSeconds = 0f
                        } else {
                            // Finished timeline! Loop or stop
                            isPlaying = false
                            currentPlayingSceneIndex = 0
                            playbackTimeSeconds = 0f
                        }
                    }
                } else {
                    isPlaying = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "HM AI EDITOR STUDIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            project?.title ?: "Timeline",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Export Button
                    Button(
                        onClick = { viewModel.exportVideo() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_video_button")
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXPORT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSlateBg)
            )
        },
        containerColor = DarkSlateBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. WIDESCREEN CINEMATIC VIDEO PLAYER FRAME
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (currentScene != null && currentScene.imageUrl.isNotEmpty()) {
                    // Animated Zoom and Pan layers
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentScene.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Scene image playback",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedScale.value
                                scaleY = animatedScale.value
                                translationX = animatedOffsetX.value
                            }
                    )

                    // Cinematic Visual Effects Filters
                    when (currentScene.effect) {
                        "Vignette" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                        }
                        "Retro Grain" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.15f))
                            ) {
                                // Dynamic ambient grains
                                val infiniteTransition = rememberInfiniteTransition()
                                val grainOffset by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 100f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.05f),
                                                radius = 2.dp.toPx(),
                                                center = center.copy(
                                                    x = center.x + grainOffset,
                                                    y = center.y - grainOffset
                                                )
                                            )
                                        }
                                )
                            }
                        }
                    }

                    // Bottom: Subtitles style
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                            .padding(bottom = 16.dp, top = 32.dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            currentScene.narrationText,
                            color = if (project?.subtitleStyle?.contains("Yellow") == true) Color.Yellow else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Audio Sound waves indicator (if playing)
                    if (isPlaying) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            // Simple waveform bars
                            repeat(4) { idx ->
                                val infiniteTransition = rememberInfiniteTransition()
                                val scaleY by infiniteTransition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween((300..600).random(), easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height((12 * scaleY).dp)
                                        .background(GoldAccent)
                                )
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Slideshow,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No generated scenes in timeline", color = TextSecondary)
                    }
                }
            }

            // Playback controls toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play time
                Text(
                    "Scene ${currentPlayingSceneIndex + 1} • ${String.format("%.1f", playbackTimeSeconds)}s / ${currentScene?.durationSeconds ?: 0}s",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = {
                            if (currentPlayingSceneIndex > 0) {
                                currentPlayingSceneIndex--
                                playbackTimeSeconds = 0f
                            }
                        },
                        enabled = currentPlayingSceneIndex > 0
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Scene", tint = if (currentPlayingSceneIndex > 0) TextPrimary else TextSecondary.copy(alpha = 0.3f))
                    }

                    // Play/Pause
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .background(GoldAccent, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play timeline",
                            tint = Color.Black
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentPlayingSceneIndex < scenes.size - 1) {
                                currentPlayingSceneIndex++
                                playbackTimeSeconds = 0f
                            }
                        },
                        enabled = currentPlayingSceneIndex < scenes.size - 1
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next Scene", tint = if (currentPlayingSceneIndex < scenes.size - 1) TextPrimary else TextSecondary.copy(alpha = 0.3f))
                    }
                }

                // Subtitle/music quick access
                IconButton(onClick = { showMusicSelector = true }) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Audio Track", tint = GoldAccent)
                }
            }

            // 2. TIMELINE EDITOR GRID / SCROLLABLE BLOCKS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EDITABLE SCENE BLOCKS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    "Click block to edit",
                    fontSize = 10.sp,
                    color = GoldAccent
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(scenes) { index, scene ->
                    val isActive = index == currentPlayingSceneIndex
                    Card(
                        onClick = { editingScene = scene },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) GoldAccent.copy(alpha = 0.08f) else CardSurface
                        ),
                        border = BorderStroke(
                            if (isActive) 2.dp else 1.dp,
                            if (isActive) GoldAccent else Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier
                            .width(180.dp)
                            .fillMaxHeight()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Scene Image Frame
                            if (scene.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(scene.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.55f)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.55f)
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f))
                                }
                            }

                            // Resolution badge
                            Surface(
                                color = if (scene.resolution == "4K") GoldAccent else Color.Gray,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    scene.resolution,
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Text details & Ordering Arrow buttons
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.45f)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${index + 1}. ${scene.sentence}",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                "${scene.durationSeconds}s",
                                                color = NeonCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                scene.transition,
                                                color = GoldAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Arrange Arrow Buttons
                                    Row {
                                        IconButton(
                                            onClick = { viewModel.moveScene(index, index - 1) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", tint = if (index > 0) TextPrimary else TextSecondary.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.moveScene(index, index + 1) },
                                            enabled = index < scenes.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", tint = if (index < scenes.size - 1) TextPrimary else TextSecondary.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add scene quick action card
                item {
                    Card(
                        onClick = { viewModel.addSceneToTimeline(project?.id ?: 0, scenes.size - 1) },
                        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .width(100.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Scene", tint = GoldAccent, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ADD SCENE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. EDIT SCENE DIALOG PANEL
        if (editingScene != null) {
            val scene = editingScene!!
            var promptField by remember { mutableStateOf(scene.imagePrompt) }
            var narrationField by remember { mutableStateOf(scene.narrationText) }
            var modelField by remember { mutableStateOf(scene.modelName) }
            var durationField by remember { mutableStateOf(scene.durationSeconds) }
            var transitionField by remember { mutableStateOf(scene.transition) }
            var effectField by remember { mutableStateOf(scene.effect) }

            var narrationTypeField by remember { mutableStateOf(scene.narrationType) }
            var narrationVoiceField by remember { mutableStateOf(scene.narrationVoice) }
            var uploadedAudioPathField by remember { mutableStateOf(scene.uploadedAudioPath) }
            var narrationVolumeField by remember { mutableStateOf(scene.narrationVolume) }
            var narrationTimingOffsetField by remember { mutableStateOf(scene.narrationTimingOffset) }

            var soundEffectField by remember { mutableStateOf(scene.soundEffect) }
            var sfxVolumeField by remember { mutableStateOf(scene.sfxVolume) }
            var sfxTimingOffsetField by remember { mutableStateOf(scene.sfxTimingOffset) }

            Dialog(onDismissRequest = { editingScene = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "EDIT TIMELINE SCENE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldAccent
                        )

                        // Sentences
                        OutlinedTextField(
                            value = promptField,
                            onValueChange = { promptField = it },
                            label = { Text("AI Image Prompt") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, focusedLabelColor = GoldAccent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = narrationField,
                            onValueChange = { narrationField = it },
                            label = { Text("Narration & Subtitle Text") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, focusedLabelColor = GoldAccent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Model selection
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Model", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Quick loop model picker
                                            val keys = ImageGenerator.modelMap.keys.toList()
                                            val currentIdx = keys.indexOf(modelField)
                                            modelField = keys[(currentIdx + 1) % keys.size]
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(modelField, color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duration", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            durationField = if (durationField >= 12) 3 else durationField + 1
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${durationField}s", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Effects and Transitions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Visual Effect", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val effects = listOf("Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Vignette", "Retro Grain", "None")
                                            val currentIdx = effects.indexOf(effectField)
                                            effectField = effects[(currentIdx + 1) % effects.size]
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(effectField, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Transition", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val transitions = listOf("Fade", "Slide", "Crossfade", "None")
                                            val currentIdx = transitions.indexOf(transitionField)
                                            transitionField = transitions[(currentIdx + 1) % transitions.size]
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(transitionField, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Section: Narration Voiceover
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "SCENE NARRATION VOICEOVER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                
                                // Narration Type Switcher
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("TTS" to "AI TTS Engine 🎙️", "Upload" to "Audio Upload 📁").forEach { source ->
                                        val selected = narrationTypeField == source.first
                                        Surface(
                                            onClick = { narrationTypeField = source.first },
                                            color = if (selected) GoldAccent.copy(alpha = 0.2f) else CardSurface,
                                            border = BorderStroke(1.dp, if (selected) GoldAccent else Color.Transparent),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                                Text(source.second, color = if (selected) GoldAccent else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (narrationTypeField == "TTS") {
                                    // Voice Picker
                                    Column {
                                        Text("Voice Model", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val voices = listOf("Warm Storyteller", "AI Deep Voice", "Cyber Speaker", "Narrator Alexa", "Calm Guide")
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val currentIdx = voices.indexOf(narrationVoiceField)
                                                    narrationVoiceField = voices[(currentIdx + 1) % voices.size]
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(narrationVoiceField, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    // Simulated Upload
                                    Column {
                                        Text("Uploaded Narration Track", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = uploadedAudioPathField,
                                                onValueChange = { uploadedAudioPathField = it },
                                                placeholder = { Text("e.g. vocal_track_03.wav") },
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent, focusedLabelColor = GoldAccent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    val randomUploads = listOf("recording_voiceover_scene_${scene.sequenceOrder + 1}.mp3", "narrator_hd_scene_${scene.sequenceOrder + 1}.wav", "audio_vocal_perfect.m4a")
                                                    uploadedAudioPathField = randomUploads.random()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF381E72))
                                            ) {
                                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("BROWSE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Narration Volume Slider
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Narration Volume", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${(narrationVolumeField * 100).toInt()}%", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = narrationVolumeField,
                                        onValueChange = { narrationVolumeField = it },
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
                                    )
                                }

                                // Narration Timing Offset
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Narration Align Offset", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${if (narrationTimingOffsetField >= 0) "+" else ""}${narrationTimingOffsetField}ms", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = narrationTimingOffsetField.toFloat(),
                                        onValueChange = { narrationTimingOffsetField = it.toInt() },
                                        valueRange = -2000f..2000f,
                                        steps = 40,
                                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                    )
                                }
                            }
                        }

                        // Section: Sound Effects (SFX) Library
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "ROYALTY-FREE SOUND EFFECTS (SFX)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )

                                // SFX Dropdown Choice
                                Column {
                                    Text("Select Sound Effect", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val sfxList = listOf("None", "Nature Wind 🍃", "Laser Shot 🔫", "Camera Click 📸", "Sci-Fi Whoosh 💨", "Footsteps 👣", "Deep Thunder ⚡", "Water Splash 💧", "City Ambient 🌆")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val currentIdx = sfxList.indexOf(soundEffectField)
                                                soundEffectField = sfxList[if (currentIdx == -1) 0 else (currentIdx + 1) % sfxList.size]
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(soundEffectField, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // SFX Volume Slider
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("SFX Volume", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${(sfxVolumeField * 100).toInt()}%", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = sfxVolumeField,
                                        onValueChange = { sfxVolumeField = it },
                                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                    )
                                }

                                // SFX Timing Offset
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("SFX Delay Offset", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${if (sfxTimingOffsetField >= 0) "+" else ""}${sfxTimingOffsetField}ms", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = sfxTimingOffsetField.toFloat(),
                                        onValueChange = { sfxTimingOffsetField = it.toInt() },
                                        valueRange = -2000f..2000f,
                                        steps = 40,
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Regenerate and Delete Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Delete Scene Button
                            Button(
                                onClick = {
                                    viewModel.deleteSceneFromTimeline(scene.id)
                                    editingScene = null
                                    Toast.makeText(context, "Scene deleted from timeline", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DELETE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Regenerate Scene Image Button
                            Button(
                                onClick = {
                                    val updated = scene.copy(
                                        imagePrompt = promptField,
                                        narrationText = narrationField,
                                        modelName = modelField,
                                        durationSeconds = durationField,
                                        transition = transitionField,
                                        effect = effectField,
                                        narrationType = narrationTypeField,
                                        narrationVoice = narrationVoiceField,
                                        uploadedAudioPath = uploadedAudioPathField,
                                        narrationVolume = narrationVolumeField,
                                        narrationTimingOffset = narrationTimingOffsetField,
                                        soundEffect = soundEffectField,
                                        sfxVolume = sfxVolumeField,
                                        sfxTimingOffset = sfxTimingOffsetField
                                    )
                                    viewModel.regenerateSceneImage(updated)
                                    editingScene = null
                                    Toast.makeText(context, "Regenerating photo...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF381E72)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RE-DRAW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Save details
                        Button(
                            onClick = {
                                val updated = scene.copy(
                                    imagePrompt = promptField,
                                    narrationText = narrationField,
                                    modelName = modelField,
                                    durationSeconds = durationField,
                                    transition = transitionField,
                                    effect = effectField,
                                    narrationType = narrationTypeField,
                                    narrationVoice = narrationVoiceField,
                                    uploadedAudioPath = uploadedAudioPathField,
                                    narrationVolume = narrationVolumeField,
                                    narrationTimingOffset = narrationTimingOffsetField,
                                    soundEffect = soundEffectField,
                                    sfxVolume = sfxVolumeField,
                                    sfxTimingOffset = sfxTimingOffsetField
                                )
                                viewModel.updateSceneDetails(updated)
                                editingScene = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE TIMELINE VALUES", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. CHOOSE BACKGROUND MUSIC PANEL
        if (showMusicSelector) {
            Dialog(onDismissRequest = { showMusicSelector = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "TIMELINE AUDIO SETTINGS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldAccent
                        )

                        // Music Track Selection
                        Text("SELECT BACKGROUND TRACK", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        val tracks = listOf("Cinematic Ambient", "Cyberpunk Industrial", "Epic orchestral", "Lofi Sunset Beat")
                        tracks.forEach { track ->
                            val selected = project?.backgroundMusic == track
                            Surface(
                                onClick = {
                                    if (project != null) {
                                        viewModel.updateProjectDetails(project!!.copy(backgroundMusic = track))
                                    }
                                },
                                color = if (selected) GoldAccent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, if (selected) GoldAccent else Color.Transparent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(track, color = if (selected) GoldAccent else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Background Music Volume Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MUSIC VOLUME", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("${(((project?.musicVolume ?: 0.5f) * 100).toInt())}%", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = project?.musicVolume ?: 0.5f,
                                onValueChange = { vol ->
                                    val currentProj = project
                                    if (currentProj != null) {
                                        viewModel.updateProjectDetails(currentProj.copy(musicVolume = vol))
                                    }
                                },
                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
                            )
                        }

                        // Background Music Start Delay
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MUSIC START OFFSET / DELAY", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("${if ((project?.musicTimingOffset ?: 0) >= 0) "+" else ""}${project?.musicTimingOffset ?: 0}ms", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = (project?.musicTimingOffset ?: 0).toFloat(),
                                onValueChange = { offset ->
                                    val currentProj = project
                                    if (currentProj != null) {
                                        viewModel.updateProjectDetails(currentProj.copy(musicTimingOffset = offset.toInt()))
                                    }
                                },
                                valueRange = -5000f..5000f,
                                steps = 50,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )
                        }

                        // Subtitle Styling
                        Text("SUBTITLE COLOR STYLE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Yellow", "White").forEach { style ->
                                val subtitleStyleStr = "Bottom Centered - $style"
                                val selected = project?.subtitleStyle == subtitleStyleStr
                                Surface(
                                    onClick = {
                                        if (project != null) {
                                            viewModel.updateProjectDetails(project!!.copy(subtitleStyle = subtitleStyleStr))
                                        }
                                    },
                                    color = if (selected) GoldAccent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (selected) GoldAccent else Color.Transparent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        style,
                                        color = if (style == "Yellow") Color.Yellow else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showMusicSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("APPLY AUDIO VALUES", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. EXPORT VIDEO RENDER OVERLAY SCREEN
        if (exportState !is ExportState.Idle) {
            Dialog(onDismissRequest = { }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (val state = exportState) {
                            is ExportState.Exporting -> {
                                Text(
                                    "COMPILING CINEMATIC FILE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent,
                                    letterSpacing = 2.sp
                                )

                                CircularProgressIndicator(
                                    progress = { state.progress },
                                    color = GoldAccent,
                                    strokeWidth = 6.dp,
                                    modifier = Modifier.size(72.dp)
                                )

                                Text(
                                    "${(state.progress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Text(
                                    state.status,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            is ExportState.Success -> {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color.Green,
                                    modifier = Modifier.size(64.dp)
                                )

                                Text(
                                    "EXPORT COMPLETE!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Green
                                )

                                Text(
                                    "Your masterpiece has been rendered at 4K Ultra-HD resolution and saved to public movie directory.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        state.filePath,
                                        fontSize = 10.sp,
                                        color = GoldAccent,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.resetExport()
                                            Toast.makeText(context, "Video shared!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF381E72)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.resetExport() },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("DONE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

// Simple App routing enum
enum class Screen {
    Home,
    Create,
    Generating,
    Timeline
}
