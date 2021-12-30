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

    @Query(value = "select tsh.* from tbl_shots as tsh " +
            "inner join tbl_scenes as tsc on (tsh.file_id = tsc.file_id and tsh.first_frame_number >= tsc.first_frame_number and tsh.last_frame_number <= tsc.last_frame_number) " +
            "where tsc.id = ?", nativeQuery = true)
    fun getShotsForScenes(sceneId:Long) : Iterable<Shot>

    @Query(value = "select tsh.* from tbl_shots as tsh " +
            "inner join tbl_events as tev on (tsh.file_id = tev.file_id and tsh.first_frame_number >= tev.first_frame_number and tsh.last_frame_number <= tev.last_frame_number) " +
            "where tev.id = ?", nativeQuery = true)
    fun getShotsForEvents(eventId:Long) : Iterable<Shot>

}