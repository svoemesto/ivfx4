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

    fun findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileId: Long, firstFrameNumber: Int): Iterable<Scene>

    fun findByFileIdAndFirstFrameNumberAndLastFrameNumber(
        fileId: Long,
        firstFrameNumber: Int,
        lastFrameNumber: Int
    ): Iterable<Scene>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_scenes WHERE id = ?", nativeQuery = true)
    fun delete(sceneId:Long)

    @Query(value = "select tsc.* from tbl_scenes as tsc " +
            "inner join tbl_shots as tsh on (tsh.file_id = tsc.file_id and tsh.first_frame_number >= tsc.first_frame_number and tsh.last_frame_number <= tsc.last_frame_number) " +
            "where tsh.id = ?", nativeQuery = true)
    fun getSceneForShot(shotId: Long): Iterable<Scene>


}