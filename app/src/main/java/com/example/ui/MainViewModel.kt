package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Project
import com.example.data.ProjectRepository
import com.example.data.Scene
import com.example.network.GeminiApiClient
import com.example.network.ImageGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GenerationState {
    object Idle : GenerationState
    object Preparing : GenerationState
    data class GeneratingImage(val scene: Scene) : GenerationState
    data class ConvertingVideo(val scene: Scene, val progress: Float) : GenerationState
    data class AddingToTimeline(val scene: Scene) : GenerationState
    object Finished : GenerationState
    data class Error(val message: String) : GenerationState
}

sealed interface ExportState {
    object Idle : ExportState
    data class Exporting(val progress: Float, val status: String) : ExportState
    data class Success(val filePath: String) : ExportState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    private val repository = ProjectRepository(application)

    // Projects list Flow
    val allProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active editing project state
    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _activeScenes = MutableStateFlow<List<Scene>>(emptyList())
    val activeScenes: StateFlow<List<Scene>> = _activeScenes.asStateFlow()

    // Sequential generation states
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _currentGeneratingProject = MutableStateFlow<Project?>(null)
    val currentGeneratingProject: StateFlow<Project?> = _currentGeneratingProject.asStateFlow()

    private val _currentGeneratingScenes = MutableStateFlow<List<Scene>>(emptyList())
    val currentGeneratingScenes: StateFlow<List<Scene>> = _currentGeneratingScenes.asStateFlow()

    private val _currentGeneratingIndex = MutableStateFlow(0)
    val currentGeneratingIndex: StateFlow<Int> = _currentGeneratingIndex.asStateFlow()

    // Export progress state
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Loaded project ID
    private var loadedProjectId: Long? = null

    fun loadProject(projectId: Long) {
        loadedProjectId = projectId
        viewModelScope.launch {
            repository.getProject(projectId).collect { proj ->
                _activeProject.value = proj
            }
        }
        viewModelScope.launch {
            repository.getScenes(projectId).collect { scenes ->
                _activeScenes.value = scenes
            }
        }
    }

    fun applyPresetToPrompt(prompt: String, preset: String): String {
        val styleModifier = when (preset.lowercase()) {
            "cinematic" -> "Cinematic film style, anamorphic, dramatic side lighting, 35mm photograph, epic scenery, atmospheric, detailed"
            "cartoon" -> "Vibrant 3D animated film cartoon style, Pixar Disney animation aesthetic, clean smooth lighting, highly colorful, playful character concept"
            "photorealistic" -> "Photorealistic, hyper-detailed, lifelike textures, natural morning light, shot on DSLR camera, professional photography, high fidelity"
            "vintage" -> "Vintage analog film photo, nostalgic 1980s retro aesthetics, subtle grain, warm faded colors, polaroid photo feel"
            else -> ""
        }
        return if (styleModifier.isNotEmpty()) "$prompt, $styleModifier" else prompt
    }

    // Process a new story prompt
    fun createAndGenerateProject(title: String, story: String, defaultModel: String, visualPreset: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Preparing
            
            // 1. Split story into scenes using Gemini
            val parsedScenes = GeminiApiClient.splitStoryIntoScenes(story)
            if (parsedScenes.isEmpty()) {
                _generationState.value = GenerationState.Error("Could not split story into scenes. Please try again.")
                return@launch
            }

            // 2. Create the project entry in Room
            val project = Project(
                title = title.ifBlank { "Untitled Story" },
                originalPrompt = story,
                status = "Generating",
                visualPreset = visualPreset
            )
            val projectId = repository.createProject(project)
            val updatedProject = project.copy(id = projectId)
            _currentGeneratingProject.value = updatedProject

            // 3. Insert initial ungenerated scenes in Room
            val dbScenes = parsedScenes.mapIndexed { index, parsed ->
                Scene(
                    projectId = projectId,
                    sequenceOrder = index,
                    sentence = parsed.sentence,
                    imagePrompt = parsed.imagePrompt,
                    videoPrompt = parsed.videoPrompt,
                    modelName = if (parsed.modelName.isNotBlank()) parsed.modelName else defaultModel,
                    narrationText = parsed.narrationText.ifBlank { parsed.sentence },
                    durationSeconds = parsed.durationSeconds.coerceIn(3, 12),
                    isGenerated = false,
                    narrationType = "TTS",
                    narrationVoice = "Warm Storyteller",
                    soundEffect = "None"
                )
            }
            repository.insertScenes(dbScenes)
            
            val initialScenes = repository.getScenesSync(projectId)
            _currentGeneratingScenes.value = initialScenes

            // 4. Start sequential, one-by-one scene generation
            startSequentialGeneration(updatedProject, initialScenes)
        }
    }

    private suspend fun startSequentialGeneration(project: Project, scenes: List<Scene>) = withContext(Dispatchers.IO) {
        var currentIdx = 0
        while (currentIdx < scenes.size) {
            val scene = scenes[currentIdx]
            _currentGeneratingIndex.value = currentIdx

            // Phase 1: Generate the image (programmatic 4K fallback check)
            _generationState.value = GenerationState.GeneratingImage(scene)
            val seed = (1000..99999).random()
            
            val styledPrompt = applyPresetToPrompt(scene.imagePrompt, project.visualPreset)
            val (imageUrl, resolution) = ImageGenerator.generateImageWithFallback(
                prompt = styledPrompt,
                model = scene.modelName,
                seed = seed
            )

            val updatedScene = scene.copy(
                imageUrl = imageUrl,
                resolution = resolution
            )
            repository.updateScene(updatedScene)
            
            // Update UI flow list in real-time
            _currentGeneratingScenes.value = _currentGeneratingScenes.value.toMutableList().apply {
                set(currentIdx, updatedScene)
            }

            // Phase 2: Convert the image into a video (simulated conversion loop)
            _generationState.value = GenerationState.ConvertingVideo(updatedScene, 0f)
            
            val progressSteps = mutableListOf(
                "Slicing 3D depth maps...",
                "Interpolating motion paths...",
                "Synthesizing optical flows...",
                "Encoding cinematic sequence..."
            )
            if (scene.narrationType == "TTS") {
                progressSteps.add("Synthesizing TTS narration with voice [${scene.narrationVoice}]...")
            } else if (scene.uploadedAudioPath.isNotEmpty()) {
                progressSteps.add("Aligning custom narration audio file...")
            } else {
                progressSteps.add("Preparing fallback silent narration tracks...")
            }

            if (scene.soundEffect != "None") {
                progressSteps.add("Blending sound effect [${scene.soundEffect}] with ${scene.sfxVolume * 100}% volume...")
            }
            progressSteps.add("Encoding audio with background music [${project.backgroundMusic}]...")
            progressSteps.add("Assembling final timeline frames...")

            for (p in 1..100) {
                delay(35) // total time ~3.5 seconds per scene video generation
                val stepRatio = (p * progressSteps.size) / 101
                val progressStepText = progressSteps[stepRatio.coerceIn(0, progressSteps.size - 1)]
                _generationState.value = GenerationState.ConvertingVideo(updatedScene, p / 100f)
            }

            // Phase 3: Save final scene state & Add to timeline
            val finalScene = updatedScene.copy(
                videoUrl = imageUrl, // we render the image with dynamic pan/zoom in timeline
                isGenerated = true
            )
            repository.updateScene(finalScene)

            _currentGeneratingScenes.value = _currentGeneratingScenes.value.toMutableList().apply {
                set(currentIdx, finalScene)
            }

            _generationState.value = GenerationState.AddingToTimeline(finalScene)
            delay(1200) // Beautiful visual pause to let user see it added to timeline

            currentIdx++
        }

        // Complete project
        val completedProject = project.copy(status = "Completed")
        repository.updateProject(completedProject)
        _currentGeneratingProject.value = completedProject
        _generationState.value = GenerationState.Finished

        // Auto load completed project
        loadProject(project.id)
    }

    // Cancel or skip generation
    fun resetGeneration() {
        _generationState.value = GenerationState.Idle
        _currentGeneratingProject.value = null
        _currentGeneratingScenes.value = emptyList()
        _currentGeneratingIndex.value = 0
    }

    // Timeline Modifications
    fun updateSceneDetails(scene: Scene) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScene(scene)
        }
    }

    fun updateProjectDetails(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProject(project)
        }
    }

    fun deleteSceneFromTimeline(sceneId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScene(sceneId)
            // Reorder remaining scenes
            val currentList = _activeScenes.value.filter { it.id != sceneId }
            repository.updateSceneOrders(currentList)
        }
    }

    fun moveScene(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _activeScenes.value.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val moved = list.removeAt(fromIndex)
                list.add(toIndex, moved)
                repository.updateSceneOrders(list)
            }
        }
    }

    fun addSceneToTimeline(projectId: Long, afterIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _activeScenes.value.toMutableList()
            val newScene = Scene(
                projectId = projectId,
                sequenceOrder = afterIndex + 1,
                sentence = "New custom timeline scene.",
                imagePrompt = "Cinematic shot, masterpiece lighting, highly detailed",
                videoPrompt = "Slow pan zoom camera effect",
                modelName = "flux",
                narrationText = "New scene narration text.",
                durationSeconds = 5,
                isGenerated = false
            )
            list.add(afterIndex + 1, newScene)
            repository.updateSceneOrders(list)
        }
    }

    fun regenerateSceneImage(scene: Scene) {
        viewModelScope.launch(Dispatchers.IO) {
            // Set ungenerated state briefly
            val resetting = scene.copy(isGenerated = false)
            repository.updateScene(resetting)
            
            val project = repository.getProjectSync(scene.projectId)
            val preset = project?.visualPreset ?: "none"
            val styledPrompt = applyPresetToPrompt(scene.imagePrompt, preset)
            
            val seed = (1000..99999).random()
            val (imageUrl, resolution) = ImageGenerator.generateImageWithFallback(
                prompt = styledPrompt,
                model = scene.modelName,
                seed = seed
            )

            val updated = scene.copy(
                imageUrl = imageUrl,
                videoUrl = imageUrl,
                resolution = resolution,
                isGenerated = true
            )
            repository.updateScene(updated)
        }
    }

    // Delete entire project
    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(project)
        }
    }

    // Export Video Simulation
    fun exportVideo() {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(0f, "Assembling timeline frames...")
            delay(1000)
            _exportState.value = ExportState.Exporting(0.15f, "Rendering visual models and effects...")
            delay(1200)
            _exportState.value = ExportState.Exporting(0.40f, "Generating subtitles and rendering fonts...")
            delay(1000)
            _exportState.value = ExportState.Exporting(0.65f, "Encoding audio with background music...")
            delay(1000)
            _exportState.value = ExportState.Exporting(0.85f, "Packaging MP4 container stream...")
            delay(800)
            _exportState.value = ExportState.Exporting(1.00f, "Finalizing!")
            delay(500)
            _exportState.value = ExportState.Success("/sdcard/Movies/HM_AI_Export_${System.currentTimeMillis()}.mp4")
        }
    }

    fun resetExport() {
        _exportState.value = ExportState.Idle
    }
}
