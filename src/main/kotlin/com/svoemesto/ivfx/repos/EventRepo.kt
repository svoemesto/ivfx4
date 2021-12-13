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

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events WHERE id = ?", nativeQuery = true)
    fun delete(sceneId:Long)

    @Query(value = "SELECT * FROM tbl_events " +
            "INNER JOIN tbl_events_shots ON tbl_events.id = tbl_events_shots.event_id " +
            "WHERE tbl_events_shots.shot_id = ?", nativeQuery = true)
    fun getEventsForShot(shotId:Long) : Iterable<Event>

}