package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface SceneRepo : CrudRepository<Scene, Long> {
    fun findByFileId(fileId: Long): Iterable<Scene>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes WHERE id = ?", nativeQuery = true)
    fun delete(sceneId:Long)

    @Query(value = "SELECT * FROM tbl_scenes " +
            "INNER JOIN tbl_scenes_shots ON tbl_scenes.id = tbl_scenes_shots.scene_id " +
            "WHERE tbl_scenes_shots.shot_id = ?", nativeQuery = true)
    fun getScenesForShot(shotId:Long) : Iterable<Scene>

}