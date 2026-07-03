package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectByIdSync(projectId: Long): Project?

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequenceOrder ASC")
    fun getScenesForProject(projectId: Long): Flow<List<Scene>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequenceOrder ASC")
    suspend fun getScenesForProjectSync(projectId: Long): List<Scene>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: Scene): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<Scene>)

    @Update
    suspend fun updateProject(project: Project)

    @Update
    suspend fun updateScene(scene: Scene)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM scenes WHERE id = :sceneId")
    suspend fun deleteSceneById(sceneId: Long)
}

@Database(entities = [Project::class, Scene::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
