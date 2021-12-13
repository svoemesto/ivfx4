package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ShotRepo : CrudRepository<Shot, Long> {
    fun findByFileId(fileId: Long): Iterable<Shot>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots WHERE id = ?", nativeQuery = true)
    fun delete(frameId:Long)

    fun findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileId: Long, firstFrameNumber: Int): Iterable<Shot>

    fun findByFileIdAndFirstFrameNumberAndLastFrameNumber(
        fileId: Long,
        firstFrameNumber: Int,
        lastFrameNumber: Int
    ): Iterable<Shot>

    @Query(value = "SELECT * FROM tbl_shots " +
            "INNER JOIN tbl_scenes_shots ON tbl_shots.id = tbl_scenes_shots.shot_id " +
            "WHERE tbl_scenes_shots.scene_id = ?", nativeQuery = true)
    fun getShotsForScenes(sceneId:Long) : Iterable<Shot>

    @Query(value = "SELECT * FROM tbl_shots " +
            "INNER JOIN tbl_events_shots ON tbl_shots.id = tbl_events_shots.shot_id " +
            "WHERE tbl_events_shots.event_id = ?", nativeQuery = true)
    fun getShotsForEvents(eventId:Long) : Iterable<Shot>

}