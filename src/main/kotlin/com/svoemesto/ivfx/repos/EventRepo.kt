package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface EventRepo : CrudRepository<Event, Long> {
    fun findByFileId(fileId: Long): Iterable<Event>

    fun findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileId: Long, firstFrameNumber: Int): Iterable<Event>

    fun findByFileIdAndFirstFrameNumberAndLastFrameNumber(
        fileId: Long,
        firstFrameNumber: Int,
        lastFrameNumber: Int
    ): Iterable<Event>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events WHERE id = ?", nativeQuery = true)
    fun delete(sceneId:Long)

    @Query(value = "select tev.* from tbl_events as tev " +
            "inner join tbl_shots as tsh on (tsh.file_id = tev.file_id and tsh.first_frame_number >= tev.first_frame_number and tsh.last_frame_number <= tev.last_frame_number) " +
            "where tsh.id = ?", nativeQuery = true)
    fun getEventForShot(shotId: Long): Iterable<Event>

}