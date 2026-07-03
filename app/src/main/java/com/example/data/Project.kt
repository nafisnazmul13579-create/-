package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalPrompt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val backgroundMusic: String = "Cinematic Ambient", // default music
    val musicVolume: Float = 0.5f,
    val musicTimingOffset: Int = 0,
    val subtitleStyle: String = "Bottom Centered - Yellow",
    val status: String = "Draft", // "Draft", "Generating", "Completed"
    val visualPreset: String = "cinematic" // "none", "cinematic", "cartoon", "photorealistic", "vintage"
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class Scene(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sequenceOrder: Int,
    val sentence: String,
    val imagePrompt: String,
    val videoPrompt: String,
    val modelName: String, // One of 10 free models
    val resolution: String = "HD", // "4K" or "HD"
    val imageUrl: String = "",
    val videoUrl: String = "",
    val narrationText: String = "",
    val durationSeconds: Int = 5,
    val transition: String = "Fade", // "Fade", "Slide", "Crossfade", "None"
    val effect: String = "Zoom In", // "Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Vignette", "Retro Grain"
    val isGenerated: Boolean = false,
    
    // Audio upload & TTS narration
    val narrationType: String = "TTS", // "TTS" or "Upload"
    val narrationVoice: String = "Warm Storyteller", // TTS voice selection
    val uploadedAudioPath: String = "", // custom uploaded audio file path/URI
    val narrationVolume: Float = 1.0f,
    val narrationTimingOffset: Int = 0,
    
    // SFX library
    val soundEffect: String = "None", // Sound effect name
    val sfxVolume: Float = 0.8f,
    val sfxTimingOffset: Int = 0
)
