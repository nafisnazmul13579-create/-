package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

class ProjectRepository(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "hm_ai_database"
    ).fallbackToDestructiveMigration()
        .build()

    private val dao = database.projectDao()

    fun getAllProjects(): Flow<List<Project>> = dao.getAllProjects()

    fun getProject(projectId: Long): Flow<Project?> = dao.getProjectById(projectId)

    suspend fun getProjectSync(projectId: Long): Project? = dao.getProjectByIdSync(projectId)

    fun getScenes(projectId: Long): Flow<List<Scene>> = dao.getScenesForProject(projectId)

    suspend fun getScenesSync(projectId: Long): List<Scene> = dao.getScenesForProjectSync(projectId)

    suspend fun createProject(project: Project): Long = dao.insertProject(project)

    suspend fun insertScenes(scenes: List<Scene>) = dao.insertScenes(scenes)

    suspend fun updateProject(project: Project) = dao.updateProject(project)

    suspend fun updateScene(scene: Scene) = dao.updateScene(scene)

    suspend fun deleteProject(project: Project) = dao.deleteProject(project)

    suspend fun deleteScene(sceneId: Long) = dao.deleteSceneById(sceneId)

    suspend fun updateSceneOrders(scenes: List<Scene>) {
        val updated = scenes.mapIndexed { index, scene ->
            scene.copy(sequenceOrder = index)
        }
        dao.insertScenes(updated)
    }
}
