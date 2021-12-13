package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.SceneShot
import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface SceneShotRepo : CrudRepository<SceneShot, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes_shots WHERE scene_id = ?", nativeQuery = true)
    fun deleteAllForScene(sceneId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes_shots WHERE shot_id = ?", nativeQuery = true)
    fun deleteAllForShot(shotId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes_shots WHERE id = ?", nativeQuery = true)
    fun delete(sceneShotId:Long)

    @Query(value = "SELECT * FROM tbl_scenes_shots WHERE shot_id = ?", nativeQuery = true)
    fun getScenesShotsForShot(shotId:Long) : Iterable<SceneShot>

    @Query(value = "SELECT * FROM tbl_scenes_shots WHERE scene_id = ?", nativeQuery = true)
    fun getScenesShotsForScene(sceneId:Long) : Iterable<SceneShot>

    @Query(value = "SELECT tbl_scenes_shots.* FROM tbl_scenes_shots INNER JOIN tbl_scenes ON tbl_scenes.id = tbl_scenes_shots.scene_id WHERE tbl_scenes.file_id = ?", nativeQuery = true)
    fun getScenesShotsForFile(fileId:Long) : Iterable<SceneShot>

}